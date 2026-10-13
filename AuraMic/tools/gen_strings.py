#!/usr/bin/env python3
"""Generates res/values*/strings.xml for all supported UI languages.

Column order: en, hi, hinglish (hi-Latn), es, pt, ar, id
`python gen_strings.py <res_dir>` rewrites the XML files; edit the table below to change copy.
"""
import os
import sys
from xml.sax.saxutils import escape

LANGS = ["en", "hi", "hi-Latn", "es", "pt", "ar", "id"]
DIRS = {"en": "values", "hi": "values-hi", "hi-Latn": "values-b+hi+Latn", "es": "values-es",
        "pt": "values-pt", "ar": "values-ar", "id": "values-in"}

S = {}
def add(key, en, hi, hl, es, pt, ar, idn):
    S[key] = [en, hi, hl, es, pt, ar, idn]

# ---- general -------------------------------------------------------------------------
add("common_back", "Back", "वापस", "Wapas", "Atrás", "Voltar", "رجوع", "Kembali")
add("common_cancel", "Cancel", "रद्द करें", "Cancel karein", "Cancelar", "Cancelar", "إلغاء", "Batal")
add("common_continue", "Continue", "जारी रखें", "Aage badhein", "Continuar", "Continuar", "متابعة", "Lanjutkan")
add("common_skip", "Skip", "छोड़ें", "Skip karein", "Omitir", "Pular", "تخطي", "Lewati")
add("common_next", "Next", "आगे", "Aage", "Siguiente", "Próximo", "التالي", "Berikutnya")
add("common_done", "Got it", "समझ गया", "Samajh gaya", "Entendido", "Entendi", "فهمت", "Mengerti")
add("common_on", "On", "चालू", "Chalu", "Activado", "Ativado", "مفعّل", "Aktif")
add("common_off", "Off", "बंद", "Band", "Desactivado", "Desativado", "متوقف", "Nonaktif")

# ---- language screen -----------------------------------------------------------------
add("lang_title", "Choose your language", "अपनी भाषा चुनें", "Apni bhasha chunein",
    "Elige tu idioma", "Escolha seu idioma", "اختر لغتك", "Pilih bahasa Anda")
add("lang_subtitle", "You can change this anytime in Settings.",
    "आप इसे कभी भी सेटिंग्स में बदल सकते हैं।", "Aap ise kabhi bhi Settings mein badal sakte hain.",
    "Puedes cambiarlo cuando quieras en Ajustes.", "Você pode mudar isso a qualquer momento nas Configurações.",
    "يمكنك تغيير ذلك في أي وقت من الإعدادات.", "Anda dapat mengubahnya kapan saja di Pengaturan.")

# ---- interactive tour ----------------------------------------------------------------
add("tour_step_of", "Step %1$d of %2$d", "चरण %1$d / %2$d", "Step %1$d / %2$d",
    "Paso %1$d de %2$d", "Passo %1$d de %2$d", "الخطوة %1$d من %2$d", "Langkah %1$d dari %2$d")
add("tour_chat_name", "Aarav", "आरव", "Aarav", "Aarav", "Aarav", "آراف", "Aarav")
add("tour_chat_status", "online", "ऑनलाइन", "online", "en línea", "online", "متصل", "online")
add("tour_msg_1", "Hey! Can you send me the details?", "अरे! क्या तुम मुझे डिटेल्स भेज सकते हो?",
    "Arre! Kya tum mujhe details bhej sakte ho?", "¡Hola! ¿Me puedes mandar los detalles?",
    "Oi! Pode me mandar os detalhes?", "مرحبًا! هل يمكنك إرسال التفاصيل؟", "Hai! Bisa kirim detailnya?")
add("tour_msg_2", "Sure, sending a voice note 🎙️", "ज़रूर, वॉइस नोट भेज रहा हूँ 🎙️",
    "Zaroor, voice note bhej raha hoon 🎙️", "Claro, te mando una nota de voz 🎙️",
    "Claro, vou mandar um áudio 🎙️", "بالتأكيد، سأرسل رسالة صوتية 🎙️", "Tentu, kukirim pesan suara 🎙️")
add("tour_t1_title", "Meet your floating mic", "मिलिए अपने फ्लोटिंग माइक से", "Apne floating mic se milein",
    "Conoce tu micrófono flotante", "Conheça seu microfone flutuante", "تعرّف على الميكروفون العائم", "Kenali mikrofon melayang Anda")
add("tour_t1_body", "This bubble floats on top of WhatsApp. It disappears as soon as you leave WhatsApp.",
    "यह बटन WhatsApp के ऊपर तैरता है। WhatsApp छोड़ते ही यह गायब हो जाता है।",
    "Yeh bubble WhatsApp ke upar float karta hai. WhatsApp chhodte hi yeh gayab ho jata hai.",
    "Esta burbuja flota sobre WhatsApp. Desaparece en cuanto sales de WhatsApp.",
    "Esta bolha flutua sobre o WhatsApp. Ela some assim que você sai do WhatsApp.",
    "تظهر هذه الفقاعة فوق واتساب وتختفي بمجرد خروجك منه.",
    "Gelembung ini melayang di atas WhatsApp dan hilang saat Anda keluar dari WhatsApp.")
add("tour_t2_title", "Press and hold to record", "रिकॉर्ड करने के लिए दबाकर रखें", "Record karne ke liye dabakar rakhein",
    "Mantén pulsado para grabar", "Pressione e segure para gravar", "اضغط مطولًا للتسجيل", "Tekan dan tahan untuk merekam")
add("tour_t2_body", "Your turn! Press and hold the glowing mic for a couple of seconds.",
    "अब आपकी बारी! चमकते माइक को कुछ सेकंड दबाकर रखें।",
    "Ab aapki baari! Chamakte mic ko kuch second dabakar rakhein.",
    "¡Tu turno! Mantén pulsado el micrófono brillante unos segundos.",
    "Sua vez! Pressione e segure o microfone brilhante por alguns segundos.",
    "دورك الآن! اضغط مطولًا على الميكروفون المضيء لبضع ثوانٍ.",
    "Giliran Anda! Tekan dan tahan mikrofon yang bersinar selama beberapa detik.")
add("tour_t2_holding", "Recording… keep holding", "रिकॉर्ड हो रहा है… दबाकर रखें", "Recording ho rahi hai… dabakar rakhein",
    "Grabando… sigue pulsando", "Gravando… continue segurando", "جارٍ التسجيل… استمر في الضغط", "Merekam… terus tahan")
add("tour_t3_title", "Release to send", "छोड़ते ही भेजें", "Chhodte hi bhejein",
    "Suelta para enviar", "Solte para enviar", "ارفع إصبعك للإرسال", "Lepas untuk mengirim")
add("tour_t3_body", "Let go. AuraMic removes background noise on your phone and sends the voice note for you.",
    "उंगली हटाएँ। AuraMic आपके फ़ोन पर ही बैकग्राउंड शोर हटाता है और वॉइस नोट खुद भेज देता है।",
    "Ungli hatayein. AuraMic aapke phone par hi background noise hatata hai aur voice note khud bhej deta hai.",
    "Suelta. AuraMic elimina el ruido de fondo en tu teléfono y envía la nota de voz por ti.",
    "Solte. O AuraMic remove o ruído de fundo no seu celular e envia o áudio por você.",
    "ارفع إصبعك. يزيل AuraMic ضوضاء الخلفية على هاتفك ويرسل الرسالة الصوتية نيابةً عنك.",
    "Lepaskan. AuraMic menghapus derau latar di ponsel Anda dan mengirim pesan suara untuk Anda.")
add("tour_demo_hold_prompt", "Hold the mic…", "माइक को दबाए रखें…", "Mic ko dabaye rakhein…",
    "Mantén el micrófono…", "Segure o microfone…", "اضغط مطولًا على الميكروفون…", "Tahan mikrofon…")
add("tour_demo_release_prompt", "Now let go!", "अब छोड़ दें!", "Ab chhod dein!",
    "¡Ahora suelta!", "Agora solte!", "الآن ارفع إصبعك!", "Sekarang lepaskan!")
add("tour_demo_cleaning", "Removing noise…", "शोर हटाया जा रहा है…", "Noise hataya ja raha hai…",
    "Quitando ruido…", "Removendo ruído…", "جارٍ إزالة الضوضاء…", "Menghapus derau…")
add("tour_demo_sent", "Sent — studio quality!", "भेज दिया — स्टूडियो क्वालिटी!", "Bhej diya — studio quality!",
    "¡Enviado, calidad de estudio!", "Enviado — qualidade de estúdio!", "تم الإرسال — جودة الاستوديو!", "Terkirim — kualitas studio!")
add("tour_t4_title", "Slide left to cancel", "रद्द करने के लिए बाएँ स्लाइड करें", "Cancel karne ke liye left slide karein",
    "Desliza a la izquierda para cancelar", "Deslize para a esquerda para cancelar", "اسحب لليسار للإلغاء", "Geser ke kiri untuk batal")
add("tour_t4_body", "Changed your mind? While holding, slide the button to the left and let go. Nothing is sent.",
    "इरादा बदल गया? दबाए रखते हुए बटन को बाएँ सरकाएँ और छोड़ दें। कुछ नहीं भेजा जाएगा।",
    "Mann badal gaya? Dabaye rakhte hue button ko left sarkayein aur chhod dein. Kuch nahi bheja jayega.",
    "¿Cambiaste de idea? Mientras mantienes pulsado, desliza el botón a la izquierda y suelta. No se envía nada.",
    "Mudou de ideia? Enquanto segura, deslize o botão para a esquerda e solte. Nada é enviado.",
    "غيّرت رأيك؟ أثناء الضغط، اسحب الزر لليسار ثم ارفع إصبعك. لن يُرسل شيء.",
    "Berubah pikiran? Saat menahan, geser tombol ke kiri lalu lepaskan. Tidak ada yang dikirim.")
add("tour_t5_title", "Drag it anywhere", "इसे कहीं भी खींचें", "Ise kahin bhi kheenchein",
    "Arrástralo a donde quieras", "Arraste para qualquer lugar", "اسحبه إلى أي مكان", "Seret ke mana saja")
add("tour_t5_body", "Quickly drag the bubble to move it. It snaps to the nearest screen edge and remembers its spot.",
    "बटन को तेज़ी से खींचकर कहीं भी रखें। यह पास के किनारे से चिपक जाता है और जगह याद रखता है।",
    "Button ko tezi se kheenchkar kahin bhi rakhein. Yeh paas ke kinare se chipak jata hai aur jagah yaad rakhta hai.",
    "Arrastra la burbuja rápidamente para moverla. Se pega al borde más cercano y recuerda su lugar.",
    "Arraste a bolha rapidamente para movê-la. Ela gruda na borda mais próxima e lembra a posição.",
    "اسحب الفقاعة بسرعة لتحريكها. تلتصق بأقرب حافة وتتذكر مكانها.",
    "Seret gelembung dengan cepat untuk memindahkannya. Ia menempel ke tepi layar terdekat dan mengingat posisinya.")
add("tour_t6_title", "Now let's grant permissions", "अब अनुमतियाँ दें", "Ab permissions dein",
    "Ahora concedamos los permisos", "Agora vamos conceder as permissões", "لنمنح الأذونات الآن", "Sekarang berikan izin")
add("tour_t6_body", "Four quick permissions make this work. Everything stays on your phone.",
    "इसे चलाने के लिए चार छोटी अनुमतियाँ चाहिए। सब कुछ आपके फ़ोन में ही रहता है।",
    "Isse chalane ke liye chaar chhoti permissions chahiye. Sab kuch aapke phone mein hi rehta hai.",
    "Cuatro permisos rápidos lo hacen funcionar. Todo se queda en tu teléfono.",
    "Quatro permissões rápidas fazem tudo funcionar. Tudo fica no seu celular.",
    "أربعة أذونات سريعة تجعله يعمل. كل شيء يبقى على هاتفك.",
    "Empat izin singkat membuatnya bekerja. Semua tetap di ponsel Anda.")
add("tour_p1_title", "Tap Allow on each card", "हर कार्ड पर अनुमति दें दबाएँ", "Har card par Allow dabayein",
    "Pulsa Permitir en cada tarjeta", "Toque em Permitir em cada cartão", "اضغط «سماح» في كل بطاقة", "Ketuk Izinkan di setiap kartu")
add("tour_p1_body", "Android opens the right settings page; switch AuraMic on and come back. We'll tick it off here.",
    "Android सही सेटिंग्स पेज खोलेगा; AuraMic को चालू करके वापस आएँ। हम यहाँ टिक लगा देंगे।",
    "Android sahi settings page kholega; AuraMic ko on karke wapas aayein. Hum yahan tick laga denge.",
    "Android abrirá la pantalla correcta; activa AuraMic y vuelve. Aquí lo marcaremos.",
    "O Android abre a tela certa; ative o AuraMic e volte. Marcaremos aqui.",
    "سيفتح أندرويد صفحة الإعدادات الصحيحة؛ فعّل AuraMic وعد إلى هنا. سنضع علامة هنا.",
    "Android membuka halaman pengaturan yang tepat; aktifkan AuraMic lalu kembali. Kami akan mencentangnya di sini.")
add("tour_p2_title", "Accessibility sends it for you", "एक्सेसिबिलिटी खुद भेज देती है", "Accessibility khud bhej deti hai",
    "Accesibilidad lo envía por ti", "A Acessibilidade envia por você", "إمكانية الوصول ترسلها نيابةً عنك", "Aksesibilitas mengirimkannya untuk Anda")
add("tour_p2_body",
    'This one taps Send in your chat app after cleaning. It only works inside the chat apps you enable.',
    'यह सफाई के बाद आपके चैट ऐप में Send दबाती है। यह सिर्फ़ आपके चुने चैट ऐप्स में काम करती है।',
    'Yeh safai ke baad aapke chat app mein Send dabati hai. Yeh sirf aapke chune hue chat apps mein kaam karti hai.',
    'Esta pulsa Enviar en tu app de chat tras la limpieza. Solo funciona en las apps de chat que actives.',
    'Esta toca em Enviar no app de chat depois da limpeza. Só funciona nos apps de chat que você ativar.',
    'تضغط هذه على «إرسال» في تطبيق المحادثة بعد التنظيف، وتعمل فقط داخل تطبيقات المحادثة التي تفعّلها.',
    'Ini menekan Kirim di aplikasi obrolan setelah pembersihan. Hanya bekerja di aplikasi obrolan yang Anda aktifkan.')

# ---- permissions ---------------------------------------------------------------------
add("perm_title", "Let's set up AuraMic", "चलिए AuraMic सेट करें", "Chaliye AuraMic set karein",
    "Configuremos AuraMic", "Vamos configurar o AuraMic", "لنقم بإعداد AuraMic", "Mari siapkan AuraMic")
add("perm_subtitle", "A few permissions make the magic work. Your audio never leaves your phone.",
    "कुछ अनुमतियाँ इसे काम करने देती हैं। आपकी ऑडियो कभी फ़ोन से बाहर नहीं जाती।",
    "Kuch permissions isse kaam karne deti hain. Aapki audio kabhi phone se bahar nahi jati.",
    "Unos permisos hacen la magia. Tu audio nunca sale de tu teléfono.",
    "Algumas permissões fazem a mágica acontecer. Seu áudio nunca sai do seu celular.",
    "بضعة أذونات تجعل السحر يعمل. صوتك لا يغادر هاتفك أبدًا.",
    "Beberapa izin membuat keajaiban terjadi. Audio Anda tidak pernah meninggalkan ponsel.")
add("perm_overlay_title", "Floating button", "फ्लोटिंग बटन", "Floating button",
    "Botón flotante", "Botão flutuante", "الزر العائم", "Tombol melayang")
add("perm_overlay_body", "Shows the mic bubble on top of WhatsApp.", "WhatsApp के ऊपर माइक बबल दिखाता है।",
    "WhatsApp ke upar mic bubble dikhata hai.", "Muestra la burbuja del micrófono sobre WhatsApp.",
    "Mostra a bolha do microfone sobre o WhatsApp.", "يعرض فقاعة الميكروفون فوق واتساب.",
    "Menampilkan gelembung mikrofon di atas WhatsApp.")
add("perm_mic_title", "Microphone", "माइक्रोफ़ोन", "Microphone", "Micrófono", "Microfone", "الميكروفون", "Mikrofon")
add("perm_mic_body", "Records your voice while you hold the button.", "बटन दबाने तक आपकी आवाज़ रिकॉर्ड करता है।",
    "Button dabane tak aapki awaaz record karta hai.", "Graba tu voz mientras mantienes pulsado el botón.",
    "Grava sua voz enquanto você segura o botão.", "يسجّل صوتك أثناء الضغط على الزر.",
    "Merekam suara Anda selama Anda menahan tombol.")
add("perm_usage_title", "Usage access", "यूसेज एक्सेस", "Usage access", "Acceso de uso", "Acesso de uso", "الوصول إلى الاستخدام", "Akses penggunaan")
add("perm_usage_body", "Detects when WhatsApp is open so the bubble only shows there.",
    "पता लगाता है कि WhatsApp खुला है ताकि बबल सिर्फ़ वहीं दिखे।",
    "Pata lagata hai ki WhatsApp khula hai taaki bubble sirf wahin dikhe.",
    "Detecta cuándo WhatsApp está abierto para que la burbuja solo aparezca ahí.",
    "Detecta quando o WhatsApp está aberto para a bolha aparecer só lá.",
    "يكتشف متى يكون واتساب مفتوحًا لتظهر الفقاعة هناك فقط.",
    "Mendeteksi saat WhatsApp terbuka agar gelembung hanya muncul di sana.")
add("perm_access_title", "Accessibility", "एक्सेसिबिलिटी", "Accessibility", "Accesibilidad", "Acessibilidade", "إمكانية الوصول", "Aksesibilitas")
add("perm_access_body",
    'Taps Send in your chat app for you once the voice note is clean.',
    'वॉइस नोट साफ़ होने पर आपके चैट ऐप में आपके लिए Send दबाती है।',
    'Voice note saaf hone par aapke chat app mein aapke liye Send dabati hai.',
    'Pulsa Enviar por ti en tu app de chat cuando la nota de voz está limpia.',
    'Toca em Enviar por você no app de chat quando o áudio estiver limpo.',
    'تضغط نيابةً عنك على «إرسال» في تطبيق المحادثة عندما تصبح الرسالة الصوتية نظيفة.',
    'Menekan Kirim di aplikasi obrolan untuk Anda setelah pesan suara bersih.')
add("perm_notif_title", "Notifications", "सूचनाएँ", "Notifications", "Notificaciones", "Notificações", "الإشعارات", "Notifikasi")
add("perm_notif_body", "Needed to keep AuraMic running reliably in the background.",
    "AuraMic को बैकग्राउंड में भरोसेमंद ढंग से चलाने के लिए ज़रूरी।",
    "AuraMic ko background mein bharosemand dhang se chalane ke liye zaroori.",
    "Necesario para que AuraMic funcione de forma fiable en segundo plano.",
    "Necessário para o AuraMic funcionar com confiabilidade em segundo plano.",
    "مطلوب لإبقاء AuraMic يعمل بثبات في الخلفية.",
    "Diperlukan agar AuraMic berjalan stabil di latar belakang.")
add("perm_battery_title", "Keep running", "चलता रहे", "Chalta rahe", "Mantener activo", "Manter ativo", "الاستمرار بالعمل", "Tetap berjalan")
add("perm_battery_body", "Stops Android from putting AuraMic to sleep. Recommended on Xiaomi, Oppo, Vivo, Samsung.",
    "Android को AuraMic सुलाने से रोकता है। Xiaomi, Oppo, Vivo, Samsung पर सुझाया गया।",
    "Android ko AuraMic sulane se rokta hai. Xiaomi, Oppo, Vivo, Samsung par recommended.",
    "Evita que Android duerma AuraMic. Recomendado en Xiaomi, Oppo, Vivo y Samsung.",
    "Impede que o Android suspenda o AuraMic. Recomendado em Xiaomi, Oppo, Vivo e Samsung.",
    "يمنع أندرويد من إيقاف AuraMic. موصى به على شاومي وأوبو وفيفو وسامسونج.",
    "Mencegah Android menidurkan AuraMic. Disarankan di Xiaomi, Oppo, Vivo, Samsung.")
add("perm_allow", "Allow", "अनुमति दें", "Allow karein", "Permitir", "Permitir", "سماح", "Izinkan")
add("perm_granted", "Allowed", "अनुमति मिली", "Allowed", "Permitido", "Permitido", "تم السماح", "Diizinkan")
add("perm_required", "Required", "ज़रूरी", "Zaroori", "Obligatorio", "Obrigatório", "مطلوب", "Wajib")
add("perm_optional", "Recommended", "सुझाया गया", "Recommended", "Recomendado", "Recomendado", "موصى به", "Disarankan")
add("perm_progress", "%1$d of %2$d ready", "%1$d / %2$d तैयार", "%1$d / %2$d ready",
    "%1$d de %2$d listos", "%1$d de %2$d prontos", "%1$d من %2$d جاهز", "%1$d dari %2$d siap")
add("perm_later", "I'll finish later", "बाद में करूँगा", "Baad mein karunga", "Lo haré después", "Faço depois", "سأكمل لاحقًا", "Saya selesaikan nanti")
add("perm_restricted_title", "Can't switch Accessibility on?", "एक्सेसिबिलिटी चालू नहीं हो रही?", "Accessibility on nahi ho rahi?",
    "¿No puedes activar Accesibilidad?", "Não consegue ativar a Acessibilidade?", "لا يمكنك تفعيل إمكانية الوصول؟", "Tidak bisa mengaktifkan Aksesibilitas?")
add("perm_restricted_body", "Android blocks it for apps installed outside the Play Store. Open App info, tap the ⋮ menu, choose “Allow restricted settings”, then try again.",
    "Play Store के बाहर से इंस्टॉल किए ऐप्स के लिए Android इसे रोकता है। ऐप जानकारी खोलें, ⋮ मेनू दबाएँ, “प्रतिबंधित सेटिंग्स की अनुमति दें” चुनें, फिर दोबारा कोशिश करें।",
    "Play Store ke bahar se install kiye apps ke liye Android ise rokta hai. App info kholein, ⋮ menu dabayein, “Allow restricted settings” chunein, phir dobara try karein.",
    "Android lo bloquea en apps instaladas fuera de Play Store. Abre Info de la app, toca el menú ⋮, elige «Permitir ajustes restringidos» y vuelve a intentarlo.",
    "O Android bloqueia isso em apps instalados fora da Play Store. Abra Informações do app, toque no menu ⋮, escolha “Permitir configurações restritas” e tente de novo.",
    "يحظره أندرويد للتطبيقات المثبّتة من خارج متجر Play. افتح معلومات التطبيق، اضغط القائمة ⋮، اختر «السماح بالإعدادات المقيّدة» ثم حاول مجددًا.",
    "Android memblokirnya untuk aplikasi yang dipasang di luar Play Store. Buka Info aplikasi, ketuk menu ⋮, pilih “Izinkan setelan terbatas”, lalu coba lagi.")
add("perm_open_app_info", "Open App info", "ऐप जानकारी खोलें", "App info kholein", "Abrir info de la app", "Abrir info do app", "فتح معلومات التطبيق", "Buka info aplikasi")
add("disclosure_title", "Accessibility disclosure", "एक्सेसिबिलिटी प्रकटीकरण", "Accessibility disclosure",
    "Aviso de accesibilidad", "Aviso de acessibilidade", "إفصاح إمكانية الوصول", "Pengungkapan Aksesibilitas")
add("disclosure_body",
    "AuraMic uses Android's Accessibility Service for one purpose only: after you record a voice note, it selects the chat you were in and taps the Send button inside your chat app (WhatsApp or WhatsApp Business) so the note is delivered without extra taps.\n\nIt works only in the chat apps you enable in Settings. It does not read, collect, store or share your messages, contacts or any personal data. Audio is processed on your device.\n\nTo put the mic on WhatsApp's own voice button and to hide it while you type, it only checks where that button is and whether the message box is empty. It never reads what you type.",
    'AuraMic एक्सेसिबिलिटी सेवा का उपयोग सिर्फ़ एक काम के लिए करता है: वॉइस नोट रिकॉर्ड करने के बाद आप जिस चैट में थे उसे चुनना और आपके चैट ऐप (WhatsApp या WhatsApp Business) के अंदर Send बटन दबाना, ताकि बिना अतिरिक्त टैप के नोट भेजा जाए।\n\nयह सिर्फ़ उन चैट ऐप्स में काम करती है जिन्हें आप सेटिंग्स में चालू करते हैं। यह आपके संदेश, संपर्क या कोई निजी डेटा न पढ़ती है, न जमा करती है, न साझा करती है। ऑडियो आपके डिवाइस पर ही प्रोसेस होती है।\n\nअपना माइक WhatsApp के वॉइस बटन पर रखने और टाइप करते समय उसे हटाने के लिए यह सिर्फ़ यह देखती है कि वह बटन कहाँ है और मैसेज बॉक्स खाली है या नहीं। आप जो टाइप करते हैं उसे यह कभी नहीं पढ़ती।',
    'AuraMic Accessibility Service ka use sirf ek kaam ke liye karta hai: voice note record karne ke baad aap jis chat mein the use chunna aur aapke chat app (WhatsApp ya WhatsApp Business) ke andar Send button dabana, taaki note bina extra tap ke bhej diya jaye.\n\nYeh sirf un chat apps mein kaam karti hai jinhe aap Settings mein on karte hain. Yeh aapke messages, contacts ya koi personal data na padhti hai, na store karti hai, na share karti hai. Audio aapke device par hi process hoti hai.\n\nMic ko WhatsApp ke voice button par rakhne aur typing ke time hatane ke liye yeh sirf dekhti hai ki wo button kahan hai aur message box khali hai ya nahi. Aap jo type karte ho use kabhi nahi padhti.',
    'AuraMic usa el Servicio de accesibilidad de Android con un único propósito: después de grabar una nota de voz, selecciona el chat en el que estabas y pulsa el botón Enviar dentro de tu app de chat (WhatsApp o WhatsApp Business) para entregarla sin toques adicionales.\n\nSolo funciona en las apps de chat que actives en Ajustes. No lee, recopila, almacena ni comparte tus mensajes, contactos ni datos personales. El audio se procesa en tu dispositivo.\n\nPara colocar el micrófono sobre el botón de voz de WhatsApp y ocultarlo mientras escribes, solo comprueba dónde está ese botón y si el cuadro de mensaje está vacío. Nunca lee lo que escribes.',
    'O AuraMic usa o Serviço de Acessibilidade do Android com um único propósito: depois que você grava um áudio, ele seleciona a conversa em que você estava e toca no botão Enviar dentro do seu app de chat (WhatsApp ou WhatsApp Business) para entregá-lo sem toques extras.\n\nFunciona apenas nos apps de chat que você ativar nas Configurações. Não lê, coleta, armazena nem compartilha suas mensagens, contatos ou dados pessoais. O áudio é processado no seu dispositivo.\n\nPara posicionar o microfone sobre o botão de voz do WhatsApp e escondê-lo enquanto você digita, ele só verifica onde está esse botão e se a caixa de mensagem está vazia. Nunca lê o que você digita.',
    'يستخدم AuraMic خدمة إمكانية الوصول في أندرويد لغرض واحد فقط: بعد تسجيل رسالة صوتية، يختار المحادثة التي كنت فيها ويضغط على زر الإرسال داخل تطبيق المحادثة (واتساب أو واتساب للأعمال) لتسليمها دون نقرات إضافية.\n\nيعمل فقط داخل تطبيقات المحادثة التي تفعّلها من الإعدادات. لا يقرأ رسائلك أو جهات اتصالك أو أي بيانات شخصية ولا يجمعها أو يخزّنها أو يشاركها. تتم معالجة الصوت على جهازك.\n\nلوضع الميكروفون فوق زر الصوت في واتساب وإخفائه أثناء الكتابة، يتحقق فقط من مكان هذا الزر ومما إذا كان مربع الرسالة فارغًا. لا يقرأ أبدًا ما تكتبه.',
    'AuraMic menggunakan Layanan Aksesibilitas Android hanya untuk satu tujuan: setelah Anda merekam pesan suara, layanan ini memilih obrolan tempat Anda berada dan menekan tombol Kirim di dalam aplikasi obrolan Anda (WhatsApp atau WhatsApp Business) agar pesan terkirim tanpa ketukan tambahan.\n\nHanya bekerja di aplikasi obrolan yang Anda aktifkan di Pengaturan. Tidak membaca, mengumpulkan, menyimpan, atau membagikan pesan, kontak, maupun data pribadi Anda. Audio diproses di perangkat Anda.\n\nUntuk menempatkan mikrofon di atas tombol suara WhatsApp dan menyembunyikannya saat Anda mengetik, layanan ini hanya memeriksa letak tombol tersebut dan apakah kotak pesan kosong. Tidak pernah membaca apa yang Anda ketik.')
add("disclosure_agree", "I agree", "मैं सहमत हूँ", "Main agree karta hoon", "Acepto", "Concordo", "أوافق", "Saya setuju")
add("disclosure_decline", "Not now", "अभी नहीं", "Abhi nahi", "Ahora no", "Agora não", "ليس الآن", "Nanti saja")

# ---- home ----------------------------------------------------------------------------
add("home_status_on", "AuraMic is active", "AuraMic सक्रिय है", "AuraMic active hai", "AuraMic está activo", "AuraMic está ativo", "AuraMic نشط", "AuraMic aktif")
add("home_status_off", "AuraMic is off", "AuraMic बंद है", "AuraMic band hai", "AuraMic está apagado", "AuraMic está desligado", "AuraMic متوقف", "AuraMic nonaktif")
add("home_status_setup", "Setup needed", "सेटअप बाकी है", "Setup baaki hai", "Falta configurar", "Configuração necessária", "الإعداد مطلوب", "Perlu pengaturan")
add("home_status_on_body", "Open WhatsApp — your floating mic is waiting.", "WhatsApp खोलें — आपका फ्लोटिंग माइक तैयार है।",
    "WhatsApp kholein — aapka floating mic tayyar hai.", "Abre WhatsApp: tu micrófono flotante te espera.",
    "Abra o WhatsApp: seu microfone flutuante está esperando.", "افتح واتساب — الميكروفون العائم بانتظارك.", "Buka WhatsApp — mikrofon melayang Anda sudah siap.")
add("home_status_off_body", "Turn on to show the floating mic in WhatsApp.", "WhatsApp में फ्लोटिंग माइक दिखाने के लिए चालू करें।",
    "WhatsApp mein floating mic dikhane ke liye on karein.", "Actívalo para mostrar el micrófono flotante en WhatsApp.",
    "Ative para mostrar o microfone flutuante no WhatsApp.", "فعّله لإظهار الميكروفون العائم في واتساب.", "Aktifkan untuk menampilkan mikrofon melayang di WhatsApp.")
add("home_status_setup_body", "Finish the permissions to get started.", "शुरू करने के लिए अनुमतियाँ पूरी करें।",
    "Shuru karne ke liye permissions poori karein.", "Termina los permisos para empezar.", "Conclua as permissões para começar.",
    "أكمل الأذونات للبدء.", "Selesaikan izin untuk memulai.")
add("home_turn_on", "Turn on", "चालू करें", "On karein", "Activar", "Ativar", "تشغيل", "Aktifkan")
add("home_turn_off", "Turn off", "बंद करें", "Off karein", "Desactivar", "Desativar", "إيقاف", "Nonaktifkan")
add("home_fix_permissions", "Finish setup", "सेटअप पूरा करें", "Setup poora karein", "Terminar configuración", "Concluir configuração", "إكمال الإعداد", "Selesaikan pengaturan")
add("home_open_whatsapp",
    'Open chat app',
    'चैट ऐप खोलें',
    'Chat app kholein',
    'Abrir app de chat',
    'Abrir app de chat',
    'فتح تطبيق المحادثة',
    'Buka aplikasi obrolan')
add("home_whatsapp_missing",
    'No supported chat app is installed on this phone.',
    'इस फ़ोन में कोई समर्थित चैट ऐप इंस्टॉल नहीं है।',
    'Is phone mein koi supported chat app install nahi hai.',
    'No hay ninguna app de chat compatible instalada en este teléfono.',
    'Nenhum app de chat compatível está instalado neste celular.',
    'لا يوجد تطبيق محادثة مدعوم مثبّت على هذا الهاتف.',
    'Tidak ada aplikasi obrolan yang didukung di ponsel ini.')
add("home_stat_notes", "Voice notes cleaned", "साफ़ किए वॉइस नोट", "Voice notes saaf kiye", "Notas de voz limpiadas", "Áudios limpos", "رسائل صوتية نُظّفت", "Pesan suara dibersihkan")
add("home_stat_minutes", "Minutes polished", "सँवारे गए मिनट", "Minutes sanwaare", "Minutos pulidos", "Minutos aprimorados", "دقائق تم تحسينها", "Menit dipoles")
add("home_lab_title", "Studio Lab", "स्टूडियो लैब", "Studio Lab", "Laboratorio de estudio", "Laboratório de estúdio", "مختبر الاستوديو", "Lab Studio")
add("home_lab_body", "Record a noisy sample and hear the before / after.", "शोर वाला सैंपल रिकॉर्ड करें और पहले / बाद सुनें।",
    "Shor wala sample record karein aur pehle / baad sunein.", "Graba una muestra con ruido y escucha el antes y el después.",
    "Grave uma amostra com ruído e ouça o antes e o depois.", "سجّل عيّنة بها ضوضاء واستمع إلى قبل وبعد.", "Rekam sampel berisik dan dengar sebelum / sesudahnya.")
add("home_guide_title", "Replay the tour", "टूर दोबारा देखें", "Tour dobara dekhein", "Repetir el tutorial", "Rever o tutorial", "إعادة الجولة", "Ulangi panduan")
add("home_how_title", "How it works", "यह कैसे काम करता है", "Yeh kaise kaam karta hai", "Cómo funciona", "Como funciona", "كيف يعمل", "Cara kerja")
add("home_how_1", "Open a WhatsApp chat", "WhatsApp चैट खोलें", "WhatsApp chat kholein", "Abre un chat de WhatsApp", "Abra uma conversa do WhatsApp", "افتح محادثة واتساب", "Buka obrolan WhatsApp")
add("home_how_2", "Hold the floating mic and speak", "फ्लोटिंग माइक दबाकर बोलें", "Floating mic dabakar bolein", "Mantén el micrófono flotante y habla", "Segure o microfone flutuante e fale", "اضغط مطولًا على الميكروفون العائم وتحدّث", "Tahan mikrofon melayang dan bicara")
add("home_how_3", "Release — noise gone, note sent", "छोड़ें — शोर गायब, नोट भेजा", "Chhodein — noise gayab, note bheja", "Suelta: sin ruido y nota enviada", "Solte: sem ruído e áudio enviado", "ارفع إصبعك — اختفت الضوضاء وأُرسلت الرسالة", "Lepas — derau hilang, pesan terkirim")

# ---- lab -----------------------------------------------------------------------------
add("lab_hint", "Hold the button and speak somewhere noisy. Then compare.", "बटन दबाएँ और शोर वाली जगह बोलें। फिर तुलना करें।",
    "Button dabayein aur shor wali jagah bolein. Phir compare karein.", "Mantén el botón y habla en un lugar ruidoso. Luego compara.",
    "Segure o botão e fale em um lugar barulhento. Depois compare.", "اضغط مطولًا على الزر وتحدّث في مكان صاخب. ثم قارن.", "Tahan tombol dan bicara di tempat berisik. Lalu bandingkan.")
add("lab_hold", "Hold to record", "रिकॉर्ड के लिए दबाएँ", "Record ke liye dabayein", "Mantén para grabar", "Segure para gravar", "اضغط مطولًا للتسجيل", "Tahan untuk merekam")
add("lab_recording", "Recording… release to finish", "रिकॉर्ड हो रहा है… खत्म करने के लिए छोड़ें", "Recording ho rahi hai… khatam karne ke liye chhodein",
    "Grabando… suelta para terminar", "Gravando… solte para terminar", "جارٍ التسجيل… ارفع إصبعك للإنهاء", "Merekam… lepaskan untuk selesai")
add("lab_processing", "Cleaning your audio…", "आपकी ऑडियो साफ़ हो रही है…", "Aapki audio saaf ho rahi hai…", "Limpiando tu audio…", "Limpando seu áudio…", "جارٍ تنظيف الصوت…", "Membersihkan audio Anda…")
add("lab_original", "Original", "मूल", "Original", "Original", "Original", "الأصلي", "Asli")
add("lab_clean", "AuraMic clean", "AuraMic क्लीन", "AuraMic clean", "AuraMic limpio", "AuraMic limpo", "نقي بـ AuraMic", "Bersih AuraMic")
add("lab_play", "Play", "चलाएँ", "Play", "Reproducir", "Reproduzir", "تشغيل", "Putar")
add("lab_stop", "Stop", "रोकें", "Stop", "Detener", "Parar", "إيقاف", "Berhenti")
add("lab_again", "Record again", "फिर रिकॉर्ड करें", "Dobara record karein", "Grabar de nuevo", "Gravar de novo", "تسجيل مرة أخرى", "Rekam lagi")
add("lab_too_short", "Too short — hold a little longer.", "बहुत छोटा — थोड़ा और देर दबाएँ।", "Bahut chhota — thoda aur der dabayein.",
    "Muy corto: mantén un poco más.", "Muito curto: segure um pouco mais.", "قصير جدًا — اضغط لفترة أطول قليلًا.", "Terlalu singkat — tahan sedikit lebih lama.")
add("lab_mic_needed", "Microphone permission needed.", "माइक्रोफ़ोन की अनुमति चाहिए।", "Microphone ki permission chahiye.",
    "Se necesita permiso de micrófono.", "É necessária a permissão do microfone.", "مطلوب إذن الميكروفون.", "Perlu izin mikrofon.")

# ---- settings ------------------------------------------------------------------------
add("set_title", "Settings", "सेटिंग्स", "Settings", "Ajustes", "Configurações", "الإعدادات", "Pengaturan")
add("set_audio", "Audio", "ऑडियो", "Audio", "Audio", "Áudio", "الصوت", "Audio")
add("set_strength", "Noise removal strength", "शोर हटाने की तीव्रता", "Noise hatane ki strength", "Intensidad de reducción de ruido", "Intensidade da redução de ruído", "قوة إزالة الضوضاء", "Kekuatan penghilang derau")
add("set_strength_low", "Gentle", "हल्का", "Halka", "Suave", "Suave", "خفيفة", "Lembut")
add("set_strength_high", "Maximum", "अधिकतम", "Maximum", "Máxima", "Máxima", "قصوى", "Maksimum")
add("set_normalize", "Loudness boost", "आवाज़ बूस्ट", "Awaaz boost", "Aumento de volumen", "Aumento de volume", "تعزيز مستوى الصوت", "Penguat volume")
add("set_normalize_desc", "Evens out quiet voices and prevents clipping.", "धीमी आवाज़ें बराबर करता है और क्लिपिंग रोकता है।", "Dheemi awaazein barabar karta hai aur clipping rokta hai.",
    "Iguala voces bajas y evita saturación.", "Equilibra vozes baixas e evita distorção.", "يوازن الأصوات الخافتة ويمنع التشويه.", "Menyeimbangkan suara pelan dan mencegah distorsi.")
add("set_format", "Output format", "आउटपुट फ़ॉर्मेट", "Output format", "Formato de salida", "Formato de saída", "صيغة الإخراج", "Format keluaran")
add("set_format_m4a", "M4A (AAC) — works everywhere", "M4A (AAC) — हर जगह चलता है", "M4A (AAC) — har jagah chalta hai", "M4A (AAC): funciona en todas partes", "M4A (AAC): funciona em qualquer lugar", "M4A (AAC) — يعمل في كل مكان", "M4A (AAC) — bisa di mana saja")
add("set_format_opus", "Opus — smaller (Android 10+)", "Opus — छोटा (Android 10+)", "Opus — chhota (Android 10+)", "Opus: más pequeño (Android 10+)", "Opus: menor (Android 10+)", "Opus — أصغر حجمًا (أندرويد 10+)", "Opus — lebih kecil (Android 10+)")
add("set_whatsapp", "WhatsApp", "WhatsApp", "WhatsApp", "WhatsApp", "WhatsApp", "واتساب", "WhatsApp")
add("set_auto_send", "Auto-send", "ऑटो-सेंड", "Auto-send", "Envío automático", "Envio automático", "الإرسال التلقائي", "Kirim otomatis")
add("set_auto_send_desc", "Tap Send for me after processing.", "प्रोसेस होने के बाद मेरे लिए Send दबाएँ।", "Process hone ke baad mere liye Send dabayein.",
    "Pulsar Enviar por mí tras procesar.", "Tocar em Enviar por mim após processar.", "الضغط على إرسال نيابةً عني بعد المعالجة.", "Tekan Kirim untuk saya setelah diproses.")
add("set_haptics", "Haptic feedback", "हैप्टिक फ़ीडबैक", "Haptic feedback", "Respuesta háptica", "Vibração tátil", "الاهتزاز اللمسي", "Umpan balik haptik")
add("set_overlay", "Floating button", "फ्लोटिंग बटन", "Floating button", "Botón flotante", "Botão flutuante", "الزر العائم", "Tombol melayang")
add("set_size", "Button size", "बटन का आकार", "Button size", "Tamaño del botón", "Tamanho do botão", "حجم الزر", "Ukuran tombol")
add("set_reset_position", "Reset position", "स्थिति रीसेट करें", "Position reset karein", "Restablecer posición", "Redefinir posição", "إعادة ضبط الموضع", "Atur ulang posisi")
add("set_general", "General", "सामान्य", "General", "General", "Geral", "عام", "Umum")
add("set_language", "Language", "भाषा", "Language", "Idioma", "Idioma", "اللغة", "Bahasa")
add("set_theme", "Theme", "थीम", "Theme", "Tema", "Tema", "المظهر", "Tema")
add("set_theme_system", "System", "सिस्टम", "System", "Sistema", "Sistema", "النظام", "Sistem")
add("set_theme_light", "Light", "लाइट", "Light", "Claro", "Claro", "فاتح", "Terang")
add("set_theme_dark", "Dark", "डार्क", "Dark", "Oscuro", "Escuro", "داكن", "Gelap")
add("set_battery", "Battery optimisation", "बैटरी ऑप्टिमाइज़ेशन", "Battery optimisation", "Optimización de batería", "Otimização de bateria", "تحسين البطارية", "Optimalisasi baterai")
add("set_about", "About", "जानकारी", "About", "Acerca de", "Sobre", "حول", "Tentang")
add("set_privacy", "Privacy Policy", "गोपनीयता नीति", "Privacy Policy", "Política de privacidad", "Política de privacidade", "سياسة الخصوصية", "Kebijakan Privasi")
add("set_terms", "Terms of Service", "सेवा की शर्तें", "Terms of Service", "Términos del servicio", "Termos de serviço", "شروط الخدمة", "Ketentuan Layanan")
add("set_a11y_notice", "Accessibility disclosure", "एक्सेसिबिलिटी प्रकटीकरण", "Accessibility disclosure", "Aviso de accesibilidad", "Aviso de acessibilidade", "إفصاح إمكانية الوصول", "Pengungkapan Aksesibilitas")
add("set_support", "Contact support", "सहायता से संपर्क करें", "Support se contact karein", "Contactar con soporte", "Falar com o suporte", "التواصل مع الدعم", "Hubungi dukungan")
add("set_version", "Version %1$s", "संस्करण %1$s", "Version %1$s", "Versión %1$s", "Versão %1$s", "الإصدار %1$s", "Versi %1$s")
add("set_email_chooser", "Send email with…", "ईमेल भेजें…", "Email bhejein…", "Enviar correo con…", "Enviar e-mail com…", "إرسال بريد عبر…", "Kirim email dengan…")

# ---- overlay / service ----------------------------------------------------------------
add("ov_hint", "Hold to record · triple-tap to move", "रिकॉर्ड के लिए दबाकर रखें · खिसकाने के लिए 3 बार टैप", "Record ke liye daba ke rakhein · move karne ke liye 3 baar tap", "Mantén para grabar · toca 3 veces para moverlo", "Segure para gravar · toque 3 vezes para mover", "اضغط مطولًا للتسجيل · اضغط 3 مرات للتحريك", "Tahan untuk merekam · ketuk 3 kali untuk memindah")
add("ov_slide_cancel", "◀ Slide to cancel", "◀ रद्द करने के लिए स्लाइड करें", "◀ Cancel ke liye slide karein", "◀ Desliza para cancelar", "◀ Deslize para cancelar", "◀ اسحب للإلغاء", "◀ Geser untuk batal")
add("ov_release_cancel", "Release to cancel", "रद्द करने के लिए छोड़ें", "Cancel ke liye chhodein", "Suelta para cancelar", "Solte para cancelar", "ارفع إصبعك للإلغاء", "Lepas untuk batal")
add("ov_processing", "Cleaning audio…", "ऑडियो साफ़ हो रही है…", "Audio saaf ho rahi hai…", "Limpiando audio…", "Limpando áudio…", "جارٍ تنظيف الصوت…", "Membersihkan audio…")
add("ov_sending", "Sending…", "भेज रहा है…", "Bhej raha hai…", "Enviando…", "Enviando…", "جارٍ الإرسال…", "Mengirim…")
add("ov_sent", "Sent!", "भेज दिया!", "Bhej diya!", "¡Enviado!", "Enviado!", "تم الإرسال!", "Terkirim!")
add("ov_failed", "Couldn't send — tap Send manually", "भेज नहीं सका — Send खुद दबाएँ", "Bhej nahi saka — Send khud dabayein", "No se pudo enviar: pulsa Enviar", "Não foi possível enviar: toque em Enviar", "تعذّر الإرسال — اضغط إرسال يدويًا", "Gagal mengirim — tekan Kirim manual")
add("ov_too_short", "Too short", "बहुत छोटा", "Bahut chhota", "Muy corto", "Muito curto", "قصير جدًا", "Terlalu singkat")
add("ov_free_on", "Move mode: drag the mic anywhere. Triple-tap to fix it again", "मूव मोड: माइक को कहीं भी खींचें। फिर से फ़िक्स करने के लिए तीन बार टैप करें", "Move mode: mic ko kahin bhi khichein. Wapas fix karne ke liye 3 baar tap karein", "Modo mover: arrastra el micrófono. Toca 3 veces para fijarlo", "Modo mover: arraste o microfone. Toque 3 vezes para fixar", "وضع التحريك: اسحب الميكروفون لأي مكان. اضغط 3 مرات لتثبيته", "Mode pindah: seret mic ke mana saja. Ketuk 3 kali untuk menempelkan lagi")
add("ov_free_off", "Mic fixed on WhatsApp's button", "माइक WhatsApp के बटन पर फ़िक्स", "Mic WhatsApp ke button par fix", "Micrófono fijado en el botón de WhatsApp", "Microfone fixado no botão do WhatsApp", "تم تثبيت الميكروفون على زر واتساب", "Mic ditempel di tombol WhatsApp")
add("ov_hint_free", "Hold to record · triple-tap to fix on WhatsApp's mic", "रिकॉर्ड के लिए दबाकर रखें · WhatsApp माइक पर फ़िक्स करने के लिए 3 बार टैप", "Record ke liye daba ke rakhein · WhatsApp mic par fix ke liye 3 baar tap", "Mantén para grabar · toca 3 veces para fijarlo", "Segure para gravar · toque 3 vezes para fixar", "اضغط مطولًا للتسجيل · اضغط 3 مرات للتثبيت", "Tahan untuk merekam · ketuk 3 kali untuk menempel")
add("ov_unconfirmed", "Probably sent — check the chat", "शायद भेज दिया — चैट देख लें", "Shayad bhej diya — chat check kar lein", "Probablemente enviado: revisa el chat", "Provavelmente enviado: confira a conversa", "ربما تم الإرسال — تحقّق من المحادثة", "Mungkin terkirim — cek obrolan")
add("ov_manual", "Pick a chat, then tap Send", "चैट चुनें, फिर Send दबाएँ", "Chat chunein, phir Send dabayein", "Elige un chat y pulsa Enviar", "Escolha uma conversa e toque em Enviar", "اختر محادثة ثم اضغط إرسال", "Pilih obrolan, lalu tekan Kirim")
add("ov_error_mic", "Microphone is busy", "माइक्रोफ़ोन व्यस्त है", "Microphone busy hai", "El micrófono está ocupado", "O microfone está ocupado", "الميكروفون مشغول", "Mikrofon sedang dipakai")
add("cd_mic", "Record voice note", "वॉइस नोट रिकॉर्ड करें", "Voice note record karein", "Grabar nota de voz", "Gravar áudio", "تسجيل رسالة صوتية", "Rekam pesan suara")
add("notif_channel", "AuraMic service", "AuraMic सेवा", "AuraMic service", "Servicio de AuraMic", "Serviço do AuraMic", "خدمة AuraMic", "Layanan AuraMic")
add("notif_title", "AuraMic is ready", "AuraMic तैयार है", "AuraMic tayyar hai", "AuraMic está listo", "AuraMic está pronto", "AuraMic جاهز", "AuraMic siap")
add("notif_text", "The floating mic appears when WhatsApp is open.", "WhatsApp खुलने पर फ्लोटिंग माइक दिखता है।", "WhatsApp khulne par floating mic dikhta hai.",
    "El micrófono flotante aparece cuando WhatsApp está abierto.", "O microfone flutuante aparece quando o WhatsApp está aberto.", "يظهر الميكروفون العائم عند فتح واتساب.", "Mikrofon melayang muncul saat WhatsApp terbuka.")
add("notif_restore_channel", "AuraMic alerts", "AuraMic अलर्ट", "AuraMic alerts", "Avisos de AuraMic", "Alertas do AuraMic", "تنبيهات AuraMic", "Peringatan AuraMic")
add("notif_restore_title", "Floating mic is off", "फ्लोटिंग माइक बंद है", "Floating mic band hai", "El micrófono flotante está apagado", "O microfone flutuante está desligado", "الميكروفون العائم متوقف", "Mic mengambang mati")
add("notif_restore_text", "Android stopped it. Tap to turn it back on.", "Android ने इसे रोक दिया। फिर से चालू करने के लिए टैप करें।", "Android ne ise rok diya. Wapas on karne ke liye tap karein.", "Android lo detuvo. Toca para volver a activarlo.", "O Android o interrompeu. Toque para ativar de novo.", "أوقفه Android. اضغط لإعادة تشغيله.", "Android menghentikannya. Ketuk untuk menyalakan lagi.")
add("notif_restore_perm", "A permission was turned off. Tap to fix it.", "कोई अनुमति बंद हो गई। ठीक करने के लिए टैप करें।", "Koi permission band ho gayi. Theek karne ke liye tap karein.", "Se desactivó un permiso. Toca para arreglarlo.", "Uma permissão foi desativada. Toque para corrigir.", "تم إيقاف أحد الأذونات. اضغط لإصلاحه.", "Sebuah izin dimatikan. Ketuk untuk memperbaikinya.")
add("notif_stop", "Turn off", "बंद करें", "Off karein", "Desactivar", "Desativar", "إيقاف", "Nonaktifkan")
add("a11y_description",
    'AuraMic taps the Send button in your chat app (WhatsApp or WhatsApp Business) after you record a voice note with the floating mic. It works only inside the chat apps you enable and never reads or stores your messages.',
    'फ्लोटिंग माइक से वॉइस नोट रिकॉर्ड करने के बाद AuraMic आपके चैट ऐप (WhatsApp या WhatsApp Business) का Send बटन दबाता है। यह सिर्फ़ आपके चुने चैट ऐप्स में काम करता है और आपके संदेश कभी नहीं पढ़ता या सहेजता।',
    'Floating mic se voice note record karne ke baad AuraMic aapke chat app (WhatsApp ya WhatsApp Business) ka Send button dabata hai. Yeh sirf aapke chune hue chat apps mein kaam karta hai aur aapke messages kabhi nahi padhta ya save karta.',
    'AuraMic pulsa el botón Enviar de tu app de chat (WhatsApp o WhatsApp Business) tras grabar una nota de voz con el micrófono flotante. Solo funciona en las apps de chat que actives y nunca lee ni guarda tus mensajes.',
    'O AuraMic toca no botão Enviar do seu app de chat (WhatsApp ou WhatsApp Business) depois que você grava um áudio com o microfone flutuante. Funciona apenas nos apps de chat que você ativar e nunca lê nem armazena suas mensagens.',
    'يضغط AuraMic على زر الإرسال في تطبيق المحادثة (واتساب أو واتساب للأعمال) بعد تسجيل رسالة صوتية بالميكروفون العائم. يعمل فقط داخل تطبيقات المحادثة التي تفعّلها ولا يقرأ رسائلك ولا يخزّنها.',
    'AuraMic menekan tombol Kirim di aplikasi obrolan Anda (WhatsApp atau WhatsApp Business) setelah Anda merekam pesan suara dengan mikrofon melayang. Hanya bekerja di aplikasi obrolan yang Anda aktifkan dan tidak pernah membaca atau menyimpan pesan Anda.')

# ---- advanced options (v1.1) -------------------------------------------------------------------------------------
add("set_enhance", "Voice enhancement", "आवाज़ सुधार", "Awaaz enhancement", "Mejora de voz", "Melhoria de voz", "تحسين الصوت", "Peningkatan suara")
add("set_engine", "Noise removal engine", "शोर हटाने का इंजन", "Noise removal engine", "Motor de reducción de ruido", "Mecanismo de redução de ruído", "محرك إزالة الضوضاء", "Mesin penghilang derau")
add("set_engine_ai", "DeepFilterNet AI — studio quality", "DeepFilterNet AI — स्टूडियो क्वालिटी", "DeepFilterNet AI — studio quality", "DeepFilterNet IA: calidad de estudio", "DeepFilterNet IA: qualidade de estúdio", "DeepFilterNet بالذكاء الاصطناعي — جودة الاستوديو", "DeepFilterNet AI — kualitas studio")
add("set_engine_classic", "Classic — fastest, lighter", "क्लासिक — सबसे तेज़, हल्का", "Classic — sabse tez, halka", "Clásico: el más rápido y ligero", "Clássico: o mais rápido e leve", "كلاسيكي — الأسرع والأخف", "Klasik — tercepat, ringan")
add("set_fast_mode", "Fast mode", "फ़ास्ट मोड", "Fast mode", "Modo rápido", "Modo rápido", "الوضع السريع", "Mode cepat")
add("set_fast_mode_desc", "Skips the finest AI stage: about a third faster, slightly less detail.", "सबसे बारीक AI चरण छोड़ता है: लगभग एक तिहाई तेज़, थोड़ा कम डिटेल।", "Sabse baareek AI stage chhodta hai: lagbhag ek tihai tez, thoda kam detail.",
    "Omite la etapa de IA más fina: un tercio más rápido, algo menos de detalle.", "Pula a etapa de IA mais fina: cerca de um terço mais rápido, um pouco menos de detalhe.", "يتخطى أدق مرحلة للذكاء الاصطناعي: أسرع بنحو الثلث وبتفاصيل أقل قليلًا.", "Melewati tahap AI paling halus: sekitar sepertiga lebih cepat, detail sedikit berkurang.")
add("set_keep_bg_value", "Keep some ambience: %1$d%%", "थोड़ा माहौल रखें: %1$d%%", "Thoda mahaul rakhein: %1$d%%", "Conservar algo de ambiente: %1$d%%", "Manter um pouco de ambiente: %1$d%%", "إبقاء بعض الأجواء: %1$d%%", "Pertahankan sedikit suasana: %1$d%%")
add("set_keep_bg_desc", "0 = background fully removed (studio). Higher values mix some of it back for a natural feel.", "0 = बैकग्राउंड पूरी तरह हटा (स्टूडियो)। ज़्यादा मान थोड़ा वापस मिलाकर प्राकृतिक एहसास देते हैं।", "0 = background poora hata (studio). Zyada value thoda wapas mila kar natural feel deti hai.",
    "0 = fondo eliminado por completo (estudio). Valores altos devuelven algo para un sonido natural.", "0 = fundo totalmente removido (estúdio). Valores maiores trazem um pouco de volta para soar natural.", "0٪ = إزالة الخلفية بالكامل (استوديو). القيم الأعلى تعيد جزءًا منها لإحساس طبيعي.", "0 = latar dihapus total (studio). Nilai lebih tinggi mencampur sebagian kembali agar terasa alami.")
add("set_preset", "Voice polish", "आवाज़ पॉलिश", "Awaaz polish", "Pulido de voz", "Acabamento de voz", "تلميع الصوت", "Poles suara")
add("set_preset_off", "Off", "बंद", "Off", "Desactivado", "Desligado", "إيقاف", "Mati")
add("set_preset_natural", "Natural", "प्राकृतिक", "Natural", "Natural", "Natural", "طبيعي", "Alami")
add("set_preset_podcast", "Podcast", "पॉडकास्ट", "Podcast", "Podcast", "Podcast", "بودكاست", "Podcast")
add("set_preset_desc", "Podcast adds EQ, a de-esser and gentle compression for a broadcast sound.", "पॉडकास्ट EQ, डी-एसर और हल्का कम्प्रेशन जोड़ता है, ब्रॉडकास्ट जैसी आवाज़ के लिए।", "Podcast EQ, de-esser aur halka compression jodta hai, broadcast jaisi awaaz ke liye.",
    "Podcast añade ecualizador, de-esser y compresión suave para un sonido de radio.", "Podcast adiciona equalização, de-esser e compressão suave para um som de rádio.", "يضيف «بودكاست» موازنة وإزالة للصفير وضغطًا لطيفًا لصوت إذاعي.", "Podcast menambah EQ, de-esser, dan kompresi halus untuk suara siaran.")
add("set_loudness_target", "Loudness target", "आवाज़ का स्तर", "Awaaz ka level", "Nivel de volumen", "Nível de volume", "مستوى الصوت المستهدف", "Target volume")
add("set_loud_chat", "Chat · −19 LUFS", "चैट · −19 LUFS", "Chat · −19 LUFS", "Chat · −19 LUFS", "Chat · −19 LUFS", "محادثة · −19 LUFS", "Obrolan · −19 LUFS")
add("set_loud_podcast", "Podcast · −16 LUFS", "पॉडकास्ट · −16 LUFS", "Podcast · −16 LUFS", "Podcast · −16 LUFS", "Podcast · −16 LUFS", "بودكاست · −16 LUFS", "Podcast · −16 LUFS")
add("set_loud_loud", "Loud · −14 LUFS", "तेज़ · −14 LUFS", "Tez · −14 LUFS", "Alto · −14 LUFS", "Alto · −14 LUFS", "عالٍ · −14 LUFS", "Keras · −14 LUFS")
add("set_cut", "Auto-cut silence", "चुप्पी अपने-आप काटें", "Chuppi apne-aap kaatein", "Cortar silencios automáticamente", "Cortar silêncios automaticamente", "قصّ الصمت تلقائيًا", "Potong hening otomatis")
add("set_cut_off", "Off", "बंद", "Off", "Desactivado", "Desligado", "إيقاف", "Mati")
add("set_cut_gentle", "Gentle", "हल्का", "Halka", "Suave", "Suave", "خفيف", "Lembut")
add("set_cut_tight", "Tight", "सख़्त", "Tight", "Ajustado", "Firme", "مشدّد", "Ketat")
add("set_cut_desc", "Shortens long pauses and trims the start and end of the note.", "लंबे विराम छोटे करता है और नोट की शुरुआत व अंत ट्रिम करता है।", "Lambe pauses chhote karta hai aur note ki shuruaat aur ant trim karta hai.",
    "Acorta las pausas largas y recorta el inicio y el final de la nota.", "Encurta pausas longas e apara o início e o fim do áudio.", "يقصّر الوقفات الطويلة ويقتطع بداية الرسالة ونهايتها.", "Memperpendek jeda panjang dan memangkas awal serta akhir pesan.")
add("set_apps", "Chat apps", "चैट ऐप्स", "Chat apps", "Apps de chat", "Apps de chat", "تطبيقات المحادثة", "Aplikasi obrolan")
add("app_whatsapp", "WhatsApp", "WhatsApp", "WhatsApp", "WhatsApp", "WhatsApp", "واتساب", "WhatsApp")
add("app_business", "WhatsApp Business", "WhatsApp Business", "WhatsApp Business", "WhatsApp Business", "WhatsApp Business", "واتساب للأعمال", "WhatsApp Business")
add("app_telegram", "Telegram", "Telegram", "Telegram", "Telegram", "Telegram", "تيليجرام", "Telegram")
add("app_instagram", "Instagram (experimental)", "Instagram (प्रायोगिक)", "Instagram (experimental)", "Instagram (experimental)", "Instagram (experimental)", "إنستغرام (تجريبي)", "Instagram (eksperimental)")
add("set_instagram_note", "Instagram does not accept audio files from other apps, so notes are saved to Music/AuraMic for you to attach.", "Instagram दूसरे ऐप्स से ऑडियो फ़ाइल नहीं लेता, इसलिए नोट Music/AuraMic में सहेजे जाते हैं ताकि आप जोड़ सकें।", "Instagram doosre apps se audio file nahi leta, isliye notes Music/AuraMic mein save hote hain taaki aap attach kar sakein.",
    "Instagram no acepta archivos de audio de otras apps, así que las notas se guardan en Music/AuraMic para que las adjuntes.", "O Instagram não aceita arquivos de áudio de outros apps, então os áudios são salvos em Music/AuraMic para você anexar.", "لا يقبل إنستغرام ملفات صوتية من تطبيقات أخرى، لذا تُحفظ الرسائل في Music/AuraMic لتقوم بإرفاقها.", "Instagram tidak menerima berkas audio dari aplikasi lain, jadi pesan disimpan di Music/AuraMic untuk Anda lampirkan.")
add("set_fallback_recent", "Use the most recent chat if the open chat can't be read", "खुली चैट न पढ़ी जा सके तो सबसे हाल की चैट चुनें", "Open chat na padhi ja sake to sabse recent chat chunein", "Usar el chat más reciente si no se puede leer el chat abierto", "Usar a conversa mais recente se a aberta não puder ser lida", "استخدام أحدث محادثة إذا تعذّرت قراءة المحادثة المفتوحة", "Gunakan obrolan terbaru jika obrolan terbuka tak terbaca")
add("set_fallback_recent_desc", "Off by default. Turn on only if auto-select fails; it may pick a different chat.", "डिफ़ॉल्ट रूप से बंद। ऑटो-सेलेक्ट फ़ेल हो तभी चालू करें; यह दूसरी चैट चुन सकता है।", "Default mein off. Auto-select fail ho tabhi on karein; yeh doosri chat chun sakta hai.",
    "Desactivado por defecto. Actívalo solo si falla la selección automática; puede elegir otro chat.", "Desativado por padrão. Ative só se a seleção automática falhar; pode escolher outra conversa.", "معطّل افتراضيًا. فعّله فقط إذا فشل الاختيار التلقائي؛ قد يختار محادثة مختلفة.", "Nonaktif secara default. Aktifkan hanya jika pilihan otomatis gagal; bisa memilih obrolan lain.")
add("set_save_copy", "Save a copy to Music/AuraMic", "एक कॉपी Music/AuraMic में सहेजें", "Ek copy Music/AuraMic mein save karein", "Guardar una copia en Music/AuraMic", "Salvar uma cópia em Music/AuraMic", "حفظ نسخة في Music/AuraMic", "Simpan salinan ke Music/AuraMic")
add("set_diag", "Copy diagnostics", "डायग्नोस्टिक्स कॉपी करें", "Diagnostics copy karein", "Copiar diagnóstico", "Copiar diagnóstico", "نسخ التشخيص", "Salin diagnostik")
add("set_diag_desc", "Copies the structure of the last chat screen (names shortened) so auto-send problems can be fixed.", "आख़िरी चैट स्क्रीन की बनावट कॉपी करता है (नाम छोटे किए गए) ताकि ऑटो-सेंड की समस्या ठीक की जा सके।", "Aakhri chat screen ki banaawat copy karta hai (naam chhote kiye gaye) taaki auto-send ki problem theek ho sake.",
    "Copia la estructura de la última pantalla de chat (nombres abreviados) para poder arreglar problemas de envío automático.", "Copia a estrutura da última tela de conversa (nomes abreviados) para corrigir problemas do envio automático.", "ينسخ بنية آخر شاشة محادثة (الأسماء مختصرة) لإصلاح مشكلات الإرسال التلقائي.", "Menyalin struktur layar obrolan terakhir (nama dipersingkat) agar masalah kirim otomatis bisa diperbaiki.")
add("diag_copied", "Copied to clipboard", "क्लिपबोर्ड पर कॉपी हुआ", "Clipboard par copy ho gaya", "Copiado al portapapeles", "Copiado para a área de transferência", "تم النسخ إلى الحافظة", "Disalin ke papan klip")
add("diag_none", "Nothing yet — send a voice note first.", "अभी कुछ नहीं — पहले एक वॉइस नोट भेजें।", "Abhi kuch nahi — pehle ek voice note bhejein.", "Aún no hay datos: envía primero una nota de voz.", "Ainda não há dados: envie um áudio primeiro.", "لا شيء بعد — أرسل رسالة صوتية أولًا.", "Belum ada — kirim pesan suara dulu.")
add("lab_removed", "Removed noise", "हटाया गया शोर", "Hataya gaya noise", "Ruido eliminado", "Ruído removido", "الضوضاء المُزالة", "Derau yang dihapus")
add("lab_info", "Processed in %1$.1f s · %2$d× faster than real time", "%1$.1f सेकंड में प्रोसेस · रियल-टाइम से %2$d× तेज़", "%1$.1f second mein process · real-time se %2$d× tez", "Procesado en %1$.1f s · %2$d× más rápido que el tiempo real", "Processado em %1$.1f s · %2$d× mais rápido que o tempo real", "تمت المعالجة في %1$.1f ث · أسرع %2$d× من الزمن الحقيقي", "Diproses dalam %1$.1f dtk · %2$d× lebih cepat dari waktu nyata")
add("lab_cut", "Silence removed: %1$.1f s", "हटाई गई चुप्पी: %1$.1f सेकंड", "Hataya gaya silence: %1$.1f second", "Silencio eliminado: %1$.1f s", "Silêncio removido: %1$.1f s", "الصمت المحذوف: %1$.1f ث", "Hening dihapus: %1$.1f dtk")
add("lab_engine_ai", "DeepFilterNet AI engine", "DeepFilterNet AI इंजन", "DeepFilterNet AI engine", "Motor DeepFilterNet IA", "Mecanismo DeepFilterNet IA", "محرك DeepFilterNet بالذكاء الاصطناعي", "Mesin DeepFilterNet AI")
add("lab_engine_classic", "Classic engine", "क्लासिक इंजन", "Classic engine", "Motor clásico", "Mecanismo clássico", "المحرك الكلاسيكي", "Mesin klasik")
add("ov_saved", "Saved to Music/AuraMic", "Music/AuraMic में सहेजा गया", "Music/AuraMic mein save hua", "Guardado en Music/AuraMic", "Salvo em Music/AuraMic", "تم الحفظ في Music/AuraMic", "Disimpan di Music/AuraMic")


# ======================= v1.3: simplified, plain-language copy (WhatsApp only, two quality levels) ==============
for _k in """set_enhance set_audio set_strength set_strength_low set_strength_high set_normalize set_normalize_desc set_format
set_format_m4a set_format_opus set_whatsapp set_size set_engine set_engine_ai set_engine_classic set_fast_mode set_fast_mode_desc
set_keep_bg_value set_keep_bg_desc set_preset set_preset_off set_preset_natural set_preset_podcast set_preset_desc set_loudness_target
set_loud_chat set_loud_podcast set_loud_loud set_cut set_cut_off set_cut_gentle set_cut_tight set_cut_desc set_apps app_whatsapp
app_business app_telegram app_instagram set_instagram_note set_fallback_recent set_fallback_recent_desc set_save_copy lab_removed lab_cut
lab_engine_ai lab_engine_classic""".split():
    S.pop(_k, None)

def _swap(key, pairs):
    for i, (old, new) in enumerate(pairs):
        assert old in S[key][i], (key, i)
        S[key][i] = S[key][i].replace(old, new)

_swap("disclosure_body", [
    ("inside your chat app (WhatsApp or WhatsApp Business)", "inside WhatsApp or WhatsApp Business"),
    ("आपके चैट ऐप (WhatsApp या WhatsApp Business) के अंदर", "WhatsApp या WhatsApp Business के अंदर"),
    ("aapke chat app (WhatsApp ya WhatsApp Business) ke andar", "WhatsApp ya WhatsApp Business ke andar"),
    ("dentro de tu app de chat (WhatsApp o WhatsApp Business)", "dentro de WhatsApp o WhatsApp Business"),
    ("dentro do seu app de chat (WhatsApp ou WhatsApp Business)", "dentro do WhatsApp ou WhatsApp Business"),
    ("داخل تطبيق المحادثة (واتساب أو واتساب للأعمال)", "داخل واتساب أو واتساب للأعمال"),
    ("di dalam aplikasi obrolan Anda (WhatsApp atau WhatsApp Business)", "di dalam WhatsApp atau WhatsApp Business"),
])
_swap("disclosure_body", [
    ("It works only in the chat apps you enable in Settings.", "It works only in WhatsApp and WhatsApp Business."),
    ("यह सिर्फ़ उन चैट ऐप्स में काम करती है जिन्हें आप सेटिंग्स में चालू करते हैं।", "यह सिर्फ़ WhatsApp और WhatsApp Business में काम करती है।"),
    ("Yeh sirf un chat apps mein kaam karti hai jinhe aap Settings mein on karte hain.", "Yeh sirf WhatsApp aur WhatsApp Business mein kaam karti hai."),
    ("Solo funciona en las apps de chat que actives en Ajustes.", "Solo funciona en WhatsApp y WhatsApp Business."),
    ("Funciona apenas nos apps de chat que você ativar nas Configurações.", "Funciona apenas no WhatsApp e no WhatsApp Business."),
    ("يعمل فقط داخل تطبيقات المحادثة التي تفعّلها من الإعدادات.", "يعمل فقط داخل واتساب وواتساب للأعمال."),
    ("Hanya bekerja di aplikasi obrolan yang Anda aktifkan di Pengaturan.", "Hanya bekerja di WhatsApp dan WhatsApp Business."),
])

add("a11y_description",
    "AuraMic taps the Send button in WhatsApp after you record a voice note with the floating mic. It works only inside WhatsApp and never reads or stores your messages.",
    "फ्लोटिंग माइक से वॉइस नोट रिकॉर्ड करने के बाद AuraMic WhatsApp में Send बटन दबाता है। यह सिर्फ़ WhatsApp के अंदर काम करता है और आपके संदेश कभी नहीं पढ़ता या सहेजता।",
    "Floating mic se voice note record karne ke baad AuraMic WhatsApp mein Send button dabata hai. Yeh sirf WhatsApp ke andar kaam karta hai aur aapke messages kabhi nahi padhta ya save karta.",
    "AuraMic pulsa el botón Enviar en WhatsApp tras grabar una nota de voz con el micrófono flotante. Solo funciona dentro de WhatsApp y nunca lee ni guarda tus mensajes.",
    "O AuraMic toca no botão Enviar do WhatsApp depois que você grava um áudio com o microfone flutuante. Funciona apenas dentro do WhatsApp e nunca lê nem armazena suas mensagens.",
    "يضغط AuraMic على زر الإرسال في واتساب بعد تسجيل رسالة صوتية بالميكروفون العائم. يعمل داخل واتساب فقط ولا يقرأ رسائلك ولا يخزّنها.",
    "AuraMic menekan tombol Kirim di WhatsApp setelah Anda merekam pesan suara dengan mikrofon melayang. Hanya bekerja di dalam WhatsApp dan tidak pernah membaca atau menyimpan pesan Anda.")

# ---- quality choice ----
add("q_title", "Voice quality", "आवाज़ की क्वालिटी", "Awaaz ki quality", "Calidad de voz", "Qualidade da voz", "جودة الصوت", "Kualitas suara")
add("q_medium", "Medium Quality", "मीडियम क्वालिटी", "Medium Quality", "Calidad media", "Qualidade média", "جودة متوسطة", "Kualitas Sedang")
add("q_medium_desc",
    "Fast and light. Removes most background noise like fans, traffic and room echo, and keeps your voice clear. Your voice note is ready in a moment and uses less battery. Great for everyday chats.",
    "तेज़ और हल्का। पंखे, ट्रैफिक और कमरे की गूँज जैसा ज़्यादातर बैकग्राउंड शोर हटाता है और आपकी आवाज़ साफ़ रखता है। वॉइस नोट तुरंत तैयार होता है और बैटरी कम खर्च होती है। रोज़ की चैट के लिए बढ़िया।",
    "Tez aur halka. Pankhe, traffic aur kamre ki goonj jaisa zyada tar background shor hatata hai aur aapki awaaz saaf rakhta hai. Voice note turant ready hota hai aur battery kam kharch hoti hai. Roz ki chat ke liye badhiya.",
    "Rápido y ligero. Quita la mayor parte del ruido de fondo, como ventiladores, tráfico y eco de la habitación, y mantiene tu voz clara. Tu nota de voz está lista al instante y gasta menos batería. Ideal para chats de todos los días.",
    "Rápido e leve. Remove a maior parte do ruído de fundo, como ventiladores, trânsito e eco do ambiente, e mantém sua voz clara. Seu áudio fica pronto num instante e gasta menos bateria. Ótimo para conversas do dia a dia.",
    "سريع وخفيف. يزيل معظم ضجيج الخلفية مثل المراوح والسيارات وصدى الغرفة ويُبقي صوتك واضحًا. تكون رسالتك الصوتية جاهزة في لحظات وتستهلك بطارية أقل. مناسب للمحادثات اليومية.",
    "Cepat dan ringan. Menghilangkan sebagian besar bising latar seperti kipas, lalu lintas, dan gema ruangan, dan menjaga suara Anda tetap jernih. Pesan suara siap dalam sekejap dan hemat baterai. Cocok untuk obrolan sehari-hari.")
add("q_high", "High Quality", "हाई क्वालिटी", "High Quality", "Calidad alta", "Qualidade alta", "جودة عالية", "Kualitas Tinggi")
add("q_high_desc",
    "Studio-clean sound. Removes almost all background noise, even people talking nearby, and makes your voice rich, balanced and loud enough, like a professionally recorded podcast. Takes a few seconds longer and uses a little more battery.",
    "स्टूडियो जैसी साफ़ आवाज़। लगभग सारा बैकग्राउंड शोर हटा देता है, पास में बोलते लोगों की आवाज़ भी, और आपकी आवाज़ को भरी-पूरी, संतुलित और पर्याप्त तेज़ बनाता है, जैसे प्रोफ़ेशनल रिकॉर्ड किया गया पॉडकास्ट। कुछ सेकंड ज़्यादा लगते हैं और बैटरी थोड़ी ज़्यादा खर्च होती है।",
    "Studio jaisi saaf awaaz. Lagbhag sara background shor hata deta hai, paas mein bolte logon ki awaaz bhi, aur aapki awaaz ko bhari-puri, balanced aur kaafi tez banata hai, bilkul professional podcast jaisi. Kuch second zyada lagte hain aur battery thodi zyada kharch hoti hai.",
    "Sonido limpio de estudio. Elimina casi todo el ruido de fondo, incluso a personas que hablan cerca, y deja tu voz rica, equilibrada y con buen volumen, como un pódcast grabado profesionalmente. Tarda unos segundos más y gasta un poco más de batería.",
    "Som limpo de estúdio. Remove quase todo o ruído de fundo, até pessoas falando por perto, e deixa sua voz rica, equilibrada e com bom volume, como um podcast gravado profissionalmente. Leva alguns segundos a mais e gasta um pouco mais de bateria.",
    "صوت نقي بجودة الاستوديو. يزيل كل ضجيج الخلفية تقريبًا، حتى أحاديث الأشخاص القريبين، ويجعل صوتك غنيًا ومتوازنًا وعالي الوضوح كأنه بودكاست مسجّل باحتراف. يستغرق بضع ثوانٍ أكثر ويستهلك بطارية أكثر قليلًا.",
    "Suara bersih ala studio. Menghilangkan hampir semua bising latar, bahkan suara orang yang berbicara di dekat Anda, dan membuat suara Anda kaya, seimbang, dan cukup keras seperti podcast rekaman profesional. Butuh beberapa detik lebih lama dan sedikit lebih boros baterai.")
add("q_badge_free", "FREE", "फ्री", "FREE", "GRATIS", "GRÁTIS", "مجاني", "GRATIS")
add("q_badge_pro", "PRO", "PRO", "PRO", "PRO", "PRO", "PRO", "PRO")
add("q_selected", "Selected", "चुना हुआ", "Selected", "Seleccionado", "Selecionado", "محدد", "Dipilih")
add("q_trial_left", "%1$d free High Quality voice notes left", "%1$d फ्री हाई क्वालिटी वॉइस नोट बचे हैं", "%1$d free High Quality voice note bache hain",
    "Te quedan %1$d notas de voz gratis en calidad alta", "Restam %1$d áudios grátis em qualidade alta", "تبقّت %1$d رسائل صوتية مجانية بجودة عالية", "Sisa %1$d pesan suara Kualitas Tinggi gratis")
add("q_trial_over", "Your free High Quality tries are over. Get Pro to keep using it.", "आपके फ्री हाई क्वालिटी नोट खत्म हो गए। इसे चलाते रहने के लिए Pro लें।",
    "Aapke free High Quality note khatam ho gaye. Chalate rehne ke liye Pro lein.", "Se acabaron tus pruebas gratis de calidad alta. Obtén Pro para seguir usándola.",
    "Suas tentativas grátis de qualidade alta acabaram. Assine o Pro para continuar usando.", "انتهت تجاربك المجانية بالجودة العالية. اشترك في Pro لمواصلة استخدامها.",
    "Percobaan gratis Kualitas Tinggi sudah habis. Berlangganan Pro untuk terus memakainya.")
add("q_pro_active", "Pro is active — High Quality is unlocked.", "Pro चालू है — हाई क्वालिटी अनलॉक है।", "Pro active hai — High Quality unlock hai.",
    "Pro está activo: la calidad alta está desbloqueada.", "O Pro está ativo — a qualidade alta está liberada.", "اشتراك Pro فعّال — الجودة العالية مفتوحة.", "Pro aktif — Kualitas Tinggi terbuka.")
add("q_get_pro", "Get Pro", "Pro लें", "Pro lein", "Obtener Pro", "Assinar o Pro", "احصل على Pro", "Dapatkan Pro")

# ---- Pro screen ----
add("pro_title", "AuraMic Pro", "AuraMic Pro", "AuraMic Pro", "AuraMic Pro", "AuraMic Pro", "AuraMic Pro", "AuraMic Pro")
add("pro_subtitle", "Studio-clean voice notes, every time.", "हर बार स्टूडियो जैसे साफ़ वॉइस नोट।", "Har baar studio jaise saaf voice note.",
    "Notas de voz limpias como de estudio, siempre.", "Áudios limpos como de estúdio, sempre.", "رسائل صوتية نقية كالاستوديو في كل مرة.", "Pesan suara bersih ala studio, setiap saat.")
add("pro_b1", "High Quality cleaning on every voice note", "हर वॉइस नोट पर हाई क्वालिटी सफ़ाई", "Har voice note par High Quality safai",
    "Limpieza de calidad alta en cada nota de voz", "Limpeza em qualidade alta em todos os áudios", "تنقية بجودة عالية لكل رسالة صوتية", "Pembersihan Kualitas Tinggi di setiap pesan suara")
add("pro_b2", "Your voice sounds rich, clear and professional", "आपकी आवाज़ भरी-पूरी, साफ़ और प्रोफ़ेशनल लगती है", "Aapki awaaz bhari-puri, saaf aur professional lagti hai",
    "Tu voz suena rica, clara y profesional", "Sua voz soa rica, clara e profissional", "يبدو صوتك غنيًا وواضحًا واحترافيًا", "Suara Anda terdengar kaya, jernih, dan profesional")
add("pro_b3", "Works fully offline — your voice never leaves your phone", "पूरी तरह ऑफ़लाइन — आपकी आवाज़ फ़ोन से बाहर नहीं जाती",
    "Poori tarah offline — aapki awaaz phone se bahar nahi jaati", "Funciona sin conexión: tu voz nunca sale de tu teléfono",
    "Funciona totalmente offline — sua voz nunca sai do celular", "يعمل بدون إنترنت — صوتك لا يغادر هاتفك أبدًا", "Berfungsi sepenuhnya offline — suara Anda tidak pernah keluar dari ponsel")
add("pro_b4", "Cancel any time in Google Play", "Google Play में कभी भी रद्द करें", "Google Play mein kabhi bhi cancel karein",
    "Cancela cuando quieras en Google Play", "Cancele quando quiser no Google Play", "ألغِ في أي وقت من Google Play", "Batalkan kapan saja di Google Play")
add("pro_monthly", "Monthly", "मासिक", "Monthly", "Mensual", "Mensal", "شهري", "Bulanan")
add("pro_yearly", "Yearly", "वार्षिक", "Yearly", "Anual", "Anual", "سنوي", "Tahunan")
add("pro_best", "Best value", "सबसे किफ़ायती", "Sabse kifayati", "Mejor valor", "Melhor custo-benefício", "الأفضل قيمة", "Paling hemat")
add("pro_per_month", "per month", "प्रति माह", "per month", "al mes", "por mês", "شهريًا", "per bulan")
add("pro_per_year", "per year", "प्रति वर्ष", "per year", "al año", "por ano", "سنويًا", "per tahun")
add("pro_subscribe", "Subscribe", "सब्सक्राइब करें", "Subscribe karein", "Suscribirme", "Assinar", "اشترك", "Berlangganan")
add("pro_free", "Continue with free Medium Quality", "फ्री मीडियम क्वालिटी के साथ जारी रखें", "Free Medium Quality ke saath chalu rakhein",
    "Seguir con la calidad media gratis", "Continuar com a qualidade média grátis", "المتابعة بالجودة المتوسطة المجانية", "Lanjut dengan Kualitas Sedang gratis")
add("pro_restore", "Restore purchase", "खरीदारी बहाल करें", "Purchase restore karein", "Restaurar compra", "Restaurar compra", "استعادة الشراء", "Pulihkan pembelian")
add("pro_manage", "Manage subscription", "सब्सक्रिप्शन मैनेज करें", "Subscription manage karein", "Administrar suscripción", "Gerenciar assinatura", "إدارة الاشتراك", "Kelola langganan")
add("pro_unavailable", "Google Play isn't available right now. Check your internet connection and try again.", "Google Play अभी उपलब्ध नहीं है। इंटरनेट जाँचकर फिर कोशिश करें।",
    "Google Play abhi available nahi hai. Internet check karke dobara try karein.", "Google Play no está disponible ahora. Revisa tu conexión a internet e inténtalo de nuevo.",
    "O Google Play não está disponível agora. Verifique sua internet e tente de novo.", "متجر Google Play غير متاح الآن. تحقق من اتصالك بالإنترنت وحاول مرة أخرى.",
    "Google Play tidak tersedia saat ini. Periksa koneksi internet lalu coba lagi.")
add("pro_active_title", "You're a Pro member 🎉", "आप Pro सदस्य हैं 🎉", "Aap Pro member hain 🎉", "Eres miembro Pro 🎉", "Você é membro Pro 🎉", "أنت عضو Pro 🎉", "Anda anggota Pro 🎉")

# ---- in-app update ----
add("upd_title", "New version available", "नया वर्ज़न उपलब्ध है", "Naya version available hai", "Nueva versión disponible", "Nova versão disponível", "إصدار جديد متاح", "Versi baru tersedia")
add("upd_btn", "Update", "अपडेट करें", "Update karein", "Actualizar", "Atualizar", "تحديث", "Perbarui")
add("upd_downloading", "Downloading update…", "अपडेट डाउनलोड हो रहा है…", "Update download ho raha hai…", "Descargando actualización…", "Baixando atualização…", "جارٍ تنزيل التحديث…", "Mengunduh pembaruan…")
add("upd_ready", "Update is ready", "अपडेट तैयार है", "Update ready hai", "La actualización está lista", "A atualização está pronta", "التحديث جاهز", "Pembaruan siap")
add("upd_restart", "Restart", "रीस्टार्ट", "Restart karein", "Reiniciar", "Reiniciar", "إعادة التشغيل", "Mulai ulang")
add("set_check_update", "Check for updates", "अपडेट जाँचें", "Update check karein", "Buscar actualizaciones", "Buscar atualizações", "التحقق من التحديثات", "Periksa pembaruan")
add("upd_latest", "You have the latest version", "आपके पास नया वर्ज़न है", "Aapke paas latest version hai", "Tienes la última versión", "Você está na versão mais recente", "لديك أحدث إصدار", "Anda memakai versi terbaru")

# ---- plain-language rewrites ----
add("set_battery", "Keep AuraMic running", "AuraMic को चालू रखें", "AuraMic ko chalu rakhein", "Mantener AuraMic activo", "Manter o AuraMic ativo", "إبقاء AuraMic يعمل", "Jaga AuraMic tetap aktif")
add("set_diag", "Copy support info", "सपोर्ट जानकारी कॉपी करें", "Support info copy karein", "Copiar información para soporte", "Copiar informações para o suporte", "نسخ معلومات الدعم", "Salin info dukungan")
add("set_diag_desc", "Having trouble with auto-send? Copy this and send it to our support team.", "ऑटो-सेंड में दिक्कत है? इसे कॉपी करके हमारी सपोर्ट टीम को भेजें।",
    "Auto-send mein dikkat hai? Ise copy karke hamari support team ko bhejein.", "¿Problemas con el envío automático? Cópialo y envíalo a soporte.",
    "Problemas com o envio automático? Copie e envie ao suporte.", "مشكلة في الإرسال التلقائي؟ انسخ هذا وأرسله إلى فريق الدعم.", "Ada masalah dengan kirim otomatis? Salin ini dan kirim ke tim dukungan.")
add("set_auto_send", "Send automatically", "अपने-आप भेजें", "Apne-aap bhejein", "Enviar automáticamente", "Enviar automaticamente", "إرسال تلقائي", "Kirim otomatis")
add("set_auto_send_desc", "Sends the voice note for you as soon as it is clean.", "वॉइस नोट साफ़ होते ही अपने-आप भेज देता है।", "Voice note saaf hote hi apne-aap bhej deta hai.",
    "Envía la nota de voz por ti en cuanto está limpia.", "Envia o áudio por você assim que ficar limpo.", "يرسل الرسالة الصوتية عنك فور تنظيفها.", "Mengirim pesan suara untuk Anda begitu selesai dibersihkan.")
add("set_haptics", "Vibration", "वाइब्रेशन", "Vibration", "Vibración", "Vibração", "الاهتزاز", "Getar")
add("set_overlay", "Floating mic", "फ्लोटिंग माइक", "Floating mic", "Micrófono flotante", "Microfone flutuante", "الميكروفون العائم", "Mikrofon melayang")
add("lab_original", "Before (your voice as recorded)", "पहले (आपकी रिकॉर्ड की आवाज़)", "Pehle (aapki record ki hui awaaz)", "Antes (tu voz tal como se grabó)", "Antes (sua voz como foi gravada)", "قبل (صوتك كما سُجّل)", "Sebelum (suara Anda apa adanya)")
add("lab_clean", "After (noise removed)", "बाद में (शोर हटा हुआ)", "Baad mein (shor hata hua)", "Después (sin ruido)", "Depois (sem ruído)", "بعد (دون ضجيج)", "Sesudah (bising dihapus)")
add("lab_done", "Done! Listen to the difference.", "हो गया! फ़र्क सुनें।", "Ho gaya! Farak suniye.", "¡Listo! Escucha la diferencia.", "Pronto! Ouça a diferença.", "تم! استمع إلى الفرق.", "Selesai! Dengarkan bedanya.")
add("lab_info", "Cleaned in %1$.1f seconds", "%1$.1f सेकंड में साफ़ किया", "%1$.1f second mein saaf kiya", "Limpiado en %1$.1f segundos", "Limpo em %1$.1f segundos", "تم التنظيف خلال %1$.1f ثانية", "Dibersihkan dalam %1$.1f detik")
add("home_stat_minutes", "Minutes cleaned", "साफ़ किए गए मिनट", "Saaf kiye gaye minutes", "Minutos limpiados", "Minutos limpos", "دقائق تم تنظيفها", "Menit dibersihkan")
add("home_open_whatsapp", "Open WhatsApp", "WhatsApp खोलें", "WhatsApp kholein", "Abrir WhatsApp", "Abrir o WhatsApp", "افتح واتساب", "Buka WhatsApp")
add("home_whatsapp_missing", "WhatsApp is not installed on this phone.", "इस फ़ोन में WhatsApp इंस्टॉल नहीं है।", "Is phone mein WhatsApp install nahi hai.",
    "WhatsApp no está instalado en este teléfono.", "O WhatsApp não está instalado neste celular.", "واتساب غير مثبّت على هذا الهاتف.", "WhatsApp belum terpasang di ponsel ini.")
add("tour_t6_body", "A few quick permissions make this work. Everything stays on your phone.", "कुछ ज़रूरी अनुमतियों से यह काम करता है। सब कुछ आपके फ़ोन पर ही रहता है।",
    "Kuch zaroori permissions se yeh kaam karta hai. Sab kuch aapke phone par hi rehta hai.", "Unos pocos permisos hacen que funcione. Todo se queda en tu teléfono.",
    "Algumas permissões fazem isso funcionar. Tudo fica no seu celular.", "بضع أذونات سريعة تجعل هذا يعمل. كل شيء يبقى على هاتفك.", "Beberapa izin singkat membuatnya berfungsi. Semua tetap di ponsel Anda.")
add("tour_p2_body", "This one taps Send in WhatsApp after your voice note is clean.", "यह आपका वॉइस नोट साफ़ होने के बाद WhatsApp में Send दबाता है।",
    "Yeh aapka voice note saaf hone ke baad WhatsApp mein Send dabata hai.", "Este toca Enviar en WhatsApp cuando tu nota de voz está limpia.",
    "Este toca em Enviar no WhatsApp quando seu áudio está limpo.", "هذا يضغط على إرسال في واتساب بعد تنظيف رسالتك الصوتية.", "Ini menekan Kirim di WhatsApp setelah pesan suara Anda bersih.")
add("perm_usage_title", "Know when WhatsApp is open", "पता चले कि WhatsApp खुला है", "Pata chale ki WhatsApp khula hai", "Saber cuándo WhatsApp está abierto", "Saber quando o WhatsApp está aberto", "معرفة متى يكون واتساب مفتوحًا", "Tahu saat WhatsApp terbuka")
add("perm_usage_body", "So the mic shows only inside WhatsApp. Android calls this “Usage access”.", "ताकि माइक सिर्फ़ WhatsApp के अंदर दिखे। Android इसे “Usage access” कहता है।",
    "Taaki mic sirf WhatsApp ke andar dikhe. Android ise “Usage access” kehta hai.", "Para que el micrófono salga solo dentro de WhatsApp. Android lo llama “Acceso de uso”.",
    "Para o microfone aparecer só dentro do WhatsApp. O Android chama isso de “Acesso de uso”.", "ليظهر الميكروفون داخل واتساب فقط. يسمّيه أندرويد «الوصول إلى الاستخدام».",
    "Agar mikrofon hanya muncul di WhatsApp. Android menyebutnya “Akses penggunaan”.")
add("perm_access_title", "Auto-send", "ऑटो-सेंड", "Auto-send", "Envío automático", "Envio automático", "الإرسال التلقائي", "Kirim otomatis")
add("perm_access_body", "Taps Send for you once your voice note is clean. Android calls this “Accessibility”.", "वॉइस नोट साफ़ होने पर आपके लिए Send दबाता है। Android इसे “Accessibility” कहता है।",
    "Voice note saaf hone par aapke liye Send dabata hai. Android ise “Accessibility” kehta hai.", "Toca Enviar por ti cuando tu nota está limpia. Android lo llama “Accesibilidad”.",
    "Toca em Enviar por você quando o áudio está limpo. O Android chama isso de “Acessibilidade”.", "يضغط إرسال نيابةً عنك بعد تنظيف الرسالة. يسمّيه أندرويد «إمكانية الوصول».",
    "Menekan Kirim untuk Anda setelah pesan suara bersih. Android menyebutnya “Aksesibilitas”.")


add("set_cut_pauses", "Remove long pauses", "लंबी चुप्पी हटाएँ", "Lambi chuppi hatayein", "Quitar pausas largas", "Remover pausas longas", "إزالة الفترات الصامتة الطويلة", "Hapus jeda panjang")
add("set_cut_pauses_desc", "Cuts silent gaps longer than 0.3 seconds so your voice note is shorter and tighter.", "0.3 सेकंड से लंबी चुप्पी काट देता है, ताकि वॉइस नोट छोटा और कसा हुआ रहे।",
    "0.3 second se lambi chuppi kaat deta hai, taaki voice note chhota aur tight rahe.", "Corta los silencios de más de 0,3 segundos para que tu nota de voz sea más corta y ágil.",
    "Corta silêncios maiores que 0,3 segundo para deixar seu áudio mais curto e objetivo.", "يقطع الفراغات الصامتة الأطول من 0.3 ثانية ليصبح تسجيلك أقصر وأكثر إحكامًا.",
    "Memotong jeda hening lebih dari 0,3 detik agar pesan suara lebih singkat dan padat.")

_extra = [
    "\n\nIt also looks up where WhatsApp's own voice button is on screen, so the AuraMic microphone can sit exactly on top of it. It never reads message text.",
    "\n\nयह स्क्रीन पर WhatsApp के अपने वॉइस बटन की जगह भी देखता है, ताकि AuraMic का माइक ठीक उसके ऊपर बैठे। यह मैसेज का टेक्स्ट कभी नहीं पढ़ता।",
    "\n\nYeh screen par WhatsApp ke apne voice button ki jagah bhi dekhta hai, taaki AuraMic ka mic theek uske upar baithe. Yeh message ka text kabhi nahi padhta.",
    "\n\nTambién localiza dónde está el botón de voz de WhatsApp en la pantalla para colocar el micrófono de AuraMic justo encima. Nunca lee el texto de los mensajes.",
    "\n\nTambém localiza onde fica o botão de voz do WhatsApp na tela para posicionar o microfone do AuraMic exatamente sobre ele. Nunca lê o texto das mensagens.",
    "\n\nكما يحدد مكان زر الصوت الخاص بواتساب على الشاشة ليظهر ميكروفون AuraMic فوقه تمامًا. لا يقرأ نص الرسائل أبدًا.",
    "\n\nLayanan ini juga mencari posisi tombol suara WhatsApp di layar agar mikrofon AuraMic berada tepat di atasnya. Tidak pernah membaca teks pesan.",
]
S["disclosure_body"] = [v + e for v, e in zip(S["disclosure_body"], _extra)]

add("set_device_id", "Device ID (tap to copy)", "डिवाइस ID (कॉपी करने के लिए टैप करें)", "Device ID (copy karne ke liye tap karein)", "ID del dispositivo (toca para copiar)", "ID do dispositivo (toque para copiar)", "معرّف الجهاز (اضغط للنسخ)", "ID perangkat (ketuk untuk menyalin)")

add("pro_renews", "Renews automatically until you cancel in Google Play.", "Google Play में रद्द करने तक अपने-आप नवीनीकृत होता है।", "Google Play mein cancel karne tak apne-aap renew hota hai.",
    "Se renueva automáticamente hasta que canceles en Google Play.", "Renova automaticamente até você cancelar no Google Play.", "يتجدد تلقائيًا حتى تلغيه من Google Play.", "Diperpanjang otomatis sampai Anda membatalkannya di Google Play.")
_a11y_extra = [" It also finds WhatsApp's voice button so the AuraMic mic can sit on it.", " यह WhatsApp का वॉइस बटन भी ढूँढता है ताकि AuraMic का माइक उस पर बैठे।", " Yeh WhatsApp ka voice button bhi dhoondhta hai taaki AuraMic ka mic uske upar baithe.",
    " También localiza el botón de voz de WhatsApp para colocar el micrófono de AuraMic encima.", " Também localiza o botão de voz do WhatsApp para posicionar o microfone do AuraMic sobre ele.", " كما يحدد مكان زر الصوت في واتساب ليظهر ميكروفون AuraMic فوقه.", " Juga mencari tombol suara WhatsApp agar mikrofon AuraMic berada di atasnya."]
S["a11y_description"] = [v + e for v, e in zip(S["a11y_description"], _a11y_extra)]

def esc(v: str) -> str:
    v = escape(v).replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    if v.startswith("@") or v.startswith("?"):
        v = "\\" + v
    return v


def main(res_dir):
    for i, lang in enumerate(LANGS):
        d = os.path.join(res_dir, DIRS[lang])
        os.makedirs(d, exist_ok=True)
        lines = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- GENERATED by tools/gen_strings.py - edit the table there. -->", "<resources>"]
        if lang == "en":
            lines.append('    <string name="app_name" translatable="false">AuraMic AI</string>')
        for key, vals in S.items():
            v = vals[i]
            fmt = ' formatted="true"' if "%" in v else ""
            lines.append(f'    <string name="{key}"{fmt}>{esc(v)}</string>')
        lines.append("</resources>\n")
        with open(os.path.join(d, "strings.xml"), "w", encoding="utf-8") as f:
            f.write("\n".join(lines))
    print(f"wrote {len(S)} strings x {len(LANGS)} languages")


if __name__ == "__main__":
    main(sys.argv[1])
