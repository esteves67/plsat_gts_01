#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : gtsRun.sh
# -----------------------------------------------------------------------------

# --- GTS_HOME
if [ "${GTS_HOME}" = "" ]; then 
    if    [ -f "/usr/local/gts_vars.env" ]; then
        echo "Found /usr/local/gts_vars.env"
        . /usr/local/gts_vars.env
    elif  [ -f "/ws/mdflynn/gts/gts_vars.env" ]; then
        echo "Found /ws/mdflynn/gts/gts_vars.env"
        . /ws/mdflynn/gts/gts_vars.env
    else
        echo "Unable to determine GTS_HOME"
    fi
fi

# --- export vars
echo "GTS_HOME      = ${GTS_HOME}"
echo "JAVA_HOME     = ${JAVA_HOME}"
echo "CATALINA_HOME = ${CATALINA_HOME}"
export GTS_HOME
export JAVA_HOME
export CATALINA_HOME
cd $GTS_HOME

# --- command option
if   [ "$1" = "config" ]; then
    cmd="bin/gtsConfig.pl"
elif [ "$1" = "admin" ]; then
    cmd="bin/gtsAdmin.pl"
else
    cmd="$1"
fi

# --- run
${cmd} $2 $3 $4 $5 $6

# ---
