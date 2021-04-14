#!/bin/sh

export DISPLAY=:1
echo "compiling resource.rc"
wine "C:\winrun4j\ResourceHacker.exe" "-open" "C:\winrun4j\resource.rc" "-save" "C:\winrun4j\resource.res" "-action" "compile"
echo "applying resource.res"
wine "C:\winrun4j\ResourceHacker.exe" "-open" "C:\winrun4j\WinRun4J64.exe" "-save" "C:\winrun4j\WinRun4J64.exe" "-action" "add" "-resource C:\winrun4j\resource.res"

echo "copying finished MCA Selector.exe"
mkdir -p build/winrun4j
cp ~/".wine/drive_c/winrun4j/WinRun4J64.exe" "build/inno/MCA Selector.exe"