set PWD=123456

call mkcert validUser "CN=00CA0000AvalidUser@mynode.prom.mycluster.sbrf.ru, OU=00CA, O=Savings Bank of the Russian Federation, C=RU" one 3650
call mkcert validUser-expired "CN=00CA0000AvalidUser@mynode.prom.mycluster.sbrf.ru, OU=00CA, O=Savings Bank of the Russian Federation, C=RU" one -1

call mkcert serverNode "CN=00CA0000SserverNode@mynode.prom.mycluster.sbrf.ru, OU=00CA, O=Savings Bank of the Russian Federation, C=RU" one 3650
call mkcert clientNode "CN=00CA0000CclientNode@mynode.prom.mycluster.sbrf.ru, OU=00CA, O=Savings Bank of the Russian Federation, C=RU" one 3650
call mkcert userAdmin "CN=00CA0000GuserAdmin@mynode.prom.mycluster.sbrf.ru, OU=00CA, O=Savings Bank of the Russian Federation, C=RU" one 3650
call mkcert testLogin1 "CN=00CA0000AtestLogin1@mynode.prom.mycluster.sbrf.ru, OU=00CA, O=Savings Bank of the Russian Federation, C=RU" one 3650

call mkcert serverNodeInvalid1 "CN=00CA0000SserverNode@mynode.notprom.mycluster.sbrf.ru, OU=00CA, O=Savings Bank of the Russian Federation, C=RU" one 3650
call mkcert serverNodeInvalid2 "CN=00CA0000SserverNode@mynode.prom.notmycluster.sbrf.ru, OU=00CA, O=Savings Bank of the Russian Federation, C=RU" one 3650
