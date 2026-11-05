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
| 3.3 | **Pile di widget dentro le cartelle popup** (es. una cartella "Casa" con i widget della domotica). | 🟡 le cartelle accettano widget singoli, non pile | P2 |
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
