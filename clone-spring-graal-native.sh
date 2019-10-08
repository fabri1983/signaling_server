#!/bin/bash
# If you need to change permissions for execution then do: sudo chmod 775 clone-spring-graal-native.sh

# download spring-graal-native
echo :::::::: Download spring-graal-native
rm -rf target/spring-graal-native 2> /dev/null
git clone --single-branch --branch master https://github.com/spring-projects-experimental/spring-graal-native.git target/spring-graal-native
