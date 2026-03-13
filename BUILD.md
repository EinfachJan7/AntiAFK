# Build & Installation Guide

## Voraussetzungen

- **Java 21** oder höher
- **Maven 3.6.0** oder höher

## Schritt-für-Schritt Anleitung

### 1. Java/Maven Installation

**Windows:**
- Lade Maven herunter: https://maven.apache.org/download.cgi
- Entpacke und füge `bin/` zum PATH hinzu
- Überprüfung: `mvn -v`

### 2. Plugin kompilieren

```bash
# Im Plugin-Verzeichnis
mvn clean package
```

Dies erstellt die JAR-Datei in `target/AntiAFK-1.0.0.jar`

### 3. Installation auf dem Server

```bash
# Kopiere die JAR zur server plugins
cp target/AntiAFK-1.0.0.jar /pfad/zum/server/plugins/

# Server neustarten
```

### 4. Neue Konfiguration aktualisieren

Die erste Konfiguration wird in `plugins/AntiAFK/config.yml` erstellt.

## Kompilierung mit optionen

**Schnelle Test-Kompilierung:**
```bash
mvn clean compile
```

**Mit Dependencies herunterladeen:**
```bash
mvn dependency:resolve
mvn clean package
```

**Debug-Output:**
```bash
mvn clean package -X
```

## Troubleshooting

**Fehler: "Java 21 nicht gefunden"**
- Überprüfe Java Version: `java -version`
- Passe `pom.xml` Maven Compiler an

**Maven nicht gefunden**
- Überprüfe ob Maven installiert ist
- Füge Maven zum PATH hinzu

**Abhängigkeit nicht heruntergeladen**
```bash
mvn clean install -U
```

## Automatisches Testing

```bash
# Nach der Installation mit /reload Befehl
/reload
/antiafk status
/antiafk info <Spielername>
```
