#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : gtsConfig.sh
# -----------------------------------------------------------------------------
# Usage:
#   % bin/gtsConfig.sh
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    if [ -f "/usr/local/gts_vars.env" ]; then
        . /usr/local/gts_vars.env
    fi
    if [ "${GTS_HOME}" = "" ]; then 
        echo "!!! ERROR: GTS_HOME not defined !!!"
        exit 99
    fi
fi
export GTS_HOME
. ${GTS_HOME}/bin/common.sh # - returns "$CPATH", "$GTS_CONF", ...
# -----------------------------------------------------------------------------
GTS_DEBUG=0

# --- usage (and exit)
function usage() {
    echo "Usage: $0"
    exit 1
}

# -----------------------------------------------------------------------------

# --- set location of Jar libraries
if [ ! -f "${JARDIR}/tools.jar" ]; then
    echo "ERROR: '${JARDIR}/tools.jar' not found!"
    echo "Possible reasons may include one or more of the following:"
    echo " - This command is not being run from the OpenGTS installation directory"
    echo " - The OpenGTS project has not been compiled properly"
fi

# --- extra classpath
CPATH="${CPATH}${PATHSEP}${JARDIR}/tools.jar"

# -----------------------------------------------------------------------------

# ---
#export PATH=$GTS_HOME/bin:$PATH

# --- Mac?
if [ $IS_MACOSX -eq 1 ]; then
    CMD_JAVA="${CMD_JAVA} -Xdock:name='GTSAdmin'"
fi

# ---
ENTRY_JAR="$GTS_HOME/build/lib/gtsadmin.jar"
COMMAND="${CMD_JAVA} -jar ${ENTRY_JAR} '-conf=${GTS_CONF}' -log.file.enable=false $1 $2 $3 $4 $5"
if [ $GTS_DEBUG -ne 0 ]; then
    echo "${COMMAND}"
fi
if [ ${IS_WINDOWS} -eq 1 ]; then
    ${COMMAND}
else
    eval "${COMMAND}"
fi

# --- exit normally
exit 0

# ---
