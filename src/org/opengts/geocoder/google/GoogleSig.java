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
package org.opengts.geocoder.google;

import java.util.*;
import java.math.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;
import org.opengts.db.*;

public class GoogleSig
{
    
    // ------------------------------------------------------------------------

    private static MACProvider macProvider = null;
    
    public static void SetMACProvider(MACProvider mp)
    {
        GoogleSig.macProvider = mp;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Object keyMac = null;

    public GoogleSig(String keyStr)
    {
        super();
        // [ENTERPRISE]
        if (!StringTools.isBlank(keyStr)) {
            if (GoogleSig.macProvider != null) {
                this.keyMac = GoogleSig.macProvider.getMAC(keyStr,"HmacSHA1");
            } else {
                try {
                    BigInteger seed = null;
                    byte kb[] = org.opengts.util.Base64.decode(keyStr.replace('-','+').replace('_','/'),seed);
                    javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1"); //NoSuchAlgorithmException
                    mac.init(new javax.crypto.spec.SecretKeySpec(kb,"HmacSHA1")); // InvalidKeyException
                    this.keyMac = mac;
                } catch (java.security.NoSuchAlgorithmException nsae) {
                    this.keyMac = null;
                } catch (java.security.InvalidKeyException ike) {
                    this.keyMac = null;
                } catch (Throwable th) {
                    this.keyMac = null;
                }
            }
        }
    }

    public String signURL(String urlStr)
    {
        String sigURL = urlStr;
        // HMAC-SHA1 signition code here
        // [ENTERPRISE]
        if (this.keyMac != null) {
            javax.crypto.Mac mac = (javax.crypto.Mac)this.keyMac;
            try {
                URL url = new URL(sigURL);
                byte pqb[] = (url.getPath() + "?" + url.getQuery()).getBytes();
                String sig = org.opengts.util.Base64.encode(mac.doFinal(pqb)).replace('+','-').replace('/','_');
                sigURL = sigURL + "&signature=" + sig;
            } catch (MalformedURLException mue) { // URISyntaxException
                sigURL = null;
            } catch (Throwable th) {
                sigURL = null;
            }
        }
        return sigURL;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        String urlStr = RTConfig.getString("url",null);
        String keyStr = RTConfig.getString("key",null);
        
        /* check url/key */
        if (StringTools.isBlank(urlStr) || StringTools.isBlank(keyStr)) {
            Print.sysPrintln("ERROR: Missing url or key");
            System.exit(99);
        }
        
        GoogleSig gs = new GoogleSig(keyStr);
        String sigURL = gs.signURL(urlStr);
        Print.sysPrintln("");
        Print.sysPrintln(sigURL);
        Print.sysPrintln("");

    }
    
}
