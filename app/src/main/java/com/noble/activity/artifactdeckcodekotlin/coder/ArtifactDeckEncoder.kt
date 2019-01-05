package com.noble.activity.artifactdeckcodekotlin.coder

import android.util.Base64
import com.noble.activity.artifactdeckcodekotlin.model.deck.CardRef
import com.noble.activity.artifactdeckcodekotlin.model.deck.Deck
import com.noble.activity.artifactdeckcodekotlin.model.deck.HeroRef
import java.lang.Exception
import kotlin.math.floor

class ArtifactDeckEncoder {

    companion object {
        private const val currentVersion = 2
        private const val encodePrefix = "ADC"

        private const val headerSize = 3
    }

    fun encode(deck: Deck): String {
        val bytes = encodeBytes(deck)

        return encodeBytesToString(bytes)
    }

    private fun encodeBytesToString(bytes: MutableList<Int>): String {
        val byteCount = bytes.size

        if (byteCount == 0) {
            throw Exception ("No deck content")
        }

        val fin = bytes.map { it.toByte() }.toByteArray()

        val encoded: String = Base64.encodeToString(fin, Base64.DEFAULT).trim()
        var deckString = encodePrefix + encoded

        deckString = deckString.replace('/', '-').replace('=', '_')

        return deckString
    }

    private fun extractNBitsWithCarry(value: Int, numBits: Int): Int {
        val limitBit = 1 shl numBits
        var result = (value and (limitBit - 1))
        if (value >= limitBit) {
            result = result or limitBit
        }

        return result
    }

    private fun addByte(bytes: MutableList<Int>, b: Int) {
        if (b > 255) {
            throw Exception("Invalid byte value")
        }
        bytes.add(b)
    }

    private fun addRemainingNumberToBuffer(value: Int, alreadyWrittenBits: Int, bytes: MutableList<Int>) {
        var data = value shr alreadyWrittenBits
        var numBytes = 0
        while (data > 0) {
            val nextByte = extractNBitsWithCarry(data, 7)
            data =  data shr 7
            addByte(bytes, nextByte)

            numBytes++
        }
    }

    private fun addCardToBuffer(count: Int, value: Int, bytes: MutableList<Int>)
    {
        if (count == 0) {
            throw Exception ("count is 0, this shouldn't ever be the case")
        }

        val countBytesStart = bytes.size

        val firstByteMaxCount = 0x03
        val extendedCount = (count - 1) >= firstByteMaxCount

        val firstByteCount = if (extendedCount) firstByteMaxCount else (count - 1)

        var firstByte = (firstByteCount shl 6)
        firstByte = firstByte or extractNBitsWithCarry(value, 5)

        addByte(bytes, firstByte)

        addRemainingNumberToBuffer(value, 5, bytes)

        if (extendedCount) {
            addRemainingNumberToBuffer(count, 0, bytes)
        }

        val countBytesEnd = bytes.size

        if (countBytesEnd - countBytesStart > 11) {
            throw Exception("something went horribly wrong")
        }
    }

    private fun computeChecksum(bytes: MutableList<Int>, numBytes: Int): Int {
        var checksum = 0
        for (addCheck in headerSize until numBytes + headerSize) {
            val b = bytes[addCheck]
            checksum += b
        }

        return checksum
    }

    private fun encodeBytes(deckContents: Deck): MutableList<Int> {
        if (deckContents.heroes.isEmpty() || deckContents.cards.isEmpty()) {
            throw Exception("Deck is empty")
        }

        deckContents.heroes = deckContents.heroes.sortedBy { it.id }
        deckContents.cards = deckContents.cards.sortedBy { it.id }

        val countHeroes = deckContents.heroes.size
        val allCards = deckContents.heroes + deckContents.cards

        val  bytes = mutableListOf<Int>()

        val version = currentVersion shl 4 or extractNBitsWithCarry(countHeroes, 3)
        addByte(bytes, version)

        val dummyChecksum = 0
        val checksumByte = bytes.size
        addByte(bytes, dummyChecksum)

        var nameLen = 0
        var name = ""

        if (!deckContents.name.isEmpty()) {

            name = deckContents.name
            var trimLen = name.length
            while (trimLen > 63)
            {
                var amountToTrim = floor((trimLen - 63).toDouble() / 4).toInt()
                amountToTrim = if (amountToTrim > 1) amountToTrim else 1
                name = name.substring(0, name.length - amountToTrim)
                trimLen = name.length
            }

            nameLen = name.length
        }

        addByte(bytes, nameLen)
        addRemainingNumberToBuffer(countHeroes, 3, bytes)

        var prevCardId = 0
        for (currHero in 0 until countHeroes) {
            val card: HeroRef = allCards[currHero] as HeroRef
            if (card.turn == 0) {
                throw Exception ("A hero's turn cannot be 0")
            }

            addCardToBuffer(card.turn, card.id - prevCardId, bytes)
            prevCardId = card.id
        }

        prevCardId = 0

        for (currCard in countHeroes until allCards.size) {
            val card: CardRef = allCards[currCard] as CardRef
            if (card.count == 0) {
                throw Exception ("A card's count cannot be 0")
            }
            if (card.id <= 0) {
                throw Exception ("A card's id cannot be 0 or less")
            }

            addCardToBuffer(card.count, card.id - prevCardId, bytes)
            prevCardId = card.id
        }

        val preStringByteCount = bytes.size

        val nameBytes = name.toByteArray(charset = Charsets.UTF_8)
        for (nameByte in nameBytes) {
            addByte(bytes, nameByte.toInt())
        }

        val unFullChecksum = computeChecksum(bytes, preStringByteCount - headerSize)
        val unSmallChecksum = unFullChecksum and 0xFF

        bytes[checksumByte] = unSmallChecksum
        return bytes
    }
}