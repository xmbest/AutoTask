package com.xmbest.autask.executor

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.xmbest.autask.model.ActionType
import com.xmbest.autask.model.FindType
import com.xmbest.autask.model.TaskAction
import kotlinx.coroutines.delay

/**
 * 操作执行器
 * 负责执行具体的无障碍操作
 */
class ActionExecutor(private val accessibilityService: AccessibilityService) {
    
    companion object {
        private const val TAG = "ActionExecutor"
    }
    
    /**
     * 执行单个操作
     */
    suspend fun executeAction(action: TaskAction): Boolean {
        Log.d(TAG, "执行操作: ${action.description} (${action.actionType})")
        
        return try {
            when (action.actionType) {
                ActionType.CLICK -> executeClick(action)
                ActionType.LONG_CLICK -> executeLongClick(action)
                ActionType.SWIPE -> executeSwipe(action)
                ActionType.INPUT_TEXT -> executeInputText(action)
                ActionType.WAIT -> executeWait(action)
                ActionType.BACK -> executeBack()
                ActionType.HOME -> executeHome()
                ActionType.SCROLL -> executeScroll(action)
                ActionType.FIND_AND_CLICK -> executeFindAndClick(action)
                ActionType.CUSTOM -> executeCustom(action)
            }.also {
                if (it && action.waitAfter > 0) {
                    delay(action.waitAfter)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "执行操作失败: ${action.description}", e)
            false
        }
    }
    
    /**
     * 执行点击操作
     */
    private suspend fun executeClick(action: TaskAction): Boolean {
        val node = findTargetNode(action) ?: return false
        return performClick(node)
    }
    
    /**
     * 执行长按操作
     */
    private suspend fun executeLongClick(action: TaskAction): Boolean {
        val node = findTargetNode(action) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }
    
    /**
     * 执行滑动操作
     */
    private suspend fun executeSwipe(action: TaskAction): Boolean {
        val startX = action.x ?: return false
        val startY = action.y ?: return false
        val endX = action.endX ?: return false
        val endY = action.endY ?: return false
        
        return performSwipe(startX, startY, endX, endY, action.duration)
    }
    
    /**
     * 执行输入文本操作
     */
    private suspend fun executeInputText(action: TaskAction): Boolean {
        val node = findTargetNode(action) ?: return false
        val text = action.inputText ?: return false
        
        // 先聚焦到输入框
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        delay(200)
        
        // 清空现有文本
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        delay(200)
        
        // 输入新文本
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
    
    /**
     * 执行等待操作
     */
    private suspend fun executeWait(action: TaskAction): Boolean {
        delay(action.duration)
        return true
    }
    
    /**
     * 执行返回操作
     */
    private suspend fun executeBack(): Boolean {
        return accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }
    
    /**
     * 执行回到桌面操作
     */
    private suspend fun executeHome(): Boolean {
        return accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }
    
    /**
     * 执行滚动操作
     */
    private suspend fun executeScroll(action: TaskAction): Boolean {
        val node = findTargetNode(action) ?: accessibilityService.rootInActiveWindow
        return node?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
    }
    
    /**
     * 执行查找并点击操作
     */
    private suspend fun executeFindAndClick(action: TaskAction): Boolean {
        val node = findTargetNode(action) ?: return false
        return performClick(node)
    }
    
    /**
     * 执行自定义操作
     */
    private suspend fun executeCustom(action: TaskAction): Boolean {
        // 预留给自定义操作实现
        Log.w(TAG, "自定义操作暂未实现: ${action.description}")
        return true
    }
    
    /**
     * 查找目标节点
     */
    private fun findTargetNode(action: TaskAction): AccessibilityNodeInfo? {
        val rootNode = accessibilityService.rootInActiveWindow ?: return null
        
        return when (action.findType) {
            FindType.BY_ID -> findNodeById(rootNode, action.target ?: "")
            FindType.BY_TEXT -> findNodeByText(rootNode, action.target ?: "")
            FindType.BY_DESCRIPTION -> findNodeByDescription(rootNode, action.target ?: "")
            FindType.BY_CLASS -> findNodeByClass(rootNode, action.target ?: "")
            FindType.BY_COORDINATE -> findNodeByCoordinate(rootNode, action.x ?: 0, action.y ?: 0)
            FindType.BY_XPATH -> findNodeByXPath(rootNode, action.target ?: "")
            FindType.BY_ROUTE -> {
                // BY_ROUTE主要用于页面识别，在操作执行中不常用
                // 这里返回null，建议使用其他查找方式进行操作
                Log.w(TAG, "BY_ROUTE不适用于操作执行，请使用其他查找方式")
                null
            }
            null -> null
        }
    }
    
    /**
     * 通过ID查找节点
     */
    private fun findNodeById(rootNode: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
        return nodes?.firstOrNull()
    }
    
    /**
     * 通过文本查找节点
     */
    private fun findNodeByText(rootNode: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        return nodes?.firstOrNull()
    }
    
    /**
     * 通过描述查找节点
     */
    private fun findNodeByDescription(rootNode: AccessibilityNodeInfo, description: String): AccessibilityNodeInfo? {
        return findNodeRecursively(rootNode) { node ->
            node.contentDescription?.toString()?.contains(description) == true
        }
    }
    
    /**
     * 通过类名查找节点
     */
    private fun findNodeByClass(rootNode: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        return findNodeRecursively(rootNode) { node ->
            node.className?.toString()?.contains(className) == true
        }
    }
    
    /**
     * 通过坐标查找节点
     */
    private fun findNodeByCoordinate(rootNode: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        return findNodeRecursively(rootNode) { node ->
            val rect = Rect()
            node.getBoundsInScreen(rect)
            rect.contains(x, y)
        }
    }
    
    /**
     * 通过XPath查找节点
     * 支持简单的XPath语法，如：//TextView[@text='搜索']、//Button[contains(@text,'确定')]
     */
    private fun findNodeByXPath(rootNode: AccessibilityNodeInfo, xpath: String): AccessibilityNodeInfo? {
        try {
            return evaluateXPath(rootNode, xpath)
        } catch (e: Exception) {
            Log.w(TAG, "XPath查找失败: $xpath", e)
            return null
        }
    }
    
    /**
     * 执行XPath表达式
     */
    private fun evaluateXPath(rootNode: AccessibilityNodeInfo, xpath: String): AccessibilityNodeInfo? {
        // 移除开头的//
        val cleanXPath = xpath.removePrefix("//")
        
        // 解析XPath表达式
        val (className, conditions) = parseXPath(cleanXPath)
        
        return findNodeRecursively(rootNode) { node ->
            // 检查类名
            val nodeClassName = node.className?.toString() ?: ""
            val classMatches = if (className.isEmpty()) {
                true // 如果没有指定类名，匹配所有节点
            } else {
                nodeClassName.contains(className) || nodeClassName.endsWith(className)
            }
            
            if (!classMatches) {
                false
            } else {
                // 检查所有条件
                conditions.all { condition ->
                    evaluateCondition(node, condition)
                }
            }
        }
    }
    
    /**
     * 解析XPath表达式
     */
    private fun parseXPath(xpath: String): Pair<String, List<XPathCondition>> {
        val conditions = mutableListOf<XPathCondition>()
        
        // 查找类名和条件部分
        val bracketIndex = xpath.indexOf('[')
        val className = if (bracketIndex == -1) {
            xpath
        } else {
            val name = xpath.substring(0, bracketIndex)
            val conditionPart = xpath.substring(bracketIndex)
            
            // 解析条件
            parseConditions(conditionPart, conditions)
            
            name
        }
        
        return Pair(className, conditions)
    }
    
    /**
     * 解析XPath条件
     */
    private fun parseConditions(conditionPart: String, conditions: MutableList<XPathCondition>) {
        // 简单的条件解析，支持 [@attr='value'] 和 [contains(@attr,'value')] 格式
        val regex = Regex("\\[(.*?)\\]")
        val matches = regex.findAll(conditionPart)
        
        for (match in matches) {
            val condition = match.groupValues[1]
            
            when {
                condition.startsWith("contains(") -> {
                    // contains(@attr,'value') 格式
                    val containsRegex = Regex("contains\\(@(\\w+),'(.*?)'\\)")
                    val containsMatch = containsRegex.find(condition)
                    if (containsMatch != null) {
                        val attr = containsMatch.groupValues[1]
                        val value = containsMatch.groupValues[2]
                        conditions.add(XPathCondition(attr, value, true))
                    }
                }
                condition.contains("=") -> {
                    // @attr='value' 格式
                    val parts = condition.split("=")
                    if (parts.size == 2) {
                        val attr = parts[0].removePrefix("@")
                        var value = parts[1].trim()
                        // 移除外层的引号（单引号或双引号）
                        if ((value.startsWith("'") && value.endsWith("'")) || 
                            (value.startsWith('"') && value.endsWith('"'))) {
                            value = value.substring(1, value.length - 1)
                        }
                        conditions.add(XPathCondition(attr, value, false))
                    }
                }
            }
        }
    }
    
    /**
     * 评估XPath条件
     */
    private fun evaluateCondition(node: AccessibilityNodeInfo, condition: XPathCondition): Boolean {
        val actualValue = when (condition.attribute) {
            "text" -> node.text?.toString() ?: ""
            "contentDescription" -> node.contentDescription?.toString() ?: ""
            "id" -> node.viewIdResourceName ?: ""
            "class" -> node.className?.toString() ?: ""
            else -> ""
        }
        
        return if (condition.isContains) {
            actualValue.contains(condition.value)
        } else {
            actualValue == condition.value
        }
    }
    
    /**
     * XPath条件数据类
     */
    private data class XPathCondition(
        val attribute: String,
        val value: String,
        val isContains: Boolean
    )
    
    /**
     * 递归查找节点
     */
    private fun findNodeRecursively(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicate(node)) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeRecursively(child, predicate)
            if (result != null) {
                return result
            }
        }
        
        return null
    }
    
    /**
     * 执行点击操作
     */
    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        return if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            // 如果节点不可点击，尝试点击其父节点
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
            }
            false
        }
    }
    
    /**
     * 执行滑动手势
     */
    private fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean {
        val path = Path().apply {
            moveTo(startX.toFloat(), startY.toFloat())
            lineTo(endX.toFloat(), endY.toFloat())
        }
        
        val gestureBuilder = GestureDescription.Builder()
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, duration)
        gestureBuilder.addStroke(strokeDescription)
        
        return accessibilityService.dispatchGesture(gestureBuilder.build(), null, null)
    }
}