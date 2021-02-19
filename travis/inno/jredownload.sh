wget --no-cookies \
--no-check-certificate \
--header "Cookie: oraclelicense=accept-securebackup-cookie" \
https://download.oracle.com/otn-pub/java/jdk/8u281-b09/89d678f2be164786b292527658ca1605/jre-8u281-windows-x64.tar.gz \
-O jre-8.tar.gz
tar -xf jre-8.tar.gz
rm jre-8.tar.gz
mv jre1.8.0_281 jre