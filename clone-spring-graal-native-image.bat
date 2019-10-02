@echo off

:: download spring-graal-native-image
echo :::::::: Download spring-graal-native-image
rmdir /Q /S target\spring-graal-native-image > NUL 2>&1
git clone --single-branch --branch master https://github.com/spring-projects-experimental/spring-graal-native-image.git target/spring-graal-native-image

exit /b
