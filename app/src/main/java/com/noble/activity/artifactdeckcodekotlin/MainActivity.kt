package com.noble.activity.artifactdeckcodekotlin

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.noble.activity.artifactdeckcodekotlin.coder.ArtifactDeckDecoder
import com.noble.activity.artifactdeckcodekotlin.coder.ArtifactDeckEncoder
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    companion object {
        private const val firstDeckCode = "ADCJQUQI30zuwEYg2ABeF1Bu94BmWIBTEkLtAKlAZakAYmHh0JsdWUvUmVkIEV4YW1wbGU_"
        private const val secondDeckCode = "ADCJWkTZX05uwGDCRV4XQGy3QGLmqUBg4GQJgGLGgO7AaABR3JlZW4vQmxhY2sgRXhhbXBsZQ__"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val encoder = ArtifactDeckEncoder()
        val decoder = ArtifactDeckDecoder()

        val deck = decoder.parseDeck(firstDeckCode)
        val deckCode = encoder.encode(deck)

        if (deckCode != firstDeckCode) {
            throw Exception("First deck mismatch")
        }

        val deck2 = decoder.parseDeck(secondDeckCode)
        val deckCode2 = encoder.encode(deck2)

        if (deckCode2 != secondDeckCode) {
            throw Exception("Second deck mismatch")
        }

        Toast.makeText(this, deck.name + " " + deck2.name, Toast.LENGTH_LONG).show()
    }
}
