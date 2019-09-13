@echo off

:: download spring-graal-feature
echo :::::::: Download spring-graal-feature
rmdir /Q /S target\spring-graal-feature > NUL 2>&1
git clone --single-branch --branch master https://github.com/spring-projects-experimental/spring-graal-feature.git target/spring-graal-feature

exit /b
