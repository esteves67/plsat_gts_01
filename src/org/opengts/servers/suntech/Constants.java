// ----------------------------------------------------------------------------
// Copyright Zona-OpenGTS
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
// Change History:
//  2012/08/05  Carlos Jesus Gonzalez Ramos
//     -Initial release
//  2012/08/18  Carlos Jesus Gonzalez Ramos
//     -This module is to support devices: Enfora Mini MT / Enfora GSM2338 / Enfora GSM2428 / Enfora Spider MTGu / Skypatrol TT8750
// ----------------------------------------------------------------------------
package org.opengts.servers.suntech;

import org.opengts.*;
import org.opengts.util.*;
import org.opengts.db.*;

/**
*** <code>Constants</code> Specifies static information specific to the type of
*** remote device communication protocol being parsed.
**/

public class Constants
{

    // ------------------------------------------------------------------------

    /* title */
    // Displayed at server startup
    public static final String  TITLE_NAME              = "Suntech DCS";
    public static final String  VERSION                 = "0.1.0";
    public static final String  COPYRIGHT               = "Developed by: Zona-OpenGTS";
    
    // ------------------------------------------------------------------------

    public static final String  DEVICE_CODE             = DCServerFactory.SUNTECH_NAME;
    
    // ------------------------------------------------------------------------

    /* all ASCII packets? */
    // Notes:
    // - Set to 'true' if *ALL* packets are expected to contain only ASCII data.
    //   If 'true', then "<TrackCLientPacketHandler>.getActualPacketLength(...)" will *NOT* 
    //   be called to determine the actual client packet length.
    // - Set to 'false' if the client device sends *ANY* binary packet data.
    //   If 'false', then "<TrackCLientPacketHandler>.getActualPacketLength(...)" *WILL*
    //   be called to allow the parser to determine the actual length of the client
    //   packet based on the first few bytes of the data packet (see 'MIN_PACKET_LENGTH').
    public static final boolean ASCII_PACKETS           = false;
    public static final int     ASCII_LINE_TERMINATOR[] = new int[] { '\r', '\n' };

    /* packet length */
    // The minimum expected packet length
    // When starting to read a new packet from the client device, the framework
    // will read 'MIN_PACKET_LENGTH' bytes from the client, then call the method
    // <TrackClientPacketHandler>.getActualPacketLength(...) with the these read
    // bytes.  'getActualPacketLength' should then use these bytes to determine
    // how many total bytes represent the actual packet length.
    public static final int     MIN_PACKET_LENGTH       = 1;
    // The maximum expected packet length
    // This value simply provide an upper limit for the maximum length of any single  
    // packet that is expected to be received from the client device.
    public static final int     MAX_PACKET_LENGTH       = 800;
    
    /* terminate flags */
    // Set to 'true' to close the session on a read timeout
    public static final boolean TERMINATE_ON_TIMEOUT    = true;

    // ------------------------------------------------------------------------

    /* TCP Timeouts (milliseconds) */
    // The time to wait to receive the 1st byte after the session has started
    public static final long    TIMEOUT_TCP_IDLE        = 10000L;
    // After the 1st byte, the remainder of a packet must be read in this timeframe
    public static final long    TIMEOUT_TCP_PACKET      = 4000L;
    // The entire session must complete within this timeframe
    public static final long    TIMEOUT_TCP_SESSION     = 15000L;

    /* UDP Timeouts (milliseconds) */
    // The time to wait to receive the 1st byte after the session has started
    public static final long    TIMEOUT_UDP_IDLE        = 5000L;
    // After the 1st byte, the remainder of a packet must be read in this timeframe
    public static final long    TIMEOUT_UDP_PACKET      = 4000L;
    // The entire session must complete within this timeframe
    public static final long    TIMEOUT_UDP_SESSION     = 60000L;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        Print.sysPrintln(VERSION);
    }

}
