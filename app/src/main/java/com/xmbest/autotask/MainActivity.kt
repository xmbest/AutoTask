package com.xmbest.autotask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xmbest.autotask.ui.theme.AutoTaskTheme
import com.xmbest.autotask.viewmodel.MainViewModel
import com.xmbest.autask.model.TaskConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoTaskTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadTasks()
        viewModel.startAccessibilityMonitoring(context)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAccessibilityMonitoring()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AutoTask 自动化任务") },
                actions = {
                    IconButton(onClick = { viewModel.openAccessibilitySettings(context) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.loadPresetTasks() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加任务")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 权限状态卡片
            PermissionStatusCard(
                isEnabled = isAccessibilityEnabled,
                onRequestPermission = { viewModel.openAccessibilitySettings(context) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 任务列表
            Text(
                text = "任务列表",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (tasks.isEmpty()) {
                EmptyTasksView(onLoadPresets = { viewModel.loadPresetTasks() })
            } else {
                LazyColumn {
                    items(tasks) { task ->
                        TaskItem(
                            task = task,
                            onStartTask = { viewModel.startTask(it) },
                            onStopTask = { viewModel.stopTask(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionStatusCard(
    isEnabled: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (isEnabled) "✓ 无障碍服务已启用" else "⚠ 无障碍服务未启用",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (!isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "需要启用无障碍服务才能执行自动化任务",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("去设置")
                }
            }
        }
    }
}

@Composable
fun EmptyTasksView(
    onLoadPresets: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "暂无任务",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onLoadPresets
        ) {
            Text("加载预设任务")
        }
    }
}

@Composable
fun TaskItem(
    task: TaskConfig,
    onStartTask: (String) -> Unit,
    onStopTask: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.taskName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                         text = task.appInfo.packageName,
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                    if (task.description.isNotEmpty()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                IconButton(
                    onClick = { onStartTask(task.taskId) }
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "开始任务",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
