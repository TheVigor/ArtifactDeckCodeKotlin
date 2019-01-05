package com.noble.activity.artifactdeckcodekotlin

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.noble.activity.artifactdeckcodekotlin.coder.ArtifactDeckDecoder
import com.noble.activity.artifactdeckcodekotlin.coder.ArtifactDeckEncoder

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val encoder: ArtifactDeckEncoder = ArtifactDeckEncoder()
        val decoder: ArtifactDeckDecoder = ArtifactDeckDecoder()

        //ADCJQUQI30zuwEYg2ABeF1Bu94BmWIBTEkLtAKlAZakAYmHh0JsdWUvUmVkIEV4YW1wbGU_
        //ADCJWkTZX05uwGDCRV4XQGy3QGLmqUBg4GQJgGLGgO7AaABR3JlZW4vQmxhY2sgRXhhbXBsZQ__

        val deck = decoder.parseDeck("ADCJWkTZX05uwGDCRV4XQGy3QGLmqUBg4GQJgGLGgO7AaABR3JlZW4vQmxhY2sgRXhhbXBsZQ__")
        Toast.makeText(this, deck.name, Toast.LENGTH_LONG).show()

        val str = encoder.encodeDeck(deck)

    }
}
