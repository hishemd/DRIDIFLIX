package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.ExternalPlayerType

object ExternalPlayerHelper {

    fun openInPlayer(
        context: Context,
        playerType: ExternalPlayerType,
        streamUrl: String,
        title: String
    ) {
        when (playerType) {
            ExternalPlayerType.VLC -> openVlc(context, streamUrl, title)
            ExternalPlayerType.MX_PLAYER -> openMxPlayer(context, streamUrl, title)
            ExternalPlayerType.JUST_PLAYER -> openJustPlayer(context, streamUrl, title)
            ExternalPlayerType.SYSTEM -> openSystemChooser(context, streamUrl, title)
            ExternalPlayerType.BUILT_IN -> {
                // Handled in UI navigation
            }
        }
    }

    fun openVlc(context: Context, streamUrl: String, title: String) {
        val uri = Uri.parse(streamUrl)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndTypeAndNormalize(uri, "video/*")
            setPackage("org.videolan.vlc")
            putExtra("title", title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Try vlc:// URL scheme or generic chooser
            try {
                val vlcUri = Uri.parse("vlc://$streamUrl")
                val vlcIntent = Intent(Intent.ACTION_VIEW, vlcUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(vlcIntent)
            } catch (e2: Exception) {
                fallbackToPlayStoreOrSystem(context, "org.videolan.vlc", streamUrl, title, "VLC")
            }
        }
    }

    fun openMxPlayer(context: Context, streamUrl: String, title: String) {
        val uri = Uri.parse(streamUrl)
        // Try Free Ad version first
        val adIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndTypeAndNormalize(uri, "video/*")
            setPackage("com.mxtech.videoplayer.ad")
            putExtra("title", title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(adIntent)
            return
        } catch (_: Exception) {}

        // Try Pro version
        val proIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndTypeAndNormalize(uri, "video/*")
            setPackage("com.mxtech.videoplayer.pro")
            putExtra("title", title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(proIntent)
        } catch (e: Exception) {
            fallbackToPlayStoreOrSystem(context, "com.mxtech.videoplayer.ad", streamUrl, title, "MX Player")
        }
    }

    fun openJustPlayer(context: Context, streamUrl: String, title: String) {
        val uri = Uri.parse(streamUrl)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndTypeAndNormalize(uri, "video/*")
            setPackage("com.brouken.player")
            putExtra("title", title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            fallbackToPlayStoreOrSystem(context, "com.brouken.player", streamUrl, title, "Just Player")
        }
    }

    fun openSystemChooser(context: Context, streamUrl: String, title: String) {
        val uri = Uri.parse(streamUrl)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndTypeAndNormalize(uri, "video/*")
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra("title", title)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No video player found", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, text: String, feedbackMessage: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("DRIDI FLIX Stream", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, feedbackMessage, Toast.LENGTH_SHORT).show()
    }

    fun shareStream(context: Context, streamUrl: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n$streamUrl")
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun fallbackToPlayStoreOrSystem(
        context: Context,
        packageName: String,
        streamUrl: String,
        title: String,
        appName: String
    ) {
        try {
            // Prompt to open or fallback to system
            Toast.makeText(context, "$appName is not installed. Opening system player...", Toast.LENGTH_SHORT).show()
            openSystemChooser(context, streamUrl, title)
        } catch (e: Exception) {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(marketIntent)
            } catch (_: Exception) {}
        }
    }
}
