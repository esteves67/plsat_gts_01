#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : gtsRequest.sh
# -----------------------------------------------------------------------------
# Description:
#   Command-line utility for requesting GTS web-service functions
# Example:
#   bin/gtsRequest.sh -file=someRequestFile.xml
#   (assumes "GTSRequest.url" has been defined)
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "!!! ERROR: GTS_HOME not defined !!!"
    exit 99
fi
# -----------------------------------------------------------------------------

# --- make sure service URL is defined
if [ "${SERVICE_URL}" = "" ]; then
    export SERVICE_URL="http://localhost:8080/track/Service"
fi

# --- check arguments
QUIET="-quiet"
ARG_OPTIONS=""
while (( "$#" )); do
    case "$1" in 

        # ------------------------------------------------------

        # - debug
        "-debug" | "-debugMode" ) 
            ARG_OPTIONS="${ARG_OPTIONS} -debugMode"
            QUIET=""
            ;;

        # - quiet
        "-quiet" | "-q" ) 
            ARG_OPTIONS="${ARG_OPTIONS} -verbose=false"
            QUIET="-quiet"
            ;;

        # ------------------------------------------------------

        # - account
        "-a" | "-account" ) 
            if [ $# -ge 2 ]; then
                ARG_OPTIONS="${ARG_OPTIONS} -account=$2"
                shift
            else
                echo "Missing '-account' argument"
                exit 99
            fi
            ;;

        # - user
        "-u" | "-user" ) 
            if [ $# -ge 2 ]; then
                ARG_OPTIONS="${ARG_OPTIONS} -user=$2"
                shift
            else
                echo "Missing '-user' argument"
                exit 99
            fi
            ;;

        # - password
        "-p" | "-password" ) 
            if [ $# -ge 2 ]; then
                ARG_OPTIONS="${ARG_OPTIONS} -password=$2"
                shift
            else
                echo "Missing '-password' argument"
                exit 99
            fi
            ;;

        # ------------------------------------------------------

        # - file
        "-f" | "-file" | "-xml" ) 
            if [ $# -ge 2 ]; then
                ARG_OPTIONS="${ARG_OPTIONS} -file=$2"
                shift
            else
                echo "Missing '-file' request argument"
                exit 99
            fi
            ;;

        # ------------------------------------------------------

        # - device
        "-d" | "-device" ) 
            if [ $# -ge 2 ]; then
                ARG_OPTIONS="${ARG_OPTIONS} -device=$2"
                shift
            else
                echo "Missing '-device' request argument"
                exit 99
            fi
            ;;

        # - command
        "-c" | "-command" ) 
            if [ $# -ge 2 ]; then
                ARG_OPTIONS="${ARG_OPTIONS} -command=$2"
                shift
            else
                echo "Missing '-command' request argument"
                exit 99
            fi
            ;;

        # - command arg
        "-ca" | "-arg" ) 
            if [ $# -ge 2 ]; then
                ARG_OPTIONS="${ARG_OPTIONS} -arg=$2"
                shift
            else
                echo "Missing '-arg' request argument"
                exit 99
            fi
            ;;

        # - table (for "dbschema" command)
        "-t" | "-table" ) 
            if [ $# -ge 2 ]; then
                ARG_OPTIONS="${ARG_OPTIONS} -table=$2"
                shift
            else
                echo "Missing '-table' request argument"
                exit 99
            fi
            ;;

        # ------------------------------------------------------

        # - skip remaining args
        "--" )
            shift
            break
            ;;

        # - error
        * )
            echo "Invalid argument! $1"
            exit 99
            ;;

    esac
    shift
done

# --- command
${GTS_HOME}/bin/exeJava ${QUIET} org.opengts.extra.service.ServiceClient ${ARG_OPTIONS} $1 $2 $3 $4

# ---

