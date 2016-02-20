#!/bin/bash

export JDK_PATH=jdk1.8.0_74

export JAVA_HOME=${OPENSHIFT_DATA_DIR}${JDK_PATH}
export PATH=$JAVA_HOME/bin:$PATH

./activator run
