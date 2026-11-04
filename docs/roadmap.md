# Roadmap di Onda

Legenda: ✅ fatto · 🟡 parziale · ⬜ da fare. Aggiornare a ogni passo concluso.

## Fatto finora (0.1 → 0.9)
- ✅ Home a lista, alfabeto "a onda" su Canvas (lato invisibile opposto), doppio tocco sull'alfabeto per bloccare
- ✅ Elenco app nella stessa pagina, app per lettera ordinate per utilizzo, notifiche anche nell'elenco
- ✅ Swipe righe (home ed elenco): destra = pannello app, sinistra = elimina notifiche; giù = tendina (fisso), su = configurabile
- ✅ Notifiche: anteprima accanto all'icona, pannello con foto contatto, conversazione, azioni, risposta diretta sopra la tastiera
- ✅ Cartelle con app e widget, pannello app con scorciatoie e widget dell'app
- ✅ Home a blocchi ordinabili; orologio (8 stili, dimensione, allineamento), widget (altezza), player (compatto/esteso, dimensione testo)
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
3. 🟡 Fase 4 — ✅ agenda del calendario come blocco della home, ✅ blocca layout, ✅ pile di widget (scorrimento laterale),
   ✅ posizione di partenza della home scelta dall'utente.
   ⬜ meteo Open-Meteo (**in attesa di decisione**: richiede il permesso INTERNET, valutare flavor `offline`)
4. 🟡 Fase 5 — ✅ prefissi di ricerca (= @ /), ✅ scorciatoie impostazioni di sistema, ✅ motore web selezionabile,
   ✅ Plug & Play cuffie, ✅ app protette con biometria e protezione delle app nascoste,
   ✅ pausa consapevole (Usage Breaker), ✅ contatti preferiti dall'alfabeto, ✅ riepilogo notifiche (blocco + consegna a orari).
   ⬜ conversioni unità/valute (le valute richiedono INTERNET: stessa decisione del meteo)
5. ✅ Fase 6 — ✅ gesti configurabili estesi (swipe su e doppio tocco), ✅ animazioni di apertura app,
   ✅ orizzontale/tablet (larghezza leggibile, rotazione), ✅ Spazio privato Android 15, ✅ introduzione al primo avvio,
   ✅ backup automatico giornaliero in una cartella scelta

## Idee successive
- Conversioni di unità offline (valute solo con il meteo/INTERNET)
- Fase 1 restante: cache icone su disco, "applica a tutta la categoria", forme SVG, parametri degli stili generati
- Fase 3 restante: temi via QR/deep link, sfondo automatico per orario

## Decisioni prese (modificabili)
- Il pannello dell'app è un foglio dal basso (non pop-up ancorato) per lasciare spazio alla tastiera nella risposta.
- Swipe giù sempre sulla tendina di sistema (come richiesto), non configurabile.
- Meteo rimandato: aggiungerebbe la prima connessione a internet dell'app; serve l'OK dell'utente.
- Plug & Play mostra l'app scelta in cima ai preferiti (non un blocco separato), solo mentre le cuffie sono collegate.
- App protette senza blocco schermo configurato: si aprono comunque, con un avviso (non c'è nulla da verificare).
- "Usage Breaker" nell'interfaccia si chiama **Pausa consapevole**; il limite giornaliero non blocca l'app, raddoppia la pausa.
- Riepilogo notifiche programmato: usa lo snooze di sistema del NotificationListener (nessuna notifica viene persa).
- Posizione della home: automatica (in basso, default) o frazione dell'altezza dello schermo; tutto segue il primo blocco.
- Introduzione mostrata solo ai nuovi utenti (prima della proposta dei preferiti); rivedibile dalle impostazioni.
