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
// ----------------------------------------------------------------------------
package org.opengts;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import org.opengts.util.*;

/**
*** This class provides runtime version information to the OpenGTS modules.
**/

public class Version
{

    // ------------------------------------------------------------------------

    // "public" as of 2.4.6Lite
    public  static final String COPYRIGHT = "Copyright 2007-2014, OpenGTS";

    // ------------------------------------------------------------------------

    // This string is parsed via 'grep' & 'sed' scripts and thus 
    // ONLY the version value specified within the quotes should change.
    private static final String VERSION = "2.4.6Lite";

    // This public constant should only be accessed externally by the 'GTSAdmin' application.
    public  static final String GTS_ENTERPRISE_PREFIX = "E";
    public  static final String COMPILED_VERSION = 
        /**/  GTS_ENTERPRISE_PREFIX + // "Enterprise"
        VERSION;

    // last compile time
    private static final long COMPILE_TIMESTAMP = CompileTime.COMPILE_TIMESTAMP;

    // ------------------------------------------------------------------------

    // package release time (modified by command-line 'sed' script to insert actual epoch time)
    private static final long PACKAGE_TIMESTAMP = 1337375332L;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Initializes the RTConfig constant 'version' property.
    **/
    public static void initVersionProperty()
    {
        RTProperties constantProps = RTConfig.getRuntimeConstantProperties();
        if (!constantProps.hasProperty(RTKey.VERSION)) {
            constantProps.setProperty(RTKey.VERSION, Version.getVersion());
            //System.out.println("Set Version = " + Version.getVersion());
        } else {
            // already initialized, no need to reinitialize
        }
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the copyright notice
    *** @return The copyright notice
    **/
    public static String getCopyright()
    {
        return COPYRIGHT;
    }

    /** 
    *** Returns the compiled version String
    *** @return The version String
    **/
    public static String getVersion()
    {
        return COMPILED_VERSION;
    }

    /** 
    *** Returns the compiled package release timestamp
    *** @return The package release timestamp
    **/
    public static long getPackageTimestamp()
    {
        return PACKAGE_TIMESTAMP;
    }

    /** 
    *** Returns the last compiled timestamp
    *** @return The last compiled timestamp
    **/
    public static long getCompileTimestamp()
    {
        return COMPILE_TIMESTAMP;
    }
    
    /**
    *** Returns the last compiled time date string
    *** @return The last compiled time date string
    **/
    public static String getCompileTime()
    {
        return (new DateTime(COMPILE_TIMESTAMP)).toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this Version
    *** @return A String representation of this Version
    **/
    public static String getInfo()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(Version.COPYRIGHT).append("\n");
        sb.append("Version: ").append(Version.getVersion()).append("\n");
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss zzz", Locale.US);
        if (PACKAGE_TIMESTAMP > 0L) {
            String dfmt = dateFmt.format(new Date(PACKAGE_TIMESTAMP * 1000L));
            sb.append("Package: [" + PACKAGE_TIMESTAMP + "] " + dfmt + "\n");
        }
        if (COMPILE_TIMESTAMP > 0L) {
            String dfmt = dateFmt.format(new Date(COMPILE_TIMESTAMP * 1000L));
            sb.append("Compile: [" + COMPILE_TIMESTAMP + "] " + dfmt + "\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Main entry point to display the current version
    *** @param argv The command-line args
    **/
    public static void main(String argv[])
    {
        if ((argv.length > 0) && argv[0].equals("-info")) {
            // Version: 2.0.5
            // Compile: [1211915119] 2008/05/27 19:36:38 GMT
            // Package: [1211915119] 2008/05/27 19:36:38 GMT
            System.out.println(Version.getInfo());
        } else
        if ((argv.length > 0) && argv[0].equals("-package")) {
            // "1211915119"
            System.out.println(Version.getPackageTimestamp());
        } else
        if ((argv.length > 0) && argv[0].equals("-compile")) {
            // "1211915119"
            System.out.println(Version.getCompileTimestamp());
        } else {
            // "1.8.3"
            System.out.println(Version.getVersion());
        }
    }

    // ------------------------------------------------------------------------

}
