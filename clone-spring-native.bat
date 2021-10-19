@echo off

:: download spring-native
echo === Download spring-native
rmdir /Q /S target\spring-native > NUL 2>&1
git clone --single-branch --branch master https://github.com/spring-projects-experimental/spring-native.git target/spring-native

exit /b
