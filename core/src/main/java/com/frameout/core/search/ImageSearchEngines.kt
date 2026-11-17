package com.frameout.core.search

import com.frameout.core.quality.TagText

data class SearchEngine(
    val id: String,
    val name: String,
    val url: String,
    val packages: List<String>,
    val descEn: String,
    val descAr: String
) {
    val supportsShare: Boolean get() = packages.isNotEmpty()
    fun desc(lang: String): String = when (lang) {
        "ar" -> descAr
        "en" -> descEn
        else -> TagText.forLang(lang)?.engines?.get(id) ?: descEn
    }
}

object ImageSearchEngines {
    const val LENS_ID = "google_lens"

    val all: List<SearchEngine> = listOf(
        SearchEngine(
            LENS_ID, "Google Lens", "https://lens.google.com/",
            listOf("com.google.ar.lens", "com.google.android.googlequicksearchbox"),
            "Identify objects, places and text, and find the image source",
            "تعرّف على الأشياء والأماكن والنصوص وابحث عن مصدر الصورة"
        )
    )

    val lens: SearchEngine get() = all.first()

    fun byId(id: String): SearchEngine? = all.firstOrNull { it.id == id }
}
