#!/bin/sh

unset DISPLAY
echo "clearing exe"
wine "C:\winrun4j\RCEDIT64.exe" "/C" "C:\winrun4j\WinRun4J64.exe"
echo "adding icon"
wine "C:\winrun4j\RCEDIT64.exe" "/I" "C:\winrun4j\WinRun4J64.exe" "C:\winrun4j\icon.ico"
echo "adding ini"
wine "C:\winrun4j\RCEDIT64.exe" "/N" "C:\winrun4j\WinRun4J64.exe" "C:\winrun4j\winrun4j.ini"
