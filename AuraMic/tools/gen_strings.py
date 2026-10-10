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
add("tour_p2_body", "This one taps WhatsApp's Send button after cleaning. It only works inside WhatsApp.",
    "यह सफाई के बाद WhatsApp का Send बटन दबाती है। यह सिर्फ़ WhatsApp के अंदर काम करती है।",
    "Yeh safai ke baad WhatsApp ka Send button dabati hai. Yeh sirf WhatsApp ke andar kaam karti hai.",
    "Esta pulsa el botón Enviar de WhatsApp tras la limpieza. Solo funciona dentro de WhatsApp.",
    "Esta toca no botão Enviar do WhatsApp depois da limpeza. Só funciona dentro do WhatsApp.",
    "تضغط هذه على زر الإرسال في واتساب بعد التنظيف، وتعمل داخل واتساب فقط.",
    "Ini menekan tombol Kirim WhatsApp setelah pembersihan. Hanya berfungsi di dalam WhatsApp.")

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
add("perm_access_body", "Taps WhatsApp's Send button for you once the voice note is clean.",
    "वॉइस नोट साफ़ होने पर आपके लिए WhatsApp का Send बटन दबाती है।",
    "Voice note saaf hone par aapke liye WhatsApp ka Send button dabati hai.",
    "Pulsa por ti el botón Enviar de WhatsApp cuando la nota de voz está limpia.",
    "Toca no botão Enviar do WhatsApp por você quando o áudio estiver limpo.",
    "تضغط نيابةً عنك على زر الإرسال في واتساب عندما تصبح الرسالة الصوتية نظيفة.",
    "Menekan tombol Kirim WhatsApp untuk Anda setelah pesan suara bersih.")
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
add("disclosure_body", "AuraMic uses Android's Accessibility Service for one purpose only: after you record a voice note, it taps the Send button inside WhatsApp so the note is delivered without extra taps.\n\nIt works only in WhatsApp and WhatsApp Business. It does not read, collect, store or share your messages, contacts or any personal data. Audio is processed on your device.",
    "AuraMic एक्सेसिबिलिटी सेवा का उपयोग सिर्फ़ एक काम के लिए करता है: वॉइस नोट रिकॉर्ड करने के बाद WhatsApp के अंदर Send बटन दबाना ताकि बिना अतिरिक्त टैप के नोट भेजा जाए।\n\nयह सिर्फ़ WhatsApp और WhatsApp Business में काम करती है। यह आपके संदेश, संपर्क या कोई निजी डेटा न पढ़ती है, न जमा करती है, न साझा करती है। ऑडियो आपके डिवाइस पर ही प्रोसेस होती है।",
    "AuraMic Accessibility Service ka use sirf ek kaam ke liye karta hai: voice note record karne ke baad WhatsApp ke andar Send button dabana, taaki note bina extra tap ke bhej diya jaye.\n\nYeh sirf WhatsApp aur WhatsApp Business mein kaam karti hai. Yeh aapke messages, contacts ya koi personal data na padhti hai, na store karti hai, na share karti hai. Audio aapke device par hi process hoti hai.",
    "AuraMic usa el Servicio de accesibilidad de Android con un único propósito: después de grabar una nota de voz, pulsa el botón Enviar dentro de WhatsApp para entregarla sin toques adicionales.\n\nSolo funciona en WhatsApp y WhatsApp Business. No lee, recopila, almacena ni comparte tus mensajes, contactos ni datos personales. El audio se procesa en tu dispositivo.",
    "O AuraMic usa o Serviço de Acessibilidade do Android com um único propósito: depois que você grava um áudio, ele toca no botão Enviar dentro do WhatsApp para entregá-lo sem toques extras.\n\nFunciona apenas no WhatsApp e no WhatsApp Business. Não lê, coleta, armazena nem compartilha suas mensagens, contatos ou dados pessoais. O áudio é processado no seu dispositivo.",
    "يستخدم AuraMic خدمة إمكانية الوصول في أندرويد لغرض واحد فقط: بعد تسجيل رسالة صوتية، يضغط على زر الإرسال داخل واتساب لتسليمها دون نقرات إضافية.\n\nيعمل فقط داخل واتساب وواتساب للأعمال. لا يقرأ رسائلك أو جهات اتصالك أو أي بيانات شخصية ولا يجمعها أو يخزّنها أو يشاركها. تتم معالجة الصوت على جهازك.",
    "AuraMic menggunakan Layanan Aksesibilitas Android hanya untuk satu tujuan: setelah Anda merekam pesan suara, layanan ini menekan tombol Kirim di dalam WhatsApp agar pesan terkirim tanpa ketukan tambahan.\n\nHanya bekerja di WhatsApp dan WhatsApp Business. Tidak membaca, mengumpulkan, menyimpan, atau membagikan pesan, kontak, maupun data pribadi Anda. Audio diproses di perangkat Anda.")
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
add("home_open_whatsapp", "Open WhatsApp", "WhatsApp खोलें", "WhatsApp kholein", "Abrir WhatsApp", "Abrir WhatsApp", "فتح واتساب", "Buka WhatsApp")
add("home_whatsapp_missing", "WhatsApp isn't installed on this phone.", "इस फ़ोन में WhatsApp इंस्टॉल नहीं है।",
    "Is phone mein WhatsApp install nahi hai.", "WhatsApp no está instalado en este teléfono.", "O WhatsApp não está instalado neste celular.",
    "واتساب غير مثبّت على هذا الهاتف.", "WhatsApp tidak terpasang di ponsel ini.")
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
add("lab_reduction", "About %1$d dB quieter background", "बैकग्राउंड लगभग %1$d dB शांत", "Background lagbhag %1$d dB shaant",
    "Fondo unos %1$d dB más silencioso", "Fundo cerca de %1$d dB mais silencioso", "الخلفية أهدأ بحوالي %1$d ديسيبل", "Latar sekitar %1$d dB lebih senyap")

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
add("set_business", "Also work in WhatsApp Business", "WhatsApp Business में भी काम करे", "WhatsApp Business mein bhi kaam kare", "Funcionar también en WhatsApp Business", "Funcionar também no WhatsApp Business", "العمل أيضًا في واتساب للأعمال", "Berfungsi juga di WhatsApp Business")
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
add("ov_hint", "Hold to record", "रिकॉर्ड के लिए दबाएँ", "Record ke liye dabayein", "Mantén para grabar", "Segure para gravar", "اضغط مطولًا للتسجيل", "Tahan untuk merekam")
add("ov_slide_cancel", "◀ Slide to cancel", "◀ रद्द करने के लिए स्लाइड करें", "◀ Cancel ke liye slide karein", "◀ Desliza para cancelar", "◀ Deslize para cancelar", "◀ اسحب للإلغاء", "◀ Geser untuk batal")
add("ov_release_cancel", "Release to cancel", "रद्द करने के लिए छोड़ें", "Cancel ke liye chhodein", "Suelta para cancelar", "Solte para cancelar", "ارفع إصبعك للإلغاء", "Lepas untuk batal")
add("ov_processing", "Cleaning audio…", "ऑडियो साफ़ हो रही है…", "Audio saaf ho rahi hai…", "Limpiando audio…", "Limpando áudio…", "جارٍ تنظيف الصوت…", "Membersihkan audio…")
add("ov_sending", "Sending…", "भेज रहा है…", "Bhej raha hai…", "Enviando…", "Enviando…", "جارٍ الإرسال…", "Mengirim…")
add("ov_sent", "Sent!", "भेज दिया!", "Bhej diya!", "¡Enviado!", "Enviado!", "تم الإرسال!", "Terkirim!")
add("ov_failed", "Couldn't send — tap Send manually", "भेज नहीं सका — Send खुद दबाएँ", "Bhej nahi saka — Send khud dabayein", "No se pudo enviar: pulsa Enviar", "Não foi possível enviar: toque em Enviar", "تعذّر الإرسال — اضغط إرسال يدويًا", "Gagal mengirim — tekan Kirim manual")
add("ov_too_short", "Too short", "बहुत छोटा", "Bahut chhota", "Muy corto", "Muito curto", "قصير جدًا", "Terlalu singkat")
add("ov_manual", "Pick a chat, then tap Send", "चैट चुनें, फिर Send दबाएँ", "Chat chunein, phir Send dabayein", "Elige un chat y pulsa Enviar", "Escolha uma conversa e toque em Enviar", "اختر محادثة ثم اضغط إرسال", "Pilih obrolan, lalu tekan Kirim")
add("ov_error_mic", "Microphone is busy", "माइक्रोफ़ोन व्यस्त है", "Microphone busy hai", "El micrófono está ocupado", "O microfone está ocupado", "الميكروفون مشغول", "Mikrofon sedang dipakai")
add("cd_mic", "Record voice note", "वॉइस नोट रिकॉर्ड करें", "Voice note record karein", "Grabar nota de voz", "Gravar áudio", "تسجيل رسالة صوتية", "Rekam pesan suara")
add("notif_channel", "AuraMic service", "AuraMic सेवा", "AuraMic service", "Servicio de AuraMic", "Serviço do AuraMic", "خدمة AuraMic", "Layanan AuraMic")
add("notif_title", "AuraMic is ready", "AuraMic तैयार है", "AuraMic tayyar hai", "AuraMic está listo", "AuraMic está pronto", "AuraMic جاهز", "AuraMic siap")
add("notif_text", "The floating mic appears when WhatsApp is open.", "WhatsApp खुलने पर फ्लोटिंग माइक दिखता है।", "WhatsApp khulne par floating mic dikhta hai.",
    "El micrófono flotante aparece cuando WhatsApp está abierto.", "O microfone flutuante aparece quando o WhatsApp está aberto.", "يظهر الميكروفون العائم عند فتح واتساب.", "Mikrofon melayang muncul saat WhatsApp terbuka.")
add("notif_stop", "Turn off", "बंद करें", "Off karein", "Desactivar", "Desativar", "إيقاف", "Nonaktifkan")
add("a11y_description", "AuraMic taps WhatsApp's Send button after you record a voice note with the floating mic. It works only inside WhatsApp and never reads or stores your messages.",
    "फ्लोटिंग माइक से वॉइस नोट रिकॉर्ड करने के बाद AuraMic WhatsApp का Send बटन दबाता है। यह सिर्फ़ WhatsApp में काम करता है और आपके संदेश कभी नहीं पढ़ता या सहेजता।",
    "Floating mic se voice note record karne ke baad AuraMic WhatsApp ka Send button dabata hai. Yeh sirf WhatsApp mein kaam karta hai aur aapke messages kabhi nahi padhta ya save karta.",
    "AuraMic pulsa el botón Enviar de WhatsApp tras grabar una nota de voz con el micrófono flotante. Solo funciona dentro de WhatsApp y nunca lee ni guarda tus mensajes.",
    "O AuraMic toca no botão Enviar do WhatsApp depois que você grava um áudio com o microfone flutuante. Funciona apenas dentro do WhatsApp e nunca lê nem armazena suas mensagens.",
    "يضغط AuraMic على زر الإرسال في واتساب بعد تسجيل رسالة صوتية بالميكروفون العائم. يعمل داخل واتساب فقط ولا يقرأ رسائلك ولا يخزّنها.",
    "AuraMic menekan tombol Kirim WhatsApp setelah Anda merekam pesan suara dengan mikrofon melayang. Hanya bekerja di dalam WhatsApp dan tidak pernah membaca atau menyimpan pesan Anda.")


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
