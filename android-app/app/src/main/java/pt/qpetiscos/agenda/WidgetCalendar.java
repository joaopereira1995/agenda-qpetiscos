package pt.qpetiscos.agenda;
import android.app.*;import android.appwidget.*;import android.os.Bundle;import java.time.LocalDate;
public class WidgetCalendar extends Activity {
 @Override public void onCreate(Bundle b){super.onCreate(b);int id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);if(id<0){finish();return;}LocalDate d=WidgetAgenda.date(this,id);DatePickerDialog dialog=new DatePickerDialog(this,(view,y,m,day)->{getSharedPreferences("widget_state",0).edit().putString("date"+id,LocalDate.of(y,m+1,day).toString()).apply();WidgetAgenda.refreshAll(this);finish();},d.getYear(),d.getMonthValue()-1,d.getDayOfMonth());dialog.setOnCancelListener(x->finish());dialog.show();}
}