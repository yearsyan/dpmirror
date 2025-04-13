package io.github.tsioam.mirror.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream

fun copyAssetToFile(context: Context, assetName: String, targetPath: String) {
    context.assets.open(assetName).use { input ->
        val outFile = File(targetPath)
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { output ->
            input.copyTo(output)
        }
    }
}
