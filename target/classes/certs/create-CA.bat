if "%1"=="" goto :eof

set PWD=123456

mkdir ca-%1

pushd ca-%1

:: Create keypair and CSR
openssl req -new -keyout cakey.pem -out careq.pem -config ../openssl.cfg

:: Self-sign
openssl x509 -signkey cakey.pem -req -days 3650 -in careq.pem -out ../ca-%1.cer -extensions v3_ca

echo 1234 > serial.txt

del /f /q ../cakey-%1.pem
copy cakey.pem ../cakey-%1.pem

popd ..

del /f /q "trust-%1.jks"
keytool -import -alias "%1" -file "ca-%1.cer" -keystore "trust-%1.jks" -storepass %PWD%
