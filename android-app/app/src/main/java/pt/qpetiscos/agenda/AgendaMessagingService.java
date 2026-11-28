package pt.qpetiscos.agenda;
import android.app.*;
import android.content.*;
import android.os.Build;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.view.View;
import android.widget.RemoteViews;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;
public class AgendaMessagingService extends FirebaseMessagingService {
 static String format(String body){return body==null?"":body.replace(" | ","\n");}
 static void channel(Context c){NotificationManager m=c.getSystemService(NotificationManager.class);m.createNotificationChannel(new NotificationChannel("qpetiscos_notifications","Agenda QPetiscos",NotificationManager.IMPORTANCE_HIGH));}
 static String val(Map<String,String> d,String key){String v=d.get(key);return v==null?"":v;}
 static int color(String s){return Color.parseColor(s);}
 static int summaryCount(Map<String,String> d,String key){try{return Math.max(0,Integer.parseInt(val(d,key)));}catch(NumberFormatException e){return 0;}}
 static String reservationLabel(int n){return n+(n==1?" reserva":" reservas");}
 static String takeawayLabel(int n){return n+(n==1?" takeaway":" takeaways");}
 static int[] summaryMeals(Map<String,String> d){
  String[] p=val(d,"notes").split(":");
  if(p.length!=3||!"qp-summary-v2".equals(p[0]))return null;
  try{int l=Integer.parseInt(p[1]),r=Integer.parseInt(p[2]);return l>=0&&r>=0&&(long)l+r==summaryCount(d,"reservations")?new int[]{l,r}:null;}catch(NumberFormatException e){return null;}
 }
 static String summaryText(Map<String,String> d,boolean compact){
  int total=summaryCount(d,"reservations"),take=summaryCount(d,"takeaways");int[] meals=summaryMeals(d);java.util.ArrayList<String> parts=new java.util.ArrayList<>();
  if(total>0){if(meals==null||compact&&meals[0]>0&&meals[1]>0)parts.add(reservationLabel(total));else{if(meals[0]>0)parts.add("Almoço: "+reservationLabel(meals[0]));if(meals[1]>0)parts.add("Jantar: "+reservationLabel(meals[1]));}}
  if(take>0)parts.add(takeawayLabel(take));return parts.isEmpty()?"Sem eventos agendados para hoje.":android.text.TextUtils.join(" · ",parts);
 }
 static String summaryDate(String raw){try{java.time.LocalDate date=java.time.LocalDate.parse(raw,java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"));return date.format(java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM",new java.util.Locale("pt","PT")));}catch(Exception e){return raw;}}
 RemoteViews summaryLayout(Map<String,String>d,boolean big,PendingIntent pi){
  RemoteViews r=new RemoteViews(getPackageName(),big?R.layout.push_summary_expanded:R.layout.push_summary_compact);
  int bg=getSharedPreferences("qp_ui",0).getInt("color",Color.WHITE);boolean light=bg==Color.WHITE,brown=bg==color("#402817"),tan=bg==color("#966039");
  int fg=light?color("#402817"):Color.WHITE,accent=light?color("#966039"):brown?color("#E5A363"):tan?color("#F5D5A9"):color("#C49A73");
  r.setInt(R.id.summary_root,"setBackgroundResource",light?R.drawable.push_shell_white:brown?R.drawable.push_shell_brown:tan?R.drawable.push_shell_tan:R.drawable.push_shell_black);
  r.setTextViewText(R.id.summary_title,val(d,"title"));r.setTextColor(R.id.summary_title,accent);r.setInt(R.id.summary_sun,"setColorFilter",accent);
  r.setTextViewText(R.id.summary_date,summaryDate(val(d,"date")));r.setTextColor(R.id.summary_date,fg);
  int total=summaryCount(d,"reservations"),take=summaryCount(d,"takeaways");int[] meals=summaryMeals(d);boolean both=meals!=null&&meals[0]>0&&meals[1]>0;
  if(!big){r.setTextViewText(R.id.summary_compact_text,summaryText(d,true));r.setTextColor(R.id.summary_compact_text,fg);}
  else{
   boolean showTotal=total>0&&(both||meals==null),showTakeCard=take>0&&(both||meals==null);
   int shell=light?R.drawable.summary_card_white:brown?R.drawable.summary_card_brown:tan?R.drawable.summary_card_tan:R.drawable.summary_card_black;
   for(int id:new int[]{R.id.summary_total_card,R.id.summary_take_card,R.id.summary_services,R.id.summary_empty})r.setInt(id,"setBackgroundResource",shell);
   r.setViewVisibility(R.id.summary_totals,showTotal||showTakeCard?View.VISIBLE:View.GONE);
   r.setViewVisibility(R.id.summary_total_card,showTotal?View.VISIBLE:View.GONE);r.setViewVisibility(R.id.summary_take_card,showTakeCard?View.VISIBLE:View.GONE);
   r.setTextViewText(R.id.summary_total_number,String.valueOf(total));r.setTextViewText(R.id.summary_take_number,String.valueOf(take));
   r.setTextViewText(R.id.summary_total_label,total==1?"Reserva":"Reservas");r.setTextViewText(R.id.summary_take_label,take==1?"Takeaway":"Takeaways");
   boolean lunch=meals!=null&&meals[0]>0,dinner=meals!=null&&meals[1]>0,takeRow=take>0&&!showTakeCard;
   r.setViewVisibility(R.id.summary_services,lunch||dinner||takeRow?View.VISIBLE:View.GONE);
   r.setViewVisibility(R.id.summary_lunch,lunch?View.VISIBLE:View.GONE);r.setViewVisibility(R.id.summary_dinner,dinner?View.VISIBLE:View.GONE);r.setViewVisibility(R.id.summary_take_row,takeRow?View.VISIBLE:View.GONE);
   r.setViewVisibility(R.id.summary_div_dinner,lunch&&dinner?View.VISIBLE:View.GONE);r.setViewVisibility(R.id.summary_div_take,(lunch||dinner)&&takeRow?View.VISIBLE:View.GONE);
   r.setTextViewText(R.id.summary_lunch_count,lunch?reservationLabel(meals[0]):"");r.setTextViewText(R.id.summary_dinner_count,dinner?reservationLabel(meals[1]):"");r.setTextViewText(R.id.summary_take_count,String.valueOf(take));
   r.setViewVisibility(R.id.summary_empty,total+take==0?View.VISIBLE:View.GONE);
   for(int id:new int[]{R.id.summary_total_number,R.id.summary_take_number,R.id.summary_total_label,R.id.summary_take_label,R.id.summary_lunch_label,R.id.summary_dinner_label,R.id.summary_take_row_label,R.id.summary_lunch_count,R.id.summary_dinner_count,R.id.summary_take_count,R.id.summary_empty_text})r.setTextColor(id,fg);
   for(int id:new int[]{R.id.summary_total_icon,R.id.summary_take_icon,R.id.summary_lunch_icon,R.id.summary_dinner_icon,R.id.summary_take_row_icon,R.id.summary_empty_icon})r.setInt(id,"setColorFilter",accent);
   for(int id:new int[]{R.id.summary_div_dinner,R.id.summary_div_take})r.setInt(id,"setBackgroundColor",light?color("#D8C8BA"):color("#92745E"));
  }
  r.setOnClickPendingIntent(R.id.summary_root,pi);return r;
 }
 static java.time.LocalDate noticeDate(String raw){try{return java.time.LocalDate.parse(raw,java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"));}catch(Exception e){try{return java.time.LocalDate.parse(raw);}catch(Exception ignored){return null;}}}
 static String relativeDate(String raw){java.time.LocalDate date=noticeDate(raw);if(date==null)return "";java.time.LocalDate today=java.time.LocalDate.now(java.time.ZoneId.of("Europe/Lisbon"));if(date.equals(today))return "Hoje";if(date.equals(today.plusDays(1)))return "Amanhã";String day=date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE",new java.util.Locale("pt","PT")));return day.substring(0,1).toUpperCase(new java.util.Locale("pt","PT"))+day.substring(1);}
 static String fullDate(String raw){java.time.LocalDate date=noticeDate(raw);if(date==null)return raw;java.time.LocalDate today=java.time.LocalDate.now(java.time.ZoneId.of("Europe/Lisbon"));return date.format(java.time.format.DateTimeFormatter.ofPattern(date.getYear()==today.getYear()?"d MMMM":"d MMMM uuuu",new java.util.Locale("pt","PT")));}
 static String mealLabel(String time){try{return java.time.LocalTime.parse(time).isBefore(java.time.LocalTime.of(15,0))?"Almoço":"Jantar";}catch(Exception e){return "";}}
 static String reminderText(Map<String,String>d,boolean task){String raw=val(d,"reminder_minutes");if(raw.isEmpty())return task?"Lembrete de prazo":"Lembrete de evento";try{int n=Integer.parseInt(raw);if(n==0)return task?"Prazo de conclusão":"Hora do evento";if(n>0)return (task?"Prazo · ":"Lembrete · ")+(n%60==0?n/60+(n==60?" hora":" horas"):n+(n==1?" minuto":" minutos"))+" antes";}catch(NumberFormatException ignored){}return task?"Lembrete de prazo":"Lembrete de evento";}
 static void text(RemoteViews r,int id,String v,int color){r.setTextViewText(id,v);r.setTextColor(id,color);}
 static void visible(RemoteViews r,int id,boolean yes){r.setViewVisibility(id,yes?View.VISIBLE:View.GONE);}
 RemoteViews layout(Map<String,String>d,boolean big,PendingIntent pi){
  if("daily_summary".equals(val(d,"type")))return summaryLayout(d,big,pi);
  RemoteViews r=new RemoteViews(getPackageName(),big?R.layout.push_expanded:R.layout.push_compact);
  int bg=getSharedPreferences("qp_ui",0).getInt("color",Color.WHITE);boolean light=bg==Color.WHITE,brown=bg==color("#402817"),tan=bg==color("#966039");
  int fg=light?color("#402817"):Color.WHITE,sub=light?color("#795E49"):color("#E9D9CA"),accent=light?color("#966039"):brown?color("#E5A363"):tan?color("#F5D5A9"):color("#C49A73");
  int shell=light?R.drawable.summary_card_white:brown?R.drawable.summary_card_brown:tan?R.drawable.summary_card_tan:R.drawable.summary_card_black;
  String type=val(d,"type"),kind=val(d,"kind");boolean internal=type.startsWith("note_")||type.startsWith("task_"),task=type.startsWith("task_")||"task".equals(kind),take="takeaway".equals(kind),reminder="reminder".equals(type)||"task_reminder".equals(type);
  int status=accent,actionIcon=take?R.drawable.ic_push_bag:R.drawable.ic_widget_calendar;
  if(internal)actionIcon=task?R.drawable.ic_widget_calendar:R.drawable.ic_push_note;
  switch(type){case "note_important":actionIcon=R.drawable.ic_widget_warning;status=light?color("#B54540"):color("#FFB4AB");break;case "note_comment":actionIcon=R.drawable.ic_push_note;break;case "task_assigned":actionIcon=R.drawable.ic_push_user;break;case "note_updated":case "updated":actionIcon=R.drawable.ic_push_edit;break;case "task_completed":case "completed":actionIcon=R.drawable.ic_push_check;status=light?color("#267A45"):color("#A2D8AD");break;case "task_reopened":case "restored":case "reopened":actionIcon=R.drawable.ic_push_undo;break;case "task_reminder":case "reminder":actionIcon=R.drawable.ic_push_alarm;break;case "deleted":actionIcon=R.drawable.ic_widget_trash;status=light?color("#B54540"):color("#FFB4AB");break;}
  r.setInt(R.id.push_root,"setBackgroundResource",light?R.drawable.push_shell_white:brown?R.drawable.push_shell_brown:tan?R.drawable.push_shell_tan:R.drawable.push_shell_black);
  r.setInt(R.id.push_stripe,"setColorFilter",status);r.setImageViewResource(R.id.push_icon,actionIcon);r.setInt(R.id.push_icon,"setColorFilter",status);
  text(r,R.id.push_title,val(d,"title"),status);text(r,R.id.push_name,val(d,"name"),fg);
  String date=val(d,"date"),time=val(d,"time"),relative=relativeDate(date),meal=internal?"":mealLabel(time);boolean hasDate=!date.isEmpty(),hasTime=!time.isEmpty();
  visible(r,R.id.push_facts,hasDate||hasTime);r.setInt(R.id.push_facts,"setBackgroundResource",shell);
  visible(r,R.id.push_date_column,hasDate);visible(r,R.id.push_relative,!relative.isEmpty()||task&&hasDate);text(r,R.id.push_relative,(task?"Prazo"+(relative.isEmpty()?"":" · "):"")+relative,accent);
  text(r,R.id.push_date,fullDate(date),fg);visible(r,R.id.push_time_column,hasTime);text(r,R.id.push_time,time,fg);
  visible(r,R.id.push_meal,big&&!meal.isEmpty());text(r,R.id.push_meal,meal,sub);
  boolean people=!internal&&!take;String count=val(d,"people");String countLabel=count.isEmpty()?"Pessoas por confirmar":count+("1".equals(count)?" pessoa":" pessoas");
  visible(r,R.id.push_people_row,people);text(r,R.id.push_people,countLabel,fg);r.setInt(R.id.push_people_icon,"setColorFilter",accent);
  String assigned=val(d,"assigned");visible(r,R.id.push_assigned_row,task&&!assigned.isEmpty());text(r,R.id.push_assigned,"Atribuída a "+assigned,fg);r.setInt(R.id.push_assigned_icon,"setColorFilter",accent);
  boolean important="note_important".equals(type);visible(r,R.id.push_reminder_row,reminder||important);text(r,R.id.push_reminder,important?"Requer confirmação de leitura":reminderText(d,task),important?status:fg);r.setImageViewResource(R.id.push_reminder_icon,important?R.drawable.ic_widget_warning:R.drawable.ic_push_alarm);r.setInt(R.id.push_reminder_icon,"setColorFilter",important?status:accent);
  if(big){String notes=val(d,"notes"),source=val(d,"source");visible(r,R.id.push_notes_row,!notes.trim().isEmpty());r.setInt(R.id.push_notes_row,"setBackgroundResource",shell);r.setInt(R.id.push_note_icon,"setColorFilter",accent);text(r,R.id.push_notes_label,"note_comment".equals(type)?"Comentário":internal?"Descrição":"Observações",sub);text(r,R.id.push_notes,notes,fg);
   visible(r,R.id.push_source_row,!source.isEmpty());text(r,R.id.push_source,"Por "+source,sub);r.setInt(R.id.push_source_icon,"setColorFilter",accent);}
  r.setOnClickPendingIntent(R.id.push_root,pi);return r;
 }
 @Override public void onMessageReceived(RemoteMessage msg){
  channel(this);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  Map<String,String>d=msg.getData();RemoteMessage.Notification n=msg.getNotification();String title=n!=null?n.getTitle():val(d,"title"),body=n!=null?n.getBody():val(d,"body");if(title==null||title.isEmpty())return;
  String id=d.get("notification_id");if(id==null)id=msg.getMessageId();if(id==null)return;
  android.content.SharedPreferences seen=getSharedPreferences("qp_push_seen",0);if(seen.contains(id))return;
  Intent i=new Intent(this,MainActivity.class).setData(android.net.Uri.parse("qpetiscos://notification/"+android.net.Uri.encode(id))).putExtra("qp_query","qp=notifications&notification_id="+android.net.Uri.encode(id)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  PendingIntent pi=PendingIntent.getActivity(this,id.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  String notificationType=val(d,"type");if("daily_summary".equals(notificationType)&&!val(d,"reservations").isEmpty())body=summaryText(d,false);String action=notificationType.startsWith("task_")?"Consultar tarefa":notificationType.startsWith("note_")?"Consultar nota":"daily_summary".equals(d.get("type"))?"Consultar agenda":"deleted".equals(d.get("type"))?"Ver notificações":"Consultar evento";
  Notification.Builder builder=new Notification.Builder(this,"qpetiscos_notifications").setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(format(body)).setStyle(new Notification.BigTextStyle().bigText(format(body))).setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(pi).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(Icon.createWithResource(this,R.drawable.ic_widget_calendar),action,pi).build());
  NotificationManager manager=getSystemService(NotificationManager.class);
  try{if(!val(d,"name").isEmpty()||("daily_summary".equals(d.get("type"))&&!val(d,"reservations").isEmpty())){builder.setStyle(new Notification.DecoratedCustomViewStyle()).setCustomContentView(layout(d,false,pi)).setCustomBigContentView(layout(d,true,pi));}manager.notify(id,0,builder.build());}catch(RuntimeException e){manager.notify(id,0,new Notification.Builder(this,"qpetiscos_notifications").setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(format(body)).setStyle(new Notification.BigTextStyle().bigText(format(body))).setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(pi).setOnlyAlertOnce(true).build());}
  android.content.SharedPreferences.Editor edit=seen.edit();long now=System.currentTimeMillis();for(Map.Entry<String,?> entry:seen.getAll().entrySet())if(entry.getValue() instanceof Long&&now-(Long)entry.getValue()>7L*86400000L)edit.remove(entry.getKey());edit.putLong(id,now).apply();WidgetAgenda.refreshAll(this);
 }
}
