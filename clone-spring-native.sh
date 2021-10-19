#!/bin/bash
# If you need to change permissions for execution then do: sudo chmod 775 clone-spring-native.sh

echo === Download spring-native
rm -rf target/spring-native 2> /dev/null
git clone --single-branch --branch master https://github.com/spring-projects-experimental/spring-native.git target/spring-native
