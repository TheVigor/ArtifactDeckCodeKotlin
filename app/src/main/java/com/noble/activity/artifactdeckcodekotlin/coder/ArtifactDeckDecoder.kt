package com.noble.activity.artifactdeckcodekotlin.coder

import android.util.Base64
import java.lang.Exception


class ArtifactDeckDecoder {

    companion object {
        private const val currentVersion = 2
        private const val encodePrefix = "ADC"
    }

    fun decodeDeckString(deckCode: String): ByteArray {
        if (deckCode.substring(0, encodePrefix.length) != encodePrefix) {
            throw Exception("Artifact Deck Code prefix missing!")
        }

        var stripDeckCode = deckCode.substring(encodePrefix.length)

        stripDeckCode = stripDeckCode.replace('-', '/')
        stripDeckCode = stripDeckCode.replace('_', '=')

        val decoded = Base64.decode(stripDeckCode, Base64.DEFAULT)

        return decoded

    }

}