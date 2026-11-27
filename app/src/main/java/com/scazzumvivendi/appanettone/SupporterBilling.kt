package com.scazzumvivendi.appanettone

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

data class SupporterState(
    val owned: Boolean = false,
    val pending: Boolean = false,
    val loading: Boolean = true,
    val purchasing: Boolean = false,
    val price: String? = null,
    val purchaseFailed: Boolean = false,
)

class SupporterBilling(private val activity: Activity) {
    private val preferences = activity.getSharedPreferences("supporter", Context.MODE_PRIVATE)
    var state by mutableStateOf(SupporterState(owned = preferences.getBoolean("owned", false)))
        private set

    private var connecting = false
    private var closed = false
    private var ownershipQuery = 0
    private var catalogQuery = 0
    private var product: ProductDetails? = null
    private var offer: ProductDetails.OneTimePurchaseOfferDetails? = null
    private val acknowledgements = mutableSetOf<String>()

    private val client = BillingClient.newBuilder(activity.applicationContext)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .setListener { result, purchases ->
            onMain {
                state = state.copy(
                    purchasing = false,
                    purchaseFailed = result.responseCode !in NON_ERROR_PURCHASE_RESPONSES,
                )
                when (result.responseCode) {
                    BillingResponseCode.OK -> {
                        ownershipQuery++
                        purchases.orEmpty().firstOrNull { PRODUCT_ID in it.products }?.let {
                            updateOwnership(listOf(it))
                        }
                        refresh()
                    }
                    BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
                }
            }
        }
        .build()

    fun refresh() {
        if (connecting || closed) return
        if (client.isReady) {
            queryPlay()
            return
        }

        connecting = true
        state = state.copy(loading = true, price = null)
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) = onMain {
                connecting = false
                if (result.responseCode == BillingResponseCode.OK) {
                    queryPlay()
                } else {
                    Log.w(TAG, "Play Billing setup failed: response=${result.responseCode}")
                    state = state.copy(loading = false)
                }
            }

            override fun onBillingServiceDisconnected() = onMain {
                connecting = false
                catalogQuery++
                state = state.copy(loading = false, price = null)
            }
        })
    }

    private fun queryPlay() {
        if (state.price == null) state = state.copy(loading = true)
        val ownershipRequest = ++ownershipQuery
        val purchases = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
        client.queryPurchasesAsync(purchases.build()) { result, items ->
            onMain {
                if (ownershipRequest == ownershipQuery && result.responseCode == BillingResponseCode.OK) {
                    updateOwnership(items)
                }
            }
        }

        val catalogRequest = ++catalogQuery
        val item = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val query = QueryProductDetailsParams.newBuilder().setProductList(listOf(item)).build()
        client.queryProductDetailsAsync(query) { result, details ->
            onMain {
                if (catalogRequest == catalogQuery) {
                    product = details.productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
                        .takeIf { result.responseCode == BillingResponseCode.OK }
                    val matchingOffer = product?.oneTimePurchaseOfferDetailsList?.firstOrNull {
                        it.purchaseOptionId == PURCHASE_OPTION_ID && it.offerId == null && it.offerToken != null
                    }
                    offer = matchingOffer ?: product?.oneTimePurchaseOfferDetails?.takeIf {
                        it.offerId == null && (it.purchaseOptionId == null || it.purchaseOptionId == PURCHASE_OPTION_ID)
                    }
                    if (offer == null) {
                        val status = details.unfetchedProductList.firstOrNull { it.productId == PRODUCT_ID }?.statusCode
                        Log.w(TAG, "Supporter product unavailable: response=${result.responseCode}, unfetchedStatus=$status, productFound=${product != null}")
                    }
                    state = state.copy(loading = false, price = offer?.formattedPrice)
                }
            }
        }
    }

    fun purchase() {
        if (state.owned || state.pending || state.purchasing || state.price == null) return
        val details = product ?: return
        val selectedOffer = offer ?: return
        val item = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        selectedOffer.offerToken?.let(item::setOfferToken)
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(item.build())).build()
        state = state.copy(purchasing = true, purchaseFailed = false)
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) {
            state = state.copy(
                purchasing = false,
                purchaseFailed = result.responseCode !in NON_ERROR_PURCHASE_RESPONSES,
            )
            refresh()
        }
    }

    private fun updateOwnership(purchases: List<Purchase>) {
        val supporterPurchases = purchases.filter { PRODUCT_ID in it.products }
        val completed = supporterPurchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val owned = completed.isNotEmpty()
        if (owned != state.owned) {
            preferences.edit().putBoolean("owned", owned).apply()
        }
        state = state.copy(
            owned = owned,
            pending = supporterPurchases.any { it.purchaseState == Purchase.PurchaseState.PENDING },
        )
        completed.filterNot { it.isAcknowledged }.forEach { purchase ->
            val token = purchase.purchaseToken
            if (acknowledgements.add(token)) {
                val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()
                client.acknowledgePurchase(params) { onMain { acknowledgements.remove(token) } }
            }
        }
    }

    fun close() {
        closed = true
        client.endConnection()
    }

    private fun onMain(action: () -> Unit) {
        activity.runOnUiThread { if (!closed) action() }
    }

    private companion object {
        const val TAG = "SupporterBilling"
        const val PRODUCT_ID = "supporter_badge"
        const val PURCHASE_OPTION_ID = "acquisto-badge"
        val NON_ERROR_PURCHASE_RESPONSES = setOf(
            BillingResponseCode.OK,
            BillingResponseCode.USER_CANCELED,
            BillingResponseCode.ITEM_ALREADY_OWNED,
        )
    }
}
