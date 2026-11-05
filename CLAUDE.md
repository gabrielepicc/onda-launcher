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
- Niente permesso `INTERNET` finché il proprietario non decide sul meteo (vedi roadmap).

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
# controlli di una PR
gh pr checks <numero>
# unione con CI verde
gh pr merge <numero> --squash --delete-branch
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
- Popup dell'app in `ui/sheets/AppQuickPopup`; `AppPanelSheet` resta per rispondere sopra la tastiera, `AppActionsSheet` per "Altre opzioni".

## Stato e roadmap
Vedi `docs/roadmap.md` (aggiornarlo a ogni passo concluso) e `CHANGELOG.md`.
