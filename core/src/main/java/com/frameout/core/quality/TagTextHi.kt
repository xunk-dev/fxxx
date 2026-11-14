package com.frameout.core.quality

internal object TagTextHi {
    val text = TagText(
        labels = TagLabelsHi.MAP,
        fixed = mapOf(
            TagRules.FACE to "चेहरे", TagRules.GROUP to "समूह",
            TagRules.SMILE to "मुस्कान", TagRules.EYES_CLOSED to "बंद आँखें",
            TagRules.GOOD_FACE to "ठीक चेहरा", TagRules.BODY to "पूरा शरीर",
            TagRules.TEXT to "टेक्स्ट वाला", TagRules.CODE to "कोई भी कोड",
            "b:contact" to "संपर्क", "b:email" to "ईमेल",
            "b:isbn" to "ISBN", "b:phone" to "फ़ोन",
            "b:product" to "उत्पाद कोड", "b:sms" to "SMS",
            "b:plain" to "टेक्स्ट कोड", "b:url" to "लिंक",
            "b:wifi" to "Wi-Fi", "b:geo" to "स्थान",
            "b:event" to "कार्यक्रम", "b:license" to "लाइसेंस",
            "o:fashion_good" to "फ़ैशन", "o:food" to "खाद्य पदार्थ",
            "o:home_good" to "घरेलू सामान", "o:place" to "स्थान",
            "o:plant" to "पौधे"
        ),
        groups = mapOf(
            "People" to "लोग", "Things" to "वस्तुएँ और दृश्य", "Categories" to "श्रेणियाँ",
            "Text" to "टेक्स्ट", "Codes" to "कोड"
        ),
        families = mapOf(
            "vehicles" to "वाहन", "animals" to "जानवर", "people" to "लोग और शरीर",
            "fashion" to "फ़ैशन और एक्सेसरीज़", "food" to "खाना-पीना", "nature" to "प्रकृति और भू-दृश्य",
            "plants" to "पौधे और बगीचे", "sky" to "आसमान और मौसम", "places" to "इमारतें और स्थान",
            "home" to "घर और इंटीरियर", "devices" to "उपकरण और स्क्रीन", "documents" to "दस्तावेज़ और बोर्ड",
            "music" to "संगीत और नृत्य", "sports" to "खेल और बाहरी गतिविधियाँ", "events" to "उत्सव और कार्यक्रम",
            "toys" to "खिलौने और किरदार", "crafts" to "कला और शिल्प", "objects" to "वस्तुएँ और औज़ार",
            "other" to "अन्य"
        ),
        contentTags = mapOf(
            "building" to "इमारतें", "skyscraper" to "गगनचुंबी इमारतें", "bridge" to "पुल",
            "tower" to "मीनारें", "mosque" to "मस्जिदें", "church" to "चर्च",
            "castle" to "क़िले और महल", "landmark" to "स्मारक", "road" to "सड़कें",
            "meal" to "भोजन", "fastfood" to "फ़ास्ट फ़ूड", "sweets" to "मिठाइयाँ",
            "bread" to "ब्रेड", "fruit" to "फल", "vegetable" to "सब्ज़ियाँ",
            "coffee" to "कॉफ़ी", "drink" to "पेय", "soccer" to "फ़ुटबॉल",
            "swimming" to "जल क्रीड़ाएँ", "running" to "दौड़ और प्रतियोगिताएँ", "dance" to "नृत्य",
            "concert" to "संगीत समारोह", "party" to "पार्टियाँ", "wedding" to "शादियाँ",
            "fireworks" to "आतिशबाज़ी", "fire" to "आग", "car" to "कारें",
            "heavy" to "वैन और ट्रक", "bus" to "बसें", "motorcycle" to "मोटरसाइकिलें",
            "bicycle" to "साइकिलें", "train" to "ट्रेनें", "plane" to "हवाई जहाज़",
            "helicopter" to "हेलीकॉप्टर", "boat" to "नावें और जहाज़", "dog" to "कुत्ते",
            "cat" to "बिल्लियाँ", "bird" to "पक्षी", "horse" to "घोड़े",
            "cattle" to "मवेशी", "bear" to "भालू", "reptile" to "सरीसृप",
            "insect" to "कीड़े", "mountain" to "पहाड़", "beach" to "समुद्र तट",
            "desert" to "रेगिस्तान", "forest" to "जंगल", "water" to "झीलें और नदियाँ",
            "waterfall" to "झरने", "sunset" to "सूर्यास्त", "flower" to "फूल",
            "ice" to "बर्फ़ और हिमनद", "storm" to "तूफ़ान", "phone" to "फ़ोन",
            "computer" to "कंप्यूटर", "tv" to "टेलीविज़न", "instrument" to "वाद्य यंत्र",
            "clock" to "घड़ियाँ", "bag" to "बैग", "glasses" to "चश्मे",
            "toy" to "खिलौने", "shoes" to "जूते", "hat" to "टोपियाँ",
            "dress" to "पोशाकें", "clothes" to "कपड़े", "jewellery" to "गहने",
            "face" to "चेहरे", "smile" to "मुस्कानें", "group" to "लोगों के समूह",
            "baby" to "शिशु", "crowd" to "भीड़ और टीमें", "text" to "टेक्स्ट",
            "code" to "QR कोड और बारकोड"
        ),
        contentGroups = mapOf(
            "Vehicles" to "वाहन", "Animals" to "जानवर", "Nature" to "प्रकृति",
            "Places" to "स्थान और इमारतें", "Food" to "खाना-पीना", "Activities" to "खेल और कार्यक्रम",
            "Devices" to "उपकरण और वस्तुएँ", "Fashion" to "फ़ैशन", "People" to "लोग और टेक्स्ट"
        ),
        engines = mapOf(
            "google_lens" to "वस्तुओं, स्थानों और टेक्स्ट को पहचानता है और इमेज का स्रोत खोजता है"
        )
    )
}
