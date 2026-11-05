# Onda Launcher — istruzioni per Claude

Launcher Android open source (GPL-3.0-or-later), Kotlin + Jetpack Compose, Material You.
Proprietario: Gabriele Piccione. Lingua di lavoro: **italiano** (commenti, commit, changelog).
Ispirato al *comportamento* di Niagara Launcher: si replicano funzioni e gesti, **mai** codice,
nomi, icone o grafica di Niagara.

## Regole non negoziabili
- Niente Google Play Services, Firebase, analytics o pubblicità. Licenze delle dipendenze compatibili con GPLv3.
- Privacy: nessun dato lascia il dispositivo. Il meteo arriva dall'app Breezy Weather installata (provider locale), non da internet.
- Permessi just-in-time, con spiegazione. Da Android 13 notifiche e accessibilità sono
  "impostazioni con limitazioni" per gli APK: mostrare la guida (`withRestrictedHelp` in HomeScreen).
- Nessun testo scritto nel codice: `stringResource(R.string.…)`, inglese in `values/`, italiano in `values-it/`.
- Ogni file `.kt`/`.kts` inizia con l'intestazione SPDX + copyright.
- Nuove impostazioni: campo con default in `Settings` (data/Models.kt). Se cambia il significato di un
  campo esistente: incrementa `Settings.CURRENT_SCHEMA` e aggiungi il passo in `SettingsCodec.migrate` + test.
- Target touch ≥ 48dp, `contentDescription` sugli elementi senza testo.
- Niente permesso `INTERNET` (decisione del proprietario; il meteo passa da Breezy Weather).

## Flusso di lavoro con git
- **Mai push diretti su `main`.** Ogni blocco di lavoro su un branch `claude/<tema-breve>`, poi una PR
  verso `main` con descrizione in italiano (cosa cambia, cosa provare sul telefono).
- Si unisce (squash) **solo con la CI verde** sulla PR. Claude può unire da solo le proprie PR se la CI è verde;
  se è rossa, si corregge sullo stesso branch, non su `main`.
- Una PR = un tema (una funzione o una correzione), così è facile da rivedere e da annullare.
- Dopo l'unione: aggiornare `docs/roadmap.md`, `CHANGELOG.md` e, quando si chiude una versione,
  `versionCode`/`versionName` e `fastlane/metadata/android/{en-US,it-IT}/changelogs/<versionCode>.txt`.

## Stile del codice
- Importare classi e funzioni: **niente nomi completi** nel codice (`com.gabrielepiccione.onda.data.X.f()`).
- La logica pura (calcoli, regole, migrazioni) sta in funzioni testabili su JVM in `data/` o `util/`, con test.

## Come riprendere il lavoro
1. Controllare le PR aperte (`gh api repos/gabrielepicc/onda-launcher/pulls --jq '.[] | "\(.number) \(.head.ref) -> \(.base.ref)"'`)
   e unire quelle con CI verde, nell'ordine in cui dipendono l'una dall'altra (il proprietario ha autorizzato l'unione).
   Se due PR toccano gli stessi file, partire da `main` aggiornato e unirle una per volta, risolvendo i conflitti sul branch.
2. Prendere la prossima voce da `docs/niagara-gap.md` ("Ordine proposto"): una voce = un branch = una PR.
3. Le richieste dell'utente arrivano spesso con screenshot di Niagara: replicare il comportamento, mai la grafica.

## Decisioni da rispettare
- Backup: il formato di riferimento è un unico file che contiene impostazioni **e** risorse dell'utente
  (immagini delle icone, font importati, temi salvati). Il backup automatico gira con WorkManager ogni 24 ore
  (oltre che all'uscita dal launcher) e conserva gli ultimi 7 file. Il ripristino deve accettare anche i vecchi `.json`.
- Elenco delle funzioni di Niagara ancora mancanti e priorità: `docs/niagara-gap.md`. Prima di iniziare un tema,
  leggere la voce corrispondente; a lavoro finito, segnarla come fatta lì e in roadmap.

## Verifica
Non c'è SDK Android in locale: la verifica è la CI di GitHub Actions (`.github/workflows/build-apk.yml`):
`testDebugUnitTest`, `lintRelease`, `ktlintCheck`, `assembleRelease`. Gira su ogni push e su ogni PR.
```bash
# stato dell'ultima run di un branch
gh api "repos/gabrielepicc/onda-launcher/actions/runs?branch=<branch>&per_page=1" --jq '.workflow_runs[0] | "\(.id) \(.status) \(.conclusion)"'
# (in queste sessioni GraphQL non è disponibile: niente `gh pr ...`, usare l'API REST)
# aprire la PR
gh api repos/gabrielepicc/onda-launcher/pulls -f base=main -f head=<branch> -f title="..." -f body="..." --jq .number
# unione con CI verde, poi eliminazione del branch
gh api -X PUT repos/gabrielepicc/onda-launcher/pulls/<numero>/merge -f merge_method=squash
gh api -X DELETE repos/gabrielepicc/onda-launcher/git/refs/heads/<branch>
# errori: gh api repos/gabrielepicc/onda-launcher/actions/jobs/<job_id>/logs | grep -E " e: file|FAILED|Error:"
```
Lint: evitare `produceState` (falso positivo `ProduceStateDoesNotAssignValue`), usare `LaunchedEffect` + stato.
L'APK release (firmato con `app/onda-dev.keystore`, chiave di sviluppo) è nell'artefatto `OndaLauncher-release`.

## Architettura (vedi docs/architecture.md)
- `:app` — UI in `ui/{theme,components,home,drawer,search,settings,sheets}`, stato in `LauncherViewModel`,
  dati in `data/` (DataStore + `SettingsCodec`), servizi in `service/`.
- `:core:icons` — parser `appfilter.xml`, `IconPacks`/`IconPackCache`, `IconRenderer` (forme, fallback, puntini), `IconMatcher`.
- Gesti righe app: componente `SwipeActions` (destra = popup dell'app `AppQuickPopup`, sinistra = elimina notifiche).
- Home a blocchi ordinabili (`HomeBlocks`: orologio, media, widget, pile, agenda, riepilogo notifiche, app) modificabili con pressione lunga (`HomeEditSheet`) o trascinando (`HomeReorderSheet`).
- Impostazioni: `ui/settings/SettingsScreen` (pagine `SettingsPage`), scelte visive e anteprime in `ui/settings/SettingsVisuals` (`ChoiceTile`, `TileGrid`, `PhonePreview`).
- Popup dell'app in `ui/sheets/AppQuickPopup`; `AppPanelSheet` resta per rispondere sopra la tastiera, `AppActionsSheet` per "Altre opzioni".
- Elenco app (`ui/drawer/AllAppsPanel`): lettere, poi Spazio privato, cartelle e voci di Onda ("Installate di recente",
  "Impostazioni"); il segno `END_OF_LIST_MARK` dell'alfabeto porta lì (`endOfListIndex`).
- Alfabeto (`ui/components/AlphabetWaveScroller`, geometria in `util/AlphabetLayout`): zona di tocco a tutta altezza,
  colonna visibile alta quanto le lettere (max 19dp ciascuna) che riposa in basso e, toccando più in alto, sale sotto il dito
  (`AlphabetLayout.anchoredTop`); in cima `FAVORITES_MARK` riporta ai preferiti.
  `onLetter(lettera)`: la home porta la prima app della sezione al centro dello schermo.
- Vibrazione: sempre `LocalOndaHaptics.current?.perform(HapticEvent.X)` (mai `LocalHapticFeedback` diretto).
  `util/HapticPlan` sceglie primitive o costanti di sistema (testato), `util/OndaHaptics` le suona e segue il sistema.
- Player: visibile solo con riproduzione attiva o pausa recente (`data/MediaRules`).
- Cartelle: `Folder.icon` (vedi `data/FolderIcons`) e `Folder.widgetHeightDp`; più widget = carosello `WidgetStackCard`.
- Ricerca: nessun pannello opaco, solo una velatura (`SEARCH_TINT` in `ui/search/SearchPanel`).
- "Aggiungi alla home" dalle altre app: `PinRequestActivity`; le scorciatoie stanno in `favorites` con `SHORTCUT_PREFIX` (`data/PinnedShortcuts`).
- Meteo: `data/WeatherRepository` legge il provider di Breezy Weather, `data/WeatherData` lo interpreta (solo versione maggiore 0).
- Alfabeto localizzato: `util/IcuSectionIndex` (ICU, non testabile su JVM), ripiego `PlainSectionIndex`; le sezioni sono `String`.
- Popup dell'app personalizzabile: `data/AppPopups`, `ui/sheets/AppPopupEditSheet`. Suggerimenti in home: `data/HomeSuggestions`.
- Backup: `data/BackupArchive` (ZIP, accetta solo `icons` e `fonts`), `data/Backups` (file e WorkManager), `data/AutoBackup` (cartella scelta).
- Promemoria di Usage Breaker: regole in `data/UsageReminders`, controllo in `service/UsageReminderEngine`, avviato dal servizio notifiche.

## Material 3
Raggi solo dalla scala (4, 8, 12, 16, 20, 28, 32, 48, pieno), testi dai ruoli di `MaterialTheme.typography` (mai `fontSize` a mano, tranne orologio e alfabeto),
bersagli di tocco di almeno 48dp, durate dai token. Regole e differenze volute in `docs/material-audit.md`.
Popup dell'app: movimento Material 3 Expressive (molla spaziale 0,8/380, effetti 1600, uscita 200 ms, contenuto a cascata).

## Benessere digitale
Statistiche pure in `data/UsageStatsMath` (testate), lettura degli eventi in `data/UsageStatsRepository` (launcher e UI di sistema esclusi),
notifiche contate da `NotificationCounter` (solo numeri). Limiti in `Settings.appLimits` (pacchetto -> minuti): pausa nel launcher (`guarded` in HomeScreen)
e avviso da `UsageReminderEngine`. Pagina `SettingsPage.WELLBEING`, grafici in `ui/settings/WellbeingDashboard`.

## Luci di casa e pausa ovunque
Luci: `data/HomeLights` (Wi-Fi, buio, alba/tramonto NOAA, testati), `data/HomePresence` (SSID e ultima posizione, permesso di posizione),
widget scelto con `WidgetTarget.HomeLights`, mostrato accanto all'orologio in HomeScreen. Pausa fuori dal launcher: `data/BreakerGate` +
`OndaAccessibilityService` (solo pacchetto in primo piano) -> `MainActivity.EXTRA_PAUSE_PACKAGE` -> `vm.pauseRequests`.

## Preferiti contestuali
Motore puro in `data/ContextProfiles` (regole E/O, priorità, scelta a mano di un'ora, slot fissi, suggerimenti; testato),
lettura del contesto in `data/ContextReader` (+ `BluetoothWatcher`, `LaunchLog`), profilo attivo in `LauncherViewModel.refreshContext`
(ogni 30 s dalla home) che riordina `home` con `ContextProfiles.favorites`. Pagina `SettingsPage.CONTEXTS` in `ui/settings/ContextProfilesPage`.

## Stato e roadmap
Vedi `docs/roadmap.md` (aggiornarlo a ogni passo concluso) e `CHANGELOG.md`.
