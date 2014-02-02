#!/bin/sh
#
# Copyright 2011 by Salman Ahmad (salman@salmanahmad.com).
# All rights reserved.
#
# Permission is granted for use, copying, modification, distribution,
# and distribution of modified versions of this work as long as the
# above copyright notice is included.
#


# resolve links - $0 may be a soft-link
PRG="$0"

while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done


DIR_NAME=`dirname "$PRG"`
# TODO - I need to switch over to the jar-with-dependencies at some point.
# JAR_NAME="silo.jar"

DEP_PATH="$DIR_NAME/../../target/dependency/*"
BUILD_PATH="$DIR_NAME/../../target/classes"
#JAR_PATH="$DIR_NAME/../../target/lib/$JAR_NAME"

CLASSPATH="$DEP_PATH:$BUILD_PATH:$JAR_PATH"
COMMAND_NAME="silo.lang.Main"

exec java -classpath "$CLASSPATH" "$COMMAND_NAME"  "$@"
