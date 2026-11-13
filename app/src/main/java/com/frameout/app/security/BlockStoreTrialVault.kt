package com.frameout.app.security

import android.content.Context
import com.frameout.core.pro.TrialVault
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.BlockstoreClient
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.StoreBytesData

internal class BlockStoreTrialVault(context: Context) : TrialVault {
    private val client: BlockstoreClient = Blockstore.getClient(context.applicationContext)

    override fun read(onResult: (ByteArray?) -> Unit) {
        val request = RetrieveBytesRequest.Builder().setKeys(listOf(KEY)).build()
        client.retrieveBytes(request)
            .addOnSuccessListener { result -> onResult(result.blockstoreDataMap[KEY]?.bytes ?: ByteArray(0)) }
            .addOnFailureListener { onResult(null) }
    }

    override fun write(bytes: ByteArray, onDone: (Boolean) -> Unit) {
        val data = StoreBytesData.Builder().setKey(KEY).setBytes(bytes).setShouldBackupToCloud(true).build()
        client.storeBytes(data)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    private companion object {
        const val KEY = "frameout.skip.trial"
    }
}
