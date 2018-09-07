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
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.math.*;

/**
*** Posix Signal Handler.
*** This class requires the availability of the "sun.misc.SignalHandler" class
*** for both compiling and running.
**/

public class PosixSignalHandler
{

    // ------------------------------------------------------------------------

    public static final String SIGNAL_HUP           = "SIGHUP";
    public static final String SIGNAL_INT           = "SIGINT";    // control-C
    public static final String SIGNAL_TERM          = "SIGTERM";
    public static final String SIGNAL_ABRT          = "SIGABRT";
    
    public static final String SIGNAL_LIST[]        = {
        SIGNAL_HUP,
        SIGNAL_INT,
        SIGNAL_TERM,
        SIGNAL_ABRT
    };
    
    public static boolean isValidSignalNames(String... sigs)
    {

        /* no signals specified */
        if (ListTools.isEmpty(sigs)) {
            return false;
        }

        /* compare against list */
        for (int i = 0; i < sigs.length; i++) {
            String sigName = (sigs[i] != null)? sigs[i].trim().toUpperCase() : null;
            if (sigName == null) {
                return false;
            }
            if (!sigName.startsWith("SIG")) {
                sigName = "SIG" + sigName;
            }
            if (!ListTools.contains(SIGNAL_LIST,sigName)) {
                return false;
            }
        }

        /* success */
        return true;
            
    }

    // ------------------------------------------------------------------------

    private boolean sunSignalAvail = false;

    /**
    *** Constructor
    **/
    public PosixSignalHandler()
    {
        try {
            Class.forName("sun.misc.SignalHandler");
            this.sunSignalAvail = true;
        } catch (Throwable th) {
            Print.logWarn("'sun.misc.Signal' is not available");
            this.sunSignalAvail = false;
        }
    }

    /**
    *** Constructor
    *** @param sigs  List of signals
    *** @throws IllegalArgumentException if a specified signal name is invalid
    **/
    public PosixSignalHandler(String... sigs)
        throws IllegalArgumentException
    {
        this();
        //Print.logInfo("sun.misc.SignalHandler available? " + this.sunSignalAvail);
        if (this.sunSignalAvail && (sigs != null)) {
            this.handleSignal(sigs);
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the "sun.misc.Signal" is available
    *** @return True if the "sun.misc.Signal" is available
    **/
    public boolean sunSignalHandlerAvailable()
    {
        return this.sunSignalAvail;
    }

    // ------------------------------------------------------------------------

    /**
    *** set up a signal handler callback for the specified/named signal
    *** (should be one of "HUP", "INT"(^C), "ABRT", "TERM")
    *** @param sigNames List of signal names
    *** @return True if the signal was successfully handled, false if "sun.misc.Signal" is not available.
    *** @throws IllegalArgumentException if a specified signal name is invalid
    **/
    public boolean handleSignal(String... sigNames)
        throws IllegalArgumentException
    {

        /* check for Signal availability */
        if (!this.sunSignalAvail) {
            return false;
        }

        /* valid signals specified */
        if (!PosixSignalHandler.isValidSignalNames(sigNames)) {
            String sigStr = StringTools.join(sigNames,",");
            throw new IllegalArgumentException("Invalid Signal names specified: " + sigStr);
        }

        /* handle signals */
        for (int i = 0; i < sigNames.length; i++) {
            this._handleSignal(sigNames[i]); // may throw IllegalArgumentException
        }
        return true;
        
    }
        
    /**
    *** set up a signal handler callback for the specified/named signal
    *** (should be one of "HUP", "INT"(^C), "ABRT", "TERM")
    *** @param sigName  The signal name
    *** @return True if the signal was successfully handled, false if "sun.misc.Signal" is not available.
    *** @throws IllegalArgumentException if the specified signal name is invalid
    **/
    public boolean _handleSignal(String sigName)
        throws IllegalArgumentException
    {

        /* check for Signal availability */
        if (!this.sunSignalAvail) {
            return false;
        }

        /* adjust signal name */
        sigName = StringTools.trim(sigName).toUpperCase(); // remove leading/trailing blanks
        if (sigName.startsWith("SIG")) { // remove leading "SIG"
            sigName = sigName.substring(3).trim();
        }
        if (StringTools.isBlank(sigName)) {
            throw new IllegalArgumentException("Invalid blank signal name");
        }

        /* handle Signal */
        try {
            sun.misc.Signal.handle(new sun.misc.Signal(sigName), new sun.misc.SignalHandler() {
                public void handle(sun.misc.Signal sig) {
                    String sigStr = (sig != null)? sig.toString() : "unknown";
                    PosixSignalHandler.this.signal(sigStr);
                }
            });
            //Print.logInfo("Installed SigHandler: " + sigName);
            return true;
        } catch (Throwable th) {
            // SignalHandler likely not available.
            Print.logException("Unable to handle signal", th);
            return false;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** overridable handler callback
    **/
    protected void signal(String sigName)
    {

        /* null */
        if (StringTools.isBlank(sigName)) {
            Print.logWarn("Received Signal: <blank>");
            return;
        }

        /* adjust */
        sigName = sigName.trim().toUpperCase();
        if (!sigName.startsWith("SIG")) { // add leading "SIG"
            sigName = "SIG" + sigName;
        }

        /* check for different known signals */
        if (sigName.equalsIgnoreCase(SIGNAL_HUP)) {
            this.signal_HUP();
        } else
        if (sigName.equalsIgnoreCase(SIGNAL_INT)) {
            this.signal_INT();
        } else
        if (sigName.equalsIgnoreCase(SIGNAL_ABRT)) {
            this.signal_ABRT();
        } else
        if (sigName.equalsIgnoreCase(SIGNAL_TERM)) {
            this.signal_TERM();
        } else {
            Print.logInfo("Received Signal: " + sigName);
        }

    }

    /**
    *** "HUP" signal callback
    **/
    protected void signal_HUP()
    {
        Print.logInfo("Received HUP");
    }

    /**
    *** "INT" signal callback
    **/
    protected void signal_INT()
    {
        Print.logInfo("Received INT");
    }

    /**
    *** "ABRT" signal callback
    **/
    protected void signal_ABRT()
    {
        Print.logInfo("Received ABRT");
    }

    /**
    *** "TERM" signal callback
    **/
    protected void signal_TERM()
    {
        Print.logInfo("Received TERM");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        String sigStr = RTConfig.getString("sig","HUP");
        Print.sysPrintln("My PID: " + OSTools.getProcessID());

        try {
            PosixSignalHandler psh = new PosixSignalHandler(sigStr);
            Print.sysPrintln("Handling "+sigStr+" ...");
            try {
                long startMS = DateTime.getCurrentTimeMillis();
                long waitMS  = 120000L, intervalMS = 3333L;
                for (;;) {
                    Thread.sleep(intervalMS);
                    long nowMS   = DateTime.getCurrentTimeMillis();
                    long deltaMS = nowMS - startMS;
                    if (deltaMS >= waitMS) { break; }
                    if (deltaMS < intervalMS) {
                        Print.sysPrint("*");
                    } else {
                        Print.sysPrint(".");
                    }
                }
            } catch (Throwable th) { // InterruptException
                // ignore
            }
        } catch (IllegalArgumentException iae) {
            Print.logException("Invalid signal name", iae);
            System.exit(1);
        }

        
    }

}
