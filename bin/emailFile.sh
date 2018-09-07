#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : emailLogFile.sh
# -----------------------------------------------------------------------------
# Description:
#   This command can email the text contents of a file to a destination email address.
#   The "smtp.*" properties must be set up properly in order to send outbound email.
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "WARNING: GTS_HOME not defined!"
    GTS_HOME=".";  # - default to current dir
fi
. ${GTS_HOME}/bin/common.sh
# -----------------------------------------------------------------------------
GTS_DEBUG=0

# --- usage (and exit)
function usage() {
    echo "Usage: $0 [-to=<emailAddr>] -file=<file>"
    exit 1
}

# -----------------------------------------------------------------------------

LOG_FILE=
TO_ADDR=

# --- options
while (( "$#" )); do
    case "$1" in 

        # - log file
        "-file" )
            if [ $# -ge 2 ]; then
                LOG_FILE="$2"
                shift
            else
                echo "Missing file specification."
                exit 99
            fi
            shift
            ;;

        # - "To", email destination
        "-to" )
            if [ $# -ge 2 ]; then
                TO_ADDR="$2"
                shift
            else
                echo "Missing 'To' address specification."
                exit 99
            fi
            shift
            ;;

        # - error
        * )
            echo "Invalid argument!"
            usage
            exit 99
            ;;

    esac
done

# -----------------------------------------------------------------------------

# --- default "To:" address?
if [ "${TO_ADDR}" = "" ]; then
    TO_ADDR="\${ServiceProvider.email}";
fi

# -----------------------------------------------------------------------------

# --- Main entry point
JMAIN="org.opengts.util.SendMail"
JMAIN_ARGS="'-conf=${GTS_CONF}' -log.file.enable=false"

# ---
SUBJ="'-subject=[\${ServiceAccount.ID=opengts}] \${ServiceAccount.Name=OpenGTS} - ${LOG_FILE}'"
BODY="'-body=Attached:\n${LOG_FILE}'" # "'-body=file:${LOG_FILE}'"
ATCH="'-attach=${LOG_FILE}'"

# ---
CMD_ARGS="'-to=${TO_ADDR}' ${SUBJ} ${BODY} ${ATCH}"
COMMAND="${CMD_JAVA} -classpath ${CPATH} ${JMAIN} ${JMAIN_ARGS} ${CMD_ARGS}"
if [ $GTS_DEBUG -ne 0 ]; then
    echo "${COMMAND}"
fi
echo ""
if [ ${IS_WINDOWS} -eq 1 ]; then
    ${COMMAND}
else
    eval "${COMMAND}"
fi

# ---
