package pt.qpetiscos.agenda;
import android.app.*;
import android.content.*;
import android.os.Build;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
public class AgendaMessagingService extends FirebaseMessagingService {
 static String format(String body){return body==null?"":body.replace(" | ","\n");}
 static void channel(Context c){NotificationManager m=c.getSystemService(NotificationManager.class);m.createNotificationChannel(new NotificationChannel("qpetiscos_notifications","Agenda QPetiscos",NotificationManager.IMPORTANCE_HIGH));}
 @Override public void onMessageReceived(RemoteMessage msg){
  channel(this);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  RemoteMessage.Notification n=msg.getNotification();String title=n!=null?n.getTitle():msg.getData().get("title"),body=n!=null?n.getBody():msg.getData().get("body");if(title==null||title.isEmpty())return;
  String id=msg.getData().get("notification_id");if(id==null)id=msg.getMessageId();if(id==null)return;
  Intent i=new Intent(this,MainActivity.class).putExtra("qp_query","qp=notifications&notification_id="+android.net.Uri.encode(id)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  PendingIntent pi=PendingIntent.getActivity(this,id.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  String action="daily_summary".equals(msg.getData().get("type"))?"Consultar agenda":"deleted".equals(msg.getData().get("type"))?"Ver notificações":"Consultar evento";Notification notice=new Notification.Builder(this,"qpetiscos_notifications").setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(format(body)).setStyle(new Notification.BigTextStyle().bigText(format(body))).setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(pi).addAction(new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this,R.drawable.ic_widget_calendar),action,pi).build()).build();
  getSystemService(NotificationManager.class).notify(id,0,notice);WidgetAgenda.refreshAll(this);
 }
}
