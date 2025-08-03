package com.xmbest.autask.factory

import com.xmbest.autask.model.*

/**
 * 任务配置工厂
 * 用于创建常见的自动化任务配置
 */
object TaskConfigFactory {

    fun createQQMusicSearchTask(): TaskConfig {
        val appInfo = AppInfo(
            packageName = "com.tencent.qqmusiccar",
            minVersion = "6.0.0",
            maxVersion = "6.1.70"
        )

        // 首页
        val homePage = PageConfig(
            pageId = "home",
            pageName = "首页",
            pageType = PageType.INTERMEDIATE,
            identifiers = listOf(
                PageIdentifier(
                    FindType.BY_ID,
                    value = "com.tencent.qqmusiccar:id/search_input_animation_layout",
                    description = "搜索按钮"
                )
            ),
            actions = listOf(
                TaskAction(
                    id = "click_search",
                    actionType = ActionType.FIND_AND_CLICK,
                    findType = FindType.BY_ID,
                    target = "com.tencent.qqmusiccar:id/search_input_animation_layout",
                    description = "点击搜索栏",
                    timeout = 5000
                )
            )
        )

        // 搜索页
        val searchPage = PageConfig(
            pageId = "search",
            pageName = "搜索页",
            pageType = PageType.INTERMEDIATE,
            identifiers = listOf(
                PageIdentifier(
                    FindType.BY_ID,
                    "com.tencent.qqmusiccar:id/etSearch",
                    "搜索输入框"
                ),
                PageIdentifier(
                    findType = FindType.BY_ID,
                    "com.tencent.qqmusiccar:id/history_view"
                )
            ),
            actions = listOf(
                TaskAction(
                    id = "input_keyword",
                    actionType = ActionType.INPUT_TEXT,
                    findType = FindType.BY_ID,
                    target = "com.tencent.qqmusiccar:id/etSearch",
                    inputText = "刘德华",
                    description = "输入搜索关键词",
                    timeout = 3000
                )
            )
        )

        // 播放详情页
        val playInfo = PageConfig(
            pageId = "playInfo",
            pageName = "播放详情页",
            pageType = PageType.INTERMEDIATE,
            identifiers = listOf(
                PageIdentifier(
                    FindType.BY_ID,
                    "com.tencent.qqmusiccar:id/inner_lyric_view",
                    "歌词"
                ),
                PageIdentifier(
                    FindType.BY_ID,
                    "com.tencent.qqmusiccar:id/back",
                    "放回"
                )
            ),
            actions = listOf(
                TaskAction(
                    id = "back",
                    actionType = ActionType.FIND_AND_CLICK,
                    findType = FindType.BY_ID,
                    target = "com.tencent.qqmusiccar:id/back",
                    description = "退出详情页",
                    timeout = 3000
                )
            )
        )


        // 搜索页候选页
        val selectItem = PageConfig(
            pageId = "selectItem",
            pageName = "选中候选列表",
            pageType = PageType.INTERMEDIATE,
            identifiers = listOf(
                PageIdentifier(
                    FindType.BY_ID,
                    "com.tencent.qqmusiccar:id/smartSearchName",
                    "选中候选"
                ),
                PageIdentifier(
                    FindType.BY_ID,
                    "com.tencent.qqmusiccar:id/smartRecyclerView",
                    "候选列表"
                )
            ),
            actions = listOf(
                TaskAction(
                    id = "input_keyword",
                    actionType = ActionType.FIND_AND_CLICK,
                    findType = FindType.BY_ID,
                    target = "com.tencent.qqmusiccar:id/smartSearchName",
                    description = "选中候选",
                    timeout = 3000
                )
            )
        )

        // 搜索结果页，播放第一首
        val result = PageConfig(
            pageId = "result",
            pageName = "结果页",
            pageType = PageType.TARGET,
            identifiers = listOf(
                PageIdentifier(
                    FindType.BY_ID,
                    value = "com.tencent.qqmusiccar:id/searchTab",
                    description = "分类按钮列表"
                ),
                PageIdentifier(
                    FindType.BY_ID,
                    value = "com.tencent.qqmusiccar:id/songRecyclerView",
                    description = "搜索结果"
                )
            ),
            actions = listOf(
                TaskAction(
                    id = "click_result",
                    actionType = ActionType.FIND_AND_CLICK,
                    findType = FindType.BY_ID,
                    target = "com.tencent.qqmusiccar:id/song_info_item_name",
                    description = "点击结果",
                    timeout = 5000
                )
            )
        )

        return TaskConfig(
            taskId = "qq_music_search",
            taskName = "qq音乐搜歌",
            appInfo = appInfo,
            pages = listOf(homePage, searchPage, selectItem, playInfo, result),
            transitions = emptyList(),
            description = "自动搜索并播放qq音乐中的歌曲",
            author = "AutoTask"
        )
    }

    fun createSettingsSearchTask(): TaskConfig {
        val appInfo = AppInfo("com.android.settings", "", "")

        val home = PageConfig(
            pageId = "home",
            pageName = "首页",
            pageType = PageType.INTERMEDIATE,
            identifiers = listOf(
                PageIdentifier(
                    FindType.BY_TEXT,
                    value = "我的设备",
                    description = "设置页"
                )
            ),
            actions = listOf(
                TaskAction(
                    id = "click_search",
                    actionType = ActionType.FIND_AND_CLICK,
                    findType = FindType.BY_ID,
                    target = "com.android.settings:id/search_mode_stub",
                    description = "点击搜索栏",
                    timeout = 5000
                )
            )
        )

        val searchPage = PageConfig(
            pageId = "searchPage",
            pageName = "搜索页",
            pageType = PageType.INTERMEDIATE,
            identifiers = listOf(
                PageIdentifier(
                    FindType.BY_ID,
                    value = "com.android.settings:id/search_panel",
                    description = "搜索框"
                ),
                PageIdentifier(
                    FindType.BY_ID,
                    value = "com.android.settings:id/search_history_tv",
                    description = "搜索记录标题"
                )
            ),
            actions = listOf(
                TaskAction(
                    id = "input_text_bluetooth",
                    actionType = ActionType.INPUT_TEXT,
                    findType = FindType.BY_CLASS,
                    target = "android.widget.EditText",
                    description = "输入蓝牙",
                    inputText = "蓝牙",
                    timeout = 3000
                )
            )
        )

        val searchResult = PageConfig(
            pageId = "searchResult",
            pageName = "搜索页候选结果页",
            pageType = PageType.INTERMEDIATE,
            identifiers = listOf(
                PageIdentifier(
                    FindType.BY_ID,
                    value = "com.android.settings:id/search_result",
                    description = "搜索结果"
                )
            ),
            actions = listOf(
                TaskAction(
                    id = "click_result",
                    actionType = ActionType.FIND_AND_CLICK,
                    findType = FindType.BY_ID,
                    target = "com.android.settings:id/settings_search_item_name",
                    description = "选中第一个",
                    timeout = 3000
                )
            )
        )

        val result = PageConfig(
            pageId = "result",
            pageName = "蓝牙页面",
            pageType = PageType.TARGET,
            identifiers = listOf(
                PageIdentifier(
                    FindType.BY_TEXT,
                    value = "设备名称",
                    description = "设备名称"
                ),
                PageIdentifier(
                    FindType.BY_TEXT,
                    value = "蓝牙",
                    description = "蓝牙标题"
                )
            ),
            actions = listOf(
                TaskAction(
                    id = "click_switch",
                    actionType = ActionType.FIND_AND_CLICK,
                    findType = FindType.BY_CLASS,
                    target = "android.widget.Switch",
                    description = "点击开关",
                    timeout = 3000
                )
            )
        )


        return TaskConfig(
            taskId = "settings_search_switch_bluetooth",
            taskName = "通过搜索开关蓝牙",
            appInfo = appInfo,
            pages = listOf(home, searchPage, searchResult, result),
            transitions = emptyList(),
            description = "自动搜索选中",
            author = "AutoTask"
        )
    }

    /**
     * 获取所有预设任务配置
     */
    fun getAllPresetTasks(): List<TaskConfig> {
        return listOf(
            createQQMusicSearchTask(),
            createSettingsSearchTask()
        )
    }
}