# Confronto con Niagara Launcher: cosa manca a Onda

Aggiornato alla 0.9.0 (novembre 2026). Si replica il **comportamento**, mai codice, nomi, icone o grafica.

Fonti: centro assistenza ufficiale di Niagara (help.niagaralauncher.app), note delle versioni 2025
(aggiornamenti "Theming" e "Artistic"), confronto con il codice di Onda. Le voci segnate *(da verificare)*
sono comportamenti osservati nell'app ma non documentati: vanno controllati sull'APK prima di implementarli.

Priorità: **P1** = si nota tutti i giorni · **P2** = completa l'esperienza · **P3** = di nicchia.
Stato: ⬜ manca · 🟡 c'è ma diverso o parziale · ✅ fatto (si sposta qui quando chiuso).

---

## 1. Home e preferiti

| # | Comportamento di Niagara | Onda oggi | Priorità |
|---|---|---|---|
| 1.1 | **App suggerite in fondo ai preferiti**: le app più usate compaiono da sole sotto i preferiti; si può tenere la home senza preferiti, del tutto "adattiva". Pressione lunga su un suggerimento = nascondilo per sempre. | ⬜ i suggerimenti ci sono solo nella ricerca | P1 |
| 1.2 | **Animazione quando cambiano i preferiti** (aggiunta, rimozione, spostamento: le righe scorrono al loro posto). | 🟡 da verificare `animateItem` su tutte le liste della home | P2 |
| 1.3 | **Pulsante flottante in basso a destra** (equivalente del "Niagara Button"): tocco = apre un'app o una cartella popup; swipe su = seconda azione. Pressione lunga = "modifica azione del tocco" / "crea cartella popup". | ⬜ | P2 |
| 1.4 | **Spazio vuoto sopra i preferiti pensato per il pollice**: il player compare sotto lo spazio vuoto e spinge giù i preferiti, così i comandi restano a portata. | 🟡 c'è la posizione della home; controllare che il player non sposti i preferiti fuori dalla zona del pollice | P2 |
| 1.5 | **Tocco sull'ora = app Sveglia, tocco sulla data = foglio agenda** (eventi + meteo), non l'app Calendario. | 🟡 la data apre il calendario di sistema; manca il **foglio agenda** | P2 |
| 1.6 | **Prossimo evento sotto la data** nell'orologio (anteprima del calendario, disattivabile), con priorità agli eventi non "tutto il giorno". | 🟡 c'è il blocco agenda; manca l'anteprima integrata nell'orologio e la priorità sugli eventi a orario | P2 |
| 1.7 | **Sostituire l'orologio con un widget** ("Sostituisci orologio con widget") mantenendo lo stesso posto e la stessa posizione regolabile. | 🟡 si ottiene togliendo l'orologio e aggiungendo un widget; manca la voce diretta | P3 |

## 2. Alfabeto ed elenco app

| # | Comportamento di Niagara | Onda oggi | Priorità |
|---|---|---|---|
| 2.1 | **Alfabeto localizzato**: lettere extra secondo le lingue del telefono (Å Ä Ö dopo la Z per svedese/finlandese, Æ Ø Å per danese/norvegese, Ñ per spagnolo); nomi in altri alfabeti (giapponese, cinese, cirillico, greco) **traslitterati** e messi sotto la lettera giusta, non sotto "#". Conta l'ordine delle lingue di sistema. | ⬜ tutto ciò che non è A–Z finisce in "#". Soluzione: `android.icu.text.AlphabeticIndex` (API 24+) con le lingue di sistema | P1 |
| 2.2 | **Velatura dietro l'elenco app** per leggere meglio sullo sfondo (niente sfocatura: Android non la permette sullo sfondo del launcher). | 🟡 c'è `wallpaperDim`; verificare che si applichi in modo diverso a home ed elenco | P3 |
| 2.3 | **Fondo dell'elenco**: dopo la Z le **cartelle**, poi le voci del launcher **"Installate di recente"** e **"Impostazioni"**; un segno in fondo all'alfabeto ci porta direttamente. | ✅ 0.10 (tutte le cartelle, senza interruttore per singola cartella) | P1 |
| 2.3b | **Stella in cima all'alfabeto** che riporta ai preferiti. | ⬜ | P2 |
| 2.4 | **Profilo di lavoro**: badge a valigetta (✅ c'è) e, quando il profilo è in pausa, app **in grigio** e non avviabili; interruttore per riattivarlo. | 🟡 manca lo stato "in pausa" in grigio | P2 |
| 2.5 | Voce **"App nascoste"** in fondo all'elenco e opzione per **toglierle anche dalla ricerca**. | 🟡 verificare l'opzione "escludi dalla ricerca" | P2 |
| 2.6 | **Due colonne nell'elenco app** su tablet e pieghevoli; alfabeto e lista più lontani dai bordi; icone scalate sull'altezza dello schermo. Interruttore "Layout a due colonne". | 🟡 c'è solo il contenuto centrato | P3 |

## 3. Swipe, popup dell'app e scorciatoie

| # | Comportamento di Niagara | Onda oggi | Priorità |
|---|---|---|---|
| 3.1 | **Popup dell'app personalizzabile come una cartella**: oltre a notifiche e scorciatoie, si possono aggiungere **altre app correlate**, **widget** e **pile di widget** (fino a 4). Si modifica con pressione lunga sul popup → "Modifica". | 🟡 c'è un solo widget per app; mancano app correlate e pile nel popup | P1 |
| 3.2 | **Nascondere le singole scorciatoie** di un'app ("Modifica scorciatoie" dal popup, con caselle da spuntare). | ⬜ | P2 |
| 3.3 | **Pile di widget dentro le cartelle popup** (es. una cartella "Casa" con i widget della domotica). | ✅ 0.10 carosello orizzontale con altezza regolabile; in più icona della cartella personalizzabile | P2 |
| 3.4 | **Notifiche complete nel popup**: conversazione intera, più notifiche della stessa app impilate, azioni. | ✅ verificare solo l'impilamento di più notifiche della stessa app | — |

## 4. Ricerca

| # | Comportamento di Niagara | Onda oggi | Priorità |
|---|---|---|---|
| 4.1 | **Tasto Home sulla home = apre la ricerca** (quando lo swipe su è impostato sulla ricerca). | 🟡 `onHomePressed` esiste: verificare che apra la ricerca e non solo chiuda i pannelli | P2 |
| 4.2 | **Swipe lungo vs scorrimento veloce**: uno scorrimento rapido dei preferiti non apre la ricerca; serve uno swipe lungo. Per la tendina, tenere fermo il dito prima di scorrere evita di aprirla. | 🟡 da tarare sul telefono (soglie di distanza e velocità) | P1 |
| 4.3 | **Calcolatrice completa**: `abs`, `log`, `log10`, `fact`, `round(n, cifre)`, `floor`, `ceiling`, `min`, `max`, `random`, `sin/cos/tan` (radianti), `asin/acos` (gradi), `rad`, `deg`, costanti `pi` ed `e`, moltiplicazione implicita `2(1+3)`, modulo `%` come resto, confronti (`>`, `<`, `==`, `!=`) e logica (`&&`, `||`, `!`). | 🟡 solo + − × ÷ ^ e % come percentuale | P2 |
| 4.4 | **Suggerimenti web mentre si scrive** con motori Brave, Bing, DuckDuckGo, Google, Kagi, Perplexity, Startpage. | 🟡 c'è la scelta del motore (senza suggerimenti, che richiedono INTERNET). Aggiungere Brave, Bing, Kagi, Perplexity all'elenco: è solo un URL | P2 (motori) / bloccato (suggerimenti) |
| 4.5 | **Comandi nascosti** scritti nella ricerca con "/" e confermati da un dialogo (es. screenshot trasparente, disattivare l'avviso "imposta come predefinito"). | ⬜ e in Onda "/" è già il prefisso delle impostazioni: usare un altro prefisso, es. `>` | P3 |
| 4.6 | **Screenshot trasparente** della home (PNG senza sfondo, con timer di 1 secondo) per mostrare il proprio setup. | ⬜ | P3 |
| 4.7 | **Tastiera fisica e mouse**: digitare sulla home avvia la ricerca, scorciatoie da tastiera. | ⬜ | P3 |

## 5. Notifiche

| # | Comportamento di Niagara | Onda oggi | Priorità |
|---|---|---|---|
| 5.1 | **Riepilogo notifiche vero**: le notifiche delle app scelte vengono **trattenute** (tolte dalla tendina) e consegnate in un unico gruppo 2–3 volte al giorno (dopo 5,5–6,5 ore, sempre allo scoccare dell'ora). Opzione "riassumi anche le notifiche silenziose"; "Svuota" fa ripartire il conteggio. | 🟡 Onda mostra solo un blocco con il conteggio; non trattiene né riconsegna | P2 |
| 5.2 | **Pressione lunga su una notifica** → disattivare le notifiche di quell'app (scorciatoia alle impostazioni di sistema). | 🟡 verificare | P3 |

## 6. Benessere digitale

| # | Comportamento di Niagara | Onda oggi | Priorità |
|---|---|---|---|
| 6.1 | **Usage Breaker a promemoria**: mentre usi un'app scelta, una notifica ti chiede se vuoi continuare dopo 5, 10, 20, 30, 45, 60, 90, 120, 180 e 240 minuti di uso continuo. Le interruzioni brevi (un messaggio, una ricerca) non azzerano il tempo. **Non blocca mai** l'app. Richiede l'accesso ai dati di utilizzo (da concedere a mano, con guida). | 🟡 Onda fa una pausa *prima* di aprire l'app: tenerla e **aggiungere** i promemoria come seconda modalità | P1 |

## 7. Widget, meteo e calendario

| # | Comportamento di Niagara | Onda oggi | Priorità |
|---|---|---|---|
| 7.1 | **Meteo**: condizioni attuali nell'orologio, previsioni orarie (24 h) e a 7 giorni nel foglio agenda; posizione attuale o fissa; °C/°F; aggiornamento ogni ~30 minuti. | ⬜ **bloccato**: serve la decisione sul permesso INTERNET (Open-Meteo, disattivabile) | P1 |
| 7.2 | **Pile di widget**: massimo 4 widget, riordino dal menu della pila; su schermi larghi **due widget affiancati** (opzione "widget a due colonne"). | 🟡 manca la modalità a due colonne; verificare il limite | P3 |
| 7.3 | **Filtro dei calendari** (scegliere quali calendari mostrare) e compleanni dei contatti. | ⬜ | P2 |
| 7.4 | **Player**: copertina presa da quella della schermata di blocco; il player sparisce quando sparisce dalla tendina; tocco sul player = apre il brano in riproduzione; animazione al cambio brano. | 🟡 verificare l'animazione al cambio brano | P3 |

## 8. Icone e aspetto

| # | Comportamento di Niagara | Onda oggi | Priorità |
|---|---|---|---|
| 8.1 | **Ripristino automatico per pack**: le icone scelte a mano sono ricordate **per ogni icon pack**; tornando a un pack usato prima, ritornano le scelte fatte allora. | ⬜ le scelte sono globali | P2 |
| 8.2 | **Suggerimenti da più categorie** nel cambio icona (es. un gioco di carte: suggerisce sia le carte sia il controller). | 🟡 c'è il classificatore a una categoria | P3 |
| 8.3 | **Famiglia di stili di icone**: oltre a quelli che Onda ha, una variante **scura** (per sfondi scuri) e una **"vivace"** con forme giocose; lo stile a puntini che prende i colori del tema. | 🟡 | P3 |
| 8.4 | **Palette dallo sfondo "onesta"**: il colore Material You di sistema si usa solo se è tra i colori dominanti dello sfondo, altrimenti si genera la palette dallo sfondo (evita il blu fisso di alcuni Samsung). | ⬜ | P2 |
| 8.5 | **Orologi con meteo, eventi e batteria** nello stesso stile. | 🟡 manca il meteo (7.1) | — |
| 8.6 | **Temi condivisibili** con un link o un QR, e una galleria a carosello in "Aspetto" con anteprima e "Applica tema". | 🟡 c'è il file `.onda-theme`; mancano link/QR e carosello | P3 |

## 9. Backup e sistema

| # | Comportamento di Niagara | Onda oggi | Priorità |
|---|---|---|---|
| 9.1 | **Backup completo** in un unico file: preferiti, icone scelte, cartelle, nomi rinominati, impostazioni, temi e **widget** (ricollegati al ripristino; la loro configurazione interna va rifatta). Pulsante "Condividi backup". | 🟡 Onda salva solo il JSON delle impostazioni: mancano immagini delle icone, font, ricollegamento dei widget, condivisione | P1 |
| 9.2 | **Integrazione con KLWP/KWGT**: il launcher pubblica variabili (schermata attuale, evento visibile, player visibile, colori della palette) per sfocare lo sfondo o mostrare elementi solo nella home; un link `onda://agenda` apre il foglio agenda. | ⬜ | P3 |
| 9.3 | **Avviso "imposta come launcher predefinito"** quando non lo è, disattivabile. | 🟡 verificare | P3 |

---

## 10. Altro emerso dall'APK 1.15.6 (testi e impostazioni dell'app)

Ricavato dai testi dell'interfaccia e dalle impostazioni contenute nell'APK (nessun codice letto o copiato).

| # | Comportamento di Niagara | Onda oggi | Priorità |
|---|---|---|---|
| 10.1 | **Alfabeto compatto e ancorato in basso**: la colonna è alta quanto le lettere (passo massimo ~19 sp) invece di occupare tutto lo schermo; ha un segno in cima (preferiti) e in fondo (cartelle / voci del launcher). | ✅ 0.10 colonna compatta in basso + segno finale · ⬜ stella in cima | P1 |
| 10.2 | **La lettera scelta si allinea al dito**: l'inizio della sezione compare all'altezza del pollice, non in cima allo schermo. | ✅ 0.10 | P1 |
| 10.3 | **Ricerca sopra lo sfondo**: nessun pannello opaco; c'è solo l'opzione "Velatura sfondo" per leggere meglio elenco e ricerca. | ✅ 0.10 | P1 |
| 10.4 | **Ricerca a vuoto con suggerimenti intelligenti**: "Cercate spesso", "Ultimo risultato", "Installate di recente"; le app nascoste compaiono in un gruppo "Nascoste". | 🟡 ci sono solo le app più usate | P2 |
| 10.5 | **Ricerca profonda con parole chiave**: iniziare con una parola chiave per cercare direttamente su un sito o un'app (YouTube, traduttore, Wolfram Alpha…), con l'elenco delle parole chiave nelle impostazioni. | ⬜ (è solo aprire un URL: niente INTERNET nell'app) | P2 |
| 10.6 | **Riconoscimento nel testo cercato**: un URL → "Apri"; un numero → "Chiama" / "Messaggio"; un'email → "Scrivi email"; un contatto inesistente → "Aggiungi ai contatti". | ⬜ | P2 |
| 10.7 | **Popup / cartelle create direttamente nell'elenco app** (alla creazione si sceglie: preferiti o elenco app) e **suggerimenti di app da aggiungere a una cartella**. | 🟡 le cartelle nascono solo dai preferiti | P2 |
| 10.8 | **Scorciatoie delle app come voci a sé**: "Aggiungi alla home" e "Mostra nell'elenco app" per una singola scorciatoia (gruppo "Scorciatoie" nell'elenco). | ⬜ | P2 |
| 10.9 | **"Smetti di suggerire" / "Suggerisci di nuovo"** sulle app suggerite sotto i preferiti; interruttore "Suggerisci app usate spesso". | ⬜ (vedi 1.1) | P1 |
| 10.10 | **Ordinamento per uso disattivabile** ("Ordina l'elenco per utilizzo"). | 🟡 in Onda è sempre attivo: aggiungere l'interruttore | P3 |
| 10.11 | **"Riordina l'elenco"**: propone di nascondere le app usate raramente (gruppi "Usate spesso" / "Usate raramente"). | ⬜ | P3 |
| 10.12 | **Tempo di utilizzo per app** dal menu dell'app ("Tempo di utilizzo", "aperta N volte"), app più usate degli ultimi 7 giorni e riepilogo annuale. | ⬜ (richiede l'accesso ai dati di utilizzo, già previsto per 6.1) | P3 |
| 10.13 | **Modalità mattino**: nei primi minuti dopo la sveglia chiede conferma prima di aprire le app "di svago" e ricorda da quanto si usa il telefono (consiglio: max 10 minuti a letto). | ⬜ | P3 |
| 10.14 | **Categoria "app di svago"** scelta dall'utente, usata da Usage Breaker e modalità mattino. | ⬜ | P3 |
| 10.15 | **Riepilogo notifiche**: "Escludi conversazioni" (i messaggi arrivano subito), chiamate e sveglie sempre immediate, "Chiudi riepilogo e pianifica il prossimo", orario del prossimo riepilogo. | ⬜ (vedi 5.1) | P2 |
| 10.16 | **Pausa delle app di lavoro** dall'elenco ("Metti in pausa" / "Riattiva"). | ⬜ (vedi 2.4) | P2 |
| 10.17 | **Widget**: "Riempi lo schermo", "Sposta e ridimensiona", "Widget a due colonne", "Sostituisci orologio con widget"; ripristino dei widget dal backup con avviso se l'app non lo supporta più. | 🟡 | P3 |
| 10.18 | **Preferiti "da aperto"** sui pieghevoli: app in più che compaiono solo con lo schermo aperto. | ⬜ | P3 |
| 10.19 | **Aspetto**: tema scuro "in base allo sfondo / al sistema / ibrido", **nero assoluto**, colore del testo automatico/chiaro/scuro, dimensione del testo 80–140 %, **font in modalità ibrida** (testi lunghi o piccoli col font di sistema), "rimodella le icone vecchie" (sfondo bianco per uniformare la forma), "sposta lo sfondo" per i wallpaper decentrati, rotazione della home attivabile. | 🟡 manca: nero assoluto, modalità ibrida, rimodella icone, rotazione disattivabile | P3 |
| 10.20 | **Stili di icone regolabili**: palette (colori dell'app, del tema, varianti), spessore linea (elegante/marcato), tonalità (chiara/scura/vivace), contorno pieno o vuoto, puntini tondi o a rombo; **modifica icone in blocco** e avviso "icone mancanti" con correzione automatica. | 🟡 (vedi 8.x) | P3 |
| 10.21 | **Meteo**: pioggia nei prossimi 30 minuti, temperatura percepita, località per città o coordinate. **Agenda**: pulsante "Nuovo evento", eventi "occupato" e "fino alle…". | ⬜ meteo bloccato · agenda 🟡 | P2 |
| 10.22 | **Musica**: "Imposta come app musicale" dal menu dell'app; app musicali anche con apparecchi acustici. | 🟡 | P3 |
| 10.23 | **Spazio privato nascosto raggiungibile cercando "spazio privato"**. | 🟡 verificare | P3 |
| 10.24 | **Impostazioni del launcher**: "Riavvia launcher", "Cambia launcher predefinito", guida alla navigazione a gesti, integrazione con Tasker/Locale per cambiare profilo. | 🟡 | P3 |

## 11. Feedback aptico

Niagara (dall'APK: impostazione "Consenti feedback aptico — segue il sistema", verifica "vibrazioni leggere supportate",
uso di composizioni di primitive e di effetti predefiniti, colpi distinti come tick, bump, spin, appear legati alle animazioni).

| # | Comportamento | Onda | Priorità |
|---|---|---|---|
| 11.1 | Colpi diversi per eventi diversi, leggerissimi sullo scorrimento dell'alfabeto | ✅ 0.10 (`HapticPlan`/`OndaHaptics`) | P1 |
| 11.2 | Segue l'impostazione di sistema del feedback al tocco | ✅ 0.10 | P1 |
| 11.3 | Verifica del supporto alle vibrazioni leggere nelle impostazioni | ✅ 0.10 ("Prova la vibrazione") | P3 |
| 11.4 | Vibrazione legata al progresso delle animazioni (es. un colpo quando il popup "atterra", al cambio di brano) | ⬜ | P3 |

## Non da copiare (scelte di Niagara o incompatibili con le regole di Onda)
- Feed di Google Discover: richiede l'app Google, contrario alla regola "niente Google".
- Integrazione con Sesame: app abbandonata, Niagara la sta togliendo.
- Suggerimenti di icone dai server dello sviluppatore, abbonamento Pro, inviti, bacheca delle richieste in-app
  (in Onda basta un link alle issue di GitHub nelle impostazioni).
- Funzioni che Niagara **non** ha e Onda sì (da tenere): pausa prima di aprire le app, app protette con biometria,
  stili di icone generati, contatti nell'alfabeto, Plug & Play nei preferiti, Spazio privato.

## Ordine proposto
1. **P1 senza internet**: 2.1 alfabeto localizzato · 1.1 suggerimenti nella home · 3.1 popup come cartella ·
   6.1 Usage Breaker a promemoria · 9.1 backup completo · 4.2 taratura dei gesti.
2. **P2**: 1.3 pulsante flottante · 1.5–1.6 foglio agenda e prossimo evento · 2.3–2.5 · 3.2–3.3 · 4.1 · 4.3–4.4 (motori) ·
   5.1 · 7.3 · 8.1 · 8.4.
3. **Bloccato**: 7.1 meteo e 4.4 suggerimenti web, finché non si decide sul permesso INTERNET.
4. **P3**: il resto.
