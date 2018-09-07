#!/usr/bin/perl
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : dumpExtract.pl
# -----------------------------------------------------------------------------
# Usage:
#   % dumpExtract.pl -dir=<dumpDir> -account=<account> -todir=<destDir>
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

# --- tables
@SYS_TABLES = (
    "SystemProps"       ,
    "UnassignedDevices" ,
);

# --- tables
@ACCT_TABLES = (
    "Account"           ,
    "AccountString"     ,
    "Antx"              ,
    "BorderCrossing"    ,
    "Device"            ,
    "DeviceGroup"       ,
    "DeviceList"        ,
    "Diagnostic"        ,
    "Driver"            ,
    "Entity"            ,
    "EventData"         ,
    "EventTemplate"     ,
    "GeoCorridor"       ,
    "GeoCorridorList"   ,
    "Geozone"           ,
    "GroupList"         ,
    "NotifyQueue"       ,
    "PendingCommands"   ,
    "PendingPacket"     ,
    "Property"          ,
    "Resource"          ,
    "Role"              ,
    "RoleAcl"           ,
    "Rule"              ,
    "RuleList"          ,
    "SessionStats"      ,
    "StatusCode"        ,
    "SystemAudit"       ,
   #"SystemProps"       ,   # <-- system table
    "Transport"         ,
   #"UnassignedDevices" ,   # <-- system table
    "UniqueXID"         ,   # <-- will not copy (account is not first column)
    "User"              ,
    "UserAcl"           ,
    "UserDevice"        ,
    "WorkOrder"         ,
    "WorkZone"          ,
    "WorkZoneList"      ,
);

# --- options
use Getopt::Long;
%argctl = (
    "todir:s"       => \$opt_todir,         # - destination directory
    "dir:s"         => \$opt_frdir,         # - dump directory
    "account:s"     => \$opt_account,       # - account 
    "help"          => \$opt_help,
);
$optok = &GetOptions(%argctl);
if (!$optok || $opt_help) {
    usage:;
    print "Usage: $0 -dir=<dumpDir> -account=<account> -todir=<destDir>\n";
    exit(1);
}
if (!(defined $opt_frdir)) {
    print "Missing '-dir' ...\n";
    goto usage;
}
if (!(defined $opt_todir)) {
    print "Missing '-todir' ...\n";
    goto usage;
}
if (!(defined $opt_account)) {
    print "Missing '-account' ...\n";
    goto usage;
}

# -----------------------------------------------------------------------------

# --- from/dump directory exists?
if (!(-d "$opt_frdir")) {
    print "Source/Dump directory does not exist: $opt_frdir\n";
    exit(1);
}

# --- destination directory already exists?
if (-e "$opt_todir") {
    print "Destination directory already exists: $opt_todir\n";
    exit(1);
}

# --- split accounts
my @acctList = split(',', $opt_account);

# --- make destination directory
print "Making destination directory ...\n";
&sysCmd("mkdir -p $opt_todir",$true);

# --- for each account
foreach my $account (@acctList) {

    # --- copy files
    $dataCopied = $false;
    foreach my $table (@ACCT_TABLES) {
        if ((-f "$opt_frdir/$table.sql") && (-f "$opt_frdir/$table.txt")) {
            print "Copying $table ...\n";
            &sysCmd("cp $opt_frdir/$table.sql $opt_todir/$table.sql",$true);
            my $sc = &sysCmd("grep '^\"$account\"' $opt_frdir/$table.txt >> $opt_todir/$table.txt",$true);
            if ($sc == 0) {
                my $rcdCnt = `wc -l $opt_todir/$table.txt`; chomp $rcdCnt;
                print "Data copied: $rcdCnt records ...\n";
                $dataCopied = $true;
            } else {
                print "Nothing to copy ...\n";
            }
        } else {
            print "Warning: $opt_frdir/$table.[sql|txt] does not exist!\n";
        }
        print "\n";
    }

    # --- no data copied?
    if ($dataCopied) {
        print "Data was copied for account: $account\n";
    } else {
        print "*******************************************\n";
        print "No data was found for account: $account\n";
        print "*******************************************\n";
    }
    print "\n";
    
}

# -----------------------------------------------------------------------------
