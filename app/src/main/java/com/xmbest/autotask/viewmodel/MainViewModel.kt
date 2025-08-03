package com.xmbest.autotask.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmbest.autask.api.AutoTaskApi
import com.xmbest.autask.model.TaskConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _tasks = MutableStateFlow<List<TaskConfig>>(emptyList())
    val tasks: StateFlow<List<TaskConfig>> = _tasks.asStateFlow()

    // 使用AutoTaskApi的权限状态Flow
    val isAccessibilityEnabled: StateFlow<Boolean> = AutoTaskApi.isAccessibilityEnabled

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 加载任务列表
     */
    fun loadTasks() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val taskList = AutoTaskApi.getAllTasks()
                _tasks.value = taskList
            } catch (e: Exception) {
                _errorMessage.value = "加载任务失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载预设任务
     */
    fun loadPresetTasks() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val presetTasks = AutoTaskApi.getPresetTasks()

                // 添加预设任务到任务管理器
                presetTasks.forEach { task ->
                    AutoTaskApi.addTask(task)
                }

                // 重新加载任务列表
                loadTasks()
            } catch (e: Exception) {
                _errorMessage.value = "加载预设任务失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 打开无障碍设置
     */
    fun openAccessibilitySettings(context: Context) {
        AutoTaskApi.requestAccessibilityPermission(context)
    }

    /**
     * 开始执行任务
     */
    fun startTask(taskId: String) {
        viewModelScope.launch {
            try {
                val success = AutoTaskApi.startTask(taskId)
                if (success) {
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "启动任务失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = "启动任务失败: ${e.message}"
            }
        }
    }

    /**
     * 停止任务
     */
    fun stopTask(taskId: String) {
        viewModelScope.launch {
            try {
                val success = AutoTaskApi.stopTask(taskId)
                if (success) {
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "停止任务失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = "停止任务失败: ${e.message}"
            }
        }
    }

    /**
     * 暂停任务
     */
    fun pauseTask(taskId: String) {
        viewModelScope.launch {
            try {
                val success = AutoTaskApi.pauseTask(taskId)
                if (success) {
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "暂停任务失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = "暂停任务失败: ${e.message}"
            }
        }
    }

    /**
     * 恢复任务
     */
    fun resumeTask(taskId: String) {
        viewModelScope.launch {
            try {
                val success = AutoTaskApi.resumeTask(taskId)
                if (success) {
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "恢复任务失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = "恢复任务失败: ${e.message}"
            }
        }
    }

    /**
     * 删除任务
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                val success = AutoTaskApi.removeTask(taskId)
                if (success) {
                    loadTasks() // 重新加载任务列表
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "删除任务失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = "删除任务失败: ${e.message}"
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}