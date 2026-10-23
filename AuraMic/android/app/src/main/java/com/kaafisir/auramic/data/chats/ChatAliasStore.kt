package com.kaafisir.auramic.data.chats

import android.content.Context
import com.kaafisir.auramic.automation.ChatAliasBook
import com.kaafisir.auramic.automation.ChatKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** [ChatAliasBook] kept in a private preferences file on the phone (names the user's own pick taught us; never uploaded). */
@Singleton
class ChatAliasStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val book by lazy { ChatAliasBook(prefs.getString(KEY, null)) }

    @Synchronized fun augment(key: ChatKey): ChatKey = book.augment(key)

    /** True when [pickedName] just became trusted for [key]'s chat. */
    @Synchronized fun observe(key: ChatKey, pickedName: String): Boolean {
        val became = book.observe(key, pickedName)
        prefs.edit().putString(KEY, book.serialize()).apply()
        return became
    }

    @Synchronized fun activeCount(): Int = book.activeCount()

    @Synchronized fun clear() {
        book.clear()
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val FILE = "auramic_chat_aliases"
        const val KEY = "book"
    }
}
