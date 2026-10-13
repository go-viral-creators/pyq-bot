package com.kaafisir.auramic.automation

import java.text.Normalizer

/** Everything that knows what WhatsApp's UI looks like - isolated so updates are one-file fixes. */
object WhatsAppUi {
    const val PKG = "com.whatsapp"
    const val PKG_BUSINESS = "com.whatsapp.w4b"
    /** Every package the accessibility service may act in (WhatsApp, WhatsApp Business). */
    val PACKAGES: Set<String> = com.kaafisir.auramic.domain.model.ChatApp.allPackages

    // Resource-id suffixes ("<pkg>:id/<name>"), newest known first. Matching is by suffix so it
    // works for both WhatsApp and WhatsApp Business.
    val CHAT_TITLE_IDS = listOf("conversation_contact_name", "conversation_title", "toolbar_title", "action_bar_title", "thread_title", "row_thread_composer_title")
    val COMPOSER_IDS = listOf("entry", "voice_note_btn", "conversation_entry_container", "row_thread_composer_edittext", "message_composer")
    /** Present only on an open conversation (not on the share picker / preview). */
    val CONVERSATION_IDS = listOf("conversation_contact_name", "entry", "voice_note_btn", "conversation_entry_action_button", "conversation_entry_container")
    /** Buttons next to the message box that must never be mistaken for "Send". */
    val COMPOSER_TOOL_IDS = listOf("voice_note_btn", "conversation_entry_action_button", "voice_note_button", "ptt_button", "camera_btn", "camera_button", "input_attach_button", "attach_button")
    val COMPOSER_TOOL_WORDS = setOf(
        "voice message", "voice note", "record voice message", "camera", "attach", "attachment", "mensaje de voz", "camara", "cámara",
        "câmera", "mensagem de voz", "anexar", "adjuntar", "pesan suara", "kamera", "lampirkan", "वॉइस संदेश", "कैमरा", "अटैच", "رسالة صوتية", "الكاميرا", "إرفاق",
    ).map { normalize(it) }.toSet()
    val PICKER_ROW_NAME_IDS = listOf("contactpicker_row_name", "conversations_row_contact_name", "contact_row_name")
    val SEND_IDS = listOf("send", "send_btn", "send_button", "send_container", "direct_send_button", "row_thread_composer_button_send")
    val SEARCH_BUTTON_IDS = listOf("menuitem_search", "search_button", "search")
    val SEARCH_INPUT_IDS = listOf("search_src_text", "search_input", "search_field")

    /** Accessible labels of the "Send" button / dialog confirm across common WhatsApp languages. */
    val SEND_WORDS = setOf(
        "send", "enviar", "भेजें", "भेजे", "भेज दें", "إرسال", "ارسال", "kirim", "envoyer", "senden", "invia",
        "gönder", "отправить", "verzenden", "wyślij", "gửi", "ส่ง", "送信", "보내기", "发送", "發送",
        "পাঠান", "அனுப்பு", "పంపండి", "പങ്കിടുക", "ਭੇਜੋ", "મોકલો", "पाठवा", "ಕಳುಹಿಸು", "بھیجیں", "ارسال کنید",
    ).map { normalize(it) }.toSet()

    val SEARCH_WORDS = setOf(
        "search", "buscar", "pesquisar", "procurar", "खोजें", "खोज", "सर्च", "بحث", "cari", "rechercher",
        "suchen", "cerca", "ara", "поиск", "zoeken", "szukaj", "tìm kiếm", "ค้นหา", "検索", "검색", "搜索",
    ).map { normalize(it) }.toSet()

    /** Case/diacritic/whitespace/bidi-insensitive form used to compare chat names. */
    fun normalize(s: String?): String {
        if (s == null) return ""
        val stripped = Normalizer.normalize(s, Normalizer.Form.NFKD)
            .filter { it.code != 0x200E && it.code != 0x200F && it.code != 0x202A && it.code != 0x202C && it.code != 0xFEFF && it.code != 0x200B }
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
        return stripped.lowercase().trim().replace(Regex("\\s+"), " ")
    }
}
