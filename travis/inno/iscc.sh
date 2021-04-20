#!/bin/sh

echo "running innosetup"
unset DISPLAY
scriptname=$(winepath -w "$1")
wine "C:\inno\ISCC.exe" "$scriptname" "/q"