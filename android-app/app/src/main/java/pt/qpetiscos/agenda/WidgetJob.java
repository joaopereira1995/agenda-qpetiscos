package pt.qpetiscos.agenda;
import android.app.job.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
public class WidgetJob extends JobService {
 private final Set<JobParameters> active=ConcurrentHashMap.newKeySet();
 public boolean onStartJob(JobParameters p){active.add(p);WidgetAgenda.runAsync(()->{if(!active.contains(p))return;try{WidgetAgenda.updateAll(getApplicationContext());}finally{if(active.remove(p))jobFinished(p,false);}});return true;}
 public boolean onStopJob(JobParameters p){active.remove(p);return true;}
}
