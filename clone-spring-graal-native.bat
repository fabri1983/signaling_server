@echo off

:: download spring-graal-native
echo === Download spring-graalvm-native
rmdir /Q /S target\spring-graalvm-native > NUL 2>&1
git clone --single-branch --branch master https://github.com/spring-projects-experimental/spring-graalvm-native.git target/spring-graalvm-native

exit /b
