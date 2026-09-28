package com.maxeydev.picklelog.ui.share

import android.content.Context
import androidx.annotation.MainThread

class WebViewWarmer(
    context: Context,
    private val templateUrl: String = CARD_TEMPLATE_URL,
    private val width: Int = CARD_WIDTH_PX,
    private val height: Int = CARD_HEIGHT_PX,
) {
    private val appContext = context.applicationContext
    private var instance: CardWebView? = null

    val isWarm: Boolean
        get() = instance?.isAlive == true

    @MainThread
    fun warm() {
        obtain()
    }

    @MainThread
    fun obtain(): CardWebView {
        instance?.takeIf { it.isAlive }?.let { return it }
        instance?.destroy()
        return CardWebView(appContext, templateUrl, width, height).also { instance = it }
    }

    @MainThread
    fun discard() {
        instance?.destroy()
        instance = null
    }
}
