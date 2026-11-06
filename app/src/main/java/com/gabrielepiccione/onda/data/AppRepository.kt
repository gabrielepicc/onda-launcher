// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.gabrielepiccione.onda.core.icons.IconPacks
import com.gabrielepiccione.onda.core.icons.IconPackCache
import com.gabrielepiccione.onda.core.icons.IconRenderer
import com.gabrielepiccione.onda.core.icons.IconShape
import com.gabrielepiccione.onda.core.icons.IconStyle

/**
 * Elenco delle app avviabili per tutti i profili (personale + lavoro),
 * aggiornato su installazioni/rimozioni e quando cambia l'icon pack.
 */
class AppRepository(
    private val context: Context,
    private val settings: StateFlow<Settings>,
) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loadJob: Job? = null

    private val _apps = MutableStateFlow<List<RawApp>>(emptyList())
    val apps: StateFlow<List<RawApp>> = _apps.asStateFlow()

    private val densityDpi = context.resources.displayMetrics.densityDpi
    private val iconSizePx: Int = (56 * context.resources.displayMetrics.density).toInt().coerceAtLeast(56)

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) = reload()
        override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) = reload()
        override fun onShortcutsChanged(packageName: String, shortcuts: MutableList<ShortcutInfo>, user: UserHandle) = refreshPinned()
    }

    /** Scorciatoia fissata in home, così com'è adesso nel sistema. */
    data class PinnedInfo(val shortcut: ShortcutInfo, val label: String, val icon: ImageBitmap?)

    private val _pinned = MutableStateFlow<Map<String, PinnedInfo>>(emptyMap())
    val pinned: StateFlow<Map<String, PinnedInfo>> = _pinned.asStateFlow()

    /** Rilegge dal sistema le scorciatoie della home; quelle non più valide spariscono da sole. */
    fun refreshPinned() {
        scope.launch {
            val result = HashMap<String, PinnedInfo>()
            settings.value.pinnedShortcuts.groupBy { it.packageName to it.userSerial }.forEach { (owner, entries) ->
                val user = runCatching { userManager.getUserForSerialNumber(owner.second) }.getOrNull() ?: return@forEach
                val query = LauncherApps.ShortcutQuery().setPackage(owner.first).setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                val live = runCatching { launcherApps.getShortcuts(query, user) }.getOrNull().orEmpty().associateBy { it.id }
                entries.forEach { entry ->
                    val info = live[entry.shortcutId] ?: return@forEach
                    result[entry.key] = PinnedInfo(
                        shortcut = info,
                        label = (info.shortLabel ?: info.longLabel)?.toString() ?: entry.label,
                        icon = shortcutIcon(info, iconSizePx),
                    )
                }
            }
            _pinned.value = result
        }
    }

    /** Toglie la scorciatoia dal sistema: [remainingIds] sono quelle da tenere per quell'app. */
    fun unpinShortcut(packageName: String, userSerial: Long, remainingIds: List<String>) {
        val user = runCatching { userManager.getUserForSerialNumber(userSerial) }.getOrNull() ?: return
        runCatching { launcherApps.pinShortcuts(packageName, remainingIds, user) }
    }

    /** Pack caricati, condivisi con il selettore di icone. */
    val packs = IconPackCache(context)

    private val knownCategories by lazy { AppClassifier.loadKnown(context) }
    private val colorCache = HashMap<String, Int>()

    // Cache in memoria delle icone già elaborate: chiave = app + parametri di resa
    private val iconCache = android.util.LruCache<String, ImageBitmap>(600)

    private data class IconParams(val pack: String, val shape: String, val style: String, val overrides: Map<String, String>, val shapes: Map<String, String>)
    private fun Settings.iconParams() = IconParams(iconPack, iconShape, iconStyle, iconOverrides, iconShapeOverrides)

    /** Stato dello Spazio privato; null = assente (o Android precedente al 15). */
    data class PrivateSpace(val user: UserHandle, val locked: Boolean)

    private val _privateSpace = MutableStateFlow<PrivateSpace?>(null)
    val privateSpace: StateFlow<PrivateSpace?> = _privateSpace.asStateFlow()

    private fun isPrivateProfile(user: UserHandle): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
            runCatching { launcherApps.getLauncherUserInfo(user)?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE }.getOrDefault(false)

    /** Blocca (true) o sblocca (false) lo Spazio privato; lo sblocco chiede l'autenticazione di sistema. */
    fun setPrivateSpaceLocked(locked: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val user = _privateSpace.value?.user ?: return
        runCatching { userManager.requestQuietModeEnabled(locked, user) }
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Blocco e sblocco dello Spazio privato: le sue app compaiono o spariscono subito
            val filter = android.content.IntentFilter().apply {
                addAction(android.content.Intent.ACTION_PROFILE_AVAILABLE)
                addAction(android.content.Intent.ACTION_PROFILE_UNAVAILABLE)
            }
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                object : android.content.BroadcastReceiver() {
                    override fun onReceive(c: Context, intent: android.content.Intent) = reload()
                },
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        reload()
        scope.launch {
            settings.map { it.iconParams() }.distinctUntilChanged().drop(1).collect { reload() }
        }
        scope.launch {
            settings.map { it.pinnedShortcuts }.distinctUntilChanged().collect { refreshPinned() }
        }
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = scope.launch {
            val pack = packs.get(settings.value.iconPack)
            val myUser = Process.myUserHandle()
            val result = mutableListOf<RawApp>()
            var privateSpace: PrivateSpace? = null
            for (user in launcherApps.profiles) {
                val isPrivate = isPrivateProfile(user)
                if (isPrivate) {
                    val locked = runCatching { userManager.isQuietModeEnabled(user) }.getOrDefault(true)
                    privateSpace = PrivateSpace(user, locked)
                    // Da bloccato nessuna app dello Spazio privato deve restare visibile o trovabile
                    if (locked) continue
                }
                val activities = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
                for (info in activities) {
                    if (info.componentName.packageName == context.packageName) continue
                    val app = runCatching { info.toRawApp(user, !isPrivate && user != myUser, pack) }.getOrNull() ?: continue
                    result += app.copy(isPrivate = isPrivate)
                }
            }
            _privateSpace.value = privateSpace
            _apps.value = result
        }
    }

    private fun LauncherActivityInfo.toRawApp(user: UserHandle, work: Boolean, pack: IconPacks.Loaded?): RawApp {
        val serial = userManager.getSerialNumberForUser(user)
        val key = "${componentName.flattenToShortString()}#$serial"
        val s = settings.value
        val cacheKey = "$key|${s.iconPack}|${s.iconShape}|${s.iconStyle}|${s.iconOverrides[key]}|${s.iconShapeOverrides[key]}"
        val base by lazy { getIcon(densityDpi) }

        val icon = iconCache.get(cacheKey) ?: buildIcon(key, componentName.packageName, base, pack, s).let { bmp ->
            val finalBmp = if (work) {
                context.packageManager.getUserBadgedIcon(android.graphics.drawable.BitmapDrawable(context.resources, bmp), user)
                    .toBitmap(iconSizePx, iconSizePx)
            } else bmp
            finalBmp.asImageBitmap().also { iconCache.put(cacheKey, it) }
        }
        val usesOriginal = s.iconOverrides[key] == null && (pack == null || pack.iconFor(componentName, densityDpi) == null)
        val appLabel = label?.toString()?.trim().orEmpty()
        val category = AppClassifier.classify(componentName.packageName, appLabel, applicationInfo.category, knownCategories)
        val color = synchronized(colorCache) {
            colorCache.getOrPut(key) { IconRenderer.dominantColor(base.toBitmap(48, 48)) }
        }
        return RawApp(
            key = key,
            component = componentName,
            user = user,
            label = label?.toString()?.trim().orEmpty().ifEmpty { componentName.packageName },
            icon = icon,
            mono = if (usesOriginal) monochromeOf(base) else null,
            isWorkProfile = work,
            category = category,
            color = color,
            installedAt = firstInstallTime,
        )
    }

    /**
     * Pipeline delle icone, in ordine di priorità:
     * 1. icona scelta a mano (immagine o pack) o forma scelta per questa app → 2. stile "Puntini"
     * → 3. icona dedicata del pack → 4. fallback del pack (iconback/mask/upon)
     * → 5. icona originale nella forma scelta.
     */
    private fun LauncherActivityInfo.buildIcon(
        key: String,
        packageName: String,
        base: android.graphics.drawable.Drawable,
        pack: IconPacks.Loaded?,
        s: Settings,
    ): android.graphics.Bitmap {
        val override = s.iconOverrides[key]
        if (override != null && override.startsWith(ICON_FILE_PREFIX)) {
            imageIcon(override.removePrefix(ICON_FILE_PREFIX))?.let { return it }
        }
        override?.split('|')?.takeIf { it.size == 2 }?.let { (pkg, name) ->
            packs.get(pkg)?.drawable(name, densityDpi)?.let { return it.toBitmap(iconSizePx, iconSizePx) }
        }
        val ownShape = s.iconShapeOverrides[key]
        if (ownShape == null) {
            val style = runCatching { IconStyle.valueOf(s.iconStyle) }.getOrDefault(IconStyle.ORIGINAL)
            if (style == IconStyle.DOTS) return IconRenderer.dot(base, iconSizePx)
            pack?.iconFor(componentName, densityDpi)?.let { return it.toBitmap(iconSizePx, iconSizePx) }
            pack?.fallbackParts(packageName, densityDpi)?.let { (back, mask, upon) ->
                return IconRenderer.packFallback(base, back, mask, upon, pack.filter.scale, iconSizePx)
            }
        }
        val shape = runCatching { IconShape.valueOf(ownShape ?: s.iconShape) }.getOrDefault(IconShape.SYSTEM)
        return IconRenderer.render(base, shape, iconSizePx)
    }

    /** Immagine scelta dall'utente: ritaglio quadrato al centro, con gli angoli arrotondati. */
    private fun imageIcon(path: String): Bitmap? {
        val source = android.graphics.BitmapFactory.decodeFile(path) ?: return null
        val side = minOf(source.width, source.height)
        val srcRect = Rect((source.width - side) / 2, (source.height - side) / 2, (source.width + side) / 2, (source.height + side) / 2)
        val out = Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888)
        val dst = android.graphics.RectF(0f, 0f, iconSizePx.toFloat(), iconSizePx.toFloat())
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
        val canvas = Canvas(out)
        canvas.drawRoundRect(dst, iconSizePx * 0.25f, iconSizePx * 0.25f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, srcRect, dst, paint)
        return out
    }

    /** Estrae il livello monocromatico di un'icona adattiva (Android 13+) come maschera alpha. */
    private fun monochromeOf(d: android.graphics.drawable.Drawable): ImageBitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val adaptive = d as? AdaptiveIconDrawable ?: return null
        val mono = adaptive.monochrome ?: return null
        val size = iconSizePx
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        // I livelli adattivi sono il 150% dell'area visibile: centriamo la parte utile
        val inset = size / 4
        val old = Rect(mono.bounds)
        mono.setBounds(-inset, -inset, size + inset, size + inset)
        mono.draw(canvas)
        mono.bounds = old
        return bmp.asImageBitmap()
    }

    // --- Azioni ---

    fun launch(app: AppInfo, bounds: Rect? = null) {
        runCatching { launcherApps.startMainActivity(app.component, app.user, bounds, openOptions()) }
    }

    /** Animazione di apertura scelta dall'utente; null = quella di sistema. */
    private fun openOptions(): android.os.Bundle? {
        val enter = when (settings.value.openAnimation) {
            OpenAnimation.SYSTEM -> return null
            OpenAnimation.FADE -> com.gabrielepiccione.onda.R.anim.onda_open_fade
            OpenAnimation.SCALE -> com.gabrielepiccione.onda.R.anim.onda_open_scale
            OpenAnimation.SLIDE -> com.gabrielepiccione.onda.R.anim.onda_open_slide
        }
        return android.app.ActivityOptions.makeCustomAnimation(context, enter, com.gabrielepiccione.onda.R.anim.onda_stay).toBundle()
    }

    fun openAppInfo(app: AppInfo) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    /** Scorciatoie dell'app (richiede di essere il launcher predefinito). */
    fun shortcutsFor(app: AppInfo): List<ShortcutInfo> {
        if (!runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)) return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setActivity(app.component)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        return runCatching { launcherApps.getShortcuts(query, app.user) }
            .getOrNull()
            .orEmpty()
            .filter { it.isEnabled }
            .sortedBy { it.rank }
            .take(5)
    }

    fun shortcutIcon(shortcut: ShortcutInfo): ImageBitmap? = runCatching {
        launcherApps.getShortcutIconDrawable(shortcut, densityDpi)?.toBitmap(iconSizePx / 2, iconSizePx / 2)?.asImageBitmap()
    }.getOrNull()

    private fun shortcutIcon(shortcut: ShortcutInfo, sizePx: Int): ImageBitmap? = runCatching {
        launcherApps.getShortcutIconDrawable(shortcut, densityDpi)?.toBitmap(sizePx, sizePx)?.asImageBitmap()
    }.getOrNull()

    /** Avvia una scorciatoia scelta in un'azione personalizzata (pacchetto e id, profilo principale). */
    fun startShortcutById(packageName: String, shortcutId: String) {
        runCatching {
            val query = LauncherApps.ShortcutQuery()
                .setPackage(packageName)
                .setShortcutIds(listOf(shortcutId))
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            launcherApps.getShortcuts(query, android.os.Process.myUserHandle())?.firstOrNull()?.let { startShortcut(it) }
        }
    }

    fun startShortcut(shortcut: ShortcutInfo) {
        runCatching { launcherApps.startShortcut(shortcut, null, null) }
    }
}
