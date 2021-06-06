#!/bin/bash

wget https://cdn.azul.com/zulu/bin/zulu16.30.15-ca-fx-jdk16.0.1-linux_x64.tar.gz -O ~/jdkfx16.tar.gz
tar -xf ~/jdkfx16.tar.gz -C ~/
rm ~/jdkfx16.tar.gz
mv ~/zulu16.30.15-ca-fx-jdk16.0.1-linux_x64 ~/jdkfx16