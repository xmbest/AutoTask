package com.xmbest.autask.model

import kotlinx.serialization.Serializable

/**
 * 任务状态枚举
 */
@Serializable
enum class TaskStatus {
    PENDING,        // 等待执行
    RUNNING,        // 正在执行
    PAUSED,         // 已暂停
    COMPLETED,      // 已完成
    FAILED,         // 执行失败
    CANCELLED,      // 已取消
    INTERRUPTED     // 被中断（用户手动操作）
}

/**
 * 任务执行结果
 */
@Serializable
data class TaskResult(
    val status: TaskStatus,           // 执行状态
    val message: String = "",         // 结果消息
    val errorCode: Int = 0,           // 错误码
    val executedActions: Int = 0,     // 已执行操作数
    val totalActions: Int = 0,        // 总操作数
    val startTime: Long = 0,          // 开始时间
    val endTime: Long = 0,            // 结束时间
    val currentPageId: String = ""    // 当前页面ID
)

/**
 * 任务配置
 */
@Serializable
data class TaskConfig(
    val taskId: String,                           // 任务唯一标识
    val taskName: String,                         // 任务名称
    val appInfo: AppInfo,                         // 目标应用信息
    val pages: List<PageConfig>,                  // 页面配置列表
    val transitions: List<PageTransition>,        // 页面跳转配置
    val globalTimeout: Long = 60000,              // 全局超时时间（毫秒）
    val enableUserInterrupt: Boolean = true,     // 是否允许用户中断
    val interruptDetectInterval: Long = 500,      // 中断检测间隔（毫秒）
    val autoRetry: Boolean = true,                // 是否自动重试
    val maxGlobalRetry: Int = 3,                  // 全局最大重试次数
    val description: String = "",                 // 任务描述
    val version: String = "1.0",                  // 配置版本
    val author: String = "",                      // 作者
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val updateTime: Long = System.currentTimeMillis()  // 更新时间
) {
    /**
     * 根据页面ID获取页面配置
     */
    fun getPageConfig(pageId: String): PageConfig? {
        return pages.find { it.pageId == pageId }
    }
    
    /**
     * 获取指定页面的可能跳转目标页面
     */
    fun getPossibleNextPages(currentPageId: String): List<PageConfig> {
        val applicableTransitions = transitions.filter { it.fromPageId == currentPageId }
            .sortedByDescending { it.priority }
        
        val nextPageIds = mutableSetOf<String>()
        applicableTransitions.forEach { transition ->
            nextPageIds.addAll(transition.toPageIds)
        }
        
        // 如果没有定义跳转规则，返回空列表
        // 页面跳转应该完全依赖transitions配置
        
        return nextPageIds.mapNotNull { getPageConfig(it) }
    }
    
    /**
     * 获取所有目标页面
     */
    fun getTargetPages(): List<PageConfig> {
        return pages.filter { it.isTargetPageType() }
    }
    

    
    /**
     * 根据页面类型获取页面列表
     */
    fun getPagesByType(pageType: PageType): List<PageConfig> {
        return pages.filter { it.pageType == pageType }
    }
    
    /**
     * 验证任务配置的完整性
     */
    fun validate(): Boolean {
        // 检查是否至少有一个目标页面
        if (getTargetPages().isEmpty()) return false
        
        // 检查页面跳转规则的完整性
        transitions.forEach { transition ->
            // 检查源页面是否存在
            if (getPageConfig(transition.fromPageId) == null) return false
            
            // 检查目标页面是否都存在
            transition.toPageIds.forEach { toPageId ->
                if (getPageConfig(toPageId) == null) return false
            }
        }
        
        // 检查是否有目标页面类型的页面
        val hasTargetPage = pages.any { it.pageType == PageType.TARGET }
        if (!hasTargetPage) return false
        
        return true
    }
}