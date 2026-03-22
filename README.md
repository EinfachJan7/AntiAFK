# AntiAFK Minecraft Plugin

Ein einfaches Anti-AFK Plugin für Minecraft 1.21.8 (Spigot/Paper) - erkennt inaktive Spieler und führt konfigurierbare Befehle aus.

## Features

✅ **Automatische AFK-Erkennung** - Führt Befehle nach konfigurierbarer Inaktivitätszeit aus
✅ **Robuste Umgehungserkennung** - Erkennt passive Bewegungen (Wasser, Loren, Fahrzeuge, Pistons, Knockback)
✅ **Back-Command** - Separater Befehl wenn Spieler AFK-Status verlässt
✅ **AFK-Statistiken** - Verwalte und verfolge AFK-Zeiten mit Datenbank- oder Dateispeicher
✅ **Offline-Player Support** - Abfrage und Reset von Stats für Offline-Spieler
✅ **MiniMessage Support** - Formatiere Nachrichten mit Farben & Styles
✅ **PlaceholderAPI** - Umfangreiche Placeholders für AFK-Statistiken
✅ **Einfache Konfiguration** - Nur wenige Einstellungen nötig
✅ **Permission-System** - Spieler können vom AFK-Check befreit werden
✅ **Debug-Modus** - Detaillierte Logs zur Fehlersuche

## Installation

1. **Kompilieren:**
```bash
mvn clean package
```

2. **Plugin installieren:**
   - Kopiere die generierte JAR aus `target/` in den `plugins/` Ordner
   - Server neustarten

3. **Du bist fertig!**

## Konfiguration

Editiere `plugins/AntiAFK/config.yml`:

```yaml
enabled: true

# Zeit bis AFK (in Sekunden)
afk-timeout: 1200          # 20 Minuten

# Check-Intervall (in Sekunden)
check-interval: 1          # Jede Sekunde überprüfen

# Stats-Update Intervall (in Sekunden)
stats-update-interval: 30  # Alle 30 Sekunden aktualisieren
                           # Wenn 0: Nur beim AFK-Ende speichern

# Befehl wenn Spieler AFK wird
command: "say <red>%player% ist AFK"

# Befehl wenn Spieler AFK-Status verlässt (leer lassen um zu deaktivieren)
command-back: "say <green>%player% ist zurück"
```

## Befehle (Konfiguration)

**AFK-Befehl** – wird ausgeführt wenn Spieler inaktiv wird:
```yaml
command: "say %player% ist AFK"
```

**Back-Befehl** – wird ausgeführt wenn Spieler sich nach AFK wieder bewegt:
```yaml
command-back: "say %player% ist zurück"
```
> Leer lassen (`command-back: ""`) um den Back-Befehl zu deaktivieren.

**Variablen:** `%player%`, `%uuid%`

## Beispiele

```yaml
# Kick nach AFK, Willkommensnachricht bei Rückkehr
command: "kick %player% Du wurdest wegen Inaktivität gekickt."
command-back: ""

# Zum Spawn teleportieren und zurück
command: "tp %player% 0 64 0"
command-back: "tp %player% spawn"

# Mit MiniMessage Formatierung
command: "say <bold><red>⚠ %player% ist AFK!"
command-back: "say <bold><green>✓ %player% ist zurück!"

# Partikeleffekte
command: "particle flame %player%"
command-back: "particle soul %player%"
```

## Admin-Befehle

| Befehl | Effekt |
|--------|--------|
| `/antiafk` oder `/antiafk status` | Zeigt Plugin-Status |
| `/antiafk reload` | Config neu laden |
| `/antiafk check <Spieler>` | Zeigt AFK-Zeit eines Spielers |
| `/antiafk stats <Spieler>` | Zeigt detaillierte AFK-Statistiken eines Spielers (auch Offline) |
| `/antiafk reset <Spieler> <time\|count\|all>` | Setzt Spieler-Stats zurück |
| `/antiafk debug` | Debug-Modus AN/AUS |

**Permission:** `antiafk.admin`

**Reset-Optionen:**
- `time` - Setzt nur die AFK-Zeit zurück
- `count` - Setzt nur die AFK-Vorkommnisse zurück
- `all` - Setzt alles zurück

## PlaceholderAPI Integration

Das Plugin bietet vollständige PlaceholderAPI Integration für AFK-Statistiken! Alle Abfragen funktionieren auch für **Offline-Spieler**.

### Stats-Update Verhalten

Die AFK-Statistiken können auf zwei Arten aktualisiert werden:

1. **Periodisches Update** (Standardeinstellung: 60 Sekunden):
   - Alle X Sekunden werden die Stats der aktiven AFK-Spieler aktualisiert
   - PlaceholderAPI-Abfragen erhalten sofort die neuen Werte
   - Einstellen mit: `stats-update-interval: 60`

2. **Nur bei AFK-Ende**:
   - Statistiken werden nur gespeichert, wenn der Spieler AFK-Status beendet
   - Geringere Datenbankbelastung
   - Einstellen mit: `stats-update-interval: 0`

### Verfügbare Placeholders

| Placeholder | Beschreibung | Beispiel |
|---|---|---|
| `%antiafk_total_afk_time_<name>%` | AFK-Zeit eines Spielers | `%antiafk_total_afk_time_Hans%` |
| `%antiafk_total_afk_time_player%` | AFK-Zeit des aktuellen Spielers | für den Spieler der es sieht |
| `%antiafk_total_afk_time_player_name%` | AFK-Zeit des aktuellen Spielers (Alias) | für den Spieler der es sieht |
| `%antiafk_afk_count_<name>%` | AFK-Sessionen eines Spielers | `%antiafk_afk_count_Hans%` |
| `%antiafk_afk_count_player%` | AFK-Sessionen des aktuellen Spielers | für den Spieler der es sieht |
| `%antiafk_afk_count_player_name%` | AFK-Sessionen des aktuellen Spielers (Alias) | für den Spieler der es sieht |
| `%antiafk_last_afk_<name>%` | Letzter AFK-Zeitpunkt eines Spielers | `%antiafk_last_afk_Hans%` |
| `%antiafk_last_afk_player%` | Letzter AFK-Zeitpunkt des aktuellen Spielers | für den Spieler der es sieht |
| `%antiafk_last_afk_player_name%` | Letzter AFK-Zeitpunkt des aktuellen Spielers (Alias) | für den Spieler der es sieht |
| `%antiafk_top_1_player%` ... `%antiafk_top_10_player%` | Top AFK-Spieler Namen | `%antiafk_top_1_player%` |
| `%antiafk_top_1_time%` ... `%antiafk_top_10_time%` | Top AFK-Spieler Zeit | `%antiafk_top_1_time%` |

### Beispiele

```
Spieler mit meisten AFK-Zeit: %antiafk_top_1_player% (%antiafk_top_1_time%)
Meine AFK-Zeit: %antiafk_total_afk_time_player%
Hans' AFK-Zeit: %antiafk_total_afk_time_Hans%
Meine AFK-Sessions: %antiafk_afk_count_player_name%
```

**Wichtig:** Es dürfen **keine verschachtelten Placeholders** in einem Placeholder sein! 
- ✅ Richtig: `%antiafk_total_afk_time_Hans%`
- ✅ Richtig: `%antiafk_total_afk_time_player%`
- ❌ Falsch: `%antiafk_total_afk_time_%player%%` (zwei Placeholders in einem)

## Befreiung vom AFK-Check

Gib einem Spieler diese Permission um ihn vom AFK-Check auszunehmen:
```
antiafk.bypass
```

## So funktioniert es

1. Spieler wird beim Einloggen registriert
2. Alle **`check-interval` Sekunden** prüft das Plugin ob sich der Spieler bewegt hat
3. Wenn sich der Spieler weniger als **0.5 Blöcke** bewegt hat → AFK-Timer läuft
4. Nach **`afk-timeout` Sekunden** Inaktivität → `command` wird ausgeführt
5. Bewegt sich der Spieler wieder → `command-back` wird ausgeführt und der Timer resettet

### Umgehungsschutz

Das Plugin erkennt passive Bewegungen und wertet diese **nicht** als echte Aktivität:

| Quelle | Erkannt |
|--------|---------|
| Wasser / Lava | ✅ |
| Minecart / Boot / Fahrzeug | ✅ |
| Piston-Bewegung | ✅ |
| Knockback / externe Velocity | ✅ |
| Kopfdrehung ohne Positionsänderung | wird als aktiv gewertet |

> Nur echte Spieler-Eingaben (WASD, Mausbewegung) setzen den AFK-Timer zurück.

## Schnellstart-Beispiele

**Schneller AFK-Kick (2 Minuten):**
```yaml
afk-timeout: 120
check-interval: 20
command: "kick %player% AFK"
command-back: ""
```

**Entspannter AFK-Kick (10 Minuten):**
```yaml
afk-timeout: 600
check-interval: 30
command: "kick %player% Du warst zu lange inaktiv."
command-back: ""
```

**Spawn-Teleport statt Kick:**
```yaml
afk-timeout: 300
check-interval: 20
command: "tp %player% 0 64 0"
command-back: "tp %player% spawn"
```

**Nur Nachricht senden:**
```yaml
afk-timeout: 180
check-interval: 20
command: "msg %player% <red>Du bist AFK!"
command-back: "msg %player% <green>Willkommen zurück!"
```

## Voraussetzungen

- Java 21+
- Maven 3.6.0+
- Paper oder Spigot 1.21.8+

## Support & Fehlersuche

- Config neu laden: `/antiafk reload`
- Debug-Logs aktivieren: `/antiafk debug`
- Plugin-Status prüfen: `/antiafk status`
- Logs findest du in der Server-Konsole

## Credits

Minecraft 1.21.8+ | Paper/Spigot kompatibel