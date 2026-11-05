// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.gabrielepiccione.onda.MainActivity
import com.gabrielepiccione.onda.OndaApp
import com.gabrielepiccione.onda.data.BreakerGate
import com.gabrielepiccione.onda.data.UsageReminders
import com.gabrielepiccione.onda.data.UsageStatsMath
import com.gabrielepiccione.onda.data.UsageStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servizio di accessibilità di Onda, usato per due cose:
 * - bloccare lo schermo con il doppio tocco in home;
 * - mostrare la pausa di Usage Breaker (o del limite giornaliero) anche quando un'app si apre fuori
 *   dal launcher, per esempio dalla tendina o dalle app recenti.
 * Legge solo il nome del pacchetto della finestra in primo piano: mai il contenuto dello schermo
 * (`canRetrieveWindowContent` è falso) e niente viene salvato.
 */
class OndaAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val usage by lazy { UsageStatsRepository(this) }

    override fun onServiceConnected() {
        instance = this
        gate = BreakerGate(overlayPackages())
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val settings = (applicationContext as OndaApp).settings.current
        val breaker = settings.breakerApps.map(UsageReminders::packageOf).toSet()
        val limits = settings.appLimits.filterValues { it > 0 }
        if (breaker.isEmpty() && limits.isEmpty()) return
        val hit = gate.onForeground(pkg) { it in breaker || it in limits } ?: return
        if (hit in breaker) {
            showPause(hit)
        } else {
            // Solo limite: la pausa serve se oggi è già stato superato
            val minutes = limits[hit] ?: return
            if (!UsageReminderEngine.hasUsageAccess(this)) return
            scope.launch { if (UsageStatsMath.overLimit(usage.todayMs(hit), minutes)) showPause(hit) }
        }
    }

    override fun onInterrupt() = Unit

    /** Porta in primo piano Onda con la pausa: il servizio di accessibilità può aprire attività da sotto. */
    private fun showPause(pkg: String) {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_PAUSE_PACKAGE, pkg)
        runCatching { startActivity(intent) }
    }

    /** Elementi che compaiono sopra le app senza che si esca da quella aperta: tendina e tastiere. */
    private fun overlayPackages(): Set<String> {
        val keyboards = runCatching {
            getSystemService(InputMethodManager::class.java).enabledInputMethodList.map { it.packageName }
        }.getOrDefault(emptyList())
        return (keyboards + SYSTEM_OVERLAYS).toSet()
    }

    companion object {
        @Volatile
        private var instance: OndaAccessibilityService? = null

        @Volatile
        private var gate = BreakerGate(emptySet())

        private val SYSTEM_OVERLAYS = listOf("com.android.systemui", "android")

        val isEnabled: Boolean get() = instance != null

        fun lockScreen(): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            return instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
        }

        /** La pausa è stata superata da Onda: l'app che si apre ora non ne mostra un'altra. */
        fun allow(packageName: String) = gate.allow(packageName)
    }
}
