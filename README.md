# AutoTask - Android 自动化任务框架

## 项目简介

AutoTask 是一个基于 Android 无障碍服务的自动化任务执行框架，可以帮助用户自动化执行各种应用操作，如搜歌、发送消息等。

## 功能特性

- 🎯 **页面识别**: 智能识别当前应用页面状态
- 🔄 **任务流程**: 支持复杂的多页面操作流程
- ⚡ **操作执行**: 支持点击、滑动、输入文本等操作
- 🛡️ **错误处理**: 完善的重试机制和错误恢复
- 🎮 **用户控制**: 支持任务暂停、恢复、中断
- 📱 **预设任务**: 内置常用应用的自动化任务

## 项目架构

### 模块结构

```
AutoTask/
├── app/                    # 主应用模块
│   ├── MainActivity.kt     # 主界面
│   └── viewmodel/         # ViewModel层
├── core/                   # 核心业务逻辑模块
│   ├── model/             # 数据模型
│   ├── executor/          # 执行器
│   ├── manager/           # 任务管理
│   ├── service/           # 无障碍服务
│   └── api/               # 对外API
└── common/                 # 通用工具模块
    └── utils/             # 工具类
```

### 核心组件

#### 1. 数据模型 (model)
- **TaskConfig**: 任务配置，定义完整的自动化流程
- **PageConfig**: 页面配置，定义页面识别和操作
- **TaskAction**: 操作定义，包含具体的执行动作
- **AppInfo**: 应用信息，目标应用的包名和版本

#### 2. 执行器 (executor)
- **TaskExecutor**: 任务执行器，管理整个任务生命周期
- **ActionExecutor**: 操作执行器，执行具体的UI操作
- **PageRecognizer**: 页面识别器，识别当前页面状态

#### 3. 管理器 (manager)
- **TaskManager**: 任务管理器，负责任务的存储和调度

#### 4. 服务 (service)
- **AutoAccessibilityService**: 无障碍服务，提供系统级操作能力

## 使用示例

### 1. 网易云音乐搜歌任务

```kotlin
val searchTask = TaskConfigFactory.createNeteaseMusicSearchTask()
AutoTaskApi.addTask(searchTask)
AutoTaskApi.startTask(searchTask.taskId)
```

### 2. 微信发送消息任务

```kotlin
val messageTask = TaskConfigFactory.createWeChatSendMessageTask()
AutoTaskApi.addTask(messageTask)
AutoTaskApi.startTask(messageTask.taskId)
```

### 3. 自定义任务

```kotlin
val customTask = TaskConfig(
    taskId = "custom_task_001",
    taskName = "自定义任务",
    targetApp = AppInfo(
        packageName = "com.example.app",
        minVersion = "1.0.0",
        maxVersion = "2.0.0"
    ),
    pages = listOf(
        PageConfig(
            pageId = "home",
            pageName = "首页",
            pageState = PageState.HOME,
            identifiers = listOf(
                PageIdentifier(FindType.BY_ID, "home_button")
            ),
            actions = listOf(
                TaskAction(
                    id = "click_search",
                    actionType = ActionType.CLICK,
                    findType = FindType.BY_ID,
                    target = "search_button"
                )
            ),
            nextPageId = "search"
        )
        // ... 更多页面配置
    ),

)

AutoTaskApi.addTask(customTask)
```

## 快速开始

### 1. 权限配置

确保在 `AndroidManifest.xml` 中声明了无障碍服务：

```xml
<service
    android:name="com.xmbest.autask.service.AutoAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

### 2. 启用无障碍服务

1. 打开系统设置
2. 进入「无障碍」或「辅助功能」
3. 找到「AutoTask」并启用

### 3. 运行应用

1. 启动 AutoTask 应用
2. 检查无障碍服务状态
3. 加载预设任务或创建自定义任务
4. 点击播放按钮开始执行

## API 文档

### AutoTaskApi

主要的对外接口，提供任务管理和执行功能：

```kotlin
// 权限检查
AutoTaskApi.isAccessibilityServiceEnabled(context): Boolean
AutoTaskApi.requestAccessibilityPermission(context)

// 任务管理
AutoTaskApi.addTask(taskConfig: TaskConfig): Boolean
AutoTaskApi.getTask(taskId: String): TaskConfig?
AutoTaskApi.getAllTasks(): List<TaskConfig>
AutoTaskApi.removeTask(taskId: String): Boolean

// 任务执行
AutoTaskApi.startTask(taskId: String): Boolean
AutoTaskApi.stopTask(taskId: String): Boolean
AutoTaskApi.pauseTask(taskId: String): Boolean
AutoTaskApi.resumeTask(taskId: String): Boolean

// 预设任务
AutoTaskApi.getPresetTasks(): List<TaskConfig>
```

## 注意事项

1. **权限要求**: 需要用户手动启用无障碍服务权限
2. **应用兼容性**: 不同版本的目标应用可能需要调整配置
3. **性能影响**: 无障碍服务会对系统性能产生一定影响
4. **安全考虑**: 请谨慎使用，避免在敏感应用中执行自动化操作

## 开发指南

### 添加新的预设任务

1. 在 `TaskConfigFactory` 中添加新的工厂方法
2. 定义页面配置和操作流程
3. 测试任务执行流程
4. 更新文档

### 扩展操作类型

1. 在 `ActionType` 枚举中添加新类型
2. 在 `ActionExecutor` 中实现对应的执行逻辑
3. 更新相关文档

## 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

## 贡献

欢迎提交 Issue 和 Pull Request 来改进项目！

## 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 GitHub Issue
- 发送邮件至项目维护者

---

**免责声明**: 本框架仅供学习和研究使用，请遵守相关法律法规和应用服务条款。