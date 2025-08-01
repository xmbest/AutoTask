package com.xmbest.autotask.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AutoAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoAccessibilityService"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d(TAG, "onAccessibilityEvent event = $event")
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
    }

    override fun onServiceConnected() {
        AccessibilityServiceInfo()
            .apply {
                eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.DEFAULT
                packageNames = arrayOf("com.netease.cloudmusic.iot")
                notificationTimeout = 10
            }
            .let { serviceInfo = it }
    }


    fun addPackageName(packageName: String) {
        if (serviceInfo.packageNames.contains(packageName)) {
            Log.d(TAG, "$packageName already exists")
            return
        }
        val newArray = serviceInfo.packageNames.plus(packageName)
        serviceInfo.packageNames = newArray
    }
}