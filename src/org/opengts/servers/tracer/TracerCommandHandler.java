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
package org.opengts.servers.tracer;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

/**
*** <code>EnforaCommandHandler</code> - This module contains the general
*** "business logic" for sending commands to the remote tracking device.
**/

public class TracerCommandHandler
    extends CommandPacketHandler
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** - Set this constant to "true" to enable sending TCP commands through the
    *** the existing TCP session established by the remote device.  The device 
    *** must have a currently established TCP session in order for this method
    *** to work properly.
    *** - Set this constant to "false" to enable sending TCP commands to the remote
    *** tracking device over a newly created TCP socket connection to the device.
    *** TCP socket reouting to the remote device must be enable by the wireless
    *** service provider in order for this method to work properly.
    **/
    private static final boolean SEND_TCP_COMMANDS_THROUGH_EXISTING_SESSION = true;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* packet handler constructor */
    public TracerCommandHandler() 
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
        DCServerConfig dcsc = Main.getServerConfig(null);
        int port = dcsc.getClientCommandPort_udp(0);
        return (port > 0)? port : dftPort;
    }

    public int getClientCommandPort_tcp(int dftPort)
    {
        DCServerConfig dcsc = Main.getServerConfig(null);
        int port = dcsc.getClientCommandPort_tcp(0);
        if (port > 0) {
            return port;
        } else
        if (dftPort > 0) {
            return dftPort;
        } else {
            int tcp[] = dcsc.getTcpPorts();
            if (!ListTools.isEmpty(tcp)) {
                return tcp[0];
            } else {
                return 0;
            }
        }
    }

    // ------------------------------------------------------------------------

    public DCServerConfig.CommandProtocol getCommandProtocol()
    {
        DCServerConfig dcsc = Main.getServerConfig(null);
        DCServerConfig.CommandProtocol proto = dcsc.getCommandProtocol();
        return (proto != null)? proto : DCServerConfig.CommandProtocol.UDP;
    }

    // ------------------------------------------------------------------------

    /* callback from CommandPacketHandler */
    public DCServerFactory.ResultCode handleCommand(
        Device device, 
        String cmdType, String cmdName, String cmdArgs[])
    {
        Print.logInfo("Received Command: type=%s name=%s args=%s", cmdType, cmdName, StringTools.join(cmdArgs,','));

        /* no Device record? */
        if (device == null) {
            return DCServerFactory.ResultCode.INVALID_DEVICE;
        }

        /* default command */
        if (cmdType.equalsIgnoreCase(DCServerConfig.COMMAND_PING)) {
            cmdType = DCServerConfig.COMMAND_CONFIG;
            cmdName = "LocateNow";
        }

        /* custom command */
        DCServerConfig dcsc = Main.getServerConfig(null);
        DCServerConfig.Command command = dcsc.getCommand(cmdName);
        DCServerConfig.CommandProtocol cmdProto = null;
        String  cmdStr    = null;
        boolean expectAck = false;
        int     cmdStCode = StatusCodes.STATUS_NONE;
        if (command != null) {
            cmdProto  = command.getCommandProtocol();
            cmdStr    = command.getCommandString(device, cmdArgs);
            expectAck = command.getExpectAck();
            cmdStCode = command.getStatusCode();
        } else {
            Print.logWarn("Command not found: " + cmdName);
        }
        Print.logInfo("CmdStr: " + cmdStr);

        /* config command */
        if (cmdType.equalsIgnoreCase(DCServerConfig.COMMAND_CONFIG)) {
            if (command == null) {
                // command required
                return DCServerFactory.ResultCode.INVALID_COMMAND;
            }
            DCServerFactory.ResultCode result = this.sendCommand(device, cmdStr, cmdProto, cmdStCode);
            if (expectAck && (result != null) && result.isSuccess()) {
                device.setExpectCommandAck(command, cmdStr);
            }
            return result;
        }

        /* nothing matched */
        return null;
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* send AT command to device */
    private DCServerFactory.ResultCode sendCommand(Device device, 
        String cmdStr, DCServerConfig.CommandProtocol cmdProto,
        int cmdStCode)
    {

        /* no device */
        if (device == null) {
            Print.logError("Device is null");
            return DCServerFactory.ResultCode.INVALID_ARG;
        }

        /* protocol */
        if (cmdProto == null) {
            cmdProto = this.getCommandProtocol();
        }

        /* validate */
        if (StringTools.isBlank(cmdStr)) {
            Print.logError("Blank command: " + cmdStr);
            return DCServerFactory.ResultCode.INVALID_ARG;
        }

        /* send command per specified protocol */
        DCServerFactory.ResultCode result = DCServerFactory.ResultCode.INVALID_PROTO;
        switch (cmdProto) {
            case UDP:
                // send command via UDP
                result = this.sendCommandPacket_udp(device, cmdStr);
                break;
            case TCP:
                // send command via TCP
                if (SEND_TCP_COMMANDS_THROUGH_EXISTING_SESSION) {
                    // to existing TCP session
                    result = this.sendCommandPacket_tcp_existing(device, cmdStr);
                } else {
                    // to new TCP session
                    result = this.sendCommandPacket_tcp_new(device, cmdStr);
                }
                break;
            case SMS:
                /// send command via SMS
                //result = DCServerFactory.ResultCode.SUCCESS;
                SMSOutboundGateway gw = SMSOutboundGateway.GetSMSGateway("met");
                result = gw.sendSMSCommand(device, cmdStr);
                break;
            default:
                result = DCServerFactory.ResultCode.INVALID_PROTO;
                break;
        }

        /* insert event? */
        if (cmdStCode > 0) {
            
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

        /* return result */
        return result;

    }

    // ------------------------------------------------------------------------

    /* send via UDP */
    private DCServerFactory.ResultCode sendCommandPacket_udp(Device device, String cmdStr)
    {

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

        /* command packet */
        Payload p = new Payload();
        p.writeString(cmdStr, cmdStr.length());
        byte pktData[] = p.getBytes();

        /* send */
        Print.logInfo("Send '%s:%d'(%dsec) [%s] 0x%s", toHost, toPort, age,
            cmdStr, StringTools.toHexString(pktData));
        try {
            this.sendDatagramMessage(frPort, toHost, toPort, pktData);
            return DCServerFactory.ResultCode.SUCCESS;
        } catch (Throwable th) {
            Print.logException("UDP transmit failure", th);
            return DCServerFactory.ResultCode.TRANSMIT_FAIL;
        }

    }

    /* send UDP packet */
    private void sendDatagramMessage(int frPort, String toHost, int toPort, byte pkt[])
         throws IOException
    {

        /* get datagram socket */
        boolean closeSocket = false;
        DatagramSocket dgSocket = TrackServer.getInstance().getUdpDatagramSocket(frPort);
        if (dgSocket == null) {
            Print.logWarn("Creating temporary DatagramSocket for transmission");
            dgSocket = ServerSocketThread.createDatagramSocket(0);
            closeSocket = true;
        }

        /* datagram packet */
        DatagramPacket respPkt = new DatagramPacket(pkt, pkt.length, 
            InetAddress.getByName(toHost), toPort);

        /* send */
        Print.logInfo("Sending Datagram (from %d) to %s:%d> 0x%s", 
            dgSocket.getLocalPort(), toHost, toPort, StringTools.toHexString(pkt));
        dgSocket.send(respPkt);

        /* close */
        if (closeSocket) {
            dgSocket.close();
        }

    }

    // ------------------------------------------------------------------------

    /* send via TCP (existing session) */
    private DCServerFactory.ResultCode sendCommandPacket_tcp_existing(Device device, String cmdStr)
    {

        /* command packet */
        Payload p = new Payload();
        p.writeString(cmdStr, cmdStr.length());
        byte pktData[] = p.getBytes();
        Print.logDebug("TCP Command: %s", cmdStr);
        Print.logInfo("TCP Command packet: 0x" + StringTools.toHexString(pktData));

        /* init */
        DCServerConfig dcs = Main.getServerConfig(null);
        String sessID = DCServerFactory.getTcpSessionID(device);
        int    frPort = device.getListenPortCurrent();
        long   age    = DateTime.getCurrentTimeSec() - device.getLastTotalConnectTime();
        ServerSocketThread sst = TrackServer.getInstance().getServerSocketThread_tcp(frPort);
        if (sst != null) { 
            boolean rtn = sst.tcpWriteToSessionID(sessID,pktData);
            if (rtn) {
                return DCServerFactory.ResultCode.SUCCESS;
            } else {
                return DCServerFactory.ResultCode.NO_SESSION;
            }
        }

        /* send */
        Print.logError("TCP commands not supported for this port: " + frPort);
        return DCServerFactory.ResultCode.TRANSMIT_FAIL;

    }

    /* send via TCP (new session) */
    private DCServerFactory.ResultCode sendCommandPacket_tcp_new(Device device, String cmdStr)
    {

        /* init */
        String host = StringTools.trim(device.getIpAddressCurrent());
        int    port = this.getClientCommandPort_tcp(device.getRemotePortCurrent());

        /* validate host/port */
        if (StringTools.isBlank(host) || host.equals("0.0.0.0") || (port <= 0)) {
            Print.logError("Device host:port not known> %s:%d", host, port);
            return DCServerFactory.ResultCode.UNKNOWN_HOST;
        }

        /* command packet */
        Payload p = new Payload();
        p.writeString(cmdStr, cmdStr.length());
        byte pktData[] = p.getBytes();
        Print.logDebug("TCP Command: %s", cmdStr);
        Print.logInfo("TCP Command packet: 0x" + StringTools.toHexString(pktData));

        /* open TCP session to device */
        DCServerFactory.ResultCode rtnErr = DCServerFactory.ResultCode.SUCCESS;
        ClientSocketThread cst = new ClientSocketThread(host, port);
        cst.setReadTimeout(5000L);
        try {
            Print.logInfo("Opening TCP Socket @ %s:%d", host, port);
            cst.openSocket(5000L);
            Print.logInfo("TCP Socket open %s:%d", host, port);
            cst.socketWriteBytes(pktData);
            byte resp[] = cst.socketReadBytes(2); // response length;
            int respLen = (((int)resp[0] & 0xFF) << 8) | ((int)resp[1] & 0xFF);
            Print.logInfo("TCP Response length: " + respLen);
            cst.setSocketReadTimeout();
            resp = cst.socketReadBytes(respLen - 2);
            Print.logInfo("TCP Response packet: 0x" + StringTools.toHexString(resp));
            rtnErr = DCServerFactory.ResultCode.SUCCESS;
        } catch (IOException ioe) {
            Print.logError("TCP Socket error: " + ioe);
            rtnErr = DCServerFactory.ResultCode.TRANSMIT_FAIL;
        } finally {
            try { cst.closeSocket(); } catch (Throwable th) {/*ignore*/}
            Print.logInfo("TCP Socket closed ...");
        }

        /* return result */
        return rtnErr;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_ACCOUNT[]       = new String[] { "acct"  , "account"    };
    private static final String ARG_DEVICE[]        = new String[] { "dev"   , "device"     };
    private static final String ARG_TCP_COMMAND[]   = new String[] { "tcpCmd", "tcpCommand" };
    
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,false);
        String accountID = RTConfig.getString(ARG_ACCOUNT,null);
        String deviceID  = RTConfig.getString(ARG_DEVICE,null);

        /* account-id specified? */
        if (StringTools.isBlank(accountID)) {
            Print.logError("Account-ID not specified.");
            System.exit(1);
        }

        /* get account */
        Account account = null;
        try {
            account = Account.getAccount(accountID); // may throw DBException
            if (account == null) {
                Print.logError("Account-ID does not exist: " + accountID);
                System.exit(1);
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + accountID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* device-id specified? */
        if (StringTools.isBlank(deviceID)) {
            Print.logError("Device-ID not specified.");
            System.exit(1);
        }

        /* get device */
        Device device = null;
        try {
            device = Device.getDevice(account, deviceID, false); // may throw DBException
            if (device == null) {
                Print.logError("Device-ID does not exist: " + accountID + "/" + deviceID);
                System.exit(1);
            }
        } catch (DBException dbe) {
            Print.logError("Error getting Device: " + accountID + "/" + deviceID);
            dbe.printException();
            System.exit(99);
        }
        
        /* TCP session command */
        if (RTConfig.hasProperty(ARG_TCP_COMMAND)) {
            //String cmd = RTConfig.getString(ARG_TCP_COMMAND,"Hello World");
            device.setDeviceCode(Constants.DEVICE_CODE);
            RTProperties rtn = DCServerFactory.sendServerCommand(device, "config", "TcpTest", null);
            Print.sysPrintln("Result: " + rtn);
            System.exit(0);
        }

    }
    
}
