package com.example.fantasyproject.v2ray.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.fantasyproject.BuildConfig
import com.example.fantasyproject.utils.GemLog
//import com.jimbovpn.jimbo2023.app.AppConstant
//import com.jimbovpn.jimbo2023.app.utils.GemLog
import com.example.fantasyproject.v2ray.V2rayConstants
import com.example.fantasyproject.v2ray.service.V2RayTestService
import java.io.Serializable


object MessageUtil {

    fun sendMsg2Service(ctx: Context, what: Int, content: Serializable) {
        sendMsg(ctx, V2rayConstants.BROADCAST_ACTION_SERVICE, what, content)
    }

    fun sendMsg2UI(ctx: Context, what: Int, content: Serializable) {
        GemLog.print("MessageUtil sendMsg2UI MSG_MEASURE_DELAY")
        sendMsg(ctx, V2rayConstants.BROADCAST_ACTION_ACTIVITY, what, content)
    }

    fun sendMsg2TestService(ctx: Context, what: Int, content: Serializable) {
        try {
            val intent = Intent()
            intent.component = ComponentName(ctx, V2RayTestService::class.java)
            intent.putExtra("key", what)
            intent.putExtra("content", content)
            ctx.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendMsg(ctx: Context, action: String, what: Int, content: Serializable) {
        try {
            GemLog.print("sendMsg MSG_MEASURE_DELAY")
            val intent = Intent()
            intent.action = action
            intent.`package` = BuildConfig.APPLICATION_ID
            intent.putExtra("key", what)
            intent.putExtra("content", content)
            ctx.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
