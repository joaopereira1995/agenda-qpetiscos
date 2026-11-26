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
 RemoteViews layout(Map<String,String>d,boolean big,PendingIntent pi){
  RemoteViews r=new RemoteViews(getPackageName(),big?R.layout.push_expanded:R.layout.push_compact);
  int bg=getSharedPreferences("qp_ui",0).getInt("color",Color.WHITE);
  boolean light=bg==Color.WHITE, brown=bg==color("#402817"), tan=bg==color("#966039");
  int panel=light?color("#F3EFE9"):brown?color("#5A3D29"):tan?color("#80502E"):color("#1C1C1C");
  int fg=light?color("#402817"):Color.WHITE,sub=light?color("#795E49"):color("#E9D9CA"),accent=light?color("#966039"):brown?color("#E5A363"):tan?color("#F5D5A9"):color("#C49A73");
  boolean take="takeaway".equals(val(d,"kind"));int stripe=take?(light?color("#966039"):tan?color("#F5D5A9"):color("#E5A363")):(light||tan?color("#402817"):color("#F5E5D2"));
  r.setInt(R.id.push_root,"setBackgroundResource",light?R.drawable.push_shell_white:brown?R.drawable.push_shell_brown:tan?R.drawable.push_shell_tan:R.drawable.push_shell_black);r.setInt(R.id.push_stripe,"setColorFilter",stripe);
  String type=val(d,"type");boolean note=type.startsWith("note_")||type.startsWith("task_");if(note){stripe="note_important".equals(type)?color("#D08070"):"task_reminder".equals(type)?color("#D6AD68"):"task_completed".equals(type)?color("#79AC87"):"task".equals(val(d,"kind"))?accent:color("#7497A6");r.setInt(R.id.push_stripe,"setColorFilter",stripe);}int actionIcon=take?R.drawable.ic_push_bag:R.drawable.ic_widget_calendar;
  switch(type){case "note_created":case "note_important":case "note_comment":actionIcon=R.drawable.ic_push_note;break;case "task_created":case "task_assigned":actionIcon=R.drawable.ic_widget_calendar;break;case "note_updated":actionIcon=R.drawable.ic_push_edit;break;case "task_completed":actionIcon=R.drawable.ic_push_check;break;case "task_reopened":actionIcon=R.drawable.ic_push_undo;break;case "task_reminder":case "reminder":actionIcon=R.drawable.ic_push_alarm;break;case "updated":actionIcon=R.drawable.ic_push_edit;break;case "deleted":actionIcon=R.drawable.ic_widget_trash;break;case "completed":actionIcon=R.drawable.ic_push_check;break;case "restored":case "reopened":actionIcon=R.drawable.ic_push_undo;break;}
  r.setImageViewResource(R.id.push_icon,actionIcon);r.setTextViewText(R.id.push_title,val(d,"title"));r.setTextViewText(R.id.push_name,val(d,"name"));r.setTextViewText(R.id.push_date,val(d,"date"));r.setTextViewText(R.id.push_time,val(d,"time"));r.setTextViewText(R.id.push_people,val(d,"people").isEmpty()?"Por confirmar":val(d,"people"));r.setViewVisibility(R.id.push_people_row,take?View.GONE:View.VISIBLE);
  for(int id:new int[]{R.id.push_icon,R.id.push_date_icon,R.id.push_time_icon,R.id.push_people_icon})r.setInt(id,"setColorFilter",accent);
  for(int id:new int[]{R.id.push_name,R.id.push_date,R.id.push_time,R.id.push_people})r.setTextColor(id,fg);r.setTextColor(R.id.push_title,accent);
  boolean summary="daily_summary".equals(type);
  if(summary){r.setImageViewResource(R.id.push_icon,R.drawable.ic_widget_sun);r.setImageViewResource(R.id.push_time_icon,R.drawable.ic_push_bag);r.setTextViewText(R.id.push_name,val(d,"date"));r.setTextViewText(R.id.push_date,val(d,"reservations"));r.setTextViewText(R.id.push_time,val(d,"takeaways"));r.setTextViewText(R.id.push_people,val(d,"people"));r.setViewVisibility(R.id.push_people_row,View.VISIBLE);}
  if(!big){r.setViewVisibility(R.id.push_facts,View.GONE);r.setTextViewText(R.id.push_name,summary?val(d,"reservations")+" res. · "+val(d,"takeaways")+" takeaway · "+val(d,"people")+" pessoas":val(d,"name")+" · "+val(d,"time")+(take||val(d,"people").isEmpty()?"":" · "+val(d,"people")+" p."));}
  if(big){String notes=summary?"Reservas: "+val(d,"reservations")+" · Takeaways: "+val(d,"takeaways")+"\nAlmoço: "+val(d,"lunch")+" · Jantar: "+val(d,"dinner")+" pessoas":val(d,"notes"),source=val(d,"source");r.setViewVisibility(R.id.push_notes_row,notes.isEmpty()?View.GONE:View.VISIBLE);r.setTextViewText(R.id.push_notes,notes);r.setTextColor(R.id.push_notes,sub);r.setInt(R.id.push_notes_row,"setBackgroundColor",bg);r.setInt(R.id.push_note_icon,"setColorFilter",accent);r.setViewVisibility(R.id.push_source_row,source.isEmpty()?View.GONE:View.VISIBLE);r.setTextViewText(R.id.push_source,source);r.setTextColor(R.id.push_source,sub);r.setInt(R.id.push_source_icon,"setColorFilter",accent);}
  if(note){r.setTextViewText(R.id.push_name,val(d,"name"));r.setTextColor(R.id.push_title,light?accent:stripe);r.setInt(R.id.push_icon,"setColorFilter",light?accent:stripe);r.setViewVisibility(R.id.push_people_row,View.GONE);if(big){boolean hasDate=!val(d,"date").isEmpty(),hasTime=!val(d,"time").isEmpty();r.setViewVisibility(R.id.push_date_row,hasDate?View.VISIBLE:View.GONE);r.setViewVisibility(R.id.push_time_row,hasTime?View.VISIBLE:View.GONE);r.setViewVisibility(R.id.push_facts,hasDate||hasTime?View.VISIBLE:View.GONE);String assigned=val(d,"assigned"),source=val(d,"source");String meta=(assigned.isEmpty()?"":"Atribuída a: "+assigned)+(source.isEmpty()?"":(assigned.isEmpty()?"":" · ")+"Por: "+source);r.setTextViewText(R.id.push_source,meta);r.setViewVisibility(R.id.push_source_row,meta.isEmpty()?View.GONE:View.VISIBLE);}}
  r.setOnClickPendingIntent(R.id.push_root,pi);return r;
 }
 @Override public void onMessageReceived(RemoteMessage msg){
  channel(this);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  Map<String,String>d=msg.getData();RemoteMessage.Notification n=msg.getNotification();String title=n!=null?n.getTitle():val(d,"title"),body=n!=null?n.getBody():val(d,"body");if(title==null||title.isEmpty())return;
  String id=d.get("notification_id");if(id==null)id=msg.getMessageId();if(id==null)return;
  android.content.SharedPreferences seen=getSharedPreferences("qp_push_seen",0);if(seen.contains(id))return;
  Intent i=new Intent(this,MainActivity.class).setData(android.net.Uri.parse("qpetiscos://notification/"+android.net.Uri.encode(id))).putExtra("qp_query","qp=notifications&notification_id="+android.net.Uri.encode(id)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  PendingIntent pi=PendingIntent.getActivity(this,id.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  String notificationType=val(d,"type");String action=notificationType.startsWith("task_")?"Consultar tarefa":notificationType.startsWith("note_")?"Consultar nota":"daily_summary".equals(d.get("type"))?"Consultar agenda":"deleted".equals(d.get("type"))?"Ver notificações":"Consultar evento";
  Notification.Builder builder=new Notification.Builder(this,"qpetiscos_notifications").setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(format(body)).setStyle(new Notification.BigTextStyle().bigText(format(body))).setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(pi).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(Icon.createWithResource(this,R.drawable.ic_widget_calendar),action,pi).build());
  NotificationManager manager=getSystemService(NotificationManager.class);
  try{if(!val(d,"name").isEmpty()||("daily_summary".equals(d.get("type"))&&!val(d,"reservations").isEmpty())){builder.setStyle(new Notification.DecoratedCustomViewStyle()).setCustomContentView(layout(d,false,pi)).setCustomBigContentView(layout(d,true,pi));}manager.notify(id,0,builder.build());}catch(RuntimeException e){manager.notify(id,0,new Notification.Builder(this,"qpetiscos_notifications").setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(format(body)).setStyle(new Notification.BigTextStyle().bigText(format(body))).setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(pi).setOnlyAlertOnce(true).build());}
  android.content.SharedPreferences.Editor edit=seen.edit();long now=System.currentTimeMillis();for(Map.Entry<String,?> entry:seen.getAll().entrySet())if(entry.getValue() instanceof Long&&now-(Long)entry.getValue()>7L*86400000L)edit.remove(entry.getKey());edit.putLong(id,now).apply();WidgetAgenda.refreshAll(this);
 }
}
