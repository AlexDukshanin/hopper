package com.alex.xdw.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

enum class AppIconMode {
    Yellow,
    Gray,
    Green,
}

class AppIconManager(
    private val context: Context,
) {
    fun apply(mode: AppIconMode) {
        val packageManager = context.packageManager
        val aliases = mapOf(
            AppIconMode.Yellow to "${context.packageName}.LauncherYellowAlias",
            AppIconMode.Gray to "${context.packageName}.LauncherGrayAlias",
            AppIconMode.Green to "${context.packageName}.LauncherGreenAlias",
        )

        aliases.forEach { (iconMode, className) ->
            val state = if (iconMode == mode) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                ComponentName(context, className),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
