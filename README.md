# TanzFit 💃

Deine eigene Trainings-App für Android: Übungen aus Instagram-Bildschirmaufnahmen
(oder ganz ohne Video) anlegen, zu Workouts zusammenstellen und geführt trainieren –
mit fester Zeit oder festen Wiederholungen pro Übung, Pausen, Abhaken und Motivations-Popups.

## Funktionen

- **Übungen** mit Name, Beschreibung, optionalem Video (aus der Galerie) und Vorgabe
  (Sekunden *oder* Wiederholungen). Videos werden in die App kopiert – du kannst die
  Bildschirmaufnahme danach aus der Galerie löschen.
- **Workouts**: mehrere Übungen auf einmal auswählen, Reihenfolge ändern, Zeit/Wdh. pro Übung
  anpassen, Pausenlänge festlegen.
- **Geführtes Training** (wie bei Leap Fitness): Countdown „Mach dich bereit“ →
  Übung (Video in Endlosschleife, Timer oder „Erledigt“-Button) → Pause mit Vorschau → …
  Signaltöne bei den letzten 3 Sekunden, Bildschirm bleibt an.
- **Freies Abhaken**: Auf der Workout-Seite einfach die Übungen antippen.
- **Motivation**: Spruch beim App-Start, Popup nach jeder Übung, „Halbzeit“-Banner, Abschluss-Popup.
- **Verlauf** mit Wochen-/Gesamtstatistik.

## APK mit GitHub Actions bauen

1. Neues Repository auf GitHub anlegen (darf privat sein).
2. Den Inhalt dieses Ordners hochladen (per `git push` oder über „Add file → Upload files“ –
   dabei den Ordner `.github` nicht vergessen).
3. Im Repository auf **Actions** gehen. Der Workflow „Android APK bauen“ startet bei jedem
   Push automatisch (oder per „Run workflow“). Dauer: ca. 5–8 Minuten.
4. Im fertigen Lauf unten unter **Artifacts** `TanzFit-APK` herunterladen, entpacken
   und `TanzFit.apk` aufs Handy bringen.
5. Auf dem Handy die APK öffnen und „Installation aus unbekannten Quellen“ erlauben.

**Tipp:** Wenn du einen Tag wie `v1.0` pushst, legt der Workflow zusätzlich ein
GitHub-Release mit der APK an – die kannst du direkt am Handy herunterladen.

Updates: Neue APK einfach drüber installieren – deine Übungen bleiben erhalten
(das Repo enthält dafür einen festen Signatur-Schlüssel `app/tanzfit.keystore`).

## Videos von Instagram holen

1. Reel/Post öffnen, Bildschirmaufnahme starten (Schnelleinstellungen).
2. In der App: **Übungen → + Übung → Video hinzufügen** und die Aufnahme auswählen.
   Videos laufen stumm in Schleife, wie ein GIF.

## Technik

Kotlin, Jetpack Compose (Material 3), Daten lokal als JSON (`data.json`) im App-Speicher,
minSdk 26 (Android 8.0).
