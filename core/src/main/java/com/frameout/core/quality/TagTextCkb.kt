package com.frameout.core.quality

internal object TagTextCkb {
    val text = TagText(
        labels = TagLabelsCkb.MAP,
        fixed = mapOf(
            TagRules.FACE to "دەموچاو", TagRules.GROUP to "گرووپ",
            TagRules.SMILE to "زەردەخەنە", TagRules.EYES_CLOSED to "چاوی نوقاو",
            TagRules.GOOD_FACE to "دەموچاوی باش", TagRules.BODY to "لەش",
            TagRules.TEXT to "نووسراوەدار", TagRules.CODE to "هەر کۆدێک",
            "b:contact" to "پەیوەندی", "b:email" to "ئیمەیل",
            "b:isbn" to "ISBN", "b:phone" to "تەلەفۆن",
            "b:product" to "کۆدی بەرهەم", "b:sms" to "SMS",
            "b:plain" to "کۆدی دەقی ئاسایی", "b:url" to "بەستەر",
            "b:wifi" to "Wi-Fi", "b:geo" to "شوێن",
            "b:event" to "ڕووداو", "b:license" to "مۆڵەت",
            "o:fashion_good" to "جلوبەرگ", "o:food" to "خۆراک",
            "o:home_good" to "کەلوپەلی ماڵ", "o:place" to "شوێنەکان",
            "o:plant" to "ڕووەک"
        ),
        groups = mapOf(
            "People" to "خەڵک", "Things" to "شتەکان و دیمەنەکان", "Categories" to "پۆلەکان",
            "Text" to "دەق", "Codes" to "کۆدەکان"
        ),
        families = mapOf(
            "vehicles" to "ئۆتۆمبێل و گواستنەوە", "animals" to "ئاژەڵ", "people" to "خەڵک و لەش",
            "fashion" to "جلوبەرگ و پێداویستی", "food" to "خواردن و خواردنەوە", "nature" to "سروشت و دیمەن",
            "plants" to "ڕووەک و باخچە", "sky" to "ئاسمان و کەشوهەوا", "places" to "بینا و شوێنەکان",
            "home" to "ماڵ و ڕازاندنەوە", "devices" to "ئامێر و شاشەکان", "documents" to "بەڵگەنامە و تەختە",
            "music" to "مۆسیقا و سەما", "sports" to "وەرزش و چالاکی دەرەوە", "events" to "ڕووداو و ئاهەنگ",
            "toys" to "یاری و کارەکتەر", "crafts" to "هونەر و دەستکرد", "objects" to "شت و ئامراز"
        ),
        contentTags = mapOf(
            "building" to "بینا", "skyscraper" to "ئاسمانخۆش", "bridge" to "پردەکان",
            "tower" to "تاوەر", "mosque" to "مزگەوتەکان", "church" to "کڵێساکان",
            "castle" to "قەڵا و کۆشک", "landmark" to "شوێنە ناسراوەکان", "road" to "ڕێگاوبان",
            "meal" to "ژەمەکان", "fastfood" to "خواردنی خێرا", "sweets" to "شیرینی",
            "bread" to "نان", "fruit" to "میوە", "vegetable" to "سەوزە",
            "coffee" to "قاوە", "drink" to "خواردنەوە", "soccer" to "تۆپی پێ",
            "swimming" to "وەرزشی ئاوی", "running" to "ڕاکردن و پێشبڕکێ", "dance" to "سەما",
            "concert" to "کۆنسێرت", "party" to "ئاهەنگ", "wedding" to "داوەت",
            "fireworks" to "ئاگربازی", "fire" to "ئاگر", "car" to "ئۆتۆمبێل",
            "heavy" to "ڤان و بارهەڵگر", "bus" to "پاس", "motorcycle" to "مۆتۆڕسکیل",
            "bicycle" to "پاسکیل", "train" to "شەمەندەفەر", "plane" to "فڕۆکە",
            "helicopter" to "هێلیکۆپتەر", "boat" to "کەشتی و بەلەم", "dog" to "سەگ",
            "cat" to "پشیلە", "bird" to "باڵندە", "horse" to "ئەسپ",
            "cattle" to "ڕەشەوڵاخ", "bear" to "ورچ", "reptile" to "خشۆک",
            "insect" to "مێرووەکان", "mountain" to "شاخ", "beach" to "کەنارئاو",
            "desert" to "بیابان", "forest" to "دارستان", "water" to "دەریاچە و ڕووبار",
            "waterfall" to "شەلال", "sunset" to "ئاوابوونی خۆر", "flower" to "گوڵ",
            "ice" to "بەفر و بەستەڵەک", "storm" to "گەردەلوول", "phone" to "تەلەفۆن",
            "computer" to "کۆمپیوتەر", "tv" to "تەلەفیزیۆن", "instrument" to "ئامێری مۆسیقا",
            "clock" to "کاتژمێر", "bag" to "جانتا", "glasses" to "چاویلکە",
            "toy" to "یاری", "shoes" to "پێلاو", "hat" to "کڵاو",
            "dress" to "جل", "clothes" to "جلوبەرگ", "jewellery" to "زێڕ و خشڵ",
            "face" to "دەموچاو", "smile" to "زەردەخەنە", "group" to "گرووپی خەڵک",
            "baby" to "منداڵ", "crowd" to "قەرەباڵغی و تیپ", "text" to "دەق",
            "code" to "QR و بارکۆد"
        ),
        contentGroups = mapOf(
            "Vehicles" to "ئۆتۆمبێل", "Animals" to "ئاژەڵ", "Nature" to "سروشت",
            "Places" to "شوێن و بینا", "Food" to "خواردن و خواردنەوە", "Activities" to "وەرزش و ڕووداو",
            "Devices" to "ئامێر و شتەکان", "Fashion" to "جلوبەرگ", "People" to "خەڵک و دەق"
        ),
        engines = mapOf(
            "google_lens" to "شت، شوێن و دەق دەناسێتەوە و سەرچاوەی وێنە دەدۆزێتەوە"
        )
    )
}
