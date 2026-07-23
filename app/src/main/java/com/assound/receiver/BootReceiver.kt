package com.assound.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.assound.audio.service.AudioProcessingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "Boot completed, starting audio service")
                
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        AudioProcessingService.startService(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start service on boot", e)
                    }
                }
            }
        }
    }
}
