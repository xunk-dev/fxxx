package com.frameout.app.about

object AboutConfig {
    const val YOUTUBE_URL: String = "https://youtube.com/@xunk-dev"

    const val CONTACT_EMAIL: String = "xunk.app@gmail.com"

    val hasYoutube: Boolean get() = YOUTUBE_URL.startsWith("https://")
    val hasEmail: Boolean get() = CONTACT_EMAIL.contains('@')
}
