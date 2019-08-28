#!/bin/bash
# If you need to change permissions for execution then do: sudo chmod 775 clone-spring-boot-graal-feature.sh

# download spring-boot-graal-feature
echo :::::::: Download spring-boot-graal-feature
rm -rf target/spring-boot-graal-feature 2> /dev/null
git clone --single-branch --branch graal_19_2_0_dev https://github.com/aclement/spring-boot-graal-feature.git target/spring-boot-graal-feature
