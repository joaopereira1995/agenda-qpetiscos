package pt.qpetiscos.agenda;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Independent widget session: never rotates the WebView refresh token. */
final class WidgetAuth {
    private static final String ALIAS="qp_widget_auth_v1";
    private static final String AUTH=BuildConfig.QP_SUPABASE_URL+"/auth/v1/token?grant_type=";
    static final class HttpError extends IOException {
        final int code; final boolean auth;
        HttpError(int code){this(code,false);}
        HttpError(int code,boolean auth){super("HTTP "+code);this.code=code;this.auth=auth;}
    }
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("widget_auth_secure",0);}
    private static javax.crypto.SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(!ks.containsAlias(ALIAS)){
            KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            gen.generateKey();
        }
        return (javax.crypto.SecretKey)ks.getKey(ALIAS,null);
    }
    private static synchronized void save(Context c,JSONObject s)throws Exception{
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        String data=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(s.toString().getBytes("UTF-8")),Base64.NO_WRAP);
        if(!prefs(c).edit().putString("session",data).commit())throw new IOException("Não foi possível guardar a sessão");
    }
    private static synchronized JSONObject load(Context c)throws Exception{
        String data=prefs(c).getString("session",null);if(data==null)return null;
        String[] parts=data.split(":",2);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
        return new JSONObject(new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),"UTF-8"));
    }
    static synchronized void clear(Context c){prefs(c).edit().clear().commit();}
    static JSONObject appSession(Context c)throws Exception{
        String raw=c.getSharedPreferences("widget_session",0).getString("session","");
        if(raw.isEmpty())throw new IOException("SESSION_REQUIRED");
        JSONObject s=new JSONObject(raw);
        if(s.optString("uid").isEmpty()||s.isNull("device")||s.optString("device").isEmpty()||s.optString("token").isEmpty())throw new IOException("SESSION_REQUIRED");
        return s;
    }
    static String account(Context c){try{return appSession(c).getString("uid");}catch(Exception e){return "";}}
    static boolean configured(Context c){try{JSONObject s=load(c);return s!=null&&s.optString("uid").equals(account(c));}catch(Exception e){return false;}}
    static JSONObject tokenRequest(Context c,String grant,JSONObject body)throws Exception{
        HttpURLConnection h=WidgetAgenda.openConnection(c,new URL(AUTH+grant));h.setConnectTimeout(10000);h.setReadTimeout(10000);h.setRequestMethod("POST");h.setInstanceFollowRedirects(false);h.setDoOutput(true);h.setRequestProperty("apikey",WidgetAgenda.KEY);h.setRequestProperty("Content-Type","application/json");
        try{try(OutputStream out=h.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));}int code=h.getResponseCode();if(code<200||code>=300)throw new HttpError(code,true);
            try(InputStream in=h.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new JSONObject(out.toString("UTF-8"));}
        }finally{h.disconnect();}
    }
    private static JSONObject normalize(JSONObject response,String uid)throws Exception{
        if(!uid.equals(response.getJSONObject("user").getString("id")))throw new IOException("ACCOUNT_MISMATCH");
        String access=response.getString("access_token"),refresh=response.getString("refresh_token");
        if(access.isEmpty()||refresh.isEmpty())throw new IOException("SESSION_REQUIRED");
        return new JSONObject().put("uid",uid).put("token",access).put("refresh",refresh).put("expires",response.optLong("expires_at",System.currentTimeMillis()/1000+response.optLong("expires_in",3600)));
    }
    // Called only by the widget executor; password is not stored.
    static void connect(Context c,String email,String password)throws Exception{
        String uid=appSession(c).getString("uid");JSONObject s=normalize(tokenRequest(c,"password",new JSONObject().put("email",email).put("password",password)),uid);
        if(!uid.equals(account(c)))throw new IOException("SESSION_REQUIRED");save(c,s);
    }
    static JSONObject session(Context c,boolean force)throws Exception{
        JSONObject app=appSession(c),s=load(c);String uid=app.getString("uid");
        if(s==null||!uid.equals(s.optString("uid")))return app;
        if(force||s.optLong("expires")<=System.currentTimeMillis()/1000+90){
            try{s=normalize(tokenRequest(c,"refresh_token",new JSONObject().put("refresh_token",s.getString("refresh"))),uid);}
            catch(HttpError e){if(e.code==400||e.code==401||e.code==403)clear(c);throw e;}
            if(!uid.equals(account(c)))throw new IOException("SESSION_REQUIRED");save(c,s);
        }
        return new JSONObject().put("uid",uid).put("device",app.getString("device")).put("token",s.getString("token"));
    }
}
