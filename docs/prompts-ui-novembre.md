DOCUMENTO DI LAVORO UI/UX DI ONDA (novembre)
Prima di tutto: salva questo testo così com'è in docs/prompts-ui-novembre.md (PR solo documentazione, unita con CI verde).
Poi lavora UN BLOCCO per sessione, nell'ordine, leggendo solo la sezione del blocco. Regole valide per tutti i blocchi:
- Token: grep prima, niente file interi >300 righe. Il codice nuovo va in file nuovi per area (non ingrandire HomeScreen.kt).
- Riusa i componenti comuni e il catalogo azioni. Stringhe in values/ e values-it/. Target ≥48dp, contentDescription.
- Logica pura in data/ o util/ con test. Nuovi campi con default; migrazione (CURRENT_SCHEMA + test) solo se cambia il significato di un campo.
- Angoli concentrici: raggio interno = raggio esterno − padding. Vibrazione solo con LocalOndaHaptics.
- Una PR per blocco, da main aggiornato, merge con CI verde, poi roadmap e CHANGELOG. Risposta breve: cosa è cambiato e cosa provare sul telefono.

BLOCCO 1 – Sfondo dinamico di sistema: a musica spenta compare un grigio (BUG)
- Verifica la causa nel servizio del live wallpaper: probabile gradiente di ripiego perché manca l'immagine di base.
- Correzione: all'attivazione l'utente sceglie l'immagine di base (selettore foto di sistema, nessun permesso), copiata nello storage
  e inclusa nel backup. A musica spenta il servizio disegna QUELL'immagine (stesso ritaglio dello sfondo normale); con musica
  applica sopra colori/animazione dell'album con dissolvenza in entrata e uscita. Senza immagine di base: gradiente dai colori
  Material You, mai grigio fisso, con invito a sceglierla.
- Vale su home e blocco quando l'API lo permette (setWallpaperComponentWithFlags, API 34); altrimenti spiega cosa è possibile.
  Scelta dello stato (base/musica/ripiego) in funzione pura testata.

BLOCCO 2 – Fondamenta di design (prima delle altre schermate)
- Scrivi docs/design-material-you.md (max 40 righe) e i componenti comuni in ui/components (riusa quelli esistenti):
  SheetScaffold (maniglia, intestazione fissa con icona+nome, corpo che scorre, altezza max ~90%),
  GroupCard (scheda tonale con titolo del gruppo in labelLarge/primary), SwitchRow, SliderRow (etichetta a sinistra, valore a destra,
  Ripristina, tacche dove i valori sono discreti), ChoiceRow (SegmentedButton se ≤4 opzioni; niente lunghe liste di RadioButton),
  ActionRow (icona in cerchio tonale 40dp, titolo su una riga, testo di supporto, chevron), DestructiveRow (colore errore, conferma).
- Ordine standard dei gruppi in ogni menu: Identità → Azioni frequenti → Aspetto/Dimensioni → Comportamento → Avanzate → Pericolo (in fondo).
- Regole: superfici tonali (surfaceContainer*) invece di riquadri uguali, scala delle forme coerente, tipografia M3, contrasto AA
  in chiaro/scuro, animazioni a molla brevi, nessun colore fisso fuori dal tema.

BLOCCO 3 – Home: player, agenda e riepilogo notifiche in un CAROSELLO orizzontale
- Oggi sono tre righe separate con forme diverse (il riepilogo ha gli angoli quadrati). Sostituiscile con un solo blocco "Schede"
  a scorrimento orizzontale (HorizontalPager, snap), con un accenno della scheda successiva e indicatore a puntini (nascosto con una sola pagina).
  Ordine delle pagine = ordine attuale dei blocchi; compaiono solo le schede con contenuto (player solo se in riproduzione, per MediaRules).
  Quando parte la musica, il carosello va al player con animazione, senza rubare lo scorrimento se l'utente sta toccando.
  Interruttore in impostazioni "Schede in carosello" (default sì; spento = le tre righe di prima), nessuna migrazione.
- Stile: parti da quello del player come riferimento. Tutte le schede stessa altezza e stessa forma (angoli concentrici),
  elemento a sinistra in tessera arrotondata, titolo + testo di supporto su una riga, azione a destra:
  Player: copertina, titolo/artista allineati, comandi. Agenda: tessera con giorno e mese, prossimo evento e ora; senza eventi,
  una riga breve ("Nessun evento · tocca per aprire il calendario"). Riepilogo: fila di icone sovrapposte (max 4) con conteggio,
  titolo "4 notifiche", testo con i NOMI delle app (mai il pacchetto: oggi compare "com.google.android.gms"), tocco per aprire.
- Pressione lunga sulla scheda = stesso comportamento di modifica blocco di oggi.

BLOCCO 4 – Menu rifatti (usano i componenti del blocco 2)
4a Popup dell'app: via il menu ⋮ a tendina (copre le scorciatoie). Intestazione icona+nome+"Apri app"; fila di 3 azioni tonali con etichetta
   (Preferiti con stella, Info app, Altro). Con notifiche: solo notifiche (niente scorciatoie). Senza: scorciatoie su una riga ciascuna, max 4 +
   "Altre scorciatoie". Si apre in corrispondenza dell'app quando c'è spazio (sopra/sotto come ripiego). Notifiche con swipe a destra o sinistra per eliminare.
4b "Altre opzioni": bottom sheet a gruppi.
   Organizza: Preferiti (interruttore), Sposta in cartella, Rinomina, Cambia icona.
   Comportamento: Azione dello swipe (valore attuale come testo di supporto), Notifiche in home (interruttore), Promemoria (interruttore; chiarisci cosa ricorda).
   Privacy e benessere: Proteggi con sblocco, Pausa prima di aprire (Usage Breaker), Nascondi.
   Aggiungi: Widget di quest'app, Scorciatoia sulla home (solo se esistono). Sistema: Info app, Disinstalla (rosso, con conferma).
   Niente verbi negativi ("Non mostrare…"): interruttori con nome positivo. Funzione pura che decide quali voci compaiono, con test.
4c Pannello della pressione lunga sulla Home: via il pulsante Cerca. Widget, Sfondo, Impostazioni come tre colonne con icona in pillola 64×40dp
   (secondaryContainer) ed etichetta sotto; sotto, ListItem con icone tonali e titoli di sezione in primary.
4d Menu cartella: intestazione con icona (tocco = cambia) e nome (tocco = rinomina). Gruppi: Cartella (Rinomina, Icona, Azione dello swipe a destra
   con valore attuale, Sposta su/giù come due pulsanti tonali con etichetta), Widget nella cartella: UNA riga per widget (icona, nome, tocco = suo menu)
   + "Aggiungi widget". Altezza e larghezza non stanno più qui (oggi non si capisce a quale widget si riferiscano). Elimina cartella in fondo.
4e Menu widget (singolo): intestazione col nome del widget. Gruppi:
   Dimensioni: Automatico (interruttore; se acceso gli slider sono nascosti con una riga che spiega perché), Altezza (SliderRow con dp e Ripristina),
     Larghezza (SliderRow a tacche: Piena / ¾ / ½).
   Posizione: "Ordine nella home" con Su/Giù (pulsanti tonali con etichetta) e "Altezza sullo schermo" come ChoiceRow Automatica (in basso) | Scelta,
     con lo slider solo se "Scelta". Unifica i due titoli attuali "Position"/"Position on screen", ora confusi.
   Azioni: Impila un altro widget, Sostituisci widget, Rimuovi widget (rosso, con conferma).
   Mentre si trascina uno slider il pannello si abbassa/diventa traslucido per vedere il widget cambiare dal vivo.

BLOCCO 5 – Selettore widget
- Cerca anche per nome dell'app. In alto: Usati di recente / Suggeriti (dalle app preferite).
- Elenco raggruppato per APP: intestazione fissa con icona e nome dell'app (e numero di widget); dentro, una fila orizzontale scorrevole
  di anteprime con nome e dimensione (es. "4×2"), al posto di una anteprima per riga con metà schermo vuota.
- Barra alfabeto a destra per navigare tra le app, riusando AlphabetWaveScroller (stesse vibrazioni, lettera all'altezza del dito;
  alfabeto localizzato se già presente). Anteprime caricate in modo pigro (solo quelle visibili), con segnaposto e cache.
- Tocco su un widget: aggiunta; pressione lunga: dettagli (dimensioni minime). Stato vuoto della ricerca chiaro.

BLOCCO 6 – Automazioni
- La sezione "Launcher" di "Aggiungi un'azione" è vuota: riempila con impostazioni di Onda: dimensione icone (piccola/media/grande),
  dimensione testo, etichette delle app, profilo dei preferiti, bianco e nero, tema chiaro/scuro, blocchi della home (orologio, schede, agenda),
  spazio tra i blocchi, sfondo musicale, vibrazione. Caso guida: Bluetooth dell'auto collegato → icone e testo grandi; scollegato → torna come prima.
- Le automazioni NON modificano le impostazioni salvate: strato di sostituzioni attive (AutomationOverrides, come profileOverride):
  impostazioni efficaci = salvate + sostituzioni. Opzione "Ripristina al termine" (attiva di default per coppie connetti/scollega, carica/scollega);
  la sostituzione cade da sola anche dopo riavvio o processo ucciso. Test: più regole insieme (vince l'ultima), scadenza, ripristino, scelta manuale.
- Editor: schermata a tutta altezza con tre schede tonali, "Quando" (chip M3 per Dispositivo/Carica/Orario/Sole), "Allora" (azioni con icona e valore,
  Rimuovi con icona), "Promemoria". Barra in basso: Salva (pieno), Annulla (testo), Elimina (testo rosso). Titoli azione su una riga + descrizione
  ("Suono attivo" + "Torna alla suoneria normale"). Selettore azioni a sezioni: Launcher / Sistema / App / Scorciatoie.

BLOCCO 7 – Profili suggeriti anche nelle impostazioni (Preferiti contestuali)
- Al posto di "N suggerimenti pronti: vedrai la scheda in home": un elenco di schede (riusa il composable della home): nome proposto, fascia/condizione,
  icone delle 3–6 app, perché ("usate insieme in 9 degli ultimi 14 giorni"). Azioni Crea / Modifica / Ignora (Ignora = silenzio 30 giorni).
  Più schede insieme; dopo Crea/Ignora la scheda esce con animazione. Sezione compressa "Ignorati" con Ripristina. Stato vuoto chiaro con data dell'ultima
  analisi; se permesso o interruttore mancano, invito ad attivarli. "Analizza adesso" (max una volta ogni 15 min).
- Home: la scheda del suggerimento compare al massimo una volta ogni 4 ore (SUGGESTION_MIN_GAP_MS), una sola per volta (la più forte), mai attivazione automatica.
  Il conteggio parte da quando è stata mostrata. Se la regola "una a settimana" è già nel codice o nei documenti, sostituiscila. Test sulla regola.

BLOCCO 8 – Coerenza di tutte le impostazioni e menu, UNA AREA PER SESSIONE (dopo i blocchi 1–7)
- Ordine: Impostazioni, Elenco app/cartelle, Notifiche/popup, Ricerca, Home. Per ciascuna: riorganizza le voci per compito (non per implementazione),
  nell'ordine standard del blocco 2; accorpa le voci simili; testi di supporto brevi; valore attuale visibile; "Ripristina" per gruppo;
  SegmentedButton al posto dei RadioButton; slider con valore numerico; sezioni con titoli chiari; ricerca nelle impostazioni se non c'è già.
- Solo aspetto e ordine, nessun comportamento cambia. Ogni PR cita in 2 righe cosa è stato spostato e perché.
blocco 9:  "Ricerca: app più cercate" (da main aggiornato; leggi solo ui/search e i file collegati).

Oggi lo stato vuoto della ricerca mostra "Usate di frequente" (8 app). Cambia così:

1. Fonte dei dati: conta quante volte l'utente apre un'app DALLA RICERCA (tocco su un risultato o Invio sul primo risultato).
   Salva solo pacchetto, conteggio e data dell'ultima volta, in locale. Nessun testo digitato, nessun dato fuori dal dispositivo.
2. Classifica (funzione pura in data/SearchRanking.kt, con test): punteggio con decadimento nel tempo (mezza vita ~14 giorni),
   a parità vince la più recente. Escludi app nascoste, protette con sblocco e Spazio privato. Soglia minima: 2 ricerche prima che un'app entri.
3. Mostra poche voci: 4 di default. In Impostazioni → Ricerca uno slider "Suggerimenti nella ricerca" da 0 a 8 (0 = nessuna lista, resta solo il suggerimento
   dei comandi); campo con default in Settings, nessuna migrazione.
4. Titolo della sezione: "Cercate spesso". Finché non ci sono abbastanza dati (meno di 2 app sopra soglia) mostra le più usate come ripiego
   con il titolo "Usate di frequente", così la schermata non è mai vuota.
5. Stesso punteggio anche per ordinare i risultati mentre si digita, come spareggio: a parità di corrispondenza, l'app che cerchi più spesso sta più in alto.
6. Impostazioni → Ricerca: pulsante "Cancella cronologia delle ricerche" (con conferma). La cronologia NON entra nel backup.
7. Il testo "Tip: = per calcolare, @ per i contatti, / per le impostazioni" resta sotto la lista, in una riga più compatta.
8. Stringhe in values/ e values-it/, target ≥48dp. Roadmap e CHANGELOG a CI verde. Risposta breve: cosa provare sul telefono.
