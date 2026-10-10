package com.kaafisir.auramic.automation

import android.content.Intent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class SendRequest(val id: Long, val chatName: String?, val targetPackage: String, val allowFirstRow: Boolean = false)

sealed interface SendResult {
    data object Sent : SendResult
    data class Failed(val reason: String) : SendResult
}

/**
 * In-process rendezvous between the overlay (which records and shares) and the accessibility
 * service (which can see and click inside WhatsApp). Both are separate Android components that
 * the system creates, so they meet through this singleton.
 */
@Singleton
class AutomationBridge @Inject constructor() {

    /** Registered by the accessibility service while it is connected. */
    @Volatile var chatTitleProvider: (() -> String?)? = null
    /** Lets the accessibility service launch the share intent (it is exempt from background-start limits). */
    @Volatile var activityLauncher: ((Intent) -> Boolean)? = null

    /** Last screen structure seen by a failed / finished flow (see [NodeDump]); copied by Settings -> Copy diagnostics. */
    @Volatile var diagnostics: String? = null

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()
    fun setConnected(value: Boolean) { _connected.value = value }

    /** Where WhatsApp's own voice button is right now (null = not in a chat / not found). Written by the accessibility service. */
    private val _micAnchor = MutableStateFlow<MicAnchor?>(null)
    val micAnchor: StateFlow<MicAnchor?> = _micAnchor.asStateFlow()
    fun setMicAnchor(a: MicAnchor?) { _micAnchor.value = a }

    /** True while a conversation (not the chat list / settings / status screens) is open in WhatsApp. */
    private val _chatOpen = MutableStateFlow(false)
    val chatOpen: StateFlow<Boolean> = _chatOpen.asStateFlow()
    fun setChatOpen(open: Boolean) { _chatOpen.value = open }

    /** True while the bubble is on screen: the accessibility service then keeps [micAnchor] up to date. */
    private val _anchorTracking = MutableStateFlow(false)
    val anchorTracking: StateFlow<Boolean> = _anchorTracking.asStateFlow()
    fun setAnchorTracking(on: Boolean) { _anchorTracking.value = on; if (!on) { _micAnchor.value = null; _chatOpen.value = false } }

    private val _pending = MutableStateFlow<SendRequest?>(null)
    val pending: StateFlow<SendRequest?> = _pending.asStateFlow()
    @Volatile private var waiter: CompletableDeferred<SendResult>? = null
    private var nextId = 1L

    /** Name of the chat that is open in WhatsApp right now, as seen by accessibility. */
    fun currentChatTitle(): String? = runCatching { chatTitleProvider?.invoke() }.getOrNull()

    /** Posts a request for the accessibility service and suspends until it finishes or times out. */
    suspend fun sendAndAwait(chatName: String?, targetPackage: String, allowFirstRow: Boolean = false, timeoutMs: Long = 30_000): SendResult {
        val deferred = CompletableDeferred<SendResult>()
        val request = synchronized(this) {
            waiter = deferred
            SendRequest(nextId++, chatName, targetPackage, allowFirstRow).also { _pending.value = it }
        }
        val result = withTimeoutOrNull(timeoutMs) { deferred.await() } ?: SendResult.Failed("timeout")
        synchronized(this) {
            if (_pending.value?.id == request.id) _pending.value = null
            if (waiter === deferred) waiter = null
        }
        return result
    }

    /** Called by the accessibility service when a request has finished. */
    fun complete(requestId: Long, result: SendResult) {
        synchronized(this) {
            if (_pending.value?.id == requestId) {
                _pending.value = null
                waiter?.complete(result)
            }
        }
    }
}
