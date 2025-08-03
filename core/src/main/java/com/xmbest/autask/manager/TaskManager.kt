package com.xmbest.autask.manager

import android.content.Context
import android.util.Log
import com.xmbest.autask.model.TaskConfig
import com.xmbest.autask.model.TaskResult
import com.xmbest.autask.model.TaskStatus
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * 任务管理器
 * 负责任务的配置管理、存储和调度
 */
class TaskManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TaskManager"
        private const val TASK_CONFIG_DIR = "task_configs"
        private const val TASK_CONFIG_EXTENSION = ".json"
    }
    
    private val taskConfigs = ConcurrentHashMap<String, TaskConfig>()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    init {
        loadAllTaskConfigs()
    }
    
    /**
     * 添加任务配置
     */
    fun addTaskConfig(taskConfig: TaskConfig): Boolean {
        return try {
            if (!taskConfig.validate()) {
                Log.e(TAG, "任务配置验证失败: ${taskConfig.taskId}")
                return false
            }
            
            taskConfigs[taskConfig.taskId] = taskConfig
            saveTaskConfig(taskConfig)
            Log.i(TAG, "任务配置添加成功: ${taskConfig.taskName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "添加任务配置失败: ${taskConfig.taskId}", e)
            false
        }
    }
    
    /**
     * 更新任务配置
     */
    fun updateTaskConfig(taskConfig: TaskConfig): Boolean {
        return try {
            if (!taskConfigs.containsKey(taskConfig.taskId)) {
                Log.e(TAG, "任务配置不存在: ${taskConfig.taskId}")
                return false
            }
            
            if (!taskConfig.validate()) {
                Log.e(TAG, "任务配置验证失败: ${taskConfig.taskId}")
                return false
            }
            
            val updatedConfig = taskConfig.copy(updateTime = System.currentTimeMillis())
            taskConfigs[updatedConfig.taskId] = updatedConfig
            saveTaskConfig(updatedConfig)
            Log.i(TAG, "任务配置更新成功: ${updatedConfig.taskName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "更新任务配置失败: ${taskConfig.taskId}", e)
            false
        }
    }
    
    /**
     * 删除任务配置
     */
    fun removeTaskConfig(taskId: String): Boolean {
        return try {
            taskConfigs.remove(taskId)
            deleteTaskConfigFile(taskId)
            Log.i(TAG, "任务配置删除成功: $taskId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "删除任务配置失败: $taskId", e)
            false
        }
    }
    
    /**
     * 获取任务配置
     */
    fun getTaskConfig(taskId: String): TaskConfig? {
        return taskConfigs[taskId]
    }
    
    /**
     * 获取所有任务配置
     */
    fun getAllTaskConfigs(): List<TaskConfig> {
        return taskConfigs.values.toList()
    }
    
    /**
     * 根据应用包名获取任务配置
     */
    fun getTaskConfigsByPackage(packageName: String): List<TaskConfig> {
        return taskConfigs.values.filter { it.appInfo.packageName == packageName }
    }
    
    /**
     * 搜索任务配置
     */
    fun searchTaskConfigs(keyword: String): List<TaskConfig> {
        return taskConfigs.values.filter {
            it.taskName.contains(keyword, ignoreCase = true) ||
            it.description.contains(keyword, ignoreCase = true) ||
            it.appInfo.packageName.contains(keyword, ignoreCase = true)
        }
    }
    
    /**
     * 导入任务配置
     */
    fun importTaskConfig(configJson: String): Boolean {
        return try {
            val taskConfig = json.decodeFromString<TaskConfig>(configJson)
            addTaskConfig(taskConfig)
        } catch (e: Exception) {
            Log.e(TAG, "导入任务配置失败", e)
            false
        }
    }
    
    /**
     * 导出任务配置
     */
    fun exportTaskConfig(taskId: String): String? {
        return try {
            val taskConfig = getTaskConfig(taskId)
            if (taskConfig != null) {
                json.encodeToString(taskConfig)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "导出任务配置失败: $taskId", e)
            null
        }
    }
    
    /**
     * 导出所有任务配置
     */
    fun exportAllTaskConfigs(): String? {
        return try {
            val allConfigs = getAllTaskConfigs()
            json.encodeToString(allConfigs)
        } catch (e: Exception) {
            Log.e(TAG, "导出所有任务配置失败", e)
            null
        }
    }
    
    /**
     * 批量导入任务配置
     */
    fun importTaskConfigs(configsJson: String): Int {
        return try {
            val taskConfigs = json.decodeFromString<List<TaskConfig>>(configsJson)
            var successCount = 0
            
            taskConfigs.forEach { taskConfig ->
                if (addTaskConfig(taskConfig)) {
                    successCount++
                }
            }
            
            Log.i(TAG, "批量导入任务配置完成，成功: $successCount, 总数: ${taskConfigs.size}")
            successCount
        } catch (e: Exception) {
            Log.e(TAG, "批量导入任务配置失败", e)
            0
        }
    }
    
    /**
     * 验证任务配置
     */
    fun validateTaskConfig(taskConfig: TaskConfig): List<String> {
        val errors = mutableListOf<String>()
        
        // 基本验证
        if (taskConfig.taskId.isBlank()) {
            errors.add("任务ID不能为空")
        }
        
        if (taskConfig.taskName.isBlank()) {
            errors.add("任务名称不能为空")
        }
        
        if (taskConfig.appInfo.packageName.isBlank()) {
            errors.add("应用包名不能为空")
        }
        
        if (taskConfig.pages.isEmpty()) {
            errors.add("页面配置不能为空")
        }
        

        
        // 检查页面跳转的完整性通过transitions配置
        taskConfig.transitions.forEach { transition ->
            if (taskConfig.getPageConfig(transition.fromPageId) == null) {
                errors.add("跳转规则中的源页面不存在: ${transition.fromPageId}")
            }
            transition.toPageIds.forEach { toPageId ->
                if (taskConfig.getPageConfig(toPageId) == null) {
                    errors.add("跳转规则中的目标页面不存在: $toPageId")
                }
            }
        }
        
        taskConfig.pages.forEach { page ->
            
            // 检查页面识别条件
            if (page.identifiers.isEmpty()) {
                errors.add("页面 ${page.pageId} 缺少识别条件")
            }
            
            // 检查操作配置
            page.actions.forEach { action ->
                if (action.actionType.name.isBlank()) {
                    errors.add("页面 ${page.pageId} 中存在无效操作")
                }
            }
        }
        
        return errors
    }
    
    /**
     * 获取任务统计信息
     */
    fun getTaskStatistics(): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        
        stats["totalTasks"] = taskConfigs.size
        stats["appPackages"] = taskConfigs.values.map { it.appInfo.packageName }.distinct().size
        stats["totalPages"] = taskConfigs.values.sumOf { it.pages.size }
        stats["totalActions"] = taskConfigs.values.sumOf { config ->
            config.pages.sumOf { it.actions.size }
        }
        
        // 按应用分组统计
        val appStats = taskConfigs.values.groupBy { it.appInfo.packageName }
            .mapValues { it.value.size }
        stats["tasksByApp"] = appStats
        
        return stats
    }
    
    /**
     * 保存任务配置到文件
     */
    private fun saveTaskConfig(taskConfig: TaskConfig) {
        try {
            val configDir = File(context.filesDir, TASK_CONFIG_DIR)
            if (!configDir.exists()) {
                configDir.mkdirs()
            }
            
            val configFile = File(configDir, "${taskConfig.taskId}$TASK_CONFIG_EXTENSION")
            val configJson = json.encodeToString(taskConfig)
            
            FileOutputStream(configFile).use { fos ->
                fos.write(configJson.toByteArray())
            }
            
            Log.d(TAG, "任务配置保存成功: ${configFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "保存任务配置失败: ${taskConfig.taskId}", e)
            throw e
        }
    }
    
    /**
     * 从文件加载任务配置
     */
    private fun loadTaskConfig(configFile: File): TaskConfig? {
        return try {
            FileInputStream(configFile).use { fis ->
                val configJson = fis.readBytes().toString(Charsets.UTF_8)
                json.decodeFromString<TaskConfig>(configJson)
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载任务配置失败: ${configFile.name}", e)
            null
        }
    }
    
    /**
     * 加载所有任务配置
     */
    private fun loadAllTaskConfigs() {
        try {
            val configDir = File(context.filesDir, TASK_CONFIG_DIR)
            if (!configDir.exists()) {
                return
            }
            
            val configFiles = configDir.listFiles { _, name ->
                name.endsWith(TASK_CONFIG_EXTENSION)
            } ?: return
            
            var loadedCount = 0
            configFiles.forEach { file ->
                val taskConfig = loadTaskConfig(file)
                if (taskConfig != null) {
                    taskConfigs[taskConfig.taskId] = taskConfig
                    loadedCount++
                }
            }
            
            Log.i(TAG, "加载任务配置完成，成功: $loadedCount, 总数: ${configFiles.size}")
        } catch (e: Exception) {
            Log.e(TAG, "加载任务配置失败", e)
        }
    }
    
    /**
     * 删除任务配置文件
     */
    private fun deleteTaskConfigFile(taskId: String) {
        try {
            val configDir = File(context.filesDir, TASK_CONFIG_DIR)
            val configFile = File(configDir, "${taskId}$TASK_CONFIG_EXTENSION")
            
            if (configFile.exists()) {
                configFile.delete()
                Log.d(TAG, "任务配置文件删除成功: ${configFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除任务配置文件失败: $taskId", e)
        }
    }
}