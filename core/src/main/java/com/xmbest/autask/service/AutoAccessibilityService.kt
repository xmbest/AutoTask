package com.xmbest.autask.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.xmbest.autask.api.AutoTaskApi
import com.xmbest.autask.executor.TaskExecutor
import com.xmbest.autask.manager.TaskManager
import com.xmbest.autask.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoAccessibilityService"
        private var instance: AutoAccessibilityService? = null

        // 获取服务实例
        fun getInstance(): AutoAccessibilityService? = instance

        // 启动任务的Intent Action
        const val ACTION_START_TASK = "com.xmbest.autask.START_TASK"
        const val EXTRA_TASK_ID = "task_id"
    }

    private lateinit var taskExecutor: TaskExecutor
    private lateinit var taskManager: TaskManager
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this
        taskManager = TaskManager(this)
        taskExecutor = TaskExecutor(this)

        // 监听任务执行状态
        serviceScope.launch {
            taskExecutor.taskResult.collect { result ->
                Log.d(TAG, "任务状态更新: ${result.status} - ${result.message}")

                // 当任务失败、取消或完成时，切换回监听本应用
                if (result.status == TaskStatus.FAILED ||
                    result.status == TaskStatus.CANCELLED ||
                    result.status == TaskStatus.COMPLETED
                ) {
                    updatePackageNamesForTask(packageName)
                    Log.i(TAG, "任务结束(${result.status})，切换回监听本应用: $packageName")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        taskExecutor.stopTask()
        AutoTaskApi.updateIsAccessibilityEnabled(false)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            Log.d(TAG, "onAccessibilityEvent: ${it.eventType} - ${it.packageName}")

            // 检测用户操作，用于任务中断
            if (it.eventType == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START ||
                it.eventType == AccessibilityEvent.TYPE_GESTURE_DETECTION_START
            ) {
                taskExecutor.notifyUserAction()
            }

            // 监听页面变化事件，用于实时页面检测
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                it.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
            ) {
                taskExecutor.notifyPageChanged()
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
        taskExecutor.stopTask()
    }

    override fun onServiceConnected() {
        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            // 默认只监听本应用，避免性能问题
            packageNames = arrayOf(packageName)
            notificationTimeout = 100
        }
        setServiceInfo(serviceInfo)

        Log.i(TAG, "无障碍服务已连接，默认监听本应用: $packageName")

        AutoTaskApi.updateIsAccessibilityEnabled(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_TASK -> {
                    val taskId = it.getStringExtra(EXTRA_TASK_ID)
                    if (taskId != null) {
                        startTask(taskId)
                    }
                }
            }
        }
        return START_STICKY
    }

    /**
     * 启动任务
     */
    fun startTask(taskId: String): Boolean {
        val taskConfig = taskManager.getTaskConfig(taskId)
        if (taskConfig == null) {
            Log.e(TAG, "任务配置不存在: $taskId")
            return false
        }

        // 切换监听到任务的目标应用
        updatePackageNamesForTask(taskConfig.appInfo.packageName)

        Log.i(TAG, "启动任务: ${taskConfig.taskName}，切换监听到: ${taskConfig.appInfo.packageName}")
        taskExecutor.executeTask(taskConfig)
        return true
    }

    /**
     * 停止当前任务
     */
    fun stopCurrentTask() {
        taskExecutor.stopTask()
    }

    /**
     * 暂停当前任务
     */
    fun pauseCurrentTask() {
        taskExecutor.pauseTask()
    }

    /**
     * 恢复当前任务
     */
    fun resumeCurrentTask() {
        taskExecutor.resumeTask()
    }

    /**
     * 获取任务管理器
     */
    fun getTaskManager(): TaskManager = taskManager

    /**
     * 获取任务执行器
     */
    fun getTaskExecutor(): TaskExecutor = taskExecutor

    /**
     * 添加监听的应用包名
     */
    fun addPackageName(packageName: String) {
        val currentPackages = serviceInfo?.packageNames?.toMutableList() ?: mutableListOf()
        if (!currentPackages.contains(packageName)) {
            currentPackages.add(packageName)

            val newServiceInfo = serviceInfo?.apply {
                this.packageNames = currentPackages.toTypedArray()
            }

            if (newServiceInfo != null) {
                setServiceInfo(newServiceInfo)
                Log.d(TAG, "添加监听包名: $packageName")
            }
        } else {
            Log.d(TAG, "包名已存在: $packageName")
        }
    }

    /**
     * 移除监听的应用包名
     */
    fun removePackageName(packageName: String) {
        val currentPackages = serviceInfo?.packageNames?.toMutableList() ?: return
        if (currentPackages.remove(packageName)) {
            val newServiceInfo = serviceInfo?.apply {
                this.packageNames = if (currentPackages.isEmpty()) {
                    arrayOf(this@AutoAccessibilityService.packageName) // 默认监听本应用
                } else {
                    currentPackages.toTypedArray()
                }
            }

            if (newServiceInfo != null) {
                setServiceInfo(newServiceInfo)
                Log.d(TAG, "移除监听包名: $packageName")
            }
        }
    }

    /**
     * 为任务更新监听的包名
     * @param targetPackageName 目标应用包名，如果为空则监听本应用
     */
    private fun updatePackageNamesForTask(targetPackageName: String?) {
        val packageToListen = if (targetPackageName.isNullOrEmpty()) {
            packageName // 监听本应用
        } else {
            targetPackageName
        }

        val newServiceInfo = serviceInfo?.apply {
            this.packageNames = arrayOf(packageToListen)
        }

        if (newServiceInfo != null) {
            setServiceInfo(newServiceInfo)
            Log.d(TAG, "更新监听包名为: $packageToListen")
        }
    }
}