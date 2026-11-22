plugins {
    alias(libs.plugins.android.asset.pack)
}

assetPack {
    packName.set("ara_model")
    dynamicDelivery {
        deliveryType.set("fast-follow")
    }
}
