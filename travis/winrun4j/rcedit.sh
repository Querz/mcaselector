#!/bin/sh


export DISPLAY=:1
echo "clearing exe"
wine "C:\winrun4j\ResourceHacker.exe" "-open" "C:\winrun4j\WinRun4J64.exe" "-save" "C:\winrun4j\WinRun4J64.exe" "-action" "delete" "-mask" ",,"

unset DISPLAY
echo "adding icon"
wine "C:\winrun4j\RCEDIT64.exe" "/I" "C:\winrun4j\WinRun4J64.exe" "C:\winrun4j\icon.ico"
echo "adding ini"
wine "C:\winrun4j\RCEDIT64.exe" "/N" "C:\winrun4j\WinRun4J64.exe" "C:\winrun4j\winrun4j.ini"

export DISPLAY=:1
echo "compiling resource.rc"
wine "C:\winrun4j\ResourceHacker.exe" "-open" "C:\winrun4j\resource.rc" "-save" "C:\winrun4j\resource.res" "-action" "compile"
echo "applying resource.res"
wine "C:\winrun4j\ResourceHacker.exe" "-open" "C:\winrun4j\WinRun4J64.exe" "-save" "C:\winrun4j\WinRun4J64.exe" "-action" "add" "-resource" "C:\winrun4j\resource.res"

echo "copying finished MCA Selector.exe"
mkdir -p build/winrun4j
cp ~/".wine/drive_c/winrun4j/WinRun4J64.exe" "build/inno/MCA Selector.exe"