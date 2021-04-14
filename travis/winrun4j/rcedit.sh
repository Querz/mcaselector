#!/bin/bash

ls -lah build/inno
exePath=$(winepath -w "build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe")
iconPath=$(winepath -w "$1")
iniPath=$(winepath -w "$2")
echo "clearing exe"
wine "C:\winrun4j\RCEDIT64.exe" "/C" "$exePath"
echo "adding icon"
wine "C:\winrun4j\RCEDIT64.exe" "/I" "$exePath" "$iconPath"
echo "adding ini"
wine "C:\winrun4j\RCEDIT64.exe" "/N" "$exePath" "$iniPath"