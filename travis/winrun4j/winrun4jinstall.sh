#!/bin/bash

rm -fr build/tmp/winrun4j
mkdir -p build/tmp/winrun4j

wget https://github.com/poidasmith/winrun4j/files/1822558/winrun4J-0.4.5.zip -O build/tmp/winrun4j/winrun4j.zip
unzip build/tmp/winrun4j/winrun4j.zip -d build/tmp/winrun4j/
rm build/tmp/winrun4j/winrun4j.zip

wget http://www.angusj.com/resourcehacker/resource_hacker.zip -O build/tmp/winrun4j/resourcehacker.zip
unzip build/tmp/winrun4j/resourcehacker.zip -d build/tmp/winrun4j/
rm build/tmp/winrun4j/resourcehacker.zip

mkdir -p ~/".wine/drive_c/winrun4j"
cp build/tmp/winrun4j/winrun4j/bin/RCEDIT64.exe ~/".wine/drive_c/winrun4j"
cp inno/icon.ico ~/".wine/drive_c/winrun4j"
cp travis/winrun4j/winrun4j.ini ~/".wine/drive_c/winrun4j"
cp build/tmp/winrun4j/ResourceHacker.exe ~/".wine/drive_c/winrun4j"
cp travis/winrun4j/resource.rc ~/".wine/drive_c/winrun4j"

unset DISPLAY
echo "clearing exe"
wine "C:\winrun4j\RCEDIT64.exe" "/C" "$(winepath -w "build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe")"
echo "adding icon"
wine "C:\winrun4j\RCEDIT64.exe" "/I" "$(winepath -w "build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe")" "C:\winrun4j\icon.ico"
echo "adding ini"
wine "C:\winrun4j\RCEDIT64.exe" "/N" "$(winepath -w "build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe")" "C:\winrun4j\winrun4j.ini"


DISPLAY=:1
echo "compiling resource.rc"
wine "C:\winrun4j\ResourceHacker.exe" "-open" "C:\winrun4j\resource.rc" "-save" "C:\winrun4j\resource.res" "-action" "compile"
echo "applying resource.res"
wine "C:\winrun4j\ResourceHacker.exe" "-open" "$(winepath -w "build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe")" "-save" "$(winepath -w "build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe")" "-action" "add" "-resource C:\winrun4j\resource.res"

echo "copying finished MCA Selector.exe"
mkdir -p build/winrun4j
cp build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe "build/inno/MCA Selector.exe"