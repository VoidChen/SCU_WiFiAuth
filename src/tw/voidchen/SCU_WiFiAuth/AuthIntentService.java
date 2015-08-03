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

import android.os.Build;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread;
import java.lang.InterruptedException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AuthIntentService extends IntentService{

    private static final String SETTING_DATA = "PrefSetData";
    private String errmsg = "";
    private String hostname = "";

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

    private void ShowNotify(String title, String msg, boolean rewrite, boolean error){
        SharedPreferences SetData = getSharedPreferences(SETTING_DATA, 0);
        SharedPreferences.Editor SetDataEditor = SetData.edit();
        if(SetData.getBoolean("ShowNotify", true)){
            //get counter
            int counter;
            if(rewrite)
                counter = 0;
            else{
                counter = SetData.getInt("notify_counter", 0);
                SetDataEditor.putInt("notify_counter", counter + 1);
                SetDataEditor.apply();
            }

            //set intent
            Intent NotifyIntent;
            if(error)
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

            //set ErrorNotify
            SetDataEditor.putBoolean("ErrorNotify", error);
            SetDataEditor.apply();
        }
    }

    private void CancelErrorNotify(){
        SharedPreferences SetData = getSharedPreferences(SETTING_DATA, 0);
        SharedPreferences.Editor SetDataEditor = SetData.edit();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if(SetData.getBoolean("ErrorNotify", false)){
            SetDataEditor.putBoolean("ErrorNotify", false);
            SetDataEditor.apply();
            manager.cancelAll();
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

    private String CaptivePortalCheck(){
        String result = "CaptivePortalUnknown";
        try{
            //set conn
            URL url = new URL("http://www.google.com/generate_204");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setFollowRedirects(false);
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(3000);

            //connect
            conn.connect();

            //check result analysis
            Integer code = conn.getResponseCode();
            if(code.equals(204))
                result = "NotCaptivePortal";
            else if(code.equals(302)){
                String location = conn.getHeaderField("Location");
                if(location != null){
                    Pattern pattern = Pattern.compile("http[s]?://([^.]+.scu.edu.tw)");
                    Matcher matcher = pattern.matcher(location);
                    if(matcher.find()){
                        result = "CaptivePortalVerified";
                        hostname = matcher.group(1);
                    }
                }
            }
            else if(code.equals(200)){
                InputStream res = conn.getInputStream();
                String body = readStream(res);
                Pattern pattern = Pattern.compile("http[s]?://([^.]+.scu.edu.tw)");
                Matcher matcher = pattern.matcher(body);
                if(matcher.find()){
                    result = "CaptivePortalVerified";
                    hostname = matcher.group(1);
                }
            }

            //disconnect
            conn.disconnect();
        }
        catch (IOException err){
            //Error handle
            result = "CaptivePortalError";
        }
        return result;
    }

    private Integer AuthRequest(String site){
        Integer result = R.string.NotifyAuthOther;
        try{
            //set conn
            URL url = new URL(site);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setFollowRedirects(false);
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(3000);

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

            //auth result analysis
            Integer code = conn.getResponseCode();
            if(code.equals(200))
                result = R.string.NotifyAuthSuccess;
            else if(code.equals(302)){
                String location = conn.getHeaderField("Location");
                if(location != null){
                    if(location.contains("?errmsg=Access denied"))
                        result = R.string.NotifyAuthDeny;
                    else if(location.contains("?errmsg=Authentication failed"))
                        result = R.string.NotifyAuthFail;
                }
            }

            //disconnect
            conn.disconnect();
        }
        catch (IOException err){
            //Error handle
            errmsg = err.getMessage();
            result = R.string.NotifyAuthError;
        }
        return result;
    }

    private boolean StartAuth(int retry){
        Integer result = R.string.NotifyAuthNotActive;
        SharedPreferences SetData = getSharedPreferences(SETTING_DATA, 0);
        SharedPreferences.Editor SetDataEditor = SetData.edit();

        //start loop
        int counter;
        for(counter = 0;counter < retry; ++counter){
            //retry delay
            try{
                Thread.sleep(1000 * counter);
            }
            catch(InterruptedException err){
            }

            //Captive Portal Check
            result = R.string.NotifyAuthNotActive;
            String check = CaptivePortalCheck();
            if(check.equals("NotCaptivePortal")){
                CancelErrorNotify();
                return false;
            }
            else if(check.equals("CaptivePortalVerified"))
                result = AuthRequest("https://" + hostname + "/auth/index.html/u");
            else if(check.equals("CaptivePortalUnknown"))
                continue;
            else if(check.equals("CaptivePortalError"))
                continue;

            //analysis auth result
            if(result.equals(R.string.NotifyAuthSuccess)){
                ShowNotify(getString(result), getString(R.string.NotifyAuthSuccessMsg), true, false);
                return true;
            }
            else if(result.equals(R.string.NotifyAuthDeny)){
                CancelErrorNotify();
                return false;
            }
            else if(result.equals(R.string.NotifyAuthFail)){
                if(!SetData.getBoolean("AuthFail", false)){
                    SetDataEditor.putBoolean("AuthFail", true);
                    SetDataEditor.apply();
                    ShowNotify(getString(result), getString(R.string.NotifyAuthFailMsg), true, true);
                }
                return false;
            }
            else if(result.equals(R.string.NotifyAuthOther)){
            }
            else if(result.equals(R.string.NotifyAuthError)){
            }
        }
        if(result.equals(R.string.NotifyAuthError))
            ShowNotify(getString(result), errmsg, true, true);
        return false;
    }

    private Network BindNetwork(){
        Context context = getApplicationContext();
        ConnectivityManager ConnMgr = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        for(Network network: ConnMgr.getAllNetworks()){
            NetworkInfo networkinfo = ConnMgr.getNetworkInfo(network);
            if(networkinfo != null){
                if(networkinfo.getType() == ConnectivityManager.TYPE_WIFI){
                    ConnMgr.setProcessDefaultNetwork(network);
                    return network;
                }
            }
        }
        return null;
    }

    private void UpdateNetwork(Network network){
        if(network != null){
            Context context = getApplicationContext();
            ConnectivityManager ConnMgr = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
            ConnMgr.reportBadNetwork(network);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent){
        Network network = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            network = BindNetwork();
        if(StartAuth(5)){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                UpdateNetwork(network);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
