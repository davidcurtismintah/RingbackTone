package com.allow.ringbacktone

import android.Manifest
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    companion object {
        private const val RBT_PERMISSIONS_REQUEST_CALL_PHONE = 1

        private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
        private const val ACTION_NOTIFICATION_LISTENER_SETTINGS =
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"

        const val EXTRA_CALL_STATE = "com.github.dcm.EXTRA_NOTIF_CODE"
        const val CALL_STATE_ONGOING = "Ongoing call"
        const val CALL_STATE_DIALING = "Dialing"

        const val ACTION_RBT_CALL_DETECTED = "com.github.dcm.RingbackTone"
        const val PICK_AUDIO_REQUEST = 100
    }

    private var mTelephonyManager: TelephonyManager? = null
    private var mListener: RBTPhoneCallListener? = null

    //---------------------------------------
    private var enableAccessibilityServiceAlertDialog: AlertDialog? = null

    //---------------------------------------
    private var enableNotificationListenerAlertDialog: AlertDialog? = null

    //---------------------------------------
    private var ringbackToneBroadcastReceiver: RBTBroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        phone_icon.setOnClickListener(::callNumber)
        button_retry.setOnClickListener(::retryApp)

        mTelephonyManager = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager

        if (isTelephonyEnabled) {
            Timber.d(getString(R.string.telephony_enabled))
            checkForPhonePermission()
            mListener = RBTPhoneCallListener()
            mTelephonyManager?.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE)
        } else {
            Toast.makeText(this, getString(R.string.telephony_not_enabled), Toast.LENGTH_LONG).show()
            Timber.d(getString(R.string.telephony_not_enabled))
            disableCallButton()
        }

        //-----------------------------------------
        if (!isNotificationServiceEnabled) {
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog()
            enableNotificationListenerAlertDialog?.show()
        }

        //-----------------------------------------
        /*if (!isAccessibilityServiceEnabled) {
            enableAccessibilityServiceAlertDialog = buildAccessibilityServiceAlertDialog();
            enableAccessibilityServiceAlertDialog?.show()
        }*/

        //-----------------------------------------
        ringbackToneBroadcastReceiver = RBTBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_RBT_CALL_DETECTED)
        registerReceiver(ringbackToneBroadcastReceiver, intentFilter)
        Timber.d("Receiver registered")

        pick_audio.setOnClickListener { pickAudio() }

    }

    private fun callNumber(view: View) {

        val phoneNumber = "tel: ${editText_main.text}"

        Timber.d("%s%s", getString(R.string.dial_number), phoneNumber)
        Toast.makeText(this, getString(R.string.dial_number) + phoneNumber, Toast.LENGTH_LONG).show()

        val callIntent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse(phoneNumber) }
        if (callIntent.resolveActivity(packageManager) != null) {
            checkForPhonePermission()
            startActivity(callIntent)
        } else {
            Timber.e("Can't resolve app for ACTION_CALL Intent.")
        }
    }

    private fun retryApp(view: View) {

    }

    @AfterPermissionGranted(RBT_PERMISSIONS_REQUEST_CALL_PHONE)
    private fun checkForPhonePermission() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CALL_PHONE)) {
            enableCallButton()
        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.call_phone_rationale),
                RBT_PERMISSIONS_REQUEST_CALL_PHONE, Manifest.permission.CALL_PHONE
            )
        }
    }

    private fun enableCallButton() {
        phone_icon.show()
    }

    private fun disableCallButton() {
        Toast.makeText(this, getString(R.string.phone_disabled), Toast.LENGTH_LONG).show()
        phone_icon.hide()
        if (isTelephonyEnabled) {
            button_retry.visibility = View.VISIBLE
        }
    }

    private val isTelephonyEnabled
        get() = mTelephonyManager?.simState == TelephonyManager.SIM_STATE_READY

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
//        enableCallButton()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Timber.d("onPermissionsDenied: %d : %d", requestCode, perms.size)
        disableCallButton()
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            Toast.makeText(this, R.string.returned_from_app_settings_to_activity, Toast.LENGTH_SHORT).show()
        } else if (requestCode == PICK_AUDIO_REQUEST) {
            data?.let {
                val rbtSoundUri = it.data
                rbtSoundUri?.let { uri ->
                    grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val takeFlags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                    RBTApp.instance.prefs.setRBTSoundUri(uri.toString())
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTelephonyEnabled) {
            mTelephonyManager?.listen(mListener, PhoneStateListener.LISTEN_NONE)
        }

        //---------------------------------------------
        unregisterReceiver(ringbackToneBroadcastReceiver)
        Timber.d("Receiver unregistered")
    }

    private inner class RBTPhoneCallListener : PhoneStateListener() {

        private var returningFromOffHook = false

        override fun onCallStateChanged(state: Int, incomingNumber: String) {

            var message = getString(R.string.phone_status)

            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    message = "$message${getString(R.string.ringing)}$incomingNumber"
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    Timber.i(message)
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    message = "$message${getString(R.string.offhook)}"
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    Timber.i(message)
                    returningFromOffHook = true
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    message = "$message${getString(R.string.idle)}"
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    Timber.i(message)
                    if (returningFromOffHook) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                            Timber.i(getString(R.string.restarting_app))
                            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
                        }
                    }
                }
                else -> {
                    message = "$message${getString(R.string.phone_off)}"
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    Timber.i(message)
                }
            }
        }
    }

    //--------------------------------------------------------------
    private val isNotificationServiceEnabled: Boolean
        get() {
            val pkgName = packageName
            val flat = Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS)
            if (!TextUtils.isEmpty(flat)) {
                val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in names.indices) {
                    val cn = ComponentName.unflattenFromString(names[i])
                    if (cn != null) {
                        if (TextUtils.equals(pkgName, cn.packageName)) {
                            return true
                        }
                    }
                }
            }
            return false
        }

    private fun buildNotificationServiceAlertDialog(): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(R.string.rbt_notification_listener_service)
        alertDialogBuilder.setMessage(R.string.rbt_notification_listener_service_explanation)
        alertDialogBuilder.setPositiveButton(android.R.string.yes) { _, _ ->
            startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        alertDialogBuilder.setNegativeButton(
            android.R.string.no
        ) { _, _ ->
            // If you choose to not enable the notification listener
            // the app. will not work as expected
        }
        return alertDialogBuilder.create()
    }

    //--------------------------------------------------------------
    private val isAccessibilityServiceEnabled: Boolean
        get() {
            val pkgName = packageName
            val flat =
                Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ACCESSIBILITY_ENABLED)
            if (!TextUtils.isEmpty(flat)) {
                val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in names.indices) {
                    val cn = ComponentName.unflattenFromString(names[i])
                    if (cn != null) {
                        if (TextUtils.equals(pkgName, cn.packageName)) {
                            return true
                        }
                    }
                }
            }
            return false
        }

    private fun buildAccessibilityServiceAlertDialog(): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(R.string.rbt_accessibility_service)
        alertDialogBuilder.setMessage(R.string.rbt_accessibility_service_explanation)
        alertDialogBuilder.setPositiveButton(android.R.string.yes) { _, _ ->
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        alertDialogBuilder.setNegativeButton(
            android.R.string.no
        ) { _, _ ->
            // If you choose to not enable the notification listener
            // the app. will not work as expected
        }
        return alertDialogBuilder.create()
    }

    //--------------------------------------------------------------
    class RBTBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val receivedState = intent.getStringExtra(EXTRA_CALL_STATE)

            Toast.makeText(context, "Received Call State: $receivedState", Toast.LENGTH_SHORT).show()
        }
    }


    private fun pickAudio() {
        val audioIntent = Intent()
        audioIntent.type = "audio/*"
        audioIntent.action = Intent.ACTION_OPEN_DOCUMENT
        audioIntent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(audioIntent, PICK_AUDIO_REQUEST)
    }

}

