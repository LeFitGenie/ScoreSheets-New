# ScoreSheets

Moderner Neubau eines Android-Punkteblocks (Spieleblock) für Kartenspiele und Würfelspiele.
Die App läuft komplett offline und braucht **keine Berechtigungen**.

Inspiriert von [SIR/ScoreSheets](https://codeberg.org/SIR/ScoreSheets) – dies ist eine eigenständige Neuentwicklung, es wurde kein Code übernommen.

## Spiele

| Spiel | Spieler | Berechnung |
| --- | --- | --- |
| Rommé | 2–8 | Punkte pro Runde addieren, wer am wenigsten hat gewinnt; Eingabe mit „Verdoppeln“-Knopf |
| Sechsundsechzig | 2–4 | 1–3 Punkte pro Spiel, Ziel: 7 |
| Piraten | 2–8 | 10 Runden mit Ansage, Stichen und Bonus |
| Stiche ansagen | 3–6 | 60 ÷ Spielerzahl Runden, 20 + 10 pro Stich bei richtiger Ansage, sonst −10 pro Abweichung |
| Würfeln (Pasch) | 1–8 | 13 Kategorien, Bonus 35 ab 63 Punkten oben, Zusatz-Fünferpasch ×100 |
| Freie Punkte | 1–8 | Beliebige Werte addieren oder subtrahieren |

Außerdem: Geberanzeige, gespeicherte Spielernamen, laufende Spiele bleiben erhalten, Deutsch und Englisch, dynamische Farben ab Android 12.

> Die Spielnamen sind bewusst neutral gehalten (z. B. „Stiche ansagen“, „Piraten“, „Würfeln“), weil Namen wie Wizard® oder Skull King markenrechtlich geschützt sind.

## Technik

- Kotlin, Jetpack Compose mit Material 3
- Room für die lokale Speicherung
- Navigation Compose, ViewModels mit Flow
- Gradle mit Kotlin DSL und Version Catalog (`gradle/libs.versions.toml`)
- minSdk 26, targetSdk 35
- Spielregeln liegen als reines Kotlin in `app/src/main/java/.../game` und sind per JUnit getestet

## Bauen

```bash
./gradlew testDebugUnitTest   # Unit-Tests
./gradlew assembleDebug       # APK unter app/build/outputs/apk/debug/
```

Voraussetzungen: JDK 17 und das Android SDK (Android Studio bringt beides mit).
Die GitHub-Action in `.github/workflows/build.yml` führt Tests und Debug-Build bei jedem Push aus.

## Aufbau

```
app/src/main/java/de/lefitgenie/scoresheets/
├── game/   Regeln und Berechnung (ohne Android-Abhängigkeit)
├── data/   Room-Datenbank und Repository
└── ui/     Compose-Oberfläche (Start, Neues Spiel, Punkteblatt)
```
