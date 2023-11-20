package com.example.fantasyproject.v2ray.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.fantasyproject.v2ray.V2rayConstants.MSG_MEASURE_CONFIG
import com.example.fantasyproject.v2ray.V2rayConstants.MSG_MEASURE_CONFIG_CANCEL
import com.example.fantasyproject.v2ray.V2rayConstants.MSG_MEASURE_CONFIG_SUCCESS
import com.example.fantasyproject.v2ray.utils.MessageUtil
import com.example.fantasyproject.v2ray.utils.SpeedTestUtil
import com.example.fantasyproject.v2ray.utils.Utils
import go.Seq
import kotlinx.coroutines.*
import libv2ray.Libv2ray
import java.util.concurrent.Executors

class V2RayTestService : Service() {
    private val realTestScope by lazy { CoroutineScope(Executors.newFixedThreadPool(10).asCoroutineDispatcher()) }

    override fun onCreate() {
        super.onCreate()
        Seq.setContext(this)
        Libv2ray.initV2Env(Utils.userAssetPath(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getIntExtra("key", 0)) {
            MSG_MEASURE_CONFIG -> {
                val contentPair = intent.getSerializableExtra("content") as Pair<String, String>
                realTestScope.launch {
                    val result = SpeedTestUtil.realPing(contentPair.second)
                    MessageUtil.sendMsg2UI(this@V2RayTestService, MSG_MEASURE_CONFIG_SUCCESS, Pair(contentPair.first, result))
                }
            }
            MSG_MEASURE_CONFIG_CANCEL -> {
                realTestScope.coroutineContext[Job]?.cancelChildren()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
