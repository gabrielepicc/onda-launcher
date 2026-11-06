# Prompt per la Fase 8 — passaggio di sessione

Ordine di priorità: a → f. Una voce = un branch `claude/<tema>` = una PR, sempre da `main` aggiornato (niente PR impilate).
Prima di ogni tema leggere solo la sua sezione di `docs/niagara-gap.md` e `docs/roadmap.md`.
Stato al momento della scrittura: vedi `CHANGELOG.md` (sezione "Non rilasciato") per ciò che è già unito.

## (a) Revisione UI
**Obiettivo.** Nessun testo tagliato, spazi proporzionati, widget ridimensionabili, etichette ben spaziate, home leggibile.
**Vincoli.** Solo componenti comuni (`ui/components`, `SettingsKit`); nessun test di screenshot (c'è `UiRulesTest`, controllo statico); target ≥ 48dp; testi in `values/` e `values-it/`.
**Passi.**
1. Testi tagliati: niente `maxLines = 1`/`Ellipsis` in impostazioni, chip e pulsanti (salvo commento `troncamento voluto`); niente `.height(Xdp)` fisso su contenitori con testo (salvo `altezza fissa voluta`).
2. Spazi troppo grandi con gli slider: spaziatura dei blocchi della home regolabile (`homeBlockSpacing`, 8dp di default), cifre dell'orologio strette.
3. Widget ridimensionabili: larghezza/altezza o "Automatica" (`WidgetSize`) per home, pila, cartella, luci.
4. Padding delle etichette di notifica coerente nel popup e nelle righe.
5. Tab di scorrimento (alfabeto) semitrasparente, senza fondo pieno; altezza che non copre le righe.
6. Popup dell'app: se ci sono notifiche mostra solo quelle, scorciatoie piegate in una riga "Scorciatoie"; interruttore "Nascondi le scorciatoie quando ci sono notifiche" (attivo di default); il widget dell'app segue la stessa regola.
7. Pulsante di ricerca in basso a destra: non copre il testo delle righe; si rimpicciolisce durante lo swipe; padding in basso della home 88dp.
**Decisioni aperte.** Se estendere lo stesso controllo statico ai dialoghi; se tenere o togliere l'animazione del pulsante di ricerca su schermi piccoli.

## (b) Fase 8: riepilogo notifiche, profili contestuali, ricerca a riga di comando, gesti per app
**Obiettivo.** Chiudere i quattro filoni della Fase 8 come da roadmap.
**Vincoli.** Nessun dato lascia il telefono; niente `INTERNET`; permessi just-in-time con spiegazione; ogni filone una PR.
**Passi.**
1. Riepilogo notifiche: raggruppare per app e per ora, tocco sulla notifica apre la notifica, swipe a destra apre il popup.
2. Profili contestuali: motore E/O, priorità, override manuale temporaneo, transizione silenziosa, profilo da calendario (già fatti: verificare sul telefono e coprire i casi mancanti di `niagara-gap.md`).
3. Ricerca a riga di comando: prefissi (`p`, `sp`, `/` per le impostazioni di Onda), risultati azione prima delle app.
4. Gesti per app: swipe/tap lungo configurabili per singola app.
**Decisioni aperte.** Sintassi definitiva dei prefissi; se i gesti per app usano la stessa tabella delle scorciatoie.

## (c) Area Musica
**Obiettivo.** Controllo della musica (Spotify e altri player) dal launcher.
**Vincoli.** MediaSession/NotificationListener, nessuna API di rete; il microfono (`RECORD_AUDIO`) solo se l'utente accende il livello del ritmo; lo sfondo animato resta solo nel launcher, non sul fondo di sistema.
**Passi.**
1. Mi piace (azione personalizzata della sessione) e brano successivo dal widget e dal popup.
2. Riproduzione dalla ricerca (`playFromSearch`/`playFromUri`).
3. Playlist come voci del launcher (ricerca e scorciatoie).
4. Ripresa con le cuffie (Plug & Play): l'app scelta compare in cima e riparte.
5. Sfondo che si anima con i colori dell'album (Palette) e, se acceso, a tempo di musica.
**Decisioni aperte.** Livello del ritmo da microfono sì/no (spento di default); comportamento quando il player non espone "mi piace".

## (d) Bianco e nero a orario
**Obiettivo.** Scala di grigi nel launcher a un orario o per profilo.
**Vincoli.** Modalità OFF / A ORARIO / PER PROFILO; default 22:30–7:00; link "Anche per tutto il telefono" (impostazione di sistema, che resta all'utente); finestre di dialogo e fogli restano a colori (limite noto).
**Passi.** Modificatore `desaturate`, `GrayscaleSchedule` pura con test, pagina nelle impostazioni, collegamento al profilo contestuale.
**Decisioni aperte.** Nomi degli intent di sistema per il "tempo di riposo" (indovinati con ripiego, da verificare sul telefono).

## (e) Scoperta delle funzioni
**Obiettivo.** Chi apre l'app la prima volta capisce cosa può fare.
**Vincoli.** Testi IT/EN; nessun contatore remoto; i suggerimenti "Lo sapevi?" compaiono pochi, non quando l'utente è occupato.
**Passi.**
1. Benvenuto con "Mostrami" che apre l'impostazione giusta e la evidenzia (`flashIfTarget`).
2. Suggerimenti `DiscoveryTips` in home, uno alla volta, chiudibili.
3. Ricerca "/" che mostra le impostazioni di Onda; gruppo "Suggerimenti" nelle impostazioni.
**Decisioni aperte.** Aggiungere dimostrazioni animate dei gesti nel benvenuto (oggi non ci sono).

## (f) Profili suggeriti dai dati d'uso
**Obiettivo.** Onda propone profili contestuali ricavati da quando e dove si aprono le app.
**Vincoli.**
- Dati solo in locale; permesso "Accesso ai dati d'uso" chiesto solo all'apertura della funzione ("Suggerisci profili"), dopo una spiegazione, revocabile.
- Mai attivare profili da soli: la scheda offre Crea / Modifica / Ignora.
- Al massimo un suggerimento a settimana; dopo "Ignora" silenzio per 30 giorni.
- Logica pura testata in `data/UsageProfiles.kt`; lettura in `service/UsageReader.kt`.
- Il Wi-Fi come segnale resta disattivato di default (si ricorda solo un codice del nome della rete, senza posizione).
**Passi.** Costanti 14–28 giorni e ≥ 5 giorni di ricorrenza; dataset sintetici (vuoto, routine, rumore); campi in `Settings` con default; scheda in home; voce in Preferiti contestuali; aggiornare `niagara-gap.md` e roadmap.
**Decisioni aperte.** Segnale "in auto" (oggi non rilevato); quanto a lungo tenere il registro di Onda (35 giorni) dato che Android conserva gli eventi dettagliati solo per una decina di giorni.
