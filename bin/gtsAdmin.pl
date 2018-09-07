#!/usr/bin/perl
# -----------------------------------------------------------------------------
# Project    : OpenGTS - Open GPS Tracking System
# URL        : http://www.opengts.org
# File       : gtsAdmin.pl
# Description: Start GTS administrative tool
# -----------------------------------------------------------------------------
# Usage:
#   % gtsAdmin.pl [-port <sqlPort>]
# -----------------------------------------------------------------------------
# If present, this command will use the following environment variables:
#  GTS_HOME - The GTS installation directory (defaults to ("<commandDir>/..")
#  GTS_CONF - The runtime config file (defaults to "$GTS_HOME/default.conf")
# -----------------------------------------------------------------------------
$GTS_HOME = $ENV{"GTS_HOME"};
if ("$GTS_HOME" eq "") {
    print "WARNING: GTS_HOME not defined!\n";
    use Cwd 'realpath'; use File::Basename;
    my $EXEC_BIN = dirname(realpath($0));
    require "$EXEC_BIN/common.pl";
} else {
    require "$GTS_HOME/bin/common.pl";
}
# -----------------------------------------------------------------------------

# --- options
use Getopt::Long;
%argctl = (
    "host:s"        => \$opt_host,
    "port:s"        => \$opt_port,
    "pageSize:s"    => \$opt_pageSize,
    "allowDelete"   => \$opt_allowDelete,
    "version"       => \$opt_version,
    "debug"         => \$opt_debug,
);
$optok = &GetOptions(%argctl);
if (!$optok || (defined $opt_help)) {
usage:;
    print "Usage:\n";
    print "   gtsAdmin.pl -version\n";
    print "   gtsAdmin.pl [-host <MySQL-host>] [-port <MySQL-port>] [-pageSize <recordCount>] [-allowDelete]\n";
    exit(1);
}

# -----------------------------------------------------------------------------

# --- Mac?
if ($IS_MACOSX) {
    $cmd_java = "$cmd_java -Xdock:name='GTSAdmin'";
}

# --- Java command
$Entry_jar = "$GTS_HOME/build/lib/gtsadmin.jar";
if (!(-f $Entry_jar)) {
    $Entry_jar = "$GTS_HOME/lib/gtsopt/gtsadmin.jar";
    if (!(-f $Entry_jar)) {
        print "'gtsadmin.jar' file not found!\n";
        exit(1);
    }
}
$Command = "$cmd_java -jar $Entry_jar -conf=$GTS_CONF -log.file.enable=false";

# --- version?
if (defined $opt_version) {
    my $cmd  = "$cmd_java -jar $Entry_jar -version";
    &sysCmd($cmd, $false);
    exit(0);
}

# --- debug mode
if (defined $opt_debug) {
    $Command .= " -debugMode";
}

# --- host
if (defined $opt_host) {
    $Command .= " -db.sql.host=$opt_host";
}

# --- port forward (ie. over ssh)
if (defined $opt_port) {
    $Command .= " -db.sql.port=$opt_port";
}

# --- allow delete?
if (defined $opt_allowDelete) {
    $Command .= " -gtsAdmin.allowRecordDelete=true";
}

# --- page size?
if (defined $opt_pageSize) {
    $Command .= " -gtsAdmin.browsePageSize=$opt_pageSize";
}

# --- execute
my $args = join(' ', @ARGV);
my $cmd  = $Command . " $args";
&sysCmd($cmd, $GTS_DEBUG);
exit(0);

# -----------------------------------------------------------------------------

