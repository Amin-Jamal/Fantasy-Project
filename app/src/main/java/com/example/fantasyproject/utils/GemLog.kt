package com.example.fantasyproject.utils

import android.util.Log
import java.lang.Exception

object GemLog {

    private var EXCEPTION_TAG = " ** EXCEPTION ** "
    var TAG = "***GemLog***"

    fun printException(
        className: String,
        funName: String,
        exception: Exception,
        otherString: String
    ) {
        try {
            val logString =
                "Class : $className / Function : $funName / Exception : ${
                    exception.message
                } Other : $otherString"
            Log.e(EXCEPTION_TAG, logString)
        } catch (e: Exception){
        }
    }

    fun print(log: String){
        Log.e(TAG, log)
    }

}