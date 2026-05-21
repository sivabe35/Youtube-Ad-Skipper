package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class YoutubeAdSkipService : AccessibilityService() {

    companion object {
        private const val TAG = "YoutubeAdSkipService"
        
        // Shared Prefs keys
        const val PREFS_NAME = "AdSkipperPrefs"
        const val KEY_SKIP_COUNT = "skip_count"
        const val KEY_LAST_SKIPPED_TIME = "last_skipped_time"
        
        // Action for broadcasting updates to UI
        const val ACTION_AD_SKIPPED = "com.example.ACTION_AD_SKIPPED"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            val pName = event.packageName?.toString()
            if (pName != null && isYouTubePackage(pName)) {
                findAndClickSkipButton(rootNode)
            }
        }
    }

    private fun isYouTubePackage(packageName: String): Boolean {
        return packageName.contains("youtube") || 
               packageName.contains("leanback") || 
               packageName.contains("amazon.firetv")
    }

    private fun findAndClickSkipButton(rootNode: AccessibilityNodeInfo) {
        // Method 1: Check known YouTube resource IDs (highly optimized, fast O(1) index lookups)
        val skipIds = listOf(
            "com.google.android.youtube:id/skip_ad_button",
            "com.google.android.youtube:id/modern_skip_ad_button",
            "com.google.android.youtube:id/skip_ad_button_text",
            "com.google.android.youtube.tv:id/skip_button",
            "com.google.android.youtube.tv:id/skip_ad_button",
            "com.google.android.youtube.tv:id/skip_button_text",
            "com.google.android.apps.youtube.kids:id/skip_ad_button",
            "com.amazon.firetv.youtube:id/skip_button"
        )

        for (id in skipIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (nodes != null && nodes.isNotEmpty()) {
                for (node in nodes) {
                    if (node != null) {
                        if (tryClick(node)) {
                            onAdSkippedSuccess()
                            return
                        }
                    }
                }
            }
        }

        // Method 2: Traverse the tree using heuristic match (for other languages or ID modifications)
        if (traverseAndClick(rootNode)) {
            onAdSkippedSuccess()
        }
    }

    private fun tryClick(node: AccessibilityNodeInfo): Boolean {
        var temp: AccessibilityNodeInfo? = node
        while (temp != null) {
            if (temp.isClickable) {
                val success = temp.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) {
                    Log.d(TAG, "Successfully clicked skip button: ${temp.viewIdResourceName ?: "no-id"}")
                    return true
                }
            }
            temp = temp.parent
        }
        // Fallback, try to click the node directly if properties aren't fully exposed
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun traverseAndClick(node: AccessibilityNodeInfo?, depth: Int = 0): Boolean {
        if (node == null || depth > 40) return false

        // heuristic 1: resource ID contains 'skip' and ('ad' or 'button')
        val id = node.viewIdResourceName
        if (id != null) {
            val lowerId = id.lowercase()
            if (lowerId.contains("skip") && (lowerId.contains("ad") || lowerId.contains("button") || lowerId.contains("video"))) {
                if (tryClick(node)) {
                    return true
                }
            }
        }

        // heuristic 2: Check text Content with multilingual skip keywords
        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            if (isSkipText(text)) {
                if (tryClick(node)) {
                    return true
                }
            }
        }

        // heuristic 3: Check contentDescription with multilingual skip keywords
        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrBlank()) {
            if (isSkipText(desc)) {
                if (tryClick(node)) {
                    return true
                }
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (traverseAndClick(child, depth + 1)) {
                return true
            }
        }
        return false
    }

    private fun isSkipText(text: String): Boolean {
        val cleanText = text.lowercase().trim()
        return cleanText.contains("skip") || // English
               cleanText.contains("skip ad") ||
               cleanText.contains("skip ads") ||
               cleanText.contains("omitir") || // Spanish/Portuguese (Omitir anúncio)
               cleanText.contains("saltar") || // Spanish (Saltar anuncio)
               cleanText.contains("pular") || // Portuguese (Pular anúncio)
               cleanText.contains("ignorer") || // French (Ignorer l'annonce)
               cleanText.contains("überspringen") || // German
               cleanText.contains("salta") || // Italian
               cleanText.contains("пропустить") || // Russian
               cleanText.contains("スキップ") || // Japanese
               cleanText.contains("건너뛰기") || // Korean
               cleanText.contains("跳过") || // Chinese
               cleanText.contains("छोड़ें") || // Hindi
               cleanText.contains("تخطي") || // Arabic
               cleanText.contains("atla") || // Turkish
               cleanText.contains("bỏ qua") || // Vietnamese
               cleanText.contains("pomiń") || // Polish
               cleanText.contains("ข้าม") || // Thai
               cleanText.contains("lewati") // Indonesian/Malay
    }

    private fun onAdSkippedSuccess() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentClicks = prefs.getInt(KEY_SKIP_COUNT, 0)
        prefs.edit()
            .putInt(KEY_SKIP_COUNT, currentClicks + 1)
            .putLong(KEY_LAST_SKIPPED_TIME, System.currentTimeMillis())
            .apply()

        // Broadcast update to the active UI if it is running
        val intent = Intent(ACTION_AD_SKIPPED)
        sendBroadcast(intent)
        Log.i(TAG, "Ad skipped! New count: ${currentClicks + 1}")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service selection interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected successfully")
    }
}
