package com.xmbest.autask.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import com.xmbest.autask.factory.TaskConfigFactory
import com.xmbest.autask.model.TaskConfig
import com.xmbest.autask.model.TaskResult
import com.xmbest.autask.service.AutoAccessibilityService
import com.xmbest.autotask.utils.AccessibilityPermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * AutoTask API接口
 * 提供给外部模块使用的核心功能接口
 */
@SuppressLint("StaticFieldLeak")
object AutoTaskApi {

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    fun updateIsAccessibilityEnabled(enabled: Boolean) {
        _isAccessibilityEnabled.update { enabled }
    }

    /**
     * 检查无障碍服务是否已启用
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return AccessibilityPermissionUtils.isAccessibilityServiceEnabled(
            context,
            AutoAccessibilityService::class.java
        )
    }

    /**
     * 请求开启无障碍服务
     */
    fun requestAccessibilityPermission(context: Context) {
        AccessibilityPermissionUtils.openAccessibilitySettings(context)
    }

    /**
     * 检查并请求无障碍权限
     */
    fun checkAndRequestAccessibilityPermission(
        context: Context,
        callback: AccessibilityPermissionUtils.AccessibilityPermissionCallback
    ) {
        AccessibilityPermissionUtils.checkAndRequestAccessibilityPermission(
            context,
            AutoAccessibilityService::class.java,
            callback
        )
    }

    /**
     * 启动任务
     */
    fun startTask(context: Context, taskId: String): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return if (service != null) {
            service.startTask(taskId)
        } else {
            // 通过Intent启动任务
            val intent = Intent(context, AutoAccessibilityService::class.java).apply {
                action = AutoAccessibilityService.ACTION_START_TASK
                putExtra(AutoAccessibilityService.EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
            true
        }
    }

    /**
     * 启动任务（重载方法，只需要taskId）
     */
    fun startTask(taskId: String): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return service?.let {
            try {
                it.startTask(taskId)
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    /**
     * 停止指定任务
     */
    fun stopTask(taskId: String): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return service?.let {
            try {
                it.stopCurrentTask()
                true
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    /**
     * 暂停指定任务
     */
    fun pauseTask(taskId: String): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return service?.let {
            try {
                it.pauseCurrentTask()
                true
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    /**
     * 恢复指定任务
     */
    fun resumeTask(taskId: String): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return service?.let {
            try {
                it.resumeCurrentTask()
                true
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    /**
     * 获取所有任务
     */
    fun getAllTasks(): List<TaskConfig> {
        val service = AutoAccessibilityService.getInstance()
        return service?.let {
            it.getTaskManager().getAllTaskConfigs()
        } ?: emptyList()
    }

    /**
     * 添加任务
     */
    fun addTask(taskConfig: TaskConfig): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return service?.let {
            it.getTaskManager().addTaskConfig(taskConfig)
        } ?: false
    }

    /**
     * 移除任务
     */
    fun removeTask(taskId: String): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return service?.let {
            it.getTaskManager().removeTaskConfig(taskId)
        } ?: false
    }

    /**
     * 停止当前任务
     */
    fun stopCurrentTask(): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return if (service != null) {
            service.stopCurrentTask()
            true
        } else {
            false
        }
    }

    /**
     * 暂停当前任务
     */
    fun pauseCurrentTask(): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return if (service != null) {
            service.pauseCurrentTask()
            true
        } else {
            false
        }
    }

    /**
     * 恢复当前任务
     */
    fun resumeCurrentTask(): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return if (service != null) {
            service.resumeCurrentTask()
            true
        } else {
            false
        }
    }

    /**
     * 获取任务执行状态
     */
    fun getTaskResult(): StateFlow<TaskResult>? {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskExecutor()?.taskResult
    }

    /**
     * 添加任务配置
     */
    fun addTaskConfig(context: Context, taskConfig: TaskConfig): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return if (service != null) {
            service.getTaskManager().addTaskConfig(taskConfig)
        } else {
            // 如果服务未启动，可以考虑直接操作文件
            false
        }
    }

    /**
     * 获取任务配置
     */
    fun getTaskConfig(taskId: String): TaskConfig? {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskManager()?.getTaskConfig(taskId)
    }

    /**
     * 获取所有任务配置
     */
    fun getAllTaskConfigs(): List<TaskConfig> {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskManager()?.getAllTaskConfigs() ?: emptyList()
    }

    /**
     * 删除任务配置
     */
    fun removeTaskConfig(taskId: String): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskManager()?.removeTaskConfig(taskId) ?: false
    }

    /**
     * 根据应用包名获取任务配置
     */
    fun getTaskConfigsByPackage(packageName: String): List<TaskConfig> {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskManager()?.getTaskConfigsByPackage(packageName) ?: emptyList()
    }

    /**
     * 搜索任务配置
     */
    fun searchTaskConfigs(keyword: String): List<TaskConfig> {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskManager()?.searchTaskConfigs(keyword) ?: emptyList()
    }

    /**
     * 导入任务配置
     */
    fun importTaskConfig(configJson: String): Boolean {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskManager()?.importTaskConfig(configJson) ?: false
    }

    /**
     * 导出任务配置
     */
    fun exportTaskConfig(taskId: String): String? {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskManager()?.exportTaskConfig(taskId)
    }

    /**
     * 导出所有任务配置
     */
    fun exportAllTaskConfigs(): String? {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskManager()?.exportAllTaskConfigs()
    }

    /**
     * 批量导入任务配置
     */
    fun importTaskConfigs(configsJson: String): Int {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskManager()?.importTaskConfigs(configsJson) ?: 0
    }

    /**
     * 验证任务配置
     */
    fun validateTaskConfig(taskConfig: TaskConfig): List<String> {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskManager()?.validateTaskConfig(taskConfig) ?: emptyList()
    }

    /**
     * 获取任务统计信息
     */
    fun getTaskStatistics(): Map<String, Any> {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskManager()?.getTaskStatistics() ?: emptyMap()
    }

    /**
     * 获取预设任务配置
     */
    fun getPresetTasks(): List<TaskConfig> {
        return TaskConfigFactory.getAllPresetTasks()
    }

    /**
     * 添加监听的应用包名
     */
    fun addPackageName(packageName: String) {
        val service = AutoAccessibilityService.getInstance()
        service?.addPackageName(packageName)
    }

    /**
     * 移除监听的应用包名
     */
    fun removePackageName(packageName: String) {
        val service = AutoAccessibilityService.getInstance()
        service?.removePackageName(packageName)
    }

    /**
     * 获取当前页面调试信息
     */
    fun getCurrentPageDebugInfo(): String? {
        val service = AutoAccessibilityService.getInstance()
        return service?.getTaskExecutor()?.getCurrentPageDebugInfo()
    }

    /**
     * 检查服务是否可用
     */
    fun isServiceAvailable(): Boolean {
        return AutoAccessibilityService.getInstance() != null
    }

    /**
     * 内部方法：由AutoAccessibilityService调用来通知状态变化
     * 当服务连接或断开时直接更新状态，比ContentObserver更准确
     */
    internal fun notifyAccessibilityServiceStateChanged(isConnected: Boolean) {
        _isAccessibilityEnabled.value = isConnected
        Log.d("AutoTaskApi", "无障碍服务状态变化: $isConnected")
    }
}