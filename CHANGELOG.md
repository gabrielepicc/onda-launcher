# Changelog

Tutte le modifiche rilevanti di Onda. Formato basato su [Keep a Changelog](https://keepachangelog.com/it-IT/1.1.0/), versioni secondo [SemVer](https://semver.org/lang/it/).

## [0.4.0] – Fase 0: fondamenta

### Aggiunto
- Persistenza su **DataStore** con **kotlinx.serialization** e campo `schemaVersion`; migrazione automatica e trasparente dalle impostazioni di Onda 0.2/0.3 (SharedPreferences).
- I backup esportati dalle versioni 0.2/0.3 restano importabili.
- Modulo **`:core:icons`** con un parser `appfilter.xml` indipendente da Android: icone dedicate, `iconback` multipli, `iconmask`, `iconupon`, `scale` e icone calendario dinamiche (le ultime quattro verranno applicate nella Fase 1).
- **Localizzazione**: tutte le stringhe in `strings.xml`, inglese (predefinito) e italiano, pronte per Weblate.
- **Token di design** (`OndaTokens`: spaziature, forme, area minima di tocco 48dp).
- Test unitari: codec delle impostazioni e migrazioni, calcolatrice, parser `appfilter.xml`.
- CI su GitHub Actions: test, Android lint, ktlint, build release; report pubblicati come artefatti.
- Metadati `fastlane` per F-Droid (it-IT, en-US), licenza **GPL-3.0-or-later** con intestazione SPDX su ogni sorgente, `CONTRIBUTING.md`, `docs/architecture.md`.

### Modificato
- UI riorganizzata in package per area: `theme`, `components`, `home`, `drawer`, `search`, `settings`, `sheets`.
- Descrizioni per TalkBack su cartelle, notifiche e controlli multimediali.

## [0.3.1]
- L'elenco delle app sostituisce i preferiti nella stessa pagina (niente pannello sovrapposto).
- Build release ottimizzata (R8, profileinstaller) e firma di sviluppo fissa.

## [0.3.0]
- Alfabeto disegnato su una sola Canvas, lato opposto invisibile, gesti verticali interattivi e configurabili.
- Pannello rapido dell'app con swipe a destra, cartelle con app e widget, suggerimenti in ricerca.
- Opzioni: nascondi barra di stato, alfabeto in home, nomi dei preferiti; dimensione icone.

## [0.2.0]
- Material You, icone a tema, icon pack, player musicale, prossimo evento, doppio tocco per bloccare, calcolatrice, backup, back predittivo.

## [0.1.0]
- Prima versione: preferiti, alfabeto a onda, notifiche in lista, ricerca, widget, personalizzazione.
