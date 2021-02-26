#!/bin/sh

echo "running innosetup"
unset DISPLAY
scriptname=$(winepath -w "$1")
echo "$scriptname"
wine "C:\inno\ISCC.exe" "$scriptname" "/q"