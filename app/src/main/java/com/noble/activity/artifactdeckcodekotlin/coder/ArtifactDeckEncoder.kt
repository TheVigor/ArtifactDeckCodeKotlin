package com.noble.activity.artifactdeckcodekotlin.coder

import android.util.Base64
import com.noble.activity.artifactdeckcodekotlin.model.CardRef
import com.noble.activity.artifactdeckcodekotlin.model.Deck
import com.noble.activity.artifactdeckcodekotlin.model.HeroRef
import java.lang.Exception
import kotlin.math.floor

class ArtifactDeckEncoder {

    companion object {
        private const val currentVersion = 2
        private const val encodePrefix = "ADC"

        private const val maxBytesForVarUint32 = 5
        private const val headerSize = 3
    }

    fun encodeDeck(deck: Deck): String {
        val bytes = encodeBytes(deck)

        return encodeBytesToString(bytes)
    }

    fun encodeBytesToString(bytes: MutableList<Int>): String {

        var byteCount = bytes.size

        //if we have an empty buffer, just throw
        if (byteCount == 0) {
            throw Exception ("No deck content")
        }

        var fin = bytes.map { it.toByte() }.toByteArray()

        var encoded: String = Base64.encodeToString(fin, Base64.DEFAULT).trim()
        var deckString = encodePrefix + encoded

        deckString = deckString.replace('/', '-')
        deckString = deckString.replace('=', '_')

        return deckString
    }


    fun extractNBitsWithCarry(value: Int, numBits: Int): Int {
            var limitBit = 1 shl numBits
            var result = (value and (limitBit - 1))
            if (value >= limitBit) {
                result = result or limitBit
            }

            return result
        }

    fun addByte(bytes: MutableList<Int>, b: Int) {
        if (b > 255) {
            throw Exception("Invalid byte value")
        }
        bytes.add(b)
    }

    fun addRemainingNumberToBuffer(value: Int, alreadyWrittenBits: Int, bytes: MutableList<Int>)
    {
        var data = value shr alreadyWrittenBits
        var numBytes = 0
        while (data > 0)
        {
            var nextByte = extractNBitsWithCarry(data, 7)
            data =  data shr 7
            addByte(bytes, nextByte)

            numBytes++
        }
    }

    fun addCardToBuffer(count: Int, value: Int, bytes: MutableList<Int>)
    {
        //this shouldn't ever be the case
        if (count == 0) {
            throw Exception ("count is 0, this shouldn't ever be the case")
        }

        var countBytesStart = bytes.size

        //determine our count. We can only store 2 bits, and we know the value is at least one, so we can encode values 1-5. However, we set both bits to indicate an
        //extended count encoding
        var firstByteMaxCount = 0x03
        var extendedCount = (count - 1) >= firstByteMaxCount

        //determine our first byte, which contains our count, a continue flag, and the first few bits of our value
        var firstByteCount = if (extendedCount) firstByteMaxCount else (count - 1)
        var firstByte = (firstByteCount shl 6)
        firstByte = firstByte or extractNBitsWithCarry(value, 5)

        addByte(bytes, firstByte)

        //now continue writing out the rest of the number with a carry flag
        addRemainingNumberToBuffer(value, 5, bytes)

        //now if we overflowed on the count, encode the remaining count
        if (extendedCount)
        {
            addRemainingNumberToBuffer(count, 0, bytes)
        }

        var countBytesEnd = bytes.size

        if (countBytesEnd - countBytesStart > 11)
        {
            //something went horribly wrong
            throw Exception("is more than 11, something went horribly wrong")
        }
    }

    fun computeChecksum(bytes: MutableList<Int>, numBytes: Int): Int
    {
        var checksum = 0
        for (addCheck in headerSize until numBytes + headerSize) {
            var b = bytes[addCheck]
            checksum += b
        }

        return checksum
    }


    fun encodeBytes(deckContents: Deck): MutableList<Int> {
        if (deckContents.heroes.isEmpty() || deckContents.cards.isEmpty()) {
            throw Exception("Deck is empty")
        }

        deckContents.heroes = deckContents.heroes.sortedBy { it.id }
        deckContents.cards = deckContents.cards.sortedBy { it.id }

        var countHeroes = deckContents.heroes.size
        var allCards = deckContents.heroes + deckContents.cards

        var  bytes = mutableListOf<Int>()

        //our version and hero count
        var version = currentVersion shl 4 or extractNBitsWithCarry(countHeroes, 3)
        addByte(bytes, version)

        //the checksum which will be updated at the end
        var dummyChecksum = 0
        var checksumByte = bytes.size
        addByte(bytes, dummyChecksum)

        // write the name size
        var nameLen = 0
        var name = ""
        if (!deckContents.name.isEmpty())
        {
            // replace strip_tags() with your own HTML santizer or escaper.
            //name = strip_tags( deckContents['name']);
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
            var card: HeroRef = allCards[currHero] as HeroRef
            if (card.turn == 0) {
                throw Exception ("A hero's turn cannot be 0")
            }

            addCardToBuffer(card.turn, card.id - prevCardId, bytes)
            prevCardId = card.id
        }

        //reset our card offset
        prevCardId = 0

        //now all of the cards
        for (currCard in countHeroes until allCards.size) {
            //see how many cards we can group together
            var card: CardRef = allCards[currCard] as CardRef
            if (card.count == 0) {
                throw Exception ("A card's count cannot be 0")
            }
            if (card.id <= 0) {
                throw Exception ("A card's id cannot be 0 or less")
            }

            //record this set of cards, and advance
            addCardToBuffer(card.count, card.id - prevCardId, bytes)

            prevCardId = card.id
        }

        // save off the pre string bytes for the checksum
        var preStringByteCount = bytes.size

        //write the string
        var nameBytes = name.toByteArray(charset = Charsets.UTF_8)
        for (nameByte in nameBytes)
        {
            addByte(bytes, nameByte.toInt())
        }

        var unFullChecksum = computeChecksum(bytes, preStringByteCount - headerSize)
        var unSmallChecksum = (unFullChecksum and 0xFF)

        bytes[checksumByte] = unSmallChecksum
        return bytes
    }



}