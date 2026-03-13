# AntiAFK Minecraft Plugin

Ein einfaches Anti-AFK Plugin für Minecraft 1.21.8 (Spigot/Paper) - kickt inaktive Spieler automatisch.

## Features

✅ **Automatische AFK-Erkennung** - Kickt Spieler nach konfigurierbarer Zeit
✅ **Robuste Umgehungserkennung** - Erkennt Umwege (Wasser, Loren, Fahrzeuge, Pistons)
✅ **MiniMessage Support** - Formatiere Nachrichten mit Farben & Styles
✅ **Einfache Konfiguration** - Nur wenige Einstellungen nötig
✅ **Permission-System** - Spieler können befreit werden

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
afk-timeout: 300          # 5 Minuten

# Check Intervall
check-interval: 20        # Alle 20 Sekunden

# Befehle
command: "say <red>%player% ist AFK"        # Wenn AFK
command-back: "say <green>%player% zurück"  # Wenn aktiv
```

## Befehle

**AFK-Befehl** (wenn Spieler inaktiv wird):
```yaml
command: "say %player% ist AFK"
```

**Back-Befehl** (wenn Spieler wieder aktiv wird):
```yaml
command-back: "say %player% ist zurück"
```

**Variablen:** `%player%`, `%uuid%`

## Beispiele

```yaml
# Kick nach AFK
command: "kick %player% AFK"
command-back: "say %player% rejoined"

# Teleport
command: "tp %player% 0 64 0"
command-back: "tp %player% spawn"

# Mit Nachrichten
command: "say <bold><red>⚠ %player% AFK"
command-back: "say <bold><green>✓ %player% back"

# Effekte
command: "particle flame %player%"
command-back: "particle soul %player%"
```

## Befehle (Admin nur)

| Befehl | Effekt |
|--------|--------|
| `/antiafk` oder `/antiafk status` | Zeigt Plugin-Status |
| `/antiafk reload` | Config neu laden |

Permission: `antiafk.admin`

## Befreiung von AFK-Kick

Gib einem Spieler diese Permission:
```
antiafk.exempt
```

## So funktioniert es

1. **Spieler** wird registriert wenn online
2. Alle **20s** - Plugin prüft ob Spieler sich bewegt hat
3. Wenn sich Spieler **nicht 0.5 Blöcke oder mehr** bewegt hat → **AFK**
4. Nach **5 Minuten AFK** → **Befehl wird ausgeführt** (z.B. Kick, Teleport, etc.)

### Umgehungsschutz

Das Plugin erkennt und ignoriert:
- ✅ Wasser/Lava (kein echte Bewegung)
- ✅ Minecarts/Boote (keine echte Bewegung)
- ✅ Drehungen ohne Bewegung

### Beispiel Config

**Schneller AFK-Kick (2 Minuten):**
```yaml
afk-timeout: 120
check-interval: 30
command: "kick %player% AFK"
```

**Entspannter AFK-Kick (10 Minuten):**
```yaml
afk-timeout: 600
check-interval: 20
command: "kick %player%"
```

**Spieler zum Spawn teleportieren:**
```yaml
command: "tp %player% 100 64 200"
```

**Nachricht im Chat:**
```yaml
command: "msg %player% Du warst zu lange inaktiv und wurdest teleportiert!"
```

## Installation via Maven

**Voraussetzungen:**
- Java 21+
- Maven 3.6.0+

**Schnellstart:**
```bash
cd /pfad/zum/antiafk
mvn clean package
cp target/AntiAFK-1.0.0.jar /pfad/zum/server/plugins/
# Server neustarten
```

## Support

- Überprüfe die Config mit `/antiafk reload`
- Logs findest du in der Konsole
- Die JAR muss in `plugins/` sein

## Credits

Minecraft 1.21.8+ | Paper/Spigot kompatibel
