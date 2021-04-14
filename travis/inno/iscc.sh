#!/bin/sh

echo "running innosetup"
ls -lah build/inno
scriptname=$(winepath -w "$1")
echo "$scriptname"
wine "C:\inno\ISCC.exe" "$scriptname" "/q"