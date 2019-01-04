package com.noble.activity.artifactdeckcodekotlin.model

data class Deck constructor(var name: String, var heroes: List<HeroRef>, var cards: List<CardRef>)