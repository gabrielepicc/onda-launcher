# Fase 8 — specifiche

Una sezione per area. Ogni area parte da questo documento (PR di sola documentazione), poi una PR per passo.

---

## Area 5 — Musica

Vale per qualsiasi app musicale che pubblica una **sessione multimediale di Android**: Spotify, YouTube Music,
Poweramp e simili, non solo Spotify.

Vincoli:
- niente librerie di Spotify, niente API web, niente permesso `INTERNET`;
- tutto passa da quello che Android già offre: sessione multimediale, intent di sistema, link `spotify:`.

### Decisioni che spettano al proprietario (in attesa)

| # | Domanda | Proposta | Stato |
|---|---|---|---|
| 1 | Livello "a ritmo di musica": legge l'audio in riproduzione e fa pulsare lo sfondo sui bassi. Richiede il permesso **microfono** (`RECORD_AUDIO`), perché Android lo pretende per l'analizzatore audio anche se non registra nulla dal microfono. Lo facciamo? | Sì, ma sempre spento di default, chiesto solo attivandolo e con una spiegazione prima della richiesta. Nessun audio viene salvato o inviato: si leggono solo le ampiezze delle frequenze, istante per istante. | ⏳ |
| 2 | Lo sfondo animato solo **dentro il launcher**, o anche come **sfondo animato di sistema** (visibile anche nella schermata di blocco)? | Solo nel launcher: consuma meno ed è più semplice. Lo sfondo di sistema richiederebbe un servizio sempre attivo. | ⏳ |

Finché le due risposte non arrivano si lavora solo sui passi che non ne dipendono (PR 1–5). Il livello "a ritmo" è la PR 6.

Le altre scelte sono già fatte nelle richieste del proprietario:
- `androidx.palette` (Apache 2.0, compatibile con GPLv3) come nuova dipendenza;
- "Riprendi la musica" spento di default;
- app musicale preferita predefinita: l'ultima usata.

### 1. Controlli in più nel player

**Comportamento**
- **"Mi piace" e altre azioni dell'app.** Se la sessione espone azioni personalizzate (`PlaybackState.customActions`, es. il cuore di Spotify), compaiono accanto a play/pausa. Usano l'icona fornita dall'app (`CustomAction.icon`, letta dalle risorse del pacchetto) e il suo nome come descrizione per TalkBack. Lo stato pieno/vuoto si vede solo se l'app cambia icona tra i due stati: Android non ha un campo standard per lo stato. Massimo 2 azioni nel player compatto e 3 in quello esteso, nell'ordine dato dall'app.
- **Prossimo brano.** Se la sessione espone la coda (`MediaController.queue`), sotto il titolo compare "Dopo: titolo — artista", preso dall'elemento che segue quello attivo (`activeQueueItemId`). Se la coda manca o è vuota, la riga non compare.
- **Verifica sul telefono, da scrivere nella PR.** Quali azioni personalizzate e quale coda espone davvero Spotify (e YouTube Music, se installata), con screenshot. Dall'esterno non si può sapere in anticipo: Spotify potrebbe non esporre la coda.

**Dati**: nessuno salvato. Tutto viene dalla sessione attiva, già letta da `MediaRepository`.

**Permessi**: nessuno nuovo. L'accesso alle notifiche, già presente, dà accesso alle sessioni.

**Test (JVM)**
- `MediaExtras.nextInQueue(queue, activeId)`: trova l'elemento successivo; vuoto e ultimo elemento danno null.
- `MediaExtras.visibleActions(actions, compact)`: limite e ordine.

### 2. Riproduzione dalla ricerca

**Comportamento**
- **`p <testo>`**: fa partire direttamente brano, artista o playlist con la funzione "riproduci da ricerca" di Android, sull'app musicale preferita.
  - Se quell'app ha una sessione attiva, si usa `TransportControls.playFromSearch(testo, extras)`.
  - Altrimenti si apre l'app con l'intent `MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH`, `SearchManager.QUERY` e `EXTRA_MEDIA_FOCUS` non specificato.
  - Se l'app non supporta nessuna delle due strade, si apre la sua ricerca, se ne ha una, con un avviso.
- **`sp <testo>`**: apre la ricerca dentro Spotify con il link `spotify:search:<testo>`. Se Spotify non è installato, la voce non compare.
- **Risultati**: nella ricerca, appena si scrive il prefisso, la prima voce è "Riproduci «testo» su App" (o "Cerca «testo» in Spotify"); Invio la esegue.
- **Prefissi personalizzabili.** Oggi i prefissi della ricerca (`=`, `@`, `/`) sono fissi nel codice. Questa PR li rende modificabili tutti: nuovo campo `Settings.searchPrefixes`, con i valori di oggi come predefiniti, e una sezione nelle impostazioni della ricerca.

**Dati**
- `Settings.musicApp` (pacchetto; vuoto = l'ultima app che ha avuto una sessione multimediale, salvata in `Settings.lastMusicApp`);
- `Settings.searchPrefixes`.

**Permessi**: nessuno.

**Test (JVM)**
- `SearchPrefixes.parse(query, prefixes)`: riconosce `p`, `sp` e i prefissi esistenti, con e senza spazio, maiuscole e minuscole. Due prefissi uguali non possono convivere.
- `SpotifyLinks.search(testo)`: codifica corretta.

### 3. Playlist e brani come voci del launcher

**Comportamento**
- **Creare una voce.** Nella ricerca, incollando un link di Spotify compare "Aggiungi «Spotify: playlist» al launcher". La voce si può mettere nei preferiti, in una cartella o nell'elenco.
- **Link accettati**: `open.spotify.com/{playlist|album|artist|track|episode|show}/{id}`, anche con `intl-xx/` e parametri `?si=`, e gli URI `spotify:{tipo}:{id}`.
- **Link corti** `spotify.link/…`: si possono risolvere solo con internet. Non si accettano, e un messaggio spiega di incollare il link completo.
- **Toccare la voce** apre il contenuto in Spotify con `ACTION_VIEW` sull'URI `spotify:` e il pacchetto di Spotify. Con l'opzione "Fai partire subito", dopo l'apertura si manda anche play alla sessione di Spotify.
- **Nome modificabile**, predefinito "Spotify · Playlist". Non si può leggere il vero nome senza internet; si propone il testo incollato, se c'è.
- **Icona**: quella di Spotify, oppure una scelta come per le app (stesso selettore di "Cambia icona").
- **Collegamenti "Aggiungi alla schermata Home" creati da Spotify**: arrivano già dal passo "pin di scorciatoie e widget" della Fase 7 (fatto, PR #2–#7). Si verifica che quelli di Spotify funzionino e si scrive il risultato nella PR.

**Dati**: nuova voce nei preferiti con prefisso `MEDIA_PREFIX`, come le scorciatoie (`SHORTCUT_PREFIX`). Si salva in `Settings.mediaShortcuts` (id, tipo, uri, nome, icona) e entra nel backup.

**Permessi**: nessuno.

**Test (JVM)**
- `SpotifyLinks.parse`: tutti i formati sopra, link non validi, link corti rifiutati, id con caratteri non ammessi.
- Andata e ritorno URI → voce → URI.

### 4. Cuffie

**Comportamento**
- **Plug & Play esistente.** Oggi mette l'app scelta in cima ai preferiti quando colleghi le cuffie. Ora si può scegliere anche un dispositivo Bluetooth audio preciso (per nome), oltre a "qualsiasi cuffia".
- **Nuova opzione "Riprendi la musica"**, spenta di default. Al collegamento, Onda manda "play" all'ultima sessione dell'app scelta:
  - se la sessione c'è, con `TransportControls.play()`;
  - altrimenti con un tasto multimediale virtuale tramite `AudioManager.dispatchMediaKeyEvent`, che Android consegna all'ultima app che ha suonato.
  - Non parte mai se è in corso una chiamata o se un'altra app sta già suonando.

**Dati**
- `Settings.plugPlayResume` (false);
- `Settings.plugPlayDevice` (vuoto = qualsiasi).

**Permessi**: per il nome del dispositivo Bluetooth si riusa "Dispositivi nelle vicinanze", già chiesto dai preferiti contestuali e solo se si sceglie un dispositivo.

**Test (JVM)**: `PlugPlay.shouldResume(evento, impostazioni, inChiamata, altraAppSuona)`.

### 5. Sfondo che si muove con la musica

**Comportamento**
- Mentre la musica suona (`MediaRules`: player visibile), dietro la home compare uno strato animato con i colori della copertina. Si può avere anche in elenco e ricerca.
- **Disegno originale, coerente con il nome**: tre o quattro "onde" morbide di colore, sovrapposte e fuse con sfocatura, che scorrono lente e cambiano forma.
- **Quando la musica si ferma**: l'animazione rallenta in 2 secondi e lo strato svanisce nello sfondo in 1,5 secondi.
- **Colori**: da 3 a 5, estratti dalla copertina con `androidx.palette` (vibrant, muted, dark vibrant, dominant). Al cambio di brano passano ai nuovi in circa 1,5 secondi.
- **"Carattere" del brano senza permessi**, stimato dalla copertina (saturazione e luminosità medie, contrasto) e dallo stato di riproduzione:
  - colori accesi e contrastati → movimento più vivace (velocità e turbolenza alte, pulsazione leggera);
  - colori scuri o tenui → movimento lento e ampio;
  - in pausa → velocità zero.
- **Livello "a ritmo di musica"** (solo se approvato, decisione 1): `android.media.audiofx.Visualizer` sulla sessione globale, banda dei bassi (20–150 Hz). La pulsazione segue l'energia dei bassi, smussata.

**Prestazioni e batteria**
- al massimo 30 fotogrammi al secondo;
- si ferma con il launcher non visibile (ciclo di vita), a schermo spento e con il risparmio energetico (`PowerManager.isPowerSaveMode`);
- Android 13+: shader AGSL (`RuntimeShader`) disegnato in un solo passaggio;
- Android 8.1–12: versione semplificata con sfumature radiali che si muovono, senza shader;
- "Rimuovi animazioni" di sistema (scala animazioni 0): colori fissi, niente movimento.

**Leggibilità**
- Lo strato sta sotto la velatura dello sfondo che già esiste.
- La luminosità dello strato è limitata in modo che testo e icone mantengano almeno 4,5:1 sul colore medio, come i temi (`SchemeContrastTest`).
- Test con copertine tutte bianche e tutte nere.

**Impostazioni** (pagina Stile, nuova sezione "Sfondo con la musica")
- **Modalità**: Spento / Solo colori / Colori e movimento (predefinito) / A ritmo di musica (se approvato);
- **Intensità**: cursore;
- **Dove mostrarlo**: home (sì), elenco (no), ricerca (no);
- **Anteprima dal vivo**: con la copertina in riproduzione o, se non suona nulla, con una copertina di esempio disegnata dal codice.

**Dati**
- `Settings.musicBackdrop` (enum `OFF`, `COLORS`, `MOTION`, `BEAT`; predefinito `MOTION`);
- `Settings.musicBackdropIntensity`;
- `Settings.musicBackdropPlaces`.

**Permessi**: nessuno, tranne il microfono per il livello "a ritmo" (decisione 1).

**Test (JVM)**
- `MusicMood.from(colori)`: copertine accese, tenui, nere, bianche, una sola tinta → velocità, turbolenza, luminosità e pulsazione entro i limiti; le copertine bianche e nere non superano il tetto di luminosità.
- `MusicMood.fade(t)`: rallentamento e dissolvenza.
- `BeatTracker.update(bande)` (PR 6): smussatura e soglia.

### "Device Broadcast Status" di Spotify

Le funzioni sopra usano la sessione multimediale e non ne hanno bisogno. Serve solo se la verifica sul telefono (passo 1) mostra che Spotify non aggiorna la sessione mentre il launcher è in primo piano. In quel caso, la prima volta compare una guida per attivarlo in Spotify (Impostazioni → Dispositivi), che si può chiudere senza farlo.

### PR previste

| # | Branch | Contenuto | Dipende da |
|---|---|---|---|
| 0 | `claude/fase8-musica-specifiche` | Questo documento | — |
| 1 | `claude/musica-sfondo` | Colori della copertina + sfondo animato senza permessi | — |
| 2 | `claude/musica-controlli` | "Mi piace" e prossimo brano | verifica sul telefono |
| 3 | `claude/musica-ricerca` | `p`, `sp` e prefissi personalizzabili | — |
| 4 | `claude/musica-voci` | Playlist e brani come voci | — |
| 5 | `claude/musica-cuffie` | Ripresa con le cuffie | — |
| 6 | `claude/musica-ritmo` | Livello "a ritmo" | decisione 1 |

Dopo ogni PR si aggiornano `docs/roadmap.md`, `CHANGELOG.md` e `docs/niagara-gap.md`.
