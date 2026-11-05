# Onda Launcher — istruzioni per Claude

Launcher Android open source (GPL-3.0-or-later), Kotlin + Jetpack Compose, Material You.
Proprietario: Gabriele Piccione. Lingua di lavoro: **italiano** (commenti, commit, changelog).
Ispirato al *comportamento* di Niagara Launcher: si replicano funzioni e gesti, **mai** codice,
nomi, icone o grafica di Niagara.

## Risparmio di token (priorità)
- Non leggere file interi sopra le 300 righe: prima grep, poi solo le righe che servono.
  Se tocchi `HomeScreen.kt`, `SettingsScreen.kt` o `LauncherViewModel.kt`, spezzali in file più piccoli per area.
- Di `docs/niagara-gap.md` e `docs/roadmap.md` leggi solo la sezione del tema in corso.
- CI: un solo comando in background che aspetta la fine della run, niente controlli ripetuti.
  Se fallisce, leggi solo le righe di errore (`grep -E " e: |FAILED|Error:"`), mai il log intero.
- Correzioni piccole della stessa area: una sola PR, non una per correzione.
- Branch sempre da `main` aggiornato; evita PR impilate (generano conflitti).
- Risposte brevi: solo cosa è cambiato e cosa provare sul telefono.
- Questo file resta sotto le 80 righe: i dettagli vanno in `docs/architecture.md`.

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

## Architettura
Vedi `docs/architecture.md` (moduli, componenti chiave, convenzioni di UI, vibrazione, player, cartelle, ricerca).

## Stato e roadmap
Vedi `docs/roadmap.md` (aggiornarlo a ogni passo concluso) e `CHANGELOG.md`.
