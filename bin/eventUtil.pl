#!/usr/bin/perl
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : eventUtil.pl
# -----------------------------------------------------------------------------
# Usage:
#   % eventUtil.pl [-debug]
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
    "help"      => \$opt_help,
    "debug"     => \$opt_debug,
);
$optok = &GetOptions(%argctl);
if (!$optok || (defined $opt_help)) {
usage:;
    print "Usage:\n";
    print "   eventUtil.pl [-debug] -- <...>\n";
    print "\n";
    print "Reverse-geocode Example:\n";
    print "   eventUtil.pl -debug -- -account=<acct> -device=ALL -rg=2007/11/01,2008/03/20 -update\n";
    print "\n";
    exit(1);
}

# -----------------------------------------------------------------------------

# --- Java command
$Entry_point = "$GTS_STD_PKG.db.EventUtil";
$CP          = "-classpath \"$CLASSPATH\"";
$Command     = "$cmd_java $CP $Entry_point -conf=$GTS_CONF";

# --- debug mode
if (defined $opt_debug) {
    $Command .= " -debugMode";
}

# --- args
$Args = "";
foreach ( @ARGV ) {
    $Args .= " '$_'";
}

# --- run
$CMD = "$Command $Args";
$status = &sysCmd("$CMD", $true);
exit($status);

# ---
