@echo off

:: download spring-graal-native
echo :::::::: Download spring-graal-native
rmdir /Q /S target\spring-graal-native > NUL 2>&1
git clone --single-branch --branch master https://github.com/spring-projects-experimental/spring-graal-native.git target/spring-graal-native

exit /b
