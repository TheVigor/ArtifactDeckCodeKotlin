package com.noble.activity.artifactdeckcodekotlin.coder

import android.util.Base64
import com.noble.activity.artifactdeckcodekotlin.model.*
import java.lang.Exception


class ArtifactDeckDecoder {

    companion object {
        private const val currentVersion = 2
        private const val encodePrefix = "ADC"
    }

    fun parseDeck(deckCode: String): Deck {

        val deckBytes = decodeDeckString(deckCode)

        val deck = parseDeckInternal(deckCode, deckBytes)

        return deck
    }

    fun decodeDeckString(deckCode: String): IntArray {
        if (deckCode.substring(0, encodePrefix.length) != encodePrefix) {
            throw Exception("Artifact Deck Code prefix missing!")
        }

        var stripDeckCode = deckCode.substring(encodePrefix.length)

        stripDeckCode = stripDeckCode.replace('-', '/')
        stripDeckCode = stripDeckCode.replace('_', '=')

        var decoded = Base64.decode(stripDeckCode, Base64.DEFAULT)

        return decoded.map { it.toInt() and 0xFF }.toIntArray()

    }

    fun ReadBitsChunk(chunk: Int, numBits: Int, currShift: Int, outBits: Int): Chunk {
        var continueBit = 1 shl numBits
        var newBits = chunk and (continueBit - 1)

        var bits = outBits or (newBits shl currShift)
        val result = (chunk and continueBit) != 0

        return Chunk(result = result, bits = bits)
    }



    fun readVarEncodedUint32(baseValue: Int, baseBits: Int, data: IntArray, indexStart: Int, indexEnd: Int, outValue: Int): EncodedVar
    {
        var outV = 0
        var deltaShift = 0

        var indexS = indexStart

        var chunk = ReadBitsChunk(baseValue, baseBits, deltaShift, outValue)

        if ((baseBits == 0) || chunk.result)
        {
            deltaShift += baseBits

            while (true)
            {
                //do we have more room?
                if (indexStart > indexEnd)
                    return EncodedVar(result = false, indexStart = indexS, outValue = chunk.bits)

                //read the bits from this next byte and see if we are done
                var nextByte = data[indexS++]

                chunk = ReadBitsChunk(nextByte, 7, deltaShift, chunk.bits)
                if (!chunk.result) {
                    break
                }

                deltaShift += 7
            }
        }

        return EncodedVar(result = true, indexStart = indexS, outValue = chunk.bits)
    }

    //handles decoding a card that was serialized
    fun readSerializedCard(data: IntArray, indexStart: Int, indexEnd: Int,
                           prevCardBase: Int, outCount: Int, outCardId: Int): HeroVar
    {
        //end of the memory block?
        if (indexStart > indexEnd) {
            return HeroVar(
                result = false,
                indexStart = indexStart,
                prevCardBase = prevCardBase,
                outCount = outCount,
                outCardId = outCardId)
        }

        var indexS = indexStart

        //header contains the count (2 bits), a continue flag, and 5 bits of offset data. If we have 11 for the count bits we have the count
        //encoded after the offset
        var header = data[indexS++]
        var hasExtendedCount = ((header shr 6) == 0x03)

        //read in the delta, which has 5 bits in the header, then additional bytes while the value is set
        var cardDelta = 0

        var chunk = readVarEncodedUint32(header, 5, data, indexS, indexEnd, cardDelta)

        cardDelta = chunk.outValue

        if (!chunk.result) {
            return HeroVar(result = false,
                indexStart = chunk.indexStart,
                prevCardBase = prevCardBase,
                outCount = outCount,
                outCardId = outCardId)

        }

        var outCardIddddd = prevCardBase + cardDelta

        var outCountttttt = outCount

        //now parse the count if we have an extended count
        if (hasExtendedCount)
        {
            var chunk = readVarEncodedUint32(0, 0, data, chunk.indexStart, indexEnd, outCount)

            if (!chunk.result) {
                return HeroVar(result = false,
                    indexStart = chunk.indexStart,
                    prevCardBase = prevCardBase,
                    outCount = outCount,
                    outCardId = outCardIddddd)
            }
        }
        else
        {
            //the count is just the upper two bits + 1 (since we don't encode zero)
            outCountttttt = (header shr 6) + 1
        }

        //update our previous card before we do the remap, since it was encoded without the remap

        var prevCardBaseeeeee = outCardIddddd
        return HeroVar(result = true,
            indexStart = chunk.indexStart,
            prevCardBase = prevCardBaseeeeee,
            outCount = outCountttttt,
            outCardId = outCardIddddd)
    }



    fun parseDeckInternal(deckCode: String, deckBytes: IntArray): Deck {
        var currentByteIndex = 0
        var totalBytes = deckBytes.size

        //check version num
        var versionAndHeroes = deckBytes[currentByteIndex++]
        var version = versionAndHeroes shr 4
        if (currentVersion != version && version != 1) {
            throw Exception("Invalid code version")
        }

        //do checksum check
        var checksum = deckBytes[currentByteIndex++]

        var stringLength = 0
        if (version > 1) {
            stringLength = deckBytes[currentByteIndex++]
        }

        var totalCardBytes = totalBytes - stringLength

        //grab the string size
            var computedChecksum = 0
            for (i in currentByteIndex until totalCardBytes) {
                computedChecksum += deckBytes[i]
            }

            var masked: Int = computedChecksum and 0xFF
            if (checksum != masked) {
                throw Exception ("checksum does not match")
            }

        //read in our hero count (part of the bits are in the version, but we can overflow bits here

        var numHeroes = 0
        var chunk = readVarEncodedUint32(versionAndHeroes, 3, deckBytes, currentByteIndex, totalCardBytes, numHeroes)

        numHeroes = chunk.outValue

        if (!chunk.result) {
            throw Exception("Missing hero count")
        }


        //now read in the heroes
        val heroes = mutableListOf<HeroRef>()
        var serCard = HeroVar(false, chunk.indexStart, 0, 0, 0)

        for (currHero in 0 until numHeroes) {
            serCard.outCount = 0
            serCard.outCardId = 0

            serCard = readSerializedCard(deckBytes, serCard.indexStart, totalCardBytes,
                serCard.prevCardBase, serCard.outCount, serCard.outCardId )

            if (!serCard.result) {
                throw Exception("Missing hero data")
            }

            heroes.add(HeroRef(id = serCard.outCardId, turn = serCard.outCount))
        }


        val cards = mutableListOf<CardRef>()
        var derCard = HeroVar(false, serCard.indexStart, 0, 0, 0)


        while (derCard.indexStart < totalCardBytes) // < instead of <=, deckBytes starts at 1 in PHP
        {
            derCard.outCount = 0
            derCard.outCardId = 0

            derCard = readSerializedCard(deckBytes, derCard.indexStart, totalBytes,
                derCard.prevCardBase, derCard.outCount, derCard.outCardId )

            if (!derCard.result) {
                throw Exception ("Missing card data")
            }

            cards.add(CardRef(id = derCard.outCardId, count= derCard.outCount))
        }
        
        var name = ""
        if (derCard.indexStart < totalBytes) // < instead of <=, deckBytes starts at 1 in PHP
        {
            var bytes = deckBytes
                .drop(deckBytes.size - stringLength)
                .map {it.toByte()}
                .toByteArray()

            name = bytes.toString(Charsets.UTF_8)
        }

        return Deck( heroes = heroes, cards = cards, name = name)
    }
}