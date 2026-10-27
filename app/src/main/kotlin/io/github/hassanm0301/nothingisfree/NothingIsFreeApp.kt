package io.github.hassanm0301.nothingisfree

import android.app.Application
import io.github.hassanm0301.nothingisfree.glyph.GlyphController
import io.github.hassanm0301.nothingisfree.glyph.GlyphModule

class NothingIsFreeApp : Application() {

    /** Process-wide Glyph access point. No-op until the SDK is wired in (docs/GLYPH.md). */
    val glyph: GlyphController by lazy { GlyphModule.controller(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NothingIsFreeApp
            private set
    }
}
