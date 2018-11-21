package com.allow.ringbacktone

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

//-----------------------------------------------------------------
class RBTPlayerService: Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val ACTION_PLAY: String = "com.github.RBT.action.PLAY"
    }

    private var mMediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val action = intent.action
        when(action) {
            ACTION_PLAY -> {
                mMediaPlayer = MediaPlayer.create(
                    applicationContext,
                    intent.getIntExtra("audio_file_id", -1)
                )
                mMediaPlayer?.apply {
                    setOnPreparedListener { start() }
                    setOnErrorListener { mp, what, extra ->
                        mMediaPlayer?.release()
                        mMediaPlayer = null
                        true
                    }
                    prepareAsync()
                }

            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaPlayer?.release()
    }
}