package com.example.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class DeviceContactEntry(
    val name: String,
    val phoneNumber: String
)

object ContactsHelper {

    fun hasContactsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    fun lookupCallerName(context: Context, phoneNumber: String): String? {
        if (!hasContactsPermission(context) || phoneNumber.isBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun loadDeviceContacts(context: Context, limit: Int = 50): List<DeviceContactEntry> {
        if (!hasContactsPermission(context)) return emptyList()
        val results = mutableListOf<DeviceContactEntry>()
        val seenNumbers = mutableSetOf<String>()
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                while (cursor.moveToNext() && results.size < limit) {
                    val name = cursor.getString(0)?.trim().orEmpty()
                    val number = cursor.getString(1)?.trim().orEmpty()
                    if (name.isNotEmpty() && number.isNotEmpty() && seenNumbers.add(number)) {
                        results.add(DeviceContactEntry(name = name, phoneNumber = number))
                    }
                }
            }
        } catch (_: Exception) {
        }
        return results
    }
}
