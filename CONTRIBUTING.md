# Contribuire a Onda

Grazie per l'interesse! Onda è un launcher Android **open source (GPL-3.0-or-later)**, senza Google Play Services e senza tracciamento.

## Principi
1. **Privacy prima di tutto**: nessun dato lascia il dispositivo, salvo funzioni esplicite e disattivabili (es. meteo, in futuro).
2. **Nessuna dipendenza proprietaria**: niente Firebase, Play Services, analytics o pubblicità. Ogni libreria deve avere licenza compatibile con la GPLv3.
3. **Permessi just-in-time**: un permesso si chiede solo quando l'utente attiva la funzione che lo richiede, spiegando perché.
4. **Fluidità**: niente lavoro pesante sul main thread, niente ricomposizioni inutili.

## Ambiente
- Android Studio Ladybug o successivo, JDK 17.
- `./gradlew testDebugUnitTest lintRelease ktlintCheck assembleRelease` deve passare prima di aprire una pull request (la CI esegue gli stessi comandi).

## Convenzioni
- Kotlin + Jetpack Compose, Material 3. Stile verificato da **ktlint**.
- Ogni file sorgente inizia con:
  ```kotlin
  // SPDX-License-Identifier: GPL-3.0-or-later
  // Copyright (C) 2026 <il tuo nome>
  ```
- **Niente testo scritto nel codice**: usa `stringResource(R.string.…)`. Aggiungi la stringa in `values/strings.xml` (inglese) e in `values-it/strings.xml`.
- Elementi toccabili di almeno 48dp e con `contentDescription` quando non hanno testo.
- Nuove impostazioni: campo con default in `Settings`; se cambi il significato di un campo esistente, incrementa `Settings.CURRENT_SCHEMA` e aggiungi il passo in `SettingsCodec.migrate`, con un test.

## Commit e pull request
- Un argomento per pull request, descrizione di **cosa** e **perché**, screenshot o video per le modifiche visive.
- Aggiorna `CHANGELOG.md` nella sezione della prossima versione.

## Traduzioni
Le stringhe sono in `app/src/main/res/values*/strings.xml`. Per una nuova lingua crea `values-<codice>/strings.xml` (in futuro tramite Weblate).
