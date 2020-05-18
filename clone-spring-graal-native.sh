#!/bin/bash
# If you need to change permissions for execution then do: sudo chmod 775 clone-spring-graalvm-native.sh

# download spring-graal-native
echo === Download spring-graalvm-native
rm -rf target/spring-graalvm-native 2> /dev/null
git clone --single-branch --branch master https://github.com/spring-projects-experimental/spring-graalvm-native.git target/spring-graalvm-native
