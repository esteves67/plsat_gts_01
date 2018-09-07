#!/usr/bin/perl
# -----------------------------------------------------------------------------
# Project    : OpenGTS - Open GPS Tracking System
# URL        : http://www.opengts.org
# File       : gtsConfig.pl
# Description: Start GTS administrative tool
# -----------------------------------------------------------------------------
# Usage:
#   % gtsConfig.pl
# -----------------------------------------------------------------------------
# If present, this command will use the following environment variables:
#  GTS_HOME - The GTS installation directory (defaults to ("<commandDir>/..")
#  GTS_CONF - The runtime config file (defaults to "$GTS_HOME/default.conf")
# -----------------------------------------------------------------------------
$GTS_HOME = $ENV{"GTS_HOME"};
if ("$GTS_HOME" eq "") {
    print "!!! ERROR: GTS_HOME not defined !!!\n";
    use Cwd 'realpath'; use File::Basename;
    my $EXEC_BIN = dirname(realpath($0));
    require "$EXEC_BIN/common.pl";
    exit(99);
} else {
    require "$GTS_HOME/bin/common.pl";
}
# -----------------------------------------------------------------------------

# --- options
use Getopt::Long;
%argctl = (
    "help"          => \$opt_help,
    "version"       => \$opt_version,
    "config"        => \$opt_config,
    "edit"          => \$opt_editConfig,
    "editConfig"    => \$opt_editConfig,
    "installLog:s"  => \$opt_logFile,
    "logFile:s"     => \$opt_logFile,
    "debug"         => \$opt_debug,
);
$optok = &GetOptions(%argctl);
if (!$optok || (defined $opt_help)) {
usage:;
    print "Usage:\n";
    print "   gtsConfig.pl -help\n";
    print "   gtsConfig.pl -version\n";
    print "   gtsConfig.pl -editConfig\n";
    print "   gtsConfig.pl [-installLog=<logFile>] [-config]\n";
    print "   gtsConfig.pl\n";
    exit(1);
}

# -----------------------------------------------------------------------------

# --- Mac?
if ($IS_MACOSX) {
    $cmd_java = "$cmd_java -Xdock:name='GTSConfig'";
}

# --- Java entry point and command-line
$Entry_jar = "$GTS_HOME/build/lib/gtsconfig.jar";
if (!(-f $Entry_jar)) {
    $Entry_jar = "$GTS_HOME/lib/gtsopt/gtsconfig.jar";
    if (!(-f $Entry_jar)) {
        print "'gtsconfig.jar' file not found!\n";
        exit(1);
    }
}
$Command = "$cmd_java -jar $Entry_jar -conf=$GTS_CONF -log.file.enable=false";

# --- debug mode
if (defined $opt_debug) {
    $GTS_DEBUG = $true;
    $Command .= " -debugMode";
}

# --- Path
#$ExportPath = "export PATH=$GTS_HOME/bin:" . $ENV{"PATH"};

# --- install log
if (defined $opt_logFile) {
    $Command .= " -installLog='$opt_logFile'";
}

# --- config "wizard"
if (defined $opt_config) {
    $Command .= " -config=true";
}

# --- command-line edit config "wizard"
if (defined $opt_editConfig) {
    $Command .= " -editConfig=true";
}

# --- execute
my $cfgArg = join(' ', @ARGV);
#my $cfgCmd  = $ExportPath . ";" . $Command . " $cfgArg";
my $cfgCmd = $Command . " $cfgArg";
&sysCmd($cfgCmd, $GTS_DEBUG);
exit(0);

# -----------------------------------------------------------------------------

