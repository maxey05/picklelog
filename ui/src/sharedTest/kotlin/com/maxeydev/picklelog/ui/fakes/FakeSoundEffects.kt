package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.ui.sound.Cue
import com.maxeydev.picklelog.ui.sound.SoundEffects

class FakeSoundEffects : SoundEffects {
    val played = mutableListOf<Cue>()
    val previewed = mutableListOf<Cue>()

    override fun play(
        cue: Cue,
        volume: Float,
    ) {
        played += cue
    }

    override fun preview(cue: Cue) {
        previewed += cue
    }
}
