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
// Description:
//  Server Initialization
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.extra.servers.sanav;

import org.opengts.util.*;
import org.opengts.db.*;

public class Constants
{

    // ------------------------------------------------------------------------

    /* title/version/copyright */
    public static final String  TITLE_NAME              = "Sanav TCP/UDP";
    public static final String  VERSION                 = "0.2.12";
    public static final String  COPYRIGHT               = "Copyright 2007-2014, OpenGTS";

    // ------------------------------------------------------------------------

    /* device code */
    public static final String  DEVICE_CODE             = DCServerFactory.SANAV_NAME;

    // ------------------------------------------------------------------------

    /* ASCII packets */
    public static final boolean ASCII_PACKETS           = false;
    public static final int     ASCII_LINE_TERMINATOR[] = new int[] { '\r', '\n' }; // new int[] { 0xFF };
    public static final int     ASCII_IGNORE_CHARS[]    = null;

    /* packet length */
    public static final int     MIN_PACKET_LENGTH       = 12;   // 4;
    public static final int     MAX_PACKET_LENGTH       = 600;
    
    /* terminate flags */
    public static final boolean TERMINATE_ON_TIMEOUT    = true;

    // ------------------------------------------------------------------------

    /* TCP Timeouts */
    public static final long    TIMEOUT_TCP_IDLE        = 10000L;
    public static final long    TIMEOUT_TCP_PACKET      = 4000L;
    public static final long    TIMEOUT_TCP_SESSION     = 15000L;

    /* UDP Timeouts */
    public static final long    TIMEOUT_UDP_IDLE        = 5000L;
    public static final long    TIMEOUT_UDP_PACKET      = 4000L;
    public static final long    TIMEOUT_UDP_SESSION     = 60000L;

    /* linger on close */
    public static final int     LINGER_ON_CLOSE_SEC     = 5;

    // ------------------------------------------------------------------------

    /* minimum acceptable speed */
    public static final double  MINIMUM_SPEED_KPH       = 3.0;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        Print.sysPrintln(VERSION);
    }

}
