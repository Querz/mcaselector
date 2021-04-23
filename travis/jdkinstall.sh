#!/bin/bash

wget https://cdn.azul.com/zulu/bin/zulu8.52.0.23-ca-fx-jdk8.0.282-linux_x64.tar.gz -O ~/jdkfx8.tar.gz
tar -xf ~/jdkfx8.tar.gz -C ~/
rm ~/jdkfx8.tar.gz
mv ~/zulu8.52.0.23-ca-fx-jdk8.0.282-linux_x64 ~/jdkfx8