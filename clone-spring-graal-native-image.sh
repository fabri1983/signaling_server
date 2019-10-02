#!/bin/bash
# If you need to change permissions for execution then do: sudo chmod 775 clone-spring-graal-native-image.sh

# download spring-graal-native-image
echo :::::::: Download spring-graal-native-image
rm -rf target/spring-graal-native-image 2> /dev/null
git clone --single-branch --branch master https://github.com/spring-projects-experimental/spring-graal-native-image.git target/spring-graal-native-image
