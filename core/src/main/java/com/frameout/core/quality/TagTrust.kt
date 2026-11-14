package com.frameout.core.quality

object TagTrust {
    const val AMBIGUOUS_WEIGHT = 0.6f
    const val HOME_GOOD_WEIGHT = 0.8f
    const val AMBIGUOUS_MIN = 0.85f

    private val ABSTRACT = setOf(
        "fun", "leisure", "recreation", "event", "pattern", "design", "art", "cool", "junk", "flap", "pocket", "bumper",
        "net", "pitch", "track", "balance", "title", "position", "class", "product", "metal", "neon", "tile", "brick",
        "mufti", "chain", "interaction", "community", "crew", "sitting", "standing", "love", "fiction", "miniature",
        "glitter", "cotton", "wool", "leather", "textile", "monochrome", "roller", "space", "comics"
    )

    private val AMBIGUOUS = setOf(
        "wheel", "tire", "street", "traffic", "wall", "roof", "stairs", "handrail", "sign", "ball", "tourism", "travel",
        "vacation", "wing", "tail", "fur", "shell", "ring", "watch", "bar", "rock", "horn", "cap", "tie", "boxer",
        "skin", "flesh", "muscle", "hair", "nail", "toe", "foot", "ear", "mouth", "eyelash", "cup", "saucer", "strap",
        "team", "competition", "race", "racing"
    )

    private fun key(name: String) = name.trim().lowercase()

    fun isAbstract(name: String): Boolean = key(name) in ABSTRACT

    fun isAmbiguous(name: String): Boolean = key(name) in AMBIGUOUS

    fun weight(name: String): Float = when {
        isAbstract(name) -> 0f
        isAmbiguous(name) -> AMBIGUOUS_WEIGHT
        else -> 1f
    }

    fun objectWeight(tag: String): Float = if (tag == "o:home_good") HOME_GOOD_WEIGHT else 1f
}
