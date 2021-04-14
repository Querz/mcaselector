#!/bin/bash

exePath=$(winepath -w "build/tmp/winrun4j/winrun4j/bin/WinRun4J64.exe")
iconPath=$(winepath -w "$1")
iniPath=$(winepath -w "$2")
wine "C:\winrun4j\RCEDIT64.exe" "/C" "$exePath"
wine "C:\winrun4j\RCEDIT64.exe" "/I" "$exePath" "$iconPath"
wine "C:\winrun4j\RCEDIT64.exe" "/N" "$exePath" "$iniPath"