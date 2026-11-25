package pt.qpetiscos.agenda;
import android.app.*;import android.os.*;import android.content.*;import android.net.Uri;import android.webkit.*;import android.widget.*;import android.view.*;import org.json.*;import java.net.*;import java.io.*;
public class MainActivity extends Activity {
 static final String SITE="https://joaopereira1995.github.io/agenda-qpetiscos/";
 String lastWidgetSave=""; WebView web; FrameLayout root; ImageView splash;
 @Override public void onCreate(Bundle b){super.onCreate(b);AgendaMessagingService.channel(this);root=new FrameLayout(this);web=new WebView(this);root.addView(web,new FrameLayout.LayoutParams(-1,-1));splash=new ImageView(this);splash.setBackgroundColor(-1);splash.setImageResource(R.drawable.splash_logo);splash.setScaleType(ImageView.ScaleType.CENTER_INSIDE);root.addView(splash,new FrameLayout.LayoutParams(-1,-1));setContentView(root);
 web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);android.webkit.CookieManager.getInstance().setAcceptCookie(true);
 web.addJavascriptInterface(new Object(){@JavascriptInterface public void ready(String color){runOnUiThread(()->{try{int bg=android.graphics.Color.parseColor(color);web.setBackgroundColor(bg);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);}catch(Exception ignored){}splash.setVisibility(View.GONE);});} @JavascriptInterface public void requestToken(){runOnUiThread(()->sendFirebaseToken());} @JavascriptInterface public void requestPermission(){runOnUiThread(()->{if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},71);});} @JavascriptInterface public void checkUpdates(){runOnUiThread(()->{if(web.getUrl()!=null&&web.getUrl().startsWith(SITE))checkUpdate(true);});}},"QPetiscosNative");web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();if("https".equals(u.getScheme())&&"joaopereira1995.github.io".equals(u.getHost())&&u.getPath()!=null&&u.getPath().startsWith("/agenda-qpetiscos/"))return false;if("https".equals(u.getScheme()))startActivity(new Intent(Intent.ACTION_VIEW,u));return true;}@Override public void onPageFinished(WebView v,String u){syncWidgetSession();} });web.loadUrl(widgetUrl(getIntent()));new Handler(Looper.getMainLooper()).postDelayed(()->splash.setVisibility(View.GONE),15000);checkUpdate(); WidgetAgenda.schedule(this); handler.post(sync); }
 String widgetUrl(Intent i){String q=notificationQuery(i);return q==null?SITE:SITE+"?"+q;}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);String q=notificationQuery(i);if(q!=null&&web.getUrl()!=null&&web.getUrl().startsWith(SITE)){web.evaluateJavascript("if(window.qpOpen&&window.qpReady){window.qpOpen("+JSONObject.quote(q)+");true}else{false}",r->{if(!"true".equals(r))web.loadUrl(widgetUrl(i));});}else web.loadUrl(widgetUrl(i));}
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable sync=new Runnable(){public void run(){syncWidgetSession();handler.postDelayed(this,15000);}};
 String notificationQuery(Intent i){String q=i.getStringExtra("qp_query");if(q!=null)return q;if(i.hasExtra("notification_id"))return "qp=notifications&notification_id="+Uri.encode(i.getStringExtra("notification_id"));return null;}
 void sendFirebaseToken(){if(web==null||web.getUrl()==null||!web.getUrl().startsWith(SITE))return;com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token->{if(web!=null&&web.getUrl()!=null&&web.getUrl().startsWith(SITE))web.evaluateJavascript("if(window.qpFirebaseToken)window.qpFirebaseToken("+JSONObject.quote(token)+")",null);});}
 void syncWidgetSession(){if(web==null||web.getUrl()==null||!web.getUrl().startsWith(SITE))return;
 web.evaluateJavascript("(()=>{let v=document.getElementById('appVersion');if(v)v.textContent='v"+BuildConfig.VERSION_NAME+"';try{let s=JSON.parse(localStorage.getItem('sb-kyzwmaydwokbfmiskhcg-auth-token')||'null');return s&&s.user?JSON.stringify({token:s.access_token,uid:s.user.id,device:localStorage.getItem('qp-device-'+s.user.id),revision:window.qpWidgetRevision||0,widgetSave:window.qpWidgetSave||\"\"}):''}catch(e){return ''}})()", result->{try{Object decoded=new JSONTokener(result).nextValue();String value=decoded instanceof String?(String)decoded:"";if(!value.isEmpty()){String saved=new JSONObject(value).optString("widgetSave","");if(!saved.isEmpty()&&!saved.equals(lastWidgetSave)){lastWidgetSave=saved;WidgetAgenda.resetToday(this,getIntent().getIntExtra("qp_wid",-1));}}android.content.SharedPreferences p=getSharedPreferences("widget_session",0);if(!value.equals(p.getString("session",""))){p.edit().putString("session",value).apply();WidgetAgenda.refreshAll(this);}}catch(Exception ignored){}});}
 @Override protected void onPause(){if(web!=null)web.evaluateJavascript("if(window.qpClosePopups)window.qpClosePopups()",null);syncWidgetSession();super.onPause();}
 @Override protected void onResume(){super.onResume();if(web!=null){syncWidgetSession();sendFirebaseToken();}}
 @Override protected void onDestroy(){handler.removeCallbacks(sync);super.onDestroy();}
 private boolean checking, downloading;
 void checkUpdate(){checkUpdate(false);}
 void checkUpdate(boolean manual){
 if(checking || downloading)return; checking=true;
 new Thread(()->{try{
 HttpURLConnection c=(HttpURLConnection)new URL("https://api.github.com/repos/joaopereira1995/agenda-qpetiscos/releases/latest").openConnection();
 c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setRequestProperty("Accept","application/vnd.github+json");
 JSONObject j;try{if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}j=new JSONObject(out.toString("UTF-8"));}finally{c.disconnect();}
 String tag=j.getString("tag_name");if(!newer(tag,BuildConfig.VERSION_NAME)){if(manual)runOnUiThread(()->Toast.makeText(this,"Aplicação atualizada",Toast.LENGTH_SHORT).show());return;}
 JSONArray assets=j.getJSONArray("assets");for(int i=0;i<assets.length();i++){
 JSONObject x=assets.getJSONObject(i);if(!x.getString("name").endsWith(".apk"))continue;
 String url=x.getString("browser_download_url");if(!url.startsWith("https://github.com/joaopereira1995/agenda-qpetiscos/releases/download/"))continue;
 String notes=j.optString("body", "");
 runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setTitle("Atualização disponível").setMessage("Versão instalada: "+BuildConfig.VERSION_NAME+"\nNova versão: "+tag+"\n\n"+notes).setNegativeButton("Mais tarde",null).setPositiveButton("Atualizar",(d,w)->downloadUpdate(url)).show();});break;
 }
 }catch(Exception e){android.util.Log.w("QPetiscos","Verificação de atualização",e);if(manual)runOnUiThread(()->Toast.makeText(this,"Não foi possível verificar atualizações",Toast.LENGTH_LONG).show());}finally{runOnUiThread(()->checking=false);}}).start();}
 private void downloadUpdate(String url){
 if(downloading)return;downloading=true;
 ProgressDialog progress=new ProgressDialog(this);progress.setMessage("A descarregar atualização…");progress.setCancelable(false);progress.show();
 new Thread(()->{try{
 File dir=new File(getCacheDir(),"updates");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Pasta indisponível");
 File apk=new File(dir,"update.apk");
 HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);
 try{if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(apk)){byte[] buf=new byte[16384];int n;long total=0;while((n=in.read(buf))!=-1){total+=n;if(total>100L*1024*1024)throw new IOException("APK demasiado grande");out.write(buf,0,n);}}}finally{c.disconnect();}
 android.content.pm.PackageInfo info=getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(),0);
 if(info==null||!getPackageName().equals(info.packageName)||info.versionCode<=BuildConfig.VERSION_CODE)throw new IOException("APK inválido");
 runOnUiThread(()->{downloading=false;if(isFinishing()||isDestroyed())return;progress.dismiss();installUpdate();});
 }catch(Exception e){runOnUiThread(()->{downloading=false;if(isFinishing()||isDestroyed())return;progress.dismiss();new AlertDialog.Builder(this).setTitle("Atualização").setMessage("Não foi possível descarregar a atualização. Tenta novamente mais tarde.").setPositiveButton("OK",null).show();});}}).start();}
 private void installUpdate(){
 try{if(!getPackageManager().canRequestPackageInstalls()){
 new AlertDialog.Builder(this).setTitle("Permitir instalação").setMessage("Autoriza a instalação de aplicações desta origem e regressa à agenda.").setNegativeButton("Cancelar",null).setPositiveButton("Definições",(d,w)->startActivityForResult(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())),42)).show();return;}
 Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(Uri.parse("content://"+getPackageName()+".updates/update.apk"),"application/vnd.android.package-archive");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);
 }catch(Exception e){Toast.makeText(this,"Não foi possível iniciar a instalação.",Toast.LENGTH_LONG).show();}}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==42&&getPackageManager().canRequestPackageInstalls())installUpdate();}
 static boolean newer(String a,String b){String[] x=a.replaceFirst("^v","").split("\\."),y=b.split("\\.");for(int i=0;i<Math.max(x.length,y.length);i++){int n=i<x.length?Integer.parseInt(x[i]):0,m=i<y.length?Integer.parseInt(y[i]):0;if(n!=m)return n>m;}return false;}
 @Override public void onBackPressed(){if(web==null){super.onBackPressed();return;}web.evaluateJavascript("window.qpBack?window.qpBack():false",r->{if(!"true".equals(r))goBackNormally();});} private void goBackNormally(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}