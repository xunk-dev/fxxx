package com.frameout.core.quality

internal object TagTextCs {
    val text = TagText(
        labels = TagLabelsCs.MAP,
        fixed = mapOf(
            TagRules.FACE to "Obličeje", TagRules.GROUP to "Skupina",
            TagRules.SMILE to "Úsměv", TagRules.EYES_CLOSED to "Zavřené oči",
            TagRules.GOOD_FACE to "Dobrý obličej", TagRules.BODY to "Celá postava",
            TagRules.TEXT to "Obsahuje text", TagRules.CODE to "Libovolný kód",
            "b:contact" to "Kontakt", "b:email" to "E-mail",
            "b:isbn" to "ISBN", "b:phone" to "Telefon",
            "b:product" to "Kód produktu", "b:sms" to "SMS",
            "b:plain" to "Textový kód", "b:url" to "Odkaz",
            "b:wifi" to "Wi-Fi", "b:geo" to "Poloha",
            "b:event" to "Událost", "b:license" to "Řidičský průkaz",
            "o:fashion_good" to "Móda", "o:food" to "Jídlo",
            "o:home_good" to "Domácí potřeby", "o:place" to "Místa",
            "o:plant" to "Rostliny"
        ),
        groups = mapOf(
            "People" to "Lidé", "Things" to "Věci a prostředí", "Categories" to "Kategorie",
            "Text" to "Text", "Codes" to "Kódy"
        ),
        families = mapOf(
            "vehicles" to "Vozidla", "animals" to "Zvířata", "people" to "Lidé a tělo",
            "fashion" to "Móda a doplňky", "food" to "Jídlo a pití", "nature" to "Příroda a krajina",
            "plants" to "Rostliny a zahrada", "sky" to "Obloha a počasí", "places" to "Budovy a místa",
            "home" to "Domov a interiér", "devices" to "Zařízení a obrazovky", "documents" to "Dokumenty a tabule",
            "music" to "Hudba a tanec", "sports" to "Sport a venkovní aktivity", "events" to "Oslavy a události",
            "toys" to "Hračky a postavy", "crafts" to "Umění a řemesla", "objects" to "Předměty a nástroje"
        ),
        contentTags = mapOf(
            "building" to "Budovy", "skyscraper" to "Mrakodrapy", "bridge" to "Mosty",
            "tower" to "Věže", "mosque" to "Mešity", "church" to "Kostely",
            "castle" to "Hrady a zámky", "landmark" to "Památky", "road" to "Silnice",
            "meal" to "Jídla", "fastfood" to "Rychlé občerstvení", "sweets" to "Sladkosti a bonbóny",
            "bread" to "Chléb a pečivo", "fruit" to "Ovoce", "vegetable" to "Zelenina",
            "coffee" to "Káva", "drink" to "Nápoje", "soccer" to "Fotbal",
            "swimming" to "Vodní sporty", "running" to "Běh a závody", "dance" to "Tanec",
            "concert" to "Koncerty", "party" to "Párty", "wedding" to "Svatby",
            "fireworks" to "Ohňostroje", "fire" to "Oheň", "car" to "Auta",
            "heavy" to "Dodávky a nákladní auta", "bus" to "Autobusy", "motorcycle" to "Motocykly",
            "bicycle" to "Kola", "train" to "Vlaky", "plane" to "Letadla",
            "helicopter" to "Vrtulníky", "boat" to "Lodě a čluny", "dog" to "Psi",
            "cat" to "Kočky", "bird" to "Ptáci", "horse" to "Koně",
            "cattle" to "Skot a buvoli", "bear" to "Medvědi", "reptile" to "Plazi",
            "insect" to "Hmyz", "mountain" to "Hory", "beach" to "Pláže",
            "desert" to "Pouště", "forest" to "Lesy", "water" to "Jezera a řeky",
            "waterfall" to "Vodopády", "sunset" to "Západy slunce", "flower" to "Květiny",
            "ice" to "Led a ledovce", "storm" to "Bouře", "phone" to "Telefony",
            "computer" to "Počítače", "tv" to "Televize", "instrument" to "Hudební nástroje",
            "clock" to "Hodiny", "bag" to "Tašky", "glasses" to "Brýle",
            "toy" to "Hračky", "shoes" to "Obuv", "hat" to "Klobouky",
            "dress" to "Šaty", "clothes" to "Oblečení", "jewellery" to "Šperky",
            "face" to "Obličeje", "smile" to "Úsměvy", "group" to "Skupiny lidí",
            "baby" to "Miminka", "crowd" to "Davy a týmy", "text" to "Text",
            "code" to "QR a čárové kódy"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Vozidla", "Animals" to "Zvířata", "Nature" to "Příroda",
            "Places" to "Místa a budovy", "Food" to "Jídlo a pití", "Activities" to "Sport a události",
            "Devices" to "Zařízení a předměty", "Fashion" to "Móda", "People" to "Lidé a text"
        ),
        engines = mapOf(
            "google_lens" to "Rozpozná objekty, místa a text a najde zdroj obrázku"
        )
    )
}
