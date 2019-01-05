package com.noble.activity.artifactdeckcodekotlin

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.noble.activity.artifactdeckcodekotlin.coder.ArtifactDeckDecoder

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val decoder: ArtifactDeckDecoder = ArtifactDeckDecoder()
        val res = decoder.parseDeck("ADCJWkTZX05uwGDCRV4XQGy3QGLmqUBg4GQJgGLGgO7AaABR3JlZW4vQmxhY2sgRXhhbXBsZQ__")

        Toast.makeText(this, res.name, Toast.LENGTH_LONG).show()

    }
}
