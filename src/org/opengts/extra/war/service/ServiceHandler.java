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
package org.opengts.extra.war.service;

import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import java.lang.management.*; 
import javax.management.*; 

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.Version;
import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.war.tools.PrivateLabel;

import org.opengts.db.*;

import org.opengts.extra.service.ServiceXML;

public class ServiceHandler
    extends NotificationBroadcasterSupport
    implements ServiceHandlerMXBean
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Service service = null;

    public ServiceHandler()
    {
        this.service = new Service();
    }

    // ------------------------------------------------------------------------
    
    public String getVersion()
    {
        return Version.getVersion();
    }

    // ------------------------------------------------------------------------

    public String handleRequest(String xmlReqStr)
    {
        return this.service.handleRequest(xmlReqStr);
    }
    
    public String handleRequest(String xmlReqStr,
        String authAcctID, String authUserID, String authPasswd)
    {
        return this.service.handleRequest(xmlReqStr, authAcctID, authUserID, authPasswd);
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        // -Dcom.sun.management.jmxremote.port=9999
        // -Dcom.sun.management.jmxremote.authenticate=false
        // -Dcom.sun.management.jmxremote.ssl=false 
        //System.setProperty("com.sun.management.jmxremote.port"        , "9999" );
        //System.setProperty("com.sun.management.jmxremote.authenticate", "false");
        //System.setProperty("com.sun.management.jmxremote.ssl"         , "false");
        DBConfig.cmdLineInit(argv,true);  // main

        // bin/exeJava -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false org.opengts.extra.war.service.ServiceHandler 

        /* check for proper environment */
        BasicPrivateLabel bpl = BasicPrivateLabelLoader.getDefaultPrivateLabel();
        if (!(bpl instanceof PrivateLabel)) {
            Print.logError("'PrivateLabel' environment not loaded!");
            System.exit(99);
        }

        /* Service */
        ServiceHandler sp = new ServiceHandler();
 
        /* mbean */
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(ServiceXML.JMXServiceObjectName); 
            mbs.registerMBean(sp, name); 
        } catch (Throwable th) { // MalformedObjectNameException, InstanceAlreadyExistsException
            Print.logException("JMX", th);
            System.exit(99);
        }
 
        /* sleep */
        Print.sysPrintln("Waiting ...");
        try { Thread.sleep(Long.MAX_VALUE); } catch (Throwable th) {/*ignore*/}

    }

}
