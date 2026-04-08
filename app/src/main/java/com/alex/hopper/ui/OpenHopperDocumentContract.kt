package com.alex.hopper.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts

class OpenHopperDocumentContract : ActivityResultContracts.OpenDocument() {
    override fun createIntent(
        context: Context,
        input: Array<String>,
    ): Intent = super.createIntent(context, input).apply {
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, downloadsInitialUri())
    }

    private fun downloadsInitialUri(): Uri =
        DocumentsContract.buildRootUri(
            DOWNLOADS_DOCUMENTS_AUTHORITY,
            DOWNLOADS_ROOT_ID,
        )

    private companion object {
        const val DOWNLOADS_DOCUMENTS_AUTHORITY = "com.android.providers.downloads.documents"
        const val DOWNLOADS_ROOT_ID = "downloads"
    }
}
