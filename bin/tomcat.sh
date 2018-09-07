#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : tomcat.sh
# -----------------------------------------------------------------------------

# --- GTS_HOME
if [ "${GTS_HOME}" = "" ]; then 
    if [ -f "/usr/local/gts_vars.env" ]; then
        . /usr/local/gts_vars.env
    else
        GTS_HOME=".";  # - default to current dir
    fi
fi
echo "GTS_HOME = ${GTS_HOME}"

# --- CATALINA_HOME
if [ "${CATALINA_HOME}" = "" ]; then 
    echo "Unable to determine CATALINA_HOME"
    exit 1
fi

# --- options
while (( "$#" )); do
    case "$1" in 

        # - Start
        "-start" | "start" | "-startup" | "startup" ) 
            "${CATALINA_HOME}/bin/startup.sh"  &
            exit 0
            ;;

        # - Stop
        "-stop" | "stop" | "-shutdown" | "shutdown" ) 
            "${CATALINA_HOME}/bin/shutdown.sh"  &
            exit 0
            ;;

    esac
done

# ---
