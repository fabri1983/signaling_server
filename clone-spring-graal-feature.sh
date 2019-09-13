#!/bin/bash
# If you need to change permissions for execution then do: sudo chmod 775 clone-spring-graal-feature.sh

# download spring-graal-feature
echo :::::::: Download spring-graal-feature
rm -rf target/spring-graal-feature 2> /dev/null
git clone --single-branch --branch master https://github.com/spring-projects-experimental/spring-graal-feature.git target/spring-graal-feature
