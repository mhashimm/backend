#!/bin/bash

export OPENSHIFT_EXT_MYSQL_DB_HOST=frontend-sisdn.rhcloud.com
export OPENSHIFT_EXT_MYSQL_DB_PORT=3306
export OPENSHIFT_EXT_MYSQL_DB_USER=adminYqSuJsi
export OPENSHIFT_EXT_MYSQL_DB_NAME=frontend

sbt run
