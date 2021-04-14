#!/bin/bash

ls -lah build/inno
echo "hello"
exePath=$(winepath -w "build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe")
echo "$exePath"
echo "made WinRun4J64.exe path"
iconPath=$(winepath -w "$1")
echo "$iconPath"
echo "made icon path"
iniPath=$(winepath -w "$2")
echo "$iniPath"
echo "made ini path"
echo "clearing exe"
wine "C:\winrun4j\RCEDIT64.exe" "/C" "$exePath"
echo "adding icon"
wine "C:\winrun4j\RCEDIT64.exe" "/I" "$exePath" "$iconPath"
echo "adding ini"
wine "C:\winrun4j\RCEDIT64.exe" "/N" "$exePath" "$iniPath"