@echo off

:: download spring-boot-graal-feature
echo :::::::: Download spring-boot-graal-feature
rmdir /Q /S target\spring-boot-graal-feature > NUL 2>&1
git clone --single-branch --branch graal_19_2_0_dev https://github.com/aclement/spring-boot-graal-feature.git target/spring-boot-graal-feature

exit /b
