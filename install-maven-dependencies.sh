#!/bin/bash

mvn install:install-file -Dfile=lib/simple-3.1.3.jar -DgroupId=org.simpleframework -DartifactId=simple -Dversion=3.1.3 -D packaging=jar