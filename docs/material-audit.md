# Controllo con Material 3

Confronto tra Onda e le linee guida di Material 3, fatto a novembre 2026. Da rifare quando cambia molto l'interfaccia.

## Da dove vengono i valori
Le pagine di m3.material.io si caricano con JavaScript e qui si leggono solo a pezzi, quindi le fonti sono due:

- gli estratti delle linee guida ufficiali, per le regole;
- i token nel codice sorgente di Compose Material 3 (`MotionTokens`, `ShapeTokens`), per i numeri.

Il controllo è stato fatto leggendo il codice e calcolando il contrasto. Nessuna schermata è stata vista su un telefono.

## Regole usate
| Tema | Regola | Fonte |
| --- | --- | --- |
| Forme | Raggi: 0, 4, 8, 12, 16, 20, 28, 32, 48, pieno. Gli elementi annidati usano raggi diversi. | [Scala dei raggi](https://m3.material.io/styles/shape/corner-radius-scale) |
| Testi | Cinque ruoli (display, headline, title, body, label) in tre misure, mai sotto i 10sp. | [Stili](https://m3.material.io/styles) |
| Tocco | Bersagli di almeno 48×48 dp con 8 dp tra l'uno e l'altro; l'icona può essere più piccola. | [Accessibilità](https://m3.material.io/foundations/designing/structure) |
| Fogli | Angoli superiori da 28 dp, larghezza massima 640 dp. | [Fogli dal basso](https://m3.material.io/components/bottom-sheets/overview) |
| Menu | I tre puntini raccolgono azioni collegate, in alto a destra. | [Schede](https://m3.material.io/components/cards/guidelines) |
| Dialoghi | Da usare poco; pulsanti a destra, conferma sul bordo. | [Dialoghi](https://m3.material.io/components/dialogs/guidelines) |
| Movimento | Durate 50, 100, 150, 200, 250, 300, 350, 400 ms e oltre; curve emphasized e standard. | [Transizioni](https://m3.material.io/styles/motion/transitions), `MotionTokens` |
| Contrasto | 4,5:1 per il testo (criterio WCAG AA, non preso dalle pagine di Material). | Valore di riferimento noto |

## Cosa è stato corretto
- **Forme:** i raggi fuori scala (24, 22, 18, 14, 10, 5, 3) ora sono gradini della scala. Scheda e bordo dei widget avevano due raggi diversi.
- **Testi:** le misure scritte a mano in ricerca, scelta dei widget, notifiche e azioni dell'app ora sono ruoli tipografici.
- **Barre in alto:** Impostazioni e Stili usano `titleLarge`.
- **Tocco:** portati a 48 dp chiudi ricerca, invia e scarta notifica, pulsanti piccoli del player, chip delle notifiche ed evento sotto l'orologio.
- **Colori da scegliere:** tutti i campioni hanno bersaglio da 48 dp, ruolo di scelta e descrizione per TalkBack. Prima l'ultimo accento poteva uscire dal riquadro sugli schermi stretti.
- **Contrasto:** nel tema chiaro l'accento era sotto 4,5:1 con acqua (4,3) e sabbia (4,1). Ora è scurito e un test controlla tutti e sei gli accenti.
- **Stato premuto:** il pulsante di ricerca non aveva l'ondina al tocco, ora ce l'ha.
- **Durate:** 140 ms diventa 150 ms (token Short3).
- **Componenti:** le azioni delle notifiche nel pannello usano il chip di Material.

## Cosa resta diverso, e perché
- **Pulsante di ricerca rotondo:** il FAB di Material ha angoli da 16 dp. È rotondo su richiesta del proprietario, come riferimento di comportamento.
- **Popup a bolla:** l'animazione non è un pattern di Material, ed è più vistosa di quanto suggeriscano le linee guida. È una scelta del proprietario. Lo scoppio resta sotto i 500 ms.
- **Colonna dell'alfabeto:** le lettere sono più piccole di 48 dp, ma il tocco si fa su tutta la colonna e la lettera segue il dito.
- **Barre in alto scritte a mano:** hanno misure e titolo di Material, ma non usano il componente `TopAppBar`.
- **Barra di ricerca:** usa `BasicTextField` per stare sopra lo sfondo senza pannello.

## Cosa non è controllabile qui
- **Colori dinamici e testo a colore scelto:** il contrasto dipende dallo sfondo del telefono e dalla scelta dell'utente.
- **Testo sopra lo sfondo:** il contrasto dipende dall'immagine; l'ombra e la velatura aiutano ma non lo garantiscono.
- **Aspetto reale:** tempi, proporzioni e leggibilità vanno giudicati sul telefono.
