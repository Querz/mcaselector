#!/bin/sh

unset DISPLAY
echo "clearing exe"
wine "C:\winrun4j\RCEDIT64.exe" "/C" "$(winepath -w "build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe")"
echo "adding icon"
wine "C:\winrun4j\RCEDIT64.exe" "/I" "$(winepath -w "build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe")" "C:\winrun4j\icon.ico"
echo "adding ini"
wine "C:\winrun4j\RCEDIT64.exe" "/N" "$(winepath -w "build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe")" "C:\winrun4j\winrun4j.ini"
