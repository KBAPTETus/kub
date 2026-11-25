package com.example.ui.components

import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.LocalKubAeroTokens
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.NativeAdLoader
import com.yandex.mobile.ads.nativeads.NativeAdView
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder

@Composable
fun KubNativeAdCard(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) return

    val context = LocalContext.current
    val tokens = LocalKubAeroTokens.current
    var loadedAd by remember(adUnitId) { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(adUnitId) {
        var loader: NativeAdLoader? = null
        try {
            loader = NativeAdLoader(context).apply {
                loadAd(
                    AdRequest.Builder(adUnitId).build(),
                    object : NativeAdLoadListener {
                        override fun onAdLoaded(nativeAd: NativeAd) {
                            loadedAd = nativeAd
                        }

                        override fun onAdFailedToLoad(error: AdRequestError) {
                            Log.d("KubNativeAdCard", "Ad failed to load ($adUnitId): ${error.description}")
                            loadedAd = null
                        }
                    }
                )
            }
        } catch (t: Throwable) {
            Log.w("KubNativeAdCard", "NativeAdLoader unavailable: ${t.message}")
            loadedAd = null
        }

        onDispose {
            try {
                loader?.cancelLoading()
            } catch (_: Throwable) {
            }
            loadedAd = null
        }
    }

    AnimatedVisibility(
        visible = loadedAd != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier.fillMaxWidth()
    ) {
        val nativeAd = loadedAd ?: return@AnimatedVisibility
        AeroGlassCard(cornerRadius = 16.dp) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    LayoutInflater.from(ctx).inflate(R.layout.view_kub_native_ad, null) as NativeAdView
                },
                update = { adView ->
                    val sponsoredView = adView.findViewById<TextView>(R.id.ad_sponsored)
                    val titleView = adView.findViewById<TextView>(R.id.ad_title)
                    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
                    val domainView = adView.findViewById<TextView>(R.id.ad_domain)
                    val warningView = adView.findViewById<TextView>(R.id.ad_warning)
                    val iconView = adView.findViewById<ImageView>(R.id.ad_icon)
                    val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)
                    val feedbackView = adView.findViewById<ImageView>(R.id.ad_feedback)

                    val density = adView.resources.displayMetrics.density
                    sponsoredView.setTextColor(tokens.textSub.toArgb())
                    sponsoredView.background = GradientDrawable().apply {
                        cornerRadius = 4f * density
                        setColor(KubMarineCobalt.copy(alpha = 0.16f).toArgb())
                    }

                    titleView.setTextColor(tokens.textMain.toArgb())
                    bodyView.setTextColor(tokens.textMuted.toArgb())
                    domainView.setTextColor(tokens.textSub.toArgb())
                    warningView.setTextColor(tokens.textSub.toArgb())

                    ctaView.setTextColor(tokens.btnPrimaryText.toArgb())
                    ctaView.backgroundTintList = null
                    ctaView.background = GradientDrawable().apply {
                        cornerRadius = 999f * density
                        setColor(KubMarineCobalt.toArgb())
                    }

                    val assets = nativeAd.adAssets
                    iconView.visibility = if (assets.icon != null) View.VISIBLE else View.GONE
                    domainView.visibility = if (!assets.domain.isNullOrBlank()) View.VISIBLE else View.GONE
                    warningView.visibility = if (!assets.warning?.value.isNullOrBlank()) View.VISIBLE else View.GONE
                    feedbackView.visibility = if (assets.isFeedbackAvailable) View.VISIBLE else View.GONE

                    try {
                        val binder = NativeAdViewBinder.Builder(adView)
                            .setTitleView(titleView)
                            .setBodyView(bodyView)
                            .setIconView(iconView)
                            .setCallToActionView(ctaView)
                            .setDomainView(domainView)
                            .setSponsoredView(sponsoredView)
                            .setWarningView(warningView)
                            .setFeedbackView(feedbackView)
                            .build()
                        nativeAd.bindNativeAd(binder)
                    } catch (t: Throwable) {
                        Log.w("KubNativeAdCard", "Failed to bind native ad: ${t.message}")
                    }
                }
            )
        }
    }
}
