# Changelog

Tutte le modifiche rilevanti di Onda. Formato basato su [Keep a Changelog](https://keepachangelog.com/it-IT/1.1.0/), versioni secondo [SemVer](https://semver.org/lang/it/).

## [0.8.0]

### Aggiunto
- **Agenda** del calendario come blocco della home (1–7 giorni, colori dei calendari), modificabile con pressione lunga.
- **Blocca layout** per evitare modifiche accidentali.
- **Ricerca**: prefissi `=` (calcolo), `@` (solo contatti), `/` (impostazioni di sistema e di Onda); motore web selezionabile
  (DuckDuckGo, Startpage, Google, Ecosia, Brave); Invio apre il primo risultato della modalità.
- **Plug & Play**: con cuffie o casse collegate, l'app scelta compare in cima alla home.
- **App protette** con impronta, volto o PIN; protezione opzionale dell'elenco delle app nascoste.

## [0.7.0]

### Aggiunto
- **Gesti uguali in home e nell'elenco app**: destra apre il pannello dell'app, sinistra elimina le notifiche; giù apre sempre la tendina.
- **Fase 1 – icone**: forme adattive (cerchio, squircle, quadrato, goccia, cilindro, petalo), fallback degli icon pack
  (iconback/iconmask/iconupon/scale), icone calendario dinamiche, "Cambia icona" con suggerimenti da tutti i pack, cache in memoria,
  icone generate per categoria negli stili Material, Contorno, Sticker, Acquerello, Marmo e Puntini, classificatore delle app.
- **Fase 2 – testo e orologio**: colore del testo, allineamento della lista, peso e spaziatura del carattere, ombra del testo,
  batteria accanto alla data, font importati da file per lista e orologio.
- **Fase 3 – temi**: Theme Studio per salvare l'aspetto come tema, applicarlo, rinominarlo, duplicarlo ed eliminarlo;
  esportazione/importazione di file `.onda-theme` con i font incorporati; palette dallo sfondo anche su Android 8.1–11.
- Player compatto ed esteso riprogettati; margini simmetrici per orologio, widget e player.
- App di ogni lettera ordinate per utilizzo; notifiche anche nell'elenco app; doppio tocco sull'alfabeto per bloccare.
- `CLAUDE.md` e `docs/roadmap.md` per lo sviluppo continuo.

### Modificato
- Schema impostazioni 3: lo swipe su impostato su "notifiche" diventa "ricerca" (lo swipe giù è ora fisso sulla tendina).

## [0.5.0]

### Aggiunto
- **Notifiche in home complete**: conversazioni (ultimi messaggi con mittente), **risposta diretta** (RemoteInput, es. WhatsApp/Telegram), pulsanti delle azioni dell'app, eliminazione con swipe o ×. Anche nel pannello rapido dell'app.
- Guida alle **impostazioni con limitazioni** di Android 13+ con scorciatoia a Info app (necessaria per le app installate da APK).
- **Home modificabile**: tenendo premuti orologio o widget si apre un foglio con anteprima dal vivo per cambiare altezza (widget), stile, dimensione e allineamento (orologio), posizione nella home, sostituire o eliminare.
- **8 stili di orologio**: sottile, impilato, deciso, monospazio, contorno, sfumato, a parole (IT/EN), analogico.
- **12 preset di stile** con sfondi generati proceduralmente (mesh, onde, dune, forme morbide, grana carta): sfondo + carattere + orologio + icone + colori, con galleria e anteprime.
- Test per l'orologio "a parole".

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
