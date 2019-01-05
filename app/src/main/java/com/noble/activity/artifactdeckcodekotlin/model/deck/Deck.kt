package com.noble.activity.artifactdeckcodekotlin.model.deck

data class Deck constructor(var name: String, var heroes: List<HeroRef>, var cards: List<CardRef>)