package com.alex.hopper.ui

sealed class AppRoute(val route: String) {
    data object Journal : AppRoute("journal")
    data object Camera : AppRoute("camera")
    data object Settings : AppRoute("settings")
    data object ReplacePhoto : AppRoute("camera/replace/{entryId}") {
        const val entryIdArg = "entryId"

        fun createRoute(entryId: Long): String = "camera/replace/$entryId"
    }
    data object Detail : AppRoute("detail/{entryId}") {
        const val entryIdArg = "entryId"

        fun createRoute(entryId: Long): String = "detail/$entryId"
    }

    data object Photo : AppRoute("photo/{entryId}") {
        const val entryIdArg = "entryId"

        fun createRoute(entryId: Long): String = "photo/$entryId"
    }
}
