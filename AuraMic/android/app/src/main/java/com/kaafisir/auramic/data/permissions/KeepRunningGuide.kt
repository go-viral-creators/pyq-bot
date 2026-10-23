package com.kaafisir.auramic.data.permissions

/**
 * "Keep AuraMic running" help, per phone maker. Many phones close apps in the background (or refuse to start them
 * after a reboot) unless the user allows it in the maker's own settings: Autostart / App launch / background activity /
 * unrestricted battery. Those pages differ per maker and change between versions, so this is best effort:
 *  - [Guide.steps] are always shown (menu names are English: they are the names on the phone's own screens);
 *  - [Guide.autostartTargets] are the makers' Autostart pages we try to open directly, one after the other; the first
 *    that exists wins, and the caller falls back to the app's info page.
 * Pure Kotlin so it is unit tested.
 */
object KeepRunningGuide {
    data class Guide(
        /** Name shown to the user, e.g. "Xiaomi / Redmi / POCO". */
        val family: String,
        val steps: List<String>,
        /** (package, activity) pairs of the maker's Autostart / background pages, most likely first. */
        val autostartTargets: List<Pair<String, String>>,
    )

    private const val LOCK = "Open Recent apps, touch and hold the AuraMic card and tap the lock icon (or Lock this app), so cleaners keep it."
    private const val UNRESTRICTED = "Battery: choose Unrestricted (or No restrictions / Don't optimize) for AuraMic."

    private val XIAOMI = Guide(
        "Xiaomi / Redmi / POCO",
        listOf(
            "Settings → Apps → Manage apps → AuraMic → turn on Autostart.",
            "On the same page: Battery saver → No restrictions.",
            LOCK,
        ),
        listOf("com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity"),
    )
    private val OPPO = Guide(
        "Oppo",
        listOf(
            "Settings → Apps → App management → AuraMic → App info → turn on Auto launch (Allow auto-start).",
            "Same page: Battery usage → turn on Allow background activity.",
            "Settings → Battery → turn off Sleep standby optimisation, and set AuraMic to Don't optimise.",
            LOCK,
        ),
        listOf(
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
        ),
    )
    private val REALME = Guide(
        "Realme",
        listOf(
            "Settings → App management → App list → AuraMic → turn on Auto launch.",
            "Same page: Battery usage → Allow background activity.",
            UNRESTRICTED,
            LOCK,
        ),
        OPPO.autostartTargets,
    )
    private val ONEPLUS = Guide(
        "OnePlus",
        listOf(
            "Settings → Apps → App management → AuraMic → Battery usage → Allow background activity.",
            "Settings → Battery → Battery optimisation → AuraMic → Don't optimise.",
            "If you see Auto launch, turn it on for AuraMic.",
            LOCK,
        ),
        OPPO.autostartTargets,
    )
    private val VIVO = Guide(
        "Vivo / iQOO",
        listOf(
            "Settings → Battery → Background power consumption → AuraMic → Allow (high background power consumption).",
            "iManager (or Settings → Apps) → Autostart → turn on AuraMic.",
            UNRESTRICTED,
            LOCK,
        ),
        listOf(
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
            "com.iqoo.secure" to "com.iqoo.secure.safeguard.PurviewTabActivity",
        ),
    )
    private val SAMSUNG = Guide(
        "Samsung",
        listOf(
            "Settings → Battery (or Battery and device care) → Background usage limits.",
            "Turn off Put unused apps to sleep, then open Never sleeping apps and add AuraMic.",
            "Settings → Apps → AuraMic → Battery → Unrestricted.",
        ),
        listOf(
            "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.BatteryActivity",
            "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity",
            "com.samsung.android.sm_cn" to "com.samsung.android.sm.ui.battery.BatteryActivity",
        ),
    )
    private val HUAWEI = Guide(
        "Huawei / Honor",
        listOf(
            "Settings → Apps → App launch → AuraMic → Manage manually.",
            "Turn on Auto-launch, Secondary launch and Run in background.",
            "Settings → Battery → turn off Power-saving for AuraMic.",
            LOCK,
        ),
        listOf(
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
        ),
    )
    private val TRANSSION = Guide(
        "Tecno / Infinix / itel",
        listOf(
            "Phone Master (or Phone Manager) → Autostart manager → turn on AuraMic.",
            "Settings → Battery → App launch / Battery lab → AuraMic → No restrictions.",
            UNRESTRICTED,
            LOCK,
        ),
        listOf(
            "com.transsion.phonemaster" to "com.cyin.himgr.autostart.AutoStartActivity",
            "com.itel.autobootmanager" to "com.itel.autobootmanager.activity.AutoBootMgrActivity",
        ),
    )
    private val MOTOROLA = Guide(
        "Motorola",
        listOf("Settings → Apps → AuraMic → App battery usage → Unrestricted.", "Settings → Battery → turn off Adaptive battery if the mic still disappears."),
        emptyList(),
    )
    private val ASUS = Guide(
        "Asus",
        listOf("Mobile Manager (or Settings → Battery) → Autostart manager → turn on AuraMic.", UNRESTRICTED),
        listOf("com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity"),
    )
    private val LENOVO = Guide(
        "Lenovo / Motorola ZUI",
        listOf("Settings → Apps → AuraMic → Battery → Don't optimise.", "Security → Background management → allow AuraMic."),
        listOf("com.lenovo.security" to "com.lenovo.security.purebackground.PureBackgroundActivity"),
    )
    private val MEIZU = Guide(
        "Meizu",
        listOf("Security → Permissions → Background management → AuraMic → Allow.", UNRESTRICTED),
        listOf("com.meizu.safe" to "com.meizu.safe.security.SHOW_APPSEC"),
    )
    private val ZTE = Guide(
        "ZTE / Nubia / RedMagic",
        listOf("Settings → Battery → Background apps (or Power saving) → AuraMic → Allow.", UNRESTRICTED, LOCK),
        emptyList(),
    )
    private val LG = Guide(
        "LG",
        listOf("Settings → Battery → Battery optimisation (or Battery saver) → AuraMic → Not optimised.", "If the mic still disappears, turn off Battery saver."),
        emptyList(),
    )
    private val STOCK = Guide(
        "Android",
        listOf(
            "Settings → Apps → AuraMic → App battery usage (or Battery) → Unrestricted.",
            "Settings → Battery → turn off Adaptive battery or Battery saver if the mic still disappears.",
        ),
        emptyList(),
    )
    private val OTHER = Guide(
        "your phone",
        listOf(
            "Settings → Apps → AuraMic → Battery → Unrestricted (or No restrictions / Don't optimise).",
            "Look for Autostart, Auto launch, App launch or Background activity in Settings or the phone's security / manager app, and allow AuraMic.",
            LOCK,
        ),
        emptyList(),
    )

    /** The guide for a phone, from `Build.MANUFACTURER` and `Build.BRAND`. */
    fun forDevice(manufacturer: String?, brand: String?): Guide {
        val id = "${manufacturer.orEmpty()} ${brand.orEmpty()}".lowercase()
        fun has(vararg names: String) = names.any { id.contains(it) }
        return when {
            has("xiaomi", "redmi", "poco", "blackshark") -> XIAOMI
            has("realme") -> REALME
            has("oneplus") -> ONEPLUS
            has("oppo") -> OPPO
            has("vivo", "iqoo") -> VIVO
            has("samsung") -> SAMSUNG
            has("huawei", "honor") -> HUAWEI
            has("tecno", "infinix", "itel", "transsion") -> TRANSSION
            has("motorola", "moto") -> MOTOROLA
            has("asus") -> ASUS
            has("lenovo") -> LENOVO
            has("meizu") -> MEIZU
            has("zte", "nubia", "redmagic") -> ZTE
            has("lge", "lg electronics") -> LG
            has("google", "nothing", "sony", "nokia", "hmd", "fairphone") -> STOCK
            else -> OTHER
        }
    }
}
