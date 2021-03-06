package com.baro.ui.share.p2p

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.*
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.baro.R
import com.baro.constants.AppCodes
import com.baro.constants.AppTags
import com.baro.constants.PermissionsEnum
import com.baro.helpers.AsyncHelpers
import com.baro.helpers.PermissionsHelper
import com.baro.helpers.interfaces.OnClientInetAddressReceived
import com.baro.helpers.interfaces.OnClientInetAddressSent
import com.baro.models.Course
import java.lang.ref.WeakReference
import java.net.InetAddress
import java.util.*


class WifiDirectActivity : AppCompatActivity(), WifiP2pManager.ConnectionInfoListener,
    OnClientInetAddressReceived, OnClientInetAddressSent {

    private var course: Course? = null

    // WifiDirect
    private val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null

    private var isReceiving: Boolean = false
    private var isSetUp: Boolean? = null

    private var otherDeviceInetAddress: InetAddress? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_direct)

        getUserIntent()
        configureView()

        initWifiP2P()
        discoverPeers()
    }

    private fun getUserIntent() {
        isReceiving =
            intent.extras?.get(AppTags.WIFIP2P_INTENT.name) != AppCodes.WIFIP2P_PEER_SEND.code

        if (!isReceiving) {
            course = intent.getParcelableExtra(AppTags.COURSE_OBJECT.name) as Course?
        }

    }


    private fun configureView() {

        if (!isReceiving) {
            val peerConnectionSelectionFragment: WifiDirectPeerConnectionSelectionFragment =
                WifiDirectPeerConnectionSelectionFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container_peer_connection, peerConnectionSelectionFragment, null)
                .commit()
        } else {
            val peerConnectionWaitingRoomFragment: WifiDirectPeerConnectionWaitingRoomFragment =
                WifiDirectPeerConnectionWaitingRoomFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(
                    R.id.fragment_container_peer_connection,
                    peerConnectionWaitingRoomFragment,
                    null
                )
                .commit()
        }
    }


    private fun discoverPeers() {


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Toast.makeText(
                    applicationContext,
                    "DEBUG: Discovering peers",
                    Toast.LENGTH_LONG
                )
                    .show()
            }

            override fun onFailure(reasonCode: Int) {
                Toast.makeText(
                    applicationContext,
                    "DEBUG: Cannot discover peers",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

    }

    private fun initWifiP2P() {
        channel = manager?.initialize(this, mainLooper, null)
        channel?.also { channel ->
            receiver = manager?.let { WiFiDirectBroadcastReceiver(it, channel, this) }
        }
    }

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }


    /* register the broadcast receiver with the intent values to be matched */
    override fun onResume() {
        super.onResume()
        receiver?.also { receiver ->
            registerReceiver(receiver, intentFilter)
        }
    }

    /* unregister the broadcast receiver */
    override fun onPause() {
        super.onPause()
        receiver?.also { receiver ->
            unregisterReceiver(receiver)
        }

        manager!!.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(
                    applicationContext,
                    "DEBUG: Device disconnected", Toast.LENGTH_LONG
                ).show()
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(
                    applicationContext,
                    "DEBUG: Could not disconnect devices Reason :$reason", Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    // Notify WifiDirectPeerConnectFragment
    fun wifiDirectStatusUpdate(wifiDirectConnected: Boolean) {
        if (!isReceiving) {
            val wifiDirectPeerConnectionFragment = supportFragmentManager
                .findFragmentById(R.id.fragment_container_peer_connection) as WifiDirectPeerConnectionSelectionFragment?
            wifiDirectPeerConnectionFragment?.changeWifiDirectStatus(wifiDirectConnected)
        } else {
            val wifiDirectPeerWaitingRoomFragment = supportFragmentManager
                .findFragmentById(R.id.fragment_container_peer_connection) as WifiDirectPeerConnectionWaitingRoomFragment?
            wifiDirectPeerWaitingRoomFragment?.changeWifiDirectStatus(wifiDirectConnected)
        }

    }

    fun updateWifiP2PDeviceList(wifiP2pDeviceList: MutableCollection<WifiP2pDevice>) {
        if (!isReceiving) {
            val wifiDirectPeerConnectionFragment = supportFragmentManager
                .findFragmentById(R.id.fragment_container_peer_connection) as WifiDirectPeerConnectionSelectionFragment
            wifiDirectPeerConnectionFragment.updateWifiP2PDeviceList(wifiP2pDeviceList)
        }
    }


    fun connectClientDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig()

        config.deviceAddress = device.deviceAddress
        channel?.also { channel ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            manager?.connect(channel, config, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    Toast.makeText(
                        applicationContext,
                        "DEBUG: Connection to " + device.deviceName,
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onFailure(reason: Int) {
                    Toast.makeText(
                        applicationContext,
                        "DEBUG: No connection established",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
        if (isSetUp == null) {
            if (info?.isGroupOwner == true && info?.groupFormed) {
                Toast.makeText(
                    applicationContext,
                    "DEBUG: GROUP OWNER",
                    Toast.LENGTH_LONG
                ).show()
                if (!isReceiving) {
                    val dataReceiver =
                        AsyncHelpers.GroupOwnerReceiveClientInetAddressAsyncTask(this)
                    dataReceiver.execute()
                } else {
                    shareCourse()
                }

            } else {
                otherDeviceInetAddress = info?.groupOwnerAddress
                if (isReceiving) {
                    val wifiReceiver = WifiDirectEndpoint(
                        AsyncHelpers.GroupOwnerReceiveClientInetAddressAsyncTask.PORT_GET_CLIENT_INET,
                        info?.groupOwnerAddress
                    )
                    val dataSender =
                        AsyncHelpers.ClientSendInetAddressAsyncTask(wifiReceiver, this)
                    dataSender.execute()
                } else {
                    shareCourse()
                }
            }
        }


    }


    override fun onClientInetAddressReceived(clientInetAddress: InetAddress?) {
        Toast.makeText(
            applicationContext,
            "DEBUG: Received Client's IP: " + clientInetAddress?.hostAddress,
            Toast.LENGTH_LONG

        ).show()
        otherDeviceInetAddress = clientInetAddress
        shareCourse()
    }

    override fun onClientInetAddressRSent() {
        shareCourse()
    }


    private fun shareCourse() {
        isSetUp = true
        if (isReceiving) {
            val receiveFragment: WifiDirectCourseReceiveFragment =
                WifiDirectCourseReceiveFragment.newInstance()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container_send_receive, receiveFragment, null)
                .addToBackStack(AppTags.RECEIVE_COURSE_FRAGMENT.name)
                .setReorderingAllowed(true)
                .commit()

        } else {

            val sendFragment: WifiDirectCourseSendFragment =
                WifiDirectCourseSendFragment.newInstance(course, otherDeviceInetAddress)

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container_send_receive, sendFragment, null)
                .addToBackStack(AppTags.SEND_COURSE_FRAGMENT.name)
                .setReorderingAllowed(true)
                .commit()
        }

    }
}