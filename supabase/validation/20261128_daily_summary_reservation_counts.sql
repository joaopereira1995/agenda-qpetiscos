-- Read-only verification; no notifications generated and no restaurant records accessed.
SELECT position('lunch_reservations' in pg_get_functiondef('public.qp_generate_daily_summary_v069()'::regprocedure)) > 0 AS counts_present,
       position('qp-summary-v2:' in pg_get_functiondef('public.qp_generate_daily_summary_v069()'::regprocedure)) > 0 AS transport_present;
WITH sample(type,event_time,people_count) AS (VALUES ('reserva',time '12:00',20),('reserva',time '15:00',3),('reserva',time '19:00',8),('takeaway',time '12:30',NULL::integer))
SELECT count(*) FILTER (WHERE type='reserva')=3 AS total_ok,
       count(*) FILTER (WHERE type='reserva' AND event_time<time '15:00')=1 AS lunch_ok,
       count(*) FILTER (WHERE type='reserva' AND event_time>=time '15:00')=2 AS dinner_ok,
       count(*) FILTER (WHERE type='takeaway')=1 AS takeaway_ok FROM sample;
