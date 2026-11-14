package com.frameout.core.quality

internal object TagTextUr {
    val text = TagText(
        labels = TagLabelsUr.MAP,
        fixed = mapOf(
            TagRules.FACE to "چہرے", TagRules.GROUP to "گروپ",
            TagRules.SMILE to "مسکراہٹ", TagRules.EYES_CLOSED to "بند آنکھیں",
            TagRules.GOOD_FACE to "اچھا چہرہ", TagRules.BODY to "جسم",
            TagRules.TEXT to "متن والے", TagRules.CODE to "کوئی بھی کوڈ",
            "b:contact" to "رابطہ", "b:email" to "ای میل",
            "b:isbn" to "ISBN", "b:phone" to "فون",
            "b:product" to "پروڈکٹ کوڈ", "b:sms" to "SMS",
            "b:plain" to "سادہ متن کا کوڈ", "b:url" to "لنک",
            "b:wifi" to "Wi-Fi", "b:geo" to "مقام",
            "b:event" to "تقریب", "b:license" to "لائسنس",
            "o:fashion_good" to "فیشن", "o:food" to "کھانے کی اشیا",
            "o:home_good" to "گھریلو سامان", "o:place" to "مقامات",
            "o:plant" to "پودے"
        ),
        groups = mapOf(
            "People" to "لوگ", "Things" to "اشیا اور مناظر", "Categories" to "زمرے",
            "Text" to "متن", "Codes" to "کوڈز"
        ),
        families = mapOf(
            "vehicles" to "گاڑیاں", "animals" to "جانور", "people" to "لوگ اور جسم",
            "fashion" to "فیشن اور لوازمات", "food" to "کھانا اور مشروبات", "nature" to "فطرت اور مناظر",
            "plants" to "پودے اور باغات", "sky" to "آسمان اور موسم", "places" to "عمارتیں اور مقامات",
            "home" to "گھر اور سجاوٹ", "devices" to "آلات اور اسکرینیں", "documents" to "دستاویزات اور بورڈ",
            "music" to "موسیقی اور رقص", "sports" to "کھیل اور بیرونی سرگرمیاں", "events" to "تقریبات اور جشن",
            "toys" to "کھلونے اور کردار", "crafts" to "فنون اور دستکاری", "objects" to "اشیا اور اوزار",
            "other" to "متفرق"
        ),
        contentTags = mapOf(
            "building" to "عمارتیں", "skyscraper" to "فلک بوس عمارتیں", "bridge" to "پل",
            "tower" to "مینار", "mosque" to "مساجد", "church" to "گرجا گھر",
            "castle" to "قلعے اور محل", "landmark" to "مشہور مقامات", "road" to "سڑکیں",
            "meal" to "کھانے", "fastfood" to "فاسٹ فوڈ", "sweets" to "مٹھائیاں",
            "bread" to "روٹی", "fruit" to "پھل", "vegetable" to "سبزیاں",
            "coffee" to "کافی", "drink" to "مشروبات", "soccer" to "فٹ بال",
            "swimming" to "آبی کھیل", "running" to "دوڑ اور ریس", "dance" to "رقص",
            "concert" to "کنسرٹ", "party" to "پارٹیاں", "wedding" to "شادیاں",
            "fireworks" to "آتش بازی", "fire" to "آگ", "car" to "گاڑیاں",
            "heavy" to "وین اور ٹرک", "bus" to "بسیں", "motorcycle" to "موٹر سائیکلیں",
            "bicycle" to "سائیکلیں", "train" to "ٹرینیں", "plane" to "طیارے",
            "helicopter" to "ہیلی کاپٹر", "boat" to "کشتیاں اور جہاز", "dog" to "کتے",
            "cat" to "بلیاں", "bird" to "پرندے", "horse" to "گھوڑے",
            "cattle" to "مویشی", "bear" to "ریچھ", "reptile" to "رینگنے والے جانور",
            "insect" to "کیڑے مکوڑے", "mountain" to "پہاڑ", "beach" to "ساحل",
            "desert" to "صحرا", "forest" to "جنگل", "water" to "جھیلیں اور دریا",
            "waterfall" to "آبشاریں", "sunset" to "غروبِ آفتاب", "flower" to "پھول",
            "ice" to "برف اور گلیشیئر", "storm" to "طوفان", "phone" to "فون",
            "computer" to "کمپیوٹر", "tv" to "ٹی وی", "instrument" to "ساز",
            "clock" to "گھڑیاں", "bag" to "بیگ", "glasses" to "عینکیں",
            "toy" to "کھلونے", "shoes" to "جوتے", "hat" to "ٹوپیاں",
            "dress" to "ملبوسات", "clothes" to "کپڑے", "jewellery" to "زیورات",
            "face" to "چہرے", "smile" to "مسکراہٹیں", "group" to "لوگوں کے گروپ",
            "baby" to "بچے", "crowd" to "ہجوم اور ٹیمیں", "text" to "متن",
            "code" to "QR اور بارکوڈ"
        ),
        contentGroups = mapOf(
            "Vehicles" to "گاڑیاں", "Animals" to "جانور", "Nature" to "فطرت",
            "Places" to "مقامات اور عمارتیں", "Food" to "کھانا اور مشروبات", "Activities" to "کھیل اور تقریبات",
            "Devices" to "آلات اور اشیا", "Fashion" to "فیشن", "People" to "لوگ اور متن"
        ),
        engines = mapOf(
            "google_lens" to "اشیا، مقامات اور متن کی شناخت کرتا ہے اور تصویر کا ماخذ تلاش کرتا ہے"
        )
    )
}
