# Roadmap di Onda

Legenda: ✅ fatto · 🟡 parziale · ⬜ da fare. Aggiornare a ogni passo concluso.

## Fatto finora (0.1 → 0.6)
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
1. ⬜ Fase 2 — Testo e orologio: colore testo personalizzabile, allineamento lista (sx/centro/dx), peso e spaziatura font,
   ombra del testo, batteria nell'orologio, font importati da file (.ttf/.otf via SAF) per lista/orologio/UI
2. ⬜ Fase 3 — Temi: modello `Theme` serializzabile, Theme Studio (crea/duplica/modifica), export `.onda-theme`, tema automatico
   dalla palette dello sfondo anche sotto Android 12, cambio sfondo per orario
3. ⬜ Fase 4 — Widget stack, pop-up avanzati, agenda calendario nativa, meteo Open-Meteo (richiede INTERNET: valutare flavor offline),
   blocca layout
4. ⬜ Fase 5 — Benessere (Usage Breaker), Plug & Play cuffie, contatti dall'alfabeto, riepilogo notifiche, prefissi di ricerca,
   ricerca estesa, app protette con biometria
5. ⬜ Fase 6 — Gesture configurabili estese, animazioni di apertura app, landscape/tablet, Private Space, onboarding, backup automatico

## Decisioni prese (modificabili)
- Il pannello dell'app è un foglio dal basso (non pop-up ancorato) per lasciare spazio alla tastiera nella risposta.
- Swipe giù sempre sulla tendina di sistema (come richiesto), non configurabile.
