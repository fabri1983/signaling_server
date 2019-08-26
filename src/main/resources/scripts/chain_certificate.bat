@echo off

del /F /Q local-keystore.jks > NUL 2>&1
del /F /Q rootca.jks localca.jks serverca.jks local-keystore.jks > NUL 2>&1
del /F /Q rootca.pem localca.pem localca_temp.pem serverca.pem serverca_temp.pem > NUL 2>&1

echo ===================================================
echo Creating fake third-party chain rootca with localca
echo ===================================================

:: generate private keys for root and local
keytool -genkeypair -alias rootca -dname "cn=Local Development (Root)" -validity 1095 -keyalg RSA -keysize 2048 -ext bc:c -keystore rootca.jks -keypass rootcapass -storepass rootcapass
keytool -genkeypair -alias localca -dname "cn=Local Development" -validity 1095 -keyalg RSA -keysize 2048 -ext bc:c -keystore localca.jks -keypass localcapass -storepass localcapass

:: generate root certificate
keytool -exportcert -rfc -alias rootca -keystore rootca.jks -storepass rootcapass -file rootca.pem

:: generate chain local certificate signed by root (rootca -> localca)
keytool -keystore localca.jks -storepass localcapass -certreq -alias localca -file localca_temp.pem
keytool -keystore rootca.jks -storepass rootcapass -gencert -alias rootca -ext bc=0 -ext san=dns:ca -rfc -infile localca_temp.pem -outfile localca.pem

:: import ca cert chain (rootca -> localca) into localca.jks
keytool -keystore localca.jks -storepass localcapass -importcert -trustcacerts -noprompt -alias rootca -file rootca.pem
keytool -keystore localca.jks -storepass localcapass -importcert -alias localca -file localca.pem

echo =======================================================================
echo Fake third-party chain generated. Now generating local-keystore.jks ...
echo =======================================================================

:: generate private keys for server
keytool -genkeypair -alias serverca -dname cn=Server -validity 1095 -keyalg RSA -keysize 2048 -keystore local-keystore.jks -keypass servercapass -storepass servercapass

:: generate chain certificate for server signed by local (rootca -> localca -> serverca)
keytool -keystore local-keystore.jks -storepass servercapass -certreq -alias serverca -file serverca_temp.pem
keytool -keystore localca.jks -storepass localcapass -gencert -alias localca -ext ku:c=dig,keyEnc ^
 -ext "san=dns:localhost,ip:127.0.0.1,ip:192.168.99.100,ip:192.168.99.101,ip:192.168.99.102,ip:172.17.0.2,ip:172.17.0.3" ^
 -ext eku=sa,ca -rfc -infile serverca_temp.pem -outfile serverca.pem

:: import server cert chain into local-keystore.jks
keytool -keystore local-keystore.jks -storepass servercapass -importcert -trustcacerts -noprompt -alias rootca -file rootca.pem
keytool -keystore local-keystore.jks -storepass servercapass -importcert -alias localca -file localca.pem
keytool -keystore local-keystore.jks -storepass servercapass -importcert -alias serverca -file serverca.pem

del /F /Q rootca.jks localca.jks serverca.jks > NUL 2>&1
del /F /Q rootca.pem localca.pem localca_temp.pem serverca.pem serverca_temp.pem > NUL 2>&1
