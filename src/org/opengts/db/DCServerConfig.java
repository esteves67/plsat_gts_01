// ----------------------------------------------------------------------------
// Copyright 2007-2014, OpenGTS
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Description:
//  Device Communication Server configuration (central registry for port usage)
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

public class DCServerConfig
    implements Comparable
{

    // ------------------------------------------------------------------------
    // flags

    // default property group id
    public static final String  DEFAULT_PROP_GROUP_ID           = "default";

    // Boolean Properties
    public static final String  P_NONE                          = "none";
    public static final String  P_HAS_INPUTS                    = "hasInputs";
    public static final String  P_HAS_OUTPUTS                   = "hasOutputs";
    public static final String  P_COMMAND_SMS                   = "commandSms";
    public static final String  P_COMMAND_UDP                   = "commandUdp";
    public static final String  P_COMMAND_TCP                   = "commandTcp";
    public static final String  P_XMIT_TCP                      = "transmitTcp";
    public static final String  P_XMIT_UDP                      = "transmitUdp";
    public static final String  P_XMIT_SMS                      = "transmitSms";
    public static final String  P_XMIT_SAT                      = "transmitSat";
    public static final String  P_JAR_OPTIONAL                  = "jarOptional";

    public static final long    F_NONE                          = 0x00000000L;
    public static final long    F_HAS_INPUTS                    = 0x00000002L; // hasInputs
    public static final long    F_HAS_OUTPUTS                   = 0x00000004L; // hasOutputs
    public static final long    F_COMMAND_TCP                   = 0x00000100L; // commandTcp
    public static final long    F_COMMAND_UDP                   = 0x00000200L; // commandUdp
    public static final long    F_COMMAND_SMS                   = 0x00000400L; // commandSms
    public static final long    F_XMIT_TCP                      = 0x00001000L; // transmitTcp
    public static final long    F_XMIT_UDP                      = 0x00002000L; // transmitUdp
    public static final long    F_XMIT_SMS                      = 0x00004000L; // transmitSms
    public static final long    F_XMIT_SAT                      = 0x00008000L; // transmitSat
    public static final long    F_JAR_OPTIONAL                  = 0x00010000L; // jarOptional
    
    public static final long    F_STD_VEHICLE                   = F_HAS_INPUTS | F_HAS_OUTPUTS | F_XMIT_TCP | F_XMIT_UDP;
    public static final long    F_STD_PERSONAL                  = F_XMIT_TCP | F_XMIT_UDP;
    
    public static long GetAttributeFlags(RTProperties rtp)
    {
        long flags = 0L;
        if (rtp.getBoolean(P_HAS_INPUTS  ,false)) { flags |= F_HAS_INPUTS  ; }
        if (rtp.getBoolean(P_HAS_OUTPUTS ,false)) { flags |= F_HAS_OUTPUTS ; }
        if (rtp.getBoolean(P_COMMAND_SMS ,false)) { flags |= F_COMMAND_SMS ; }
        if (rtp.getBoolean(P_COMMAND_UDP ,false)) { flags |= F_COMMAND_UDP ; }
        if (rtp.getBoolean(P_COMMAND_TCP ,false)) { flags |= F_COMMAND_TCP ; }
        if (rtp.getBoolean(P_XMIT_TCP    ,false)) { flags |= F_XMIT_TCP    ; }
        if (rtp.getBoolean(P_XMIT_UDP    ,false)) { flags |= F_XMIT_UDP    ; }
        if (rtp.getBoolean(P_XMIT_SMS    ,false)) { flags |= F_XMIT_SMS    ; }
        if (rtp.getBoolean(P_XMIT_SAT    ,false)) { flags |= F_XMIT_SAT    ; }
        if (rtp.getBoolean(P_JAR_OPTIONAL,false)) { flags |= F_JAR_OPTIONAL; }
        return flags;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public enum CommandProtocol implements EnumTools.IsDefault, EnumTools.IntValue {
        UDP(0,"udp"),
        TCP(1,"tcp"),
        SMS(2,"sms");
        // ---
        private int         vv = 0;
        private String      ss = "";
        CommandProtocol(int v, String s) { vv = v; ss = s; }
        public int getIntValue()   { return vv; }
        public String toString()   { return ss; }
        public boolean isDefault() { return this.equals(UDP); }
        public boolean isSMS()     { return this.equals(SMS); }
    };

    /* get the CommandProtocol Enum valud, based on the value of the specified String */
    public static CommandProtocol getCommandProtocol(String v)
    {
        // returns 'null' if protocol value is invalid
        return EnumTools.getValueOf(CommandProtocol.class, v);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Device event code to status code translation
    **/
    public static class EventCode
    {
        private int    oldCode     = 0;
        private int    statusCode  = StatusCodes.STATUS_NONE;
        private String dataString  = null;
        private long   dataLong    = Long.MIN_VALUE;
        public EventCode(int oldCode, int statusCode, String data) {
            this.oldCode    = oldCode;
            this.statusCode = statusCode;
            this.dataString = data;
            this.dataLong   = StringTools.parseLong(data, Long.MIN_VALUE);
        }
        public int getCode() {
            return this.oldCode;
        }
        public int getStatusCode() {
            return this.statusCode;
        }
        public String getDataString(String dft) {
            return !StringTools.isBlank(this.dataString)? this.dataString : dft;
        }
        public long getDataLong(long dft) {
            return (this.dataLong != Long.MIN_VALUE)? this.dataLong : dft;
        }
        public String toString() {
            return StringTools.toHexString(this.getCode(),16) + " ==> 0x" + StringTools.toHexString(this.getStatusCode(),16);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Perl 'psjava' command (relative to GTS_HOME) */
    private static final String PSJAVA_PERL  = File.separator + "bin" + File.separator + "psjava";

    /**
    *** Returns the "psjava" command relative to GTS_HOME
    *** @return The "psjava" command relative to GTS_HOME
    **/
    public static String getPSJavaCommand()
    {
        File psjava = FileTools.toFile(DBConfig.get_GTS_HOME(), new String[] {"bin","psjava"});
        if (psjava != null) {
            return psjava.toString();
        } else {
            return null;
        }
    }

    /**
    *** Returns the "psjava" command relative to GTS_HOME, and returning the 
    *** specified information for the named jar file
    *** @param name    The DCServerConfig name
    *** @param display The type of information to return ("pid", "name", "user")
    *** @return The returned 'display' information for the specified DCServerConfig
    **/
    public static String getPSJavaCommand_jar(String name, String display)
    {
        String psjava = DCServerConfig.getPSJavaCommand();
        if (!StringTools.isBlank(psjava)) {
            StringBuffer sb = new StringBuffer();
            sb.append(psjava);
            if (OSTools.isWindows()) {
                sb.append(" \"-jar=").append(name).append(".jar\"");
                if (!StringTools.isBlank(display)) {
                    sb.append(" \"-display="+display+"\"");
                }
            } else {
                sb.append(" -jar=").append(name).append(".jar");
                if (!StringTools.isBlank(display)) {
                    sb.append(" -display="+display+"");
                }
            }
            return sb.toString();
        } else {
            return null;
        }
    }
    
    /**
    *** Returns the file path for the named running DCServerConfig jar files.<br>
    *** This method will return 'null' if no DCServerConfig jar files with the specified
    *** name are currently running.<br>
    *** All matching running DCServerConfig entries will be returned.
    *** @param name  The DCServerConfig name
    *** @return The matching running jar file paths, or null if no matching server enteries are running.
    **/
    public static File[] getRunningJarPath(String name)
    {
        if (OSTools.isLinux() || OSTools.isMacOS()) {
            try {
                String cmd = DCServerConfig.getPSJavaCommand_jar(name,"name");
                Process process = (cmd != null)? Runtime.getRuntime().exec(cmd) : null;
                if (process != null) {
                    BufferedReader procReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    StringBuffer sb = new StringBuffer();
                    for (;;) {
                        String line = procReader.readLine();
                        if (line == null) { break; }
                        sb.append(StringTools.trim(line));
                    }
                    process.waitFor();
                    procReader.close();
                    int exitVal = process.exitValue();
                    if (exitVal == 0) {
                        String jpath[] = StringTools.split(sb.toString(), '\n');
                        java.util.List<File> jpl = new Vector<File>();
                        for (int i = 0; i < jpath.length; i++) {
                            if (StringTools.isBlank(jpath[i])) { continue; }
                            File jarPath = new File(sb.toString());
                            try {
                                jpl.add(jarPath.getCanonicalFile());
                            } catch (Throwable th) {
                                jpl.add(jarPath);
                            }
                        }
                        if (!ListTools.isEmpty(jpl)) {
                            return jpl.toArray(new File[jpl.size()]);
                        }
                    }
                } else {
                    if (StringTools.isBlank(cmd)) {
                        Print.logWarn("Unable to create 'psjava' command for '"+name+"'");
                    } else {
                        Print.logError("Unable to execute command: " + cmd);
                    }
                }
            } catch (Throwable th) {
                Print.logException("Unable to determine if Tomcat is running:", th);
            }
            return null;
        } else {
            // not supported on Windows
            return null;
        }
    }

    /**
    *** Return log file paths from jar file paths
    **/
    public static File getLogFilePath(File jarPath)
    {
        if (jarPath != null) {
            // "/usr/local/GTS_1.2.3/build/lib/enfora.jar"
            String jarName = jarPath.getName();
            if (jarName.endsWith(".jar")) {
                String name = jarName.substring(0,jarName.length()-4);
                File logDir = new File(jarPath.getParentFile().getParentFile().getParentFile(), "logs");
                if (logDir.isDirectory()) {
                    return new File(logDir, name + ".log");
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static class EventDataAnalogField
    {
        private int         index       = 0;
        private double      gain        = 1.0 / (double)(1L << 10);  // default 10-bit analog
        private double      offset      = 0.0;
        private DBField     dbField     = null;
        public EventDataAnalogField(int ndx, double gain, double offset) {
            this(ndx, gain, offset, null);
        }
        public EventDataAnalogField(int ndx, double gain, double offset, String fieldN) {
            this.index  = ndx;
            this.gain   = gain;
            this.offset = offset;
            this.setFieldName(fieldN);
            Print.logInfo("New AnalogField["+this.index+"]: " + this);
        }
        public EventDataAnalogField(int ndx, String gof, double dftGain, double dftOffset) {
            this.index  = ndx;
            String  v[] = StringTools.split(gof,',');
            this.gain   = (v.length >= 1)? StringTools.parseDouble(v[0],dftGain  ) : dftGain;
            this.offset = (v.length >= 2)? StringTools.parseDouble(v[1],dftOffset) : dftOffset;
            this.setFieldName((v.length >= 3)? StringTools.blankDefault(v[2],null) : null);
            Print.logInfo("New AnalogField["+this.index+"]: " + this);
        }
        public int getIndex() {
            return this.index;
        }
        public void setGain(double gain) {
            this.gain = gain;
        }
        public double getGain() {
            return this.gain;
        }
        public void setOffset(double offset) {
            this.offset = offset;
        }
        public double getOffset() {
            return this.offset;
        }
        public double convert(long value) {
            return this.convert((double)value);
        }
        public double convert(double value) {
            return (value * this.gain) + this.offset;
        }
        public void setFieldName(String fieldN) {
            this.dbField = null;
            if (!StringTools.isBlank(fieldN)) {
                DBField dbf = EventData.getFactory().getField(fieldN);
                if (dbf == null) {
                    Print.logError("**** EventData analog field does not exist: " + fieldN);
                } else {
                    this.dbField = dbf;
                    Object val = this.getValueObject(0.0);
                    if (val == null) {
                        Print.logError("**** EventData field type not supported: " + fieldN);
                        this.dbField = null;
                    }
                }
            }
        }
        public DBField getDBField() {
            return this.dbField;
        }
        public String getFieldName() {
            return (this.dbField != null)? this.dbField.getName() : null;
        }
        public Object getValueObject(double value) {
            // DBField
            DBField dbf = this.getDBField();
            if (dbf == null) {
                return null;
            }
            // convert to type
            Class dbfc = dbf.getTypeClass();
            if (dbfc == String.class) {
                return String.valueOf(value);
            } else
            if ((dbfc == Integer.class) || (dbfc == Integer.TYPE)) {
                return new Integer((int)value);
            } else
            if ((dbfc == Long.class)    || (dbfc == Long.TYPE   )) {
                return new Long((long)value);
            } else
            if ((dbfc == Float.class)   || (dbfc == Float.TYPE  )) {
                return new Float((float)value);
            } else
            if ((dbfc == Double.class)  || (dbfc == Double.TYPE )) {
                return new Double(value);
            } else
            if ((dbfc == Boolean.class) || (dbfc == Boolean.TYPE)) {
                return new Boolean(value != 0.0);
            }
            return null;
        }
        public boolean saveEventDataFieldValue(EventData evdb, double value) {
            if (evdb != null) {
                Object objVal = this.getValueObject(value); // null if no dbField
                if (objVal != null) {
                    String fn = this.getFieldName();
                    boolean ok = evdb.setFieldValue(fn, objVal);
                    Print.logInfo("Set AnalogField["+this.getIndex()+"]: "+fn+" ==> " + (ok?evdb.getFieldValue(fn):"n/a"));
                    return ok;
                }
            }
            return false;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("index="  ).append(this.getIndex());
            sb.append(" gain="  ).append(this.getGain());
            sb.append(" offset=").append(this.getOffset());
            sb.append(" field=" ).append(this.getFieldName());
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String DecodeEscapeChars(String s)
    {
        if (s == null) {
            return "";
        } else {
            s = StringTools.replace(s,"\\n","\n");
            s = StringTools.replace(s,"\\r","\r");
            return s;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String  COMMAND_CONFIG                  = "config";     // arg=deviceCommandString
    public static final String  COMMAND_PING                    = "ping";       // arg=commandID
  //public static final String  COMMAND_OUTPUT                  = "output";     // arg=gpioOutputState
    public static final String  COMMAND_GEOZONE                 = "geozone";    // arg=""

    public static final String  DEFAULT_ARG_NAME                = "arg";

    public class Command
    {
        private String                  name        = null;
        private String                  desc        = null;
        private String                  types[]     = null;
        private String                  aclName     = "";
        private AclEntry.AccessLevel    aclDft      = AclEntry.AccessLevel.WRITE;
        private String                  cmdStr      = "";
        private boolean                 hasArgs     = false;
        private String                  cmdProto    = null;
        private String                  protoHandlr = null;
        private boolean                 expectAck   = false;
        private int                     cmdStCode   = StatusCodes.STATUS_NONE;
        private OrderedMap<String,CommandArg> argMap = null;
        public Command(
            String name, String desc, 
            String types[], String aclName, AclEntry.AccessLevel aclDft,
            String cmdStr, boolean hasArgs, Collection<CommandArg> cmdArgs,
            String cmdProtoH, boolean expectAck,
            int cmdStCode) {
            this.name      = StringTools.trim(name);
            this.desc      = StringTools.trim(desc);
            this.types     = (types != null)? types : new String[0];
            this.aclName   = StringTools.trim(aclName);
            this.aclDft    = (aclDft != null)? aclDft : AclEntry.AccessLevel.WRITE;
            //this.cmdStr    = (cmdStr != null)? cmdStr : ""; // DecodeEscapeChars
            this.cmdStr    = DecodeEscapeChars(cmdStr);
            //if (!this.cmdStr.equals(cmdStr)) {Print.logInfo("\""+cmdStr+"\" ==> \""+this.cmdStr+"\"");}
            this.hasArgs   = hasArgs || (this.cmdStr.indexOf("${") >= 0);
            cmdProtoH  = StringTools.trim(cmdProtoH);
            if (cmdProtoH.indexOf(":") >= 0) {
                int p = cmdProtoH.indexOf(":");
                this.cmdProto    = StringTools.trim(cmdProtoH.substring(0,p));
                this.protoHandlr = StringTools.trim(cmdProtoH.substring(p+1));
            } else {
                this.cmdProto    = cmdProtoH;
                this.protoHandlr = null;
            }
            this.expectAck = expectAck;
            this.cmdStCode = (cmdStCode > 0)? cmdStCode : StatusCodes.STATUS_NONE;
            if (!ListTools.isEmpty(cmdArgs) && this.hasArgs) {
                this.argMap = new OrderedMap<String,CommandArg>();
                for (CommandArg arg : cmdArgs) {
                    arg.setCommand(this);
                    this.argMap.put(arg.getName(),arg);
                }
            }
        }
        public DCServerConfig getDCServerConfig() {
            return DCServerConfig.this;
        }
        public String getName() {
            return this.name; // not null
        }
        public String getDescription() {
            return this.desc; // not null
        }
        public String[] getTypes() {
            return this.types;
        }
        public boolean isType(String type) {
            return ListTools.contains(this.types, type);
        }
        public String getAclName() {
            return this.aclName; // not null
        }
        public AclEntry.AccessLevel getAclAccessLevelDefault() {
            return this.aclDft; // not null
        }
        public String getCommandString() {
            return this.cmdStr; // not null
        }
        //public String getCommandString(String cmdArgs[]) {
        //    return this.getCommandString(null, cmdArgs);
        //}
        public String getCommandString(Device device, String cmdArgs[]) {
            String cs = this.getCommandString();
            // insert command arguments
            Print.logInfo("................................");
            Print.logInfo(device.getUniqueID());
//            for (int i=0; i<cmdArgs.length;i++) {
//              Print.logInfo(cmdArgs[i]);
//              }
            if (this.hasCommandArgs()) {
                final String args[] = (cmdArgs != null)? cmdArgs : new String[0];
                return StringTools.replaceKeys(cs, new StringTools.KeyValueMap() {
                    public String getKeyValue(String key, String notUsed, String dft) {
                        int argNdx = (Command.this.argMap != null)? Command.this.argMap.indexOfKey(key) : -1;
                        if ((argNdx >= 0) && (argNdx < args.length)) {
                            Print.logInfo("................................1");
                            return (args[argNdx] != null)? args[argNdx] : dft;
                        } else
                        if (key.equals(DEFAULT_ARG_NAME)) { 
                            Print.logInfo("................................2");
                            Print.logInfo("argumento: " + args[0]);
                            Print.logInfo("dft:" + dft);
                            return ((args.length > 0) && (args[0] != null))? args[0] : dft; 
                        } else {
                            for (int i = 0; i < args.length; i++) {
                                if (key.equals(DEFAULT_ARG_NAME+i)) { // "arg0", "arg1", ...
                                    Print.logInfo("................................3");
                                    return (args[i] != null)? args[i] : dft; 
                                }
                            }
                            Print.logInfo("................................4");
                            return dft;
                        }
                    }
                });
            }
            // insert fixed arguments
            if (device != null) {
                String dataKey  = device.getDataKey();      // %{dataKey}, {DATAKEY}
                String modemID  = device.getModemID();      // %{modemID}, {MODEMID}
                String imeiNum  = device.getImeiNumber();   // %{imei}   , {IMEI}
                String serial   = device.getSerialNumber(); // %{serial} , {SERIAL}
                //846326
                String hardcode_devices = "205467978,205467989,205467977,205467963,205469507,205469497,205469501,205468624,205468644,205467990,205467987,205469499,205469517,205467988,205469500,205469510,205206141,205206187,205206182,205206181,205470535,205470534,205470533,205470532,205470528,205470524,205470530,205470525,205498485,205498480,205498486,205498484,205470531,205498481,205498482,205206198,205470529,205498476,205498478,205498477,205470526,205498475,205470563,205498487,205498488,205498489,205498490,205498491,205498492,205498493,205498494,205498495,205498496,205498497,205498498,205498499,205498500,205498501,205498502,205498503,205498504,205498505,205498506,205498507,205498508,205498509,205498510,205498511,205498512,205498513,205498514,205498515,205498516,205498517,205498518,205498519,205498520,205498521,205498522,205498525,205498534,205185718,205190427,205185716,205190442,205900095,205901359,205227198,205229234,835778,827625,841089,963452,205470572,205470573,205470574,205470575,205470576,205470577,205470578,205470579,205470581,205470582,205498369,205498371,205498378,205498379,205498380,205498381,205498382,205498383,205498384,205498385,205498386,205498387,205498388,205498389,205498390,008364679,008365247,008374093,008375578,008376989,008437967,008438956,008438117,008437962,008443695,008444047,008444095,008443558,008438921,008438914,008443694,008443683,008443685,008444083,008437749,008438916,008438933,008444082,008437753,008438972,205190427,205190437,205226983,205190491,205190374,205900119,827582,827517,827625,846298,775550,836058,827501,828503,828500,827517,827582,205226967,205208727,205208920,205226981,205168376,205190485,205900576,205190413,205900767,205194211,205194228,205190395,205190397,205900719,205190503,205168133,205901011,205900118,205236930,205900130,205900423,205226962,205227089,205168399,205185823,841102,841088,205232505,205226980,205194222,205190368,205236955,205900813,205900810,205900113,205168399,205900114,205901183,847313,962542,841098,830169,835780,843248,205901395,205901398,205901399,205901401,205901402,205901403,205901404,205901406,205901407,205901408,205901410,205901411,205901412,205901413,205901414,205901415,205901416,205901417,205901418,205901424,205901426,205901430,205901649,205901656,205901660,205901668,205232519,205900054,205900057,205900064,205900073,205900075,205900079,205900086,205900328,205900330,205900332,205900335,205900337,205900341,205900344,205900348,205901383,205901384,205901385,205901386,205901391,205901392,205901393,205901394,205901396,205168399,775550,205168320,205190419,205775569,205901366,205190454,846316,205168138,205185829,205185825,205190389,205229253,205227035,962569,205900543,205900544,205900545,205900546,205900547,205900548,205900549,205900550,205900551,205900552,205900553,205900554,205900555,205900556,205900557,205900558,205900559,205900560,205900561,205900562,205900563,205900564,205900565,205900566,205901011,205901015,205900519,205900520,205900521,205900522,205900523,205900524,205900525,205900526,205900527,205900528,205900529,205900530,205900531,205900532,205900533,205900534,205900535,205900536,205900537,205900538,205900539,205900540,205900541,205900542,205900543,205900567,205194210,205901361,205901696,205236955,205226989,205227041,205227043,827524,828489,205190394,205168367,205900109,205901353,728512,828489,775570,841098,841098,205236953,205901337,205227079,205208766,205227194,205208758,835813,205900072,205236959,205901344,205900577,205236955,205232524,205226986,835813,205901709,205901710,205901711,205901712,205901713,205901714,205901715,205901716,205901717,205901718,205900350,205900772,205901692,205901693,205901694,205901695,205901696,205901697,205901698,205901699,205901700,205901701,205901702,205901703,205901704,205901705,205901706,205901707,205901708,205901709,5561763947,5561749504,5561721332,5561642299,5561528317,5561514700,5561468281,5561266898,5561256394,5561255158,5554982667,5552757962,5561228629,5561220219,5561220211,5561220025,5561037271,5561018286,205901671,205901672,205901673,205901674,205901675,205901676,205901677,205901678,205901679,205901680,205901681,205901682,205901683,205901684,205901685,205901686,205901687,205901688,205901689,205901690,205901691,205901340,205229245,205232514,205226992,828493,835813,841094,205229217,205229223,205229221,205229228,205229225,205229224,205208733,835813,775570,205190143,205229217,205229223,205229221,205229228,205229225,205229224,205208733,205900569,205900590,205901189,205190518,205190433,205901538,205901250,205901360,205185692,205900858,205900376,835813,205232514,205168369,205190452,205190365,205900823,847306,775587,827561,205465297,205465299,205168369,205900431,205190438,205901339,843320,843240,830018,205465297,205465299,205168369,205900431,205465295,205465296,205465298,205465300,205465302,205465303,205465304,205465305,205465306,205465307,205465276,205465277,205465282,205465283,205465284,205465285,205465286,205465287,205465288,205465289,205465290,205465291,205465292,205465293,205465294,205208751,205185691,205900580,205236925,827573,828410,205900070,205900069,205465260,205465262,205465263,205465270,205465271,205465272,205465273,205465274,205465275,205465278,205465279,205465280,205465281,205465269,205465268,205465267,205465266,205465265,205465264,205465261,205900835,205236928,205185703,205190411,205901352,205168374,205227036,843239,962524,205227249,205227246,951933,835855,830157,827553,843317,843307,846316,828502,827631,962557,843230,827572,205190366,205226957,205227200,848651,962534,843315,828489,962554,827613,962521,205190414,205900868,205900409,205900579,205900777,205901358,205168364,205900871,205229238,205229236,205227084,205232513,830180,827520,830372,836037,205900867,205900870,205900864,205901612,205901618,833569,833528,830325,205901599,205901669,205901622,205901620,205901615,205901621,205901616,205901611,205901610,205901619,205991618,205901613,206901612,205901614,205901601,205901608,205901666,205901599,205901606,205901609,205901603,205901602,205901596,205901597,205901598,205901605,205901600,205901699,205901661,205901604,205901579,205901577,205900719,205900716,205900711,205900712,205901594,205901595,205901587,205901588,205901589,205901590,205901591,205901592,205901593,205901580,205901581,205901582,205901583,205901585,205901575,205900743,205901586,205901576,205901607,205901584,205901578,208900740,205900740,205900720,205900744,205900713,205900728,205900731,205900739,205900715,205900729,205900727,205900756,205900734,205900738,205900733,205900755,205900726,205900725,205900724,205900721,205900714,205900730,205900737,205900749,205900732,205900754,205900748,205900753,205900723,205900722,205900718,205900717,205900735,205900736,205900741,205900742,205900745,205900746,205900747,205900748,205900750,205900751,205900752,205900753,205900754,205900757,205900758,841125,847503,830182,205226972,205208752,205226997,205190521,205140414,205168322,775563,962535,847503,843259,775559,205208742,205190521,205227014,830380,827514,846320,205227228,205900378,205901252,205901259,205901260,205901261,205901262,205901255,205208729,205236920,205190497,205185688,205168114,205194219,205190400,841997,775472,827618,830171,830158,205901557,205901558,205901559,205901560,205901561,205901562,205901563,205901564,205901565,205901566,205901567,205901568,205901569,205901570,205901571,205901572,205901573,205901248,205901249,205901250,205901251,205208729,205236920,205190497,205185688,205168114,205194219,205190400,841997,775472,827618,830171,830158,205901536,205901537,205901538,205901539,205901540,205901541,205901542,205901543,205901544,205901545,205901546,205901547,205901548,205901549,205901550,205901551,205901552,205901553,205901554,205901555,205901556,205900801,205900802,205900803,205900804,205900805,205900806,205901254,205901258,205901263,205901266,205901269,205901285,205901527,205901528,205901529,205901530,205901531,205901532,205901533,205901534,205901535,205227229,205900759,205900760,205900761,205900762,205900763,205900764,205900765,205900766,205900767,205900768,205900769,205900770,205900771,205900772,205900773,205900792,205900793,205900794,205900795,205900796,205900797,205900798,205900799,205900800,205900774,205900775,205900776,205900777,205900778,205900779,205900780,205900781,205900782,205900783,205900784,205900785,205900786,205900787,205900788,205900789,205900790,205900791,205900763,205185686,208208784,830185,205208747,205168361,205168314,205194201,205236936,829598,205208784,205208747,205168361,205190497,205168314,205194201,205236936,205900884,205168114,830185,830185,829598,205227045,205229231,205190371,205168312,205901031,205901153,205194232,205168404,205190519,205194209,205168147,205190490,838622,848584,830307,847536,802795,205901187,205901186,205900598,205190375,205901154,205901174,205168144,205901372,205168385,205227224,205208788,205226996,205232506,205227057,205901152,205901153,205901154,205901156,205901159,205901149,205901157,205901031,205901179,205901180,205901178,205901177,205901181,205901175,205901186,205901185,205901188,205901176,205901190,205194232,205168404,205190519,205194209,205168147,205190490,205900572,205901183,205901184,205901171,205901169,205901172,205901173,205901182,205901160,205901161,205901162,205901165,205901166,205901167,205901168,205901170,205901145,205901151,205901155,205901158,205901146,205901163,205901164,205901144,205901147,205901148,205901150,205901143,205901027,205168143,843170,205168118,827560,843875,827522,205900134,205900133,205900132,205185830,205190429,205190455,205226996,205190447,205190431,828425,847504,930185,827585,827598,205226963,205227046,205226988,205190407,205190425,205229242,205467978,205185689,205227245,205190390,205190384,830380,205236984,205190376,205229236,205232521,205226971,205226968,205226969,205227199,205226995,205232527,205168144,205901357,205900574,847504,775597,205236931,830021,205900454,205900470,205900458,205900459,205900460,205900461,205900462,205900463,205900464,205900465,205900466,205900467,205900468,205900469,205900470,205900454,205900448,205900449,205900450,205900451,205900452,205900453,205900454,205900455,205900456,205900457205194232,830306,827521,835864,205227070,830339,962532,205469522,205901362,846327,827610,205236936,205190469,205467987,205227048,205900126,205900441,205900442,205900443,205900444,205900445,205900446,205900447,205900432,205900433,205900434,205900435,205900436,205900437,205900438,205900439,205900440,205900423,205900424,205900425,205900426,205900427,205900428,205900429,205900430,205465355,205465350,205227238,205226993,205900887,205900880,205900879,205900878,205900877,205900874,205900870,205900869,205900865,205900862,205900861,205190496,205900859,205900856,205900873,205226969,205227199,205226971,205226968,205226995,205900574,205232527,205900609,205901357,205900867,205901049,205901055,205900392,205900375,205900385,205900409,205900376,205901372,205901053,205900419,205900888,205900882,205900883,205900885,205900886,205900881,205900889,205900890,205900380,205900899,205900871,205900131,205229247,205232488,205232503,205236961,205900568,205900807,205900808,205900809,205900811,205901360,205900129,205900128,205190461,205168361,205168118,205168121,205168314,205185692,205190367,205190377,205190418,205190461,205194226,205208781,205227028,205227234,205229237,205232513,205900127,205900128,205900129,205900589,205900857,205900858,205900860,205900866,205900868,205900876,205901056,775565,830316,836049,847308,205900875,205900863,835872,847310,775464,843222,843255,847314,841127";
                Print.logInfo("dataKey: " + dataKey + ", modemID: " + modemID + ", imeiNum: " + imeiNum + ", serial: " + serial);
                cs = SMSOutboundGateway.REPLACE(cs, SMSOutboundGateway.REPL_dataKey, dataKey);
                Print.logInfo("1..." + cs + "..." + device.getUniqueID());
                cs = SMSOutboundGateway.REPLACE(cs, SMSOutboundGateway.REPL_modemID, modemID);
                Print.logInfo("2..." + cs + "..." + device.getUniqueID());
                cs = SMSOutboundGateway.REPLACE(cs, SMSOutboundGateway.REPL_imei   , imeiNum);
                Print.logInfo("3..." + cs + "..." + device.getUniqueID());
                cs = SMSOutboundGateway.REPLACE(cs, SMSOutboundGateway.REPL_serial , serial );
                Print.logInfo("4..." + cs + "..." + device.getUniqueID());
                Print.logInfo(".....constuyendo comando para: " + device.getUniqueID());

                if (hardcode_devices.contains(device.getUniqueID())) {
                  if (cs.contains("Disable1")) {
                    Print.logInfo("cambiando de Disable1 a Enable1");
                    cs = StringTools.replace(cs, "Disable1", "Enable1");
                  } else if (cs.contains("Enable1")) {
                    Print.logInfo("cambiando de Enable1 a Disable1");
                    cs = StringTools.replace(cs, "Enable1", "Disable1");
                  } else {
                  }
                } else {
                }

                cs = StringTools.replace(cs, "SUNTECHST910", device.getUniqueID());
                cs = StringTools.replace(cs, "SUNTECHST600R", device.getUniqueID());
                cs = StringTools.replace(cs, "SUNTECHST330V148", device.getUniqueID());
                cs = StringTools.replace(cs, "SUNTECHST330", device.getUniqueID());
                cs = StringTools.replace(cs, "SUNTECHST300KE", device.getUniqueID());
                cs = StringTools.replace(cs, "SKYPATROL", device.getUniqueID());
                cs = StringTools.replace(cs, "SUNTECH", device.getUniqueID());
                cs = StringTools.replace(cs, "ENFORA8750", device.getUniqueID());
                cs = StringTools.replace(cs, "TK10X", device.getUniqueID());
                Print.logInfo(cs);
            }
            // return command string
            Print.logInfo("................................5");
            Print.logInfo(cs);
            return cs;
        }
        public boolean hasCommandArgs() {
            //return (this.cmdStr != null)? (this.cmdStr.indexOf("${arg") >= 0) : false;
            return this.hasArgs;
        }
        public int getArgCount() {
            if (!this.hasArgs) {
                return 0;
            } else
            if (this.argMap != null) {
                return this.argMap.size();
            } else {
                return 1;
            }
        }
        public CommandArg getCommandArg(int argNdx) {
            if (this.hasArgs && (this.argMap != null)) {
                return this.argMap.getValue(argNdx);
            } else {
                return null;
            }
        }
        public CommandProtocol getCommandProtocol() {
            return this.getCommandProtocol(null);
        }
        public CommandProtocol getCommandProtocol(CommandProtocol dftProto) {
            if (!StringTools.isBlank(this.cmdProto)) {
                // may return null if cmdProto is not one of the valid values
                return DCServerConfig.getCommandProtocol(this.cmdProto);
            } else {
                return dftProto;
            }
        }
        public boolean isCommandProtocolSMS() {
            CommandProtocol proto = this.getCommandProtocol(null);
            return ((proto != null) && proto.isSMS());
        }
        public String getCommandProtocolHandler() {
            return this.protoHandlr; // may be null
        }
        public boolean getExpectAck() {
            return this.expectAck;
        }
        public int getStatusCode() {
            return this.cmdStCode;
        }
    }

    public static class CommandArg
    { // static because the 'Args' are initialized before the 'Command' is
        private Command command   = null;   // The command that owns this arg
        private String  name      = null;   // This argument name
        private String  desc      = null;   // This argument description
        private boolean readOnly  = false;
        private String  resKey    = null;
        private String  dftVal    = "";
        private int     lenDisp   = 70;
        private int     lenMax    = 500;
        public CommandArg(String name, String desc, boolean readOnly, String resKey, String dftVal) {
            this.name      = StringTools.trim(name);
            this.desc      = StringTools.trim(desc);
            this.readOnly  = readOnly;
            this.resKey    = !StringTools.isBlank(resKey)? resKey : null;
            this.dftVal    = StringTools.trim(dftVal);
        }
        public String getName() {
            return this.name;
        }
        public String getDescription() {
            return this.desc;
        }
        public boolean isReadOnly() {
            return this.readOnly;
        }
        public void setCommand(Command cmd) {
            this.command = cmd;
        }
        public Command getCommand() {
            return this.command;
        }
        public String getResourceName() {
            return this.resKey;
            // ie. "DCServerConfig.enfora.DriverMessage.arg"
            /*
            StringBuffer sb = new StringBuffer();
            sb.append("DCServerConfig.");
            sb.append(cmd.getDCServerConfig().getName());
            sb.append(".");
            sb.append(cmd.getName());
            sb.append(".");
            sb.append(this.getName());
            return sb.toString();
            */
        }
        public String getDefaultValue() {
            return this.dftVal;
        }
        public void setLength(int dispLen, int maxLen) {
            this.lenDisp = (dispLen > 0)? dispLen : 70;
            this.lenMax  = (maxLen  > 0)? maxLen  : (this.lenDisp * 2);
        }
        public int getDisplayLength() {
            return this.lenDisp;
        }
        public int getMaximumLength() {
            return this.lenMax;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** FuelLevelProfile class
    **/
    public static class FuelLevelProfile
    {
        // 0,0|1,1
        private double evtLevel = 0.0;
        private double actLevel = 0.0;
        public FuelLevelProfile(String lvl) {
            if (!StringTools.isBlank(lvl)) {
                String L[] = StringTools.split(lvl,',');
                if (ListTools.size(L) >= 2) {
                    this.evtLevel = StringTools.parseDouble(L[0],-1.0);
                    this.actLevel = StringTools.parseDouble(L[1],-1.0);
                    if ((this.evtLevel < 0.0) || (this.evtLevel > 1.0) ||
                        (this.actLevel < 0.0) || (this.actLevel > 1.0)   ) {
                        Print.logError("Invalid FuelLevelProfile value: " + lvl);
                        this.evtLevel = 0.0;
                        this.actLevel = 0.0;
                    }
                }
            }
        }
        public FuelLevelProfile(double evLvl, double acLvl) {
            this.evtLevel = evLvl;
            this.actLevel = acLvl;
        }
        public double getEventLevel() {
            return this.evtLevel;
        }
        public double getActualLevel() {
            return this.actLevel;
        }
    }

    /**
    *** Parse the FuelLevelProfile String
    *** @param profile  The string containing the fuel-level profile
    *** @return An array of FuelLevelProfile entries
    **/
    public static FuelLevelProfile[] ParseFuelLevelProfile(String profile)
    {
        String p[] = StringTools.split(profile,'|');
        FuelLevelProfile flp[] = new FuelLevelProfile[p.length];
        for (int i = 0; i < p.length; i++) {
            flp[i] = new FuelLevelProfile(p[i]);
        }
        return flp;
    }

    /**
    *** Adjust the event fuel-level based on the specified profile
    *** @param fuelLevel  The event fuel-level to adjust
    *** @param flp        The FuelLevelProfile array used to adjust the fuel-level
    *** @return The adjusted fuel-level
    **/
    public static double adjustFuelLevelProfile(double fuelLevel, FuelLevelProfile flp[])
    {
        double FL = fuelLevel;
        if (!ListTools.isEmpty(flp)) {
            FuelLevelProfile hi = null;
            FuelLevelProfile lo = null;
            for (int i = 0; i < flp.length; i++) {
                if (flp[i].getEventLevel() == FL) {
                    hi = flp[i];
                    lo = flp[i];
                    break;
                } else
                if (flp[i].getEventLevel() >= FL) {
                    hi = flp[i];
                    lo = (i > 0)? flp[i - 1] : null;
                    break;
                }
            }
            if ((hi != null) && (lo != null)) {
                double evHi = hi.getEventLevel();
                double evLo = lo.getEventLevel();
                double evD  = (FL - evLo) / (evHi - evLo);
                double acHi = hi.getActualLevel();
                double acLo = lo.getActualLevel();
                FL = acLo + (evD * (acHi - acLo));
            }
        }
        return FL;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String                          dcName                  = "";
    private String                          dcDesc                  = "";

    private String                          uniquePrefix[]          = null;

    private boolean                         useSSL                  = false;
    private OrderedMap<Integer,InetAddress> tcpPortMap              = null;
    private OrderedMap<Integer,InetAddress> udpPortMap              = null;

    private int                             startStopCodes[]        = null;
    private boolean                         startStopCodes_init     = false;

    private boolean                         customCodeEnabled       = true;
    private Map<Object,EventCode>           customCodeMap           = new HashMap<Object,EventCode>();

    private String                          commandHost             = null;
    private int                             commandPort             = 0;
    private CommandProtocol                 commandProtocol         = null;

    private long                            attrFlags               = F_NONE;

    private Map<String,RTProperties>        rtPropsMap              = new OrderedMap<String,RTProperties>();

    private String                          commandsAclName         = null;
    private AclEntry.AccessLevel            commandsAccessLevelDft  = null;

    private OrderedMap<String,Command>      commandMap              = null;

    /**
    *** Blank Constructor
    **/
    public DCServerConfig()
    {
        this.getDefaultProperties();
        this.setName("unregistered");
        this.setDescription("Unregistered DCS");
        this.setAttributeFlags(F_NONE);
        this.setUseSSL(false);
        this.setTcpPorts(null, null, false);
        this.setUdpPorts(null, null, false);
        this.setCommandDispatcherPort(0);
        this.setUniquePrefix(null);
        this._postInit();
    }

    /**
    *** Constructor
    **/
    public DCServerConfig(String name, String desc, int tcpPorts[], int udpPorts[], int commandPort, long flags, String... uniqPfx)
    {
        this.getDefaultProperties();
        this.setName(name);
        this.setDescription(desc);
        this.setAttributeFlags(flags);
        this.setUseSSL(false);
        this.setTcpPorts(null, tcpPorts, true);
        this.setUdpPorts(null, udpPorts, true);
        this.setCommandDispatcherPort(commandPort, true);
        this.setUniquePrefix(uniqPfx);
        this._postInit();
    }

    private void _postInit()
    {
        // etc.
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the server name/id
    **/
    protected void setName(String n)
    {
        this.dcName = StringTools.trim(n);
    }

    /**
    *** Gets the server name/id
    **/
    public String getName()
    {
        return this.dcName;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the server description
    **/
    public void setDescription(String d)
    {
        this.dcDesc = StringTools.trim(d);
    }

    /**
    *** Gets the server description
    **/
    public String getDescription()
    {
        return this.dcDesc;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the server attribute flags
    **/
    public void setAttributeFlags(long f)
    {
        this.attrFlags = f;
    }

    /**
    *** Gets the server attribute flags
    **/
    public long getAttributeFlags()
    {
        return this.attrFlags;
    }

    /**
    *** Returns true if the indicate mask is non-zero
    **/
    public boolean isAttributeFlag(long mask)
    {
        return ((this.getAttributeFlags() & mask) != 0L);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets an array of server ports from the specified runtime keys.
    *** (first check command-line, then config file, then default) 
    *** @param name The server name
    *** @param rtPropKey  The runtime key names
    *** @param dft  The default array of server ports if not defined otherwise
    *** @return The array of server ports
    **/
    private int[] _getServerPorts(
        String cmdLineKey[],
        String rtPropKey[], 
        int dft[])
    {
        String portStr[] = null;

        /* check command-line override first */
        RTProperties cmdLineProps = RTConfig.getCommandLineProperties();
        if ((cmdLineProps != null) && cmdLineProps.hasProperty(cmdLineKey)) {
            portStr = cmdLineProps.getStringArray(cmdLineKey, null);
        }

        /* check runtime config override */
        if (ListTools.isEmpty(portStr)) {
            String ak[] = this.normalizeKeys(rtPropKey); // "tcpPort" ==> "enfora.tcpPort"
            if (!this.hasProperty(ak,false)) { // exclude 'defaults'
                // no override defined
                return dft;
            }
            portStr = this.getStringArrayProperty(ak, null);
            if (ListTools.isEmpty(portStr)) {
                // ports explicitly removed
                //Print.logInfo(name + ": Returning 'null' ports");
                return null;
            }
        }

        /* parse/return port numbers */
        int p = 0;
        int srvPorts[] = new int[portStr.length];
        for (int i = 0; i < portStr.length; i++) {
            int port = StringTools.parseInt(portStr[i], 0);
            if (ServerSocketThread.isValidPort(port)) {
                srvPorts[p++] = port;
            }
        }
        if (p < srvPorts.length) {
            // list contains invalid port numbers
            int newPorts[] = new int[p];
            System.arraycopy(srvPorts, 0, newPorts, 0, p);
            srvPorts = newPorts;
        }
        if (!ListTools.isEmpty(srvPorts)) {
            //Print.logInfo(name + ": Returning server ports: " + StringTools.join(srvPorts,","));
            return srvPorts;
        } else {
            //Print.logInfo(name + ": Returning 'null' ports");
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Sets whether to use SSL on TCP connections
    **/
    public void setUseSSL(boolean useSSL)
    {
        this.useSSL = useSSL;
    }

    /**
    *** Gets whether to use SSL on TCP connections
    **/
    public boolean getUseSSL()
    {
        return this.useSSL;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the default TCP port for this server
    **/
    public void setTcpPorts(InetAddress bindAddr, int tcp[], boolean checkRTP)
    {
        if (checkRTP) {
            tcp = this._getServerPorts(
                DCServerFactory.ARG_tcpPort,
                DCServerFactory.CONFIG_tcpPort(this.getName()), 
                tcp);
        }
        if (!ListTools.isEmpty(tcp)) {
            if (this.tcpPortMap == null) { this.tcpPortMap = new OrderedMap<Integer,InetAddress>(); }
            for (int i = 0; i < tcp.length; i++) {
                Integer p = new Integer(tcp[i]);
                if (this.tcpPortMap.containsKey(p)) {
                    Print.logWarn("TCP port already defined ["+this.getName()+"]: " + p);
                }
                this.tcpPortMap.put(p, bindAddr);
            }
        }
    }

    /**
    *** Get TCP Port bind address
    **/
    public InetAddress getTcpPortBindAddress(int port)
    {
        InetAddress bind = (this.tcpPortMap != null)? this.tcpPortMap.get(new Integer(port)) : null;
        return (bind != null)? bind : ServerSocketThread.getDefaultBindAddress();
    }

    /**
    *** Gets the default TCP port for this server
    **/
    public int[] getTcpPorts()
    {
        if (ListTools.isEmpty(this.tcpPortMap)) {
            return null;
        } else {
            int ports[] = new int[this.tcpPortMap.size()];
            for (int i = 0; i < ports.length; i++) {
                ports[i] = this.tcpPortMap.getKey(i).intValue();
            }
            return ports;
        }
    }

    /**
    *** Create TCP ServerSocketThread
    **/
    public ServerSocketThread createServerSocketThread_TCP(int port)
        throws SocketException, IOException
    {
        boolean useSSL = this.getUseSSL();
        return this.createServerSocketThread_TCP(port, useSSL);
    }

    /**
    *** Create TCP ServerSocketThread
    **/
    public ServerSocketThread createServerSocketThread_TCP(int port, boolean useSSL)
        throws SocketException, IOException
    {
        InetAddress bindAddr = this.getTcpPortBindAddress(port);
        return new ServerSocketThread(bindAddr, port, useSSL);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the default UDP port for this server
    **/
    public void setUdpPorts(InetAddress bindAddr, int udp[], boolean checkRTP)
    {
        if (checkRTP) {
            udp = this._getServerPorts(
                DCServerFactory.ARG_udpPort,
                DCServerFactory.CONFIG_udpPort(this.getName()), 
                udp);
        }
        if (!ListTools.isEmpty(udp)) {
            if (this.udpPortMap == null) { this.udpPortMap = new OrderedMap<Integer,InetAddress>(); }
            for (int i = 0; i < udp.length; i++) {
                Integer p = new Integer(udp[i]);
                if (this.udpPortMap.containsKey(p)) {
                    Print.logWarn("UDP port already defined ["+this.getName()+"]: " + p);
                }
                this.udpPortMap.put(p, bindAddr);
                //Print.logInfo("Setting UDP listener at " + StringTools.blankDefault(bindAddr,"<ALL>") + " : " + p);
            }
        }
    }
    
    /**
    *** Get UDP Port bind address
    **/
    public InetAddress getUdpPortBindAddress(int port)
    {
        InetAddress bind = (this.udpPortMap != null)? this.udpPortMap.get(new Integer(port)) : null;
        return (bind != null)? bind : ServerSocketThread.getDefaultBindAddress();
    }

    /**
    *** Gets the default UDP port for this server
    **/
    public int[] getUdpPorts()
    {
        if (ListTools.isEmpty(this.udpPortMap)) {
            return null;
        } else {
            int ports[] = new int[this.udpPortMap.size()];
            for (int i = 0; i < ports.length; i++) {
                ports[i] = this.udpPortMap.getKey(i).intValue();
            }
            return ports;
        }
    }

    /**
    *** Create UDP ServerSocketThread
    **/
    public ServerSocketThread createServerSocketThread_UDP(int port)
        throws SocketException, IOException
    {
        InetAddress bindAddr = this.getUdpPortBindAddress(port);
        return new ServerSocketThread(ServerSocketThread.createDatagramSocket(bindAddr, port));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final int STARTSTOP_STARTSTOP[] = { StatusCodes.STATUS_MOTION_START, StatusCodes.STATUS_MOTION_STOP  };
    public static final int STARTSTOP_IGNITION[]  = { StatusCodes.STATUS_IGNITION_ON , StatusCodes.STATUS_IGNITION_OFF };
    public static final int STARTSTOP_ENGINE[]    = { StatusCodes.STATUS_ENGINE_START, StatusCodes.STATUS_ENGINE_STOP  };
    public static final int STARTSTOP_PARK[]      = { StatusCodes.STATUS_UNPARKED    , StatusCodes.STATUS_PARKED       };

    /**
    *** Sets the start and stop status codes.
    *** @param ssKey   The start status code key ("ignition", "parked", "startStop")
    **/
    public void setStartStopStatusCodes(String ssKey)
    {
        // <Property key="startStopStatusCodes">default</Property>
        // <Property key="startStopStatusCodes">ignition</Property>
        // <Property key="startStopStatusCodes">engine</Property>
        // <Property key="startStopStatusCodes">parked</Property>
        // <Property key="startStopStatusCodes">startstop</Property>
        String ssk = StringTools.trim(ssKey).toLowerCase();
        if (StringTools.isBlank(ssk)) {
            this.startStopCodes = null;
            return;
        } else
        if (ssk.equals("default")) {
            if (this.getStartStopSupported(false)) {
                this.startStopCodes = STARTSTOP_STARTSTOP;
            } else {
                this.startStopCodes = null;
            }
            return;
        } else
        if (ssk.equals("ign") || ssk.equals("ignition")) {
            this.startStopCodes = STARTSTOP_IGNITION;
            return;
        } else
        if (ssk.equals("eng") || ssk.equals("engine")) {
            this.startStopCodes = STARTSTOP_ENGINE;
            return;
        } else
        if (ssk.equals("park") || ssk.equals("parked")) {
            this.startStopCodes = STARTSTOP_PARK;
            return;
        } else
        if (ssk.equals("ss") || ssk.equals("startstop")) {
            this.startStopCodes = STARTSTOP_STARTSTOP;
            return;
        } else
        if (ssk.indexOf(',') > 0) {
            String sc[] = StringTools.split(ssk,',');
            if (ListTools.size(sc) >= 2) {
                int startSC = StringTools.parseInt(sc[0],0);
                int stopSC  = StringTools.parseInt(sc[1],0);
                if ((startSC > 0) && (stopSC > 0)) {
                    this.startStopCodes = new int[] { startSC, stopSC };
                    return;
                }
            }
        }
        Print.logWarn("Invalid Start/Stop Status Code specification: " + ssKey);
        //Print.logStackTrace("here");
        this.startStopCodes = null;
    }

    /**
    *** Sets the start and stop status codes.
    *** @param startCode  The start status code
    *** @param stopCode   The stop status code
    **/
    public void setStartStopStatusCodes(int startCode, int stopCode)
    {
        if ((startCode > 0) && (stopCode > 0)) {
            this.startStopCodes = new int[] { startCode, stopCode };
        } else {
            this.startStopCodes = null;
        }
    }

    /**
    *** Sets the start and stop status codes.
    *** @param startStopCodes  An array containing 2 elements.  The first element
    ***                        is the start status code, and the second element
    ***                        is the stop status code.
    **/
    public void setStartStopStatusCodes(int startStopCodes[])
    {
        if (ListTools.size(startStopCodes) >= 2) {
            this.setStartStopStatusCodes(startStopCodes[0], startStopCodes[1]);
        } else {
            this.startStopCodes = null;
        }
    }

    /**
    *** Gets the start/stop status codes, or null if no start/stop status codes
    *** have been defined.  If non-null, element '0' will always contain the 
    *** start code, and element '1' will contain the stop code.
    *** @return The start/stop status codes, or null if no codes defined.
    **/
    public int[] getStartStopStatusCodes()
    {
        if (!this.startStopCodes_init) {
            String ssk = this.getStringProperty(DCServerFactory.CONFIG_startStopStatusCodes(this.getName()), null);
            this.setStartStopStatusCodes(ssk);
            this.startStopCodes_init = true;
        }
        return this.startStopCodes;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Start Command Listener
    *** @param port   The listen port
    *** @param handler  The command handler class
    **/
    public static ServerSocketThread startCommandHandler(int port, Class handler)
        throws Throwable
    {
        ServerSocketThread sst = null;

        /* create server socket */
        try {
            sst = new ServerSocketThread(port);
        } catch (Throwable t) { // trap any server exception
            Print.logException("ServerSocket error", t);
            throw t;
        }

        /* initialize */
        sst.setTextPackets(true);
        sst.setBackspaceChar(null); // no backspaces allowed
        sst.setLineTerminatorChar(new int[] { '\r', '\n' });
        sst.setIgnoreChar(null);
        sst.setMaximumPacketLength(1024);       // safety net
        sst.setMinimumPacketLength(1);
        sst.setIdleTimeout(1000L);              // time between packets
        sst.setPacketTimeout(1000L);            // time from start of packet to packet completion
        sst.setSessionTimeout(5000L);           // time for entire session
        sst.setLingerTimeoutSec(5);
        sst.setTerminateOnTimeout(true);
        sst.setClientPacketHandlerClass(handler);

        /* start thread */
        DCServerConfig.startServerSocketThread(sst,"Command");
        return sst;

    }

    /**
    *** Start ServerSocketThread
    *** @param sst    The ServerSocketThread to start
    *** @param type   The short 'type' name of the socket listener 
    **/
    public static void startServerSocketThread(ServerSocketThread sst, String type)
    {
        if (sst != null) {
            String m        = StringTools.trim(type);
            int    port     = sst.getLocalPort();
            String bindAddr = StringTools.blankDefault(sst.getBindAddress(), "(ALL)");
            if (bindAddr.startsWith("/")) { bindAddr = bindAddr.substring(1); }
            if (sst.getServerSocket() != null) {
                // TCP
                long tmo = sst.getSessionTimeout();
                Print.logInfo("Starting "+m+" Listener (TCP) - " +port+ " [" +bindAddr+ "] timeout="+tmo+"ms ...");
            } else
            if (sst.getDatagramSocket() != null) {
                // UDP
                Print.logInfo("Starting "+m+" Listener (UDP) - " +port+ " [" +bindAddr+ "] ...");
            } else {
                Print.logStackTrace("ServerSocketThread is invalid!");
            }
            sst.start();
        } else {
            Print.logStackTrace("ServerSocketThread is null!");
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns an array of all TCP/UDP 'listen' ports
    *** @return An array of all TCP/UDP 'listen' ports
    **/
    public int[] getListenPorts()
    {
        if (ListTools.isEmpty(this.tcpPortMap)) {
            return this.getUdpPorts(); // may still be null/empty
        } else
        if (ListTools.isEmpty(this.udpPortMap)) {
            return this.getTcpPorts(); // may still be null/empty
        } else {
            java.util.List<Integer> portList = new Vector<Integer>();
            int tcpPorts[] = this.getTcpPorts();
            for (int t = 0; t < tcpPorts.length; t++) {
                Integer tcp = new Integer(tcpPorts[t]);
                portList.add(tcp); 
            }
            int udpPorts[] = this.getUdpPorts();
            for (int u = 0; u < udpPorts.length; u++) {
                Integer udp = new Integer(udpPorts[u]);
                if (!portList.contains(udp)) {
                    portList.add(udp);
                }
            }
            int ports[] = new int[portList.size()];
            for (int p = 0; p < portList.size(); p++) {
                ports[p] = portList.get(p).intValue();
            }
            return ports;
        }
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Load device record from unique-id (not yet used/tested)
    *** @param modemID The unique modem ID (IMEI, ESN, etc)
    *** @return The Device record
    **/
    public Device loadDeviceUniqueID(String modemID)
    {
        return DCServerFactory.loadDeviceByPrefixedModemID(this.getUniquePrefix(), modemID);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the named server is defined
    **/
    public boolean serverJarExists()
    {
        return DCServerFactory.serverJarExists(this.getName());
    }
    
    /**
    *** Returns true if this DCS requires a Jar file
    **/
    public boolean isJarOptional()
    {
        return this.isAttributeFlag(F_JAR_OPTIONAL);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the command protocol to use when communicating with remote devices
    *** @param proto  The CommandProtocol
    **/
    public void setCommandProtocol(String proto)
    {
        this.commandProtocol = DCServerConfig.getCommandProtocol(proto);
    }

    /**
    *** Sets the command protocol to use when communicating with remote devices
    *** @param proto  The CommandProtocol
    **/
    public void setCommandProtocol(CommandProtocol proto)
    {
        this.commandProtocol = proto;
    }

    /**
    *** Gets the command protocol to use when communicating with remote devices
    *** @return The Command Protocol
    **/
    public CommandProtocol getCommandProtocol()
    {
        return (this.commandProtocol != null)? this.commandProtocol : CommandProtocol.UDP;
    }

    /**
    *** Gets the "Client Command Port" 
    *** @param dft  The default Client Command Port
    *** @return The Client Command Port
    **/
    public int getClientCommandPort_udp(int dft)
    {
        return this.getIntProperty(DCServerFactory.CONFIG_clientCommandPort_udp(this.getName()), dft);
    }

    /**
    *** Gets the "Client Command Port" 
    *** @param dft  The default Client Command Port
    *** @return The Client Command Port
    **/
    public int getClientCommandPort_tcp(int dft)
    {
        return this.getIntProperty(DCServerFactory.CONFIG_clientCommandPort_tcp(this.getName()), dft);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the "ACK Response Port" 
    *** @param dft  The ACK response port
    *** @return The ack response port
    **/
    public int getAckResponsePort(int dft)
    {
        return this.getIntProperty(DCServerFactory.CONFIG_ackResponsePort(this.getName()), dft);
    }

    /**
    *** Gets the "ACK Response Port" 
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The ACK response port
    *** @return The ack response port
    **/
    public static int getAckResponsePort(DCServerConfig dcsc, int dft)
    {
        return (dcsc != null)? dcsc.getAckResponsePort(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "TCP idle timeout" 
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getTcpIdleTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_tcpIdleTimeoutMS(this.getName()), dft);
    }

    /**
    *** Gets the "TCP packet timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getTcpPacketTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_tcpPacketTimeoutMS(this.getName()), dft);
    }

    /**
    *** Gets the "TCP session timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getTcpSessionTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_tcpSessionTimeoutMS(this.getName()), dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "UDP idle timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getUdpIdleTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_udpIdleTimeoutMS(this.getName()), dft);
    }

    /**
    *** Gets the "UDP packet timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getUdpPacketTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_udpPacketTimeoutMS(this.getName()), dft);
    }

    /**
    *** Gets the "UDP session timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getUdpSessionTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_udpSessionTimeoutMS(this.getName()), dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the array of allowed UniqueID prefixes
    *** @param dftPfx  The default list of prefixes
    *** @return The array of allowed UniqueID prefixes
    **/
    public String[] getUniquePrefix(String dftPfx[])
    {
        if (ListTools.isEmpty(this.uniquePrefix)) {
            // set non-empty default
            this.uniquePrefix = !ListTools.isEmpty(dftPfx)? dftPfx : new String[] { "" };
        }
        return this.uniquePrefix;
    }

    /**
    *** Gets the array of allowed UniqueID prefixes
    *** @return The array of allowed UniqueID prefixes
    **/
    public String[] getUniquePrefix()
    {
        return this.getUniquePrefix(null);
    }

    /**
    *** Sets the array of allowed UniqueID prefixes
    *** @param pfx  The default UniqueID prefixes
    **/
    public void setUniquePrefix(String pfx[])
    {
        if (!ListTools.isEmpty(pfx)) {
            for (int i = 0; i < pfx.length; i++) {
                String p = pfx[i].trim();
                if (p.equals("<blank>") || p.equals("*")) { 
                    p = ""; 
                } else
                if (p.endsWith("*")) {
                    p = p.substring(0, p.length() - 1);
                }
                pfx[i] = p;
            }
            this.uniquePrefix = pfx;
        } else {
            this.uniquePrefix = new String[] { "" };;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Minimum Moved Meters"
    *** @param dft  The default minimum distance
    *** @return The Minimum Moved Meters
    **/
    public double getMinimumMovedMeters(double dft)
    {
        return this.getDoubleProperty(DCServerFactory.CONFIG_minimumMovedMeters(this.getName()), dft);
    }

    /**
    *** Gets the "Minimum Moved Meters"
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default minimum distance
    *** @return The Minimum Moved Meters
    **/
    public static double getMinimumMovedMeters(DCServerConfig dcsc, double dft)
    {
        return (dcsc != null)? dcsc.getMinimumMovedMeters(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Minimum Speed KPH"
    *** @param dft  The default minimum speed
    *** @return The Minimum Speed KPH
    **/
    public double getMinimumSpeedKPH(double dft)
    {
        return this.getDoubleProperty(DCServerFactory.CONFIG_minimumSpeedKPH(this.getName()), dft);
    }

    /**
    *** Gets the "Minimum Speed KPH"
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default minimum speed
    *** @return The Minimum Speed KPH
    **/
    public static double getMinimumSpeedKPH(DCServerConfig dcsc, double dft)
    {
        return (dcsc != null)? dcsc.getMinimumSpeedKPH(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Estimate Odometer" flag
    *** @param dft  The default estimate odometer flag
    *** @return The Estimate Odometer flag
    **/
    public boolean getEstimateOdometer(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_estimateOdometer(this.getName()), dft);
    }

    /**
    *** Gets the "Estimate Odometer" flag
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default estimate odometer flag
    *** @return The Estimate Odometer flag
    **/
    public static boolean getEstimateOdometer(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getEstimateOdometer(dft) : dft;
    }
 
    // ------------------------------------------------------------------------

    /**
    *** Gets the "Ignore Device Odometer" flag
    *** @param dft  The default ignore device odometer flag
    *** @return The Ignore Device Odometer flag
    **/
    public boolean getIgnoreDeviceOdometer(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_ignoreDeviceOdometer(this.getName()), dft);
    }

    /**
    *** Gets the "Ignore Device Odometer" flag
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default ignore device odometer flag
    *** @return The ignore device Odometer flag
    **/
    public static boolean getIgnoreDeviceOdometer(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getIgnoreDeviceOdometer(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Simulate Geozones"
    *** @param dft  The default Simulate Geozones state
    *** @return The Simulate Geozones
    **/
    public boolean getSimulateGeozones(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_simulateGeozones(this.getName()), dft);
    }

    /**
    *** Gets the "Simulate Geozones"
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default Simulate Geozones state
    *** @return The Simulate Geozones
    **/
    public static boolean getSimulateGeozones(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getSimulateGeozones(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Maximum HDOP"
    *** @param dft  The default maximum HDOP
    *** @return The Maximum HDOP
    **/
    public double getMaximumHDOP(double dft)
    {
        return this.getDoubleProperty(DCServerFactory.CONFIG_maximumHDOP(this.getName()), dft);
    }

    /**
    *** Gets the "Maximum HDOP"
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default maximum HDOP
    *** @return The Maximum HDOP
    **/
    public static double getMaximumHDOP(DCServerConfig dcsc, double dft)
    {
        return (dcsc != null)? dcsc.getMaximumHDOP(dft) : dft;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Gets the "Check Last Odometer" config 
    *** @param dft  The default "Check Last Odometer" state
    *** @return The "Check Last Odometer" state
    **/
    public boolean getCheckLastOdometer(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_checkLastOdometer(this.getName()), dft);
    }

    /**
    *** Gets the "Check Last Odometer" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Check Last Odometer" state
    *** @return The "Check Last Odometer" state
    **/
    public static boolean getCheckLastOdometer(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getCheckLastOdometer(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Save Raw Data Packet" config
    *** @param dft  The default "Save Raw Data Packet" state
    *** @return The "Save Raw Data Packet" state
    **/
    public boolean getSaveRawDataPackets(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_saveRawDataPackets(this.getName()), dft);
    }

    /**
    *** Gets the "Save Raw Data Packet" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Save Raw Data Packet" state
    *** @return The "Save Raw Data Packet" state
    **/
    public static boolean getSaveRawDataPackets(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getSaveRawDataPackets(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Start/Stop StatusCode supported" config
    *** @param dft  The default "Start/Stop StatusCode supported" state
    *** @return The "Start/Stop StatusCode supported" state
    **/
    public boolean getStartStopSupported(boolean dft)
    {
        String n = this.getName();
        if (n.equals(DCServerFactory.OPENDMTP_NAME)) {
            return true;
        } else {
            return this.getBooleanProperty(DCServerFactory.CONFIG_startStopSupported(this.getName()), dft);
        }
    }

    /**
    *** Gets the "Start/Stop StatusCode supported" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Start/Stop StatusCode supported" state
    *** @return The "Start/Stop StatusCode supported" state
    **/
    public static boolean getStartStopSupported(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getStartStopSupported(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Status Location/InMotion Translation" config
    *** @param dft  The default "Status Location/InMotion Translation" state
    *** @return The "Status Location/InMotion Translation" state
    **/
    public boolean getStatusLocationInMotion(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_statusLocationInMotion(this.getName()), dft);
    }

    /**
    *** Gets the "Status Location/InMotion Translation" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Status Location/InMotion Translation" state
    *** @return The "Status Location/InMotion Translation" state
    **/
    public static boolean getStatusLocationInMotion(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getStatusLocationInMotion(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Ignore Invalid GPS Location Flag" config
    *** @param dft  The default "Ignore Invalid GPS Location Flag" state
    *** @return The "Ignore Invalid GPS Location Flag" state
    **/
    public boolean getIgnoreInvalidGPSFlag(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_ignoreInvalidGPSFlag(this.getName()), dft);
    }

    /**
    *** Gets the "Ignore Invalid GPS Location Flag" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Ignore Invalid GPS Location Flag" state
    *** @return The "Ignore Invalid GPS Location Flag" state
    **/
    public static boolean getIgnoreInvalidGPSFlag(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getIgnoreInvalidGPSFlag(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Use Last Valid GPS Location" config
    *** @param dft  The default "Use Last Valid GPS Location" state
    *** @return The "Use Last Valid GPS Location" state
    **/
    public boolean getUseLastValidGPSLocation(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_useLastValidGPSLocation(this.getName()), dft);
    }

    /**
    *** Gets the "Use Last Valid GPS Location" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Use Last Valid GPS Location" state
    *** @return The "Use Last Valid GPS Location" state
    **/
    public static boolean getUseLastValidGPSLocation(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getUseLastValidGPSLocation(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Save Session Statistics" config
    *** @param dft  The default "Save Session Statistics" state
    *** @return The "Save Session Statistics" state
    **/
    public boolean getSaveSessionStatistics(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_saveSessionStatistics(this.getName()), dft);
    }

    /**
    *** Gets the "Save Session Statistics" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Save Session Statistics" state
    *** @return The "Save Session Statistics" state
    **/
    public static boolean getSaveSessionStatistics(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getSaveSessionStatistics(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Battery Level Range" config
    *** @param dft  The default "Battery Level Range" min/max values
    *** @return The "Battery Level Range" min/max values
    **/
    public double[] getBatteryLevelRange(double dft[])
    {
        String propKeys[] = DCServerFactory.CONFIG_batteryLevelRange(this.getName());

        /* get property string */
        String blrS = this.getStringProperty(propKeys, null);
        if (StringTools.isBlank(blrS)) {
            return dft;
        }

        /* parse */
        double blr[] = StringTools.parseDouble(StringTools.split(blrS,','),0.0);
        double min   = 0.0;
        double max   = 0.0;
        if (ListTools.size(blr) <= 0) {
            min = (ListTools.size(dft) > 0)? dft[0] : 11.4;
            max = (ListTools.size(dft) > 1)? dft[1] : 12.8;
        } else
        if (ListTools.size(blr) == 1) {
            // <Property key="...">12.0</Property>
            min = blr[0];
            max = blr[0];
        } else {
            min = blr[0];
            max = blr[1];
        }

        /* adjust */
        if (min < 0.0) { min = 0.0; }
        if (max < 0.0) { max = 0.0; }
        if (max <= min) {
            // <Property>12.8,11.4</Property>
            double tmp = max;
            max = min;
            min = tmp;
        }

        /* return */
        return new double[] { min, max };

    }

    /**
    *** Gets the "Battery Level Range" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Battery Level Range" min/max values
    *** @return The "Battery Level Range" min/max values
    **/
    public static double[] getBatteryLevelRange(DCServerConfig dcsc, double dft[])
    {
        return (dcsc != null)? dcsc.getBatteryLevelRange(dft) : dft;
    }

    /**
    *** Calculates/returns the battery level based on the specified voltage range
    *** @param voltage  The current battery voltage
    *** @param range    The allowed voltage range
    *** @return The battery level percent
    **/
    public static double CalculateBatteryLevel(double voltage, double range[])
    {

        /* no specified voltage? */
        if (voltage <= 0.0) {
            return 0.0;
        }

        /* no specified range? */
        int rangeSize = ListTools.size(range);
        if ((rangeSize < 1) || (voltage <= range[0])) {
            return 0.0;
        } else
        if ((rangeSize < 2) || (voltage >= range[1])) {
            return 1.0;
        }

        /* get percent */
        // Note: the above filters out (range[1] == range[0])
        double percent = (voltage - range[0]) / (range[1] - range[0]);
        if (percent < 0.0) {
            return 0.0;
        } else
        if (percent > 1.0) {
            return 1.0;
        } else {
            return percent;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Simulate Geozones" mask
    *** @param dft  The default Simulate Geozones mask
    *** @return The Simulate Geozones mask
    **/
    public long getSimulateDigitalInputs(long dft)
    {
        String maskStr = this.getStringProperty(DCServerFactory.CONFIG_simulateDigitalInputs(this.getName()), null);
        if (StringTools.isBlank(maskStr)) {
            // not specified (or blank)
            return dft;
        } else
        if (maskStr.equalsIgnoreCase("default")) {
            // explicit "default"
            return dft;
        } else
        if (maskStr.equalsIgnoreCase("true")) {
            // explicit "true"
            return 0xFFFFFFFFL;
        } else
        if (maskStr.equalsIgnoreCase("false")) {
            // explicit "false"
            return 0x00000000L;
        } else {
            // mask specified
            long mask = StringTools.parseLong(maskStr, -1L);
            return (mask >= 0L)? mask : dft;
        }
    }

    /**
    *** Gets the "Simulate Geozones" mask
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default Simulate Geozones mask
    *** @return The Simulate Geozones mask
    **/
    public static long getSimulateDigitalInputs(DCServerConfig dcsc, long dft)
    {
        return (dcsc != null)? dcsc.getSimulateDigitalInputs(dft) : dft;
    }

    /**
    *** Returns true if this device supports digital inputs
    *** @return True if this device supports digital inputs
    **/
    public boolean hasDigitalInputs()
    {
        return this.isAttributeFlag(F_HAS_INPUTS);
    }

    /**
    *** Returns true if this device supports digital outputs
    *** @return True if this device supports digital outputs
    **/
    public boolean hasDigitalOutputs()
    {
        return this.isAttributeFlag(F_HAS_OUTPUTS);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Convenience for converting the initial/final packet to a byte array.
    *** If the string begins with "0x" the the remain string is assumed to be hex
    *** @return The byte array
    **/
    public static byte[] convertToBytes(String s)
    {
        if (s == null) {
            return null;
        } else
        if (s.startsWith("0x")) {
            byte b[] = StringTools.parseHex(s,null);
            if (b != null) {
                return b;
            } else {
                return null;
            }
        } else {
            return StringTools.getBytes(s);
        }
    }

    /**
    *** Gets the "Initial Packet" byte array
    *** @param dft  The default "Initial Packet" byte array
    *** @return The "Initial Packet" byte array
    **/
    public byte[] getInitialPacket(byte[] dft)
    {
        String s = this.getStringProperty(DCServerFactory.CONFIG_initialPacket(this.getName()), null);
        if (s == null) {
            return dft;
        } else 
        if (s.startsWith("0x")) {
            return StringTools.parseHex(s,dft);
        } else {
            return s.getBytes();
        }
    }

    /**
    *** Gets the "Final Packet" byte array
    *** @param dft  The default "Final Packet" byte array
    *** @return The "Final Packet" byte array
    **/
    public byte[] getFinalPacket(byte[] dft)
    {
        String s = this.getStringProperty(DCServerFactory.CONFIG_finalPacket(this.getName()), null);
        if (s == null) {
            return dft;
        } else 
        if (s.startsWith("0x")) {
            return StringTools.parseHex(s,dft);
        } else {
            return s.getBytes();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the "Event Code Map Enable" config
    *** @param enabled  The "Event Code Map Enable" state
    **/
    public void setEventCodeEnabled(boolean enabled)
    {
        this.customCodeEnabled = enabled;
        //Print.logDebug("[" + this.getName() + "] EventCode translation enabled=" + this.customCodeEnabled);
    }

    /**
    *** Gets the "Event Code Map Enable" config
    *** @return The "Event Code Map Enable" state
    **/
    public boolean getEventCodeEnabled()
    {
        return this.customCodeEnabled;
    }
    
    /**
    *** Sets the EventCodeMap
    **/
    public void setEventCodeMap(Map<Object,EventCode> codeMap)
    {
        this.customCodeMap = (codeMap != null)? codeMap : new HashMap<Object,EventCode>();
    }
    
    /**
    *** Prints the EventCodeMap
    **/
    public boolean printEventCodeMap()
    {
        if (ListTools.isEmpty(this.customCodeMap)) {
            return false;
        }
        Vector<Object> keyList = new Vector<Object>(this.customCodeMap.keySet());
        int maxKeyLen = 0;
        for (Object keyObj : keyList) {
            int len = keyObj.toString().length();
            if (len > maxKeyLen) { maxKeyLen = len; }
        }
        ListTools.sort(keyList,null);
        for (Object keyObj : keyList) {
            StringBuffer sb = new StringBuffer();
            EventCode evc = this.customCodeMap.get(keyObj);
            int sc = evc.getStatusCode();
            sb.append(StringTools.rightAlign(keyObj.toString(),maxKeyLen));
            sb.append(" ==> ");
            sb.append(StatusCodes.ToString(evc.getStatusCode()));
            sb.append("\n");
            Print.sysPrint(sb.toString());
        }
        return true;
    }

    /**
    *** Returns the EventCode instance for the specified code
    *** @param code  The code
    *** @return The EventCode
    **/
    public EventCode getEventCode(int code)
    {
        if (!this.customCodeEnabled) {
            return null;
        } else {
            Object keyObj = new Integer(code);
            return this.customCodeMap.get(keyObj);
        }
    }

    /**
    *** Returns the EventCode instance for the specified code
    *** @param code  The code
    *** @return The EventCode
    **/
    public EventCode getEventCode(long code)
    {
        if (!this.customCodeEnabled) {
            return null;
        } else {
            Object keyObj = new Integer((int)code);
            return this.customCodeMap.get(keyObj);
        }
    }

    /**
    *** Returns the EventCode instance for the specified code
    *** @param code  The code
    *** @return The EventCode
    **/
    public EventCode getEventCode(String code)
    {
        if (!this.customCodeEnabled || (code == null)) {
            return null;
        } else {
            Object keyObj = StringTools.trim(code).toLowerCase();
            return this.customCodeMap.get(keyObj);
        }
    }

    /**
    *** Translates the specified device status code into a GTS status code
    *** @param code           The code to translate
    *** @param dftStatusCode  The default code returned if no translation is defined
    *** @return The translated GTS status code
    **/
    public int translateStatusCode(int code, int dftStatusCode)
    {
        EventCode sci = this.getEventCode(code);
        return (sci != null)? sci.getStatusCode() : dftStatusCode;
    }

    /**
    *** Translates the specified device status code into a GTS status code
    *** @param code           The code to translate
    *** @param dftStatusCode  The default code returned if no translation is defined
    *** @return The translated GTS status code
    **/
    public int translateStatusCode(String code, int dftStatusCode)
    {
        EventCode sci = this.getEventCode(code);
        return (sci != null)? sci.getStatusCode() : dftStatusCode;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the device command listen host (may be null to use default bind-address)
    *** @param cmdHost The device command listen host
    **/
    public void setCommandDispatcherHost(String cmdHost)
    {
        this.commandHost = cmdHost;
    }

    /**
    *** Gets the device command listen host
    *** @return The device command listen host
    **/
    public String getCommandDispatcherHost()
    {
        if (!StringTools.isBlank(this.commandHost)) {
            return this.commandHost;
        } else
        if (!StringTools.isBlank(DCServerFactory.BIND_ADDRESS)) {
            return DCServerFactory.BIND_ADDRESS;
        } else {
            // DCServer.DCSNAME.bindAddress
            String bindKey  = DCServerFactory.PROP_DCServer_ + this.getName() + "." + DCServerFactory.ATTR_bindAddress;
            String bindAddr = this.getStringProperty(bindKey, null);
            return !StringTools.isBlank(bindAddr)? bindAddr : "localhost";
        }
    }

    /**
    *** Sets the device command listen port
    *** @param cmdPort  The device command listen port
    *** @param checkRTP True to allow the RTConfig propertiesto override this value
    **/
    public void setCommandDispatcherPort(int cmdPort, boolean checkRTP)
    {
        if (checkRTP) {
            int port = 0;
            // First try command-line override
            RTProperties cmdLineProps = RTConfig.getCommandLineProperties();
            if ((cmdLineProps != null) && cmdLineProps.hasProperty(DCServerFactory.ARG_commandPort)) {
                port = cmdLineProps.getInt(DCServerFactory.ARG_commandPort, 0);
            }
            // then try standard runtime config override
            if (port <= 0) {
                port = this.getIntProperty(DCServerFactory.CONFIG_commandPort(this.getName()), 0);
            }
            // change port if overridden
            if (port > 0) {
                cmdPort = port;
            }
        }
        this.commandPort = cmdPort;
    }

    /**
    *** Sets the device command listen port
    *** @param cmdPort  The device command listen port
    **/
    public void setCommandDispatcherPort(int cmdPort)
    {
        this.setCommandDispatcherPort(cmdPort,false);
    }

    /**
    *** Gets the device command listen port (returns '0' if not supported)
    *** @return The device command listen port
    **/
    public int getCommandDispatcherPort()
    {
        return this.commandPort;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Commands Acl name
    *** @param aclName The Commands Acl name
    **/
    public void setCommandsAclName(String aclName, AclEntry.AccessLevel dft)
    {
        this.commandsAclName        = StringTools.trim(aclName);
        this.commandsAccessLevelDft = dft;
    }

    /**
    *** Gets the Commands Acl name
    *** @return The Commands Acl name
    **/
    public String getCommandsAclName()
    {
        return this.commandsAclName;
    }

    /**
    *** Gets the Commands Acl AccessLevel default
    *** @return The Commands Acl AccessLevel default
    **/
    public AclEntry.AccessLevel getCommandsAccessLevelDefault()
    {
        return this.commandsAccessLevelDft;
    }

    /**
    *** Returns True if the specified user has access to the named command
    **/
    public boolean userHasAccessToCommand(BasicPrivateLabel privLabel, User user, String commandName)
    {

        /* BasicPrivateLabel must be specified */
        if (privLabel == null) {
            return false;
        }

        /* get command */
        Command command = this.getCommand(commandName);
        if (command == null) {
            return false;
        }

        /* has access to commands */
        if (privLabel.hasWriteAccess(user, this.getCommandsAclName())) {
            return false;
        }

        /* has access to specific command? */
        if (privLabel.hasWriteAccess(user, command.getAclName())) {
            return false;
        }

        /* access granted */
        return true;

    }
    
    // ------------------------------------------------------------------------
    
    public void addCommand(
        String cmdName, String cmdDesc, 
        String cmdTypes[], 
        String cmdAclName, AclEntry.AccessLevel cmdAclDft,
        String cmdString, boolean hasArgs, Collection<DCServerConfig.CommandArg> cmdArgList,
        String cmdProto, boolean expectAck,
        int cmdSCode)
    {
        if (StringTools.isBlank(cmdName)) {
            Print.logError("Ignoreing blank command name");
        } else
        if ((this.commandMap != null) && this.commandMap.containsKey(cmdName)) {
            Print.logError("Command already defined: " + cmdName);
        } else {
            Command cmd = new Command(
                cmdName, cmdDesc, 
                cmdTypes, 
                cmdAclName, cmdAclDft,
                cmdString, hasArgs, cmdArgList,
                cmdProto, expectAck,
                cmdSCode);
            if (this.commandMap == null) {
                this.commandMap = new OrderedMap<String,Command>();
            }
            this.commandMap.put(cmdName, cmd);
        }
    }

    public Command getCommand(String name)
    {
        return (this.commandMap != null)? this.commandMap.get(name) : null;
    }

    /**
    *** Gets the "Command List"
    *** @return The "Command List"
    **/
    public String[] getCommandList()
    {
        if (ListTools.isEmpty(this.commandMap)) {
            return null;
        } else {
            return this.commandMap.keyArray(String.class);
        }
    }

    /**
    *** Gets the "Command Description" for the specified command
    *** @param dft  The default "Command Description"
    *** @return The "Command Description" for the specified command
    **/
    public String getCommandDescription(String cmdName, String dft)
    {
        Command cmd = this.getCommand(cmdName);
        return (cmd != null)? cmd.getDescription() : dft;
    }

    /**
    *** Gets the "Command String" for the specified command
    *** @param dft  The default "Command String"
    *** @return The "Command String" for the specified command
    **/
    public String getCommandString(String cmdName, String dft)
    {
        Command cmd = this.getCommand(cmdName);
        return (cmd != null)? cmd.getCommandString() : dft;
    }

    /**
    *** Gets the status-code for the specified command.  An event with this 
    *** status code will be inserted into the EventData table when this command
    *** is sent to the device.
    *** @param code  The default status-code
    *** @return The status-code for the specified command
    **/
    public int getCommandStatusCode(String cmdName, int code)
    {
        Command cmd = this.getCommand(cmdName);
        return (cmd != null)? cmd.getStatusCode() : code;
    }

    /**
    *** Gets the command's (name,description) map
    *** @param type The description type 
    *** @return The command's (name,description) map
    **/
    public Map<String,Command> getCommandMap(BasicPrivateLabel privLabel, User user, String type)
    {
        boolean inclReplCmds = true; // for now, include all commands
        String cmdList[] = this.getCommandList();
        if (!ListTools.isEmpty(cmdList)) {
            Map<String,Command> cmdMap = new OrderedMap<String,Command>();
            for (Command cmd : this.commandMap.values()) {
                if (!DCServerFactory.isCommandTypeAll(type) && !cmd.isType(type)) {
                    // ignore this command
                    //Print.logInfo("Command '%s' is not property type '%s'", cmd.getName(), type);
                } else
                if ((privLabel != null) && !privLabel.hasWriteAccess(user,cmd.getAclName())) {
                    // user does not have access to this command
                    //Print.logInfo("User does not have access to command '%s'", cmd.getName());
                } else {
                    String key  = cmd.getName();
                    String desc = cmd.getDescription();
                    String cstr = cmd.getCommandString();
                    if (StringTools.isBlank(desc) && StringTools.isBlank(cstr)) {
                        // skip commands with blank description and commands
                        Print.logInfo("Command does not have a descripton, or command is blank");
                        continue;
                    } else
                    if (!inclReplCmds) {
                        if (cstr.indexOf("${") >= 0) { //}
                            // should not occur ('type' should not include commands that require parameters)
                            // found "${text}"
                            continue;
                        }
                    }
                    cmdMap.put(key,cmd);
                }
            }
            return cmdMap;
        } else {
            //Print.logInfo("Command list is empty: " + this.getName());
            return null;
        }
    }

    /**
    *** Gets the command's (name,description) map
    *** @param type The description type 
    *** @return The command's (name,description) map
    **/
    public Map<String,String> getCommandDescriptionMap(BasicPrivateLabel privLabel, User user, String type)
    {
        Map<String,Command> cmdMap = this.getCommandMap(privLabel, user, type);
        if (!ListTools.isEmpty(cmdMap)) {
            Map<String,String> cmdDescMap = new OrderedMap<String,String>();
            for (Command cmd : cmdMap.values()) {
                String key  = cmd.getName();
                String desc = cmd.getDescription();
                cmdDescMap.put(key,desc); // Commands are pre-qualified
            }
            return cmdDescMap;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public RTProperties getDefaultProperties()
    {
        RTProperties rtp = this.rtPropsMap.get(DEFAULT_PROP_GROUP_ID);
        if (rtp == null) {
            rtp = new RTProperties();
            this.rtPropsMap.put(DEFAULT_PROP_GROUP_ID, rtp);
        }
        return rtp;
    }
    
    public Set<String> getPropertyGroupNames()
    {
        this.getDefaultProperties(); // make sure the detault properties are cached
        return this.rtPropsMap.keySet();
    }

    public RTProperties getProperties(String propID)
    {
        return this.getProperties(propID, false);
    }

    public RTProperties getProperties(String propID, boolean createNewGroup)
    {
        if (StringTools.isBlank(propID) || propID.equalsIgnoreCase(DEFAULT_PROP_GROUP_ID)) {
            // blank group, return default
            return this.getDefaultProperties();
        } else {
            RTProperties rtp = this.rtPropsMap.get(propID);
            if (rtp != null) {
                // found, return properties group
                return rtp;
            } else
            if (createNewGroup) {
                // not found, create
                rtp = new RTProperties();
                this.rtPropsMap.put(propID, rtp);
                return rtp;
            } else {
                // do not create, return default
                return this.getDefaultProperties();
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Prepend DCS name to key
    **/
    public String normalizeKey(String key)
    {
        if (StringTools.isBlank(key)) {
            return "";
        } else
        if (key.indexOf(this.getName() + ".") >= 0) {
            // "enfora.tcpPort"
            // "DCServer.enfora.tcpPort"
            return key;
        } else {
            // "tcpPort" ==> "enfora.tcpPort"
            return this.getName() + "." + key;
        }
    }

    /**
    *** Prepend DCS name to keys
    **/
    public String[] normalizeKeys(String key[])
    {
        if (!ListTools.isEmpty(key)) {
            for (int i = 0; i < key.length; i++) {
                key[i] = this.normalizeKey(key[i]);
            }
        }
        return key;
    }

    // ------------------------------------------------------------------------

    public boolean hasProperty(String key[], boolean inclDft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return true;
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return true;
            } else {
                return RTConfig.hasProperty(k, inclDft);
            }
        }
    }

    public Set<String> getPropertyKeys(String prefix)
    {
        RTProperties rtp = this.getDefaultProperties();
        Set<String> propKeys = new HashSet<String>();

        /* regualr keys */
        propKeys.addAll(rtp.getPropertyKeys(prefix));
        propKeys.addAll(RTConfig.getPropertyKeys(prefix));

        /* normalized keys */
        String pfx = this.normalizeKey(prefix);
        propKeys.addAll(rtp.getPropertyKeys(pfx));
        propKeys.addAll(RTConfig.getPropertyKeys(pfx));

        return propKeys;
    }

    // ------------------------------------------------------------------------

    public String[] getStringArrayProperty(String key, String dft[])
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getStringArray(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getStringArray(k, dft);
            } else {
                return RTConfig.getStringArray(k, dft);
            }
        }
    }

    public String[] getStringArrayProperty(String key[], String dft[])
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getStringArray(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getStringArray(k, dft);
            } else {
                return RTConfig.getStringArray(k, dft);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    public String getStringProperty(String key, String dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getString(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                // local normalized key
                return rtp.getString(k, dft);
            } else
            if (RTConfig.hasProperty(k)) {
                // global normalized key
                return RTConfig.getString(k, dft);
            } else {
                // global original key
                return RTConfig.getString(key, dft);
            }
        }
    }

    public String getStringProperty(String key[], String dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getString(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                // local normalized key
                return rtp.getString(k, dft);
            } else
            if (RTConfig.hasProperty(k)) {
                // global normalized key
                return RTConfig.getString(k, dft);
            } else {
                // global original key
                return RTConfig.getString(key, dft);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    public int getIntProperty(String key, int dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getInt(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getInt(k, dft);
            } else {
                return RTConfig.getInt(k, dft);
            }
        }
    }

    public int getIntProperty(String key[], int dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getInt(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getInt(k, dft);
            } else {
                return RTConfig.getInt(k, dft);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    public long getLongProperty(String key, long dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getLong(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getLong(k, dft);
            } else {
                return RTConfig.getLong(k, dft);
            }
        }
    }

    public long getLongProperty(String key[], long dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getLong(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getLong(k, dft);
            } else {
                return RTConfig.getLong(k, dft);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    public double getDoubleProperty(String key, double dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getDouble(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getDouble(k, dft);
            } else {
                return RTConfig.getDouble(k, dft);
            }
        }
    }

    public double getDoubleProperty(String key[], double dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getDouble(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getDouble(k, dft);
            } else {
                return RTConfig.getDouble(k, dft);
            }
        }
    }

    // ------------------------------------------------------------------------

    public boolean getBooleanProperty(String key, boolean dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getBoolean(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getBoolean(k, dft);
            } else {
                return RTConfig.getBoolean(k, dft);
            }
        }
    }

    public boolean getBooleanProperty(String key[], boolean dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getBoolean(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getBoolean(k, dft);
            } else {
                return RTConfig.getBoolean(k, dft);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** Returns the state of the indicated bit within the mask for this device type.
    *** @param mask  The input mask from the device
    *** @param bit   The bit to test
    **/
    public boolean getDigitalInputState(long mask, int bit)
    {
        int ofs = this.getIntProperty(DCServerFactory.PROP_Attribute_InputOffset, -1);
        int b   = (ofs > 0)? (ofs - bit) : bit;
        return (b >= 0)? ((mask & (1L << b)) != 0L) : false;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the 'other' DCServerCOnfig is equal to this DCServerConfig
    *** based on the name.
    *** @param other  The other DCServerConfig instance.
    *** @return True if the other DCServerConfig as the same name as this DCServerConfig
    **/
    public boolean equals(Object other)
    {
        if (other instanceof DCServerConfig) {
            String thisName  = this.getName();
            String otherName = ((DCServerConfig)other).getName();
            return thisName.equals(otherName);
        } else {
            return false;
        }
    }

    /**
    *** Compares another DCServerConfig instance to this instance.
    *** @param other  The other DCServerConfig instance.
    *** @return 'compareTo' operator on DCServerConfig names.
    **/
    public int compareTo(Object other)
    {
        if (other instanceof DCServerConfig) {
            String thisName  = this.getName();
            String otherName = ((DCServerConfig)other).getName();
            return thisName.compareTo(otherName);
        } else {
            return -1;
        }
    }

    /**
    *** Return hashCode based on the DCServerConfig name
    *** @return this.getNmae().hashCoe()
    **/
    public int hashCode()
    {
        return this.getName().hashCode();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    *** @return A String representation
    **/
    public String toString()
    {
        return this.toString(true);
    }

    /**
    *** Returns a String representation of this instance
    *** @param inclName True to include the name in the returnsed String representation
    *** @return A String representation
    **/
    public String toString(boolean inclName)
    {
        // "(opendmtp) OpenDMTP Server [TCP=31000 UDP=31000 CMD=30050]
        StringBuffer sb = new StringBuffer();

        /* name/description */
        if (inclName) {
            sb.append("(").append(this.getName()).append(") ");
        }
        sb.append(this.getDescription()).append(" ");

        /* ports */
        sb.append("[");
        this.getPortsString(sb);
        sb.append("]");
        
        /* String representation */
        return sb.toString();

    }
    
    public StringBuffer getPortsString(StringBuffer sb)
    {
        if (sb == null) { sb = new StringBuffer(); }
        int p = 0;
        int tcp[] = this.getTcpPorts();
        if (!ListTools.isEmpty(tcp)) {
            if (p > 0) { sb.append(" "); }
            sb.append("TCP=" + StringTools.join(tcp,","));
            p++;
        }
        int udp[] = this.getUdpPorts();
        if (!ListTools.isEmpty(udp)) {
            if (p > 0) { sb.append(" "); }
            sb.append("UDP=" + StringTools.join(udp,","));
            p++;
        }
        int cmd = this.getCommandDispatcherPort();
        if (cmd > 0) {
            if (p > 0) { sb.append(" "); }
            sb.append("CMD=" + cmd);
            p++;
        }
        if (p == 0) {
            sb.append("no-ports");
        }
        return sb;
    }
    
    public String getPortsString()
    {
        return this.getPortsString(null).toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return running jar file path
    **/
    public File[] getRunningJarPath()
    {
        return DCServerConfig.getRunningJarPath(this.getName());
    }

}
