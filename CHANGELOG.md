# Changelog

Tutte le modifiche rilevanti di Onda. Formato basato su [Keep a Changelog](https://keepachangelog.com/it-IT/1.1.0/), versioni secondo [SemVer](https://semver.org/lang/it/).


## [Non rilasciato]
- **Alfabeto, fondo dell'elenco**: il segno • in fondo all'alfabeto mostra per intero cartelle, installate di recente e impostazioni,
  senza dover scorrere. Mentre il dito è sull'alfabeto il pulsante di ricerca si fa da parte, così l'onda delle lettere non ci passa sopra.
- **Material 3, tocco e leggibilità**: bersagli di almeno 48 dp, campioni di colore che vanno a capo e si leggono con TalkBack, accento più
  leggibile nel tema chiaro, ondina al tocco sul pulsante di ricerca. Il controllo completo è in `docs/material-audit.md`.
- **Material 3, forme e testi**: raggi solo dalla scala ufficiale (4, 8, 12, 16, 20, 28), testi dai ruoli della scala tipografica invece di
  misure scritte a mano, titolo delle barre in alto come quello di Material.
- **Bolla vera per il popup**: si apre come un cerchio con pellicola iridescente che nasce dall'icona e diventa il popup. Chiudendolo si
  raccoglie in un cerchio, la pellicola si fora in un punto, si ritira dal foro e il bordo si rompe in goccioline. Niente più riquadro stirato.
- **Notifiche nel popup**: testo della stessa misura delle scorciatoie, una sola scheda, massimo 4 righe, azioni che vanno a capo.
- **Popup dell'app riordinato**: in alto l'app ("Apri l'app") e, in alto a destra, i tre puntini con il menu di tutte le azioni
  (preferiti, informazioni, modifica popup, disinstalla, altre opzioni). Sotto, sezioni con titolo: Notifiche in schede, Scorciatoie
  in un gruppo, App correlate e Widget. Spariscono i cinque pulsanti in fondo.
- **Popup a bolla**: si apre gonfiandosi dall'icona (supera un poco la misura e si assesta) e si chiude come se scoppiasse:
  si dilata e svanisce. Tirandolo oltre il bordo si tende come una bolla e, se lo lasci abbastanza teso, scoppia.
  Il gesto a destra sulle righe non fa più scorrere la riga: si tende appena e cresce una bolla che, superata la soglia, scatta.
- **Modifica dalla home**: stile e allineamento dell'orologio, stile del player e posizione della home usano gli stessi riquadri
  con il disegno delle Impostazioni, al posto dei semplici pulsanti con il nome.
- **Popup dell'app**: tirandolo oltre il bordo del contenuto (in cima, in fondo, o quando c'è tutto in vista) segue il dito con
  resistenza; lasciandolo abbastanza lontano, o con un colpo deciso, esce con un'animazione fluida nella stessa direzione.
  Altrimenti torna al suo posto con un rimbalzo.
- **Altezza dell'alfabeto**: in Alfabeto e ricerca si sceglie dove riposa la colonna delle lettere, dal basso (predefinito)
  al centro fino in cima; l'anteprima del telefono la segue. Toccando più in alto la colonna sale ancora sotto il dito.
- **Cerca nelle impostazioni**: un campo in cima all'indice trova la voce per nome (senza maiuscole né accenti, anche con più
  parole) e porta alla pagina che la contiene.
- **Alfabeto**: toccando il bordo sopra le lettere, la colonna sale incontro al dito e lo scorrimento parte da lì, invece di
  selezionare la stella. Alzando il dito torna al suo posto. La zona di tocco arriva quasi in cima allo schermo.
- **Pulsante di ricerca**: fluttua in basso accanto all'alfabeto, velato sullo sfondo, a destra o a sinistra secondo la mano.
  Sta poco sopra la barra dei gesti, come nel riferimento. Si personalizza in Alfabeto e ricerca: colore (del tema o a scelta),
  icona tra dodici e azione di tocco, pressione lunga e scorrimento in su (ricerca, tutte le app, notifiche, blocca lo schermo,
  impostazioni, apri un'app o niente). Si può togliere del tutto.
- **Impostazioni rifatte**: l'indice spiega cosa c'è in ogni pagina e mostra la configurazione solo se manca qualcosa.
  Orologio, testo, icone, home, alfabeto e ricerca hanno ora pagine separate, con un'anteprima in alto che cambia mentre modifichi.
  Le scelte si vedono invece di essere solo nomi: stili dell'orologio, caratteri, stili e forme delle icone, tema, allineamento.
  Un telefono in miniatura mostra la home; toccandolo o trascinando si sceglie dove inizia.
- **Alfabeto**: si possono regolare la dimensione delle lettere e la distanza tra loro. La prima app della lettera scelta
  va al centro dello schermo, qualunque sia la lettera.
- **Elenco delle app**: in alto le righe svaniscono prima di arrivare alla barra di stato.
- **Player**: la dimensione ora cambia tutto il player, non solo il testo.
- **Popup dell'app**: si chiude con un'animazione.
- **Cartelle**: via la freccetta; molti più simboli e icone prese dagli icon pack, salvate anche nel backup.
- **Cambia icona**: scelta del pack e ricerca restano ferme sopra la griglia e la tastiera non salta più.
- **Notifiche**: anteprima di immagini e sticker, quando l'app li rende disponibili. Le GIF mostrano il primo fotogramma.
  La miniatura sta accanto al testo e non allunga la riga; l'immagine intera si vede nel popup. Nelle impostazioni della home
  si sceglie tra miniatura, solo nel popup e mai.
- **Più messaggi**: la riga dell'app dice "5 messaggi da 2 chat" e sotto l'ultimo messaggio con il nome di chi l'ha scritto.
- **Gesti**: ogni gesto dice dove si fa e cosa fa ora; quelli sempre attivi sono elencati uno per riga.

## [0.10.0]
- **Usage Breaker a promemoria**: per le app scelte, una notifica dopo 5–240 minuti di uso continuo chiede se vuoi
  continuare, senza bloccare nulla. Si aggiunge alla pausa prima di aprire l'app. Richiede l'accesso ai dati di
  utilizzo, le notifiche di Onda e l'accesso alle notifiche, chiesti solo a chi la usa; i dati restano sul telefono.
- **Backup completo in un unico file**: oltre alle impostazioni contiene le immagini delle icone e i font importati.
  Si può anche condividere. Il backup automatico gira ogni 24 ore pure a launcher chiuso (con WorkManager).
  I vecchi backup `.json` restano ripristinabili. I widget vanno ancora aggiunti di nuovo.
- **Popup dell'app personalizzabile**: con "Modifica il popup" si aggiungono app correlate e fino a 4 widget, da sfogliare
  in orizzontale, e si scelgono le scorciatoie da mostrare.
- **App suggerite in home**: opzione per mostrare sotto i preferiti le app usate spesso. Con la pressione lunga,
  "Smetti di suggerire"; dalle impostazioni si può tornare indietro.
- **Alfabeto localizzato**: lettere e ordine seguono le lingue del telefono (per esempio Å Ä Ö in svedese, Ñ in spagnolo).
  I nomi in altri alfabeti vanno sotto la lettera giusta invece che sotto "#"; se la lingua non li copre, da Android 10
  vengono traslitterati.
- **Alfabeto**: si può iniziare a scorrere ovunque lungo il bordo, mentre le lettere restano raccolte in basso.
  In cima c'è una stella che riporta ai preferiti.
- **Meteo senza internet**: la temperatura accanto alla data e le previsioni (24 ore e 7 giorni) arrivano dall'app
  Breezy Weather, se installata. Si attiva nelle impostazioni della home; il permesso viene chiesto solo allora.
- **Foglio agenda**: toccando la data si aprono meteo ed eventi dei prossimi 7 giorni, invece del calendario di sistema.
- **Aggiungi alla home dalle altre app**: quando Chrome, Maps, WhatsApp e altre app chiedono di aggiungere una
  scorciatoia o un widget, Onda chiede conferma e lo mette in home. Le scorciatoie stanno tra i preferiti:
  si riordinano e, con la pressione lunga, si tolgono.
- Corretto: il titolo delle notifiche nel popup dell'app era nero su fondo scuro.
- Il player compare solo quando qualcosa è in riproduzione; dopo una pausa resta un minuto, poi sparisce.
- In fondo all'elenco delle app: le cartelle, poi "Installate di recente" e "Impostazioni di Onda";
  il segno • in fondo all'alfabeto porta lì.
- Alfabeto compatto e ancorato in basso, vicino al pollice: alto quanto le lettere invece di tutto lo schermo.
- Scegliendo una lettera, l'inizio della sezione si porta all'altezza del dito.
- La ricerca non apre più un pannello scuro: si vede sempre lo sfondo, appena velato.
- Feedback aptico ripensato: colpi leggeri e diversi per lettera, inizio/fine alfabeto, soglia dello swipe
  (attivata e disattivata), pressione lunga, apertura del popup ed eliminazione delle notifiche. Usa le
  "vibrazioni leggere" del motore aptico quando ci sono (Android 11+), altrimenti i colpi di sistema;
  segue sempre il feedback al tocco di sistema. Nuova voce "Prova la vibrazione" nelle impostazioni.
- Cartelle: icona personalizzabile (automatica, simbolo, icona di un'app della cartella o immagine);
  più widget nella stessa cartella si sfogliano in un carosello orizzontale; altezza dei widget regolabile
  dalle opzioni della cartella.

## [0.9.0]

### Aggiunto
- **Spazio privato** (Android 15+): contenitore separato in fondo all'elenco, con blocco, sblocco e opzione per nasconderlo. Da bloccato le sue app non sono visibili né cercabili. Richiede il permesso `ACCESS_HIDDEN_PROFILES` (di tipo "normal") e che Onda sia il launcher predefinito.
- **Backup automatico**: una copia al giorno delle impostazioni nella cartella scelta, conservando le ultime 7.
- **Benvenuto al primo avvio**: spiega alfabeto e gesti e propone i permessi; si può saltare e non compare a chi aggiorna.
- **Tablet e orizzontale**: home, elenco e impostazioni restano centrati con una larghezza leggibile; la rotazione non ricrea l'Activity.
- **Gesti configurabili**: swipe su e doppio tocco possono aprire ricerca, elenco, tendina, impostazioni, bloccare lo schermo o aprire un'app a scelta. Schema impostazioni 4: chi aveva "doppio tocco per bloccare" mantiene lo stesso comportamento.
- **Animazioni di apertura delle app**: di sistema, dissolvenza, zoom o dal basso.
- **Riordina la home**: blocchi e preferiti si spostano trascinandoli (dal menu della home).
- **Icone delle singole app**: immagine dalla galleria e forma diversa per ogni app, disponibili anche senza icon pack. L'icona scelta ha la precedenza su pack e stili generati.
- **Riepilogo notifiche**: blocco opzionale della home con il totale delle notifiche e le app da cui arrivano; toccandolo si vedono tutte raggruppate per app e si possono eliminare insieme.
- **Contatti nell'alfabeto**: opzione per mostrare i contatti sotto ogni lettera dell'elenco, dopo le app. L'accesso ai contatti viene chiesto solo quando la si attiva.
- **Usage Breaker**: una pausa regolabile (3–60 secondi) prima di aprire le app scelte, con le aperture di oggi. Si attiva dalle opzioni dell'app; i conteggi restano sul dispositivo.
- **Pile di widget**: più widget nello stesso blocco della home, da sfogliare in orizzontale. Si creano dal foglio di modifica di un widget (pressione lunga).
- **Posizione della home**: si sceglie il punto dello schermo in cui inizia il primo blocco (orologio o widget); gli altri blocchi e i preferiti lo seguono. Si regola dal foglio di modifica di un blocco o dalle impostazioni; il predefinito resta ancorato in basso.

### Modificato
- **Popup dell'app all'altezza della riga** al posto dei fogli dal basso: pressione lunga e swipe a destra mostrano notifiche, scorciatoie, widget dell'app e azioni rapide. "Rispondi" apre il pannello con il campo sopra la tastiera, "Altre opzioni" l'elenco completo.
- **Impostazioni a pagine** (Aspetto, Home, Gesti, Ricerca, App, Backup) con **anteprima dal vivo** di orologio e preferiti nella pagina Aspetto.
- Dopo aver applicato un preset si può **annullare** per qualche secondo; rimuovere un widget chiede conferma.
- "Nascondi nomi" vale anche per le cartelle (il nome resta disponibile per TalkBack).
- Selettore widget con ricerca e widget raggruppati per app.
- Icone diverse per ogni voce dei menu.
- Slider con il valore visibile e "Ripristina" per tornare al predefinito.

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
