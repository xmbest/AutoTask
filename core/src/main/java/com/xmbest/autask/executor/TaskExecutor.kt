package com.xmbest.autask.executor

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import com.xmbest.autask.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 任务执行器
 * 负责协调整个自动化任务的执行流程
 */
class TaskExecutor(private val accessibilityService: AccessibilityService) {
    
    companion object {
        private const val TAG = "TaskExecutor"
    }
    
    /**
     * 检测当前页面
     * 遍历所有页面配置，找到匹配当前界面的页面
     */
    private suspend fun detectCurrentPage(taskConfig: TaskConfig): String? {
        Log.i(TAG, "开始检测当前页面...")
        
        // 等待无障碍服务完全初始化
        delay(500)
        
        val rootNode = accessibilityService.rootInActiveWindow
        if (rootNode == null) {
            Log.w(TAG, "无法获取根节点，可能无障碍服务未完全启动或权限不足")
            return null
        }
        
        // 遍历所有页面配置
        for (pageConfig in taskConfig.pages) {
            try {
                // 检查页面是否匹配当前界面
                if (pageRecognizer.isPageMatched(rootNode, pageConfig)) {
                    Log.i(TAG, "匹配到页面: ${pageConfig.pageId} (${pageConfig.pageName})")
                    return pageConfig.pageId
                }
            } catch (e: Exception) {
                Log.w(TAG, "检测页面 ${pageConfig.pageId} 时出错: ${e.message}")
            }
        }
        
        Log.w(TAG, "未能匹配到任何页面")
        return null
    }
    
    private val actionExecutor = ActionExecutor(accessibilityService)
    private val pageRecognizer = PageRecognizer(accessibilityService)
    
    private var currentJob: Job? = null
    private var isUserInterrupted = false
    private var lastUserActionTime = 0L
    
    // 任务状态流
    private val _taskResult = MutableStateFlow(TaskResult(TaskStatus.PENDING))
    val taskResult: StateFlow<TaskResult> = _taskResult.asStateFlow()
    
    // 当前执行状态
    private var currentTaskConfig: TaskConfig? = null
    private var currentPageId: String = ""
    private var executedActionsCount = 0
    private var totalActionsCount = 0
    
    /**
     * 执行任务
     */
    fun executeTask(taskConfig: TaskConfig) {
        // 取消之前的任务
        stopTask()
        
        // 验证任务配置
        if (!taskConfig.validate()) {
            Log.e(TAG, "任务配置验证失败")
            _taskResult.value = TaskResult(
                status = TaskStatus.FAILED,
                message = "任务配置验证失败",
                errorCode = -1
            )
            return
        }
        
        currentTaskConfig = taskConfig
        currentPageId = taskConfig.startPageId
        executedActionsCount = 0
        totalActionsCount = calculateTotalActions(taskConfig)
        isUserInterrupted = false
        
        // 启动任务执行
        currentJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                _taskResult.value = TaskResult(
                    status = TaskStatus.RUNNING,
                    message = "任务开始执行",
                    startTime = System.currentTimeMillis(),
                    currentPageId = currentPageId,
                    totalActions = totalActionsCount
                )
                
                val result = executeTaskInternal(taskConfig)
                _taskResult.value = result.copy(
                    endTime = System.currentTimeMillis()
                )
                
            } catch (e: CancellationException) {
                Log.i(TAG, "任务被取消")
                _taskResult.value = TaskResult(
                    status = TaskStatus.CANCELLED,
                    message = "任务被取消",
                    executedActions = executedActionsCount,
                    totalActions = totalActionsCount,
                    endTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "任务执行异常", e)
                _taskResult.value = TaskResult(
                    status = TaskStatus.FAILED,
                    message = "任务执行异常: ${e.message}",
                    errorCode = -2,
                    executedActions = executedActionsCount,
                    totalActions = totalActionsCount,
                    endTime = System.currentTimeMillis()
                )
            }
        }
    }
    
    /**
     * 停止任务
     */
    fun stopTask() {
        currentJob?.cancel()
        currentJob = null
    }
    
    /**
     * 暂停任务
     */
    fun pauseTask() {
        if (_taskResult.value.status == TaskStatus.RUNNING) {
            _taskResult.value = _taskResult.value.copy(
                status = TaskStatus.PAUSED,
                message = "任务已暂停"
            )
        }
    }
    
    /**
     * 恢复任务
     */
    fun resumeTask() {
        if (_taskResult.value.status == TaskStatus.PAUSED) {
            _taskResult.value = _taskResult.value.copy(
                status = TaskStatus.RUNNING,
                message = "任务已恢复"
            )
        }
    }
    
    /**
     * 通知用户操作（用于中断检测）
     */
    fun notifyUserAction() {
        lastUserActionTime = System.currentTimeMillis()
        if (currentTaskConfig?.enableUserInterrupt == true && 
            _taskResult.value.status == TaskStatus.RUNNING) {
            isUserInterrupted = true
            Log.i(TAG, "检测到用户操作，任务将被中断")
        }
    }
    
    /**
     * 启动目标应用
     */
    private suspend fun launchTargetApp(packageName: String): Boolean {
        return try {
            Log.i(TAG, "正在启动应用: $packageName")
            val context = accessibilityService.applicationContext
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                
                // 等待应用启动
                delay(3000)
                Log.i(TAG, "应用启动完成: $packageName")
                true
            } else {
                Log.e(TAG, "无法找到应用的启动Intent: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动应用失败: $packageName, 错误: ${e.message}")
            false
        }
    }
    
    /**
     * 内部任务执行逻辑
     */
    private suspend fun executeTaskInternal(taskConfig: TaskConfig): TaskResult {
        var retryCount = 0
        
        while (retryCount <= taskConfig.maxGlobalRetry) {
            try {
                // 启动目标应用
                if (!launchTargetApp(taskConfig.appInfo.packageName)) {
                    return TaskResult(
                        status = TaskStatus.FAILED,
                        message = "无法启动目标应用: ${taskConfig.appInfo.packageName}",
                        errorCode = -2
                    )
                }
                
                // 检测当前页面
                val currentPageId = detectCurrentPage(taskConfig)
                    ?: return TaskResult(
                        status = TaskStatus.FAILED,
                        message = "无法识别当前页面，请确保应用处于正确状态",
                        errorCode = -3
                    )
                
                Log.i(TAG, "检测到当前页面: $currentPageId")
                
                // 从当前页面开始执行页面流程
                val result = executePageFlow(taskConfig, currentPageId)
                if (result.status == TaskStatus.COMPLETED) {
                    return result
                }
                
                // 如果失败且允许重试，则重试
                if (taskConfig.autoRetry && retryCount < taskConfig.maxGlobalRetry) {
                    retryCount++
                    Log.i(TAG, "任务执行失败，开始第${retryCount}次重试")
                    delay(1000) // 重试前等待
                    continue
                } else {
                    return result
                }
                
            } catch (e: Exception) {
                if (retryCount < taskConfig.maxGlobalRetry) {
                    retryCount++
                    Log.w(TAG, "任务执行异常，开始第${retryCount}次重试", e)
                    delay(1000)
                    continue
                } else {
                    throw e
                }
            }
        }
        
        return TaskResult(
            status = TaskStatus.FAILED,
            message = "任务重试次数已用完",
            errorCode = -5,
            executedActions = executedActionsCount,
            totalActions = totalActionsCount
        )
    }
    
    /**
     * 执行页面流程
     */
    private suspend fun executePageFlow(taskConfig: TaskConfig, startPageId: String): TaskResult {
        var currentPageId = startPageId
        val visitedPages = mutableSetOf<String>()
        
        while (true) {
            // 检查是否被取消
            if (!currentJob?.isActive!!) {
                return TaskResult(
                    status = TaskStatus.CANCELLED,
                    message = "任务被取消"
                )
            }
            
            // 检查是否被暂停
            while (_taskResult.value.status == TaskStatus.PAUSED) {
                delay(100)
            }
            
            // 检查用户中断
            if (isUserInterrupted) {
                return TaskResult(
                    status = TaskStatus.INTERRUPTED,
                    message = "任务被用户中断",
                    executedActions = executedActionsCount,
                    totalActions = totalActionsCount
                )
            }
            
            // 获取当前页面配置
            val pageConfig = taskConfig.getPageConfig(currentPageId)
                ?: return TaskResult(
                    status = TaskStatus.FAILED,
                    message = "找不到页面配置: $currentPageId",
                    errorCode = -6
                )
            
            // 检查是否为目标页面，但仍需执行该页面的actions
            val isTargetPage = pageConfig.isTargetPageType()
            
            // 检查循环
            if (visitedPages.contains(currentPageId)) {
                Log.w(TAG, "检测到页面循环: $currentPageId")
            }
            visitedPages.add(currentPageId)
            
            // 等待页面出现
            if (!pageRecognizer.waitForPage(pageConfig, pageConfig.pageTimeout)) {
                return TaskResult(
                    status = TaskStatus.FAILED,
                    message = "等待页面超时: ${pageConfig.pageName}",
                    errorCode = -7,
                    currentPageId = currentPageId
                )
            }
            
            // 执行页面操作
            val pageResult = executePageActions(pageConfig)
            if (!pageResult) {
                return TaskResult(
                    status = TaskStatus.FAILED,
                    message = "页面操作执行失败: ${pageConfig.pageName}",
                    errorCode = -8,
                    executedActions = executedActionsCount,
                    totalActions = totalActionsCount,
                    currentPageId = currentPageId
                )
            }
            
            // 更新当前页面状态
            this.currentPageId = currentPageId
            _taskResult.value = _taskResult.value.copy(
                currentPageId = currentPageId,
                executedActions = executedActionsCount
            )
            
            // 如果是目标页面且已执行完actions，则任务完成
            if (isTargetPage) {
                return TaskResult(
                    status = TaskStatus.COMPLETED,
                    message = "任务执行完成",
                    executedActions = executedActionsCount,
                    totalActions = totalActionsCount,
                    currentPageId = currentPageId
                )
            }
            
            // 检测当前实际页面，在所有pages中找到符合条件的页面
            val detectedPageId = detectCurrentPage(taskConfig)
            if (detectedPageId == null) {
                return TaskResult(
                    status = TaskStatus.FAILED,
                    message = "无法识别当前页面，任务执行失败",
                    errorCode = -9,
                    executedActions = executedActionsCount,
                    totalActions = totalActionsCount,
                    currentPageId = currentPageId
                )
            }
            
            // 如果检测到的页面与当前页面相同，说明页面没有变化，可能需要等待或者任务完成
            if (detectedPageId == currentPageId) {
                // 检查是否还有其他可能的页面跳转
                val possibleNextPages = taskConfig.getPossibleNextPages(currentPageId)
                if (possibleNextPages.isEmpty()) {
                    return TaskResult(
                        status = TaskStatus.COMPLETED,
                        message = "任务执行完成（无下一页面）",
                        executedActions = executedActionsCount,
                        totalActions = totalActionsCount,
                        currentPageId = currentPageId
                    )
                }
                // 等待一段时间后重新检测
                delay(1000)
                continue
            }
            
            // 更新到检测到的页面
            currentPageId = detectedPageId
        }
    }
    
    /**
     * 执行页面操作
     */
    private suspend fun executePageActions(pageConfig: PageConfig): Boolean {
        var retryCount = 0
        
        while (retryCount <= pageConfig.maxRetryCount) {
            var allActionsSucceeded = true
            
            for (action in pageConfig.actions) {
                // 检查用户中断
                if (currentTaskConfig?.enableUserInterrupt == true) {
                    checkUserInterrupt(currentTaskConfig!!.interruptDetectInterval)
                    if (isUserInterrupted) {
                        return false
                    }
                }
                
                // 执行操作
                var actionRetryCount = 0
                var actionSucceeded = false
                
                while (actionRetryCount <= action.retryCount) {
                    try {
                        actionSucceeded = actionExecutor.executeAction(action)
                        if (actionSucceeded) {
                            executedActionsCount++
                            break
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "操作执行异常: ${action.description}", e)
                    }
                    
                    actionRetryCount++
                    if (actionRetryCount <= action.retryCount) {
                        Log.w(TAG, "操作失败，重试第${actionRetryCount}次: ${action.description}")
                        delay(500)
                    }
                }
                
                if (!actionSucceeded) {
                    if (action.isOptional) {
                        Log.w(TAG, "可选操作失败，继续执行: ${action.description}")
                    } else {
                        Log.e(TAG, "必需操作失败: ${action.description}")
                        allActionsSucceeded = false
                        break
                    }
                }
            }
            
            if (allActionsSucceeded) {
                return true
            }
            
            retryCount++
            if (retryCount <= pageConfig.maxRetryCount) {
                Log.w(TAG, "页面操作失败，重试第${retryCount}次: ${pageConfig.pageName}")
                delay(1000)
            }
        }
        
        return false
    }
    
    /**
     * 检查用户中断
     */
    private suspend fun checkUserInterrupt(interval: Long) {
        delay(interval)
        // 这里可以添加更复杂的用户操作检测逻辑
    }
    
    /**
     * 计算总操作数
     */
    private fun calculateTotalActions(taskConfig: TaskConfig): Int {
        return taskConfig.pages.sumOf { it.actions.size }
    }
    
    /**
     * 获取当前任务配置
     */
    fun getCurrentTaskConfig(): TaskConfig? = currentTaskConfig
    
    /**
     * 获取当前页面调试信息
     */
    fun getCurrentPageDebugInfo(): String {
        return pageRecognizer.getCurrentPageDebugInfo()
    }
}