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


