package com.example.telephony

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

data class TelephonySystemStatus(
    val hasCallScreeningRole: Boolean = false,
    val isDefaultDialer: Boolean = false,
    val hasRecordAudioPermission: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val hasPhoneStatePermission: Boolean = false,
    val hasAnswerCallsPermission: Boolean = false,
    val hasCallPhonePermission: Boolean = false,
    val hasNotificationsPermission: Boolean = false
) {
    val allCorePermissionsGranted: Boolean
        get() = hasRecordAudioPermission && hasContactsPermission && hasPhoneStatePermission && hasAnswerCallsPermission
}

object TelephonyRoleManager {

    fun requiredRuntimePermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            perms.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.toTypedArray()
    }

    fun inspectSystemStatus(context: Context): TelephonySystemStatus {
        val isScreeningHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }

        val telecomManager = context.getSystemService(TelecomManager::class.java)
        val isDialerHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            telecomManager?.defaultDialerPackage == context.packageName
        }

        fun granted(perm: String): Boolean =
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

        val answerGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            granted(Manifest.permission.ANSWER_PHONE_CALLS)
        } else true

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted(Manifest.permission.POST_NOTIFICATIONS)
        } else true

        return TelephonySystemStatus(
            hasCallScreeningRole = isScreeningHeld,
            isDefaultDialer = isDialerHeld,
            hasRecordAudioPermission = granted(Manifest.permission.RECORD_AUDIO),
            hasContactsPermission = granted(Manifest.permission.READ_CONTACTS),
            hasPhoneStatePermission = granted(Manifest.permission.READ_PHONE_STATE),
            hasAnswerCallsPermission = answerGranted,
            hasCallPhonePermission = granted(Manifest.permission.CALL_PHONE),
            hasNotificationsPermission = notifGranted
        )
    }

    fun createCallScreeningRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            }
        }
        return null
    }

    fun createDefaultDialerRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            }
        }
        return Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }
    }

    @SuppressLint("MissingPermission")
    fun answerRingingCallViaTelecom(context: Context): Boolean {
        return try {
            val telecomManager = context.getSystemService(TelecomManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
            ) {
                @Suppress("DEPRECATION")
                telecomManager?.acceptRingingCall()
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun endCallViaTelecom(context: Context): Boolean {
        return try {
            val telecomManager = context.getSystemService(TelecomManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
            ) {
                @Suppress("DEPRECATION")
                telecomManager?.endCall() == true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun launchDialerCallback(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Generates standard GSM conditional call forwarding MMI dialer strings
     * (*61* = no answer, *62* = unreachable, *67* = busy) for hosted AI phone routing.
     */
    fun buildConditionalForwardMmiCode(hostedNumber: String, delaySeconds: Int = 15): String {
        val clean = hostedNumber.filter { it.isDigit() || it == '+' }
        val clampedDelay = ((delaySeconds + 4) / 5 * 5).coerceIn(5, 30)
        return "*61*$clean*11*$clampedDelay#"
    }

    fun buildCancelForwardingMmiCode(): String = "##002#"

    fun openDialerWithMmiCode(context: Context, mmiCode: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(mmiCode)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
