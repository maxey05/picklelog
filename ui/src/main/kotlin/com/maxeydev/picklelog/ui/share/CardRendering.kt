package com.maxeydev.picklelog.ui.share

interface CardRendering {
    suspend fun render(data: CardData): CardRenderResult
}
