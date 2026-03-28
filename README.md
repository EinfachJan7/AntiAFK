# 🚫 AntiAFK

Ein leichtgewichtiges Spigot/Paper Plugin das inaktive Spieler erkennt und konfigurierbare Befehle ausführt.

---

## ✨ Features

- ✅ **Automatische AFK-Erkennung** — Befehl nach konfigurierbarer Inaktivitätszeit
- ✅ **Umgehungsschutz** — Erkennt passive Bewegungen (Wasser, Loren, Fahrzeuge, Pistons, Knockback)
- ✅ **Back-Command** — Separater Befehl wenn Spieler AFK-Status verlässt
- ✅ **AFK-Statistiken** — AFK-Zeiten mit Datenbank- oder Dateispeicher
- ✅ **Offline-Player Support** — Stats auch für offline Spieler abfragen & zurücksetzen
- ✅ **MiniMessage Support** — Nachrichten mit Farben & Styles formatieren
- ✅ **PlaceholderAPI** — Umfangreiche Placeholders für AFK-Statistiken
- ✅ **Permission-System** — Spieler vom AFK-Check befreien
- ✅ **Debug-Modus** — Detaillierte Logs zur Fehlersuche

---

## 📋 Voraussetzungen

- Java **21+**
- Paper oder Spigot **1.21.8+**

---

## 🚀 Installation

1. JAR kompilieren:
```bash
mvn clean package
```
2. JAR aus `target/` in den `plugins/` Ordner kopieren
3. Server neustarten

---

## ⚙️ Konfiguration (`config.yml`)

```yaml
enabled: true

afk-timeout: 1200          # Zeit bis AFK (Sekunden) — Standard: 20 Minuten
check-interval: 1          # Prüf-Intervall in Sekunden
stats-update-interval: 30  # Stats-Update Intervall (0 = nur beim AFK-Ende)
placeholder-show-seconds: false

command: "say <red>%player% ist AFK"
command-back: "say <green>%player% ist zurück"  # Leer lassen zum Deaktivieren
```

**Variablen:** `%player%`, `%uuid%`

---

## 💬 Befehle

| Befehl | Beschreibung |
|--------|--------------|
| `/antiafk` | Plugin-Status anzeigen |
| `/antiafk reload` | Config neu laden |
| `/antiafk check <Spieler>` | AFK-Zeit eines Spielers anzeigen |
| `/antiafk stats <Spieler>` | Detaillierte Stats (auch Offline) |
| `/antiafk reset <Spieler> <time\|count\|all>` | Stats zurücksetzen |
| `/antiafk debug` | Debug-Modus AN/AUS |

**Permission:** `antiafk.admin` — **Bypass:** `antiafk.bypass`

---

## 🔧 Wie es funktioniert

1. Spieler wird beim Einloggen registriert
2. Alle **`check-interval`** Sekunden prüft das Plugin ob der Spieler sich bewegt hat
3. Weniger als **0.5 Blöcke** bewegt → AFK-Timer läuft
4. Nach **`afk-timeout`** Sekunden → `command` wird ausgeführt
5. Spieler bewegt sich → `command-back` wird ausgeführt, Timer resettet

### Umgehungsschutz

| Quelle | Erkannt |
|--------|---------|
| Wasser / Lava | ✅ |
| Minecart / Boot / Fahrzeug | ✅ |
| Piston-Bewegung | ✅ |
| Knockback / externe Velocity | ✅ |
| Kopfdrehung ohne Positionsänderung | ⚠️ Wird als aktiv gewertet |

---

## 📊 PlaceholderAPI

Alle Placeholders funktionieren auch für **Offline-Spieler**.

| Placeholder | Beschreibung |
|-------------|--------------|
| `%antiafk_total_afk_time_<name>%` | Gesamte AFK-Zeit eines Spielers |
| `%antiafk_total_afk_time_player%` | Gesamte AFK-Zeit des aktuellen Spielers |
| `%antiafk_avg_afk_time_<name>%` | Durchschnittliche AFK-Zeit pro Session |
| `%antiafk_avg_afk_time_player%` | Durchschnitt des aktuellen Spielers |
| `%antiafk_afk_count_<name>%` | AFK-Sessions eines Spielers |
| `%antiafk_afk_count_player%` | AFK-Sessions des aktuellen Spielers |
| `%antiafk_last_afk_<name>%` | Letzter AFK-Zeitpunkt eines Spielers |
| `%antiafk_last_afk_player%` | Letzter AFK-Zeitpunkt des aktuellen Spielers |
| `%antiafk_top_1_player%` ... `%antiafk_top_10_player%` | Top AFK-Spieler Namen |
| `%antiafk_top_1_time%` ... `%antiafk_top_10_time%` | Top AFK-Spieler Zeiten |

> ⚠️ Keine verschachtelten Placeholders!
> - ✅ `%antiafk_total_afk_time_Hans%`
> - ❌ `%antiafk_total_afk_time_%player_name%%`

---

## 📝 Beispiele

```yaml
# AFK-Kick
command: "kick %player% Du wurdest wegen Inaktivität gekickt."
command-back: ""

# Spawn-Teleport
command: "tp %player% 0 64 0"
command-back: "tp %player% spawn"

# Nur Nachricht
command: "msg %player% <red>Du bist AFK!"
command-back: "msg %player% <green>Willkommen zurück!"
```

---

*Minecraft 1.21.8+ | Paper/Spigot kompatibel*
```
