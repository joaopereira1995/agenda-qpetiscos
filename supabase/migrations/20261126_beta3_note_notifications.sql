-- Agenda QPetiscos v0.8.0-beta.3
-- Apply only after review/backup in Supabase; this file is NOT executed by GitHub.
-- Existing devices remain opted out. Existing event queue and RPCs are unchanged.
BEGIN;

ALTER TABLE public.notification_preferences
  ADD COLUMN IF NOT EXISTS notes_notifications_enabled boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS notify_notes boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS notify_important_notes boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notify_tasks boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notify_assigned_tasks boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notify_note_updates boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS notify_note_comments boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notify_task_completed boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS notify_task_reopened boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS task_reminders_enabled boolean NOT NULL DEFAULT true;

ALTER TABLE public.internal_notes
  ADD COLUMN IF NOT EXISTS reminder_minutes integer,
  ADD COLUMN IF NOT EXISTS reminder_recipient text,
  ADD COLUMN IF NOT EXISTS reminder_device_id uuid REFERENCES public.devices(id),
  ADD COLUMN IF NOT EXISTS reminder_revision bigint NOT NULL DEFAULT 0;

ALTER TABLE public.internal_notes DROP CONSTRAINT IF EXISTS qp_note_reminder_check;
ALTER TABLE public.internal_notes ADD CONSTRAINT qp_note_reminder_check CHECK (
  (reminder_minutes IS NULL AND reminder_recipient IS NULL AND reminder_device_id IS NULL)
  OR (
    kind = 'task' AND due_at IS NOT NULL AND reminder_minutes IS NOT NULL
    AND reminder_minutes IN (0,5,15,30,60,120,1440)
    AND reminder_recipient IS NOT NULL AND (
      (reminder_recipient = 'me' AND reminder_device_id IS NOT NULL)
      OR (reminder_recipient = 'all' AND reminder_device_id IS NULL)
      OR (reminder_recipient = 'assigned' AND assigned_device_id IS NOT NULL AND reminder_device_id IS NULL)
    )
  )
);

ALTER TABLE public.notifications DROP CONSTRAINT notifications_type_check;
ALTER TABLE public.notifications ADD CONSTRAINT notifications_type_check CHECK (
  type IN ('created','updated','deleted','restored','completed','reopened','reminder','daily_summary',
    'note_created','note_important','task_created','task_assigned','note_updated',
    'note_comment','task_completed','task_reopened','task_reminder')
);

-- Separate outbox: event notification_queue requires a non-null event_id.
CREATE TABLE IF NOT EXISTS public.internal_note_notification_jobs (
  history_id uuid PRIMARY KEY REFERENCES public.internal_note_history(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  processed_at timestamptz
);
ALTER TABLE public.internal_note_notification_jobs ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.internal_note_notification_jobs FROM PUBLIC, anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.internal_note_notification_jobs TO service_role;
CREATE INDEX IF NOT EXISTS qp_note_notice_pending_idx
  ON public.internal_note_notification_jobs(created_at) WHERE processed_at IS NULL;
CREATE INDEX IF NOT EXISTS qp_task_reminder_due_idx
  ON public.internal_notes(due_at)
  WHERE kind='task' AND completed=false AND deleted_at IS NULL AND reminder_minutes IS NOT NULL;
CREATE INDEX IF NOT EXISTS qp_task_reminder_notice_idx
  ON public.notifications((details->>'note_id')) WHERE type='task_reminder' AND dismissed_at IS NULL;

CREATE OR REPLACE FUNCTION public.qp_note_reminder_validate()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
  -- Old clients may remove a due date or assignment without knowing the new fields.
  IF NEW.kind <> 'task' OR NEW.due_at IS NULL OR NEW.reminder_minutes IS NULL
     OR (NEW.reminder_recipient = 'assigned' AND NEW.assigned_device_id IS NULL) THEN
    NEW.reminder_minutes := NULL;
    NEW.reminder_recipient := NULL;
    NEW.reminder_device_id := NULL;
  ELSE
    IF NEW.reminder_minutes NOT IN (0,5,15,30,60,120,1440)
       OR NEW.reminder_recipient IS NULL
       OR NEW.reminder_recipient NOT IN ('all','assigned','me') THEN
      RAISE EXCEPTION 'Configuração de lembrete inválida';
    END IF;
    IF NEW.reminder_recipient = 'me' THEN
      IF NEW.reminder_device_id IS NULL OR NOT EXISTS (
        SELECT 1 FROM public.devices d WHERE d.id=NEW.reminder_device_id AND d.user_id=NEW.user_id
      ) THEN RAISE EXCEPTION 'Dispositivo do lembrete inválido'; END IF;
    ELSE NEW.reminder_device_id := NULL;
    END IF;
  END IF;
  IF TG_OP='INSERT' THEN
    NEW.reminder_revision := 0;
  ELSE
    NEW.reminder_revision := OLD.reminder_revision;
    IF ROW(NEW.kind,NEW.due_at,NEW.reminder_minutes,NEW.reminder_recipient,NEW.reminder_device_id,
           CASE WHEN NEW.reminder_recipient='assigned' THEN NEW.assigned_device_id END,NEW.completed,NEW.deleted_at)
       IS DISTINCT FROM
       ROW(OLD.kind,OLD.due_at,OLD.reminder_minutes,OLD.reminder_recipient,OLD.reminder_device_id,
           CASE WHEN OLD.reminder_recipient='assigned' THEN OLD.assigned_device_id END,OLD.completed,OLD.deleted_at) THEN
      NEW.reminder_revision := OLD.reminder_revision+1;
    END IF;
  END IF;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS qp_note_reminder_validate ON public.internal_notes;
CREATE TRIGGER qp_note_reminder_validate BEFORE INSERT OR UPDATE ON public.internal_notes
  FOR EACH ROW EXECUTE FUNCTION public.qp_note_reminder_validate();

CREATE OR REPLACE FUNCTION public.qp_enqueue_note_notice()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
  IF NEW.action IN ('created','updated','commented','completed','reopened')
    AND EXISTS (
      SELECT 1 FROM public.devices d JOIN public.notification_preferences p ON p.device_id=d.id
      WHERE d.user_id=NEW.user_id AND d.id<>NEW.device_id
        AND d.notifications_enabled AND p.notes_notifications_enabled
    ) THEN
    INSERT INTO public.internal_note_notification_jobs(history_id) VALUES(NEW.id)
    ON CONFLICT DO NOTHING;
  END IF;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS qp_enqueue_note_notice ON public.internal_note_history;
CREATE TRIGGER qp_enqueue_note_notice AFTER INSERT ON public.internal_note_history
  FOR EACH ROW EXECUTE FUNCTION public.qp_enqueue_note_notice();

-- Existing wake-up function is resilient to pg_net errors; cron remains a fallback.
DROP TRIGGER IF EXISTS qp_wake_note_worker ON public.internal_note_notification_jobs;
CREATE TRIGGER qp_wake_note_worker AFTER INSERT ON public.internal_note_notification_jobs
  FOR EACH STATEMENT EXECUTE FUNCTION public.qp_wake_notification_worker();

CREATE OR REPLACE FUNCTION public.generate_internal_note_notifications()
RETURNS integer LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
  h record;
  n public.internal_notes%ROWTYPE;
  snapshot jsonb;
  source_name text;
  assigned_name text;
  total integer := 0;
  affected integer;
BEGIN
  FOR h IN
    SELECT hist.* FROM public.internal_note_notification_jobs j
    JOIN public.internal_note_history hist ON hist.id=j.history_id
    WHERE j.processed_at IS NULL ORDER BY j.created_at,j.history_id
    LIMIT 100 FOR UPDATE OF j SKIP LOCKED
  LOOP
    SELECT * INTO n FROM public.internal_notes WHERE id=h.note_id AND user_id=h.user_id;
    IF FOUND AND n.deleted_at IS NULL THEN
      snapshot := CASE WHEN h.action='commented' THEN to_jsonb(n) ELSE COALESCE(h.new_data,to_jsonb(n)) END;
      SELECT name INTO source_name FROM public.devices WHERE id=h.device_id AND user_id=h.user_id;
      SELECT name INTO assigned_name FROM public.devices
        WHERE id::text=snapshot->>'assigned_device_id' AND user_id=h.user_id;
      WITH eligible AS (
        SELECT d.id AS device_id,
          CASE
            WHEN h.action='commented' AND p.notify_note_comments THEN 'note_comment'
            WHEN h.action='completed' AND p.notify_task_completed THEN 'task_completed'
            WHEN h.action='reopened' AND p.notify_task_reopened THEN 'task_reopened'
            WHEN h.action IN ('created','updated') AND snapshot->>'kind'='task'
              AND snapshot->>'assigned_device_id'=d.id::text AND p.notify_assigned_tasks
              AND (h.action='created' OR h.old_data->>'assigned_device_id' IS DISTINCT FROM snapshot->>'assigned_device_id')
              THEN 'task_assigned'
            WHEN h.action IN ('created','updated') AND snapshot->>'important'='true'
              AND p.notify_important_notes
              AND (h.action='created' OR h.old_data->>'important' IS DISTINCT FROM 'true')
              THEN 'note_important'
            WHEN h.action='created' AND snapshot->>'kind'='task' AND p.notify_tasks THEN 'task_created'
            WHEN h.action='created' AND snapshot->>'kind'='note' AND p.notify_notes THEN 'note_created'
            WHEN h.action='updated' AND p.notify_note_updates THEN 'note_updated'
            ELSE NULL
          END AS notice_type
        FROM public.devices d
        JOIN public.notification_preferences p ON p.device_id=d.id
        WHERE d.user_id=h.user_id AND d.id<>h.device_id
          AND d.notifications_enabled AND p.notes_notifications_enabled
      ), payload AS (
        SELECT e.*,
          CASE e.notice_type
            WHEN 'note_created' THEN 'Nova nota'
            WHEN 'note_important' THEN 'Nota importante'
            WHEN 'task_created' THEN 'Nova tarefa'
            WHEN 'task_assigned' THEN 'Tarefa atribuída a ti'
            WHEN 'note_updated' THEN CASE WHEN snapshot->>'kind'='task' THEN 'Tarefa editada' ELSE 'Nota editada' END
            WHEN 'note_comment' THEN 'Novo comentário'
            WHEN 'task_completed' THEN 'Tarefa concluída'
            WHEN 'task_reopened' THEN 'Tarefa reaberta'
          END AS label,
          CASE WHEN h.action='commented' THEN h.new_data->>'content' ELSE snapshot->>'content' END AS description
        FROM eligible e WHERE e.notice_type IS NOT NULL
      ), inserted AS (
        INSERT INTO public.notifications(device_id,event_id,type,title,body,deduplication_key,details)
        SELECT p.device_id,NULL,p.notice_type,p.label,
          concat_ws(E'\n',snapshot->>'title',NULLIF(left(p.description,650),''),CASE WHEN source_name IS NOT NULL THEN 'Por: '||source_name END),
          'internal-note:'||h.id::text,
          jsonb_build_object('note_id',h.note_id,'name',snapshot->>'title','kind',snapshot->>'kind',
            'notes',COALESCE(p.description,''),'source',source_name,'assigned',assigned_name,
            'important',snapshot->>'important',
            'date',to_char((snapshot->>'due_at')::timestamptz AT TIME ZONE 'Europe/Lisbon','DD/MM/YYYY'),
            'time',to_char((snapshot->>'due_at')::timestamptz AT TIME ZONE 'Europe/Lisbon','HH24:MI'))
        FROM payload p ON CONFLICT(device_id,deduplication_key) DO NOTHING
        RETURNING id,device_id
      )
      INSERT INTO public.notification_deliveries(notification_id,device_id)
        SELECT id,device_id FROM inserted ON CONFLICT(notification_id,device_id) DO NOTHING;
      GET DIAGNOSTICS affected=ROW_COUNT;
      total:=total+affected;
    END IF;
    UPDATE public.internal_note_notification_jobs SET processed_at=now() WHERE history_id=h.id;
  END LOOP;
  RETURN total;
END $$;

-- Central eligibility predicate is shared by generation and pre-send validation.
CREATE OR REPLACE FUNCTION public.qp_task_reminder_eligible(p_note_id uuid,p_device_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
 SELECT EXISTS (
   SELECT 1 FROM public.internal_notes n
   JOIN public.devices d ON d.id=p_device_id AND d.user_id=n.user_id
   JOIN public.notification_preferences p ON p.device_id=d.id
   WHERE n.id=p_note_id AND n.kind='task' AND NOT n.completed AND n.deleted_at IS NULL
     AND n.due_at IS NOT NULL AND n.reminder_minutes IS NOT NULL
     AND d.notifications_enabled AND p.notes_notifications_enabled AND p.task_reminders_enabled
     AND (n.reminder_recipient='all'
       OR (n.reminder_recipient='assigned' AND n.assigned_device_id=d.id)
       OR (n.reminder_recipient='me' AND n.reminder_device_id=d.id))
     AND now() >= n.due_at-make_interval(mins=>n.reminder_minutes)
     AND now() < CASE WHEN n.reminder_minutes=0 THEN n.due_at+interval '5 minutes' ELSE n.due_at END
 );
$$;

CREATE OR REPLACE FUNCTION public.generate_task_reminder_notifications()
RETURNS integer LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE generated integer;
BEGIN
  WITH inserted AS (
    INSERT INTO public.notifications(device_id,event_id,type,title,body,deduplication_key,details)
    SELECT d.id,NULL,'task_reminder','Lembrete de tarefa',
      concat_ws(E'\n',n.title,CASE WHEN n.reminder_minutes=0 THEN 'Prazo de conclusão atingido'
        ELSE 'Prazo: '||to_char(n.due_at AT TIME ZONE 'Europe/Lisbon','DD/MM/YYYY HH24:MI') END,
        NULLIF(left(n.content,500),'')),
      'task-reminder:'||n.id::text||':'||n.reminder_revision::text,
      jsonb_build_object('note_id',n.id,'name',n.title,'kind','task','notes',n.content,
        'important',n.important,'assigned',a.name,'reminder_minutes',n.reminder_minutes,
        'date',to_char(n.due_at AT TIME ZONE 'Europe/Lisbon','DD/MM/YYYY'),
        'time',to_char(n.due_at AT TIME ZONE 'Europe/Lisbon','HH24:MI'))
    FROM public.internal_notes n JOIN public.devices d ON d.user_id=n.user_id
    LEFT JOIN public.devices a ON a.id=n.assigned_device_id AND a.user_id=n.user_id
    WHERE n.kind='task' AND NOT n.completed AND n.deleted_at IS NULL
      AND n.reminder_minutes IS NOT NULL
      AND public.qp_task_reminder_eligible(n.id,d.id)
    ON CONFLICT(device_id,deduplication_key) DO NOTHING RETURNING id,device_id
  )
  INSERT INTO public.notification_deliveries(notification_id,device_id)
  SELECT id,device_id FROM inserted ON CONFLICT(notification_id,device_id) DO NOTHING;
  GET DIAGNOSTICS generated=ROW_COUNT;
  RETURN generated;
END $$;

CREATE OR REPLACE FUNCTION public.validate_task_reminder_notification(p_notification_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
 SELECT EXISTS (
   SELECT 1 FROM public.notifications x JOIN public.internal_notes n ON n.id::text=x.details->>'note_id'
   WHERE x.id=p_notification_id AND x.type='task_reminder' AND x.dismissed_at IS NULL
     AND x.deduplication_key='task-reminder:'||n.id::text||':'||n.reminder_revision::text
     AND public.qp_task_reminder_eligible(n.id,x.device_id)
 );
$$;

CREATE OR REPLACE FUNCTION public.qp_cancel_obsolete_task_reminders()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
  IF NEW.reminder_revision IS DISTINCT FROM OLD.reminder_revision THEN
    UPDATE public.notifications SET dismissed_at=now()
    WHERE type='task_reminder' AND dismissed_at IS NULL AND details->>'note_id'=NEW.id::text;
  END IF;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS qp_cancel_obsolete_task_reminders ON public.internal_notes;
CREATE TRIGGER qp_cancel_obsolete_task_reminders AFTER UPDATE ON public.internal_notes
  FOR EACH ROW EXECUTE FUNCTION public.qp_cancel_obsolete_task_reminders();

REVOKE ALL ON FUNCTION public.qp_note_reminder_validate() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.qp_enqueue_note_notice() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.qp_cancel_obsolete_task_reminders() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.generate_internal_note_notifications() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.generate_task_reminder_notifications() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.validate_task_reminder_notification(uuid) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.qp_task_reminder_eligible(uuid,uuid) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.generate_internal_note_notifications() TO service_role;
GRANT EXECUTE ON FUNCTION public.generate_task_reminder_notifications() TO service_role;
GRANT EXECUTE ON FUNCTION public.validate_task_reminder_notification(uuid) TO service_role;
GRANT EXECUTE ON FUNCTION public.qp_task_reminder_eligible(uuid,uuid) TO service_role;

COMMIT;
