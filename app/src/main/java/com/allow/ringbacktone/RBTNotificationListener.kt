package com.allow.ringbacktone

import android.app.Notification
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.annotation.RequiresApi
import android.widget.Toast
import timber.log.Timber
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri


@RequiresApi(Build.VERSION_CODES.KITKAT)
class RBTNotificationListener : NotificationListenerService() {

    private var mMediaPlayer: MediaPlayer? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Timber.i("Notification Posted")
        Timber.i(
            "${sbn.packageName}\t${sbn.notification.tickerText}\t${sbn.notification.extras.getString(Notification.EXTRA_TEXT)}"
        )

        val extras = sbn.notification.extras
        if ("Dialing" == extras.getString(Notification.EXTRA_TEXT)) {
            manageVolume(true)
        } else {
            manageVolume(false)
        }

        /*if ("Ongoing call" == extras.getString(Notification.EXTRA_TEXT)) {
            Timber.i("Ongoing call")

            val intent = Intent(MainActivity.ACTION_RBT_CALL_DETECTED)
            intent.putExtra(MainActivity.EXTRA_CALL_STATE, MainActivity.CALL_STATE_ONGOING)
            sendBroadcast(intent)

            Toast.makeText(applicationContext, "Ongoing call", Toast.LENGTH_SHORT).show()

            manageVolume(false)

        } else if ("Dialing" == extras.getString(Notification.EXTRA_TEXT)) {
            Timber.i("Dialing")

            val intent = Intent(MainActivity.ACTION_RBT_CALL_DETECTED)
            intent.putExtra(MainActivity.EXTRA_CALL_STATE, MainActivity.CALL_STATE_DIALING)
            sendBroadcast(intent)

            Toast.makeText(applicationContext, "Dialing", Toast.LENGTH_SHORT).show()

            manageVolume(true)
        }*/
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Timber.i("********** onNotificationRemoved")
        Timber.i("ID :${sbn.id}\t${sbn.notification.tickerText}\t${sbn.packageName}")

        manageVolume(false)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Toast.makeText(this, "Ringback Tone Service connected", Toast.LENGTH_SHORT).show()
    }

    @Suppress("DEPRECATION")
    private fun manageVolume(playmusic: Boolean) {
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL,
                if (playmusic) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0)
        } else {
            audioManager.setStreamMute(AudioManager.STREAM_VOICE_CALL, playmusic)
        }

        if (playmusic) {
            val rbtSoundUri = RBTApp.instance.prefs.getRBTSoundUri()
            rbtSoundUri?.let {
                val parsedUri = Uri.parse(it)
                mMediaPlayer = MediaPlayer.create(applicationContext, parsedUri).apply {
                    start()
                    isLooping = true
                }
            }
        } else {
            mMediaPlayer?.run {
                stop()
                release()
            }
            mMediaPlayer = null
        }

    }

}

