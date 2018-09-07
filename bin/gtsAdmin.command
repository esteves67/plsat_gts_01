#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : gtsAdmin.command
# -----------------------------------------------------------------------------

# --- always reload environment vars
if [ -f "/usr/local/gts_vars.env" ]; then
    . /usr/local/gts_vars.env
fi

# --- make sure we have GTS_HOME defined
if [ "${GTS_HOME}" = "" ]; then 
    echo "Unable to determine GTS_HOME"
    sleep 3
    exit 1
fi

# --- start admin tool
cd ${GTS_HOME}
./bin/gtsAdmin.pl -allowDelete
