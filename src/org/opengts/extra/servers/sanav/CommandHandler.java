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
package org.opengts.extra.servers.sanav;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

public class CommandHandler
    extends CommandPacketHandler
{

    // ------------------------------------------------------------------------

    /* packet handler constructor */
    public CommandHandler() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    public String getServerName()
    {
        return Constants.DEVICE_CODE;
    }

    public int getClientCommandPort_udp(int dftPort)
    {
        DCServerConfig dcsc = Main.getServerConfig();
        int port = dcsc.getClientCommandPort_udp(0);
        return (port > 0)? port : dftPort;
    }

    // ------------------------------------------------------------------------
    
    private byte[] createPacket(Device device, String cmdName, DCServerConfig.CommandProtocol cmdProto, String cmdStr)
    {
        // TODO:
        return null;
    }
        
    // ------------------------------------------------------------------------

    public DCServerFactory.ResultCode handleCommand(Device device, String cmdType, String cmdName, String cmdArgs[])
    {
        Print.logInfo("Received Command: type=%s name=%s arg=%s", cmdType, cmdName, StringTools.join(cmdArgs,','));
        if (device == null) {
            return DCServerFactory.ResultCode.INVALID_DEVICE;
        }

        /* default command */
        if (cmdType.equalsIgnoreCase(DCServerConfig.COMMAND_PING)) {
            cmdType = DCServerConfig.COMMAND_CONFIG;
            cmdName = "LocateNow";
        }
        if (!cmdType.equalsIgnoreCase(DCServerConfig.COMMAND_CONFIG)) {
            return DCServerFactory.ResultCode.INVALID_TYPE;
        }

        /* custom command */
        DCServerConfig dcsc = Main.getServerConfig();
        DCServerConfig.Command command = dcsc.getCommand(cmdName);
        if (command == null) {
            Print.logWarn("Command not found: " + cmdName);
            return DCServerFactory.ResultCode.INVALID_COMMAND;
        }
        String cmdStr = command.getCommandString(device, cmdArgs);
        int cmdStCode = command.getStatusCode();
        Print.logInfo("CmdStr: " + cmdStr);
        
        /* validate protocol */
        DCServerConfig.CommandProtocol cmdProto = command.getCommandProtocol(DCServerConfig.CommandProtocol.UDP);
        if (cmdProto == null) {
            return DCServerFactory.ResultCode.INVALID_PROTO;
        } else
        if (!cmdProto.equals(DCServerConfig.CommandProtocol.UDP)) {
            return DCServerFactory.ResultCode.INVALID_PROTO;
        }

        /* command packet */
        Print.logDebug("Command [device=%s, cmd=%s]", device.getDeviceID(), cmdName);
        byte packet[] = this.createPacket(device, cmdName, cmdProto, cmdStr);
        if (ListTools.isEmpty(packet)) {
            return DCServerFactory.ResultCode.INVALID_PACKET;
        }
        
        /* send packet */
        DCServerFactory.ResultCode cmdResp = DCServerFactory.ResultCode.INVALID_PROTO;
        switch (cmdProto) {
            case UDP:
                cmdResp = this.sendCommandPacket_udp(device, cmdType, packet);
                break;
            case SMS:
                cmdResp = this.sendCommandPacket_sms(device, cmdType, packet);
                break;
            case TCP:
                // TODO:
                cmdResp = DCServerFactory.ResultCode.INVALID_PROTO;
                break;
        }

        /* exit if error */
        if (!cmdResp.isSuccess()) {
            return cmdResp;
        }

        /* insert event? */
        if (cmdStCode > 0) { // (cmdStCode != StatusCodes.STATUS_NONE)

            /* create event */
            String    acctID  = device.getAccountID();
            String    devID   = device.getDeviceID();
            long      fixtime = DateTime.getCurrentTimeSec(); // now
            EventData.Key evk = new EventData.Key(acctID, devID, fixtime, cmdStCode);
            EventData evd     = evk.getDBRecord();

            /* display */
            DateTime dt   = new DateTime(fixtime);
            String scHex  = StatusCodes.GetHex(cmdStCode);
            String scDesc = StatusCodes.GetDescription(cmdStCode,null);
            Print.logInfo("Event: ["+dt+"] "+scHex+"-"+scDesc+"  ");

            /* insert event */
            device.insertEventData(evd);
            
        }

        /* return response (SUCCESS) */
        return cmdResp;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private DCServerFactory.ResultCode sendCommandPacket_sms(Device device, String cmdType, byte pktData[])
    {

        /* validate */
        if (ListTools.isEmpty(pktData)) {
            Print.logError("Command packet is null/empty (%s)", cmdType);
            return DCServerFactory.ResultCode.INVALID_ARG;
        }
        String pktStr = StringTools.toStringValue(pktData);

        /* SMS email */
        String smsEmail = device.getSmsEmail();
        if (StringTools.isBlank(smsEmail)) {
            Print.logError("Device SMS Email address is blank: " + device.getDeviceID());
            return DCServerFactory.ResultCode.NOT_SUPPORTED;
        }
        
        /* send SMS email */

        /* not currently supported */
        return DCServerFactory.ResultCode.INVALID_PROTO;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private DCServerFactory.ResultCode sendCommandPacket_udp(Device device, String cmdType, byte pktData[])
    {

        /* validate */
        if (ListTools.isEmpty(pktData)) {
            Print.logError("Command packet is null/empty (%s)", cmdType);
            return DCServerFactory.ResultCode.INVALID_ARG;
        }

        /* init */
        int    frPort = device.getListenPortCurrent();
        String toHost = StringTools.trim(device.getIpAddressCurrent());
        int    toPort = this.getClientCommandPort_udp(device.getRemotePortCurrent());
        long   age    = DateTime.getCurrentTimeSec() - device.getLastTotalConnectTime();

        /* validate host/port */
        if (StringTools.isBlank(toHost) || toHost.equals("0.0.0.0") || (toPort <= 0)) {
            Print.logError("Device host:port not known> %s:%d", toHost, toPort);
            return DCServerFactory.ResultCode.UNKNOWN_HOST;
        }

        /* send */
        Print.logInfo("Send '%s:%d' (age %dsec) [%s] 0x%s", toHost, toPort, age,
            cmdType, StringTools.toHexString(pktData));
        try {
            this.sendDatagramMessage(frPort, toHost, toPort, pktData);
            return DCServerFactory.ResultCode.SUCCESS;
        } catch (Throwable th) {
            Print.logException("UDP transmit failure", th);
            return DCServerFactory.ResultCode.TRANSMIT_FAIL;
        }

    }

    private void sendDatagramMessage(int frPort, String toHost, int toPort, byte pkt[])
         throws IOException
    {

        /* get datagram socket */
        boolean closeSocket = false;
        DatagramSocket dgSocket = TrackServer.getTrackServer().getUdpDatagramSocket(frPort);
        if (dgSocket == null) {
            Print.logWarn("Creating temporary DatagramSocket for transmission");
            dgSocket = ServerSocketThread.createDatagramSocket(0);
            closeSocket = true;
        }

        /* datagram packet */
        DatagramPacket respPkt = new DatagramPacket(pkt, pkt.length, 
            InetAddress.getByName(toHost), toPort);

        /* send */
        Print.logInfo("Sending Datagram(from %d) to %s:%d> 0x%s", dgSocket.getLocalPort(), 
            toHost, toPort, StringTools.toHexString(pkt));
        dgSocket.send(respPkt);

        /* close */
        if (closeSocket) {
            dgSocket.close();
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String args[])
    {
        RTConfig.setCommandLineArgs(args);
        CommandHandler ech = new CommandHandler();
    }
    
}
