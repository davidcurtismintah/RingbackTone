package com.allow.ringbacktone

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import timber.log.Timber

class RBTAccessibilityService : AccessibilityService() {

    private var isCalling = false
    private var isAnswered = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val info = event.source
            if (info != null && info.text != null) {
                val label = info.text.toString()
                val zeroSeconds = String.format("%02d:%02d", 0, 0)
//                Timber.d("after calculation - $zeroSeconds --- $duration")
                when (label) {
                    zeroSeconds -> if (!isAnswered) {
                        isAnswered = true
                        Timber.i("Call answered")

                        val intent = Intent("com.github.dcm.RingbackTone")
                        intent.putExtra(MainActivity.EXTRA_CALL_STATE, MainActivity.CALL_STATE_ONGOING)
                        sendBroadcast(intent)

                        Toast.makeText(applicationContext, "Call answered", Toast.LENGTH_SHORT).show()
                    }
                    "Calling…" -> if (!isCalling) {
                        isCalling = true
                        Timber.i("Calling…")

                        val intent = Intent("com.github.dcm.RingbackTone")
                        intent.putExtra(MainActivity.EXTRA_CALL_STATE, MainActivity.CALL_STATE_DIALING)
                        sendBroadcast(intent)

                        Toast.makeText(applicationContext, "Calling...", Toast.LENGTH_SHORT).show()
                    }
                }
                info.recycle()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "Ringback Tone Service connected", Toast.LENGTH_SHORT).show()
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 0
        info.packageNames = null
        serviceInfo = info
    }

    override fun onInterrupt() {

    }
}
