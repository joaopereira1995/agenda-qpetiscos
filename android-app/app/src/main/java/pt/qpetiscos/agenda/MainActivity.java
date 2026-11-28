package pt.qpetiscos.agenda;
import android.app.*;import android.os.*;import android.content.*;import android.net.Uri;import android.webkit.*;import android.widget.*;import android.view.*;import org.json.*;import java.net.*;import java.io.*;
public class MainActivity extends Activity {
 void setBetaEditing(boolean enabled){if(!BuildConfig.QP_BETA)return;getSharedPreferences("qp_ui",0).edit().putBoolean("beta_editing",enabled).apply();if(web!=null)web.evaluateJavascript("if(window.qpBetaModeRefresh)window.qpBetaModeRefresh()",null);WidgetAgenda.refreshAll(this);}
 static final String SITE="https://joaopereira1995.github.io/agenda-qpetiscos/";
 long widgetEpoch=0; String lastWidgetSave=""; WebView web; FrameLayout root; ImageView splash; int uiColor=android.graphics.Color.WHITE; Dialog updateDialog;
 @Override public void onCreate(Bundle b){super.onCreate(b);AgendaMessagingService.channel(this);root=new FrameLayout(this);web=new WebView(this);root.addView(web,new FrameLayout.LayoutParams(-1,-1));splash=new ImageView(this);splash.setBackgroundColor(-1);splash.setImageResource(R.drawable.splash_logo);splash.setScaleType(ImageView.ScaleType.CENTER_INSIDE);root.addView(splash,new FrameLayout.LayoutParams(-1,-1));if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);setContentView(root);root.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());android.graphics.Insets ime=insets.getInsets(android.view.WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom));return android.view.WindowInsets.CONSUMED;}return insets;});root.requestApplyInsets();applySystemTheme(getSharedPreferences("qp_ui",0).getInt("color",android.graphics.Color.WHITE));
 if(getSharedPreferences("qp_ui",0).getInt("web_cache_version",0)!=BuildConfig.VERSION_CODE){web.clearCache(true);getSharedPreferences("qp_ui",0).edit().putInt("web_cache_version",BuildConfig.VERSION_CODE).apply();}web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);android.webkit.CookieManager.getInstance().setAcceptCookie(true);
 web.addJavascriptInterface(new Object(){@JavascriptInterface public String appMetadata(){org.json.JSONObject j=new org.json.JSONObject();try{j.put("build",BuildConfig.VERSION_CODE);j.put("sha",BuildConfig.QP_BUILD_SHA);j.put("date",BuildConfig.QP_BUILD_DATE);j.put("model",android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL);j.put("android",android.os.Build.VERSION.RELEASE);}catch(org.json.JSONException ignored){}return j.toString();}
 @JavascriptInterface public String testUpdateMetadata(){return getSharedPreferences(BuildConfig.QP_BETA?"qp_test_updates":"qp_prod_updates",0).getString("metadata","{}");}
 @JavascriptInterface public void copyAppDiagnostic(String value){runOnUiThread(()->{android.content.ClipboardManager c=getSystemService(android.content.ClipboardManager.class);c.setPrimaryClip(android.content.ClipData.newPlainText("Diagnóstico",value));});}
 @JavascriptInterface public boolean isBeta(){return BuildConfig.QP_BETA;}
 @JavascriptInterface public void exportReport(String name,String mime,String data,boolean share){runOnUiThread(()->{try{if(!name.matches("QPetiscos-[0-9-]+\\.pdf")||!"application/pdf".equals(mime)||data.length()>24000000)throw new java.io.IOException("Invalid report");java.io.File dir=new java.io.File(getCacheDir(),"reports");dir.mkdirs();java.io.File file=new java.io.File(dir,name);try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){out.write(android.util.Base64.decode(data,android.util.Base64.DEFAULT));}android.net.Uri uri=android.net.Uri.parse("content://"+getPackageName()+".reports/"+name);android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_SEND);intent.setType(mime);intent.putExtra(android.content.Intent.EXTRA_STREAM,uri);intent.setClipData(android.content.ClipData.newRawUri(name,uri));intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(android.content.Intent.createChooser(intent,share?"Partilhar relatório":"Guardar ou abrir relatório"));}catch(Exception e){android.widget.Toast.makeText(MainActivity.this,"Não foi possível exportar o relatório",android.widget.Toast.LENGTH_LONG).show();}});}
 @JavascriptInterface public void previewReport(String name,String data){runOnUiThread(()->{try{if(!name.matches("QPetiscos-[0-9-]+\\.pdf")||data.length()>24000000)throw new IOException("Invalid PDF");java.io.File dir=new java.io.File(getCacheDir(),"reports");dir.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,name))){out.write(android.util.Base64.decode(data,android.util.Base64.DEFAULT));}Uri uri=Uri.parse("content://"+getPackageName()+".reports/"+name);Intent intent=new Intent(Intent.ACTION_VIEW);intent.setDataAndType(uri,"application/pdf");intent.setClipData(ClipData.newRawUri(name,uri));intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(intent);}catch(ActivityNotFoundException e){Toast.makeText(MainActivity.this,"Não existe um leitor PDF instalado. Utiliza Exportar PDF.",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(MainActivity.this,"Não foi possível pré-visualizar o PDF",Toast.LENGTH_LONG).show();}});}
 @JavascriptInterface public boolean betaEditing(){return BuildConfig.QP_BETA&&!WidgetAgenda.readOnly(MainActivity.this);}
 @JavascriptInterface public void requestBetaEditing(boolean enabled){runOnUiThread(()->{if(!BuildConfig.QP_BETA)return;if(!enabled){setBetaEditing(false);return;}new AlertDialog.Builder(MainActivity.this).setTitle("Ativar edição TEST?").setMessage("As alterações são exclusivas do ambiente TEST.").setNegativeButton("Cancelar",(d,w)->setBetaEditing(false)).setPositiveButton("Ativar edição",(d,w)->setBetaEditing(true)).show();});}
 @JavascriptInterface public String widgetDiagnostics(){return WidgetAgenda.diagnostics(MainActivity.this);}
 @JavascriptInterface public void widgetTest(){WidgetAgenda.runAsync(()->{try{WidgetAgenda.updateAll(MainActivity.this);}finally{runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())web.evaluateJavascript("if(window.qpWidgetDiagRefresh)window.qpWidgetDiagRefresh()",null);});}});}
 @JavascriptInterface public void widgetCopy(String text){runOnUiThread(()->{android.content.ClipboardManager clipboard=getSystemService(android.content.ClipboardManager.class);clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Diagnóstico dos widgets",WidgetAgenda.diagnostics(MainActivity.this)));});}
 @JavascriptInterface public void widgetConnect(){runOnUiThread(()->showWidgetLogin());}
 @JavascriptInterface public void widgetClear(){runOnUiThread(()->{widgetEpoch++;setBetaEditing(false);WidgetAgenda.clearSession(MainActivity.this);});}
 @JavascriptInterface public void ready(String color){runOnUiThread(()->{try{int bg=android.graphics.Color.parseColor(color);applySystemTheme(bg);}catch(Exception ignored){}splash.setVisibility(View.GONE);tryShowTestUpdate();});} @JavascriptInterface public void setTheme(String color){runOnUiThread(()->{try{applySystemTheme(android.graphics.Color.parseColor(color));}catch(Exception ignored){}});} @JavascriptInterface public void requestToken(){runOnUiThread(()->sendFirebaseToken());} @JavascriptInterface public void requestPermission(){runOnUiThread(()->{if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},71);});} @JavascriptInterface public void checkUpdates(){runOnUiThread(()->{if(web.getUrl()!=null&&web.getUrl().startsWith(SITE))checkUpdate(true);});}},"QPetiscosNative");web.setWebViewClient(new WebViewClient(){
 @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
  Uri u=r.getUrl();if(BuildConfig.QP_BETA&&"kyzwmaydwokbfmiskhcg.supabase.co".equals(u.getHost()))return new WebResourceResponse("text/plain","UTF-8",403,"Production blocked",java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
  if(!"https".equals(u.getScheme())||!"joaopereira1995.github.io".equals(u.getHost()))return null;
  String path=u.getPath(),prefix="/agenda-qpetiscos/";if(path==null||!path.startsWith(prefix))return null;
  String local=path.substring(prefix.length());if(local.isEmpty())local="index.html";
  if(!(BuildConfig.QP_BETA&&(local.equals("core/release.js")||local.equals("core/build-info.js")))&&!local.equals("index.html")&&!local.equals("assets/vendor/plotly.min.js")&&!local.equals("assets/vendor/qp-icons.js")&&!local.equals("core/icons.js")&&!local.matches("assets/[A-Za-z0-9._-]+")&&!local.equals("core/app.js")&&!local.equals("core/bootstrap.js")&&!local.equals("mobile/mobile.js")&&!local.equals("shared/base.css")&&!local.equals("shared/themes.css")&&!local.equals("core/view.js")&&!local.equals("core/reports.js")&&!local.equals("core/service.js"))return null;
  String mime=local.endsWith(".html")?"text/html":local.endsWith(".png")?"image/png":local.endsWith(".js")?"application/javascript":local.endsWith(".css")?"text/css":"application/octet-stream";
  try{return new WebResourceResponse(mime,(local.endsWith(".html")||local.endsWith(".js")||local.endsWith(".css"))?"UTF-8":null,getAssets().open("qp-web/"+local));}
  catch(IOException e){return new WebResourceResponse("text/plain","UTF-8",503,"Bundle unavailable",java.util.Collections.emptyMap(),new ByteArrayInputStream("Interface indisponível. Reinstala o APK correto.".getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
 }
 @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();if("https".equals(u.getScheme())&&"joaopereira1995.github.io".equals(u.getHost())&&u.getPath()!=null&&u.getPath().startsWith("/agenda-qpetiscos/"))return false;if("https".equals(u.getScheme()))startActivity(new Intent(Intent.ACTION_VIEW,u));return true;}@Override public void onPageFinished(WebView v,String u){syncWidgetSession();syncSystemTheme();} });web.loadUrl(widgetUrl(getIntent()));new Handler(Looper.getMainLooper()).postDelayed(()->splash.setVisibility(View.GONE),15000); WidgetAgenda.schedule(this); handler.post(sync); }
 void applySystemTheme(int bg){uiColor=bg;getSharedPreferences("qp_ui",0).edit().putInt("color",bg).apply();if(Build.VERSION.SDK_INT>=29){getWindow().setStatusBarContrastEnforced(false);getWindow().setNavigationBarContrastEnforced(false);}root.setBackgroundColor(bg);web.setBackgroundColor(bg);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);boolean light=(0.2126*android.graphics.Color.red(bg)+0.7152*android.graphics.Color.green(bg)+0.0722*android.graphics.Color.blue(bg))>150;if(Build.VERSION.SDK_INT>=30){android.view.WindowInsetsController c=getWindow().getInsetsController();if(c!=null){int mask=android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;c.setSystemBarsAppearance(light?mask:0,mask);}}else{getWindow().getDecorView().setSystemUiVisibility(light?(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR):0);}}
 void syncSystemTheme(){if(web!=null&&web.getUrl()!=null&&web.getUrl().startsWith(SITE))web.evaluateJavascript("(()=>{if(window.QPetiscosNative){const c=getComputedStyle(document.documentElement).getPropertyValue('--bg').trim();if(/^#[0-9a-f]{3,8}$/i.test(c)){window.QPetiscosNative.setTheme(c)}}})()",null);}
 String widgetUrl(Intent i){String q=notificationQuery(i);return SITE+"?v="+BuildConfig.VERSION_CODE+(q==null?"":"&"+q);}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);String q=notificationQuery(i);if(q!=null&&web.getUrl()!=null&&web.getUrl().startsWith(SITE)){web.evaluateJavascript("if(window.qpOpen&&window.qpReady){window.qpOpen("+JSONObject.quote(q)+");true}else{false}",r->{if(!"true".equals(r))web.loadUrl(widgetUrl(i));});}else web.loadUrl(widgetUrl(i));}
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable sync=new Runnable(){public void run(){syncWidgetSession();handler.postDelayed(this,15000);}};
 String notificationQuery(Intent i){String q=i.getStringExtra("qp_query");if(q!=null)return q;if(i.hasExtra("notification_id"))return "qp=notifications&notification_id="+Uri.encode(i.getStringExtra("notification_id"));return null;}
 void sendFirebaseToken(){if(web==null||web.getUrl()==null||!web.getUrl().startsWith(SITE))return;com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token->{if(web!=null&&web.getUrl()!=null&&web.getUrl().startsWith(SITE))web.evaluateJavascript("if(window.qpFirebaseToken)window.qpFirebaseToken("+JSONObject.quote(token)+",2)",null);});}
 void syncWidgetSession(){if(web==null||web.getUrl()==null||!web.getUrl().startsWith(SITE))return;final long epoch=widgetEpoch;
 web.evaluateJavascript("(()=>{let v=document.getElementById('appVersion');if(window.qpBetaModeRefresh&&window.QPetiscosNative?.isBeta?.())window.qpBetaModeRefresh();else if(v)v.textContent='v"+BuildConfig.VERSION_NAME+"';try{let s=JSON.parse(localStorage.getItem('sb-"+(BuildConfig.QP_BETA?"ooctwbyhlpifmldcbvzi":"kyzwmaydwokbfmiskhcg")+"-auth-token')||'null');return s&&s.user?JSON.stringify({token:s.access_token,uid:s.user.id,device:localStorage.getItem('qp-device-'+s.user.id),revision:window.qpWidgetRevision||0,widgetSave:window.qpWidgetSave||\"\"}):''}catch(e){return ''}})()", result->{if(epoch!=widgetEpoch)return;try{Object decoded=new JSONTokener(result).nextValue();String value=decoded instanceof String?(String)decoded:"";if(!value.isEmpty()){String saved=new JSONObject(value).optString("widgetSave","");if(!saved.isEmpty()&&!saved.equals(lastWidgetSave)){lastWidgetSave=saved;WidgetAgenda.resetToday(this,getIntent().getIntExtra("qp_wid",-1));}}android.content.SharedPreferences p=getSharedPreferences("widget_session",0);if(!value.isEmpty()){JSONObject incoming=new JSONObject(value);if(!incoming.optString("token").isEmpty()&&!incoming.isNull("device")&&!incoming.optString("device").isEmpty()&&!incoming.optString("uid").isEmpty()&&!value.equals(p.getString("session",""))){String oldUid=WidgetAuth.account(this);if(!oldUid.isEmpty()&&!oldUid.equals(incoming.optString("uid")))WidgetAgenda.clearSession(this);p.edit().putString("session",value).apply();WidgetAgenda.refreshAll(this);}}}catch(Exception ignored){}});}
 void showWidgetLogin(){
  if(WidgetAuth.account(this).isEmpty()){Toast.makeText(this,"Inicia sessão na agenda primeiro",Toast.LENGTH_LONG).show();return;}
  LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(dip(20),dip(8),dip(20),0);
  EditText email=new EditText(this),password=new EditText(this);email.setHint("Email da conta da agenda");email.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);password.setHint("Palavra-passe");password.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);fields.addView(email);fields.addView(password);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Sessão autónoma dos widgets").setMessage("Utiliza a mesma conta da agenda. A palavra-passe não é guardada. Confirma que a conta permite várias sessões em simultâneo.").setView(fields).setNegativeButton("Cancelar",null).setPositiveButton("Ligar",null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String e=email.getText().toString().trim(),p=password.getText().toString();if(e.isEmpty()||p.isEmpty())return;password.setText("");dialog.dismiss();Toast.makeText(this,"A configurar sessão…",Toast.LENGTH_SHORT).show();WidgetAgenda.runAsync(()->{String message;try{WidgetAuth.connect(this,e,p);message="Sessão autónoma configurada";}catch(Exception ex){message="Não foi possível ligar: "+WidgetAgenda.issue(ex);}final String result=message;runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){Toast.makeText(this,result,Toast.LENGTH_LONG).show();web.evaluateJavascript("if(window.qpWidgetDiagRefresh)window.qpWidgetDiagRefresh()",null);}});WidgetAgenda.refreshAll(this);});}));dialog.show();
 }
 @Override protected void onPause(){testForeground=false;handler.removeCallbacks(testUpdatePoll);if(updateDialog!=null&&updateDialog.isShowing())updateDialog.dismiss();if(web!=null)web.evaluateJavascript("if(window.qpSuspendPopups)window.qpSuspendPopups()",null);syncWidgetSession();super.onPause();}
 @Override protected void onResume(){super.onResume();if(web!=null){syncWidgetSession();syncSystemTheme();sendFirebaseToken();}testForeground=true;testPrompted=false;checkUpdate(false);handler.removeCallbacks(testUpdatePoll);handler.postDelayed(testUpdatePoll,2000);}
 @Override protected void onDestroy(){handler.removeCallbacks(sync);handler.removeCallbacks(testUpdatePoll);testForeground=false;super.onDestroy();}

 private boolean testForeground,testPrompted,testChecking,testShowing;
 private long testLastCheck;
 private TestUpdate testPending;
 private static final String TEST_RELEASES="https://api.github.com/repos/joaopereira1995/agenda-qpetiscos/releases?per_page=100&page=";
 private static final class TestUpdate{
  final int build;final String version,notes,url;
  TestUpdate(int b,String v,String n,String u){build=b;version=v;notes=n;url=u;}
 }
 private final Runnable testUpdatePoll=new Runnable(){public void run(){
  if(!testForeground||isFinishing()||isDestroyed())return;
  if(android.os.SystemClock.elapsedRealtime()-testLastCheck>=900000L)checkUpdate(false);
  tryShowTestUpdate();handler.postDelayed(this,2000);
 }};
 private void checkTestUpdate(boolean manual){
  if(testChecking||downloading)return;
  if(manual)testPrompted=false;
  testChecking=true;testLastCheck=android.os.SystemClock.elapsedRealtime();
  final long attempt=System.currentTimeMillis();saveTestUpdateMetadata(attempt,"checking",null);
  new Thread(()->{
   TestUpdate best=null;String failure=null;JSONArray history=new JSONArray();
   try{
    for(int page=1;page<=10;page++){
     HttpURLConnection c=(HttpURLConnection)new URL(TEST_RELEASES+page).openConnection();
     c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("User-Agent","Agenda-QPetiscos-TEST");c.setUseCaches(false);
     JSONArray releases;
     try{
      if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
      ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>10000000)throw new IOException("Resposta demasiado grande");out.write(buf,0,n);}}
      releases=new JSONArray(out.toString("UTF-8"));
     }finally{c.disconnect();}
     for(int i=0;i<releases.length();i++){
      JSONObject release=releases.getJSONObject(i);
      if(release.optBoolean("draft")||release.optBoolean("prerelease")!=BuildConfig.QP_BETA)continue;
      String tag=release.optString("tag_name");
      java.util.regex.Matcher t=java.util.regex.Pattern.compile("android-test-b([0-9]+)-([0-9a-f]{7})").matcher(tag);
      if(BuildConfig.QP_BETA?!t.matches():!tag.matches("v[0-9]+\\.[0-9]+\\.[0-9]+"))continue;
      int tagBuild=0;if(BuildConfig.QP_BETA){try{tagBuild=Integer.parseInt(t.group(1));}catch(NumberFormatException ex){continue;}if(tagBuild<=0)continue;}
      JSONArray assets=release.optJSONArray("assets");if(assets==null)continue;
      for(int k=0;k<assets.length();k++){
       JSONObject a=assets.getJSONObject(k);String name=a.optString("name");
       String version;int build;
       if(BuildConfig.QP_BETA){
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("Agenda-QPetiscos-TEST-v([0-9]+\\.[0-9]+\\.[0-9]+)-b"+tagBuild+"-"+t.group(2)+"\\.apk").matcher(name);
        if(!m.matches())continue;version=m.group(1);build=tagBuild;
       }else{
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("Agenda-QPetiscos-"+tag.replace(".","\\.")+"(?:-b([0-9]+))?\\.apk").matcher(name);
        if(!m.matches())continue;version=tag.substring(1);build=0;
        if(m.group(1)!=null){try{build=Integer.parseInt(m.group(1));}catch(NumberFormatException ex){continue;}if(build<=0)continue;}
       }
       String url=a.optString("browser_download_url");
       if(!a.optString("state").equals("uploaded")||a.optLong("size")<=0||!url.equals("https://github.com/joaopereira1995/agenda-qpetiscos/releases/download/"+tag+"/"+name))continue;
       JSONObject entry=new JSONObject();entry.put("build",build);entry.put("version",version);entry.put("notes",release.optString("body",""));entry.put("published",release.optString("published_at",""));history.put(entry);
       boolean eligible=BuildConfig.QP_BETA?build>BuildConfig.VERSION_CODE:build>BuildConfig.VERSION_CODE&&newer(version,BuildConfig.VERSION_NAME);
       if(eligible&&(best==null||build>best.build))best=new TestUpdate(build,version,release.optString("body",""),url);break;
      }
     }
     if(releases.length()<100)break;
    }
   }catch(Exception e){failure=e.toString();android.util.Log.w("QPetiscos","Verificação TEST",e);}
   final TestUpdate found=best;final String error=failure;
   runOnUiThread(()->{
    testChecking=false;saveTestUpdateMetadata(attempt,error==null?(found==null?"current":"available"):"error",error==null?history:null);if(isFinishing()||isDestroyed())return;
    if(error==null){testPending=found;if(manual&&found==null)Toast.makeText(this,BuildConfig.QP_BETA?"Aplicação TEST atualizada":"Aplicação atualizada",Toast.LENGTH_SHORT).show();}
    else if(manual)Toast.makeText(this,"Não foi possível verificar atualizações",Toast.LENGTH_LONG).show();
    tryShowTestUpdate();
   });
  }).start();
 }
 private void saveTestUpdateMetadata(long last,String status,JSONArray history){
  try{android.content.SharedPreferences prefs=getSharedPreferences(BuildConfig.QP_BETA?"qp_test_updates":"qp_prod_updates",0);JSONObject data=new JSONObject(prefs.getString("metadata","{}"));data.put("last",last);data.put("status",status);if(history!=null)data.put("history",history);prefs.edit().putString("metadata",data.toString()).apply();
   if(web!=null&&!isFinishing()&&!isDestroyed())web.evaluateJavascript("if(window.qpTestUpdatesRefresh)window.qpTestUpdatesRefresh()",null);
  }catch(Exception e){android.util.Log.w("QPetiscos","Metadados TEST",e);}
 }
 private void tryShowTestUpdate(){
  if(!testForeground||testPrompted||testShowing||testPending==null||downloading||web==null||isFinishing()||isDestroyed()||!hasWindowFocus()||(updateDialog!=null&&updateDialog.isShowing()))return;
  testShowing=true;
  web.evaluateJavascript("Boolean(window.qpReady && !document.hidden && !document.querySelector('dialog[open]') && !document.querySelector('main').hidden && !document.querySelector('form:focus-within'))",result->{
   testShowing=false;
   if(!"true".equals(result)||!testForeground||testPrompted||testPending==null||downloading||isFinishing()||isDestroyed()||!hasWindowFocus())return;
   TestUpdate u=testPending;testPrompted=true;
   showUpdateDialog(BuildConfig.QP_BETA?u.version+" · build "+u.build:u.version,u.notes,u.url);
  });
 }

 private boolean checking, downloading;
 void checkUpdate(){checkUpdate(false);}
 void checkUpdate(boolean manual){
 checkTestUpdate(manual);}
 int dip(int n){return Math.round(n*getResources().getDisplayMetrics().density);} android.graphics.drawable.GradientDrawable rounded(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dip(radius));return d;} TextView updateText(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);return t;}
 void showUpdateDialog(String tag,String notes,String url){if(isFinishing()||isDestroyed()||(updateDialog!=null&&updateDialog.isShowing()))return;int bg=uiColor,fg=(android.graphics.Color.red(bg)*.2126+android.graphics.Color.green(bg)*.7152+android.graphics.Color.blue(bg)*.0722)>150?0xff402817:0xffffffff;int accent=fg==0xff402817?0xff966039:0xffe5a363;int panel=fg==0xff402817?0xfff4f0ea:bg==0xff402817?0xff5a3d29:bg==0xff966039?0xff80502e:0xff242424;Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);updateDialog=d;LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(dip(20),dip(20),dip(20),dip(16));l.setBackground(rounded(bg,20));TextView title=updateText("Nova atualização",22,fg,true);title.setGravity(Gravity.CENTER);l.addView(title);TextView version=updateText(BuildConfig.QP_BETA?"Instalada: "+BuildConfig.VERSION_NAME+" · build "+BuildConfig.VERSION_CODE+"\nDisponível: "+tag:"Versão "+tag+" disponível",14,fg,false);version.setGravity(Gravity.CENTER);version.setPadding(0,dip(8),0,dip(16));l.addView(version);if(notes!=null&&!notes.trim().isEmpty()){LinearLayout info=new LinearLayout(this);info.setOrientation(1);info.setPadding(dip(14),dip(12),dip(14),dip(12));info.setBackground(rounded(panel,12));info.addView(updateText("O que há de novo?",16,fg,true));ScrollView sc=new ScrollView(this);TextView body=updateText(notes,14,fg,false);body.setPadding(0,dip(8),0,0);sc.addView(body);int max=Math.min(dip(220),getResources().getDisplayMetrics().heightPixels/3);info.addView(sc,new LinearLayout.LayoutParams(-1,notes.length()>220?max:-2));l.addView(info);}
 Button update=new Button(this);update.setAllCaps(false);update.setText("Atualizar agora");update.setTextColor(fg==0xff402817?0xffffffff:0xff402817);update.setBackground(rounded(accent,12));LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(-1,dip(48));up.topMargin=dip(16);l.addView(update,up);update.setOnClickListener(v->{d.dismiss();downloadUpdate(url);});Button later=new Button(this);later.setAllCaps(false);later.setText("Mais tarde");later.setTextColor(fg);later.setBackgroundColor(android.graphics.Color.TRANSPARENT);l.addView(later,new LinearLayout.LayoutParams(-1,dip(44)));later.setOnClickListener(v->d.dismiss());d.setContentView(l);d.setCanceledOnTouchOutside(true);d.show();if(d.getWindow()!=null){d.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));d.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dip(32),dip(440)),-2);}}
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