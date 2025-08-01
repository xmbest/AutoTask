package com.xmbest.autotask.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * 无障碍权限工具类
 * 专门用于无障碍服务权限的检测和申请
 */
object AccessibilityPermissionUtils {

    /**
     * 检查无障碍服务是否已启用
     * @param context 上下文
     * @param serviceClass 无障碍服务类
     * @return true表示已启用，false表示未启用
     */
    fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val serviceName = "${context.packageName}/${serviceClass.name}"
        return isAccessibilityServiceEnabled(context, serviceName)
    }

    /**
     * 检查无障碍服务是否已启用
     * @param context 上下文
     * @param serviceName 服务名称（格式：包名/服务类全名）
     * @return true表示已启用，false表示未启用
     */
    fun isAccessibilityServiceEnabled(context: Context, serviceName: String): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            0
        }

        if (accessibilityEnabled == 1) {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return enabledServices?.contains(serviceName) == true
        }
        return false
    }

    /**
     * 打开无障碍服务设置页面
     * @param context 上下文
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * 检查并引导用户开启无障碍服务
     * @param context 上下文
     * @param serviceClass 无障碍服务类
     * @param callback 回调接口
     */
    fun checkAndRequestAccessibilityPermission(
        context: Context,
        serviceClass: Class<*>,
        callback: AccessibilityPermissionCallback
    ) {
        if (isAccessibilityServiceEnabled(context, serviceClass)) {
            callback.onAccessibilityPermissionGranted()
        } else {
            callback.onAccessibilityPermissionDenied()
            openAccessibilitySettings(context)
        }
    }

    /**
     * 无障碍权限回调接口
     */
    interface AccessibilityPermissionCallback {
        /**
         * 无障碍权限已授权
         */
        fun onAccessibilityPermissionGranted()

        /**
         * 无障碍权限未授权
         */
        fun onAccessibilityPermissionDenied()
    }
}