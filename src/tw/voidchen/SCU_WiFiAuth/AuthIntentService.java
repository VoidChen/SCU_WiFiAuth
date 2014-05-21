/*
 * SCU WiFi Auth (tw.voidchen.SCU_WiFiAuth)
 * Copyright (C) 2014 VoidChen <void.scu@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package tw.voidchen.SCU_WiFiAuth;

import android.app.IntentService;
import android.content.Intent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.lang.Thread;
import java.lang.InterruptedException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.content.Context;

public class AuthIntentService extends IntentService{

    private static final String SETTING_DATA = "PrefSetData";
    private String errmsg = "";

    public AuthIntentService(){
        super("AuthIntentService");
    }

    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return super.onStartCommand(intent, flags, startId);
    }

    private void ShowNotify(String title, String msg, boolean rewrite, boolean redirect){
        SharedPreferences SetData = getSharedPreferences(SETTING_DATA, 0);
        if(SetData.getBoolean("ShowNotify", false)){
            //get counter
            int counter;
            if(rewrite)
                counter = 0;
            else{
                counter = SetData.getInt("notify_counter", 0);
                SharedPreferences.Editor SetDataEditor = SetData.edit();
                SetDataEditor.putInt("notify_counter", counter + 1);
                SetDataEditor.apply();
            }

            //set intent
            Intent NotifyIntent;
            if(redirect)
                NotifyIntent = new Intent(getApplicationContext(), MainActivity.class);
            else
                NotifyIntent = new Intent();
            PendingIntent StartIntent = PendingIntent.getActivity(getApplicationContext(), counter, NotifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            //create notify
            Notification notify = new Notification(R.drawable.icon_notify, title, System.currentTimeMillis());
            notify.flags |= Notification.FLAG_AUTO_CANCEL;
            notify.setLatestEventInfo(getApplicationContext(), title, msg, StartIntent);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.notify(counter, notify);
        }
    }

    private String readStream(InputStream res){
        String body = "";
        int temp;
        try{
            for(temp = res.read(); temp != -1; temp = res.read())
                body += (char) temp;
        }
        catch (IOException err){
        }
        return body;
    }

    private String WriteReq(String req, String key, String value){
        if(req != "")
            req += "&";
        return req + URLEncoder.encode(key) + "=" + URLEncoder.encode(value); 
    }

    private Integer AuthRequest(String site){
        try{
            URL url = new URL(site);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setFollowRedirects(false);
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(3000);

            //set CustomHostnameVerifier
            HostnameVerifier CustomHostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    if(hostname.equals("wlangw-city.scu.edu.tw") || hostname.equals("wlangw-main.scu.edu.tw"))
                        return true;
                    else{
                        HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                        return hv.verify(hostname, session);
                    }
                }
            };
            conn.setHostnameVerifier(CustomHostnameVerifier);

            //set request data
            OutputStream reqbody = conn.getOutputStream();

            SharedPreferences SetData = getSharedPreferences(SETTING_DATA, 0);
            String reqdata = "";
            reqdata = WriteReq(reqdata, "user", SetData.getString("username", "NULL"));
            reqdata = WriteReq(reqdata, "password", SetData.getString("password", "NULL"));
            reqdata = WriteReq(reqdata, "Login", "");

            reqbody.write(reqdata.getBytes());
            reqbody.flush();
            reqbody.close();

            //connect
            conn.connect();

            //get response body
            InputStream res = conn.getInputStream();
            String body = readStream(res);

            //auth result
            Integer code = conn.getResponseCode();
            Integer result = 0;
            if(code.equals(200))
                result = R.string.NotifyAuthSuccess;
            else if(code.equals(302)){
                String location = conn.getHeaderField("Location");
                if(location != null){
                    if(location.contains("?errmsg=Access denied"))
                        result = R.string.NotifyAuthDeny;
                    else if(location.contains("?errmsg=Authentication failed"))
                        result = R.string.NotifyAuthFail;
                    else{
                        errmsg = "HTTP Status Code: " + code.toString() + " Location: " + location;
                        result = R.string.NotifyAuthOther;
                    }
                }
                else{
                    errmsg = "HTTP Status Code: " + code.toString();
                    result = R.string.NotifyAuthOther;
                }
            }
            else{
                errmsg = "HTTP Status Code: " + code.toString();
                result = R.string.NotifyAuthOther;
            }

            conn.disconnect();
            return result;
        }
        catch (IOException err){
            errmsg = err.getMessage();
            return R.string.NotifyAuthError;
        }
    }

    private void StartAuth(int retry){
        Integer result = R.string.NotifyAuthNotActive;
        SharedPreferences SetData = getSharedPreferences(SETTING_DATA, 0);
        SharedPreferences.Editor SetDataEditor = SetData.edit();
        boolean flag = SetData.getBoolean("CampusFlag", true);
        int counter;
        for(counter = 1;counter <= retry; ++counter){
            //set url
            if(((counter&1) == 1) == flag)
                result = AuthRequest("https://wlangw-city.scu.edu.tw/auth/index.html/u");
            else
                result = AuthRequest("https://wlangw-main.scu.edu.tw/auth/index.html/u");

            //analysis result
            if(result.equals(R.string.NotifyAuthSuccess)){
                ShowNotify(getString(result), getString(R.string.NotifyAuthSuccessMsg), true, false);
                SetDataEditor.putBoolean("CampusFlag", ((counter & 1) == 1) == flag);
                SetDataEditor.apply();
                return ;
            }
            else if(result.equals(R.string.NotifyAuthDeny)){
                SetDataEditor.putBoolean("CampusFlag", ((counter & 1) == 1) == flag);
                SetDataEditor.apply();
                return ;
            }
            else if(result.equals(R.string.NotifyAuthFail)){
                if(!SetData.getBoolean("AuthFail", false)){
                    SetDataEditor.putBoolean("AuthFail", true);
                    SetDataEditor.apply();
                    ShowNotify(getString(result), getString(R.string.NotifyAuthFailMsg), true, true);
                }
                SetDataEditor.putBoolean("CampusFlag", ((counter & 1) == 1) == flag);
                SetDataEditor.apply();
                return ;
            }
            else if(result.equals(R.string.NotifyAuthOther)){
            }
            else if(result.equals(R.string.NotifyAuthError)){
            }

            //retry delay
            if(counter != retry){
                try{
                    Thread.sleep(1000 * counter);
                }
                catch(InterruptedException err){
                }
            }
        }
        if(result.equals(R.string.NotifyAuthNotActive))
            ShowNotify(getString(result), getString(R.string.NotifyAuthNotActiveMsg), true, true);
        else
            ShowNotify(getString(result), errmsg, true, true);
    }

    @Override
    protected void onHandleIntent(Intent intent){
        StartAuth(5);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
