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
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;

public class AuthIntentService extends IntentService{

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

    @Override
    protected void onHandleIntent(Intent intent){
        try{
            URL url = new URL("https://wlangw-city.scu.edu.tw/auth/index.html/u");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);

            OutputStream reqbody = conn.getOutputStream();

            String reqdata = "";
            reqdata = WriteReq(reqdata, "username", intent.getStringExtra("username"));
            reqdata = WriteReq(reqdata, "password", intent.getStringExtra("password"));
            reqdata = WriteReq(reqdata, "Login", intent.getStringExtra("Login"));

            reqbody.write(reqdata.getBytes());
            reqbody.flush();
            reqbody.close();

            conn.connect();

            InputStream res = conn.getInputStream();
            String body = readStream(res);
            conn.disconnect();
        }
        catch (IOException err){
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
