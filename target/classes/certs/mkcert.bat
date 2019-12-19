set ALIAS=%~1
if "%ALIAS%" == "" goto :eof

set DNAME=%~2
if "%DNAME%" == "" goto :eof

set CA=%~3
if "%CA%" == "" goto :eof

set DAYS=%~4
if "%DAYS%" == "" goto :eof

set PWD=123456

del /f /q "%ALIAS%.jks"

keytool -genkey -alias "%ALIAS%" -storepass %PWD% -keystore "%ALIAS%.jks" -keyalg RSA -keysize 2048 -validity 3650 -dname "%DNAME%" -keypass %PWD%

keytool -certreq -alias "%ALIAS%" -storepass %PWD% -keystore "%ALIAS%.jks" -file "%ALIAS%.csr" -keypass %PWD%

del /f /q "%ALIAS%.cer"

openssl x509 -CA "ca-%CA%.cer" -CAkey "ca-%CA%/cakey.pem" -CAserial "ca-%CA%/serial.txt" -req -in "%ALIAS%.csr" -out "%ALIAS%.cer" -days "%DAYS%"

del /f /q "%ALIAS%.csr"

keytool -import -trustcacerts -alias "%CA%" -keystore "%ALIAS%.jks" -file "ca-%CA%.cer" -storepass %PWD%
keytool -import -alias "%ALIAS%" -file "%ALIAS%.cer" -keystore "%ALIAS%.jks" -storepass %PWD%
keytool -delete -alias "%CA%" -keystore "%ALIAS%.jks" -storepass %PWD%
keytool -importkeystore -srcstoretype JKS -srckeystore "%ALIAS%.jks" -srcstorepass %PWD% -deststoretype PKCS12 -destkeystore "%ALIAS%.p12" -deststorepass %PWD% -destkeypass %PWD%

del /f /q "%ALIAS%.cer"
