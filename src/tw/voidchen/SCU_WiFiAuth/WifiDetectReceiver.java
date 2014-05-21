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
import android.net.ConnectivityManager;

public class WifiDetectReceiver extends BroadcastReceiver{
    private static final String SETTING_DATA = "PrefSetData";

    private void AuthService(Context context){
        Intent AuthIntent = new Intent(context, AuthIntentService.class);
        context.startService(AuthIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent){
        SharedPreferences SetData = context.getSharedPreferences(SETTING_DATA, 0);
        if(SetData.getBoolean("MainToggleState", false)){
            ConnectivityManager ConnMgr = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
            NetworkInfo networkinfo = ConnMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if(networkinfo != null){
                String state = (networkinfo.getDetailedState()).toString();
                if(state.equals("CONNECTED") || state.equals("CAPTIVE_PORTAL_CHECK")){
                    WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
                    if(wifimanager != null){
                        WifiInfo wifiinfo = wifimanager.getConnectionInfo();
                        if(wifiinfo != null){
                            String ssid = wifiinfo.getSSID();
                            if(ssid.equals("\"SCU WiFi\"") || ssid.equals("\"SCU WiFi New\"") || ssid.equals("SCU WiFi") || ssid.equals("SCU WiFi New"))
                                AuthService(context);
                        }
                    }
                }
            }
        }
    }
}
