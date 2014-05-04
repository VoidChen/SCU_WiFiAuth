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

import android.view.View;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class WifiDetectReceiver extends BroadcastReceiver{
    private static final String SETTING_DATA = "PrefSetData";

    private void AuthService(Context context){
        Intent AuthIntent = new Intent(context, AuthIntentService.class);

        SharedPreferences SetData = context.getSharedPreferences(SETTING_DATA, 0);
        AuthIntent.putExtra("username", SetData.getString("username", "NULL"));
        AuthIntent.putExtra("password", SetData.getString("password", "NULL"));
        AuthIntent.putExtra("Login", "");

        context.startService(AuthIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent){
        SharedPreferences SetData = context.getSharedPreferences(SETTING_DATA, 0);
        if(SetData.getBoolean("MainToggleState", false)){
            NetworkInfo networkinfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if(networkinfo != null){
                if(networkinfo.getState() == NetworkInfo.State.CONNECTED){
                    String ssid = "";
                    WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
                    WifiInfo wifiinfo = wifimanager.getConnectionInfo();
                    if(wifiinfo != null){
                        ssid = wifiinfo.getSSID();
                        if(ssid.equals("\"SCU WiFi\"") || ssid.equals("\"SCU WiFi New\""))
                            AuthService(context);
                    }
                }
                else if(networkinfo.getState() == NetworkInfo.State.CONNECTING){
                }
                else{
                }
            }
        }
    }
}
