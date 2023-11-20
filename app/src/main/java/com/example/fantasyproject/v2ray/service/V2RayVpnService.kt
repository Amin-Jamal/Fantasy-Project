package com.example.fantasyproject.v2ray.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.*
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.fantasyproject.R
import com.example.fantasyproject.rx.RxBus
import com.example.fantasyproject.rx.RxEvent
import com.example.fantasyproject.utils.BroadCastUtil
import com.example.fantasyproject.utils.GemLog
//import com.jimbovpn.jimbo2023.R
//import com.jimbovpn.jimbo2023.app.rx.RxBus
//import com.jimbovpn.jimbo2023.app.rx.RxEvent
//import com.jimbovpn.jimbo2023.app.utils.BroadCastUtil
//import com.jimbovpn.jimbo2023.app.utils.GemLog
import com.example.fantasyproject.v2ray.V2rayConstants
import com.example.fantasyproject.v2ray.dto.ERoutingMode
import com.example.fantasyproject.v2ray.utils.MmkvManager
import com.example.fantasyproject.v2ray.utils.MyContextWrapper
import com.example.fantasyproject.v2ray.utils.Utils
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Process
import java.lang.ref.SoftReference

class V2RayVpnService : VpnService(), ServiceControl {
    companion object {
        private const val VPN_MTU = 1500
        private const val PRIVATE_VLAN4_CLIENT = "26.26.26.1"
        private const val PRIVATE_VLAN4_ROUTER = "26.26.26.2"
        private const val PRIVATE_VLAN6_CLIENT = "da26:2626::1"
        private const val PRIVATE_VLAN6_ROUTER = "da26:2626::2"
        private const val TUN2SOCKS = "libtun2socks.so"
    }

    private val settingsStorage by lazy {
        MMKV.mmkvWithID(
            MmkvManager.ID_SETTING,
            MMKV.MULTI_PROCESS_MODE
        )
    }

    private lateinit var mInterface: ParcelFileDescriptor
    private var isRunning = false

    //val fd: Int get() = mInterface.fd
    private lateinit var process: Process

    /**destroy
     * Unfortunately registerDefaultNetworkCallback is going to return our VPN interface: https://android.googlesource.com/platform/frameworks/base/+/dda156ab0c5d66ad82bdcf76cda07cbc0a9c8a2e
     *
     * This makes doing a requestNetwork with REQUEST necessary so that we don't get ALL possible networks that
     * satisfies default network capabilities but only THE default network. Unfortunately we need to have
     * android.permission.CHANGE_NETWORK_STATE to be able to call requestNetwork.
     *
     * Source: https://android.googlesource.com/platform/frameworks/base/+/2df4c7d/services/core/java/com/android/server/ConnectivityService.java#887
     */
    @delegate:RequiresApi(Build.VERSION_CODES.P)
    private val defaultNetworkRequest by lazy {
        NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()
    }

    private val connectivity by lazy { getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    @delegate:RequiresApi(Build.VERSION_CODES.P)
    private val defaultNetworkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                setUnderlyingNetworks(arrayOf(network))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                // it's a good idea to refresh capabilities
                setUnderlyingNetworks(arrayOf(network))
            }

            override fun onLost(network: Network) {
                setUnderlyingNetworks(null)
            }
        }
    }

    override fun onCreate() {
        try {
            super.onCreate()
            Log.e("V2RayVpnService", "onCreate")
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            V2RayServiceManager.serviceControl = SoftReference(this)
        } catch (e: Exception) {
            GemLog.printException("V2RayVpnService", "onCreate", e, "")
        }
    }

    override fun onRevoke() {
        Log.e("V2RayVpnService", "onRevoke")
        stopV2Ray()
    }

//    override fun onLowMemory() {
//        stopV2Ray()
//        super.onLowMemory()
//    }

    override fun onDestroy() {
        Log.e("V2RayVpnService", "onDestroy")
        V2RayServiceManager.cancelNotification()
        super.onDestroy()


    }

    private fun setup() {
        try {

            Log.e("V2RayVpnService", "setup")
            val prepare = prepare(this)
            if (prepare != null) {
                return
            }

            // If the old interface has exactly the same parameters, use it!
            // Configure a builder while parsing the parameters.
            val builder = Builder()
            //val enableLocalDns = defaultDPreference.getPrefBoolean(AppConfig.PREF_LOCAL_DNS_ENABLED, false)

            val routingMode = settingsStorage?.decodeString(V2rayConstants.PREF_ROUTING_MODE)
                ?: ERoutingMode.GLOBAL_PROXY.value

            builder.setMtu(VPN_MTU)
            builder.addAddress(PRIVATE_VLAN4_CLIENT, 30)
            //builder.addDnsServer(PRIVATE_VLAN4_ROUTER)
            if (routingMode == ERoutingMode.BYPASS_LAN.value || routingMode == ERoutingMode.BYPASS_LAN_MAINLAND.value) {
                resources.getStringArray(R.array.bypass_private_ip_address).forEach {
                    val addr = it.split('/')
                    builder.addRoute(addr[0], addr[1].toInt())
                }
            } else {
                builder.addRoute("0.0.0.0", 0)
            }

            if (settingsStorage?.decodeBool(V2rayConstants.PREF_PREFER_IPV6) == true) {
                builder.addAddress(PRIVATE_VLAN6_CLIENT, 126)
                if (routingMode == ERoutingMode.BYPASS_LAN.value || routingMode == ERoutingMode.BYPASS_LAN_MAINLAND.value) {
                    builder.addRoute("2000::", 3) //currently only 1/8 of total ipV6 is in use
                } else {
                    builder.addRoute("::", 0)
                }
            }

            if (settingsStorage?.decodeBool(V2rayConstants.PREF_LOCAL_DNS_ENABLED) == true) {
                builder.addDnsServer(PRIVATE_VLAN4_ROUTER)
            } else {
                Utils.getVpnDnsServers()
                    .forEach {
                        if (Utils.isPureIpAddress(it)) {
                            builder.addDnsServer(it)
                        }
                    }
            }

            builder.setSession(V2RayServiceManager.currentConfig?.remarks.orEmpty())

            if (settingsStorage?.decodeBool(V2rayConstants.PREF_PER_APP_PROXY) == true) {
                val apps = settingsStorage?.decodeStringSet(V2rayConstants.PREF_PER_APP_PROXY_SET)
                val bypassApps =
                    settingsStorage?.decodeBool(V2rayConstants.PREF_BYPASS_APPS) ?: false
                apps?.forEach {
                    try {
                        if (bypassApps)
                            builder.addDisallowedApplication(it)
                        else
                            builder.addAllowedApplication(it)
                    } catch (e: PackageManager.NameNotFoundException) {
                        e.printStackTrace()
                        GemLog.printException("V2rayVpnService","setup",e,"apps?.forEach {")
                    }
                }
            }

            // Close the old interface since the parameters have been changed.
            try {
                mInterface.close()
            } catch (ignored: Exception) {
//                ignored.printStackTrace()
//                GemLog.printException("V2rayVpnService","setup",ignored,"   mInterface.close()")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    connectivity.requestNetwork(defaultNetworkRequest, defaultNetworkCallback)
                } catch (e: Exception) {
                    e.printStackTrace()
                    GemLog.printException("V2rayVpnService","setup",e,"if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) / connectivity.requestNetwork")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            // Create a new interface using the builder and save the parameters.
            try {
                mInterface = builder.establish()!!
                isRunning = true
                runTun2socks()
            } catch (e: Exception) {
                // non-nullable lateinit var
                e.printStackTrace()
                GemLog.printException("V2rayVpnService","setup",e,"Create a new interface using the builder and save the parameters.")
                stopV2Ray()
            }
        } catch (e: Exception) {
            GemLog.printException("V2RayVpnService", "setup", e, "")
        }
    }

    private fun runTun2socks() {
        try {

            Log.e("V2RayVpnService", "runTun2socks")
            val socksPort = Utils.parseInt(
                settingsStorage?.decodeString(V2rayConstants.PREF_SOCKS_PORT),
                V2rayConstants.PORT_SOCKS.toInt()
            )

            val cmd = arrayListOf(
                File(applicationContext.applicationInfo.nativeLibraryDir, TUN2SOCKS).absolutePath,
                "--netif-ipaddr",
                PRIVATE_VLAN4_ROUTER,
                "--netif-netmask",
                "255.255.255.252",
                "--socks-server-addr",
                "127.0.0.1:${socksPort}",
                "--tunmtu",
                VPN_MTU.toString(),
                "--sock-path",
                "sock_path",//File(applicationContext.filesDir, "sock_path").absolutePath,
                "--enable-udprelay",
                "--loglevel",
                "notice"
            )

            /*if (BuildConfig.DEBUG) {
                val file = File(applicationContext.applicationInfo.nativeLibraryDir)
                val array: Array<out String>? = file.list()
                Log.e("Files", "array: " + array!!.size)
                for (item in array) {
                    Log.e("Files", "item: $item")
                }
            }*/

            if (settingsStorage?.decodeBool(V2rayConstants.PREF_PREFER_IPV6) == true) {
                cmd.add("--netif-ip6addr")
                cmd.add(PRIVATE_VLAN6_ROUTER)
            }
            if (settingsStorage?.decodeBool(V2rayConstants.PREF_LOCAL_DNS_ENABLED) == true) {
                val localDnsPort = Utils.parseInt(
                    settingsStorage?.decodeString(V2rayConstants.PREF_LOCAL_DNS_PORT),
                    V2rayConstants.PORT_LOCAL_DNS.toInt()
                )
                cmd.add("--dnsgw")
                cmd.add("127.0.0.1:${localDnsPort}")
            }

            try {
                val proBuilder = ProcessBuilder(cmd)
                proBuilder.redirectErrorStream(true)
                process = proBuilder
                    .directory(applicationContext.filesDir)
                    .start()
                Thread(Runnable {
                    process.waitFor()
                    if (isRunning) {
                        runTun2socks()
                    }
                }).start()
                Log.d(packageName, process.toString())

                sendFd()
            } catch (e: Exception) {
                Log.e(packageName, "Error $e")
                GemLog.printException("V2RayVpnService", "setup", e, "ProcessBuilder(cmd)")
            }
        } catch (e: Exception) {
            GemLog.printException("V2RayVpnService", "setup", e, "")
        }
    }

    private fun sendFd() {
        try {

            val fd = mInterface.fileDescriptor
            val path = File(applicationContext.filesDir, "sock_path").absolutePath
            Log.d(packageName, path)

            GlobalScope.launch(Dispatchers.IO) {
                var tries = 0
                while (true) try {
                    Thread.sleep(50L shl tries)
                    Log.d(packageName, "sendFd tries: $tries")
                    LocalSocket().use { localSocket ->
                        localSocket.connect(
                            LocalSocketAddress(
                                path,
                                LocalSocketAddress.Namespace.FILESYSTEM
                            )
                        )
                        localSocket.setFileDescriptorsForSend(arrayOf(fd))
                        localSocket.outputStream.write(42)
                    }
                    break
                } catch (e: Exception) {
                    Log.d(packageName, "Error $e")
                    GemLog.printException("V2rayVpnService","sendFd",e," GlobalScope.launch(Dispatchers.IO) {")
                    if (tries > 5) break
                    tries += 1
                }
            }
        } catch (e: Exception) {
            GemLog.printException("V2RayVpnService", "sendFd", e, "")
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        Log.e("V2RayVpnService", "checking time arrived ")
        val intent = Intent(BroadCastUtil.checkConnectActivitySeenIntentFilter)
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("V2RayVpnService", "onStartCommand")
        V2RayServiceManager.startV2rayPoint()

        try {
            val delayMillis: Long = 10000
            handler.postDelayed(runnable, delayMillis)
        } catch (e: Exception) {
            GemLog.printException("V2RayVpnService", "onStartCommand", e, "postDelayed")
        }


        return START_STICKY
        //return super.onStartCommand(intent, flags, startId)
    }

    private fun stopV2Ray(isForced: Boolean = true) {
        try {

            Log.e("V2RayVpnService", "stopV2Ray")
//        val configName = defaultDPreference.getPrefString(PREF_CURR_CONFIG_GUID, "")
//        val emptyInfo = VpnNetworkInfo()
//        val info = loadVpnNetworkInfo(configName, emptyInfo)!! + (lastNetworkInfo ?: emptyInfo)
//        saveVpnNetworkInfo(configName, info)
            isRunning = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    connectivity.unregisterNetworkCallback(defaultNetworkCallback)
                } catch (ignored: Exception) {
                    GemLog.printException("V2rayVpnService","stopV2Ray",ignored,"connectivity.unregisterNetworkCallback(defaultNetworkCallback)")
                }
            }

            try {
                Log.d(packageName, "tun2socks destroy")
                process.destroy()
            } catch (e: Exception) {
                Log.d(packageName, "Error $e")
                GemLog.printException("V2rayVpnService","stopV2Ray",e,"  process.destroy()")
            }

            V2RayServiceManager.stopV2rayPoint()

            if (isForced) {
                //stopSelf has to be called ahead of mInterface.close(). otherwise v2ray core cannot be stooped
                //It's strage but true.
                //This can be verified by putting stopself() behind and call stopLoop and startLoop
                //in a row for several times. You will find that later created v2ray core report port in use
                //which means the first v2ray core somehow failed to stop and release the port.
                stopSelf()

                try {
                    mInterface.close()
                } catch (ignored: Exception) {
                    GemLog.printException("V2rayVpnService","stopV2Ray",ignored,"mInterface.close()")
                }
            }
        } catch (e: Exception) {
            GemLog.printException("V2RayVpnService", "stopV2Ray", e, "")
        }
    }

    override fun getService(): Service {
        return this
    }

    override fun startService() {
        Log.e("V2RayVpnService", "startService")
        setup()
    }

    override fun stopService() {
        Log.e("V2RayVpnService", "stopService")
        RxBus.publish(RxEvent.StopProxyService())
        stopV2Ray(true)
    }

    override fun vpnProtect(socket: Int): Boolean {
        return protect(socket)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun attachBaseContext(newBase: Context?) {
        val context = newBase?.let {
            MyContextWrapper.wrap(newBase, Utils.getLocale(newBase))
        }
        super.attachBaseContext(context)
    }
}
