#!/bin/bash

# Konfiguration
PI_USER="mradle"
PI_HOST="192.168.178.165"
PI_APP_DIR="./smart-home"
APP_NAME="kberry-server.jar"

JAR_FILE=$(ls -t target/*.jar | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "Keine JAR-Datei im target-Verzeichnis gefunden!"
    exit 1
fi

echo "Gefundene JAR: $JAR_FILE"

echo "Prüfe, ob App läuft..."
ssh $PI_USER@$PI_HOST "pkill -f '$APP_NAME' || echo 'Keine laufende App gefunden'"

echo "Kopiere JAR-Datei nach Raspberry Pi..."
scp "$JAR_FILE" $PI_USER@$PI_HOST:$PI_APP_DIR/$APP_NAME

#echo "Starte App auf Raspberry Pi..."
#ssh $PI_USER@$PI_HOST "nohup java -jar $PI_APP_DIR/$APP_NAME  --mapping SmartHomeExport.csv --knx 169.254.188.52 > $PI_APP_DIR/log.txt 2>&1 &"

echo "Deployment abgeschlossen ✅"