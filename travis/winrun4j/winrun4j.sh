#!/bin/bash

rm -fr build/tmp/winrun4j
mkdir -p build/tmp/winrun4j

wget https://github.com/poidasmith/winrun4j/files/1822558/winrun4J-0.4.5.zip -O build/tmp/winrun4j/winrun4j.zip
unzip build/tmp/winrun4j/winrun4j.zip -d build/tmp/winrun4j/
rm build/tmp/winrun4j/winrun4j.zip

wget https://github.com/electron/rcedit/releases/download/v1.1.1/rcedit-x64.exe -O build/tmp/winrun4j/rcedit.exe

mkdir -p ~/".wine/drive_c/winrun4j"
cp build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe ~/".wine/drive_c/winrun4j"
cp build/tmp/winrun4j/winrun4j/bin/RCEDIT64.exe ~/".wine/drive_c/winrun4j"
cp inno/icon.ico ~/".wine/drive_c/winrun4j"
cp travis/winrun4j/winrun4j.ini ~/".wine/drive_c/winrun4j"
cp build/tmp/winrun4j/rcedit.exe ~/".wine/drive_c/winrun4j"

echo "assembling winrun4j"
wine "C:\winrun4j\RCEDIT64.exe" "/I" "WinRun4J64.exe" "icon.ico"
wine "C:\winrun4j\RCEDIT64.exe" "/N" "WinRun4J64.exe" "winrun4j.ini"
wine "C:\winrun4j\rcedit.exe" "WinRun4J64.exe" "--set-file-version" "1.0.0.0" "--set-product-version" "1.15.3" "--set-version-string" "LegalCopyright" "Querz"
mkdir -p build/winrun4j
cp ~/".wine/drive_c/winrun4j/WinRun4J.exe" "build/winrun4j/MCA Selector.exe"