#!/bin/bash

rm -rf build/tmp/inno
mkdir -p build/tmp/inno
cd build/tmp/inno

wget https://constexpr.org/innoextract/files/innoextract-1.9/innoextract-1.9-linux.tar.xz --no-check-certificate -O innoextract.tar.xz
tar -xf innoextract.tar.xz
rm innoextract.tar.xz

wget -O is.exe http://files.jrsoftware.org/is/6/innosetup-6.1.2.exe
innoextract-1.9-linux/bin/amd64/innoextract is.exe
mkdir -p ~/".wine/drive_c/inno"
cp -a app/* ~/".wine/drive_c/inno"