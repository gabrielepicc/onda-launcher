# Componenti e font di terze parti

Onda è GPL-3.0-or-later: ogni dipendenza e ogni font deve avere una licenza compatibile (Apache-2.0, MIT, OFL, BSD).
Niente Google Sans né altri font proprietari.

## Librerie
| Componente | Licenza | Uso |
|---|---|---|
| Jetpack Compose, Material 3, AndroidX | Apache-2.0 | interfaccia |
| `androidx.graphics:graphics-shapes` | Apache-2.0 | forme Material 3 (biscotto, quadrante M3) dell'orologio |

## Font
| Font | Licenza | Dove |
|---|---|---|
| Noto Serif (font serif di sistema, `FontFamily.Serif`) | SIL OFL 1.1 | stili «Serif» e «Romano» dell'orologio; non è incluso nell'APK |
| Roboto / Roboto Mono (font di sistema) | Apache-2.0 | testo dell'interfaccia e stile «Mono» |

Gli stili «Segmenti» e «Matrice a punti» non usano font: sono disegnati in Canvas.
