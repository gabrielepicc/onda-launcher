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
   ✅ widget stack, ⬜ meteo Open-Meteo (**in attesa di decisione**: richiede il permesso INTERNET, valutare flavor `offline`)
4. 🟡 Fase 5 — ✅ prefissi di ricerca (= @ /), ✅ scorciatoie impostazioni di sistema, ✅ motore web selezionabile,
   ✅ Plug & Play cuffie, ✅ app protette con biometria e protezione delle app nascoste.
   ✅ Usage Breaker, ✅ contatti dall'alfabeto, ✅ riepilogo notifiche, ⬜ conversioni unità/valute
5. ✅ Blocco UX/UI (prima della Fase 6, così l'onboarding spiega gesti e menu definitivi):
   ✅ nascondere i nomi anche delle cartelle, ✅ ricerca e raggruppamento nel selettore widget, ✅ icone distinte nei menu,
   ✅ valori e ripristino sugli slider, ✅ popup all'altezza dell'icona al posto dei fogli dal basso (la risposta alle notifiche resta sopra la tastiera),
   ✅ anteprima dal vivo e pagine nelle impostazioni, ✅ riordino trascinando, ✅ modifica completa delle singole icone,
   ✅ annulla dopo un preset e conferma prima di eliminare un widget
6. ✅ Fase 6 — ✅ gesture configurabili estese, ✅ animazioni di apertura app, ✅ landscape/tablet, ✅ Private Space, ✅ onboarding, ✅ backup automatico

7. ⬜ Fase 7 — Parità con Niagara: elenco dettagliato e priorità in `docs/niagara-gap.md`.
   Prima: alfabeto localizzato, suggerimenti nella home, popup dell'app come cartella, Usage Breaker a promemoria,
   backup completo (impostazioni + icone + font + temi, pianificato con WorkManager), taratura dei gesti.

## Decisioni prese (modificabili)
- Pressione lunga e swipe a destra aprono un popup all'altezza della riga; il foglio dal basso resta solo per rispondere alle notifiche, così il campo sta sopra la tastiera, e per "Altre opzioni".
- Swipe giù sempre sulla tendina di sistema (come richiesto), non configurabile.
- Spazio privato fuori da alfabeto, ricerca e suggerimenti, in un contenitore separato come chiede Android: si raggiunge da fondo elenco.
- Si lavora su branch + PR, unione solo con CI verde (vedi CLAUDE.md).
- Meteo rimandato: aggiungerebbe la prima connessione a internet dell'app; serve l'OK dell'utente.
- Plug & Play mostra l'app scelta in cima ai preferiti (non un blocco separato), solo mentre le cuffie sono collegate.
- App protette senza blocco schermo configurato: si aprono comunque, con un avviso (non c'è nulla da verificare).
