package com.kaafisir.auramic.data.pro

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

private val Context.proStore: DataStore<Preferences> by preferencesDataStore(name = "auramic_pro")

/**
 * Google Play Billing implementation. The app has no server: Play's own on-device cache decides whether the
 * subscription is active, and the last known answer is remembered so the app keeps working offline.
 */
@Singleton
class PlayProAccess @Inject constructor(@ApplicationContext private val context: Context) : ProAccess {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lock = Mutex()
    private val _state = MutableStateFlow(ProState())
    override val state: StateFlow<ProState> = _state.asStateFlow()

    private val subActive = booleanPreferencesKey("sub_active")
    private val subVerifiedAt = longPreferencesKey("sub_verified_at")

    private val trial = TrialCounter(
        listOf(DataStoreTrialSource(context.proStore), MediaStoreTrialSource(context)),
        TrialCounter.keyFor(context),
    )

    private var client: BillingClient? = null
    private var details: ProductDetails? = null

    init {
        scope.launch {
            // Cached answer first so the first voice note after launch already knows the plan.
            val p = runCatching { context.proStore.data.first() }.getOrNull()
            val cached = p?.get(subActive) == true && System.currentTimeMillis() - (p[subVerifiedAt] ?: 0L) < OFFLINE_GRACE_MS
            val used = runCatching { trial.used() }.getOrDefault(0)
            _state.update { it.copy(subscribed = it.subscribed || cached, trialUsed = maxOf(it.trialUsed, used)) }
        }
    }

    private val listener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { applyPurchases(purchases, reachable = true) }
        }
    }

    private suspend fun connect(): BillingClient? {
        client?.takeIf { it.isReady }?.let { return it }
        val c = client ?: BillingClient.newBuilder(context)
            .setListener(listener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build().also { client = it }
        val ready = CompletableDeferred<Boolean>()
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) { ready.complete(r.responseCode == BillingClient.BillingResponseCode.OK) }
            override fun onBillingServiceDisconnected() { ready.complete(false) }
        })
        return if (withTimeoutOrNull(CONNECT_TIMEOUT_MS) { ready.await() } == true) c else null
    }

    override suspend fun refresh() = lock.withLock {
        val used = runCatching { trial.used() }.getOrDefault(_state.value.trialUsed)
        _state.update { it.copy(trialUsed = maxOf(it.trialUsed, used)) }
        val c = runCatching { connect() }.getOrNull()
        if (c == null) {
            applyPurchases(null, reachable = false)   // keep the cached answer
            return@withLock
        }
        runCatching {
            val purchases = c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
            if (purchases.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                applyPurchases(purchases.purchasesList, reachable = true)
            } else applyPurchases(null, reachable = false)

            val q = QueryProductDetailsParams.newBuilder().setProductList(
                listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.SUBS).build()),
            ).build()
            val r = c.queryProductDetails(q)
            details = r.productDetailsList?.firstOrNull()
            val offers = details?.subscriptionOfferDetails.orEmpty().mapNotNull { o ->
                val plan = Plan.entries.firstOrNull { it.basePlanId == o.basePlanId } ?: return@mapNotNull null
                val price = o.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice ?: return@mapNotNull null
                PlanOffer(plan, price)
            }.distinctBy { it.plan }.sortedBy { it.plan.ordinal }
            _state.update { it.copy(offers = offers) }
        }.onFailure { Log.w(TAG, "refresh failed", it) }
        Unit
    }

    private suspend fun applyPurchases(purchases: List<Purchase>?, reachable: Boolean) {
        if (!reachable || purchases == null) {
            // Store unreachable: trust the last known answer for a while (the app is fully usable offline).
            val p = runCatching { context.proStore.data.first() }.getOrNull()
            val ok = p?.get(subActive) == true && System.currentTimeMillis() - (p[subVerifiedAt] ?: 0L) < OFFLINE_GRACE_MS
            _state.update { it.copy(subscribed = ok) }
            return
        }
        val active = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && PRODUCT_ID in it.products }
        active.filter { !it.isAcknowledged }.forEach { p ->
            runCatching { client?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) }
        }
        val subscribed = active.isNotEmpty()
        runCatching {
            context.proStore.edit { it[subActive] = subscribed; it[subVerifiedAt] = System.currentTimeMillis() }
        }
        _state.update { it.copy(subscribed = subscribed) }
    }

    override fun purchase(activity: Activity, plan: Plan): Boolean {
        val c = client?.takeIf { it.isReady } ?: return false
        val d = details ?: return false
        val offer = d.subscriptionOfferDetails?.firstOrNull { it.basePlanId == plan.basePlanId } ?: return false
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).setOfferToken(offer.offerToken).build()),
        ).build()
        return c.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    override suspend fun consumeFreeHighNote() {
        if (_state.value.subscribed) return
        val used = runCatching { trial.increment() }.getOrDefault(_state.value.trialUsed + 1)
        _state.update { it.copy(trialUsed = maxOf(it.trialUsed, used)) }
    }

    companion object {
        const val PRODUCT_ID = "auramic_pro"
        private const val TAG = "ProAccess"
        private const val CONNECT_TIMEOUT_MS = 8_000L
        private const val OFFLINE_GRACE_MS = 14L * 24 * 60 * 60 * 1000
    }
}
