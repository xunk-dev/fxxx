package com.frameout.core.quality

internal object TagTextEs {
    val text = TagText(
        labels = TagLabelsEs.MAP,
        fixed = mapOf(
            TagRules.FACE to "Rostros", TagRules.GROUP to "Grupo",
            TagRules.SMILE to "Sonrisa", TagRules.EYES_CLOSED to "Ojos cerrados",
            TagRules.GOOD_FACE to "Buen rostro", TagRules.BODY to "Cuerpo entero",
            TagRules.TEXT to "Contiene texto", TagRules.CODE to "Cualquier código",
            "b:contact" to "Contacto", "b:email" to "Correo electrónico",
            "b:isbn" to "ISBN", "b:phone" to "Teléfono",
            "b:product" to "Código de producto", "b:sms" to "SMS",
            "b:plain" to "Código de texto", "b:url" to "Enlace",
            "b:wifi" to "Wi-Fi", "b:geo" to "Ubicación",
            "b:event" to "Evento", "b:license" to "Licencia de conducir",
            "o:fashion_good" to "Moda", "o:food" to "Alimentos",
            "o:home_good" to "Artículos para el hogar", "o:place" to "Lugares",
            "o:plant" to "Plantas"
        ),
        groups = mapOf(
            "People" to "Personas", "Things" to "Objetos y escenas", "Categories" to "Categorías",
            "Text" to "Texto", "Codes" to "Códigos"
        ),
        families = mapOf(
            "vehicles" to "Vehículos", "animals" to "Animales", "people" to "Personas y cuerpo",
            "fashion" to "Moda y accesorios", "food" to "Comida y bebida", "nature" to "Naturaleza y paisajes",
            "plants" to "Plantas y jardines", "sky" to "Cielo y clima", "places" to "Edificios y lugares",
            "home" to "Hogar y decoración", "devices" to "Dispositivos y pantallas", "documents" to "Documentos y pizarras",
            "music" to "Música y baile", "sports" to "Deporte y aire libre", "events" to "Fiestas y eventos",
            "toys" to "Juguetes y figuras", "crafts" to "Arte y manualidades", "objects" to "Objetos y herramientas",
            "other" to "Otros"
        ),
        contentTags = mapOf(
            "building" to "Edificios", "skyscraper" to "Rascacielos", "bridge" to "Puentes",
            "tower" to "Torres", "mosque" to "Mezquitas", "church" to "Iglesias",
            "castle" to "Castillos y palacios", "landmark" to "Lugares emblemáticos", "road" to "Carreteras",
            "meal" to "Platos", "fastfood" to "Comida rápida", "sweets" to "Dulces",
            "bread" to "Pan", "fruit" to "Fruta", "vegetable" to "Verduras",
            "coffee" to "Café", "drink" to "Bebidas", "soccer" to "Fútbol",
            "swimming" to "Deportes acuáticos", "running" to "Carreras y competiciones", "dance" to "Baile",
            "concert" to "Conciertos", "party" to "Fiestas", "wedding" to "Bodas",
            "fireworks" to "Fuegos artificiales", "fire" to "Fuego", "car" to "Autos",
            "heavy" to "Camionetas y camiones", "bus" to "Autobuses", "motorcycle" to "Motocicletas",
            "bicycle" to "Bicicletas", "train" to "Trenes", "plane" to "Aviones",
            "helicopter" to "Helicópteros", "boat" to "Barcos y buques", "dog" to "Perros",
            "cat" to "Gatos", "bird" to "Aves", "horse" to "Caballos",
            "cattle" to "Ganado vacuno", "bear" to "Osos", "reptile" to "Reptiles",
            "insect" to "Insectos", "mountain" to "Montañas", "beach" to "Playas",
            "desert" to "Desierto", "forest" to "Bosques", "water" to "Lagos y ríos",
            "waterfall" to "Cascadas", "sunset" to "Atardeceres", "flower" to "Flores",
            "ice" to "Hielo y glaciares", "storm" to "Tormentas", "phone" to "Teléfonos",
            "computer" to "Computadoras", "tv" to "Televisores", "instrument" to "Instrumentos musicales",
            "clock" to "Relojes", "bag" to "Bolsas", "glasses" to "Lentes",
            "toy" to "Juguetes", "shoes" to "Zapatos", "hat" to "Sombreros",
            "dress" to "Vestidos", "clothes" to "Ropa", "jewellery" to "Joyería",
            "face" to "Rostros", "smile" to "Sonrisas", "group" to "Grupos de personas",
            "baby" to "Bebés", "crowd" to "Multitudes y equipos", "text" to "Texto",
            "code" to "Códigos QR y de barras"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Vehículos", "Animals" to "Animales", "Nature" to "Naturaleza",
            "Places" to "Lugares y edificios", "Food" to "Comida y bebida", "Activities" to "Deporte y eventos",
            "Devices" to "Dispositivos y objetos", "Fashion" to "Moda", "People" to "Personas y texto"
        ),
        engines = mapOf(
            "google_lens" to "Reconoce objetos, lugares y texto, y encuentra el origen de la imagen"
        )
    )
}
