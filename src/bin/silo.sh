#!/bin/sh
#
# Copyright 2011 by Salman Ahmad (salman@salmanahmad.com).
# All rights reserved.
#
# Permission is granted for use, copying, modification, distribution,
# and distribution of modified versions of this work as long as the
# above copyright notice is included.
#



# Determine the location of this script file.

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



# Process the command-line arguments.

declare -a java_args
declare -a silo_args

while [ $# -gt 0 ]
do
    case "$1" in
        --java.cp|-j.cp|--classpath|-cp )
            # Add the next argument to the classpath
            CLASSPATH="$CLASSPATH:$2"
            shift 2
            ;;
        --java.* )
            java_args=("${java_args[@]}" "-${1:7}")
            shift
            ;;
        -j.* )
            java_args=("${java_args[@]}" "-${1:3}")
            shift
            ;;
        * )
            silo_args=("${silo_args[@]}" "$1")
            shift
            ;;
    esac
done



# Build the classpath

DIR_NAME=`dirname "$PRG"`
# TODO - I need to switch over to the jar-with-dependencies at some point.
# JAR_NAME="silo.jar"

DEP_PATH="$DIR_NAME/../../target/dependency/*"
BUILD_PATH="$DIR_NAME/../../target/classes"
#JAR_PATH="$DIR_NAME/../../target/lib/$JAR_NAME"

CLASSPATH="$CLASSPATH:$DEP_PATH:$BUILD_PATH:$JAR_PATH"
COMMAND_NAME="silo.lang.Main"

exec java "${java_args[@]}" -classpath "$CLASSPATH" "$COMMAND_NAME" "${silo_args[@]}"
