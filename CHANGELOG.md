# Changelog

Tutte le modifiche rilevanti di Onda. Formato basato su [Keep a Changelog](https://keepachangelog.com/it-IT/1.1.0/), versioni secondo [SemVer](https://semver.org/lang/it/).


## [Non rilasciato]
- **Fondamenta di design (blocco 2)**: componenti comuni `SheetScaffold`, `GroupCard`, `SwitchRow`, `SliderRow`, `ChoiceRow` (pulsanti segmentati fino a 4 opzioni), `ActionRow`, `DestructiveRow` in `ui/components/DesignKit.kt`; le sezioni e gli interruttori delle impostazioni li usano già. Ordine standard dei gruppi nei menu e regole aggiornate in `docs/design-material-you.md`.
- **Profili suggeriti anche nelle impostazioni**: in Preferiti contestuali → Suggerisci profili c'è l'elenco completo, una scheda per suggerimento (condizione, app con le icone, «le hai usate insieme in N degli ultimi M giorni»), con Crea, Modifica e Ignora come in home; le schede escono animate. Sezione «Ignorati» con Ripristina, stato vuoto con la data dell'ultima analisi, invito se manca il permesso o la funzione è spenta, e «Analizza adesso» (al massimo ogni 15 minuti).
- **In home un suggerimento ogni 4 ore** invece di uno a settimana: uno per volta, il più forte; il successivo dopo 4 ore dall'ultimo mostrato (o subito dopo Crea/Ignora, se sono già passate). Ignora vale sempre 30 giorni; la notifica resta al massimo settimanale.
- **Material You – Home**: card della home (orologio, player, agenda, suggerimenti) con la forma large dei token e la superficie tonale bassa; tutti i fogli della home (agenda, cartelle, modifica, riordino, riepilogo, menu) su surfaceContainerLow come i fogli di Material 3. Chiude la passata Material You per area.
- **Material You – Ricerca**: barra di ricerca su superficie tonale alta, titoli delle sezioni dei risultati in labelLarge nel colore primario, icone delle azioni in cerchi tonali, righe alte almeno 56 dp con forme dai token.
- **Material You – Popup e notifiche**: titoli delle sezioni del popup e del riepilogo in labelLarge nel colore primario, scorciatoie e notifiche su superfici tonali, icona tonale della riga «Scorciatoie», forme dai token (le card di notifica restano ad angoli retti).
- **Material You – Elenco app e cartelle**: intestazioni delle lettere e delle sezioni in labelLarge (primario sulla lettera attiva), voci del launcher e del foglio della cartella con icone tonali, cartella aperta su superficie tonale alta, forme dai token.
- **Material You – Impostazioni**: sezioni su superficie tonale bassa con titoli in labelLarge nel colore primario, icone delle voci in cerchi tonali, scala delle forme 12/16/28/32; nuove superfici tonali (bassa e più alta) nel tema con contrasto AA verificato. Regole in `docs/design-material-you.md`.
- **Sfondo musicale di sistema senza grigio**: a musica spenta si vede l'immagine di base scelta dall'utente (selettore foto, nessun permesso, inclusa nel backup), ritagliata come lo sfondo normale; con la musica i colori dell'album entrano ed escono in dissolvenza. Senza immagine di base: gradiente dai colori Material You e invito a sceglierla. In impostazioni è spiegato come averlo anche sulla schermata di blocco secondo la versione di Android.
- **Pannello Home in stile Material You**: Widget, Sfondo e Impostazioni con l'indicatore a pillola di Material 3 (64×40, colore secondario tonale, molla alla pressione), etichetta su una riga; voci dell'elenco come ListItem con icona in un cerchio tonale e titoli di sezione nel colore primario.
- **Player coerente**: copertina con angoli concentrici rispetto al contenitore (raggio del contenitore meno il margine) e stesso margine su ogni lato; titolo e artista allineati nella stessa colonna, su una riga con i puntini se non entrano.
- **Azioni «Launcher» nelle automazioni**: dimensione delle icone e del testo, nomi delle app, profilo dei preferiti, bianco e nero, tema, blocchi della home (orologio, agenda, riepilogo, player), spazio tra i blocchi, sfondo musicale, vibrazione.
  - Le automazioni non toccano le impostazioni salvate: aggiungono sostituzioni attive sopra di esse. Con «Ripristina al termine» (acceso di default per collego/scollego, carica, arrivo/uscita) la sostituzione cade da sola quando la condizione finisce, anche dopo un riavvio; per gli orari vale fino a mezzanotte. Una scelta manuale sulla stessa impostazione vince.
  - Esempio «Modalità auto»: icone e testo grandi quando si collega l'auto, come prima quando si scollega.
  - Editor a tutta altezza con schede «Quando», «Allora», «Promemoria» e barra Salva/Annulla/Elimina; selettore delle azioni a sezioni (Launcher, Sistema, App, Scorciatoie) con icone.
- **Blocchi delle app** (Benessere digitale → Blocchi delle app): scegli le app, i giorni e la fascia oraria (anche oltre la mezzanotte) in cui non si aprono; ogni regola può spegnersi nei giorni festivi, riconosciuti dal calendario Google «Festività in Italia» (con l'elenco delle festività nazionali, Pasquetta compresa, se il calendario non c'è). Il blocco vale dal launcher e da fuori (notifiche, recenti, link), tramite l'accessibilità.
- **Azioni «Blocca un'app» e «Sblocca un'app»** nelle automazioni: per esempio «Buonanotte» blocca i social, «Buongiorno» li sblocca. Le app bloccate a mano si sbloccano dallo stesso elenco.
- **Automazioni «se… allora»** (Impostazioni → Automazioni): un evento e cosa fare, tutto sul telefono.
  - Eventi: arrivo o uscita da casa (Wi-Fi di casa), cuffie, caricatore, un dispositivo che si collega o scollega (per esempio il Bluetooth dell'auto), un orario (con giorni), alba e tramonto.
  - Azioni: tutte quelle del catalogo senza schermate (torcia, silenzioso, suoneria attiva, app, scorciatoie come le scene di Google Home o Home Assistant), il passaggio a un profilo per un'ora e un promemoria in notifica.
  - Dieci esempi pronti: arrivo a casa, uscita di casa, buonanotte, buongiorno, modalità auto, cuffie → musica, staccato dal caricatore, tramonto, alba, promemoria a casa.
  - Nuove azioni nel catalogo: «Silenzioso (attiva)» e «Suoneria attiva».
  - Limiti: girano finché Onda è viva in background; Android può impedire di aprire app dallo sfondo.
- **Profili suggeriti più pertinenti**: un'app entra in un profilo solo se in quella fascia si apre almeno il 20% più spesso che nel resto dei giorni lavorativi o del weekend (le app usate sempre, come messaggi e browser, non definiscono più una fascia). La proposta ha la precedenza su «Lo sapevi?». In Impostazioni → Preferiti contestuali una riga spiega cosa ha visto Onda e perché una proposta non c'è ancora.
- **Player compatto**: font un po' più piccolo (19 sp il titolo, 15 sp l'artista).
- **Profili suggeriti, subito**: bastano 6 giorni di storia e 3 giorni di ricorrenza (prima 14 e 5), così con l'accesso ai dati d'uso appena concesso il primo suggerimento può comparire subito (Android conserva circa 10 giorni di aperture). La scheda si ricalcola appena si concede il permesso e, al massimo una volta a settimana, una notifica avvisa del profilo proposto (solo con «Suggerisci profili» acceso e notifiche consentite).
- **Player compatto**: testo più grande, nessun pulsante in più (niente «aggiungi alla playlist» né ripetizione casuale).
- **Notifiche leggibili e pannello Home**: card di notifica ad angoli retti con margini ≥16 dp; titoli su tutto lo spazio libero e su 2 righe prima di troncare; nel popup le notifiche si eliminano anche scorrendo a destra o a sinistra (vibrazione alla soglia e alla conferma, lista che si richiude senza salti); il pannello della pressione lunga in home ha tre azioni di pari larghezza (Widget, Sfondo, Impostazioni), senza «Cerca».
- **Spazio dentro l'orologio**: slider in Impostazioni → Home (0–32 dp, passo 4, predefinito 8) per la distanza tra ora, data ed evento; tolti i margini fissi attorno a orologio e agenda, così la distanza tra i blocchi segue «Spazio tra i blocchi».
- **Sfondo musicale di sistema** (live wallpaper «solo colori»): i colori della copertina e il carattere del brano anche fuori dalla home e nella schermata di blocco, senza microfono né permessi. Impostazioni → Sfondo musicale → «Usa come sfondo di sistema». Il disegno è condiviso con la home (`WaveBlobs`).
- **Correzioni widget e cartelle**: nel carosello di widget lo scorrimento non apre più la modifica (la pressione lunga ferma resta); con l'orologio sostituito da un widget, le luci di casa compaiono comunque in cima alla home.
- **Azioni personalizzate ovunque**: un solo catalogo (torcia, pannello rapido, silenzioso, blocco, screenshot, app recenti, app, scorciatoie delle app, ricerca, elenco app, impostazioni) per gesti, pulsante di ricerca, swipe delle app e delle cartelle, popup e ricerca. Permessi chiesti al momento, con la guida delle impostazioni con limitazioni.
- **Popup in corrispondenza dell'app**: centrato sulla riga e, se c'è posto, di lato; altrimenti sotto o sopra.
- **Alfabeto e icone**: zona di tocco dell'alfabeto più larga e regolabile; icone personalizzabili anche per scorciatoie e web app di Chrome (incluse nel backup).
- **Palette più decisa**: il tema usa due colori, primario e accento (default: colori di sistema), con contrasti WCAG AA verificati da test.
- **Profili suggeriti dall'uso** (Preferiti contestuali → Suggerisci profili, spento di default):
  - Onda guarda quando apri le app (ultime 4 settimane, almeno 14 giorni di storia, app presenti in almeno 5 giorni diversi) e propone un profilo per fascia (mattina, pomeriggio, sera, notte; feriali e weekend), con 3–6 app. Se ha il contesto, anche per cuffie e carica.
  - Una scheda in home con Crea, Modifica e Ignora. Niente si attiva da solo. Dopo "Ignora" la proposta tace per 30 giorni; al massimo una proposta a settimana.
  - Il permesso dei dati d'uso si chiede solo accendendo la funzione, dopo una spiegazione, e si revoca dalla stessa pagina. Tutto resta sul telefono.
  - Il Wi-Fi come segnale è spento di default e ricorda solo un codice del nome della rete.
- **Scoprire le funzioni**:
  - **Benvenuto nuovo** con otto punti (alfabeto, swipe, pressione lunga, ricerca, cartelle, profili, benessere digitale, privacy). Ogni punto ha "Mostrami", che apre le impostazioni sulla voce giusta e la fa lampeggiare.
  - **"Lo sapevi?"**: una scheda piccola in home, al massimo una al giorno, mai durante un gesto o un pannello, con "Mostrami", "Non ora" e "Non mostrare più". Compare quando serve: app aperta spesso fuori dai preferiti, prima notifica, elenco aperto senza alfabeto, prima musica, prime cuffie, la sera, dopo una settimana.
  - **Ricerca delle impostazioni anche dalla barra del launcher** col prefisso `/`: trova le voci di Onda per nome e sinonimi, apre la pagina e la voce lampeggia.
  - **Impostazioni → Suggerimenti**: li si può spegnere, riportare quelli scartati e rivedere il benvenuto.
- **Onda in bianco e nero** (Benessere digitale): a orario (per esempio 22:30–7:00, anche a cavallo della mezzanotte) oppure finché è attivo un profilo dei preferiti.
  - Icone, copertine, widget e sfondo animato diventano in scala di grigi con un passaggio morbido di 1,5 secondi.
  - Vale per Onda; per tutto il telefono, "Anche per tutto il telefono" apre la modalità riposo di sistema (o la correzione colori).
- **Home più compatta e widget ridimensionabili**:
  - **Spazio tra i blocchi**: da 2 a 8 dp tra orologio, widget, player e app, su griglia da 4 dp. Nuovo slider in Impostazioni → Home (da compatto ad ampio), con ripristino; il telefono in miniatura lo segue.
  - **Data attaccata all'orologio**: le cifre non portano più lo spazio vuoto sopra e sotto. Anche i margini dell'orologio sono ridotti.
  - **Widget ridimensionabili in altezza e larghezza**, più "Automatica" (la misura preferita dal widget). Vale per i widget della home, le pile, le cartelle e per quello delle luci accanto all'orologio, che si regola tenendolo premuto.
- **Swipe sulle righe più leggero**:
  - Niente più fondo pieno scuro. La riga che si sposta resta trasparente, con una velatura leggerissima, e si vede sempre lo sfondo.
  - La traccia dietro nasce invisibile e si colora d'accento fino a circa il 22% alla soglia; superata la soglia passa al colore contenitore al 35%, con la vibrazione di sempre.
  - L'icona dell'azione appare con una dissolvenza e si ingrandisce alla soglia. Rilasciando prima, tutto torna trasparente con una molla morbida.
  - Il pulsante di ricerca si fa piccolo e tenue mentre trascini una riga, e in home le ultime righe restano sopra di lui.
- **Sfondo "a ritmo di musica"** (Musica, passo 6, approvato).
  - Con l'analizzatore audio di Android lo sfondo pulsa sui bassi (20–150 Hz) dell'audio in riproduzione.
  - Spento di default; il permesso microfono si chiede solo scegliendolo, dopo una spiegazione.
  - Legge solo l'intensità delle frequenze, mentre suona e Onda si vede: niente viene registrato o salvato.
- **Cuffie** (Musica, passo 5).
  - Plug & Play può valere per cuffie precise, scelte per nome; Android lo dà senza permessi.
  - Nuova opzione "Riprendi la musica", spenta di default: collegando le cuffie riparte l'app scelta, mai durante una chiamata o se qualcosa suona già.
- **Playlist e brani come voci** (Musica, passo 4).
  - Incollando nella ricerca un link di Spotify (playlist, album, artista, brano, episodio, podcast) si aggiunge una voce ai preferiti.
  - Toccandola, il contenuto parte subito se Spotify ha già una sessione, altrimenti si apre la sua pagina.
  - Tenendola premuta si sposta, si rinomina o si toglie.
- **Musica dalla ricerca** (Musica, passo 3).
  - "p testo" fa partire brano, artista o playlist sull'app musicale scelta (o sull'ultima che ha suonato), con il "riproduci da ricerca" di Android.
  - "sp testo" apre la ricerca dentro Spotify.
  - Tutti i prefissi della ricerca (=, @, /, p, sp) si possono cambiare in Impostazioni → Alfabeto e ricerca.
- **Revisione dell'interfaccia, parte 1**:
  - **Scelte**: non si troncano più. Fino a 3 opzioni corte sono chip che vanno a capo interi, altrimenti un elenco con pulsanti radio (es. "Immagini nelle notifiche", "Primo promemoria dopo").
  - **Etichette e titoli**: le etichette dei riquadri vanno su più righe; il titolo delle pagine non ha più "…".
  - **Anteprima fissa**: solo su schermi alti e con testo normale, altrimenti scorre con la pagina e non copre la sezione sotto.
  - **Pagine e editor**: ogni pagina riparte con la barra grande; tastiera e barre di sistema non coprono il contenuto; l'editor dei profili ha la barra di Material e non finisce sotto la barra di stato.
  - **Chip comuni** con margini 16/8 dp e altezza minima 40 dp: azioni delle notifiche, suggerimenti in home.
  - **Benessere digitale**: "Scegli app" per Usage Breaker e promemoria.
  - **TalkBack**: interruttori, caselle e colori ora hanno un nome.
  - **Popup dell'app con notifiche**: prima le notifiche, scorciatoie e widget piegati in una riga "Scorciatoie" in fondo, che si apre sul posto. Si spegne in Impostazioni → Home → Notifiche.
  - **Controllo statico** (`UiRulesTest`): fallisce se nelle impostazioni, nei chip o nei pulsanti un testo si tronca, o se un contenitore con testo ha un'altezza fissa.
- **Player: azioni dell'app e prossimo brano** (Musica, passo 2).
  - Se l'app mette azioni in più nella sessione (es. il cuore "Mi piace" di Spotify), compaiono accanto ai comandi, con la sua icona: una nel player compatto, fino a tre in quello esteso.
  - Se l'app espone la coda, sotto l'artista compare "Dopo: titolo — artista".
- **Sfondo con la musica** (Fase 8, Musica, passo 1). Mentre suona qualcosa, dietro la home si muovono onde morbide con 3–5 colori presi dalla copertina.
  - Il movimento segue il "carattere" del brano, stimato dalla copertina: accesa e contrastata vuol dire più vivace, scura o tenua più lenta.
  - Al cambio di brano i colori cambiano in 1,5 secondi; quando la musica si ferma, le onde rallentano e svaniscono.
  - Consumi: al massimo 30 fps, si ferma con Onda non visibile, con il risparmio energetico e con "Rimuovi animazioni".
  - Shader su Android 13+, sfumature sulle versioni precedenti.
  - Resta sotto la velatura, con colori sempre leggibili sotto il testo.
  - Impostazioni → Stile: modalità, intensità, dove mostrarlo, anteprima.
- **Impostazioni secondo Material You**:
  - **Barra in alto**: grande, si riduce scorrendo.
  - **Voci**: tutte uguali, con titolo a 16sp, supporto a 14sp, 56/72 dp di altezza e l'interruttore che si attiva toccando tutta la riga.
  - **Sezioni**: titoli a 14sp nel colore d'accento, spiegazioni sempre nello stesso stile, stato dei permessi con l'icona a destra.
  - Le pagine Benessere digitale e Preferiti contestuali usano gli stessi componenti.
- **Pannello della pressione lunga sulla home**:
  - In cima, quattro riquadri: Widget, Sfondo, Cerca, Impostazioni.
  - Sotto, le sezioni "Aggiungi alla home" e "Disposizione", con spiegazioni brevi e un interruttore per bloccare la disposizione.
- **Luci di casa più chiare e affidabili**:
  - "Wi-Fi di casa" chiede conferma ("Sei collegato a «rete». Lo uso come Wi-Fi di casa?") e mostra la rete salvata.
  - Un riquadro "Adesso" dice in diretta Wi-Fi attuale, se sei a casa, quando è buio, se il widget è scelto e il primo motivo per cui non compare.
  - Il nome del Wi-Fi si legge nel modo che funziona da Android 12. Se manca la posizione Onda la chiede alla rete; finché non c'è, il buio è dalle 19 alle 7.
- **Riavvia launcher** in fondo alle impostazioni, con conferma.
- **Preferiti contestuali** (Impostazioni → Preferiti contestuali):
  - **Condizioni**: Wi-Fi, dispositivo Bluetooth, luogo approssimativo, fascia oraria, evento in calendario, carica, cuffie e Non disturbare, anche al contrario.
  - **Regole**: E dentro un gruppo, O tra i gruppi. Tra più profili validi vince la priorità più alta, a parità il primo. Niente deduzioni nascoste.
  - **Slot fissi**: i primi preferiti (2 di base) non cambiano mai; gli altri seguono il profilo.
  - **Profilo da calendario pronto**: durante un evento con un link di Teams mostra Teams, OneNote e il registratore.
  - **Cambio silenzioso**: al massimo cambia l'accento. Scorrendo di lato sull'orologio scegli il profilo a mano per un'ora, poi si torna all'automatico.
  - **Regole proposte**: Onda nota le abitudini che si ripetono (stessa app, stesso giorno e ora, 3 settimane su 4) e propone una regola, senza crearla da solo.
- **Luci di casa**: quando il telefono è sul Wi-Fi di casa e sta facendo buio (dal tramonto, calcolato sul telefono), accanto
  all'orologio compare il widget di Google Home scelto. Si attiva in Impostazioni → Home; chiede la posizione solo attivandolo.
- **Pausa anche fuori dal launcher**: con l'accessibilità di Onda attiva, Usage Breaker e i limiti giornalieri mostrano la pausa anche
  aprendo l'app dalla tendina di sistema, dalle app recenti o da un link. Onda vede solo quale app è aperta, mai il contenuto.
- **Più fluida**: profilo di base per compilare Onda in anticipo all'installazione, righe che non si ricompongono a ogni pixel
  mentre si scorrono di lato, elenco con tipi di elemento per riciclare meglio le righe.
- **Benessere digitale**, nuova pagina delle impostazioni:
  - **Giornata**: tempo davanti allo schermo e confronto con la media, sblocchi, aperture e notifiche ricevute.
  - **Grafici**: ultimi 7 giorni con la media tratteggiata, uso ora per ora (si tocca una barra per vedere quel giorno o quell'ora).
  - **Sintesi**: media, variazione sulla settimana prima, sessione tipica, controlli rapidi, uso notturno, primo utilizzo, giorno più intenso, tempo per sblocco.
  - **App**: elenco per tempo con aperture e sessione più lunga. Toccandone una si imposta un limite giornaliero: superato, l'app passa dalla pausa e, se la stai usando, arriva un avviso. Non viene mai bloccata.
  - Usage Breaker e promemoria sono spostati qui.
  - Tutti i dati restano sul telefono; per le notifiche si salva solo un numero per app e per giorno.
- **Pausa di Usage Breaker** con conto alla rovescia circolare, aperture e tempo di oggi, e stato del limite.
- **Popup con il movimento di Material 3**: cresce dall'icona con una molla appena elastica, il contenuto entra a cascata, esce in 200 ms
  rimpicciolendosi verso l'icona. Tirato oltre il bordo si rimpicciolisce come con il gesto Indietro. Via la bolla.
- **Tocco sulla notifica**: l'anteprima sotto il nome apre la notifica (prima apriva il popup); il popup resta su swipe a destra e pressione lunga.
- **Usage Breaker anche su notifiche e scorciatoie** aperte dal launcher, non solo sull'app.
- **Build più veloci**: Gradle con cache e moduli in parallelo.
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
