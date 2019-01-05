# ArtifactDeckCodeKotlin
A Kotlin port of [ArtifactDeckCode](https://github.com/ValveSoftware/ArtifactDeckCode)

Refer to that page for more detailed information on Artifact Deck Codes.
## Documentation
### Decoder
Decoder returns a Deck object with contains a list of Heroes(ids and turn numbers), Cards(ids and count), and the deck Name.
```kotlin
val decoder = ArtifactDeckDecoder()
val deck = decoder.decode("ADCJQUQI30zuwEYg2ABeF1Bu94BmWIBTEkLtAKlAZakAYmHh0JsdWUvUmVkIEV4YW1wbGU_")
```
### Encoder
Encoder returns a string. This string is the ArtifactDeckCode.
```kotlin
val encoder = ArtifactDeckEncoder()
val deckCode = encoder.encode(deck)
```