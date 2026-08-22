package com.example.cloudbot.util

import android.content.Context
import android.net.wifi.WifiManager

object WifiUtil {
    fun getCurrentSsid(ctx: Context): String? {
        return try {
            val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifi.connectionInfo ?: return null
            var ssid = info.ssid ?: return null
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) ssid = ssid.substring(1, ssid.length - 1)
            if (ssid.equals("<unknown ssid>", true)) null else ssid
        } catch (_: Exception) {
            null
        }
    }
}