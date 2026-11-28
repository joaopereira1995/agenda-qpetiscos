-- UI-009: additive summary fields; preserve legacy people counts and body.
-- Existing worker forwards details.notes to protocol 2 clients; summary-only versioned transport.
-- Do not run against PROD automatically. Verify function and schema before applying.
CREATE OR REPLACE FUNCTION public.qp_generate_daily_summary_v069()
 RETURNS integer
 LANGUAGE plpgsql
 SET search_path TO ''
AS $function$
DECLARE
  d record;
  p public.notification_preferences%ROWTYPE;
  local_now timestamp := now() AT TIME ZONE 'Europe/Lisbon';
  day_value date := local_now::date;
  schedule_at timestamp;
  schedule_time time;
  revision_value bigint;
  key_value text;
  notice_id uuid;
  reservations bigint;
  takeaways bigint;
  people bigint;
  lunch bigint;
  dinner bigint;
  lunch_reservations bigint;
  dinner_reservations bigint;
  generated integer := 0;
  body_value text;
BEGIN
  FOR d IN SELECT id,user_id FROM public.devices WHERE notifications_enabled IS TRUE ORDER BY id LOOP
    -- Serialize generation with existing preference-row updates.
    SELECT * INTO p FROM public.notification_preferences WHERE device_id=d.id FOR UPDATE;
    IF NOT FOUND THEN
      schedule_time := time '09:00'; revision_value := 0;
    ELSE
      IF NOT COALESCE(p.daily_summary_enabled,true) THEN CONTINUE; END IF;
      schedule_time := COALESCE(p.daily_summary_time,time '09:00');
      revision_value := COALESCE(p.summary_schedule_revision,0);
    END IF;
    schedule_at := day_value + schedule_time;
    IF local_now < schedule_at OR local_now >= schedule_at + interval '30 minutes' THEN CONTINUE; END IF;
    key_value := 'daily_summary:'||day_value::text||CASE WHEN revision_value=0 THEN '' ELSE ':r'||revision_value::text END;
    -- Legacy rows may predate the new schedule metadata. Preserve their deduplication.
    IF revision_value=0 AND EXISTS (SELECT 1 FROM public.notifications n WHERE n.device_id=d.id AND n.type='daily_summary' AND (n.created_at AT TIME ZONE 'Europe/Lisbon')::date=day_value) THEN CONTINUE; END IF;
    SELECT count(*) FILTER (WHERE type='reserva'),count(*) FILTER (WHERE type='takeaway'),
      COALESCE(sum(people_count) FILTER (WHERE type='reserva'),0),
      COALESCE(sum(people_count) FILTER (WHERE type='reserva' AND event_time < time '15:00'),0),
      COALESCE(sum(people_count) FILTER (WHERE type='reserva' AND event_time >= time '15:00'),0),
      count(*) FILTER (WHERE type='reserva' AND event_time < time '15:00'),
      count(*) FILTER (WHERE type='reserva' AND event_time >= time '15:00')
    INTO reservations,takeaways,people,lunch,dinner,lunch_reservations,dinner_reservations
    FROM public.events WHERE user_id=d.user_id AND event_date=day_value AND deleted_at IS NULL AND completed IS FALSE;
    IF reservations+takeaways=0 AND NOT COALESCE(p.daily_summary_empty,false) THEN CONTINUE; END IF;
    body_value := concat_ws(E'\n',to_char(day_value,'DD/MM/YYYY'),
      'Reservas: '||reservations,'Takeaways: '||takeaways,'Pessoas registadas: '||people,
      'Almoço: '||lunch||' pessoas','Jantar: '||dinner||' pessoas','Total de eventos: '||(reservations+takeaways));
    notice_id := NULL;
    INSERT INTO public.notifications(device_id,event_id,type,title,body,deduplication_key,details)
    VALUES(d.id,NULL,'daily_summary',CASE WHEN schedule_time < time '12:00' THEN 'Bom dia! A tua agenda de hoje' ELSE 'A tua agenda de hoje' END,body_value,key_value,
      jsonb_build_object('date',to_char(day_value,'DD/MM/YYYY'),'reservations',reservations,'takeaways',takeaways,'people',people,'lunch',lunch,'dinner',dinner,'lunch_reservations',lunch_reservations,'dinner_reservations',dinner_reservations,
        'notes','qp-summary-v2:'||lunch_reservations||':'||dinner_reservations,'schedule_time',left(schedule_time::text,5),'schedule_revision',revision_value))
    ON CONFLICT(device_id,deduplication_key) DO NOTHING RETURNING id INTO notice_id;
    IF notice_id IS NOT NULL THEN
      INSERT INTO public.notification_deliveries(notification_id,device_id) VALUES(notice_id,d.id) ON CONFLICT(notification_id,device_id) DO NOTHING;
      generated := generated+1;
    END IF;
  END LOOP;
  RETURN generated;
END; $function$;
