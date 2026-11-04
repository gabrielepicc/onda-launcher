# Onda Launcher — istruzioni per Claude

Launcher Android open source (GPL-3.0-or-later), Kotlin + Jetpack Compose, Material You.
Proprietario: Gabriele Piccione. Lingua di lavoro: **italiano** (commenti, commit, changelog).
Ispirato al *comportamento* di Niagara Launcher: si replicano funzioni e gesti, **mai** codice,
nomi, icone o grafica di Niagara.

## Regole non negoziabili
- Niente Google Play Services, Firebase, analytics o pubblicità. Licenze delle dipendenze compatibili con GPLv3.
- Privacy: nessun dato lascia il dispositivo (eccezione futura: meteo Open-Meteo, disattivabile).
- Permessi just-in-time, con spiegazione. Da Android 13 notifiche e accessibilità sono
  "impostazioni con limitazioni" per gli APK: mostrare la guida (`withRestrictedHelp` in HomeScreen).
- Nessun testo scritto nel codice: `stringResource(R.string.…)`, inglese in `values/`, italiano in `values-it/`.
- Ogni file `.kt`/`.kts` inizia con l'intestazione SPDX + copyright.
- Nuove impostazioni: campo con default in `Settings` (data/Models.kt). Se cambia il significato di un
  campo esistente: incrementa `Settings.CURRENT_SCHEMA` e aggiungi il passo in `SettingsCodec.migrate` + test.
- Target touch ≥ 48dp, `contentDescription` sugli elementi senza testo.

## Verifica
Non c'è SDK Android in locale: la verifica è la CI di GitHub Actions (`.github/workflows/build-apk.yml`):
`testDebugUnitTest`, `lintRelease`, `ktlintCheck`, `assembleRelease`. Dopo ogni push attendere la run:
```bash
gh api "repos/gabrielepicc/onda-launcher/actions/runs?branch=main&per_page=1" --jq '.workflow_runs[0] | "\(.id) \(.status) \(.conclusion)"'
# errori: gh api repos/gabrielepicc/onda-launcher/actions/jobs/<job_id>/logs | grep -E " e: file|FAILED|Error:"
```
Lint: evitare `produceState` (falso positivo `ProduceStateDoesNotAssignValue`), usare `LaunchedEffect` + stato.
L'APK release (firmato con `app/onda-dev.keystore`, chiave di sviluppo) è nell'artefatto `OndaLauncher-release`.

## Architettura (vedi docs/architecture.md)
- `:app` — UI in `ui/{theme,components,home,drawer,search,settings,sheets}`, stato in `LauncherViewModel`,
  dati in `data/` (DataStore + `SettingsCodec`), servizi in `service/`.
- `:core:icons` — parser `appfilter.xml`, `IconPacks`/`IconPackCache`, `IconRenderer` (forme, fallback, puntini), `IconMatcher`.
- Gesti righe app: componente `SwipeActions` (destra = pannello app, sinistra = elimina notifiche).
- Home a blocchi ordinabili (`HomeBlocks`: orologio, media, agenda, riepilogo notifiche, widget, pile di widget, app)
  modificabili con pressione lunga (`HomeEditSheet`); `Settings.homeOffset` decide dove inizia la home.

## Stato e roadmap
Vedi `docs/roadmap.md` (aggiornarlo a ogni passo concluso) e `CHANGELOG.md`.
