#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : installGTSUpgrade.sh
# -----------------------------------------------------------------------------
# Description:
#   GTS installation update
# Usage:
#   % bin/installGTSUpgrade.sh
# -----------------------------------------------------------------------------
VERSION="0.1.1"

# --- commands
CMD_chown="/bin/chown"
if [ ! -x "$CMD_chown" ]; then
    CMD_chown="/usr/sbin/chown"
fi
if [ ! -x "$CMD_chown" ]; then
    echo "Command 'chown' not found"
    exit 1
fi
CMD_chmod="/bin/chmod"
if [ ! -x "$CMD_chmod" ]; then
    echo "Command 'chmod' not found"
    exit 1
fi
CMD_whoami="/usr/bin/whoami"
if [ ! -x "$CMD_whoami" ]; then
    echo "Command 'whoami' not found"
    exit 1
fi
CMD_su="/bin/su"
if [ ! -x "$CMD_su" ]; then
    CMD_su="/usr/bin/su"
fi
if [ ! -x "$CMD_su" ]; then
    echo "Command 'su' not found"
    exit 1
fi
CMD_ln="/bin/ln"
if [ ! -x "$CMD_ln" ]; then
    echo "Command 'ln' not found"
    exit 1
fi
CMD_rm="/bin/rm"
if [ ! -x "$CMD_rm" ]; then
    echo "Command 'rm' not found"
    exit 1
fi
CMD_pwd="/bin/pwd"
if [ ! -x "$CMD_pwd" ]; then
    echo "Command 'pwd' not found"
    exit 1
fi
CMD_sleep="/bin/sleep"
if [ ! -x "$CMD_sleep" ]; then
    echo "Command 'sleep' not found"
    exit 1
fi
CMD_dirname="/usr/bin/dirname"
if [ ! -x "$CMD_dirname" ]; then
    echo "Command 'dirname' not found"
    exit 1
fi

# --- running as 'root'?
IS_ROOT=0
WHOAMI=`${CMD_whoami}`
if [ "${WHOAMI}" = "root" ]; then
    IS_ROOT=1
fi

# --- current directory
#COMMAND_DIR=`${CMD_dirname} $0`
THIS_GTS_DIR=`(cd -P $(dirname $0)/..; ${CMD_pwd})`
if [ -f "${THIS_GTS_DIR}/private.xml" ]; then
    echo "GTS directory: ${THIS_GTS_DIR}"
else
    echo "Invalid directory: ${THIS_GTS_DIR}"
    THIS_GTS_DIR=""
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# --- functions

# --- set execute bit on executable command script
function setExecutable()
{
    # arg $1 = command name
    local CMD="$1"
    if [ -f "${GTS_HOME}/bin/${CMD}" ]; then
        echo "Command: (${CMD_chmod} a+x ${GTS_HOME}/bin/${CMD})"
        ${CMD_chmod} a+x "${GTS_HOME}"/bin/${CMD}
    fi
}

# --- usage (and exit)
function usage() {
    # arg $1 = exit code
    local exitCode=$1
    echo ""
    echo "Usage: $0 -dir <GTS_install_dir> -user <GTS_user> -gtsLink"
    echo "  or"
    echo "Usage: $0 -help"
    echo ""
    echo "Options:"
    echo "   -dir <GTS_install_dir>  - The GTS installation directory (ie. '/usr/local/GTS_2.3.7-B10')"
    echo "   -user <GTS_user>        - User that owns GTS installation directory (ie. 'opengts')"
    echo "   -gtslink                - Create symbolic link '/usr/local/gts'"
    echo "   -dbupdate               - Run 'bin/dbAdmin.pl -tables=cak' to update tables"
   #echo "   -tomcat                 - Restart Tomcat."
    echo "   -help                   - This displayed help"
    echo ""
    echo "Notes    : "
    echo "   1) This update script assumes that the prerequisite components for GTS to operate  "
    echo "      have already been installed (ie. Java, Ant, MySQL, etc)."
    echo "   2) Currently, this update script expects the parent installation directory to be "
    echo "      '/usr/local'.  This allows this update script to make some assumptions about the "
    echo "      location of the 'gts' symbolic link, and other links."
    echo "   3) This script should be run as 'root' to complete the changing of the GTS installation"
    echo "      directory owner, and create the /usr/local/gts symbolic link."
    echo ""
    exit ${exitCode}
}

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# --- check command arguments
GTS_HOME=""
GTS_BASE=""
GTS_USER=""
GTS_GROUP=""
CHECKINSTALL=0
DB_UPDATE=0
CREATE_GTS_LINK=0
while (( "$#" )); do
    case "$1" in 

        # ------------------------------------------------------

        # - help
        "-help" | "-h" ) 
            usage 0
            ;;

        # ------------------------------------------------------

        # - GTS_HOME
        "-GTS_HOME" | "-home" | "-dir" ) 
            if [ $# -ge 2 ]; then
                GTS_HOME=`(cd -P $2; /bin/pwd)`
                export GTS_HOME
                shift
            else
                echo ""
                echo "Missing GTS_HOME argument"
                usage 99
            fi
            ;;

        # - user
        "-GTS_USER" | "-user" | "-u" ) 
            if [ $# -ge 2 ]; then
                GTS_USER="$2"
                export GTS_USER
                shift
            else
                echo ""
                echo "*** Missing '-user' argument"
                usage 99
            fi
            ;;

        # - group
        "-GTS_GROUP" | "-group" | "-g" ) 
            if [ $# -ge 2 ]; then
                GTS_GROUP="$2"
                export GTS_GROUP
                shift
            else
                echo ""
                echo "*** Missing '-group' argument"
                usage 99
            fi
            ;;

        # ------------------------------------------------------

        # - create GTS link
        "-gtslink" | "-gtsLink" | "-link" ) 
            CREATE_GTS_LINK=1
            ;;

        # ------------------------------------------------------

        # - DB update: dbAdmin.pl -tables=cak
        "-dbupdate" | "-dbUpdate" | "-db" ) 
            DB_UPDATE=1
            ;;

        # - no db update
        "-nodbupdate" | "-noDbUpdate" | "-nodb" ) 
            DB_UPDATE=0
            ;;

        # ------------------------------------------------------

        # - checkinstall
        "-checkinstall" | "-checkInstall" | "-ci" ) 
            CHECKINSTALL=1
            ;;

        # - no checkinstall
        "-nocheckinstall" | "-noCheckInstall" | "-noci" ) 
            CHECKINSTALL=0
            ;;

        # ------------------------------------------------------

        # - error
        * )
            echo ""
            echo "*** Invalid parameter! $1"
            usage 99
            ;;

    esac
    shift
done

# --- default GTS_HOME
if [ "${GTS_HOME}" == "" ] && [ "${THIS_GTS_DIR}" != "" ]; then
    GTS_HOME="${THIS_GTS_DIR}"
fi

# --- derivative vars
GTS_BASE=""
if [ "${GTS_HOME}" != "" ]; then
    GTS_BASE=$(basename $GTS_HOME)
fi
#GTS_EXTN=${GTS_HOME##*.}
#GTS_FILE=${GTS_HOME%.*}
INSTALL_DIR=`(cd -P ${GTS_HOME}/..; /bin/pwd)`
GTS_BIN="${GTS_HOME}/bin"

# -----------------------------------------------------------------------------
# --- pre qualify installation update

# --- check for proper GTS_HOME
if [ ! -d "${GTS_BIN}" ] || [ ! -f "${GTS_BIN}/makeGtsVarsEnv.sh" ]; then
    echo ""
    echo "Missing '-dir' option"
    usage 99
fi

# --- check for proper INSTALL_DIR
if [ "${INSTALL_DIR}" != "/usr/local" ]; then
    echo ""
    echo "Invalid installation parent directory"
    echo "  Expecting: /usr/local"
    echo "  Found    : ${INSTALL_DIR}"
    echo ""
    exit 1
fi

# --- check for proper GTS_USER
if [ "${GTS_USER}" = "" ]; then
    echo ""
    echo "Missing '-user' option"
    usage 99
fi

# --- check for "/usr/local/gts"
if [ $CREATE_GTS_LINK -eq 1 ]; then
    if   [ ${IS_ROOT} -eq 0 ]; then
        echo ""
        echo "Not running as 'root'."
        echo "Cannot create '${INSTALL_DIR}/gts' symbolic link."
        echo "Rerun command without '-gtsLink' option."
        exit 1
    elif [ -h "${INSTALL_DIR}/gts" ]; then
        echo ""
        echo "Symbolic link '${INSTALL_DIR}/gts' already exists."
        echo "Please remove existing link before continuing."
        exit 1
    elif [ -e "${INSTALL_DIR}/gts" ]; then
        echo ""
        echo "'${INSTALL_DIR}/gts' already exists, but is not a symbolic link!"
        echo "Cannot create symbolic link - rerun command without '-gtsLink' option."
        exit 2
    elif [ "${GTS_BASE}" = "" ]; then
        echo ""
        echo "Unable to determine '${GTS_HOME}' base directory name!"
        exit 3
    fi
else
    if [ ! -h "${INSTALL_DIR}/gts" ]; then
        echo "" 
        echo "Symbolic link does not exist: ${INSTALL_DIR}/gts (required for installation)"
        if [ ${IS_ROOT} -eq 1 ]; then
            echo "Re-run with '-gtsLink' option."
        else
            echo "Re-run as 'root', with '-gtsLink' option."
        fi
        exit 4
    fi
fi

# --- precheck "${INSTALL_DIR}/java" and "${INSTALL_DIR}/tomcat"
if [ ! -h "${INSTALL_DIR}/java" ]; then
    echo ""
    echo "Symbolic link does not exist: ${INSTALL_DIR}/java"
    echo "(Symbolic link '${INSTALL_DIR}/java' should point to the Java JDK directory)"
    exit 5
fi
if [ ! -h "${INSTALL_DIR}/tomcat" ]; then
    echo ""
    echo "Symbolic link does not exist: ${INSTALL_DIR}/tomcat"
    echo "(Symbolic link '${INSTALL_DIR}/tomcat' should point to the Tomcat installation directory)"
    exit 5
fi

# -----------------------------------------------------------------------------
# --- header
echo "====================================================================================="
echo "Version  : ${VERSION}"
echo "Inst. Dir: ${INSTALL_DIR}"
echo "GTS_HOME : ${GTS_HOME}"
echo "GTS Base : ${GTS_BASE}"
echo "GTS User : ${GTS_USER}"
if [ ${IS_ROOT} -eq 1 ]; then
echo "'root'   : TRUE"
else
echo "'root'   : FALSE"
fi

# -----------------------------------------------------------------------------
# --- perform upgrade tasks

# --- set execute bit on various commands
echo ""
echo "====================================================================================="
echo "Setting executable bit on GTS commands in ${GTS_HOME}/bin ..."
echo "Command: (${CMD_chmod} a+x ${GTS_HOME}/bin/*.sh)"
${CMD_chmod} a+x "${GTS_HOME}"/bin/*.sh
echo "Command: (${CMD_chmod} a+x ${GTS_HOME}/bin/*.pl)"
${CMD_chmod} a+x "${GTS_HOME}"/bin/*.pl
setExecutable gtsAdmin.command
setExecutable gtsConfig.command
setExecutable psJava
setExecutable exeJava
setExecutable readTCP
setExecutable readUDP
setExecutable writeTCP
setExecutable writeUDP
if [ -d "${GTS_HOME}/bcross" ]; then
    echo "Command: (${CMD_chmod} a+rx ${GTS_HOME}/bcross/*.sh)"
    ${CMD_chmod} a+rx "${GTS_HOME}"/bcross/*.sh
    echo "Command: (${CMD_chmod} a+r ${GTS_HOME}/bcross/*.psql)"
    ${CMD_chmod} a+r  "${GTS_HOME}"/bcross/*.psql
    echo "Command: (${CMD_chmod} a+r ${GTS_HOME}/bcross/*.zip)"
    ${CMD_chmod} a+r  "${GTS_HOME}"/bcross/*.zip
fi
echo "... done"
${CMD_sleep} 1

# --- change owner
if [ ${IS_ROOT} -eq 1 ] && [ "${GTS_USER}" != "" ]; then
    echo ""
    echo "====================================================================================="
    if [ "${GTS_GROUP}" != "" ]; then
        echo "Changing ownership of GTS home (${GTS_HOME}) to user:group '${GTS_USER}:${GTS_GROUP}' ..."
        echo "Command: (${CMD_chown} -R ${GTS_USER}:${GTS_GROUP} ${GTS_HOME})"
        ${CMD_chown} -R ${GTS_USER}:${GTS_GROUP} ${GTS_HOME}
    else
        echo "Changing ownership of GTS home (${GTS_HOME}) to user '${GTS_USER}' ..."
        echo "Command: (${CMD_chown} -R ${GTS_USER} ${GTS_HOME})"
        ${CMD_chown} -R ${GTS_USER} ${GTS_HOME}
    fi
    if [ $? -ne 0 ]; then
        echo "... fail"
        exit 1
    else
        echo "... done"
        ${CMD_sleep} 1
    fi
fi

# --- create 'gts' symbolic link
if [ $CREATE_GTS_LINK -eq 1 ]; then
    echo ""
    echo "====================================================================================="
    echo "Creating '${INSTALL_DIR}/gts' symbolic link ..."
    echo "Command: (${CMD_ln} -s ${GTS_BASE} ${INSTALL_DIR}/gts)"
    ${CMD_ln} -s ${GTS_BASE} ${INSTALL_DIR}/gts
    if [ $? -ne 0 ]; then
        echo "... fail"
        exit 1
    else
        echo "... done"
        ${CMD_sleep} 1
    fi
fi

# --- create "gts_vars.env"
if [ "${GTS_USER}" != "" ]; then
    echo ""
    echo "====================================================================================="
    echo "Creating local ${GTS_HOME}/gts_vars.env ..."
    ${CMD_rm} -f ${GTS_HOME}/gts_vars.env
    ${GTS_HOME}/bin/makeGtsVarsEnv.sh -installDir ${INSTALL_DIR} -user ${GTS_USER} -out ${GTS_HOME}/gts_vars.env
    if [ ! -f "${GTS_HOME}/gts_vars.env" ]; then
        echo "... failed"
        exit 1
    fi
    if [ ${IS_ROOT} -eq 1 ]; then
        if [ "${GTS_GROUP}" != "" ]; then
            ${CMD_chown} ${GTS_USER}:${GTS_GROUP} ${GTS_HOME}/gts_vars.env
        else
            ${CMD_chown} ${GTS_USER} ${GTS_HOME}/gts_vars.env
        fi
    fi
    echo "... done"
    ${CMD_sleep} 1
    if [ ${IS_ROOT} -eq 1 ]; then
        if [ ! -f "${INSTALL_DIR}/gts_vars.env" ]; then
            echo ""
            echo "====================================================================================="
            echo "Creating ${INSTALL_DIR}/gts_vars.env ..."
            ${GTS_HOME}/bin/makeGtsVarsEnv.sh -installDir ${INSTALL_DIR} -user ${GTS_USER} -out ${INSTALL_DIR}/gts_vars.env
            if [ ! -f "${INSTALL_DIR}/gts_vars.env" ]; then
                echo "... failed"
                exit 1
            fi
            echo "... done"
            ${CMD_sleep} 1
        else
            echo ""
            echo "====================================================================================="
            echo "File ${INSTALL_DIR}/gts_vars.env already exists ..."
            ${CMD_sleep} 1
        fi
    fi
fi

# --- update tables
echo ""
echo "====================================================================================="
if [ $DB_UPDATE -eq 1 ]; then
    echo "Updating GTS tables ..."
    if [ ${IS_ROOT} -eq 1 ]; then
        echo "Command: ${CMD_su} ${GTS_USER} -c \"( . ${INSTALL_DIR}/gts_vars.env; export GTS_HOME=${GTS_HOME}; cd ${GTS_HOME}; bin/dbAdmin.pl -tables=cak )\""
        ${CMD_su} ${GTS_USER} -c "( . ${INSTALL_DIR}/gts_vars.env; export GTS_HOME=${GTS_HOME}; cd ${GTS_HOME}; bin/dbAdmin.pl -tables=cak )"
    else
        echo "Command: ( . ${INSTALL_DIR}/gts_vars.env; export GTS_HOME=${GTS_HOME}; cd ${GTS_HOME}; bin/dbAdmin.pl -tables=cak )"
        ( . ${INSTALL_DIR}/gts_vars.env; export GTS_HOME=${GTS_HOME}; cd ${GTS_HOME}; bin/dbAdmin.pl -tables=cak )
    fi
else
    echo "Checking GTS tables (tables will not be updated) ..."
    if [ ${IS_ROOT} -eq 1 ]; then
        echo "Command: ${CMD_su} ${GTS_USER} -c \"( . ${INSTALL_DIR}/gts_vars.env; export GTS_HOME=${GTS_HOME}; cd ${GTS_HOME}; bin/dbAdmin.pl -tables )\""
        ${CMD_su} ${GTS_USER} -c "( . ${INSTALL_DIR}/gts_vars.env; export GTS_HOME=${GTS_HOME}; cd ${GTS_HOME}; bin/dbAdmin.pl -tables )"
    else
        echo "Command: ( . ${INSTALL_DIR}/gts_vars.env; export GTS_HOME=${GTS_HOME}; cd ${GTS_HOME}; bin/dbAdmin.pl -tables )"
        ( . ${INSTALL_DIR}/gts_vars.env; export GTS_HOME=${GTS_HOME}; cd ${GTS_HOME}; bin/dbAdmin.pl -tables )
    fi
fi
echo "... done"
${CMD_sleep} 1

# --- run checkInstall
CMD_CHECKINSTALL=". ${INSTALL_DIR}/gts_vars.env ; cd ${GTS_HOME} ; bin/checkInstall.sh"
if [ $CHECKINSTALL -eq 1 ]; then
    echo ""
    echo "====================================================================================="
    echo "Running CheckInstall ..."
    if [ ${IS_ROOT} -eq 1 ]; then
        echo "${CMD_su} ${GTS_USER} -c \"( ${CMD_CHECKINSTALL} )\""
        ${CMD_su} ${GTS_USER} -c "( ${CMD_CHECKINSTALL} )"
    else
        echo "( ${CMD_CHECKINSTALL} )"
        ( . ${INSTALL_DIR}/gts_vars.env ; cd ${GTS_HOME} ; bin/checkInstall.sh )
    fi
    echo ""
    ${CMD_sleep} 1
else
    echo ""
    echo "====================================================================================="
    echo "Skipping CheckInstall ..."
    echo "Execute the following command to run CheckInstall manually:"
    if [ ${IS_ROOT} -eq 1 ]; then
        echo "${CMD_su} ${GTS_USER} -c \"( ${CMD_CHECKINSTALL} )\""
    else
        echo "( ${CMD_CHECKINSTALL} )"
    fi
fi

# --- post install messages
echo ""
echo "====================================================================================="
echo "Upgrade should be complete."
echo "Manually check ${INSTALL_DIR} directory for proper installation"
echo ""

# -----------------------------------------------------------------------------
# ---
