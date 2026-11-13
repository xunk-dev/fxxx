package com.frameout.core.quality

internal object TagTextBn {
    val text = TagText(
        labels = TagLabelsBn.MAP,
        fixed = mapOf(
            TagRules.FACE to "মুখ", TagRules.GROUP to "দল",
            TagRules.SMILE to "হাসি", TagRules.EYES_CLOSED to "বন্ধ চোখ",
            TagRules.GOOD_FACE to "ভালো মুখ", TagRules.BODY to "পুরো শরীর",
            TagRules.TEXT to "লেখাসহ", TagRules.CODE to "যেকোনো কোড",
            "b:contact" to "পরিচিতি", "b:email" to "ইমেল",
            "b:isbn" to "ISBN", "b:phone" to "ফোন",
            "b:product" to "পণ্যের কোড", "b:sms" to "SMS",
            "b:plain" to "টেক্সট কোড", "b:url" to "লিংক",
            "b:wifi" to "Wi-Fi", "b:geo" to "অবস্থান",
            "b:event" to "ইভেন্ট", "b:license" to "লাইসেন্স",
            "o:fashion_good" to "ফ্যাশন", "o:food" to "খাবার",
            "o:home_good" to "ঘরের জিনিস", "o:place" to "স্থান",
            "o:plant" to "উদ্ভিদ"
        ),
        groups = mapOf(
            "People" to "মানুষ", "Things" to "বস্তু ও দৃশ্য", "Categories" to "বিভাগ",
            "Text" to "লেখা", "Codes" to "কোড"
        ),
        families = mapOf(
            "vehicles" to "যানবাহন", "animals" to "প্রাণী", "people" to "মানুষ ও শরীর",
            "fashion" to "ফ্যাশন ও অ্যাক্সেসরিজ", "food" to "খাবার ও পানীয়", "nature" to "প্রকৃতি ও ভূদৃশ্য",
            "plants" to "গাছপালা ও বাগান", "sky" to "আকাশ ও আবহাওয়া", "places" to "ভবন ও স্থান",
            "home" to "ঘর ও ইন্টেরিয়র", "devices" to "ডিভাইস ও স্ক্রিন", "documents" to "নথি ও বোর্ড",
            "music" to "সংগীত ও নাচ", "sports" to "খেলাধুলা ও বাইরের কার্যকলাপ", "events" to "উৎসব ও অনুষ্ঠান",
            "toys" to "খেলনা ও চরিত্র", "crafts" to "শিল্প ও কারুকাজ", "objects" to "জিনিসপত্র ও যন্ত্রপাতি"
        ),
        contentTags = mapOf(
            "building" to "ভবন", "skyscraper" to "আকাশচুম্বী ভবন", "bridge" to "সেতু",
            "tower" to "টাওয়ার", "mosque" to "মসজিদ", "church" to "গির্জা",
            "castle" to "দুর্গ ও প্রাসাদ", "landmark" to "স্মৃতিস্তম্ভ ও দর্শনীয় স্থান", "road" to "রাস্তা",
            "meal" to "খাবার", "fastfood" to "ফাস্ট ফুড", "sweets" to "মিষ্টি",
            "bread" to "রুটি", "fruit" to "ফল", "vegetable" to "সবজি",
            "coffee" to "কফি", "drink" to "পানীয়", "soccer" to "ফুটবল",
            "swimming" to "জলক্রীড়া", "running" to "দৌড় ও প্রতিযোগিতা", "dance" to "নাচ",
            "concert" to "কনসার্ট", "party" to "পার্টি", "wedding" to "বিয়ে",
            "fireworks" to "আতশবাজি", "fire" to "আগুন", "car" to "গাড়ি",
            "heavy" to "ভ্যান ও ট্রাক", "bus" to "বাস", "motorcycle" to "মোটরসাইকেল",
            "bicycle" to "সাইকেল", "train" to "ট্রেন", "plane" to "বিমান",
            "helicopter" to "হেলিকপ্টার", "boat" to "নৌকা ও জাহাজ", "dog" to "কুকুর",
            "cat" to "বিড়াল", "bird" to "পাখি", "horse" to "ঘোড়া",
            "cattle" to "গবাদি পশু", "bear" to "ভালুক", "reptile" to "সরীসৃপ",
            "insect" to "পোকামাকড়", "mountain" to "পাহাড়", "beach" to "সমুদ্রসৈকত",
            "desert" to "মরুভূমি", "forest" to "বন", "water" to "হ্রদ ও নদী",
            "waterfall" to "জলপ্রপাত", "sunset" to "সূর্যাস্ত", "flower" to "ফুল",
            "ice" to "বরফ ও হিমবাহ", "storm" to "ঝড়", "phone" to "ফোন",
            "computer" to "কম্পিউটার", "tv" to "টিভি", "instrument" to "বাদ্যযন্ত্র",
            "clock" to "ঘড়ি", "bag" to "ব্যাগ", "glasses" to "চশমা",
            "toy" to "খেলনা", "shoes" to "জুতা", "hat" to "টুপি",
            "dress" to "পোশাক", "clothes" to "কাপড়চোপড়", "jewellery" to "গয়না",
            "face" to "মুখ", "smile" to "হাসি", "group" to "মানুষের দল",
            "baby" to "শিশু", "crowd" to "ভিড় ও দল", "text" to "লেখা",
            "code" to "QR কোড ও বারকোড"
        ),
        contentGroups = mapOf(
            "Vehicles" to "যানবাহন", "Animals" to "প্রাণী", "Nature" to "প্রকৃতি",
            "Places" to "স্থান ও ভবন", "Food" to "খাবার ও পানীয়", "Activities" to "খেলাধুলা ও অনুষ্ঠান",
            "Devices" to "ডিভাইস ও জিনিসপত্র", "Fashion" to "ফ্যাশন", "People" to "মানুষ ও লেখা"
        ),
        engines = mapOf(
            "google_lens" to "বস্তু, স্থান ও লেখা শনাক্ত করে এবং ছবির উৎস খুঁজে দেয়"
        )
    )
}
