package com.xmbest.autask.model

import kotlinx.serialization.Serializable

/**
 * 执行自动化的应用信息
 * [packageName] 应用包名
 * [minVersion] - [maxVersion] 支持的版本区间
 */

@Serializable
data class AppInfo(
    val packageName: String,
    val minVersion: String,
    val maxVersion: String
)
