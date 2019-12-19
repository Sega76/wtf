call create-CA.bat one
call create-CA.bat two

set PWD=123456

del /f /q "trust-both.jks"
keytool -importkeystore -srckeystore trust-one.jks -srcstorepass %PWD% -destkeystore trust-both.jks -deststorepass %PWD%
keytool -importkeystore -srckeystore trust-two.jks -srcstorepass %PWD% -destkeystore trust-both.jks -deststorepass %PWD%
