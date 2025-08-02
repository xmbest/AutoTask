package com.xmbest.autask.model

import kotlinx.serialization.Serializable

/**
 * 任务操作类型枚举
 */
@Serializable
enum class ActionType {
    CLICK,          // 点击操作
    LONG_CLICK,     // 长按操作
    SWIPE,          // 滑动操作
    INPUT_TEXT,     // 输入文本
    WAIT,           // 等待操作
    BACK,           // 返回操作
    HOME,           // 回到桌面
    SCROLL,         // 滚动操作
    FIND_AND_CLICK, // 查找并点击
    CUSTOM          // 自定义操作
}

/**
 * 元素查找方式
 */
@Serializable
enum class FindType {
    BY_ID,          // 通过ID查找
    BY_TEXT,        // 通过文本查找
    BY_DESCRIPTION, // 通过描述查找
    BY_CLASS,       // 通过类名查找
    BY_XPATH,       // 通过XPath查找
    BY_COORDINATE,  // 通过坐标查找
    BY_ROUTE        // 通过路由查找
}

/**
 * 任务操作数据类
 */
@Serializable
data class TaskAction(
    val id: String,                    // 操作唯一标识
    val actionType: ActionType,        // 操作类型
    val findType: FindType? = null,    // 查找方式
    val target: String? = null,        // 目标元素标识（ID、文本、坐标等）
    val inputText: String? = null,     // 输入的文本内容
    val x: Int? = null,               // X坐标
    val y: Int? = null,               // Y坐标
    val endX: Int? = null,            // 滑动结束X坐标
    val endY: Int? = null,            // 滑动结束Y坐标
    val duration: Long = 500,         // 操作持续时间（毫秒）
    val timeout: Long = 5000,         // 操作超时时间（毫秒）
    val retryCount: Int = 3,          // 重试次数
    val description: String = "",     // 操作描述
    val isOptional: Boolean = false,  // 是否为可选操作（失败不影响整体流程）
    val waitAfter: Long = 1000        // 操作后等待时间（毫秒）
)