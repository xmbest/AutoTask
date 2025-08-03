package com.xmbest.autask.model

import kotlinx.serialization.Serializable

/**
 * 页面类型枚举
 * 用于标识页面的基本类型，可由集成应用扩展
 */
@Serializable
enum class PageType {
    INTERMEDIATE,   // 中间页面
    TARGET          // 目标页面
}

/**
 * 页面识别条件
 */
@Serializable
data class PageIdentifier(
    val findType: FindType,           // 查找方式
    val value: String,                // 查找值
    val description: String = ""      // 描述
)

/**
 * 页面配置
 * 基于唯一标识的灵活页面配置系统
 */
@Serializable
data class PageConfig(
    val pageId: String,                           // 页面唯一标识
    val pageName: String,                         // 页面名称
    val pageType: PageType = PageType.INTERMEDIATE, // 页面类型
    val identifiers: List<PageIdentifier>,        // 页面识别条件（多个条件AND关系）
    val actions: List<TaskAction>,                // 该页面需要执行的操作列表
    val maxRetryCount: Int = 3,                   // 页面最大重试次数
    val pageTimeout: Long = 10000,                // 页面超时时间（毫秒）
    val customProperties: Map<String, String> = emptyMap(), // 自定义属性，供集成应用使用
    val description: String = ""                  // 页面描述
) {
    /**
     * 检查是否为目标页面
     */
    fun isTargetPageType(): Boolean = pageType == PageType.TARGET
    
    /**
     * 获取自定义属性值
     */
    fun getCustomProperty(key: String): String? = customProperties[key]
}

/**
 * 页面跳转规则
 * 支持动态页面跳转和条件判断
 */
@Serializable
data class PageTransition(
    val transitionId: String,                     // 跳转规则唯一标识
    val fromPageId: String,                       // 源页面ID
    val toPageIds: List<String>,                  // 可能的目标页面ID列表
    val priority: Int = 0,                        // 优先级（数字越大优先级越高）
    val description: String = ""                  // 描述
)