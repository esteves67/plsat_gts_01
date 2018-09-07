// ----------------------------------------------------------------------------
// Copyright 2007-2014, OpenGTS
// All rights reserved
// ----------------------------------------------------------------------------
//
// This source module is PROPRIETARY and CONFIDENTIAL.
// NOT INTENDED FOR PUBLIC RELEASE.
// 
// Use of this software is subject to the terms and conditions outlined in
// the 'Commercial' license provided with this software.  If you did not obtain
// a copy of the license with this software please request a copy from the
// Software Provider.
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// Example Responses:
// ---------
//
// Table Schema Response:
// <GTSResponse command="dbschema" type="success">
//    <TableSchema table="Device">
//       <Description><![CDATA[This table defines Device/Vehicle specific information for an Account.]]></Description>
//       <Field name="accountID" primaryKey="true" type="STRING[32]"/>
//       <Field name="deviceID" primaryKey="true" type="STRING[32]"/>
//       ...
//    </TableSchema>
// </GTSResponse>
//
// Report Response
// <GTSResponse command="report" type="success">
//    <Report name="EventDetail">
//       <Device>mydevice</Device>
//       <DeviceGroup>mydevice</DeviceGroup>
//       <TimeFrom timezone="GMT">2009/03/13,00:00:00</TimeFrom>
//       <TimeTo timezone="GMT">2009/03/13,23:59:59</TimeTo>
//       <Title>Report Title</Title>
//       <Subtitle>Subtitle</Subtitle>
//       <HeaderRow>
//          <HeaderColumn name="abc">title</Column>
//       </HeaderRow>
//       <BodyRow>
//          <BodyColumn name="abc">value</Column>
//       </BodyRow>
//    </Report>
// </GTSResponse>
//
// ----------------------------------------------------------------------------
//
// Error Message Response:
// <GTSResponse type="error">
//    <Message code="code"><![CDATA[ Error Message ]]></Error>
// </GTSResponse>
//
// Success Message Response:
// <GTSResponse type="success">
//    <Message code="code"><![CDATA[ Success Message ]]></Error>
// </GTSResponse>
//
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.extra.service;

import java.util.*;
import java.io.*;
import java.net.*;

import java.lang.management.*; 
import javax.management.*; 
import javax.management.remote.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.DBConfig;
import org.opengts.db.tables.*;

public class ServiceClient
    implements ServiceXML
{
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static void listAccounts(GTSServiceRequest servReq)
    {
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_HELP[]      = new String[] { "help"    , "h"    };
    private static final String ARG_VERSBOSE[]  = new String[] { "verbose" , "v"    };
    private static final String ARG_QUIET[]     = new String[] { "quiet"   , "q"    };
    private static final String ARG_NOEMAIL[]   = new String[] { "noemail" , "debug"};
    private static final String ARG_FILE[]      = new String[] { "file"    , "xml"  };
    private static final String ARG_MISC_ARG[]  = new String[] { "arg"              };
    private static final String ARG_TABLE[]     = new String[] { "table"            };
    private static final String ARG_COMMAND[]   = new String[] { "command" , "c"    };
    private static final String ARG_URL[]       = new String[] { "url"     ,          "SERVICE_URL"     , "GTSRequest.url" };
    private static final String ARG_ACCOUNT[]   = new String[] { "account" , "a"    , "SERVICE_ACCOUNT" };
    private static final String ARG_DEVICE[]    = new String[] { "device"  , "d"    , "SERVICE_DEVICE"  };
    private static final String ARG_USER[]      = new String[] { "user"    , "u"    , "SERVICE_USER"    };
    private static final String ARG_PASSWORD[]  = new String[] { "password", "p"    , "SERVICE_PASSWORD"};
  //private static final String ARG_LIST_DEV[]  = new String[] { "listdev"          };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Command-line entry for for testing/debugging:
    // ------------------------------------------------------------------------
    // export SERVICE_URL=http://localhost:8080/track/ws
    // export SERVICE_ACCOUNT=demo
    // bin/exeJava org.opengts.extra.service.ServiceClient -debugMode -file=misc/demoEventData.xml
    // bin/exeJava org.opengts.extra.service.ServiceClient -c=dbget    -a=demo -u=admin -p= -arg=rcd -table=Device
    // bin/exeJava org.opengts.extra.service.ServiceClient -c=dbget    -a=sysadmin -p=sysadmin -arg=rcd -table=Account
    // bin/exeJava org.opengts.extra.service.ServiceClient -c=dbput    -a=demo -u=admin -p= -d=demo2 -arg='Demo Test'
    // bin/exeJava org.opengts.extra.service.ServiceClient -c=dbschema -a=demo -u=admin -p= -arg=Device
    // bin/exeJava org.opengts.extra.service.ServiceClient -c=report   -a=demo -u=admin -p= -d=demo2
    // bin/exeJava org.opengts.extra.service.ServiceClient -c=mapdata  -a=demo -u=admin -p= -d=demo2
    // bin/exeJava org.opengts.extra.service.ServiceClient -c=pushpins -a=demo -u=admin -p=
    // ----------
    // bin/exeJava org.opengts.extra.service.ServiceClient -c=dbschema -a=demo -u=admin -p= -arg=Device
    // bin/exeJava org.opengts.extra.service.ServiceClient -c=pushpins -a=demo -u=admin -p=
    // bin/exeJava org.opengts.extra.service.ServiceClient -c=report   -a=demo -u=admin -p= -device=demo2
    // bin/exeJava org.opengts.extra.service.ServiceClient -c=mapdata  -a=demo -u=admin -p=

    private static void usage_exit(int exitCode)
    {
        Print.sysPrintln("Description:");
        Print.sysPrintln("  Web-service client test tool.");
        Print.sysPrintln("");
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + ServiceClient.class.getName() + " {options}");
        Print.sysPrintln("");
        Print.sysPrintln("Common Options:");
        Print.sysPrintln("  -url=<url>      Service URL");
        Print.sysPrintln("  -account=<id>   Account ID");
        Print.sysPrintln("  -user=<id>      User ID");
        Print.sysPrintln("  -password=<pwd> Password");
        Print.sysPrintln("");
        Print.sysPrintln("File Command Options:");
        Print.sysPrintln("  -file=<file>    Load request from specified XML file");
        Print.sysPrintln("");
        Print.sysPrintln("Command-Line Command Options:");
        Print.sysPrintln("  -device=<id>    Device ID");
        Print.sysPrintln("  -command=<cmd>  Command [login|logout|dbschema|dbget|dbput|dbdel|propget|report|mapdata|pushpins|messages|commands]");
        Print.sysPrintln("  -arg=<arg>      Command argument");
        Print.sysPrintln("  -table=<table>  Table name");
        Print.sysPrintln("");
        Print.sysPrintln("Example Env Vars:");
        Print.sysPrintln("  export SERVICE_URL=http://localhost:8080/track/Service");
      //Print.sysPrintln("  export SERVICE_URL=service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");
        Print.sysPrintln("  export SERVICE_ACCOUNT=demo");
        Print.sysPrintln("  export SERVICE_USER=user");
        Print.sysPrintln("  export SERVICE_PASSWORD=");
        Print.sysPrintln("");
        System.exit(exitCode);
    }

    /* cron entry point */
    public static int cron(String argv[])
    {
        //RTConfig.setCommandLineArgs(argv);
        DBConfig.cmdLineInit(argv,false);  // main

        /* verbose/quiet */
        boolean verbose = true;
        if (RTConfig.hasProperty(ARG_VERSBOSE)) {
            verbose = RTConfig.getBoolean(ARG_VERSBOSE,true);
        } else
        if (RTConfig.hasProperty(ARG_QUIET)) {
            verbose = !RTConfig.getBoolean(ARG_QUIET,false);
        }
 
        /* SendMail thread mode */
        if (RTConfig.getBoolean(ARG_NOEMAIL,false)) {
            SendMail.SetThreadModel(SendMail.THREAD_NONE);
        } else {
            SendMail.SetThreadModel(SendMail.THREAD_CURRENT);
        }

        /* ServiceRequest */
        final String urlStr  = RTConfig.getString(ARG_URL     , "");
        final String acctID  = RTConfig.getString(ARG_ACCOUNT , "");
        final String userID  = RTConfig.getString(ARG_USER    , "");
        final String passwd  = RTConfig.getString(ARG_PASSWORD, "");
        GTSServiceRequest servReq = null;
        try {
            if (StringTools.isBlank(urlStr)) {
                Print.logError("GTSRequest URL not specified");
                return 1;
            }
            servReq = new GTSServiceRequest(urlStr);
            servReq.setAuthorization(acctID, userID, passwd);
        } catch (MalformedURLException mue) {
            Print.logException("Error", mue);
            return 1;
        }

        /* file request? */
        if (RTConfig.hasProperty(ARG_FILE)) {
            // file contains full "GTSRequest" XML
            File file = RTConfig.getFile(ARG_FILE,null);
            if (file == null) {
                Print.sysPrintln("Warning] GTSRequest file not specified.");
            } else
            if (!file.isFile()) {
                Print.sysPrintln("ERROR] GTSRequest file does not exist: " + file);
            } else {
                //Print.sysPrintln("Reading request from file: " + file);
                Document xmlDoc = null;
                try {
                    xmlDoc = servReq.sendRequest(file);
                    if (xmlDoc == null) {
                        Print.logError("Request error ...");
                        return 99;
                    }
                    if (verbose) {
                        Print.sysPrintln("");
                        Print.sysPrintln("Response:");
                        Print.sysPrintln(XMLTools.nodeToString(xmlDoc));
                        Print.sysPrintln("");
                    }
                } catch (IOException ioe) {
                    Print.logException("Error", ioe);
                    return 99;
                }
            }
            return 0;
        }

        /* nothing processed */
        return 1;

    }

    /* command-line entry point */
    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main

        /* verbose/quiet */
        boolean verbose = true;
        if (RTConfig.hasProperty(ARG_VERSBOSE)) {
            verbose = RTConfig.getBoolean(ARG_VERSBOSE,true);
        } else
        if (RTConfig.hasProperty(ARG_QUIET)) {
            verbose = !RTConfig.getBoolean(ARG_QUIET,false);
        }

        /* help */
        if (RTConfig.hasProperty(ARG_HELP)) {
            usage_exit(0);
            // control does not reach here
        }

        /* Invoke Cron: load file */
        if (RTConfig.hasProperty(ARG_FILE)) {
            int exitCode = ServiceClient.cron(args);
            if (exitCode == 1) {
                ServiceClient.usage_exit(exitCode);
            }
            System.exit(exitCode);
        }

        // ---------------------------------------------------
        // Debug stuff follows
 
        /* ServiceRequest */
        final String urlStr  = RTConfig.getString(ARG_URL     , "");
        final String acctID  = RTConfig.getString(ARG_ACCOUNT , "");
        final String userID  = RTConfig.getString(ARG_USER    , "");
        final String passwd  = RTConfig.getString(ARG_PASSWORD, "");
        GTSServiceRequest servReq = null;
        try {
            servReq = new GTSServiceRequest(urlStr);
            servReq.setAuthorization(acctID, userID, passwd);
        } catch (MalformedURLException mue) {
            Print.logException("Error", mue);
            System.exit(99);
        }

        /* command-line specified commands */
        final String command = RTConfig.getString(ARG_COMMAND , "");
        final String devID   = RTConfig.getString(ARG_DEVICE  , "");

        /* convenience commands */
        if (command.equalsIgnoreCase(CMD_version)) {
            try {
                String version = servReq.getVersion();
                Print.sysPrintln("Version: " + version);
            } catch (Throwable th) {
                Print.logException("Error", th);
            }
            System.exit(0);
        } else
        if (command.equalsIgnoreCase(CMD_dbschema)) {
            try {
                String tableName = RTConfig.getString(ARG_TABLE,"");
                Document xmlDoc = servReq.getTableSchema_Document(tableName);
            } catch (Throwable th) {
                Print.logException("Error", th);
            }
            System.exit(0);
        } else
        if (command.equalsIgnoreCase(CMD_mapdata)) {
            try {
                DateTime frDT = new DateTime(null, 2007, 3, 13,  0,  0,  0);
                DateTime toDT = new DateTime(null, 2007, 3, 13, 23, 59, 59);
                Document xmlDoc = servReq.getMapDataDevice_Document(devID,frDT,toDT);
            } catch (Throwable th) {
                Print.logException("Error", th);
            }
            System.exit(0);
        } else
        if (command.equalsIgnoreCase(CMD_report)) {
            try {
                String rtpName = RTConfig.getString(ARG_MISC_ARG,"EventDetail");
                DateTime frDT = new DateTime(null, 2007, 3, 13,  0,  0,  0);
                DateTime toDT = new DateTime(null, 2007, 3, 13, 23, 59, 59);
                Document xmlDoc = servReq.getReportDevice_Document(rtpName,devID,frDT,toDT);
            } catch (Throwable th) {
                Print.logException("Error", th);
            }
            System.exit(0);
        } else
        if (command.equalsIgnoreCase("getAccount")) {
            try {
                Account account = servReq.getAccount(acctID);
            } catch (DBException dbe) {
                Print.logException("Account", dbe);
            }
            System.exit(0);
        } else
        if (command.equalsIgnoreCase("getAccounts")) {
            try {
                String acctIDList[] = servReq.getAccountIDs();
                for (int i = 0; i < acctIDList.length; i++) {
                    String virtAcctID = acctIDList[i];
                    try {
                        Print.sysPrintln(" " + (i+1) + ": " + virtAcctID);
                        Account acct = servReq.getAccount(virtAcctID);
                        Print.sysPrintln("Account: " + acct);
                    } catch (DBException dbe) {
                        Print.logException("Account error: " + virtAcctID, dbe);
                    }
                }
            } catch (DBException dbe) {
                Print.logException("Account List", dbe);
            }
            System.exit(0);
        } else
        if (command.equalsIgnoreCase("getDevice")) {
            try {
                Device device = servReq.getDevice(acctID,devID);
            } catch (DBException dbe) {
                Print.logException("Device", dbe);
            }
            System.exit(0);
        } else
        if (command.equalsIgnoreCase("getUser")) {
            try {
                User user = servReq.getUser(acctID,userID);
            } catch (DBException dbe) {
                Print.logException("User", dbe);
            }
            System.exit(0);
        }

        /* commands */
        ServiceRequest.RequestBody reqBody = null;
        if (command.equalsIgnoreCase(CMD_dbget)) {
            boolean rcdType = RTConfig.getString(ARG_MISC_ARG,"").equalsIgnoreCase("rcd");
            final String tagName   = rcdType? "Record" : "RecordKey";
            final String tableName = RTConfig.getString(ARG_TABLE,"Device");
            reqBody = new ServiceRequest.RequestBody() {
                public StringBuffer appendRequestBody(StringBuffer sb, int indent) {
                    sb.append("   <"+tagName+" table=\""+tableName+"\" "+ATTR_partial+"=\"true\">\n");
                    sb.append("      <Field name=\""+Account.FLD_accountID+"\">"+acctID+"</Field>\n");
                    sb.append("      <Field name=\""+Account.FLD_description+"\"/>\n");
                    sb.append("   </"+tagName+">\n");
                    return sb;
                }
            };
        } else
        if (command.equalsIgnoreCase(CMD_dbput)) {
            final String desc = RTConfig.getString(ARG_MISC_ARG,"");
            final String tableName = RTConfig.getString(ARG_TABLE,"Device");
            reqBody = new ServiceRequest.RequestBody() {
                public StringBuffer appendRequestBody(StringBuffer sb, int indent) {
                    sb.append("   <Record table=\""+tableName+"\">\n");
                    sb.append("      <Field name=\""+Device.FLD_accountID+"\">"+acctID+"</Field>\n");
                    sb.append("      <Field name=\""+Device.FLD_deviceID+"\">"+devID+"</Field>\n");
                    sb.append("      <Field name=\""+Device.FLD_description+"\">"+desc+"</Field>\n");
                    sb.append("   </Record>\n");
                    return sb;
                }
            };
        } else
        if (command.equalsIgnoreCase(CMD_commands)) {
            // no RequestBody required
        } else {
            Print.sysPrintln("Unrecognized command: " + command);
            usage_exit(1);
            // control does not reach here
        }

        /* send request */
        Document xmlDoc = null;
        try {
            xmlDoc = servReq.sendRequest(command, reqBody);
        } catch (IOException ioe) {
            Print.logException("Error", ioe);
        }

    }

}

