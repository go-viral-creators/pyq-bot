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
                 "WhatsApp LLC or Meta Platforms, Inc. WhatsApp is a trademark of WhatsApp LLC.")

L = {}

# --------------------------------------------------------------------------------------------
L["en-US"] = dict(
    title="AuraMic AI: Voice Note Cleaner",
    short_description="Voice note cleaner for WhatsApp: removes noise, sends for you. Free + Pro",
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
• Tiny, WhatsApp-friendly files (AAC/M4A).
• Drag the mic anywhere; it snaps to the edge and remembers its place.
• Haptic feedback, smooth animations, light and dark themes.
• Quick Settings tile to switch AuraMic on or off in one tap.
• 7 languages: English, हिन्दी, Hinglish, Español, Português, العربية, Bahasa Indonesia.

★ TWO QUALITY LEVELS
• Medium Quality (free): fast and light. Removes most background noise such as fans, traffic and room echo and keeps your voice clear.
• High Quality (AuraMic Pro): studio-clean sound. Removes almost all background noise and makes your voice rich, balanced and loud enough. Try it with 10 free voice notes; after that it is a monthly or yearly subscription you can cancel any time in Google Play.
• Long pauses (over 0.3 s) are cut automatically (switch it off in Settings) and the volume is evened out.
• Works in WhatsApp and WhatsApp Business. Inside a chat the AuraMic mic sits on WhatsApp's own voice button and works the same way: hold to record, slide left to cancel.

★ PERMISSIONS — WHY WE ASK
• Display over other apps: to show the floating mic on top of WhatsApp.
• Microphone: to record while you hold the button.
• Usage access: to know when WhatsApp is open, so the mic only appears there.
• Accessibility service: used ONLY to tap WhatsApp's Send button (and choose the chat you were in) after your voice note is ready. It works only inside WhatsApp / WhatsApp Business and does not read, store or share your messages or personal data. You will see a clear disclosure before enabling it.
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
• Chhoti, WhatsApp-friendly files (AAC/M4A).
• Mic ko kahin bhi drag karo; yeh kinare se chipak jata hai aur jagah yaad rakhta hai.
• Haptic feedback, smooth animation, light aur dark theme.
• 7 bhasha: English, हिन्दी, Hinglish, Español, Português, العربية, Bahasa Indonesia.

★ DO QUALITY LEVELS
• Medium Quality (free): tez aur halka. Pankha, traffic aur kamre ki goonj jaisa zyada tar shor hata deta hai, aawaz saaf rehti hai.
• High Quality (AuraMic Pro): studio jaisi saaf awaaz. Lagbhag sara shor hata deta hai aur aawaz ko bhari-puri, balanced aur kaafi tez banata hai. 10 voice notes free try karo; uske baad monthly ya yearly subscription, Google Play me kabhi bhi cancel.
• Lambi chuppi (0.3 second se zyada) apne-aap kat jaati hai (Settings me band kar sakte ho) aur volume barabar hota hai.
• WhatsApp aur WhatsApp Business me chalta hai. Chat ke andar AuraMic ka mic WhatsApp ke apne voice button par baithta hai aur waise hi chalta hai: dabake record, left slide par cancel.

★ PERMISSIONS — KYUN MAANGTE HAIN
• Display over other apps: WhatsApp ke upar floating mic dikhane ke liye.
• Microphone: button dabane tak record karne ke liye.
• Usage access: pata karne ke liye ki WhatsApp khula hai, taaki mic sirf wahin dikhe.
• Accessibility service: SIRF voice note taiyaar hone ke baad WhatsApp ka Send button dabane (aur wahi chat chunne) ke liye. Yeh sirf WhatsApp / WhatsApp Business mein kaam karti hai; aapke messages ya personal data ko na padhti hai, na save karti hai, na share karti hai. Enable karne se pehle saaf disclosure dikhaya jata hai.
• Notifications: AuraMic on hone par chhota status notification.

★ PRIVACY PEHLE
Aapki awaaz phone se bahar nahi jati. Hum audio, messages, contacts ya koi personal data collect nahi karte. Temporary files send ke baad apne aap delete ho jati hain.

Madad chahiye? support@fireclashpro.com

AuraMic AI ek independent app hai. Ismein WhatsApp LLC ya Meta Platforms, Inc. ka koi affiliation, endorsement ya sponsorship nahi hai. WhatsApp, WhatsApp LLC ka trademark hai.""",
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
• छोटी, WhatsApp-फ्रेंडली फ़ाइलें (AAC/M4A)।
• 7 भाषाएँ: English, हिन्दी, Hinglish, Español, Português, العربية, Bahasa Indonesia।

★ दो क्वालिटी स्तर
• मीडियम क्वालिटी (फ्री): तेज़ और हल्की। पंखे, ट्रैफिक और कमरे की गूँज जैसा ज़्यादातर शोर हटाती है और आवाज़ साफ़ रखती है।
• हाई क्वालिटी (AuraMic Pro): स्टूडियो जैसी साफ़ आवाज़। लगभग सारा शोर हटाती है और आवाज़ को भरी-पूरी और संतुलित बनाती है। 10 वॉइस नोट फ्री आज़माएँ; उसके बाद मासिक या वार्षिक सब्सक्रिप्शन, Google Play में कभी भी रद्द करें।
• स्मार्ट रूम फ़िक्स: रिकॉर्डिंग में इको या रीवर्ब हो तो कम करता है; न हो तो कुछ नहीं बदलता।
• 0.3 सेकंड से लंबी चुप्पी अपने-आप कट जाती है (सेटिंग्स में बंद कर सकते हैं) और वॉल्यूम बराबर रहता है।
• WhatsApp और WhatsApp Business में काम करता है। चैट के अंदर AuraMic का माइक WhatsApp के अपने वॉइस बटन पर बैठता है: दबाकर रिकॉर्ड, बाईं ओर स्लाइड करके रद्द।

★ अनुमतियाँ — हम क्यों माँगते हैं
• अन्य ऐप्स के ऊपर दिखाना: WhatsApp के ऊपर फ्लोटिंग माइक दिखाने के लिए।
• माइक्रोफ़ोन: बटन दबाने तक रिकॉर्ड करने के लिए।
• यूसेज एक्सेस: यह जानने के लिए कि WhatsApp खुला है, ताकि माइक सिर्फ़ वहीं दिखे।
• एक्सेसिबिलिटी सेवा: सिर्फ़ वॉइस नोट तैयार होने के बाद WhatsApp का Send बटन दबाने (और वही चैट चुनने) के लिए। यह केवल WhatsApp / WhatsApp Business में काम करती है; आपके संदेश या निजी डेटा को न पढ़ती है, न सहेजती है, न साझा करती है। चालू करने से पहले स्पष्ट सूचना दिखाई जाती है।
• सूचनाएँ: AuraMic चालू होने पर छोटी स्टेटस सूचना।

★ पहले गोपनीयता
आपकी आवाज़ फ़ोन से बाहर नहीं जाती। हम ऑडियो, संदेश, संपर्क या कोई निजी डेटा एकत्र नहीं करते। अस्थायी फ़ाइलें भेजने के बाद अपने-आप हट जाती हैं।

मदद चाहिए? support@fireclashpro.com

AuraMic AI एक स्वतंत्र ऐप है। इसका WhatsApp LLC या Meta Platforms, Inc. से कोई संबंध, समर्थन या प्रायोजन नहीं है। WhatsApp, WhatsApp LLC का ट्रेडमार्क है।""",
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
• Archivos pequeños y compatibles (AAC/M4A).
• Arrastra el micrófono a donde quieras; se pega al borde y recuerda su lugar.
• 7 idiomas: English, हिन्दी, Hinglish, Español, Português, العربية, Bahasa Indonesia.

★ DOS NIVELES DE CALIDAD
• Calidad media (gratis): rápida y ligera. Quita la mayor parte del ruido de fondo, como ventiladores, tráfico y eco de la habitación, y mantiene tu voz clara.
• Calidad alta (AuraMic Pro): sonido limpio de estudio. Elimina casi todo el ruido y deja tu voz rica, equilibrada y con buen volumen. Pruébala con 10 notas de voz gratis; después es una suscripción mensual o anual que puedes cancelar cuando quieras en Google Play.
• Las pausas largas (más de 0,3 s) se cortan automáticamente (se desactiva en Ajustes) y el volumen se nivela.
• Funciona en WhatsApp y WhatsApp Business. Dentro de un chat el micrófono de AuraMic se coloca sobre el botón de voz de WhatsApp y funciona igual: mantén para grabar, desliza a la izquierda para cancelar.

★ PERMISOS: POR QUÉ LOS PEDIMOS
• Mostrar sobre otras apps: para ver el micrófono flotante sobre WhatsApp.
• Micrófono: para grabar mientras mantienes pulsado el botón.
• Acceso de uso: para saber cuándo WhatsApp está abierto y mostrar el micrófono solo ahí.
• Servicio de accesibilidad: SOLO para pulsar el botón Enviar de WhatsApp (y elegir el chat en el que estabas) cuando tu nota de voz está lista. Funciona únicamente en WhatsApp / WhatsApp Business y no lee, guarda ni comparte tus mensajes ni datos personales. Verás un aviso claro antes de activarlo.
• Notificaciones: una pequeña notificación mientras AuraMic está activo.

★ PRIVACIDAD ANTE TODO
Tu voz nunca sale del teléfono. No recopilamos audio, mensajes, contactos ni datos personales. Los archivos temporales se eliminan automáticamente tras el envío.

¿Ayuda? support@fireclashpro.com

AuraMic AI es una app independiente. No está afiliada, respaldada ni patrocinada por WhatsApp LLC ni Meta Platforms, Inc. WhatsApp es una marca registrada de WhatsApp LLC.""",
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
• Arquivos pequenos e compatíveis (AAC/M4A).
• Arraste o microfone para onde quiser; ele gruda na borda e lembra a posição.
• 7 idiomas: English, हिन्दी, Hinglish, Español, Português, العربية, Bahasa Indonesia.

★ DOIS NÍVEIS DE QUALIDADE
• Qualidade média (grátis): rápida e leve. Remove a maior parte do ruído de fundo, como ventiladores, trânsito e eco do ambiente, e mantém sua voz clara.
• Qualidade alta (AuraMic Pro): som limpo de estúdio. Remove quase todo o ruído e deixa sua voz rica, equilibrada e com bom volume. Experimente com 10 áudios grátis; depois é uma assinatura mensal ou anual que você cancela quando quiser no Google Play.
• Pausas longas (mais de 0,3 s) são cortadas automaticamente (desative em Configurações) e o volume é nivelado.
• Funciona no WhatsApp e no WhatsApp Business. Dentro de uma conversa o microfone do AuraMic fica sobre o botão de voz do WhatsApp e funciona igual: segure para gravar, deslize para a esquerda para cancelar.

★ PERMISSÕES: POR QUE PEDIMOS
• Sobrepor a outros apps: para mostrar o microfone flutuante sobre o WhatsApp.
• Microfone: para gravar enquanto você segura o botão.
• Acesso ao uso: para saber quando o WhatsApp está aberto e mostrar o microfone só lá.
• Serviço de acessibilidade: usado SOMENTE para tocar no botão Enviar do WhatsApp (e escolher a conversa em que você estava) quando o áudio está pronto. Funciona apenas no WhatsApp / WhatsApp Business e não lê, armazena nem compartilha suas mensagens ou dados pessoais. Você verá um aviso claro antes de ativar.
• Notificações: uma pequena notificação enquanto o AuraMic está ativo.

★ PRIVACIDADE EM PRIMEIRO LUGAR
Sua voz nunca sai do celular. Não coletamos áudio, mensagens, contatos nem dados pessoais. Arquivos temporários são apagados automaticamente após o envio.

Precisa de ajuda? support@fireclashpro.com

O AuraMic AI é um app independente. Não é afiliado, endossado nem patrocinado pela WhatsApp LLC ou pela Meta Platforms, Inc. WhatsApp é marca registrada da WhatsApp LLC.""",
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
• ملفات صغيرة ومتوافقة (AAC/M4A).
• اسحب الميكروفون إلى أي مكان؛ يلتصق بالحافة ويتذكّر موضعه.
• ٧ لغات: English، हिन्दी، Hinglish، Español، Português، العربية، Bahasa Indonesia.

★ مستويان للجودة
• جودة متوسطة (مجانية): سريعة وخفيفة. تزيل معظم ضجيج الخلفية مثل المراوح والسيارات وصدى الغرفة وتُبقي صوتك واضحًا.
• جودة عالية (AuraMic Pro): صوت نقي بجودة الاستوديو. تزيل الضجيج تقريبًا بالكامل وتجعل صوتك غنيًا ومتوازنًا. جرّبها مع 10 رسائل صوتية مجانية؛ بعدها اشتراك شهري أو سنوي يمكنك إلغاؤه في أي وقت من Google Play.
• إصلاح ذكي للغرفة: إذا كان في التسجيل صدى أو ارتداد صوتي يتم تقليله، وإذا لم يكن فلا يتغير شيء.
• تُقطع الفترات الصامتة الطويلة (أكثر من 0.3 ثانية) تلقائيًا (يمكن إيقافها من الإعدادات) ويتساوى مستوى الصوت.
• يعمل في واتساب وواتساب للأعمال. داخل المحادثة يظهر ميكروفون AuraMic فوق زر الصوت في واتساب ويعمل مثله: اضغط مطولًا للتسجيل واسحب لليسار للإلغاء.

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
• Berkas kecil yang ramah WhatsApp (AAC/M4A).
• Seret mikrofon ke mana saja; menempel ke tepi dan mengingat posisinya.
• 7 bahasa: English, हिन्दी, Hinglish, Español, Português, العربية, Bahasa Indonesia.

★ DUA TINGKAT KUALITAS
• Kualitas Sedang (gratis): cepat dan ringan. Menghilangkan sebagian besar bising latar seperti kipas, lalu lintas, dan gema ruangan, dan menjaga suara tetap jernih.
• Kualitas Tinggi (AuraMic Pro): suara bersih ala studio. Menghilangkan hampir semua bising dan membuat suara kaya serta seimbang. Coba dengan 10 pesan suara gratis; setelah itu berlangganan bulanan atau tahunan yang bisa dibatalkan kapan saja di Google Play.
• Perbaikan ruangan pintar: jika rekaman memiliki gema atau gaung, akan dikurangi; jika tidak ada, tidak ada yang diubah.
• Jeda panjang (lebih dari 0,3 detik) dipotong otomatis (bisa dimatikan di Pengaturan) dan volume diratakan.
• Berfungsi di WhatsApp dan WhatsApp Business. Di dalam obrolan, mikrofon AuraMic berada di atas tombol suara WhatsApp dan bekerja sama: tahan untuk merekam, geser ke kiri untuk membatalkan.

★ IZIN — MENGAPA KAMI MEMINTANYA
• Tampil di atas aplikasi lain: untuk menampilkan mikrofon melayang di atas WhatsApp.
• Mikrofon: untuk merekam selama Anda menahan tombol.
• Akses penggunaan: untuk mengetahui kapan WhatsApp terbuka agar mikrofon hanya muncul di sana.
• Layanan Aksesibilitas: HANYA untuk menekan tombol Kirim di WhatsApp (dan memilih obrolan tempat Anda berada) setelah pesan suara siap. Hanya bekerja di WhatsApp / WhatsApp Business, dan tidak membaca, menyimpan, atau membagikan pesan maupun data pribadi Anda. Anda akan melihat pengungkapan yang jelas sebelum mengaktifkannya.
• Notifikasi: notifikasi status kecil saat AuraMic aktif.

★ PRIVASI NOMOR SATU
Suara Anda tidak pernah meninggalkan ponsel. Kami tidak mengumpulkan audio, pesan, kontak, atau data pribadi. Berkas sementara dihapus otomatis setelah pengiriman.

Butuh bantuan? support@fireclashpro.com

AuraMic AI adalah aplikasi independen. Tidak berafiliasi, didukung, atau disponsori oleh WhatsApp LLC maupun Meta Platforms, Inc. WhatsApp adalah merek dagang WhatsApp LLC.""",
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
