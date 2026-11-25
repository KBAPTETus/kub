package com.example.ads

import com.example.BuildConfig

object KubAdConfig {
    private const val DEMO_NATIVE_AD_UNIT_ID = "demo-native-content-yandex"

    val nativeAdUnitId: String
        get() = if (BuildConfig.DEBUG) {
            DEMO_NATIVE_AD_UNIT_ID
        } else {
            BuildConfig.YANDEX_AD_UNIT_ID.ifBlank { DEMO_NATIVE_AD_UNIT_ID }
        }
}
