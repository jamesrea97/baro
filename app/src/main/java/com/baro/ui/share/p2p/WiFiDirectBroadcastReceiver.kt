package com.baro.ui.share.p2p

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat


class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val activity: WifiDirectActivity
) : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                when (intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)) {
                    WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
                        activity.wifiDirectStatusUpdate(wifiDirectConnected=true)
                    }
                    else -> {
                        activity.wifiDirectStatusUpdate(wifiDirectConnected=false)
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                if (ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                manager?.requestPeers(channel) { peers: WifiP2pDeviceList? ->
                    // Handle peers list
                    peers?.let { activity.updateWifiP2PDeviceList(it.deviceList) }
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections

                manager.requestConnectionInfo(channel, activity);

            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing
            }
        }
    }
}