package com.example.fantasyproject.ui

import android.net.VpnService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.fantasyproject.AppConstant
import com.example.fantasyproject.R
import com.example.fantasyproject.databinding.ActivityMainBinding
import com.example.fantasyproject.v2ray.V2rayConstants
import com.example.fantasyproject.v2ray.dto.ServersCache
import com.example.fantasyproject.v2ray.extension.toast
import com.example.fantasyproject.v2ray.service.V2RayServiceManager
import com.example.fantasyproject.v2ray.utils.AngConfigManager
import com.example.fantasyproject.v2ray.utils.MmkvManager
import com.example.fantasyproject.v2ray.utils.Utils
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private val mainStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_MAIN, MMKV.MULTI_PROCESS_MODE) }
    private val settingsStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SETTING, MMKV.MULTI_PROCESS_MODE) }
    val serversCache = mutableListOf<ServersCache>()
    val mainViewModel: MainViewModel by viewModels()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        copyAssets()
        migrateLegacy()
        initViews()

    }

//    private val settingsStorage by lazy {
//        MMKV.mmkvWithID(
//            MmkvManager.ID_SETTING,
//            MMKV.MULTI_PROCESS_MODE
//        )
//    }

//    private val mainStorage by lazy {
//        MMKV.mmkvWithID(
//            MmkvManager.ID_MAIN,
//            MMKV.MULTI_PROCESS_MODE
//        )
//    }

    private fun connectV2ray() {
        try {
            if((settingsStorage?.decodeString(V2rayConstants.PREF_MODE) ?: "VPN") == "VPN") {
                val intent = VpnService.prepare(this)
                if (intent == null){
                    startV2Ray()
                } else {

                }
            } else {
                startV2Ray()
            }
        } catch (e: Exception){

        }
    }

    private fun startV2Ray() {
        try {
            if (mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER).isNullOrEmpty()){
                return
            }
            V2RayServiceManager.startV2Ray(this)
        } catch (e: Exception){

        }
    }

    private fun copyAssets() {
        try {
            val extFolder = Utils.userAssetPath(this)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val geo = arrayOf("geosite.dat", "geoip.dat")
                    assets.list("")
                        ?.filter { geo.contains(it) }
                        ?.filter { !File(extFolder, it).exists() }
                        ?.forEach {
                            val target = File(extFolder, it)
                            assets.open(it).use { input ->
                                FileOutputStream(target).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception){
        }
    }

    private fun migrateLegacy() {
        try {

            lifecycleScope.launch(Dispatchers.IO) {
                val result = AngConfigManager.migrateLegacyConfig(this@MainActivity)
                if (result != null) {
                    launch(Dispatchers.Main) {
                        if (result) {
                        } else {
                        }
                    }
                }
            }
        } catch (e: Exception) {

        }
    }

    private val requestVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK){
                startV2Ray()
            } else {
            }
        }

    fun initViews() {
        binding.btnDisConnect.setOnClickListener {
            Utils.stopVService(this)
        }



        binding.btnConnect.setOnClickListener {
            try {
                var config =
                    "vmess://eyJhZGQiOiJpcmMyLnNjb2ZpZWxkLndlYnNpdGUiLCJhaWQiOiIwIiwiYWxwbiI6IiIsImZwIjoiIiwiaG9zdCI6Inp1bGEuaXIiLCJpZCI6IjMwNDgzMTkyLWJmZjctNDQxZi1hZGU5LTlkMWRhZTJkNzZiMCIsIm5ldCI6InRjcCIsInBhdGgiOiIvIiwicG9ydCI6IjI2MTU2IiwicHMiOiI1NiIsInNjeSI6ImF1dG8iLCJzbmkiOiIiLCJ0bHMiOiIiLCJ0eXBlIjoiaHR0cCIsInYiOiIyIn0="
                importBatchConfig(config)
            } catch (e: Exception){
                e.printStackTrace()
            }

            if (mainViewModel.isRunning.value == true) {
                Utils.stopVService(this)
            } else if (settingsStorage?.decodeString(AppConstant.PREF_MODE) ?: "VPN" == "VPN") {
                val intent = VpnService.prepare(this)
                if (intent == null) {
                    startV2Ray()
                } else {
                    requestVpnPermission.launch(intent)
                }
            } else {
                startV2Ray()
            }
        }
    }



    fun importBatchConfig(server: String?, subid: String = "") {
        val subid2 = if(subid.isNullOrEmpty()){
            mainViewModel.subscriptionId
        }else{
            subid
        }
        val append = subid.isNullOrEmpty()

        var count = AngConfigManager.importBatchConfig(server, subid2, append)
        if (count <= 0) {
            count = AngConfigManager.importBatchConfig(Utils.decode(server!!), subid2, append)
        }
        if (count > 0) {
            toast(R.string.toast_success)
            mainViewModel.reloadServerList()
        } else {
            toast(R.string.toast_failure)
        }
    }

}













