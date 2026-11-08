# Design Material You di Onda

Regole per l'aspetto, da applicare con i componenti comuni (mai copie locali). Ogni PR di aspetto cita prima/dopo.

## Colori
- Solo ruoli del tema (`MaterialTheme.colorScheme` o `LocalOndaColors`): nessun colore fisso fuori dal tema.
- Il tema ruota su due colori (primario e accento, `data/ThemePalette.kt`); contrasto AA verificato dai test in chiaro e scuro.
- Superfici tonali, dal fondo al primo piano: `surface` → `surfaceContainerLow` (sezioni) → `surfaceContainer` →
  `surfaceContainerHigh` (schede dentro una sezione, editor) → `surfaceContainerHighest`. Niente riquadri tutti uguali.
- Accenti tonali: icone in contenitori `secondaryContainer` / `onSecondaryContainer`; azione principale `primary`.
- Errore (Elimina) solo con `colorScheme.error`.

## Forme (`ui/theme/Tokens.kt`, `OndaShapes`)
- small 12 dp: chip, campi, miniature.
- medium 16 dp: voci premibili, righe evidenziate.
- large 28 dp: sezioni, schede, card della home, player.
- extraLarge 32 dp: fogli e pannelli a tutta larghezza.
- Angoli concentrici: raggio interno = raggio esterno − margine (`PlayerLayout.concentricRadius`).

## Tipografia
- Titoli di pagina: headlineSmall. Titoli di sezione: labelLarge in `primary`.
- Voci: titolo bodyLarge, supporto bodyMedium in `onSurfaceVariant`. Etichette di pulsanti e chip: labelLarge.
- Niente troncamenti nelle impostazioni (vedi `UiRulesTest`); dove serve una riga sola: ellissi e descrizione sotto.

## Componenti comuni
- `SettingsSection`, `SettingsItem`, `SettingsSwitch`, `SettingsSupporting`, `SettingsLabel` (ui/settings/SettingsKit.kt).
- `TonalIcon` (icona in cerchio tonale 40 dp), `OndaChip`, `ValueSlider`, `RadioItem`, `ActionPickerDialog`.
- Interruttori `Switch` e cursori `Slider` di Material 3, sempre con l'intera riga toccabile.

## Movimento
- Molle brevi (`spring`, smorzamento 0,6–0,85) per pressioni e cambi di misura; dissolvenze 150–400 ms (`Motion`).

## Accessibilità
- Area di tocco ≥ 48 dp; `contentDescription` su ogni elemento senza testo; testi in `values/` e `values-it/`.

## Ordine di applicazione
Impostazioni → Elenco app e cartelle → Popup e notifiche → Ricerca → Home. Una PR per area, solo aspetto.
