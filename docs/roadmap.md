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
   ⬜ cerca nelle impostazioni, ⬜ posizione dell'alfabeto, ⬜ stessi selettori visivi nei fogli di modifica della home.

## Decisioni prese (modificabili)
- Pressione lunga e swipe a destra aprono un popup all'altezza della riga; il foglio dal basso resta solo per rispondere alle notifiche, così il campo sta sopra la tastiera, e per "Altre opzioni".
- Swipe giù sempre sulla tendina di sistema (come richiesto), non configurabile.
- Spazio privato fuori da alfabeto, ricerca e suggerimenti, in un contenitore separato come chiede Android: si raggiunge da fondo elenco.
- Si lavora su branch + PR, unione solo con CI verde (vedi CLAUDE.md).
- Meteo senza connessione: Onda legge i dati dall'app Breezy Weather (se installata). Open-Meteo diretto resta escluso finché l'utente non dà l'OK al permesso INTERNET.
- App suggerite in home spente di default, per non cambiare da sole la home di chi aggiorna; si attivano nelle impostazioni.
- Promemoria di Usage Breaker con `PACKAGE_USAGE_STATS` e `POST_NOTIFICATIONS`, approvati dal proprietario: il controllo gira nel servizio notifiche già presente, una volta al minuto a schermo acceso e solo con app scelte.
- Plug & Play mostra l'app scelta in cima ai preferiti (non un blocco separato), solo mentre le cuffie sono collegate.
- App protette senza blocco schermo configurato: si aprono comunque, con un avviso (non c'è nulla da verificare).
