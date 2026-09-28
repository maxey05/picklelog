package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.ui.share.CardData
import com.maxeydev.picklelog.ui.share.CardRenderFailure
import com.maxeydev.picklelog.ui.share.CardRenderResult
import com.maxeydev.picklelog.ui.share.CardRendering
import java.util.concurrent.CopyOnWriteArrayList

class FakeCardRenderer(
    var result: (CardData) -> CardRenderResult = { CardRenderResult.Failed(CardRenderFailure.LAYOUT_NOT_READY) },
) : CardRendering {
    val rendered: MutableList<CardData> = CopyOnWriteArrayList()

    override suspend fun render(data: CardData): CardRenderResult {
        rendered += data
        return result(data)
    }
}
