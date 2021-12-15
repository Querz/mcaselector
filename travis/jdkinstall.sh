#!/bin/bash

wget https://cdn.azul.com/zulu/bin/zulu17.30.15-ca-fx-jdk17.0.1-linux_x64.tar.gz -O ~/jdkfx17.tar.gz
tar -xf ~/jdkfx17.tar.gz -C ~/
rm ~/jdkfx17.tar.gz
mv ~/zulu17.30.15-ca-fx-jdk17.0.1-linux_x64 ~/jdkfx17