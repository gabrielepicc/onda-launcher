# Roadmap di Onda

Legenda: ✅ fatto · 🟡 parziale · ⬜ da fare. Aggiornare a ogni passo concluso.

## Fatto finora (0.1 → 0.8)
- ✅ Home a lista, alfabeto "a onda" su Canvas (lato invisibile opposto), doppio tocco sull'alfabeto per bloccare
- ✅ Elenco app nella stessa pagina, app per lettera ordinate per utilizzo, notifiche anche nell'elenco
- ✅ Swipe righe (home ed elenco): destra = pannello app, sinistra = elimina notifiche; giù = tendina (fisso), su = configurabile
- ✅ Notifiche: anteprima accanto all'icona, pannello con foto contatto, conversazione, azioni, risposta diretta sopra la tastiera
- ✅ Cartelle con app e widget, pannello app con scorciatoie e widget dell'app
- ✅ Home a blocchi ordinabili; orologio (8 stili, dimensione, allineamento), widget (altezza), player (compatto/esteso, dimensione testo)
- ✅ Posizione della home: il primo blocco parte dal punto scelto (o ancorato in basso, predefinito) e il resto lo segue
- ✅ Material You, 12 preset di stile con sfondi procedurali
- ✅ Fase 0: DataStore + kotlinx.serialization con migrazioni, `:core:icons`, stringhe IT/EN, test, CI, F-Droid metadata
- 🟡 Fase 1 icone: ✅ forme adattive, fallback pack (iconback/mask/upon/scale), calendari dinamici, Cambia icona con suggerimenti,
  cache memoria, stili generati (Material, Contorno, Sticker, Acquerello, Marmo, Puntini), classificatore categorie.
  ⬜ cache su disco, "applica a tutta la categoria", forma SVG custom, parametri regolabili degli stili generati

## Prossimi passi (in ordine)
1. ✅ Fase 2 — Testo e orologio: colore testo, allineamento lista, peso e spaziatura, ombra, batteria nell'orologio,
   font importati (.ttf/.otf) per lista e orologio. (Meteo nell'orologio spostato in Fase 4: richiede INTERNET)
2. 🟡 Fase 3 — Temi: ✅ `LookTheme` serializzabile, Theme Studio (salva/applica/rinomina/duplica/elimina), file `.onda-theme`
   con font incorporati, palette dallo sfondo anche su Android 8.1–11. ⬜ QR/deep link, sfondo automatico per orario
3. 🟡 Fase 4 — ✅ agenda del calendario come blocco della home, ✅ blocca layout.
   ✅ widget stack, ✅ meteo letto da Breezy Weather installato, senza permesso INTERNET (Fase 7, passo 2)
4. 🟡 Fase 5 — ✅ prefissi di ricerca (= @ /), ✅ scorciatoie impostazioni di sistema, ✅ motore web selezionabile,
   ✅ Plug & Play cuffie, ✅ app protette con biometria e protezione delle app nascoste.
   ✅ Usage Breaker, ✅ contatti dall'alfabeto, ✅ riepilogo notifiche, ⬜ conversioni unità/valute
5. ✅ Blocco UX/UI (prima della Fase 6, così l'onboarding spiega gesti e menu definitivi):
   ✅ nascondere i nomi anche delle cartelle, ✅ ricerca e raggruppamento nel selettore widget, ✅ icone distinte nei menu,
   ✅ valori e ripristino sugli slider, ✅ popup all'altezza dell'icona al posto dei fogli dal basso (la risposta alle notifiche resta sopra la tastiera),
   ✅ anteprima dal vivo e pagine nelle impostazioni, ✅ riordino trascinando, ✅ modifica completa delle singole icone,
   ✅ annulla dopo un preset e conferma prima di eliminare un widget
6. ✅ Fase 6 — ✅ gesture configurabili estese, ✅ animazioni di apertura app, ✅ landscape/tablet, ✅ Private Space, ✅ onboarding, ✅ backup automatico

7. ✅ Fase 7 — Parità con Niagara, parte per la versione 0.10 (rilasciata). Il resto dell'elenco e le priorità sono in `docs/niagara-gap.md`.
   Fatto (PR #2–#7): ✅ fondo elenco con cartelle, "Installate di recente" e "Impostazioni di Onda" (segno • sull'alfabeto);
   ✅ alfabeto compatto ancorato in basso e lettera allineata al dito; ✅ ricerca sopra lo sfondo (niente pannello nero);
   ✅ feedback aptico a colpi distinti con "Prova la vibrazione"; ✅ player solo con riproduzione attiva (60 s dopo la pausa);
   ✅ cartelle con icona personalizzabile, widget in carosello e altezza regolabile; ✅ titolo delle notifiche leggibile nel popup.

   **Prossimi passi, in ordine** (una voce = un branch = una PR):
   1. ✅ Aggiungi alla home dalle altre app: richieste di pin di scorciatoie e widget (gap 12.2).
   2. ✅ Meteo senza internet da Breezy Weather (gap 12.1, 7.1): orologio + foglio agenda toccando la data (1.5).
   3. ✅ Alfabeto: zona di tocco a tutta altezza con colonna visibile compatta (12.3) e stella dei preferiti in cima (10.1).
   4. ✅ Alfabeto localizzato con `AlphabeticIndex` (2.1).
   5. ✅ App suggerite sotto i preferiti con "Smetti di suggerire" (1.1, 10.9).
   6. ✅ Popup dell'app come cartella: app correlate, widget e pile, scorciatoie nascondibili (3.1, 3.2).
   7. ✅ Usage Breaker a promemoria (6.1) e backup completo con WorkManager (9.1; manca il ricollegamento dei widget).
   8. ✅ Rilascio 0.10: versionCode 10, changelog fastlane 10.txt, roadmap.

   **Prima della prossima versione:** provare sul telefono le voci della 0.10, che finora hanno solo test e CI.

8. 🟡 Dopo la prima prova sul telefono (0.10):
   ✅ alfabeto che parte dal punto toccato; ✅ pulsante di ricerca nell'angolo;
   ✅ impostazioni rifatte: pagine per argomento, anteprima dal vivo su ogni pagina di aspetto, scelte visive, telefono in miniatura;
   ✅ prima app della lettera al centro; ✅ sfumatura in cima all'elenco; ✅ player scalato per intero; ✅ chiusura animata del popup;
   ✅ cartelle senza freccetta, con più simboli e icone dagli icon pack; ✅ ricerca delle icone stabile; ✅ immagini nelle notifiche (GIF ferme);
   ✅ cerca nelle impostazioni, ✅ altezza dell'alfabeto, ✅ stessi selettori visivi nei fogli di modifica della home.

9. ✅ Popup a bolla e controllo con Material 3: bolla che si fora in goccioline, popup con sezioni e menu, forme, testi, tocco e contrasto
   (dettagli e differenze volute in `docs/material-audit.md`). ⬜ Da giudicare sul telefono.

10. ✅ Benessere digitale: dashboard statistica (`data/UsageStatsMath`, `data/UsageStatsRepository`), limiti giornalieri, Usage Breaker su notifiche e scorciatoie.
    ✅ Pausa anche per le aperture fuori dal launcher, tramite il servizio di accessibilità (solo nome del pacchetto, `data/BreakerGate`).

11. ✅ Luci di casa (Wi-Fi + tramonto) e pausa anche fuori dal launcher.
12. ✅ Preferiti contestuali: profili con regole E/O, priorità, slot fissi, scelta a mano di un'ora, profilo riunioni Teams, regole proposte.
    - ✅ Profili suggeriti dai dati d'uso ("Suggerisci profili"): scheda Crea/Modifica/Ignora, silenzio 30 giorni, in home una scheda al massimo ogni 4 ore (elenco completo nelle impostazioni), Wi-Fi come segnale spento di default.

## Fase 8 (specifiche in `docs/fase8.md`)
- Area 5 — Musica, per ogni app con sessione multimediale, senza internet. Decisioni prese: livello "a ritmo" sì, spento di default; sfondo solo nel launcher.
  PR previste, in ordine:
  1. ✅ sfondo animato con i colori della copertina;
  2. ✅ mi piace e prossimo brano (cosa espone Spotify: da verificare sul telefono);
  3. ✅ riproduzione dalla ricerca (`p`, `sp`, prefissi personalizzabili);
  4. ✅ playlist e brani come voci (nei preferiti; cartelle e icona personalizzata dopo);
  5. ✅ ripresa con le cuffie;
  6. ✅ livello "a ritmo" (approvato, spento di default).

13. ✅ Scoperta delle funzioni: benvenuto con "Mostrami", suggerimenti "Lo sapevi?", ricerca delle impostazioni dalla barra.
14. ✅ Onda in bianco e nero a orario; swipe trasparente; home compatta e widget ridimensionabili.

## Gruppi di richieste concluse (novembre 2026)
- ✅ Correzioni widget e cartelle (#64) · Azioni personalizzate ovunque (#65) · Popup in corrispondenza (#66) · Alfabeto e icone (#67) · Palette su due colori (#68).
- ✅ Sfondo musicale ovunque (#70): live wallpaper «solo colori», disegno condiviso con la home, senza microfono. Da verificare sul telefono: batteria e comportamento nella schermata di blocco.

- ✅ Notifiche leggibili e pannello Home (#72) · Spazio dentro l'orologio (#73).

- ✅ Automazioni «se… allora» (motore, pagina, dieci esempi). Da fare dopo: tag NFC come evento, azione «imposta profilo» nel catalogo, notifica per i permessi mancanti.

- ✅ Blocchi delle app a fasce orarie con sospensione nei festivi + azioni «Blocca/Sblocca un\'app» nelle automazioni.

- ✅ Sfondo musicale con immagine di base (#80) · Player coerente (#81) · Pannello Home M3 (#82) · Azioni Launcher nelle automazioni con sostituzioni attive (#83).
- ✅ Passata Material You per area (regole in `docs/design-material-you.md`): Impostazioni (#84), Elenco app e cartelle (#85), Popup e notifiche (#86), Ricerca (#87), Home.

- Documento di lavoro UI/UX di novembre (`docs/prompts-ui-novembre.md`): blocchi 1, 6, 7 e 4c già fatti (#80, #83, #89, #82); blocco 2 (fondamenta) ✅ (#91); blocco 3 (carosello in home) ✅; prossimi: 4a/4b/4d/4e, 5, 8, 9.

## Decisioni prese (modificabili)
- Pressione lunga e swipe a destra aprono un popup all'altezza della riga; il foglio dal basso resta solo per rispondere alle notifiche, così il campo sta sopra la tastiera, e per "Altre opzioni".
- Swipe giù sempre sulla tendina di sistema (come richiesto), non configurabile.
- Spazio privato fuori da alfabeto, ricerca e suggerimenti, in un contenitore separato come chiede Android: si raggiunge da fondo elenco.
- Si lavora su branch + PR, unione solo con CI verde (vedi CLAUDE.md).
- Meteo senza connessione: Onda legge i dati dall'app Breezy Weather (se installata). Open-Meteo diretto resta escluso finché l'utente non dà l'OK al permesso INTERNET.
- App suggerite in home spente di default, per non cambiare da sole la home di chi aggiorna; si attivano nelle impostazioni.
- Promemoria di Usage Breaker con `PACKAGE_USAGE_STATS` e `POST_NOTIFICATIONS`, approvati dal proprietario: il controllo gira nel servizio notifiche già presente, una volta al minuto a schermo acceso e solo con app scelte.
- Plug & Play mostra l'app scelta in cima ai preferiti (non un blocco separato), solo mentre le cuffie sono collegate.
- Pausa fuori dal launcher con il servizio di accessibilità già presente, approvata dal proprietario: legge solo il pacchetto in primo piano, mai il contenuto.
- Luci di casa: widget di Google Home accanto all'orologio quando si è sul Wi-Fi di casa e fa buio. Permesso di posizione approvato dal proprietario, chiesto solo attivando la funzione; nessun dato lascia il telefono.
- Preferiti contestuali: permesso Bluetooth "dispositivi nelle vicinanze" chiesto solo aggiungendo una condizione Bluetooth (richiesta del proprietario); registro delle aperture solo sul telefono, 35 giorni.
- App protette senza blocco schermo configurato: si aprono comunque, con un avviso (non c'è nulla da verificare).
