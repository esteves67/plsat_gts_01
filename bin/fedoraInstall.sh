#!/bin/bash
# -----------------------------------------------------------------------------
#
# The following environment variables are assumed:
#   $ARCH         : The 32/64-bit system architecture indicator (either "i386" or "x86_64")
#   $INSTALL_DIR  : "/usr/local" (manual installation)
#   $GTS_USER     : The user id which will own the GTS installation
#   $GTS_HOME     : The directory where GTS is installed (or will be installed)
#   $JAVA_HOME    : The directory where Java is installed (manual installation)
#       This will be one of the following:
#           /usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0.${ARCH}
#           /usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0
#
# Internal set vars:
#   $ANT_DIR      : "apache-ant-1.7.1"      (not currently used)
#   $TOMCAT_DIR   : "apache-tomcat-5.5.27"
#
# ---------------------------------------------------------------------
# Synopsis of installed packages:
# 
# Fedora: Installed via 'yum' (some of which may already be installed):
#  - openssh-server                 sshd
#  - openssh-clients                ssh, scp  (esp if error "bash: scp: command not found")
#  - jwhois                         whois (good-grief, why can'it just be 'whois'?)
#  - unzip                          to unzip packages
#  - wget                           to download Tomcat
#  - telnet                         to easily test socket connectivity
#  - bind-utils                     for 'nsloolkup', etc
#  - mysql                          MySQL client
#  - mysql-server                   MySQL server
#  - mysql-connector-java           JDBC connector [missing on CentOS?]
#  - java-1.6.0-openjdk-devel       Java
#  - bitmap-fonts                   Fonts for Java
#  - xorg-x11-fonts-Type1           Fonts for Java
#  - xorg-x11-fonts-100dpi          Fonts for Java
#  - xorg-x11-fonts-75dpi           Fonts for Java
#  - ant                            Apache Ant
#  - xorg-x11-xauth                 (optional) XHosting support
#  - httpd php php-mysql            (optional) misc web support
#  - postgresql postgresql-server   (optional) stateline border crossing
#  - postgis postgresql-jdbc        (optional) stateline border crossing
#
# Alternate locations for 'mysql-connector-java': (libmysql-java?)
#  - http://download.softagency.net/MySQL/Downloads/Connector-J/mysql-connector-java-5.1.5.tar.gz
#  - jpackage respository: http://www.jpackage.org/yum.php
# 
# Debian/Ubuntu: Installed via 'apt-get':
#  Run "apt-get update" first
#  "apt-get install xxx"
#  - unzip                          to unzip packages
#  - wget                           to download Tomcat
#  - telnet                         to easily test socket connectivity
#  - apt-file                       search for missing Debian packages
#  - dnsutils                       for 'nsloolkup', etc
#  - mysql-client                   MySQL client
#  - mysql-server                   MySQL server
#  - libmysql-java                  JDBC connector [/usr/share/java/mysql-connector-java-5.1.6.jar]
#  - sun-java6-jdk                  Java (requires user input to agree to license)
#                                   run "update-alternatives --config java"
#                                   or "update-java-alternatives -s java-6-sun"
#                                   http://wiki.debian.org/Java
#                                   http://wiki.debian.org/Java/Sun
#  - openjdk-6-jdk                  Possible alternative to "sun-java6-jdk" (not yet confirmed)
#  - ant                            Apache Ant
#  - xorg-x11-xauth                 (optional) XHosting support
#  - httpd php php-mysql            (optional) misc web support
#  - postgresql postgresql-server   (optional) stateline border crossing
#  - postgis postgresql-jdbc        (optional) stateline border crossing
#
# Install Tomcat:
#  - cd /usr/local
#  - wget http://www.apache.org/dist/tomcat/tomcat-6/v6.0.33/bin/apache-tomcat-6.0.33.zip
#  - wget http://www.apache.org/dist/tomcat/tomcat-6/v6.0.33/bin/apache-tomcat-6.0.33.zip.md5
#  - md5sum -c apache-tomcat-6.0.33.zip.md5
#    (alt: openssl md5 apache-tomcat-6.0.33.zip)
#  - unzip apache-tomcat-6.0.33.zip
#  - chown -R opengts:opengts apache-tomcat-6.0.33
#  - ln -s apache-tomcat-6.0.33 tomcat
#
#  - cd /usr/local
#  - wget http://www.apache.org/dist/tomcat/tomcat-7/v7.0.26/bin/apache-tomcat-7.0.26.zip
#  - wget http://www.apache.org/dist/tomcat/tomcat-7/v7.0.26/bin/apache-tomcat-7.0.26.zip.md5
#  - md5sum -c apache-tomcat-7.0.23.zip.md5
#  - unzip apache-tomcat-7.0.23.zip
#  - chown -R opengts:opengts apache-tomcat-7.0.23
#  - chmod a+x apache-tomcat-7.0.23/bin/*.sh
#  - ln -s apache-tomcat-7.0.23 tomcat
#
# Using the Fedora/CentOS pre-installed version of Tomcat
#  - export CATALINA_HOME=/usr/share/tomcat6
#  - mkdir $CATALINA_HOME/common/lib
#  - cd $CATALINA_HOME/common/lib
#  - ln -s /usr/share/java/tomcat6-servlet-2.5-api-6.0.24.jar servlet-api.jar
#
# Install APache POI:
#  - cd /usr/local
#  - wget http://www.apache.org/dist/poi/release/bin/poi-bin-3.7-20101029.zip   [MD5:d353644608f9c1b9e38d9d2b722551c0]
#  - echo "d353644608f9c1b9e38d9d2b722551c0 *poi-bin-3.7-20101029.zip" > poi-bin-3.7-20101029.zip.md5
#  - md5sum -c poi-bin-3.7-20101029.zip.md5
#    alt: openssl md5 poi-bin-3.7-20101029.zip  (ie Mac OS X)
#  - unzip poi-bin-3.7-20101029.zip
#  - cp poi-3.7/poi-3.7-20101029.jar $JAVA_HOME/jre/lib/ext/.
#  - chmod a+r $JAVA_HOME/jre/lib/ext/poi-3.7-20101029.jar
#
# Optional packages:
#  - postgresql postgresql-server postgis postgresql-jdbc
#  - httpd php php-mysql
#  - xorg-x11-xauth
#
# Other packages/files installed in "/usr/local":
#  - GTS_x.x.x-Bxx/           (latest GTS Enterprise)
#  - apache-tomcat-6.0.32/    (downloaded directly from Apache)
#  - gts                      (symbolic link to latest GTS installation)
#  - java                     (symbolic link to Java installation)
#  - tomcat                   (symbolic link to latest Tomcat)
#  - gts_vars.env             (bash script initializing environment vars)
#
# Other procedures:
#  - MySQL JDBC connector copied to $JAVA_HOME/jre/lib/ext/.
#  - JavaMail copied to $JAVA_HOME/jre/lib/ext/.
#
# Pipe output:
#  ${COMMAND} 1>> ${LOG_FILE_OUT} 2>&1  # pipe stdout/stderr to output file
#
# ---------------------------------------------------------------------
# Handle
#ERROR: Command unavailable - /usr/sbin/selinuxenabled
#ERROR: Command unavailable - /usr/sbin/setenforce
#ERROR: Command unavailable - /usr/sbin/getenforce
#
# ---------------------------------------------------------------------
# This installation script performs the following:
#   1) Install GTS
#       a) Unzip to ${INSTALL_DIR}
#           Commands:
#               unzip /tmp/GTS_2.X.X_xxxx.zip
#       b) Change user:group ownership of $GTS_HOME to $GTS_USER
#           Commands: 
#               chown -R ${GTS_USER}:${GTS_USER} ${GTS_HOME}
#       c) Install 'opengts' autostart
#           Commands: 
#               cp ${GTS_HOME}/bin/onboot/fedora/opengts /etc/init.d/.
#               chmod 755 /etc/init.d/opengts
#               chkconfig --add opengts
#               chkconfig opengts on
#               chkconfig --list opengts
#   2) Install MySQL (skip if already installed)
#       a) Install via 'yum'
#           Commands: 
#               yum -y install mysql mysql-server
#   3a) Install 'wget' (skip if already installed)
#       a) Install via 'yum'
#           Commands: 
#               yum -y install wget 
#   3b) Install 'unzip' (skip if already installed)
#       a) Install via 'yum'
#           Commands: 
#               yum -y install unzip
#   3c) Install 'telnet' (skip if already installed)
#       a) Install via 'yum'
#           Commands: 
#               yum -y install telnet
#   3d) Install 'bind-utils' (skip if already installed)
#       a) Install via 'yum'
#           Commands: 
#               yum -y install bind-utils
#   3e) Install fonts (skip if already installed)
#       a) Install via 'yum'
#           Commands: 
#               yum -y install bitmap-fonts xorg-x11-fonts-Type1 xorg-x11-fonts-100dpi xorg-x11-fonts-75dpi
#   4) Install Java OpenJDK
#       a) Install via 'yum' (skip if already installed)
#           Commands:
#               yum -y install java-1.6.0-openjdk-devel
#       a) Creates symbolic link at "${INSTALL_DIR}/java"
#           Commands: 
#               cd ${INSTALL_DIR}
#               ln -s ${JAVA_HOME} java
#       b) Install 'mail.jar'
#           Commands: 
#               cp ${GTS_HOME}/jlib/javamail/mail.jar ${JAVA_HOME}/jre/lib/ext/.
#       c) Install MySQL JDBC driver ("mysql-connector-java-3.1.7-bin.jar")
#          [download from "http://dev.mysql.com/downloads/connector/j/3.1.html"]
#           Commands: 
#               cp /tmp/mysql-connector-java-3.1.7-bin.jar ${JAVA_HOME}/jre/lib/ext/.
#   5) Installs Apache Ant (skip if already installed)
#       a) Install via 'yum'
#           Commands: 
#               yum -y install ant.${ARCH}
#   6) Installs Apache Tomcat (via 'wget' Apache) (NOTE: NOT the 'yum' installed version!)
#       a) Install via 'wget'
#           Commands:
#               cd /tmp
#               wget http://www.apache.org/dist/tomcat/tomcat-6/v6.0.32/bin/apache-tomcat-6.0.32.zip
#               wget http://www.apache.org/dist/tomcat/tomcat-6/v6.0.32/bin/apache-tomcat-6.0.32.zip.md5
#               md5sum -c ./apache-tomcat-6.0.32.zip.md5
#               cd ${INSTALL_DIR}
#               unzip /tmp/apache-tomcat-6.0.32.zip
#               chmod a+x ${INSTALL_DIR}/apache-tomcat-6.0.32/bin/*.sh
#       b) Create symbolic link at "${INSTALL_DIR}/tomcat"
#           Commands:
#               cd ${INSTALL_DIR}
#               ln -s ${TOMCAT_DIR} tomcat
#       c) Deploy 'track.war'
#           Commands:
#               cp ${GTS_HOME}/build/track.war ${INSTALL_DIR}/${TOMCAT_DIR}/webapps/.
#       d) Set owner to $GTS_USER
#           Commands:
#               chown -R ${GTS_USER}:${GTS_USER} ${INSTALL_DIR}/${TOMCAT_DIR}
#       d) Install 'tomcat' autostart
#           Commands:
#               cp ${GTS_HOME}/bin/onboot/fedora/tomcat /etc/init.d/.
#               chmod 755 /etc/init.d/tomcat
#               chkconfig --add tomcat
#               chkconfig tomcat on
#               chkconfig --list tomcat
#   7) Create "${INSTALL_DIR}/gts_vars.env"
#       a) Create file "${INSTALL_DIR}/gts_vars.env"
#           File Contents: (Note difference between the quote types ', `, and ")
#               # --- Users
#               GTS_USER=${GTS_USER}
#               TOMCAT_USER=${GTS_USER}
#               # --- Directories
#               export GTS_HOME=`(cd -P /usr/local/gts; /bin/pwd)`
#               export JAVA_HOME=`(cd -P /usr/local/java; /bin/pwd)`
#               export CATALINA_HOME=`(cd -P /usr/local/tomcat; /bin/pwd)`
#               # --- Path
#               export PATH=.:${JAVA_HOME}/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin
#               # --- Aliases
#               alias setgts='export GTS_HOME=`(cd -P .;/bin/pwd)`; echo ${GTS_HOME}'
#               alias ls='ls -aF'
#               alias wh='find . -name'
#               alias psjava='${GTS_HOME}/bin/psjava'
#   8) Restart MySQL (skip if already configured)
#       a) Make sure MySQL is set for autostart
#           Commands:
#               chkconfig --add mysqld
#               chkconfig mysqld on
#       b) Restart MySQL
#           Commands:
#               service mysqld restart
#   9) Restart GTS services (if MySQL JDBC driver installed)
#       a) Restart Tomcat/OpenGTS
#           Commands:
#               service tomcat restart
#               service opengts restart
#  10) Initialize GTS installation (if MySQL JDBC driver installed)
#       a) Initializes GTS database
#           Commands:
#               cd ${GTS_HOME}
#               bin/initdb.sh [-rootUser=root -rootPass=xxxxxx]
#       b) Create 'sysadmin' GTS user
#           Commands:
#               cd ${GTS_HOME}
#               ./bin/admin.pl Account -account=sysadmin -pass=syspass -create
#       c) Load sample GTS data
#           Commands:
#               cd ${GTS_HOME}
#               ./sampleData/loadSampleData.sh
#  11) Install PostgreSQL/PostGIS (for future use)
#       a) Install via 'yum':
#           Commands:
#               yum -y install postgresql postgresql-server postgis postgresql-jdbc
#       b) Install PostgreSQL JDBC driver
#           Commands:
#               cp /usr/share/java/postgresql-jdbc-8.3.603.jar ${JAVA_HOME}/jre/lib/ext/.
#       c) Initializes postgresql db
#           Commands:
#               service postgresql initdb
#  12) Install httpd/php/php-mysql (for future use)
#       a) Install via 'yum':
#           Commands:
#               yum -y install httpd php php-mysql 
#  13) If '-sudoer' specified, adds $GTS_USER to '/etc/sudoers'
#       ...
#
# SELinux stuff (not yet implemented - because it doesn't work like I want it to yet)
#   1) Create file "opengts.te": (yes, ".te")
#       policy_module(opengts, 2.0)
#       require { attribute port_type; } 
#       type opengts_port_t, port_type; 
#   2) Make policy file:
#       make -f /usr/share/selinux/devel/Makefile opengts.pp 
#   3) Install policy file:
#       semodule -i opengts.pp
#       semanage port -a -t opengts_port_t -p tcp 37000-37999
#       semanage port -a -t opengts_port_t -p udp 37000-37999
#
# Notes for Ubuntu installation
#   - Use "apt-get" instead of "yum"
#        apt-get update
#        apt-get install unzip  wget  telnet  dnsutils
#        apt-get install apt-file
#        apt-get install mysql-client  mysql-server  libmysql-java
#        apt-get install sun-java6-jdk  openjdk-6-jdk  ant
#   - User "useradd opengts --shell /bin/bash"    (instead of "useradd -M opengts") <- without a home
#      or   "useradd -m opengts --shell /bin/bash" (instead of "useradd opengts")    <- with a home
#   - Add existing user to group:
#       usermod -a -G admin,opengts opengts
#   - Disable "tomcat6"
#       update-rc.d -f tomcat6 remove
#   - bin/initdb.sh -rootUser=debian-sys-maint -rootPass=<see /etc/mysql/debian.cnf>
#   - /usr/bin/tr
#   - arch?
#   - chkconfig?
#   - service?
#   - sudo?
#
# Notes for Debian installation
#   1) Use "apt-get" instead of "yum"
#   2) User "useradd opengts"    (instead of "useradd -M opengts") <- without a home
#      or   "useradd -m opengts" (instead of "useradd opengts")    <- with a home
#
# Setting MySQL root user password:
#   mysqladmin -u root password <password>
#
# Increasing the maximum number of allowed open files
#   1) "ulimit -n6000"   (as root)
#   2) Edit "/etc/security/limits.conf":
#        hard nofile 20000
#        soft nofile 20000
#      (reset session)
#   3) Check with "ulimit -aH"
#
# Using the Linux distro version of Tomcat, hints:
#   1) Create symbolic links:
#       mkdir /usr/share/tomcat6/common/lib
#       cd /usr/share/tomcat6/common/lib
#       ln -s /usr/share/java/tomcat6-servlet-2.5-api-6.0.24.jar servlet-api.jar
#
# -----------------------------------------------------------------------------
# Update process:

# 1) Stop the currently running device communication servers (DCS):
#     (run as "opengts"):
#     $  . /usr/local/gts_vars.env                 <-- set environment variables
#     $  cd $GTS_HOME
#     $  bin/startServers.sh -stop
#          or stop specific servers
#        bin/runserver.pl -s calamp -kill
#     $  bin/psjava
#          ...                                     <-- confirm no running DCS
# 2) Unzip the GTS_2.2.1-B28.zip file along side the previous installation:  IE.
#     (run as root):
#     #  cd /usr/local
#     #  unzip /tmp/GTS_2.2.1-B28.zip
#     #  chown -R opengts:opengts ./GTS_2.2.1-B28  <-- set owner user 
#     #  rm /usr/local/gts                         <-- remove old symbolic link
#     #  ln -s /usr/local/GTS_2.2.1-B28 gts        <-- create new symbolic link
# 3) Update database
#     (run as "opengts"):
#     $  . /usr/local/gts_vars.env                 <-- set environment variables
#     $  cd $GTS_HOME
#     $  bin/dbAdmin.pl -tables=ca
# 4) Install new 'track.war':
#     (run as "opengts"):
#     $  . /usr/local/gts_vars.env
#     $  cd $GTS_HOME
#     $  cp build/track.war $CATALINA_HOME/webapps/.
# 5) Restart device communication servers (DCS):
#     (run as "opengts"):
#     $  . /usr/local/gts_vars.env                 <-- set environment variables
#     $  cd $GTS_HOME
#     $  bin/startServers.sh -start
#          or start individual servers
#        bin/runserver.pl -s calamp -debug -start
#     $  bin/psjava                                <-- confirm running DCS
#
# -----------------------------------------------------------------------------
# The following environment vars may be predefined:
# - $GTS_HOME
# - $GTS_USER  (${CMD_useradd} [-M] $GTS_USER)  ["-M" => no home directory]
# -----------------------------------------------------------------------------
# Notes:
# - "[Errno 14] PYCURL ERROR 6"
#   May be due to a DNS issue (try "nslookup google.com" to confirm)
#   see also http://75.134.27.61:8101/wordpress/solvedfedora-repository-error-errno-14-pycurl-error-6/
# -----------------------------------------------------------------------------

# --- initial blank line
SEP="---------------------------------------------------------------------------"
echo ""
echo "${SEP}"

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# --- All Commands used in this script

# --- command existance check
COMMAND_ERROR=0
function chkCmd() { if [ ! -f "$1" ]; then echo "ERROR: Command unavailable - $1"; COMMAND_ERROR=1; fi }

# --- commands
CMD_cat="/bin/cat"                              ; chkCmd "${CMD_cat}"
CMD_tr="/usr/bin/tr"                            ; chkCmd "${CMD_tr}"
CMD_chmod="/bin/chmod"                          ; chkCmd "${CMD_chmod}"
CMD_chown="/bin/chown"                          ; chkCmd "${CMD_chown}"         # - root
CMD_rm="/bin/rm"                                ; chkCmd "${CMD_rm}"
CMD_cp="/bin/cp"                                ; chkCmd "${CMD_cp}"
CMD_mv="/bin/mv"                                ; chkCmd "${CMD_mv}"
CMD_free="/usr/bin/free"                        ; chkCmd "${CMD_free}"
CMD_grep="/bin/grep"                            ; chkCmd "${CMD_grep}"
CMD_groupadd="/usr/sbin/groupadd"               ; chkCmd "${CMD_groupadd}"      # - root
CMD_ln="/bin/ln"                                ; chkCmd "${CMD_ln}"
CMD_ls="/bin/ls"                                ; chkCmd "${CMD_ls}"
CMD_md5sum="/usr/bin/md5sum"                    ; chkCmd "${CMD_md5sum}"
CMD_mkdir="/bin/mkdir"                          ; chkCmd "${CMD_mkdir}"
CMD_sed="/bin/sed"                              ; chkCmd "${CMD_sed}"
CMD_sleep="/bin/sleep"                          ; chkCmd "${CMD_sleep}"
CMD_uname="/bin/uname"                          ; chkCmd "${CMD_uname}"
CMD_useradd="/usr/sbin/useradd"                 ; chkCmd "${CMD_useradd}"       # - root
CMD_usermod="/usr/sbin/usermod"                 ; chkCmd "${CMD_usermod}"       # - root
CMD_whoami="/usr/bin/whoami"                    ; chkCmd "${CMD_whoami}"
CMD_selinuxenabled="/usr/sbin/selinuxenabled"   ; chkCmd "${CMD_selinuxenabled}"
CMD_setenforce="/usr/sbin/setenforce"           ; chkCmd "${CMD_setenforce}"    # - root
CMD_getenforce="/usr/sbin/getenforce"           ; chkCmd "${CMD_getenforce}"

# --- installed in this script
CMD_createdb="/usr/bin/createdb"                ; #chkCmd "${CMD_createdb}"     <-- installed in this script
CMD_createlang="/usr/bin/createlang"            ; #chkCmd "${CMD_createlang}"   <-- installed in this script
CMD_psql="/usr/bin/psql"                        ; #chkCmd "${CMD_psql}"         <-- installed in this script
CMD_unzip="/usr/bin/unzip"                      ; #chkCmd "${CMD_unzip}"        <-- installed in this script
CMD_wget="/usr/bin/wget"                        ; #chkCmd "${CMD_wget}"         <-- installed in this script
CMD_telnet="/usr/bin/telnet"                    ; #chkCmd "${CMD_telnet}"       <-- installed in this script
CMD_iptables_save="/sbin/iptables-save"         ; #chkCmd "${CMD_iptables_save}"
CMD_iptables="/sbin/iptables"                   ; #chkCmd "${CMD_iptables}"

# --- Fedora only
CMD_arch="/bin/arch"                            ; #chkCmd "${CMD_arch}"
CMD_yum="/usr/bin/yum"                          ; #chkCmd "${CMD_yum}"          # - root
CMD_chkconfig="/sbin/chkconfig"                 ; #chkCmd "${CMD_chkconfig}"    # - root
CMD_service="/sbin/service"                     ; #chkCmd "${CMD_service}"      # - root

# --- built-in commands
CMD_read="read"                                 ; #                             <-- 'bash' built-in

# --- command errors?
if [ ${COMMAND_ERROR} -ne 0 ]; then
    echo ""
    echo "ERROR: Missing required commands (is this not Fedora Linux?)"
    echo ""
    echo "Control-C to exit, Enter to continue ..."
    ${CMD_read} -s -n 1  # silent, 1-char, no-timeout
    #exit 99
fi

# -----------------------------------------------------------------------------
# --- init constants

# --- pause between sections?
# - <0 = don't pause
# -  0 = pause until key is pressed
# - >0 = pause until key is pressed, or X seconds
PAUSE_HEADER=-1
PAUSE_FORANYKEY=0

# --- temp directory
TMP_DIR="/tmp"

# --- is 'root'?
IS_ROOT=0

# --- create user?
CREATE_USER=0

# --- MySQL
MYSQLD_NAME="mysqld" # - could be just "mysql" on some servers
MYSQL_STARTED=0
HAS_MYSQL_DRIVER=0

# --- MySQL root user/password
MYSQL_ROOT_USER=
MYSQL_ROOT_PASS=

# --- install extra (http, php, PostgreSQL, etc )
INSTALL_EXTRA=1

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# -- Keyboard wait

# ---- hit any key
function hitAnyKey()
{
    # arg $1 = timeout
    local timeout="$1"
    
    # --- consume pending chars
    #echo "Clearing ..."
    ${CMD_read} -s -d '~' -n 99999999 -t 1  # silent, N-char, 1-sec timeout
    
    # --- read char
    echo -n "."
    if [ ${timeout} -gt 0 ]; then
        ${CMD_read} -s -n 1 -t ${timeout}  # silent, 1-char, timeout
    else
        ${CMD_read} -s -n 1  # silent, 1-char, no-timeout
    fi
    
}

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# -- Logging output

# ---- print header
SECTION_COUNT=0
function printHeader()
{
    # arg $1 = "message"
    local msg="$1"
    
    # --- count section
    SECTION_COUNT=`expr $SECTION_COUNT + 1`

    # --- header
    echo ""
    echo ""
    echo "${SEP}"
    echo "---- ${msg}"

    # -- pause
    if   [ ${PAUSE_HEADER} -lt 0 ]; then
        ${CMD_sleep} 1
    elif [ ${PAUSE_HEADER} -eq 0 ]; then
        echo ">>>>>>>> [${SECTION_COUNT}] Press any key to continue ... "
        #${CMD_read} -s -n 1  # silent, 1-char, no-timeout
        hitAnyKey 0
        echo "..."
    else
        echo ">>>>>>>> [${SECTION_COUNT}] Press any key to continue (timeout in ${PAUSE_HEADER} seconds) ..."
        #${CMD_read} -s -n 1 -t ${PAUSE_HEADER}  # silent, 1-char, timeout
        hitAnyKey ${PAUSE_HEADER}
        echo "..."
    fi
    
}

# ---- print message
function printMessage()
{
    # arg $1 = "message"
    local msg="$1"
    echo ""
    echo "---- ${msg}"
}

# ---- print error
function printError()
{
    # arg $1 = "message"
    local errMsg="$1"
    echo ""
    echo "==== ${errMsg}"
    echo ""
}

# ---- pause for any-key
function waitForAnyKey()
{
    # arg $1 = "message"
    local msg="$1"
    if [ "${msg}" != "" ]; then
        echo "$msg"
    fi
    
    # -- pause
    if   [ ${PAUSE_FORANYKEY} -lt 0 ]; then
        ${CMD_sleep} 1
    elif [ ${PAUSE_FORANYKEY} -eq 0 ]; then
        echo ""
        echo ">>>>>>>> Press any key to continue (Control-C to exit) ..."
        #${CMD_read} -s -n 1  # silent, 1-char, no-timeout
        hitAnyKey 0
        echo "..."
    else
        echo ""
        echo ">>>>>>>> Press any key to continue (timeout in ${PAUSE_FORANYKEY} seconds) ..."
        #${CMD_read} -s -n 1 -t ${PAUSE_FORANYKEY}  # silent, 1-char, timeout
        hitAnyKey ${PAUSE_FORANYKEY}
        echo "..."
    fi
}

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# --- Platform checks

# --- system architecture
if [ -x "${CMD_arch}" ]; then
    SYS_ARCH="`${CMD_arch}`"
else
    SYS_ARCH="`${CMD_uname} -m`"
fi
KERNEL_RELEASE="`${CMD_uname} -r`"
echo "System Arch : ${SYS_ARCH}"
echo "Kernel Rel  : ${KERNEL_RELEASE}"

# --- memory
TOTAL_MEM=`${CMD_free} -m | ${CMD_grep} Mem: | ${CMD_sed} 's/^.*:[ ]*\([0-9]*\) .*$/\1/'`
echo "Total Memory: ${TOTAL_MEM} Mb (should be at least 512Mb)"

# --- Fedora version 10/11/12/12/14/15 (may also check "/etc/issue")
# --- CentOS 6.2
# - Should also check for "CentOS 5.5", "CentOS 6.x"
FEDORA_VERSION=0
CENTOS_VERSION=0
IGNORE_FEDORA_VERSION_ERROR=0
if [ -f "/etc/redhat-release" ]; then
    RH_REL=`${CMD_cat} /etc/redhat-release`
    #echo "RedHat Release: $RH_REL"
    echo "${RH_REL}" | ${CMD_grep} -q "Fedora release"
    if [ $? -eq 0 ]; then
        FEDORA_VERSION=`echo ${RH_REL} | ${CMD_sed} 's|Fedora release ||;s| .*$||'`
        #echo "RedHat Fedora Version: ${FEDORA_VERSION}"
    fi
fi
if [ ${FEDORA_VERSION} -eq 0 ]; then
    echo "${KERNEL_RELEASE}" | grep -q '\.fc10\.'
    if [ $? -eq 0 ]; then
        FEDORA_VERSION=10
    else
        echo "${KERNEL_RELEASE}" | grep -q '\.fc11\.'
        if [ $? -eq 0 ]; then
            FEDORA_VERSION=11
        else
            echo "${KERNEL_RELEASE}" | grep -q '\.fc12\.'
            if [ $? -eq 0 ]; then
                FEDORA_VERSION=12
            else
                echo "${KERNEL_RELEASE}" | grep -q '\.fc13\.'
                if [ $? -eq 0 ]; then
                    FEDORA_VERSION=13
                else
                    echo "${KERNEL_RELEASE}" | grep -q '\.fc14\.'
                    if [ $? -eq 0 ]; then
                        FEDORA_VERSION=14
                    else
                        echo "${KERNEL_RELEASE}" | grep -q '\.fc15\.'
                        if [ $? -eq 0 ]; then
                            FEDORA_VERSION=15
                        else
                            echo "Not Fedora ..."
                        fi
                    fi
                fi
            fi
        fi
    fi
fi
echo "Fedora Vers : ${FEDORA_VERSION}"
if [ -f "/etc/redhat-release" ]; then
    RH_REL=`${CMD_cat} /etc/redhat-release`
    echo "RedHat Rel  : $RH_REL"
fi
echo "${SEP}"
if [ "${FEDORA_VERSION}" -ne 11 ] && [ "${FEDORA_VERSION}" -ne 12 ] && [ "${FEDORA_VERSION}" -ne 13 ] && [ "${FEDORA_VERSION}" -ne 14 ] && [ "${FEDORA_VERSION}" -ne 15 ]; then
    if [ ${IGNORE_FEDORA_VERSION_ERROR} -eq 1 ]; then
        echo "*** WARNING: Not a Linux Fedora 11/12/13/14/15 distribution! ***"
        waitForAnyKey "Continuing ..."
    else
        echo "Should be Linux Fedora 11/12/13/14 to continue ..."
        waitForAnyKey "Control-C to exit, Enter to continue ..."
        #exit 99
    fi
else
    waitForAnyKey ""
fi
echo ""
${CMD_sleep} 1

# --- JDK version (i386, x86_64, i686)
JDK_ARCH="${SYS_ARCH}"
if  [ "${JDK_ARCH}" = "x86_64" ]; then
    ANT_PACKAGE="ant.x86_64"
    JDK_PACKAGE="java-1.6.0-openjdk-devel.x86_64"
    JDK_DIR="java-1.6.0-openjdk-1.6.0.0.x86_64"
elif [ "${JDK_ARCH}" = "i386" ]; then
    ANT_PACKAGE="ant"
    JDK_PACKAGE="java-1.6.0-openjdk-devel" # .i386"
    JDK_DIR="java-1.6.0-openjdk-1.6.0.0"
else
    ANT_PACKAGE="ant"
    JDK_PACKAGE="java-1.6.0-openjdk-devel"
    JDK_DIR="java-1.6.0-openjdk-1.6.0.0"
fi

# --- Ant
ANT_DIR="apache-ant-1.7.1"

# --- Tomcat
# - http://www.apache.org/dist/tomcat/tomcat-6/v6.0.33/bin/apache-tomcat-6.0.33.zip
# - http://www.apache.org/dist/tomcat/tomcat-6/v6.0.33/bin/apache-tomcat-6.0.33.zip.md5
TOMCAT_DIR_6_0_33="apache-tomcat-6.0.33"
TOMCAT_DOWNLOAD_6_0_33="http://www.apache.org/dist/tomcat/tomcat-6/v6.0.33/bin/apache-tomcat-6.0.33.zip"
# --
# - http://www.apache.org/dist/tomcat/tomcat-7/v7.0.25/bin/apache-tomcat-7.0.25.zip
# - http://www.apache.org/dist/tomcat/tomcat-7/v7.0.25/bin/apache-tomcat-7.0.25.zip.md5
TOMCAT_DIR_7_0_25="apache-tomcat-7.0.25"
TOMCAT_DOWNLOAD_7_0_25="http://www.apache.org/dist/tomcat/tomcat-7/v7.0.25/bin/apache-tomcat-7.0.25.zip"
# --
# - http://www.apache.org/dist/tomcat/tomcat-7/v7.0.26/bin/apache-tomcat-7.0.26.zip
# - http://www.apache.org/dist/tomcat/tomcat-7/v7.0.26/bin/apache-tomcat-7.0.26.zip.md5
TOMCAT_DIR_7_0_26="apache-tomcat-7.0.26"
TOMCAT_DOWNLOAD_7_0_26="http://www.apache.org/dist/tomcat/tomcat-7/v7.0.26/bin/apache-tomcat-7.0.26.zip"
# - 
TOMCAT_DIR="${TOMCAT_DIR_7_0_26}"
TOMCAT_DOWNLOAD="${TOMCAT_DOWNLOAD_7_0_26}"

# --- Apache POI
# - http://www.apache.org/dist//poi/release/bin/poi-bin-3.7-20101029.zip
POI_DIR_3_7="poi-3.7"
POI_ZIP_3_7="poi-bin-3.7-20101029.zip"
POI_MD5_3_7="d353644608f9c1b9e38d9d2b722551c0"
POI_DOWNLOAD_3_7="http://www.apache.org/dist//poi/release/bin/poi-bin-3.7-20101029.zip"
POI_XLS_JAR_3_7="poi-3.7-20101029.jar"
# - http://www.apache.org/dyn/closer.cgi/poi/release/bin/poi-bin-3.8-20120326.zip
POI_DIR_3_8="poi-3.8"
POI_ZIP_3_8="poi-bin-3.8-20120326.zip"
POI_MD5_3_8="4f0e518baff9af70c792d86d4f21804b"
POI_DOWNLOAD_3_8="http://www.us.apache.org/dist/poi/release/bin/poi-bin-3.8-20120326.zip"
POI_XLS_JAR_3_8="poi-3.8-20120326.jar"
# - 
POI_DIR="${POI_DIR_3_8}"
POI_ZIP="${POI_ZIP_3_8}"
POI_MD5="${POI_MD5_3_8}"
POI_DOWNLOAD="${POI_DOWNLOAD_3_8}"
POI_XLS_JAR="${POI_XLS_JAR_3_8}"

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# --- modular install functions below

# --- install general tools
function install_Tools() 
{
    
    # --- install unzip
    if [ ! -x "${CMD_unzip}" ]; then
        printHeader "Installing 'unzip' ..."
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_yum} -y install unzip
            if [ ! -x "${CMD_unzip}" ]; then
                printError "ERROR: Command '${CMD_unzip}' not installed"
                exit 1
            else
                waitForAnyKey "... installed"
            fi
        else
            # - required
            printError "ERROR: not running as 'root', unable to install."
            exit 1
        fi
    fi
    
    # --- install wget
    if [ ! -x "${CMD_wget}" ]; then
        printHeader "Installing 'wget' ..."
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_yum} -y install wget
            if [ ! -x "${CMD_wget}" ]; then
                printError "ERROR: Command '${CMD_wget}' not installed"
                exit 1
            else
                waitForAnyKey "... installed"
            fi
        else
            # - required
            printError "ERROR: not running as 'root', unable to install."
            exit 1
        fi
    fi
    
    # --- install telnet
    if [ ! -x "${CMD_telnet}" ]; then
        printHeader "Installing 'telnet' ..."
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_yum} -y install telnet
            if [ ! -x "${CMD_telnet}" ]; then
                echo "WARN: Unable to install '${CMD_telnet}', continuing ..."
            else
                waitForAnyKey "... installed"
            fi
        else
            # - not required
            waitForAnyKey "... not running as 'root', skipping install"
        fi
    fi

    # --- install fonts
    if [ ! -d "/usr/share/fonts/bitmap-fonts" ]; then #  && [ ! -d "/usr/share/fonts/bitmap" ]; then
        printHeader "Installing fonts: bitmap-fonts ..."
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_yum} -y install bitmap-fonts # xorg-x11-fonts-Type1 xorg-x11-fonts-100dpi xorg-x11-fonts-75dpi
            if [ ! -d "/usr/share/fonts/bitmap-fonts" ] && [ ! -d "/usr/share/fonts/bitmap" ]; then
                echo "WARN: Unable to install 'bitmap-fonts', continuing ..."
                waitForAnyKey "... not installed"
            else
                waitForAnyKey "... installed"
            fi
        else
            waitForAnyKey "... not running as 'root', skipping install"
        fi
    fi

    # --- install bind-utils (optional)
    if [ ! -x "/usr/bin/nslookup" ]; then
        printHeader "Installing 'bind-utils' ('nslookup', ...) ..."
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_yum} -y install bind-utils
            if [ ! -x "/usr/bin/nslookup" ]; then
                echo "WARN: Unable to install '/usr/bin/nslookup', continuing ..."
            else
                waitForAnyKey "... installed"
            fi
        else
            # - not required
            waitForAnyKey "... not running as 'root', skipping install"
        fi
    fi

}

# -----------------------------------------------------------------------------

# --- install Tomcat
function install_Tomcat() 
{
    printHeader "Installing Tomcat ..."
    if [ -d "${INSTALL_DIR}/tomcat" ]; then # {
        echo "Apache Tomcat already installed (found ${INSTALL_DIR}/tomcat)"
        echo "(remove '${INSTALL_DIR}/tomcat' to re-initialize Tomcat installation)"
        waitForAnyKey ""
    else # } {
        if [ -d "${INSTALL_DIR}/${TOMCAT_DIR}" ]; then # {
            echo "Apache Tomcat already unzipped (found ${INSTALL_DIR}/${TOMCAT_DIR})"
        else # } {
            # - download
            if [ -f "${TMP_DIR}/${TOMCAT_DIR}.zip" ]; then
                echo "WARNING: Using existing Tomcat zip: ${TMP_DIR}/${TOMCAT_DIR}.zip"
            else
                ( cd ${TMP_DIR}; ${CMD_wget} ${TOMCAT_DOWNLOAD}; )
                ( cd ${TMP_DIR}; ${CMD_wget} ${TOMCAT_DOWNLOAD}.md5; )
            fi
            # - check MD5 checksum
            if [ -f "${TMP_DIR}/${TOMCAT_DIR}.zip.md5" ]; then # {
                # - Apache Tomcat MD5 file is broken, join multiple lines
                # ${CMD_cat} "${TMP_DIR}/${TOMCAT_DIR}.zip.md5" | ${CMD_tr} -d '\n' > "${TMP_DIR}/${TOMCAT_DIR}.zip.md5"
                echo "`${CMD_cat} ${TMP_DIR}/${TOMCAT_DIR}.zip.md5 | ${CMD_tr} -d '\n' | ${CMD_sed} 's/[ \t].*$//'` *${TOMCAT_DIR}.zip" > "${TMP_DIR}/${TOMCAT_DIR}.zip.md5"
                # - MD5 checksum (file contains *file specification)
                ( cd ${TMP_DIR}; ${CMD_md5sum} -c ./${TOMCAT_DIR}.zip.md5; )
                if [ $? -ne 0 ]; then
                    printError "ERROR: MD5 checksum failed for ${TOMCAT_DIR}.zip"
                    exit 1
                fi
            else # } {
                printError "ERROR: MD5 checksum file missing: ${TMP_DIR}/${TOMCAT_DIR}.zip.md5"
                echo "(Download MD5 checksum file, or delete ${TMP_DIR}/${TOMCAT_DIR}.zip to reload"
                exit 1
            fi # }
            # - unzip
            ( cd ${INSTALL_DIR}; ${CMD_unzip} ${TMP_DIR}/${TOMCAT_DIR}.zip; )
            if [ ! -d "${INSTALL_DIR}/${TOMCAT_DIR}" ]; then
                printError "ERROR: Unable to install Apache Tomcat";
                exit 1
            fi
        fi # }
        # - set Tomcat '.sh' files executable
        ${CMD_chmod} a+rx ${INSTALL_DIR}/${TOMCAT_DIR}/bin/*.sh
        # - create "/usr/local/tomcat" symbolic link
        ( cd ${INSTALL_DIR}; ${CMD_ln} -s ${TOMCAT_DIR} tomcat; )
        # - enable auto-start
        if [ -f "/etc/init.d/tomcat" ]; then # {
            echo "Tomcat autostart already exists: /etc/init.d/tomcat"
            echo "(remove '/etc/init.d/tomcat' to re-install)"
        else # } {
            if [ ! -f "${GTS_HOME}/bin/onboot/fedora/tomcat" ]; then
                echo "WARNING: Not installing Tomcat autostart (not found)"
            else
                echo "Setting Tomcat autostart '/etc/init.d/tomcat'"
                if [ ${IS_ROOT} -eq 1 ]; then
                    ${CMD_cp} ${GTS_HOME}/bin/onboot/fedora/tomcat /etc/init.d/.
                    ${CMD_chmod} 755 /etc/init.d/tomcat
                    ${CMD_chkconfig} --add tomcat
                    ${CMD_chkconfig} tomcat on
                    ${CMD_chkconfig} --list tomcat
                    #/etc/init.d/tomcat restart
                    waitForAnyKey "... enabled"
                else
                    # - required, but not fatal
                    waitForAnyKey "... not running as 'root', skipping 'tomcat' autostart"
                fi
           fi
        fi # }
        # - rename example/demo Tomcat files/directories
        echo "Moving aside Tomcat demo/examples/docs ..."
        if [ -d "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/jsp-examples" ]; then
            ${CMD_mv} "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/jsp-examples" "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/jsp-examples.x"
        fi
        if [ -d "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/servlets-examples" ]; then
            ${CMD_mv} "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/servlets-examples" "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/servlets-examples.x"
        fi
        if [ -d "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/tomcat-docs" ]; then
            ${CMD_mv} "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/tomcat-docs" "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/tomcat-docs.x"
        fi
        if [ -f "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/ROOT/index.html" ]; then
            ${CMD_rm} -f "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/ROOT/index.html.x"
            ${CMD_mv} "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/ROOT/index.html" "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/ROOT/index.html.x"
        fi
        echo "<html><meta HTTP-EQUIV='refresh' CONTENT='1; URL=./track/Track'></meta></html>" > "${INSTALL_DIR}/${TOMCAT_DIR}/webapps/ROOT/index.html"
        # - create new "tomcat-users.xml.new" (not yet installed) [http://localhost:8080/manager/html]
        TOMCAT_USERS_XML="${INSTALL_DIR}/${TOMCAT_DIR}/conf/tomcat-users.xml.new"
        echo "Creating '$TOMCAT_USERS_XML' ..."
        ${CMD_rm} -f "$TOMCAT_USERS_XML"
        TOMCAT_PASSW="SecretStuff"
        echo "<?xml version='1.0' encoding='utf-8'?>"                                               > "$TOMCAT_USERS_XML"
        echo "<tomcat-users>"                                                                       > "$TOMCAT_USERS_XML"
        echo "  <role rolename=\"manager\"/>"                                                       > "$TOMCAT_USERS_XML"
        echo "  <role rolename=\"tomcat\"/>"                                                        > "$TOMCAT_USERS_XML"
        echo "  <role rolename=\"role1\"/>"                                                         > "$TOMCAT_USERS_XML"
        echo "  <user username=\"tomcat\" password=\"${TOMCAT_PASSW}\" roles=\"manager,tomcat\"/>"  > "$TOMCAT_USERS_XML"
        echo "  <!-- <user username=\"both\" password=\"tomcat\" roles=\"tomcat,role1\"/> -->"      > "$TOMCAT_USERS_XML"
        echo "  <!-- <user username=\"role1\" password=\"tomcat\" roles=\"role1\"/> -->"            > "$TOMCAT_USERS_XML"
        echo "</tomcat-users>"                                                                      > "$TOMCAT_USERS_XML"
        # - change Tomcat owner (again)
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_chown} -R ${GTS_USER}:${GTS_USER} "${INSTALL_DIR}/${TOMCAT_DIR}"
        fi
        # - wait here
        waitForAnyKey "... installed"
    fi # }
    # http://localhost:8080/manager/html
}

# -----------------------------------------------------------------------------

# --- install Apache POI
function install_ApachePOI() 
{
    printHeader "Installing Apache POI ..."
    if [ -d "${INSTALL_DIR}/poi" ]; then # {
        echo "Apache POI already installed (found ${INSTALL_DIR}/poi)"
        echo "(remove '${INSTALL_DIR}/poi' to re-initialize Apache POI installation)"
        waitForAnyKey ""
    else # } {
        if [ -d "${INSTALL_DIR}/${POI_DIR}" ]; then
            echo "Apache POI already unzipped (found ${INSTALL_DIR}/${POI_DIR})"
        else
            # - download
            if [ -f "${TMP_DIR}/${POI_ZIP}" ]; then
                echo "WARNING: Using existing POI zip: ${TMP_DIR}/${POI_ZIP}"
            else
                ( cd ${TMP_DIR}; ${CMD_wget} ${POI_DOWNLOAD}; )
                ( cd ${TMP_DIR}; echo "${POI_MD5} *${POI_ZIP}" > ${POI_ZIP}.md5; )
            fi
            # - check MD5 checksum
            if [ -f "${TMP_DIR}/${POI_ZIP}.md5" ]; then
                # - Apache POI MD5 file is broken, join multiple lines
                # ${CMD_cat} "${TMP_DIR}/${POI_ZIP}.md5" | ${CMD_tr} -d '\n' > "${TMP_DIR}/${POI_ZIP}.md5"
                echo "`${CMD_cat} ${TMP_DIR}/${POI_ZIP}.md5 | ${CMD_tr} -d '\n' | ${CMD_sed} 's/[ \t].*$//'` *${POI_ZIP}" > "${TMP_DIR}/${POI_ZIP}.md5"
                # - MD5 checksum (file contains *file specification)
                ( cd ${TMP_DIR}; ${CMD_md5sum} -c ./${POI_ZIP}.md5; )
                if [ $? -ne 0 ]; then
                    printError "ERROR: MD5 checksum failed for ${POI_ZIP}"
                    exit 1
                fi
            else
                printError "ERROR: MD5 checksum file missing: ${TMP_DIR}/${POI_ZIP}.md5"
                echo "(Download MD5 checksum file, or delete ${TMP_DIR}/${POI_ZIP} to reload"
                exit 1
            fi
            # - unzip
            ( cd ${INSTALL_DIR}; ${CMD_unzip} ${TMP_DIR}/${POI_ZIP}; )
            if [ ! -d "${INSTALL_DIR}/${POI_DIR}" ]; then
                printError "ERROR: Unable to install Apache POI dir : ${INSTALL_DIR}/${POI_DIR}";
                exit 1
            fi
        fi
        # - create "/usr/local/tomcat" symbolic link
        ( cd ${INSTALL_DIR}; ${CMD_ln} -s ${POI_DIR} poi; )
        # - wait here
        waitForAnyKey "... Apache POI installed at ${INSTALL_DIR}/${POI_DIR}"
    fi # }
}

# -----------------------------------------------------------------------------

# --- initialize GTS database
function init_GTSDatabase()
{

    # --- init GTS database
    # - Ubuntu/Debian:
    # -  bin/initdb.sh -rootUser=debian-sys-maint -rootPass=<see /etc/mysql/debian.cnf>
    printHeader "Initializing GTS database ..."
    if [ ${MYSQL_STARTED} -eq 1 ] && [ ${HAS_MYSQL_DRIVER} -eq 1 ]; then
        if [ "${MYSQL_ROOT_USER}" != "" ]; then
            ( cd ${GTS_HOME}; ./bin/initdb.sh -user=${MYSQL_ROOT_USER} -pass=${MYSQL_ROOT_PASS}; )
        else
            ( cd ${GTS_HOME}; ./bin/initdb.sh; )
        fi
        waitForAnyKey "... initialized"
    else
        echo "WARNING: No MySQL driver."
        echo "WARNING: Not initializing OpenGTS database."
        waitForAnyKey ""
    fi
    
    # --- create 'sysadmin'
    printHeader "Creating 'sysadmin' account [pass='syspass'] ..."
    if [ ${MYSQL_STARTED} -eq 1 ] && [ ${HAS_MYSQL_DRIVER} -eq 1 ]; then
        ( cd ${GTS_HOME}; ./bin/admin.pl Account -account=sysadmin -pass=syspass -create; )
        waitForAnyKey "... created"
    else
        echo "WARNING: No MySQL driver, or MySQL not running."
        echo "WARNING: Not creating 'sysadmin' account."
        waitForAnyKey ""
    fi
    
    # --- load sample data
    printHeader "Loading sample 'demo' data ..."
    if [ ${MYSQL_STARTED} -eq 1 ] && [ ${HAS_MYSQL_DRIVER} -eq 1 ]; then
        ( cd ${GTS_HOME}; ./sampleData/loadSampleData.sh; )
        waitForAnyKey "... sample data loaded"
    else
        echo "WARNING: No MySQL driver, or MySQL not running."
        echo "WARNING: Not loading sample 'demo' data."
        waitForAnyKey ""
    fi

}

# -----------------------------------------------------------------------------

# --- install PostgreSQL
function install_PostgreSQL() 
{

    # --- install postgresql/postgis
    printHeader "Installing PostgreSQL"
    if [ ${IS_ROOT} -eq 1 ]; then
    
        # Ubuntu: 
        #   apt-get install postgresql postgresql-client postgresql-contrib pgadmin3
        #   apt-get install postgresql-8.3-postgis
        
        # CentOS:
        #   "PostGIS" not available.
        
        ${CMD_yum} -y install postgresql postgresql-server postgresql-contrib postgresql-jdbc
        ${CMD_yum} -y install postgis
        ${CMD_ls} ${JAVA_EXT_DIR}/postgresql-*.jar >/dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo "The Java installation appears to already contain a PostgreSQL JDBC driver"
        else
            if   [ -f "/usr/share/java/postgresql-jdbc-8.4.701.jar" ]; then
                ${CMD_cp} /usr/share/java/postgresql-jdbc-8.4.701.jar ${JAVA_EXT_DIR}/.
                echo "Installed 'postgresql-jdbc-8.4.701.jar' ..."
            elif [ -f "/usr/share/java/postgresql-jdbc-8.3.604.jar" ]; then
                ${CMD_cp} /usr/share/java/postgresql-jdbc-8.3.604.jar ${JAVA_EXT_DIR}/.
                echo "Installed 'postgresql-jdbc-8.3.604.jar' ..."
            elif [ -f "/usr/share/java/postgresql-jdbc-8.3.603.jar" ]; then
                ${CMD_cp} /usr/share/java/postgresql-jdbc-8.3.603.jar ${JAVA_EXT_DIR}/.
                echo "Installed 'postgresql-jdbc-8.3.603.jar' ..."
            elif [ -f "/usr/share/java/postgresql-jdbc-8.1.407.jar" ]; then
                ${CMD_cp} /usr/share/java/postgresql-jdbc-8.1.407.jar ${JAVA_EXT_DIR}/.
                echo "Installed 'postgresql-jdbc-8.1.407.jar' ..."
            else
                echo "ERROR: Unable to locate specific '/usr/share/java/postgresql-jdbc-8.X.XXX.jar'"
                echo "Options:"
                ${CMD_ls} -l /usr/share/java/postgresql-jdbc-*.jar
            fi
        fi
        echo "(Ignore any 'Data directory is not empty!' error)"
        ${CMD_service} postgresql initdb    # - will fail if already initialized
        
        # -
        # TODO: other config still needs to be done to PostgreSQL:
        #   1) Edit "/var/lib/pgsql/data/postgresql.conf".  Uncomment following:
        #         #listen_addresses = 'localhost'        # ...
        #   2) Edit "/var/lib/pgsql/data/pg_hba.conf".      Set the following
        #         ...
        #         # IPv4 local connections:
        #   from: host    all     all     127.0.0.1/32     ident [sameuser]
        #     to: host    all     all     127.0.0.1/32     trust
        #         ...
        # -
        #${CMD_chkconfig} postgresql on
        #${CMD_service} postgresql restart
        
        # -
        # TODO: Init 'bcrossing_db' database: (as user "postgres")
        #   1) Create daabase
        #         $ createdb bcrossing_db
        #   2) Configure PostGIS
        #         $ cd /usr/share/pgsql/contrib
        #         $ createlang plpgsql bcrossing_db
        #         $ psql -d bcrossing_db -f [lw]postgis[-64].sql    # 32/64-bit arch
        #         $ psql -d bcrossing_db -f spatial_ref_sys.sql
        #   3) Create role
        #         $ psql
        #             > CREATE ROLE bcrossing;
        #             > CREATE ROLE bcross IN GROUP bcrossing;
        #           or
        #         $ psql < bcrossRoles.psql
        #   4) Import data
        #         $ psql bcrossing_db < stateShape.sql
        #BCROSS_DB="bcrossing_db"
        #BCROSS_DIR="${GTS_HOME}/bcross"
        #if [ -f "${BCROSS_DIR}/stateShape.sql.zip" ] && [ -x "${CMD_psql}" ]; then
        #    if [ ! -f "${BCROSS_DIR}/stateShape.sql" ]; then
        #        echo "Unzipping ${BCROSS_DIR}/stateShape.sql.zip ..."
        #        ( cd ${BCROSS_DIR}; ${CMD_unzip} stateShape.sql.zip; )
        #    fi
        #    if [ -f "${BCROSS_DIR}/stateShape.sql" ]; then
        #        su - postgres 'createPostgreSQLDB "${BCROSS_DB}"'
        #        if [ -f "${BCROSS_DIR}/bcrossRoles.psql" ]; then
        #            ${CMD_psql} < "${BCROSS_DIR}/bcrossRoles.psql"
        #        fi
        #        ${CMD_psql} ${BCROSS_DB} < "${BCROSS_DIR}/bcrossRoles.psql"
        #    fi
        #fi
        # -
        # TODO: Init 'navteq_db' database: (as user "postgres")
        #   1) Create daabase
        #         $ createdb navteq_db
        #   2) Configure PostGIS
        #         $ cd /usr/share/pgsql/contrib
        #         $ createlang plpgsql navteq_db
        #         $ psql -d navteq_db -f [lw]postgis[-64].sql    # 32/64-bit arch
        #         $ psql -d navteq_db -f spatial_ref_sys.sql
        # -
        # TODO: Init 'gisgraphy' database: (as user "postgres")
        #   1) Create daabase
        #         $ psql -c "CREATE DATABASE gisgraphy WITH TEMPLATE = template0 ENCODING = 'UTF8';"
        #            ==> "CREATE DATABASE"
        #   2) Configure PostGIS
        #         $ cd /usr/share/pgsql/contrib
        #         $ createlang plpgsql gisgraphy 
        #         $ psql -d gisgraphy -f [lw]postgis[-64].sql    # 32/64-bit arch
        #         $ psql -d gisgraphy -f spatial_ref_sys.sql
        #   3) Configure GISGraphy: (as user "postgres")
        #         $ cd /usr/local/gisgraphy-2.0-beta
        #         $ psql -d gisgraphy -f sql/create_tables.sql
        #         $ psql -d gisgraphy -f sql/createGISTIndex.sql
        #         $ psql -d gisgraphy -f sql/insert_users.sql       # TODO: change admin/user passwords later
        #   4) Change port 8080 binding to 8081
        #         $ cd /usr/local/gisgraphy-2.0-beta
        #         $ vi webapps/ROOT/WEB-INF/classes/env.properties        ["fulltextSearchUrl="]
        #         $ vi etc/jetty.xml                                      ["<Set name="port">"]
        #   5) Launch GISGraphy (as user 'postgres')
        #         $ cd /usr/local/gisgraphy-2.0-beta
        #         $ chmod a+x launch.sh
        #         $ ./launch.sh
        #   6) Import data [http://localhost:8081/admin/mainMenu.html]
        #         (expect 25+ hours)
        #   7) Optimize  (as user 'postgres')
        #         $ cd /usr/local/gisgraphy-2.0-beta
        #         $ psql -d gisgraphy -f sql/createGISTIndex.sql
        #         (will take a while)
        # -
        #${CMD_chkconfig} postgresql on
        #${CMD_service} postgresql restart
        waitForAnyKey "... installed"
        
    else
    
        # - not required
        waitForAnyKey "... not running as 'root', skipping install"
        
    fi

}

# -----------------------------------------------------------------------------

# --- install extra modules
function install_ExtraModules() 
{
    
    # --- install httpd
    if [ ! -x "/usr/sbin/httpd" ]; then
        printHeader "Installing httpd (for future use)"
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_yum} -y install httpd php php-mysql 
            #${CMD_chkconfig} --add httpd
            #${CMD_chkconfig} httpd on
            #${CMD_chkconfig} --list httpd
            waitForAnyKey "... installed"
        else
            # - not required
            waitForAnyKey "... not running as 'root', skipping install"
        fi
    fi
    
    # --- install php, etc
    if [ ! -x "/usr/bin/php" ]; then
        printHeader "Installing php, etc. (for future use)"
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_yum} -y install php php-mysql 
            waitForAnyKey "... installed"
        else
            # - not required
            waitForAnyKey "... not running as 'root', skipping install"
        fi
    fi
    
    # --- install xorg-x11-xauth
    if [ ! -x "/usr/bin/xauth" ]; then
        printHeader "Installing xorg-x11-xauth (for X11 authentication)"
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_yum} -y install xorg-x11-xauth
            waitForAnyKey "... installed"
        else
            # - not required
            waitForAnyKey "... not running as 'root', skipping install"
        fi
    fi

}

# -----------------------------------------------------------------------------

# ---- add user as 'sudoer'
function add_sudoer()
{
    # arg $1 = userid
    # arg $2 = "1"=user groupid, "2"='sudoer'
    local USER="$1"
    local MODE="$2"
    local GROUP="sudoer"
    
    # --- only if 'root'?
    if [ ${IS_ROOT} -eq 1 ]; then
    
        # --- create sudoer
        if [ ${MODE} -eq 1 ]; then
            printHeader "Setting '${USER}' as sudoer (user) ..."
            ${CMD_grep} -q "${USER}" /etc/group
            if [ $? -eq 1 ]; then
                echo "ERROR: Group '${USER}' does not exist."
            else
                ${CMD_grep} -q "%${USER}" /etc/sudoers
                if [ $? -eq 1 ]; then
                    echo "Adding group '${USER}' to /etc/sudoers ..."
                    echo "%${USER} ALL=(ALL) ALL" >> /etc/sudoers
                else
                    echo "WARN: Group '${USER}' already added to /etc/sudoers"
                fi
            fi
            waitForAnyKey "... added"
        elif [ ${MODE} -eq 2 ]; then
            printHeader "Setting '${USER}' as sudoer (group) ..."
            ${CMD_grep} -q "${GROUP}" /etc/group
            if [ $? -eq 1 ]; then
                echo "Creating group '${GROUP}' ..."
                ${CMD_groupadd} ${GROUP}
            fi
            ${CMD_grep} -q "%${GROUP}" /etc/sudoers
            if [ $? -eq 1 ]; then
                echo "Adding group '${GROUP}' to /etc/sudoers ..."
                echo "%${GROUP} ALL=(ALL) ALL" >> /etc/sudoers
            else
                echo "WARN: Group '${GROUP}' already added to /etc/sudoers"
            fi
            ${CMD_usermod} -a -G sudoer ${USER}
            waitForAnyKey "... added"
        else
            waitForAnyKey "... skipping 'sudo' settings."
        fi

    else
    
        # --- display warning
        waitForAnyKey "... not running as 'root', skipping 'sudo' configuration"

    fi
    
}

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# BEGIN

# --- usage (and exit)
function usage() {
    # arg $1 = exit code
    local exitCode=$1
    echo ""
    echo "Usage: $0 -home <dir> -user <user> [-sudo] [-pause | -p <sec>] -install"
    echo "  or"
    echo "Usage: $0 -home <dir> -user <user> -gtsVars <file>"
    echo "  or"
    echo "Usage: $0 -arch"
    echo ""
    echo "Options:"
    echo "   -home <dir>       - GTS install directory (\$GTS_HOME used if not specified)"
    echo "   -user[add] <user> - User which should own the GTS/Tomcat ('useradd' to create user)"
    echo "   -install          - perform installation"
    echo "   -pause            - pause between sections until a key is pressed"
    echo "   -nopause          - do not pause between sections"
    echo "   -p <sec>          - pause between sections until key-press or timeout <sec>"
    echo "   -sudo             - Add <GTS_USER> group to '/etc/sudoers'"
    echo "   -arch             - Display system architecture only, and exit"
    echo "   -gtsVars <file>   - Create GTS environment <file>, and exit"
    echo ""
    exit ${exitCode}
}

# --- check arguments
DO_INSTALL=0
SET_SUDOER=0
while (( "$#" )); do
    case "$1" in 

        # ------------------------------------------------------

        # - help
        "-help" | "-h" ) 
            usage 0
            ;;

        # ------------------------------------------------------

        # - GTS_HOME
        "-GTS_HOME" | "-home" ) 
            if [ $# -ge 2 ]; then
                GTS_HOME="$2"
                export GTS_HOME
                shift
            else
                echo ""
                echo "Missing GTS_HOME argument"
                usage 99
            fi
            ;;

        # - GTS_USER
        "-GTS_USER" | "-user" ) 
            if [ $# -ge 2 ]; then
                GTS_USER="$2"
                export GTS_USER
                shift
            else
                echo ""
                echo "Missing GTS_USER argument"
                usage 99
            fi
            ;;

        # - userc (create user if non-existent)
        "-useradd" ) 
            if [ $# -ge 2 ]; then
                GTS_USER="$2"
                export GTS_USER
                CREATE_USER=1
                shift
            else
                echo ""
                echo "Missing GTS_USER argument"
                usage 99
            fi
            ;;

        # ------------------------------------------------------

        # - create /usr/local/gts_vars.env
        "-gtsVars" | "-gtsvars" )
            if [ $# -ge 2 ]; then
                INSTALL_DIR=${GTS_HOME%/*}
                ${GTS_HOME}/bin/makeGtsVarsEnv.sh -dir "${INSTALL_DIR}" -user "${GTS_USER}" -out "$2"
                exit 0
            else
                echo ""
                echo "Missing '-gtsVars' argument"
                usage 99
            fi
            ;;

        # ------------------------------------------------------

        # - MySQL root user
        "-mysqlUser" )
            if [ $# -ge 2 ]; then
                MYSQL_ROOT_USER="$2"
                shift
            else
                echo ""
                echo "Missing 'mysqlUser' argument"
                usage 99
            fi
            ;;

        # - MySQL root password
        "-mysqlPass" )
            if [ $# -ge 2 ]; then
                MYSQL_ROOT_PASS="$2"
                shift
            else
                echo ""
                echo "Missing 'mysqlPass' argument"
                usage 99
            fi
            ;;

        # ------------------------------------------------------

        # - install
        "-install" ) 
            DO_INSTALL=1
            ;;

        # - noextra
        "-noextra" ) 
            INSTALL_EXTRA=0
            ;;

        # - nopause
        "-nopause" | "-np" ) 
            PAUSE_FORANYKEY=-1
            ;;

        # - pause
        "-pause" ) 
            PAUSE_FORANYKEY=0
            ;;

        # - pause
        "-p" ) 
            if [ $# -ge 2 ]; then
                PAUSE_FORANYKEY=$2
                shift
            else
                echo ""
                echo "Missing 'p' pause argument"
                usage 99
            fi
            ;;

        # ------------------------------------------------------

        # - nosudoer
        "-nosudoer" | "-nosudo" ) 
            SET_SUDOER=0
            ;;

        # - sudoer
        "-sudoer" | "-sudo" ) 
            SET_SUDOER=1
            ;;

        # - sudoer
        "-sudoergroup" | "-sudog" ) 
            SET_SUDOER=2
            ;;

        # ------------------------------------------------------

        # - skip remaining args
        "--" )
            shift
            break
            ;;

        # - error
        * )
            echo ""
            echo "Invalid argument! $1"
            usage 99
            ;;

    esac
    shift
done

# -----------------------------------------------------------------------------
# Validate selected options ---------------------------------------------------
# -----------------------------------------------------------------------------

# --- are we root?
WHOAMI=`${CMD_whoami}`
if [ "${WHOAMI}" = "root" ]; then
    IS_ROOT=1
else
    waitForAnyKey "WARNING: Not running as 'root'!  (Control-C to exit)"
fi

# --- check GTS_HOME
if [ "${GTS_HOME}" = "" ]; then 
    printError "ERROR: Var GTS_HOME not defined!"
    usage 99
fi
if [ ! -d "${GTS_HOME}" ]; then 
    printError "ERROR: ${GTS_HOME} does not exist, or is not a directory!"
    usage 99
fi
INSTALL_DIR=${GTS_HOME%/*}   # sed 's|/[^/]*$||'
echo "INSTALL_DIR = ${INSTALL_DIR}"
if [ "${INSTALL_DIR}" != "/usr/local" ]; then
    waitForAnyKey "WARNING: INSTALL_DIR = ${INSTALL_DIR}  (Control-C to exit)"
fi

# --- check GTS_USER
if [ "${GTS_USER}" = "" ]; then 
    printError "ERROR: Var GTS_USER not defined!"
    #echo "[useradd <user>]"
    usage 99
fi
${CMD_grep} -q ^${GTS_USER}: /etc/passwd
if [ $? -ne 0 ]; then
    if [ ${IS_ROOT} -eq 1 ]; then
        if [ ${CREATE_USER} -eq 1 ]; then
            ${CMD_useradd} ${GTS_USER}
            if [ $? -ne 0 ]; then
                printError "ERROR: Unable to create user '${GTS_USER}'"
                usage 99
            fi
        else
            printError "ERROR: User '${GTS_USER}' is not defined in /etc/passwd"
            echo "  # useradd ${GTS_USER}      <-- to create with a home directory"
            echo "(or 'useradd -M ${GTS_USER}' without a home directory)"
            usage 99
        fi
    else
        # - required
        printError "ERROR: not running as 'root', unable to create user"
        exit 1
    fi
fi

# --- validate installation directory
INSTALL_DIR=${GTS_HOME%/*}   # sed 's|/[^/]*$||'
if [ "${INSTALL_DIR}" = "" ] || [ "${INSTALL_DIR}" = "/" ] || [ ! -d "${INSTALL_DIR}" ]; then
    printError "ERROR: Invalid installation directory: ${INSTALL_DIR}"
    exit 1
fi

# --- install specified?
if [ ${DO_INSTALL} -ne 1 ]; then
    if [ ${SET_SUDOER} -ne 0 ]; then
        add_sudoer "${GTS_USER}" "${SET_SUDOER}"
        exit 0
    else
        printError "ERROR: Missing '-install' argument"
        usage 99
    fi
fi

# --- Critical commands required for installation
if [ ! -x "${CMD_yum}" ]; then
    echo ""
    echo "ERROR: Missing '${CMD_yum}' (is this not Fedora Linux?)"
    echo ""
    exit 99
fi
if [ ! -x "${CMD_chkconfig}" ]; then
    echo ""
    echo "ERROR: Missing '${CMD_chkconfig}' (is this not Fedora Linux?)"
    echo ""
    exit 99
fi
if [ ! -x "${CMD_service}" ]; then
    echo ""
    echo "ERROR: Missing '${CMD_service}' (is this not Fedora Linux?)"
    echo ""
    exit 99
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# --- Start Installation
# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# -----------------------------------------------------------------------------
# --- Install the commands we may need (unzip, wget, etc)
install_Tools

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

printHeader "Installing GTS and supporting modules into directory '${INSTALL_DIR}'"

# --- change ownership of GTS_HOME
printMessage "Changing GTS ownership to user ${GTS_USER}"
if [ ${IS_ROOT} -eq 1 ]; then
    ${CMD_chown} -R ${GTS_USER}:${GTS_USER} ${GTS_HOME}
    printMessage "Creating symbolic link: ${INSTALL_DIR}/gts --> ${GTS_HOME}"
    if [ -d "${INSTALL_DIR}/gts" ]; then
        echo "'${INSTALL_DIR}/gts' already exists, leaving directory as-is"
        echo "(remove '${INSTALL_DIR}/gts' to reset 'gts' symbolic link)"
    else
        ( cd ${INSTALL_DIR}; ${CMD_ln} -s ${GTS_HOME} gts )
    fi
else
    # - not required
    waitForAnyKey "Not running as 'root', hopefully '${GTS_USER}' already owns ${GTS_HOME}"
fi

# --- make sure *.sh files are executable
printMessage "Setting GTS *.sh files executable ..."
${CMD_chmod} a+rx ${GTS_HOME}/bin/*.sh ${GTS_HOME}/sampleData/*.sh
if [ -d "${GTS_HOME}/bcross" ]; then
    ${CMD_chmod} a+rx ${GTS_HOME}/bcross/*.sh
fi
waitForAnyKey ""

# --- enable 'auto-start' on boot
printMessage "Enabling GTS autostart"
if [ -f "/etc/init.d/opengts" ]; then
    echo "GTS autostart already exists: /etc/init.d/opengts"
    echo "(remove '/etc/init.d/opengts' to reset GTS aurostart)"
    waitForAnyKey ""
else
    if [ ! -f "${GTS_HOME}/bin/onboot/fedora/opengts" ]; then
        echo "WARNING: Not installing GTS autostart (not found)"
        waitForAnyKey ""
    else
        echo "Setting GTS autostart '/etc/init.d/opengts'"
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_cp} ${GTS_HOME}/bin/onboot/fedora/opengts /etc/init.d/.
            ${CMD_chmod} 755 /etc/init.d/opengts
            ${CMD_chkconfig} --add opengts
            ${CMD_chkconfig} opengts on
            ${CMD_chkconfig} --list opengts
            #/etc/init.d/opengts restart
            waitForAnyKey "... enabled"
        else
            # - required, but not fatal
            waitForAnyKey "... not running as 'root', skipping 'opengts' autostart"
        fi
    fi
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- install MySQL
printHeader "Installing 'mysql', 'mysql-server' ..."
if [ ! -x "/usr/bin/mysql" ] || [ ! -x "/usr/bin/mysqld_safe" ]; then
    if [ ${IS_ROOT} -eq 1 ]; then
        ${CMD_yum} -y install mysql mysql-server
        waitForAnyKey "... installed"
    else
        # - required
        printError "ERROR: not running as 'root', unable to install."
        exit 1
    fi
else
    echo "MySQL appears to already be installed."
fi
if [ -d "/var/lib/mysql" ]; then
    GTS_DUMP_DIR="/var/lib/mysql/gtsDump"
    if [ ! -d "${GTS_DUMP_DIR}" ]; then
        # - The purpose of this directory is to provide MySQL dump an directory
        # - to which it can write the dumped files.  It may have the side effect
        # - of displaying 'gtsDump' as a valid database via "show databases;"
        printMessage "Creating '${GTS_DUMP_DIR}' (owned by '${GTS_USER}')"
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_mkdir} ${GTS_DUMP_DIR}
            ${CMD_chown} ${GTS_USER}:${GTS_USER} ${GTS_DUMP_DIR}
            ${CMD_chmod} 777 ${GTS_DUMP_DIR}
            waitForAnyKey "... created"
        else
            # - not required
            waitForAnyKey "... not running as 'root', skipping GTS-dump directory creation"
        fi
    fi
fi

# --- install MySQL JDBC driver
printHeader "Installing 'mysql-connector-java' ..." # --- appears to be missing on CentOS
${CMD_ls} /usr/share/java/mysql-connector-java*.jar >/dev/null 2>&1 
if [ $? -eq 0 ]; then
    echo "MySQL JDBC driver already installed."
else
    if [ ${IS_ROOT} -eq 1 ]; then
        ${CMD_yum} -y install mysql-connector-java
        ${CMD_ls} /usr/share/java/mysql-connector-java*.jar >/dev/null 2>&1 
        if [ $? -eq 0 ]; then
            waitForAnyKey "... installed"
        else
            waitForAnyKey "ERROR: Unable to install '/usr/share/java/mysql-connector-java*.jar' ..."
        fi
    else
        # - required
        printError "ERROR: not running as 'root', unable to install."
        exit 1
    fi
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- install Java OpenJDK
printHeader "Installing Java OpenJDK ..."
if [ -d "${INSTALL_DIR}/java" ]; then
    echo "Java already installed (found ${INSTALL_DIR}/java)"
else
    #${CMD_ls} /usr/lib/jvm/${JDK_DIR}* >/dev/null 2>&1
    if [ -d "/usr/lib/jvm/${JDK_DIR}/bin" ]; then
        echo "OpenJDK (/usr/lib/jvm/${JDK_DIR}) appears to already be installed"
    else
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_yum} -y install ${JDK_PACKAGE}
            if [ ! -d "/usr/lib/jvm/${JDK_DIR}" ]; then
                printError "ERROR: Unable to install Java OpenJDK"
                exit 1
            fi
        else
            # - required
            printError "ERROR: not running as 'root', unable to install."
            exit 1
        fi
    fi
    ( cd ${INSTALL_DIR}; ${CMD_ln} -s /usr/lib/jvm/${JDK_DIR} java; )
    waitForAnyKey "... installed"
fi
JAVA_HOME=`(cd -P ${INSTALL_DIR}/java; /bin/pwd)`
export JAVA_HOME
JAVA_EXT_DIR="${JAVA_HOME}/jre/lib/ext"

# --- mail.jar
printHeader "Copying 'mail.jar' to Java installation ..."
if [ -f "${JAVA_EXT_DIR}/mail.jar" ]; then
    echo "The Java installation appears to already contain 'mail.jar'"
    echo "(remove '${JAVA_EXT_DIR}/mail.jar' to re-install)"
    waitForAnyKey ""
else
    if [ -f "${GTS_HOME}/jlib/javamail/mail.jar" ]; then
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_cp} ${GTS_HOME}/jlib/javamail/mail.jar ${JAVA_EXT_DIR}/.
            ${CMD_chmod} a+r ${JAVA_EXT_DIR}/mail.jar
            waitForAnyKey "... copied"
         else
            # - required, but not fatal
            waitForAnyKey "... not running as 'root', skipping 'mail.jar' installation"
         fi
    else
        # http://www.gnu.org/software/classpathx/javamail/javamail.html
        # ${CMD_yum} -y install classpathx-mail
        # ${CMD_cp} /usr/share/java/classpathx-mail-1.3.1-monolithic.jar ${JAVA_EXT_DIR}/.
        echo "WARNING: Unable to install 'mail.jar' ... continuing ..."
        waitForAnyKey ""
    fi
fi
 
# --- MySQL JDBC driver
printHeader "Copying MySQL JDBC driver to Java installation ..."
HAS_MYSQL_DRIVER=0
${CMD_ls} ${JAVA_EXT_DIR}/mysql*.jar >/dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "The Java installation appears to already contain a MySQL JDBC driver"
    echo "(remove '${JAVA_EXT_DIR}/mysql*.jar' to re-install)"
    waitForAnyKey ""
    HAS_MYSQL_DRIVER=1
else
    JLIB_MYSQL_DIR="${GTS_HOME}/jlib/jdbc.mysql"
    ${CMD_ls} ${JLIB_MYSQL_DIR}/mysql-connector-java*.jar >/dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "Copying MySQL JDBC driver from GTS installation ..."
        if [ ${IS_ROOT} -eq 1 ]; then
            ${CMD_cp} ${JLIB_MYSQL_DIR}/mysql-connector-java*.jar ${JAVA_EXT_DIR}/.
            ${CMD_chmod} a+r ${JAVA_EXT_DIR}/mysql*.jar
            HAS_MYSQL_DRIVER=1
            waitForAnyKey "... copied"
        else
            # - required, but not fatal
            waitForAnyKey "... not running as 'root', skipping MySQL JDBC driver installation"
        fi
    else
        ${CMD_ls} /usr/share/java/mysql-connector-java*.jar >/dev/null 2>&1 
        if [ $? -eq 0 ]; then
            echo "Copying MySQL JDBC driver from '/usr/share/java/' ..."
            if [ ${IS_ROOT} -eq 1 ]; then
                ${CMD_cp} /usr/share/java/mysql-connector-java*.jar ${JAVA_EXT_DIR}/.
                ${CMD_chmod} a+r ${JAVA_EXT_DIR}/mysql*.jar
                HAS_MYSQL_DRIVER=1
                waitForAnyKey "... copied"
            else
                # - required, but not fatal
                waitForAnyKey "... not running as 'root', skipping MySQL JDBC driver installation"
            fi
        else
            echo "****************************************************************"
            echo "WARNING: MySQL JDBC driver jar not found!"
            echo "(remainding sections requiring the JDBC driver will be skipped)"
            echo "****************************************************************"
            ${CMD_sleep} 4
            waitForAnyKey ""
        fi
    fi
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- install Ant
printHeader "Installing Apache Ant [/usr/bin/ant]"
if [ -x "/usr/bin/ant" ]; then
    echo "Apache Ant package already installed (found /usr/bin/ant)"
    waitForAnyKey ""
else
    if [ ${IS_ROOT} -eq 1 ]; then
        ${CMD_yum} -y install ${ANT_PACKAGE}
        if [ ! -x "/usr/bin/ant" ] && [ "${ANT_PACKAGE}" != "ant" ]; then
            printError "WARN: Unable to install '${ANT_PACKAGE}', trying 'ant'"
            ${CMD_yum} -y install ant
        fi
        if [ ! -x "/usr/bin/ant" ]; then
            printError "ERROR: Unable to install Apache Ant"
            exit 1
        else
            waitForAnyKey "... installed"
        fi
    else
        # - required
        printError "ERROR: not running as 'root', unable to install."
        exit 1
    fi
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- install Apache POI
install_ApachePOI

# --- Apache POI XLS jar
printHeader "Copying Apache POI XLS support to Java installation ..."
${CMD_ls} ${JAVA_EXT_DIR}/poi-*.jar >/dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "The Java installation appears to already contain Apache POI XLS support"
    echo "(remove '${JAVA_EXT_DIR}/poi-*.jar' to re-install)"
    waitForAnyKey ""
else
    if [ -f "${INSTALL_DIR}/${POI_DIR}/${POI_XLS_JAR}" ]; then
        echo "Copying Apache POI support from '${INSTALL_DIR}/${POI_DIR}/${POI_XLS_JAR}' ..."
        ${CMD_cp} ${INSTALL_DIR}/${POI_DIR}/${POI_XLS_JAR} ${JAVA_EXT_DIR}/.
        ${CMD_chmod} a+r ${JAVA_EXT_DIR}/${POI_XLS_JAR}
        waitForAnyKey "... copied"
    else
        echo "****************************************************************"
        echo "WARNING: Apache POI jar not found!"
        echo "(Microsoft XLS support not installed)"
        echo "****************************************************************"
        waitForAnyKey ""
    fi
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- install Tomcat
install_Tomcat

# --- set CATALINA_HOME
CATALINA_HOME=`(cd -P ${INSTALL_DIR}/tomcat; /bin/pwd)`
export CATALINA_HOME

# --- install track.war
printHeader "Deploying 'track.war' to Tomcat"
if [ ! -f "${GTS_HOME}/build/track.war" ]; then
    printError "WARNING: '${GTS_HOME}/build/track.war' not found! (skipping install)"
else
    ${CMD_cp} ${GTS_HOME}/build/track.war ${CATALINA_HOME}/webapps/.
    waitForAnyKey "... installed"
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- create "/var/log/gts" (for GTS_LOGDIR redirection)
if [ ${IS_ROOT} -eq 1 ]; then
    VAR_LOG="/var/log"
    GTS_LOGDIR="${VAR_LOG}/gts"
    if [ -d "${VAR_LOG}" ] && [ ! -d "${GTS_LOGDIR}" ]; then
        printMessage "Creating '${GTS_LOGDIR}' (owned by '${GTS_USER}')"
        ${CMD_mkdir} ${GTS_LOGDIR}
        if [ -d "${GTS_LOGDIR}" ]; then
            ${CMD_chown} ${GTS_USER}:${GTS_USER} ${GTS_LOGDIR}
            ${CMD_chmod} 755 ${GTS_LOGDIR}
            waitForAnyKey "... created"
            export GTS_LOGDIR
        else
            echo "Unable to create '${GTS_LOGDIR}'"
            waitForAnyKey ""
        fi
    fi
fi

# --- create "/var/run/gts" (for GTS_PIDDIR redirection)
if [ ${IS_ROOT} -eq 1 ]; then
    VAR_RUN="/var/run"
    GTS_PIDDIR="${VAR_RUN}/gts"
    if [ -d "${VAR_RUN}" ] && [ ! -d "${GTS_PIDDIR}" ]; then
        printMessage "Creating '${GTS_PIDDIR}' (owned by '${GTS_USER}')"
        ${CMD_mkdir} ${GTS_PIDDIR}
        if [ -d "${GTS_PIDDIR}" ]; then
            ${CMD_chown} ${GTS_USER}:${GTS_USER} ${GTS_PIDDIR}
            ${CMD_chmod} 755 ${GTS_PIDDIR}
            waitForAnyKey "... created"
            export GTS_PIDDIR
        else
            echo "Unable to create '${GTS_PIDDIR}'"
            waitForAnyKey ""
        fi
    fi
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- creating gts.env file
GTSVARS_ENV="${INSTALL_DIR}/gts_vars.env"
printHeader "Creating environment setup file '${GTSVARS_ENV}' ..."
${GTS_HOME}/bin/makeGtsVarsEnv.sh -dir "${INSTALL_DIR}" -user "${GTS_USER}" -out "${GTSVARS_ENV}"
if [ $? -eq 0 ]; then
    waitForAnyKey "... created"
else
    echo "Unable to create file '${GTSVARS_ENV}' due to previous errors"
    waitForAnyKey ""
fi
#if [ -f "${GTSVARS_ENV}" ]; then
#    echo "Environment setup file '${GTSVARS_ENV}' already exists"
#    echo "(remove '${GTSVARS_ENV}' to re-install)"
#    waitForAnyKey ""
#else
#    create_gtsVarsEnv "${GTSVARS_ENV}" "${INSTALL_DIR}" "${GTS_USER}"
#    waitForAnyKey "... created"
#fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- restart MySQL
# - MySQL instructions display on the first startup:
# To start mysqld at boot time you have to copy
# support-files/mysql.server to the right place for your system
#
# PLEASE REMEMBER TO SET A PASSWORD FOR THE MySQL root USER !
# To do so, start the server, then issue the following commands:
# /usr/bin/mysqladmin -u root password 'new-password'
# /usr/bin/mysqladmin -u root -h localhost.localdomain password 'new-password'
#
# Alternatively you can run:
# /usr/bin/mysql_secure_installation
#
# which will also give you the option of removing the test
# databases and anonymous user created by default.  This is
# strongly recommended for production servers.
#
# See the manual for more instructions.
#
# You can start the MySQL daemon with:
# cd /usr ; /usr/bin/mysqld_safe &
#
# You can test the MySQL daemon with mysql-test-run.pl
# cd mysql-test ; perl mysql-test-run.pl
#
# Please report any problems with the /usr/bin/mysqlbug script!
#
# The latest information about MySQL is available on the web at
# http://www.mysql.com
# Support MySQL by buying support/licenses at http://shop.mysql.com
# -------------------------------------------------------------------
# In one case the "/etc/init.d/mysqld" was never installed by "yum install mysql-server".
# Attempting to copy this file manually, and calling "service mysqld start" resulted in
# the following error display:
# ----
# Note: Forwarding request to 'systemctl enable mysqld.service'.
# ln -s '/lib/systemd/system/mysqld.service' '/etc/systemd/system/multi-user.target.wants/mysqld.service'
# 
# Note: This output shows SysV services only and does not include native
#       systemd services. SysV configuration data might be overridden by native
#       systemd configuration.
# 
# Restarting mysqld (via systemctl):  Job failed. See system logs and 'systemctl status' for details.
#                                                            [FAILED]
# ERROR: Unable to start service 'mysqld'
# ----
# The workaround was to simple start mysqld with "/etc/init.d/mysqld start", however
# there will still likely be an issue with restarting mysqld on reboot.
# -------------------------------------------------------------------
printHeader "(Re)Starting MySQL ..."
DO_MYSQL_RESTART=1
if [ ${DO_MYSQL_RESTART} -eq 1 ] && [ ${IS_ROOT} -eq 1 ]; then
    ${CMD_chkconfig} --add ${MYSQLD_NAME}
    if [ $? -eq 0 ]; then
        ${CMD_chkconfig} ${MYSQLD_NAME} on
        ${CMD_chkconfig} --list ${MYSQLD_NAME}
        ${CMD_service} ${MYSQLD_NAME} restart
        if [ $? -ne 0 ]; then
            echo "ERROR: Unable to start service '${MYSQLD_NAME}'"
            waitForAnyKey ""
        else
            MYSQL_STARTED=1
            waitForAnyKey "... restarted"
        fi
    else
        echo "WARNING: Unable to add service '${MYSQLD_NAME}' (${MYSQLD_NAME} not found?)"
        waitForAnyKey ""
    fi
else
    # - required, but we'll assume that MySQL is running
    waitForAnyKey "... skipping 'MySQL' restart"
    MYSQL_STARTED=1
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- initialize GTS database
init_GTSDatabase

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- restart opengts/tomcat
printHeader "(Re)Starting Tomcat/OpenGTS servers ..."
if [ ${IS_ROOT} -eq 1 ]; then
    if [ ${HAS_MYSQL_DRIVER} -eq 1 ]; then
        # --- restart Tomcat
        echo "(Ignore any 'Connection refused' error when stopping Tomcat)"
        ${CMD_service} tomcat restart
        if [ $? -eq 0 ]; then
            echo "Started service 'tomcat'"
        else
            echo "ERROR: Unable to start service 'tomcat'"
        fi
        # --- restart OpenGTS services
        ${CMD_service} opengts restart
        if [ $? -eq 0 ]; then
            echo "Started service 'opengts'"
        else
            echo "ERROR: Unable to start service 'opengts'"
        fi
        # --- step complete
        waitForAnyKey ""
    else
        # --- warning
        echo "WARNING: No MySQL driver."
        echo "WARNING: Not restarting services for 'tomcat'/'opengts'"
        waitForAnyKey ""
    fi
else
    # - required, but not fatal
    waitForAnyKey "... not running as 'root', skipping 'tomcat'/'opengts' restart"
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

if [ $INSTALL_EXTRA -eq 1 ]; then
    
    # --- install postgresql/postgis
    install_PostgreSQL
    
    # --- install extra modules
    install_ExtraModules

fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- create sudoer
if [ ${SET_SUDOER} -ne 0 ]; then
    add_sudoer "${GTS_USER}" "${SET_SUDOER}"
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- check SELinux
if [ ${IS_ROOT} -eq 1 ] && [ -x "${CMD_selinuxenabled}" ]; then
    ${CMD_selinuxenabled} # - will still return '0' if 'Permissive'
    if [ $? -eq 0 ]; then
        echo ""
        echo "WARN: SELinux may be enabled! (getenforce ==> `${CMD_getenforce}`)"
        echo ""
        echo "To temporarily disable SELinux (until next boot):"
        echo "   #  ${CMD_setenforce} 0"
        echo "To permanently disable SELinux:"
        echo "   Edit '/etc/selinux/config', change this"
        echo "      SELINUX=enforcing"
        echo "   To this"
        echo "      SELINUX=permissive"
        echo "   Then reboot"
        waitForAnyKey ""
    fi
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- iptables display
# - /sbin/iptables -I INPUT 5 -p tcp -m tcp --dport 8080 -j ACCEPT
# - /sbin/iptables -I INPUT 5 -p tcp -m state --state NEW -m tcp --dport 8080 -j ACCEPT
# - /etc/init.d/iptables save
if [ -x "${CMD_iptables_save}" ]; then
    echo ""
    echo "Current iptables settings:"
    ${CMD_iptables_save}
    echo "-----------------------------------------------------------------"
    echo "Example commands for inserting access rules for port 8080:"
    echo " /sbin/iptables -I INPUT 5 -p tcp -m tcp --dport 8080 -j ACCEPT"
    echo " /etc/init.d/iptables save"
    echo "-----------------------------------------------------------------"
    waitForAnyKey ""
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- done
printHeader "Done."
echo ""
exit 0

# ---
