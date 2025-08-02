package com.xmbest.autask.executor

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.xmbest.autask.model.FindType
import com.xmbest.autask.model.PageConfig
import com.xmbest.autask.model.PageIdentifier
import kotlinx.coroutines.delay

/**
 * 页面识别器
 * 负责识别当前页面状态
 */
class PageRecognizer(private val accessibilityService: AccessibilityService) {

    companion object {
        private const val TAG = "PageRecognizer"
    }

    /**
     * 识别当前页面
     * @param pageConfigs 页面配置列表
     * @return 匹配的页面配置，如果没有匹配则返回null
     */
    suspend fun recognizeCurrentPage(pageConfigs: List<PageConfig>): PageConfig? {
        val rootNode = accessibilityService.rootInActiveWindow
        if (rootNode == null) {
            Log.w(TAG, "无法获取根节点")
            return null
        }

        // 遍历所有页面配置，找到匹配的页面
        for (pageConfig in pageConfigs) {
            if (isPageMatched(rootNode, pageConfig)) {
                Log.d(TAG, "识别到页面: ${pageConfig.pageName} (${pageConfig.pageId})")
                return pageConfig
            }
        }

        Log.d(TAG, "未识别到任何已配置的页面")
        return null
    }

    /**
     * 等待指定页面出现
     * @param pageConfig 目标页面配置
     * @param timeout 超时时间（毫秒）
     * @param checkInterval 检查间隔（毫秒）
     * @return 是否成功等待到目标页面
     */
    suspend fun waitForPage(
        pageConfig: PageConfig,
        timeout: Long = 10000,
        checkInterval: Long = 500
    ): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeout) {
            val rootNode = accessibilityService.rootInActiveWindow
            if (rootNode != null && isPageMatched(rootNode, pageConfig)) {
                Log.d(TAG, "成功等待到页面: ${pageConfig.pageName}")
                return true
            }
            delay(checkInterval)
        }

        Log.w(TAG, "等待页面超时: ${pageConfig.pageName}")
        return false
    }

    /**
     * 等待任意指定页面出现
     * @param pageConfigs 目标页面配置列表
     * @param timeout 超时时间（毫秒）
     * @param checkInterval 检查间隔（毫秒）
     * @return 匹配的页面配置，如果超时则返回null
     */
    suspend fun waitForAnyPage(
        pageConfigs: List<PageConfig>,
        timeout: Long = 10000,
        checkInterval: Long = 500
    ): PageConfig? {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeout) {
            val currentPage = recognizeCurrentPage(pageConfigs)
            if (currentPage != null) {
                return currentPage
            }
            delay(checkInterval)
        }

        Log.w(TAG, "等待任意页面超时")
        return null
    }

    /**
     * 检查页面是否匹配
     */
    internal fun isPageMatched(rootNode: AccessibilityNodeInfo, pageConfig: PageConfig): Boolean {
        // 页面识别条件是AND关系，所有条件都必须匹配
        return pageConfig.identifiers.all { identifier ->
            isIdentifierMatched(rootNode, identifier)
        }
    }

    /**
     * 检查单个识别条件是否匹配
     */
    private fun isIdentifierMatched(
        rootNode: AccessibilityNodeInfo,
        identifier: PageIdentifier
    ): Boolean {
        return when (identifier.findType) {
            FindType.BY_ID -> findNodeById(rootNode, identifier.value) != null
            FindType.BY_TEXT -> findNodeByText(rootNode, identifier.value) != null
            FindType.BY_DESCRIPTION -> findNodeByDescription(rootNode, identifier.value) != null
            FindType.BY_CLASS -> findNodeByClass(rootNode, identifier.value) != null
            FindType.BY_XPATH -> findNodeByXPath(rootNode, identifier.value) != null
            FindType.BY_COORDINATE -> {
                // 坐标格式: "x,y"
                val coords = identifier.value.split(",")
                if (coords.size == 2) {
                    val x = coords[0].toIntOrNull() ?: 0
                    val y = coords[1].toIntOrNull() ?: 0
                    findNodeByCoordinate(rootNode, x, y) != null
                } else {
                    false
                }
            }

            FindType.BY_ROUTE -> findNodeByRoute(rootNode, identifier.value) != null
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
    private fun findNodeByText(
        rootNode: AccessibilityNodeInfo,
        text: String
    ): AccessibilityNodeInfo? {
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        return nodes?.firstOrNull()
    }

    /**
     * 通过描述查找节点
     */
    private fun findNodeByDescription(
        rootNode: AccessibilityNodeInfo,
        description: String
    ): AccessibilityNodeInfo? {
        return findNodeRecursively(rootNode) { node ->
            node.contentDescription?.toString()?.contains(description) == true
        }
    }

    /**
     * 通过类名查找节点
     */
    private fun findNodeByClass(
        rootNode: AccessibilityNodeInfo,
        className: String
    ): AccessibilityNodeInfo? {
        return findNodeRecursively(rootNode) { node ->
            node.className?.toString()?.contains(className) == true
        }
    }

    /**
     * 通过坐标查找节点
     */
    private fun findNodeByCoordinate(
        rootNode: AccessibilityNodeInfo,
        x: Int,
        y: Int
    ): AccessibilityNodeInfo? {
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
    private fun findNodeByXPath(
        rootNode: AccessibilityNodeInfo,
        xpath: String
    ): AccessibilityNodeInfo? {
        try {
            return evaluateXPath(rootNode, xpath)
        } catch (e: Exception) {
            Log.w(TAG, "XPath查找失败: $xpath", e)
            return null
        }
    }

    /**
     * 执行XPath表达式
     * 支持简单的XPath语法：
     * //ClassName - 查找所有指定类名的节点
     * //ClassName[@text='value'] - 查找指定类名且文本为value的节点
     * //ClassName[@contentDescription='value'] - 查找指定类名且描述为value的节点
     * //ClassName[contains(@text,'value')] - 查找指定类名且文本包含value的节点
     * //ClassName[contains(@contentDescription,'value')] - 查找指定类名且描述包含value的节点
     */
    private fun evaluateXPath(
        rootNode: AccessibilityNodeInfo,
        xpath: String
    ): AccessibilityNodeInfo? {
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
     * 返回类名和条件列表
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
                            (value.startsWith('"') && value.endsWith('"'))
                        ) {
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
     * 获取当前页面的调试信息
     */
    fun getCurrentPageDebugInfo(): String {
        val rootNode = accessibilityService.rootInActiveWindow ?: return "No root node available"
        val packageName = rootNode.packageName?.toString() ?: "Unknown"
        val className = rootNode.className?.toString() ?: "Unknown"
        
        val textNodes = mutableListOf<String>()
        collectTextNodes(rootNode, textNodes)
        
        // 收集前几层的类名信息
        val classHierarchy = mutableListOf<String>()
        fun collectClassNames(node: AccessibilityNodeInfo, depth: Int, maxDepth: Int = 3) {
            if (depth > maxDepth) return
            val nodeClass = node.className?.toString()
            if (!nodeClass.isNullOrEmpty()) {
                classHierarchy.add("${"".repeat(depth * 2)}$nodeClass")
            }
            for (i in 0 until node.childCount.coerceAtMost(5)) { // 限制子节点数量
                val child = node.getChild(i)
                if (child != null) {
                    collectClassNames(child, depth + 1, maxDepth)
                    child.recycle()
                }
            }
        }
        collectClassNames(rootNode, 0)
        
        return buildString {
            appendLine("=== 当前页面调试信息 ===")
            appendLine("包名: $packageName")
            appendLine("根节点类名: $className")
            appendLine("")
            appendLine("类层次结构:")
            classHierarchy.take(15).forEach { classInfo ->
                appendLine(classInfo)
            }
            if (classHierarchy.size > 15) {
                appendLine("  ... 还有 ${classHierarchy.size - 15} 个类")
            }
            appendLine("")
            appendLine("可见文本节点:")
            textNodes.take(10).forEach { text ->
                appendLine("  - $text")
            }
            if (textNodes.size > 10) {
                appendLine("  ... 还有 ${textNodes.size - 10} 个文本节点")
            }
            appendLine("")
            appendLine("=== 路由匹配建议 ===")
            appendLine("1. 包名匹配: $packageName")
            appendLine("2. Activity匹配: ${className.substringAfterLast('.')}")
            appendLine("3. 包名+Activity: $packageName/${className.substringAfterLast('.')}")
        }
    }

    /**
     * 收集文本节点
     */
    private fun collectTextNodes(node: AccessibilityNodeInfo, textNodes: MutableList<String>) {
        if (node.isVisibleToUser && !node.text.isNullOrEmpty()) {
            textNodes.add(node.text.toString())
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTextNodes(child, textNodes)
        }
    }

    /**
     * 通过路由查找AccessibilityNodeInfo节点
     * 支持多种路由格式：
     * 1. 绝对路径：android.widget.LinearLayout[0]\android.widget.Button[1]
     * 2. 资源ID：@resource-id=com.example:id/button#
     * 3. 文本内容：@text=按钮文本#
     * 4. 内容描述：@content-desc=按钮描述#
     * 5. 相对路径：. (当前节点) .. (父节点)
     */
    private fun findNodeByRoute(rootNode: AccessibilityNodeInfo, router: String): AccessibilityNodeInfo? {
        return rootNode.findNodeBySplit(router)
    }

    /**
     * 扩展函数：通过路由分割查找节点
     */
    private fun AccessibilityNodeInfo.findNodeBySplit(router: String, index: Int = -1): AccessibilityNodeInfo? {
        try {
            Log.d(TAG, "findNodeBySplit.router=$router")
            
            // 先去除开头的分割符
            val realRouter = if (router.startsWith("\\")) {
                router.substring(1, router.length)
            } else {
                router
            }
            
            // 处理自身返回
            if ((realRouter == "") or (realRouter == ".")) {
                return this
            }
            
            // 处理父节点
            if (realRouter == "..") {
                if (parent == null) return null
                return parent.findNodeBySplit(".")
            }
            
            // 处理回溯路径
            if (realRouter.startsWith("..")) {
                if (parent == null) return null
                val backCallResult = backCall(realRouter, this)
                return backCallResult.root.findNodeBySplit(backCallResult.router)
            }
            
            // 处理相对路径
            if (realRouter.startsWith(".")) {
                return findNodeBySplit(router.substring(1))
            }
            
            val childSize = childCount
            
            // 深度查找子节点
            fun depthChild(childRouter: String): AccessibilityNodeInfo? {
                if (childSize == 0) {
                    Log.d(TAG, "findNodeBySplit noChild")
                    return null
                }
                for (i in 0 until childSize) {
                    val targetChild = getChild(i)?.findNodeBySplit(childRouter, i)
                    if (targetChild != null && targetChild.isVisibleToUser) {
                        Log.d(TAG, "findNodeBySplit Success")
                        return targetChild
                    }
                }
                return null
            }
            
            // 根据属性查找
            fun depthAttr(split: String, attr: String?): AccessibilityNodeInfo? {
                // 为空, 直接往下找
                if (attr.isNullOrEmpty()) {
                    val child = depthChild(realRouter)
                    return child
                } else {
                    // 先拼接
                    val realAttr = split + attr
                    Log.d(TAG, "depthAttr realAttr=$realAttr")
                    if (realRouter.startsWith(realAttr)) {
                        // 判断是否是当前node的id开头
                        val childRouter = realRouter.substring(realAttr.length)
                        if (childRouter == "" || childRouter == "." || childRouter == "\\.") {
                            return this
                        }
                        if (childRouter == "..") {
                            if (parent == null) return null
                            return parent
                        }
                        val child = depthChild(childRouter)
                        return child
                    } else {
                        // 不是以当前节点开头的, 不改变router,继续往下走
                        val child = depthChild(realRouter)
                        return child
                    }
                }
            }
            
            // 先比较是否与当前节点相同
            if (realRouter.startsWith("@resource-id=")) {
                val child = depthAttr("@resource-id=", "$viewIdResourceName#")
                return child
            }
            
            if (realRouter.startsWith("@text=")) {
                val child = depthAttr("@text=", "${text?.toString() ?: ""}#")
                return child
            }
            
            if (realRouter.startsWith("@content-desc=")) {
                val child = depthAttr("@content-desc=", "${contentDescription?.toString() ?: ""}#")
                return child
            }
            
            // 判断绝对路径
            val childIndex = if (index == -1) {
                getNodeIndexInParent(this)
            } else {
                index
            }
            val absoluteRouter = "$className[$childIndex]"
            
            // 先移除本身的router
            var childRouter: String? = null
            if (realRouter.startsWith(absoluteRouter)) {
                childRouter = realRouter.substring(absoluteRouter.length)
            } else {
                return null
            }
            
            // 移除完后, childRouter为空字符串, 直接返回
            if (childRouter == "" || childRouter == ".\\" || childRouter == "\\." || childRouter == ".") {
                Log.d(TAG, "findNodeBySplit find success")
                return this
            }
            
            // 再找到child哪个符合
            for (i in 0 until childSize) {
                val targetChild = getChild(i)?.findNodeBySplit(childRouter, i)
                if (targetChild != null) {
                    Log.d(TAG, "findNodeBySplit,find success")
                    return targetChild
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "unSupport router[$router] error: ${e.message}")
        }
        return null
    }

    /**
     * 获取节点在父节点中的索引
     */
    private fun getNodeIndexInParent(node: AccessibilityNodeInfo): Int {
        val parent = node.parent ?: return 0
        for (i in 0 until parent.childCount) {
            val child = parent.getChild(i)
            if (child == node) {
                return i
            }
        }
        return 0
    }

    /**
     * 回溯路径处理
     */
    private data class BackCallResult(
        val root: AccessibilityNodeInfo,
        val router: String
    )

    private fun backCall(router: String, currentNode: AccessibilityNodeInfo): BackCallResult {
        var node = currentNode
        var remainingRouter = router
        
        while (remainingRouter.startsWith("..")) {
            node = node.parent ?: break
            remainingRouter = if (remainingRouter.length > 2) {
                remainingRouter.substring(2)
            } else {
                ""
            }
            
            // 移除可能的分隔符
            if (remainingRouter.startsWith("\\")) {
                remainingRouter = remainingRouter.substring(1)
            }
        }
        
        return BackCallResult(node, remainingRouter)
    }
}