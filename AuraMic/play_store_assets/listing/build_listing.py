#!/usr/bin/env python3
"""Single source of truth for the Google Play listing copy.

Run `python build_listing.py` to (re)generate one folder per Play locale containing
title.txt / short_description.txt / full_description.txt, and to validate Play's limits
(title <= 30, short description <= 80, full description <= 4000 characters).
"""
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent
LIMITS = {"title": 30, "short_description": 80, "full_description": 4000}

DISCLAIMER_EN = ("AuraMic AI is an independent app. It is not affiliated with, endorsed by or sponsored by "
                 "WhatsApp LLC or Meta Platforms, Inc. WhatsApp is a trademark of WhatsApp LLC. Telegram and Instagram are trademarks of their respective owners.")

L = {}

# --------------------------------------------------------------------------------------------
L["en-US"] = dict(
    title="AuraMic AI: Voice Note Cleaner",
    short_description="Floating mic for WhatsApp & Telegram: AI noise removal, podcast-clear voice",
    full_description="""Studio-quality voice notes, anywhere. AuraMic AI puts a floating mic on top of WhatsApp: hold it, speak, let go — the background noise is removed by on-device AI and your voice note is sent for you.

Stop re-recording because of traffic, fans, crowds or a noisy café. AuraMic AI is a voice note cleaner and background noise remover that works in real time, right on your phone.

★ HOW IT WORKS
1. Open any WhatsApp chat — the floating mic appears (and disappears when you leave WhatsApp).
2. Press and hold the mic and speak. Slide left to cancel.
3. Release. AuraMic AI cleans your audio, picks the same chat and taps Send for you.

★ FEATURES
• AI noise suppression: removes steady background noise such as traffic, fans, AC, hum and wind, and reduces crowd noise.
• 100% on-device processing: no cloud, no account, no upload. Works in airplane mode.
• Floating mic that only shows inside WhatsApp and WhatsApp Business — never in your other apps.
• Auto-send: one hold, one release, done. Prefer to review? Turn auto-send off and choose the chat yourself.
• Loudness boost & limiter: evens out quiet voices and prevents distortion, so everyone hears you clearly.
• Studio Lab: record a noisy sample in the app and compare original vs. cleaned audio.
• Tiny, WhatsApp-friendly files (AAC/M4A, or Opus on Android 10+).
• Drag the mic anywhere; it snaps to the edge and remembers its place.
• Haptic feedback, smooth animations, light and dark themes.
• Quick Settings tile to switch AuraMic on or off in one tap.
• 7 languages: English, हिन्दी, Hinglish, Español, Português, العربية, Bahasa Indonesia.

★ PODCAST-GRADE CLEANUP
• DeepFilterNet3 AI (full-band 48 kHz) runs on your phone via ONNX Runtime: studio-style voice isolation, typically 10–30× faster than real time.
• Podcast polish: EQ, de-esser, compressor and −16 LUFS loudness mastering with a look-ahead limiter.
• Auto-cut silence: shorten long pauses and trim the start and end (Gentle or Tight).
• Separate the background: listen to the removed noise on its own, or keep a little ambience.
• Works in WhatsApp, WhatsApp Business and Telegram; Instagram is experimental (the note is saved for you to attach).

★ PERMISSIONS — WHY WE ASK
• Display over other apps: to show the floating mic on top of WhatsApp.
• Microphone: to record while you hold the button.
• Usage access: to know when WhatsApp is open, so the mic only appears there.
• Accessibility service: used ONLY to tap WhatsApp's Send button (and choose the chat you were in) after your voice note is ready. It works only inside WhatsApp / WhatsApp Business / Telegram / Instagram and does not read, store or share your messages or personal data. You will see a clear disclosure before enabling it.
• Notifications: shows a small status notification while AuraMic is on.

★ PRIVACY FIRST
Your voice never leaves your phone. We don't collect audio, messages, contacts or personal data. Temporary files are deleted automatically after sending.

★ TIPS
• Best results with steady noise; very loud or voice-like noise (e.g. a busy crowd) is reduced, not eliminated.
• On some phones (Xiaomi, Oppo, Vivo, Samsung) allow "Unrestricted battery" for AuraMic so it keeps running.
• If Android blocks the Accessibility switch for a sideloaded install, open App info → ⋮ → "Allow restricted settings".

Need help? support@fireclashpro.com

""" + DISCLAIMER_EN,
)

# --------------------------------------------------------------------------------------------
L["en-IN"] = dict(  # Hinglish listing for India (en-IN) - written the way users search and speak
    title="AuraMic AI: Voice Note Cleaner",
    short_description="WhatsApp ka floating mic: AI se noise hatao, studio jaisi voice note, auto-send",
    full_description="""Studio jaisi voice notes, kahin bhi! AuraMic AI WhatsApp ke upar ek floating mic laata hai: dabaao, bolo, chhodo — background noise on-device AI se hat jata hai aur voice note khud send ho jata hai.

Traffic, pankha, bheed ya noisy café ki wajah se baar-baar record karna band. AuraMic AI ek voice note cleaner aur background noise remover hai jo seedha aapke phone par chalta hai.

★ KAISE KAAM KARTA HAI
1. Koi bhi WhatsApp chat kholo — floating mic dikhne lagta hai (WhatsApp chhodte hi gayab).
2. Mic ko dabakar rakho aur bolo. Cancel karne ke liye left slide karo.
3. Chhodo. AuraMic AI audio saaf karta hai, wahi chat chunta hai aur Send bhi khud dabata hai.

★ KHAASIYAT
• AI noise suppression: traffic, fan, AC, hum, hawa jaisa lagataar noise hataata hai; bheed ka shor kam karta hai.
• 100% on-device: koi cloud nahi, koi account nahi, koi upload nahi. Airplane mode mein bhi chalta hai.
• Floating mic sirf WhatsApp aur WhatsApp Business mein dikhta hai.
• Auto-send: ek baar dabao, ek baar chhodo, bas. Khud check karna ho to auto-send band kar do.
• Loudness boost: dheemi awaaz ko barabar karta hai, distortion se bachata hai.
• Studio Lab: noisy sample record karke original aur clean audio ki tulna suno.
• Chhoti, WhatsApp-friendly files (AAC/M4A ya Android 10+ par Opus).
• Mic ko kahin bhi drag karo; yeh kinare se chipak jata hai aur jagah yaad rakhta hai.
• Haptic feedback, smooth animation, light aur dark theme.
• 7 bhasha: English, हिन्दी, Hinglish, Español, Português, العربية, Bahasa Indonesia.

★ PODCAST-GRADE SAFAI
• DeepFilterNet3 AI (full-band 48 kHz) seedha phone par chalta hai: studio jaisi voice isolation, real-time se 10–30× tez.
• Podcast polish: EQ, de-esser, compressor aur −16 LUFS loudness mastering.
• Auto-cut silence: lambe pauses chhote karo, shuruaat aur ant trim karo (Gentle ya Tight).
• Background alag karo: hataya hua noise alag se suno, ya thoda ambience rakho.
• WhatsApp, WhatsApp Business aur Telegram mein chalta hai; Instagram experimental hai (note save ho jata hai, aap attach karein).

★ PERMISSIONS — KYUN MAANGTE HAIN
• Display over other apps: WhatsApp ke upar floating mic dikhane ke liye.
• Microphone: button dabane tak record karne ke liye.
• Usage access: pata karne ke liye ki WhatsApp khula hai, taaki mic sirf wahin dikhe.
• Accessibility service: SIRF voice note taiyaar hone ke baad WhatsApp ka Send button dabane (aur wahi chat chunne) ke liye. Yeh sirf WhatsApp / WhatsApp Business / Telegram / Instagram mein kaam karti hai; aapke messages ya personal data ko na padhti hai, na save karti hai, na share karti hai. Enable karne se pehle saaf disclosure dikhaya jata hai.
• Notifications: AuraMic on hone par chhota status notification.

★ PRIVACY PEHLE
Aapki awaaz phone se bahar nahi jati. Hum audio, messages, contacts ya koi personal data collect nahi karte. Temporary files send ke baad apne aap delete ho jati hain.

Madad chahiye? support@fireclashpro.com

AuraMic AI ek independent app hai. Ismein WhatsApp LLC ya Meta Platforms, Inc. ka koi affiliation, endorsement ya sponsorship nahi hai. WhatsApp, WhatsApp LLC ka trademark hai. Telegram aur Instagram apne-apne maalikon ke trademark hain.""",
)

# --------------------------------------------------------------------------------------------
L["hi-IN"] = dict(
    title="AuraMic AI: वॉइस नोट क्लीनर",
    short_description="WhatsApp के लिए फ्लोटिंग माइक: AI से शोर हटाएँ, स्टूडियो जैसी वॉइस नोट, ऑटो-सेंड",
    full_description="""कहीं भी स्टूडियो जैसी वॉइस नोट। AuraMic AI आपके WhatsApp के ऊपर एक फ्लोटिंग माइक लाता है: दबाएँ, बोलें, छोड़ें — बैकग्राउंड शोर ऑन-डिवाइस AI से हट जाता है और वॉइस नोट अपने-आप भेज दिया जाता है।

ट्रैफ़िक, पंखे, भीड़ या शोर वाले कैफ़े की वजह से बार-बार रिकॉर्ड करना बंद। AuraMic AI एक वॉइस नोट क्लीनर और बैकग्राउंड नॉइज़ रिमूवर है जो सीधे आपके फ़ोन पर चलता है।

★ यह कैसे काम करता है
1. कोई भी WhatsApp चैट खोलें — फ्लोटिंग माइक दिखने लगता है (WhatsApp छोड़ते ही गायब)।
2. माइक को दबाकर रखें और बोलें। रद्द करने के लिए बाएँ स्लाइड करें।
3. छोड़ें। AuraMic AI ऑडियो साफ़ करता है, वही चैट चुनता है और Send भी अपने-आप दबाता है।

★ ख़ासियतें
• AI नॉइज़ सप्रेशन: ट्रैफ़िक, पंखा, AC, हम और हवा जैसा लगातार शोर हटाता है; भीड़ का शोर कम करता है।
• 100% ऑन-डिवाइस: कोई क्लाउड नहीं, कोई अकाउंट नहीं, कोई अपलोड नहीं। एयरप्लेन मोड में भी चलता है।
• फ्लोटिंग माइक सिर्फ़ WhatsApp और WhatsApp Business में दिखता है।
• ऑटो-सेंड: एक बार दबाएँ, एक बार छोड़ें, बस। खुद जाँचना हो तो ऑटो-सेंड बंद करें।
• आवाज़ बूस्ट: धीमी आवाज़ें बराबर करता है और डिस्टॉर्शन रोकता है।
• स्टूडियो लैब: शोर वाला सैंपल रिकॉर्ड करें और मूल व साफ़ ऑडियो की तुलना सुनें।
• छोटी, WhatsApp-फ्रेंडली फ़ाइलें (AAC/M4A, या Android 10+ पर Opus)।
• 7 भाषाएँ: English, हिन्दी, Hinglish, Español, Português, العربية, Bahasa Indonesia।

★ पॉडकास्ट-ग्रेड सफ़ाई
• DeepFilterNet3 AI (फुल-बैंड 48 kHz) सीधे आपके फ़ोन पर चलता है: स्टूडियो जैसी आवाज़, रियल-टाइम से 10–30× तेज़।
• पॉडकास्ट पॉलिश: EQ, डी-एसर, कम्प्रेसर और −16 LUFS लाउडनेस मास्टरिंग।
• चुप्पी अपने-आप काटें: लंबे विराम छोटे करें, शुरुआत और अंत ट्रिम करें (हल्का या सख़्त)।
• बैकग्राउंड अलग करें: हटाया गया शोर अलग सुनें, या थोड़ा माहौल रखें।
• WhatsApp, WhatsApp Business और Telegram में काम करता है; Instagram प्रायोगिक है (नोट सहेजा जाता है, आप जोड़ें)।

★ अनुमतियाँ — हम क्यों माँगते हैं
• अन्य ऐप्स के ऊपर दिखाना: WhatsApp के ऊपर फ्लोटिंग माइक दिखाने के लिए।
• माइक्रोफ़ोन: बटन दबाने तक रिकॉर्ड करने के लिए।
• यूसेज एक्सेस: यह जानने के लिए कि WhatsApp खुला है, ताकि माइक सिर्फ़ वहीं दिखे।
• एक्सेसिबिलिटी सेवा: सिर्फ़ वॉइस नोट तैयार होने के बाद WhatsApp का Send बटन दबाने (और वही चैट चुनने) के लिए। यह केवल WhatsApp / WhatsApp Business / Telegram / Instagram में काम करती है; आपके संदेश या निजी डेटा को न पढ़ती है, न सहेजती है, न साझा करती है। चालू करने से पहले स्पष्ट सूचना दिखाई जाती है।
• सूचनाएँ: AuraMic चालू होने पर छोटी स्टेटस सूचना।

★ पहले गोपनीयता
आपकी आवाज़ फ़ोन से बाहर नहीं जाती। हम ऑडियो, संदेश, संपर्क या कोई निजी डेटा एकत्र नहीं करते। अस्थायी फ़ाइलें भेजने के बाद अपने-आप हट जाती हैं।

मदद चाहिए? support@fireclashpro.com

AuraMic AI एक स्वतंत्र ऐप है। इसका WhatsApp LLC या Meta Platforms, Inc. से कोई संबंध, समर्थन या प्रायोजन नहीं है। WhatsApp, WhatsApp LLC का ट्रेडमार्क है। Telegram और Instagram अपने-अपने स्वामियों के ट्रेडमार्क हैं।""",
)

# --------------------------------------------------------------------------------------------
L["es-419"] = dict(
    title="AuraMic AI: Limpia Notas Voz",
    short_description="Micrófono flotante p/ WhatsApp: IA quita el ruido y envía tus notas de voz",
    full_description="""Notas de voz con calidad de estudio, en cualquier lugar. AuraMic AI coloca un micrófono flotante sobre WhatsApp: mantén pulsado, habla y suelta. La IA del propio teléfono elimina el ruido de fondo y la nota de voz se envía por ti.

Olvídate de grabar otra vez por el tráfico, un ventilador, la multitud o una cafetería ruidosa. AuraMic AI es un limpiador de notas de voz y eliminador de ruido de fondo que funciona directamente en tu teléfono.

★ CÓMO FUNCIONA
1. Abre cualquier chat de WhatsApp: aparece el micrófono flotante (y desaparece al salir de WhatsApp).
2. Mantén pulsado el micrófono y habla. Desliza a la izquierda para cancelar.
3. Suelta. AuraMic AI limpia el audio, elige el mismo chat y pulsa Enviar por ti.

★ FUNCIONES
• Supresión de ruido con IA: elimina ruido constante como tráfico, ventiladores, aire acondicionado, zumbidos y viento, y reduce el ruido de multitudes.
• 100 % en el dispositivo: sin nube, sin cuenta, sin subidas. Funciona en modo avión.
• El micrófono flotante solo aparece en WhatsApp y WhatsApp Business.
• Envío automático: una pulsación, una suelta, listo. ¿Prefieres revisar? Desactívalo y elige tú el chat.
• Refuerzo de volumen y limitador: iguala voces bajas y evita la distorsión.
• Laboratorio de estudio: graba una muestra ruidosa y compara el original con el audio limpio.
• Archivos pequeños y compatibles (AAC/M4A, u Opus en Android 10+).
• Arrastra el micrófono a donde quieras; se pega al borde y recuerda su lugar.
• 7 idiomas: English, हिन्दी, Hinglish, Español, Português, العربية, Bahasa Indonesia.

★ LIMPIEZA NIVEL PODCAST
• La IA DeepFilterNet3 (banda completa de 48 kHz) funciona en tu teléfono con ONNX Runtime: aislamiento de voz de estudio, 10–30 veces más rápido que el tiempo real.
• Pulido podcast: ecualizador, de-esser, compresor y masterización a −16 LUFS con limitador.
• Corte automático de silencios: acorta pausas largas y recorta inicio y final (Suave o Ajustado).
• Separa el fondo: escucha el ruido eliminado aparte o conserva algo de ambiente.
• Funciona en WhatsApp, WhatsApp Business y Telegram; Instagram es experimental (la nota se guarda para que la adjuntes).

★ PERMISOS: POR QUÉ LOS PEDIMOS
• Mostrar sobre otras apps: para ver el micrófono flotante sobre WhatsApp.
• Micrófono: para grabar mientras mantienes pulsado el botón.
• Acceso de uso: para saber cuándo WhatsApp está abierto y mostrar el micrófono solo ahí.
• Servicio de accesibilidad: SOLO para pulsar el botón Enviar de WhatsApp (y elegir el chat en el que estabas) cuando tu nota de voz está lista. Funciona únicamente en WhatsApp / WhatsApp Business / Telegram / Instagram y no lee, guarda ni comparte tus mensajes ni datos personales. Verás un aviso claro antes de activarlo.
• Notificaciones: una pequeña notificación mientras AuraMic está activo.

★ PRIVACIDAD ANTE TODO
Tu voz nunca sale del teléfono. No recopilamos audio, mensajes, contactos ni datos personales. Los archivos temporales se eliminan automáticamente tras el envío.

¿Ayuda? support@fireclashpro.com

AuraMic AI es una app independiente. No está afiliada, respaldada ni patrocinada por WhatsApp LLC ni Meta Platforms, Inc. WhatsApp es una marca registrada de WhatsApp LLC. Telegram e Instagram son marcas de sus respectivos propietarios.""",
)

# --------------------------------------------------------------------------------------------
L["pt-BR"] = dict(
    title="AuraMic AI: Limpa Áudios",
    short_description="Microfone flutuante p/ WhatsApp: IA remove ruído e envia seus áudios",
    full_description="""Áudios com qualidade de estúdio, em qualquer lugar. O AuraMic AI coloca um microfone flutuante sobre o WhatsApp: segure, fale e solte. A IA do próprio celular remove o ruído de fundo e o áudio é enviado por você.

Chega de regravar por causa do trânsito, de um ventilador, da multidão ou de um café barulhento. O AuraMic AI é um limpador de mensagens de voz e removedor de ruído de fundo que funciona direto no seu celular.

★ COMO FUNCIONA
1. Abra qualquer conversa do WhatsApp: o microfone flutuante aparece (e some quando você sai do WhatsApp).
2. Segure o microfone e fale. Deslize para a esquerda para cancelar.
3. Solte. O AuraMic AI limpa o áudio, escolhe a mesma conversa e toca em Enviar por você.

★ RECURSOS
• Supressão de ruído com IA: remove ruído constante como trânsito, ventiladores, ar-condicionado, zumbido e vento, e reduz o barulho de multidões.
• 100% no dispositivo: sem nuvem, sem conta, sem upload. Funciona no modo avião.
• O microfone flutuante só aparece no WhatsApp e no WhatsApp Business.
• Envio automático: segurar, soltar, pronto. Prefere revisar? Desative e escolha a conversa você mesmo.
• Reforço de volume e limitador: equilibra vozes baixas e evita distorção.
• Laboratório de estúdio: grave uma amostra barulhenta e compare o original com o áudio limpo.
• Arquivos pequenos e compatíveis (AAC/M4A, ou Opus no Android 10+).
• Arraste o microfone para onde quiser; ele gruda na borda e lembra a posição.
• 7 idiomas: English, हिन्दी, Hinglish, Español, Português, العربية, Bahasa Indonesia.

★ LIMPEZA NÍVEL PODCAST
• A IA DeepFilterNet3 (banda completa de 48 kHz) roda no seu celular com ONNX Runtime: isolamento de voz de estúdio, 10–30 vezes mais rápido que o tempo real.
• Acabamento podcast: equalizador, de-esser, compressor e masterização em −16 LUFS com limitador.
• Corte automático de silêncio: encurta pausas longas e apara início e fim (Suave ou Firme).
• Separe o fundo: ouça o ruído removido à parte ou mantenha um pouco de ambiente.
• Funciona no WhatsApp, WhatsApp Business e Telegram; o Instagram é experimental (o áudio é salvo para você anexar).

★ PERMISSÕES: POR QUE PEDIMOS
• Sobrepor a outros apps: para mostrar o microfone flutuante sobre o WhatsApp.
• Microfone: para gravar enquanto você segura o botão.
• Acesso ao uso: para saber quando o WhatsApp está aberto e mostrar o microfone só lá.
• Serviço de acessibilidade: usado SOMENTE para tocar no botão Enviar do WhatsApp (e escolher a conversa em que você estava) quando o áudio está pronto. Funciona apenas no WhatsApp / WhatsApp Business / Telegram / Instagram e não lê, armazena nem compartilha suas mensagens ou dados pessoais. Você verá um aviso claro antes de ativar.
• Notificações: uma pequena notificação enquanto o AuraMic está ativo.

★ PRIVACIDADE EM PRIMEIRO LUGAR
Sua voz nunca sai do celular. Não coletamos áudio, mensagens, contatos nem dados pessoais. Arquivos temporários são apagados automaticamente após o envio.

Precisa de ajuda? support@fireclashpro.com

O AuraMic AI é um app independente. Não é afiliado, endossado nem patrocinado pela WhatsApp LLC ou pela Meta Platforms, Inc. WhatsApp é marca registrada da WhatsApp LLC. Telegram e Instagram são marcas de seus respectivos proprietários.""",
)

# --------------------------------------------------------------------------------------------
L["ar"] = dict(
    title="AuraMic AI: منقّي الصوت",
    short_description="ميكروفون عائم لواتساب: ذكاء اصطناعي يزيل الضوضاء ويرسل رسائلك الصوتية تلقائيًا",
    full_description="""رسائل صوتية بجودة الاستوديو في أي مكان. يضع AuraMic AI ميكروفونًا عائمًا فوق واتساب: اضغط مطولًا وتحدّث ثم ارفع إصبعك — يزيل الذكاء الاصطناعي على جهازك ضوضاء الخلفية ويُرسل الرسالة الصوتية نيابةً عنك.

لا مزيد من إعادة التسجيل بسبب الزحام أو المراوح أو الأصوات العالية في المقهى. AuraMic AI منقّي للرسائل الصوتية ومزيل لضوضاء الخلفية يعمل مباشرةً على هاتفك.

★ كيف يعمل
١. افتح أي محادثة في واتساب — يظهر الميكروفون العائم (ويختفي عند خروجك من واتساب).
٢. اضغط مطولًا على الميكروفون وتحدّث. اسحب لليسار للإلغاء.
٣. ارفع إصبعك. ينظّف AuraMic AI الصوت ويختار المحادثة نفسها ويضغط «إرسال» نيابةً عنك.

★ المزايا
• إزالة الضوضاء بالذكاء الاصطناعي: تزيل الضوضاء المستمرة مثل السيارات والمراوح والتكييف والطنين والرياح، وتخفّف ضوضاء الزحام.
• معالجة ١٠٠٪ على الجهاز: بلا سحابة ولا حساب ولا رفع للملفات. يعمل في وضع الطيران.
• لا يظهر الميكروفون العائم إلا في واتساب وواتساب للأعمال.
• إرسال تلقائي: اضغط ثم ارفع وانتهى. تفضّل المراجعة؟ عطّل الإرسال التلقائي واختر المحادثة بنفسك.
• تعزيز مستوى الصوت: يوازن الأصوات الخافتة ويمنع التشويه.
• مختبر الاستوديو: سجّل عيّنة صاخبة وقارن بين الأصلي والمنقّى.
• ملفات صغيرة ومتوافقة (AAC/M4A أو Opus على أندرويد ١٠+).
• اسحب الميكروفون إلى أي مكان؛ يلتصق بالحافة ويتذكّر موضعه.
• ٧ لغات: English، हिन्दी، Hinglish، Español، Português، العربية، Bahasa Indonesia.

★ تنظيف بمستوى البودكاست
• يعمل الذكاء الاصطناعي DeepFilterNet3 (نطاق كامل 48 kHz) على هاتفك عبر ONNX Runtime: عزل صوت بجودة الاستوديو وأسرع من الزمن الحقيقي بـ10–30 مرة.
• لمسة بودكاست: موازنة وإزالة للصفير وضغط وضبط الجهارة عند −16 LUFS مع محدّد.
• قصّ الصمت تلقائيًا: يقصّر الوقفات الطويلة ويقتطع البداية والنهاية (خفيف أو مشدّد).
• افصل الخلفية: استمع إلى الضوضاء المُزالة وحدها أو احتفظ ببعض الأجواء.
• يعمل في واتساب وواتساب للأعمال وتيليجرام؛ إنستغرام تجريبي (تُحفظ الرسالة لتقوم بإرفاقها).

★ الأذونات — لماذا نطلبها
• العرض فوق التطبيقات الأخرى: لإظهار الميكروفون العائم فوق واتساب.
• الميكروفون: للتسجيل أثناء الضغط على الزر.
• الوصول إلى الاستخدام: لمعرفة متى يكون واتساب مفتوحًا فيظهر الميكروفون هناك فقط.
• خدمة إمكانية الوصول: تُستخدم فقط للضغط على زر «إرسال» في واتساب (واختيار المحادثة التي كنت فيها) بعد جاهزية رسالتك الصوتية. تعمل داخل واتساب / واتساب للأعمال فقط، ولا تقرأ رسائلك أو بياناتك الشخصية ولا تخزّنها أو تشاركها. سيظهر لك إفصاح واضح قبل التفعيل.
• الإشعارات: إشعار صغير أثناء تشغيل AuraMic.

★ الخصوصية أولًا
صوتك لا يغادر هاتفك أبدًا. لا نجمع الصوت أو الرسائل أو جهات الاتصال أو أي بيانات شخصية. تُحذف الملفات المؤقتة تلقائيًا بعد الإرسال.

هل تحتاج مساعدة؟ support@fireclashpro.com

AuraMic AI تطبيق مستقل وليس تابعًا لشركة WhatsApp LLC أو Meta Platforms, Inc. ولا مدعومًا أو برعايتها. واتساب علامة تجارية لشركة WhatsApp LLC. تيليجرام وإنستغرام علامتان تجاريتان لأصحابهما.""",
)

# --------------------------------------------------------------------------------------------
L["id"] = dict(
    title="AuraMic AI: Pembersih Suara",
    short_description="Mikrofon melayang WhatsApp: AI hapus derau & kirim pesan suara otomatis",
    full_description="""Pesan suara berkualitas studio, di mana saja. AuraMic AI menempatkan mikrofon melayang di atas WhatsApp: tahan, bicara, lepaskan — AI di ponsel Anda menghapus derau latar dan pesan suara dikirim untuk Anda.

Tak perlu merekam ulang karena lalu lintas, kipas angin, keramaian, atau kafe yang bising. AuraMic AI adalah pembersih pesan suara dan penghapus derau latar yang bekerja langsung di ponsel Anda.

★ CARA KERJA
1. Buka obrolan WhatsApp mana pun — mikrofon melayang muncul (dan hilang saat Anda keluar dari WhatsApp).
2. Tekan dan tahan mikrofon lalu bicara. Geser ke kiri untuk membatalkan.
3. Lepaskan. AuraMic AI membersihkan audio, memilih obrolan yang sama, dan menekan Kirim untuk Anda.

★ FITUR
• Peredam derau AI: menghapus derau stabil seperti lalu lintas, kipas, AC, dengung, dan angin, serta mengurangi bising keramaian.
• 100% di perangkat: tanpa cloud, tanpa akun, tanpa unggahan. Berfungsi dalam mode pesawat.
• Mikrofon melayang hanya muncul di WhatsApp dan WhatsApp Business.
• Kirim otomatis: tahan, lepas, selesai. Ingin memeriksa dulu? Matikan kirim otomatis dan pilih obrolan sendiri.
• Penguat volume: menyeimbangkan suara pelan dan mencegah distorsi.
• Lab Studio: rekam sampel yang bising dan bandingkan audio asli dengan yang sudah bersih.
• Berkas kecil yang ramah WhatsApp (AAC/M4A, atau Opus di Android 10+).
• Seret mikrofon ke mana saja; menempel ke tepi dan mengingat posisinya.
• 7 bahasa: English, हिन्दी, Hinglish, Español, Português, العربية, Bahasa Indonesia.

★ PEMBERSIHAN KELAS PODCAST
• AI DeepFilterNet3 (full-band 48 kHz) berjalan di ponsel lewat ONNX Runtime: isolasi suara kualitas studio, 10–30 kali lebih cepat dari waktu nyata.
• Poles podcast: EQ, de-esser, kompresor, dan mastering −16 LUFS dengan limiter.
• Potong hening otomatis: persingkat jeda panjang dan pangkas awal serta akhir (Lembut atau Ketat).
• Pisahkan latar: dengarkan derau yang dihapus secara terpisah atau pertahankan sedikit suasana.
• Berfungsi di WhatsApp, WhatsApp Business, dan Telegram; Instagram eksperimental (pesan disimpan untuk Anda lampirkan).

★ IZIN — MENGAPA KAMI MEMINTANYA
• Tampil di atas aplikasi lain: untuk menampilkan mikrofon melayang di atas WhatsApp.
• Mikrofon: untuk merekam selama Anda menahan tombol.
• Akses penggunaan: untuk mengetahui kapan WhatsApp terbuka agar mikrofon hanya muncul di sana.
• Layanan Aksesibilitas: HANYA untuk menekan tombol Kirim di WhatsApp (dan memilih obrolan tempat Anda berada) setelah pesan suara siap. Hanya bekerja di WhatsApp / WhatsApp Business / Telegram / Instagram, dan tidak membaca, menyimpan, atau membagikan pesan maupun data pribadi Anda. Anda akan melihat pengungkapan yang jelas sebelum mengaktifkannya.
• Notifikasi: notifikasi status kecil saat AuraMic aktif.

★ PRIVASI NOMOR SATU
Suara Anda tidak pernah meninggalkan ponsel. Kami tidak mengumpulkan audio, pesan, kontak, atau data pribadi. Berkas sementara dihapus otomatis setelah pengiriman.

Butuh bantuan? support@fireclashpro.com

AuraMic AI adalah aplikasi independen. Tidak berafiliasi, didukung, atau disponsori oleh WhatsApp LLC maupun Meta Platforms, Inc. WhatsApp adalah merek dagang WhatsApp LLC. Telegram dan Instagram adalah merek dagang pemiliknya masing-masing.""",
)


def main():
    ok = True
    for loc, d in L.items():
        folder = ROOT / loc
        folder.mkdir(exist_ok=True)
        for key, limit in LIMITS.items():
            text = d[key].strip()
            n = len(text)
            status = "ok" if n <= limit else "TOO LONG"
            if n > limit:
                ok = False
            print(f"{loc:6s} {key:18s} {n:5d}/{limit}  {status}")
            (folder / f"{key}.txt").write_text(text + "\n", encoding="utf-8")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
