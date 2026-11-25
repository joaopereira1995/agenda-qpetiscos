package pt.qpetiscos.agenda;
import android.app.*;
import android.content.*;
import android.os.Build;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
public class AgendaMessagingService extends FirebaseMessagingService {
 static void channel(Context c){NotificationManager m=c.getSystemService(NotificationManager.class);m.createNotificationChannel(new NotificationChannel("qpetiscos_notifications","Agenda QPetiscos",NotificationManager.IMPORTANCE_HIGH));}
 @Override public void onMessageReceived(RemoteMessage msg){
  channel(this);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  RemoteMessage.Notification n=msg.getNotification();if(n==null)return;
  String id=msg.getData().get("notification_id");if(id==null)id=msg.getMessageId();if(id==null)return;
  Intent i=new Intent(this,MainActivity.class).putExtra("qp_query","qp=notifications").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  PendingIntent pi=PendingIntent.getActivity(this,id.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification notice=new Notification.Builder(this,"qpetiscos_notifications").setSmallIcon(R.drawable.ic_notification).setContentTitle(n.getTitle()).setContentText(n.getBody()).setStyle(new Notification.BigTextStyle().bigText(n.getBody())).setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(pi).build();
  getSystemService(NotificationManager.class).notify(id,0,notice);WidgetAgenda.refreshAll(this);
 }
}
