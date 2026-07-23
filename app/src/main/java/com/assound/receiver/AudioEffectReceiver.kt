package com.assound.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log

class AudioEffectReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AudioEffectReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val action = intent.action ?: return
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)
        val audioSession = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        val contentType = intent.getIntExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        
        when (action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                Log.d(TAG, "Open audio effect session: $audioSession from $packageName")
                
                // Forward to service to attach effects
                val serviceIntent = Intent(context, com.assound.audio.service.AudioProcessingService::class.java).apply {
                    this.action = "com.assound.action.ATTACH_SESSION"
                    putExtra("session_id", audioSession)
                    putExtra("package_name", packageName)
                    putExtra("content_type", contentType)
                }
                
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start service for session attachment", e)
                }
            }
            
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                Log.d(TAG, "Close audio effect session: $audioSession from $packageName")
                
                // Forward to service to detach effects
                val serviceIntent = Intent(context, com.assound.audio.service.AudioProcessingService::class.java).apply {
                    this.action = "com.assound.action.DETACH_SESSION"
                    putExtra("session_id", audioSession)
                    putExtra("package_name", packageName)
                }
                
                try {
                    context.startService(serviceIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to notify service of session close", e)
                }
            }
        }
    }
}
