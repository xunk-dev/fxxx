package com.frameout.core.quality

internal object TagTextNb {
    val text = TagText(
        labels = TagLabelsNb.MAP,
        fixed = mapOf(
            TagRules.FACE to "Ansikter", TagRules.GROUP to "Gruppe",
            TagRules.SMILE to "Smil", TagRules.EYES_CLOSED to "Lukkede øyne",
            TagRules.GOOD_FACE to "Vellykket ansikt", TagRules.BODY to "Hel person",
            TagRules.TEXT to "Inneholder tekst", TagRules.CODE to "Enhver kode",
            "b:contact" to "Kontakt", "b:email" to "E-post",
            "b:isbn" to "ISBN", "b:phone" to "Telefon",
            "b:product" to "Produktkode", "b:sms" to "SMS",
            "b:plain" to "Tekstkode", "b:url" to "Lenke",
            "b:wifi" to "Wi-Fi", "b:geo" to "Posisjon",
            "b:event" to "Arrangement", "b:license" to "Førerkort",
            "o:fashion_good" to "Mote", "o:food" to "Mat",
            "o:home_good" to "Husholdningsvarer", "o:place" to "Steder",
            "o:plant" to "Planter"
        ),
        groups = mapOf(
            "People" to "Mennesker", "Things" to "Ting og scener", "Categories" to "Kategorier",
            "Text" to "Tekst", "Codes" to "Koder"
        ),
        families = mapOf(
            "vehicles" to "Kjøretøy", "animals" to "Dyr", "people" to "Mennesker og kropp",
            "fashion" to "Mote og tilbehør", "food" to "Mat og drikke", "nature" to "Natur og landskap",
            "plants" to "Planter og hager", "sky" to "Himmel og vær", "places" to "Bygninger og steder",
            "home" to "Hjem og interiør", "devices" to "Enheter og skjermer", "documents" to "Dokumenter og tavler",
            "music" to "Musikk og dans", "sports" to "Sport og friluftsliv", "events" to "Fester og arrangementer",
            "toys" to "Leker og figurer", "crafts" to "Kunst og håndverk", "objects" to "Gjenstander og verktøy",
            "other" to "Annet"
        ),
        contentTags = mapOf(
            "building" to "Bygninger", "skyscraper" to "Skyskrapere", "bridge" to "Broer",
            "tower" to "Tårn", "mosque" to "Moskeer", "church" to "Kirker",
            "castle" to "Slott og palasser", "landmark" to "Landemerker", "road" to "Veier",
            "meal" to "Retter", "fastfood" to "Hurtigmat", "sweets" to "Søtsaker",
            "bread" to "Brød", "fruit" to "Frukt", "vegetable" to "Grønnsaker",
            "coffee" to "Kaffe", "drink" to "Drikke", "soccer" to "Fotball",
            "swimming" to "Vannsport", "running" to "Løp og konkurranser", "dance" to "Dans",
            "concert" to "Konserter", "party" to "Fester", "wedding" to "Bryllup",
            "fireworks" to "Fyrverkeri", "fire" to "Ild", "car" to "Biler",
            "heavy" to "Varebiler og lastebiler", "bus" to "Busser", "motorcycle" to "Motorsykler",
            "bicycle" to "Sykler", "train" to "Tog", "plane" to "Fly",
            "helicopter" to "Helikoptre", "boat" to "Båter og skip", "dog" to "Hunder",
            "cat" to "Katter", "bird" to "Fugler", "horse" to "Hester",
            "cattle" to "Storfe", "bear" to "Bjørner", "reptile" to "Krypdyr",
            "insect" to "Insekter", "mountain" to "Fjell", "beach" to "Strender",
            "desert" to "Ørken", "forest" to "Skoger", "water" to "Innsjøer og elver",
            "waterfall" to "Fosser", "sunset" to "Solnedganger", "flower" to "Blomster",
            "ice" to "Is og isbreer", "storm" to "Uvær", "phone" to "Telefoner",
            "computer" to "Datamaskiner", "tv" to "TV-er", "instrument" to "Musikkinstrumenter",
            "clock" to "Klokker", "bag" to "Vesker", "glasses" to "Briller",
            "toy" to "Leketøy", "shoes" to "Sko", "hat" to "Hatter",
            "dress" to "Kjoler", "clothes" to "Klær", "jewellery" to "Smykker",
            "face" to "Ansikter", "smile" to "Smil", "group" to "Persongrupper",
            "baby" to "Babyer", "crowd" to "Folkemengder og lag", "text" to "Tekst",
            "code" to "QR- og strekkoder"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Kjøretøy", "Animals" to "Dyr", "Nature" to "Natur",
            "Places" to "Steder og bygninger", "Food" to "Mat og drikke", "Activities" to "Sport og arrangementer",
            "Devices" to "Enheter og gjenstander", "Fashion" to "Mote", "People" to "Mennesker og tekst"
        ),
        engines = mapOf(
            "google_lens" to "Gjenkjenner objekter, steder og tekst og finner bildets opprinnelse"
        )
    )
}
