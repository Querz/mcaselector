#!/bin/bash

exePath=$(winepath -w "build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe")
wine "C:\winrun4j\ResourceHacker.exe"
wine "C:\winrun4j\ResourceHacker.exe" "-open C:\winrun4j\resource.rc -save C:\winrun4j\resource.res -action compile"
wine "C:\winrun4j\ResourceHacker.exe" "-open $exePath -save $exePath -action add -resource C:\winrun4j\resource.res"

mkdir -p build/winrun4j
cp build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe "build/inno/MCA Selector.exe"