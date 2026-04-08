package com.alex.hopper.ui

sealed class AppRoute(val route: String) {
    data object Collections : AppRoute("collections")

    data object Journal : AppRoute("journal/{collectionId}") {
        const val collectionIdArg = "collectionId"

        fun createRoute(collectionId: Long): String = "journal/$collectionId"
    }

    data object Camera : AppRoute("camera/{collectionId}") {
        const val collectionIdArg = "collectionId"

        fun createRoute(collectionId: Long): String = "camera/$collectionId"
    }

    data object SearchGlobal : AppRoute("search/global")

    data object SearchCollection : AppRoute("search/collection/{collectionId}") {
        const val collectionIdArg = "collectionId"

        fun createRoute(collectionId: Long): String = "search/collection/$collectionId"
    }

    data object Settings : AppRoute("settings")

    data object ScanFrameEditor : AppRoute("settings/scan-frame-editor")

    data object ShareQr : AppRoute("share/qr")

    data object ImportQr : AppRoute("import/qr")

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
