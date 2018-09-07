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
//  Sanav UDP data packet 'business' logic.
//  This modules parses client data packets and inserts them into the EventData table.
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
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.cellid.CellTower;

public class TrackClientPacketHandler
    extends AbstractClientPacketHandler
{

    // ------------------------------------------------------------------------

    public  static  String  UNIQUEID_PREFIX[]               = null;
    public  static  double  MINIMUM_SPEED_KPH               = Constants.MINIMUM_SPEED_KPH;
    public  static  boolean ESTIMATE_ODOMETER               = true;
    public  static  boolean SIMEVENT_GEOZONES               = true;
    public  static  double  MINIMUM_MOVED_METERS            = 0.0;
    public  static  boolean XLATE_LOCATON_INMOTION          = true;
    public  static  boolean SAVE_SESSION_STATISTICS         = true;

    public  static  boolean DEBUG_MODE                      = false;
    
    public  static  String  GS818_ACK                       = "ACK";

    // ------------------------------------------------------------------------

    private static  double  MAX_BATTERY_VOLTS               = 4.100; // 1100 maH
    private static  double  MIN_BATTERY_VOLTS               = 3.650;
    private static  double  RANGE_BATTERY_VOLTS             = MAX_BATTERY_VOLTS - MIN_BATTERY_VOLTS; // 0.45
    
    private static double CalcBatteryPercent(double voltage)
    {
        // formula obtained from Sanav
        double percent = (voltage - MIN_BATTERY_VOLTS) / RANGE_BATTERY_VOLTS;
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

    /* current device */
    private Device          device                          = null;
    private DataTransport   dataXPort                       = null;

    /* packet handler constructor */
    public TrackClientPacketHandler() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    /* callback when session is starting */
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText)
    {
        super.sessionStarted(inetAddr, isTCP, isText);
        super.clearTerminateSession();
    }

    /* callback when session is terminating */
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        super.sessionTerminated(err, readCount, writeCount);

        /* save session statistics */
        if (SAVE_SESSION_STATISTICS && !DEBUG_MODE && (this.device != null)) {
            this.device.insertSessionStatistic(this.getSessionStartTime(),this.getIPAddress(),
                this.isDuplex(),readCount,writeCount,this.getSavedEventCount());
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* based on the supplied packet data, return the remaining bytes to read in the packet */
    public int getActualPacketLength(byte packet[], int packetLen)
    {
        if (this.isDuplex()) {
            // TCP - there is no \r\n line terminator
            //Print.logInfo("TCP packet terminated by end of stream");
            return ServerSocketThread.PACKET_LEN_END_OF_STREAM; // Remainder is an ASCII packet - look for end of stream
        } else {
            // UDP - look for \r\n line terminator (which still may be non-existant)
            //Print.logInfo("UDP packet terminated by end of line terminator");
            return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR; // Remainder is an ASCII packet - look for line terminator
        }
    }

    // ------------------------------------------------------------------------

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) 
    {
        if (pktBytes == null) {
            Print.logError("Packet is null");
            return null;
        } else
        if (pktBytes.length < 12) {
            Print.logError("Unexpected packet length: " + pktBytes.length);
            return null;
        } else {
            String s = StringTools.toStringValue(pktBytes).trim();
            Print.logInfo("Recv: " + s); // debug message
            return this.parseInsertRecord(s);
        }
    }

    // ------------------------------------------------------------------------

    /* parse status code */
    private int parseStatusCode(String evCode)
    {
        String code = StringTools.trim(evCode).toUpperCase();

        /* prefixing "B" means that the event was stored in flash */
        if (code.startsWith("B")) {
            if (code.startsWith("B-")) {
                code = code.substring(2); // remove "B-"
            } else {
                code = code.substring(1); // remove "B"
            }
        }
        int codeLen = code.length();
        
        /* default status code */
        int statusCode = StatusCodes.STATUS_LOCATION; // default

        /* translate code */
        DCServerConfig dcs = Main.getServerConfig();
        if (dcs != null) {
            int sc = dcs.translateStatusCode(code, -9999);
            if (sc >= 0) {
                return sc;
            }
        }

        /* translate event code string to status code */
        if (codeLen == 0) {
            statusCode = StatusCodes.STATUS_LOCATION;
        } else
        if (code.startsWith("0X")) {
            // explicit hex status code definition
            statusCode = StringTools.parseInt(code,StatusCodes.STATUS_LOCATION);
        } else
        if (code.equals("AUTO")) {
            // periodic event
            statusCode = StatusCodes.STATUS_LOCATION;
        } else 
        if (code.equals("SOS")) {
            // panic button
            statusCode = StatusCodes.STATUS_WAYMARK_0; // StatusCodes.STATUS_PANIC_ON;
        } else
        if (code.equals("MOVE")) {
            // device is moving?
            statusCode = StatusCodes.STATUS_MOTION_MOVING;
        } else 
        if (code.equals("POLL")) {
            // response to "Locate Now"
            statusCode = StatusCodes.STATUS_QUERY;
        } else
        if (code.equals("GIN")) {
            // Geofence arrive
            statusCode = StatusCodes.STATUS_GEOFENCE_ARRIVE;
        } else
        if (code.equals("GOUT")) {
            // Geofence depart
            statusCode = StatusCodes.STATUS_GEOFENCE_DEPART;
        } else
        if (code.startsWith("GF")) {
            if (code.endsWith("IN")) {
                // Geofence arrive
                statusCode = StatusCodes.STATUS_GEOFENCE_ARRIVE;
            } else
            if (code.endsWith("OUT")) {
                // Geofence depart
                statusCode = StatusCodes.STATUS_GEOFENCE_DEPART;
            } else {
                // should not occur
                statusCode = StatusCodes.STATUS_GEOFENCE_VIOLATION;
            }
        } else
        if (code.equals("PARK")) {
            // parked
            statusCode = StatusCodes.STATUS_PARKED;
        } else
        if (code.equals("UNPARK") || code.equals("UNPA")) {
            // unparked
            statusCode = StatusCodes.STATUS_UNPARKED;
        } else
        if (code.equals("START")) {
            // start?
            statusCode = StatusCodes.STATUS_LOCATION;
        } else
        if (code.equals("ACCON")) {
            // accessory on (assume ignition)
            statusCode = StatusCodes.STATUS_IGNITION_ON;
        } else
        if (code.equals("ACCOFF")) {
            // accessory off (assume ignition)
            statusCode = StatusCodes.STATUS_IGNITION_OFF;
        } else
        if (code.equals("LP")) {
            // Low power
            statusCode = StatusCodes.STATUS_LOW_BATTERY;
        } else
        if (code.equals("DC")) {
            // lost power ??
            statusCode = StatusCodes.STATUS_POWER_FAILURE; // ???
        } else
        if (code.equals("CH")) {
            // charging?
            statusCode = StatusCodes.STATUS_POWER_RESTORED; // charging?
        } else
        if (code.equals("OPEN")) { 
            // on normally "open" switch (provided by Sanav), this is alarm "ON"
            statusCode = StatusCodes.InputStatusCodes_ON[0];
        } else
        if (code.equals("CLOSE")) { 
            // on normally "open" switch (provided by Sanav), this is alarm "OFF"
            statusCode = StatusCodes.InputStatusCodes_OFF[0];
        } else
        if (code.startsWith("ALARM") && (codeLen >= 6)) { // "ALARM1" .. "ALARM6"
            // "ALARM1" ==> StatusCodes.STATUS_INPUT_ON_01
            int ndx = (code.charAt(5) - '0'); // will be 1..6 ('0' not used here)
            if ((ndx >= 0) && (ndx <= 9) && (ndx < StatusCodes.InputStatusCodes_ON.length)) {
                statusCode = StatusCodes.InputStatusCodes_ON[ndx];
            } else {
                statusCode = StatusCodes.STATUS_INPUT_ON;
            }
        } else
        if (code.equals("STATIONARY")) {
            // not moving
            statusCode = StatusCodes.STATUS_MOTION_DORMANT; // not moving
        } else
        if (code.equals("VIBRATION")) {
            // device was 'shaken'
            statusCode = StatusCodes.STATUS_VIBRATION_ON;
        } else 
        if (code.equals("OVERSPEED")) {
            // over speed
            statusCode = StatusCodes.STATUS_MOTION_EXCESS_SPEED;
        } else 
        {
            // GS-818: "code" could contain barcode data
            statusCode = StatusCodes.STATUS_LOCATION;
        }
        return statusCode;

    }

    /* parse and insert data record */
    private byte[] parseInsertRecord(String s)
    {
        // --
        // imei=471923002250245&rmc=$GPRMC,023000.000,A,3130.0577,N,14271.7421,W,0.53,208.37,210507,,*19,AUTO
        // -- 
        // GS-818: all fields
        // imei=753197040181023&rmc=$GPRMC,185319.000,A,3155.4786,N,14204.0111,W,0.00,0.90,091011,,*13,ACCoff,0300,2.1,05,712,01,0000,13F9,64,712,01,2ABD,13F9,67,712,01,2A13,1400,79,712,01,0000,13F9,82,712,01,0000,13F9,82,712,01,29CF,1400,83,712,01,75D3,1400,84,1,24,0.00,0.00,35.67
        // imei=753197040181023&rmc=$GPRMC,191458.000,A,3155.4832,N,14203.9914,W,0.00,0.00,091011,,*17,AUTO  ,0300,1.2,07,712,01,2848,13F9,63,712,01,2ABD,13F9,68,712,01,75D3,1400,82,712,01,29CF,1400,83,712,01,2847,13F9,83,712,01,2A15,1400,87,712,01,27B1,13F9,87,0,25,0.00,0.00,35.11
        // imei=753197040181023&rmc=$GPRMC,193703.000,A,3155.4643,N,14204.0112,W,0.00,0.00,091011,,*10,AUTO  ,0300,1.2,07,712,01,2848,13F9,62,712,01,2ABD,13F9,70,712,01,75D3,1400,80,712,01,29CF,1400,84,712,01,2847,13F9,83,712,01,2A15,1400,85,712,01,29EB,13FA,89,0,26,0.00,0.00,35.11,ACK0001
        // 0------------------- 1--------------------------------------------------------------------- 2----- 3--- 4-- 5- 6------------------ 7------------------ 8------------------ 9------------------ A------------------ B------------------ C------------------ D E- F--- H--- H---- J------
        // |                    |                                                                      |      |    |   |  |                   |                   |                   |                   |                   |                   |                   | |  |    |    |     |> ACK Ser#
        // |                    |                                                                      |      |    |   |  |                   |                   |                   |                   |                   |                   |                   | |  |    |    |> Temp C
        // |                    |                                                                      |      |    |   |  |                   |                   |                   |                   |                   |                   |                   | |  |    |> Analog #2
        // |                    |                                                                      |      |    |   |  |                   |                   |                   |                   |                   |                   |                   | |  |> Analog #1
        // |                    |                                                                      |      |    |   |  |                   |                   |                   |                   |                   |                   |                   | |> RSSI
        // |                    |                                                                      |      |    |   |  |                   |                   |                   |                   |                   |                   |                   |> TA
        // |                    |                                                                      |      |    |   |  |                   |                   |                   |                   |                   |                   |> CellTower #6
        // |                    |                                                                      |      |    |   |  |                   |                   |                   |                   |                   |> CellTower #5
        // |                    |                                                                      |      |    |   |  |                   |                   |                   |                   |> CellTower #4
        // |                    |                                                                      |      |    |   |  |                   |                   |                   |> CellTower #3
        // |                    |                                                                      |      |    |   |  |                   |                   |> CellTower #2
        // |                    |                                                                      |      |    |   |  |                   |> CellTower #1
        // |                    |                                                                      |      |    |   |  |> CellTower #0
        // |                    |                                                                      |      |    |   |> Sat Count
        // |                    |                                                                      |      |    |> HDOP
        // |                    |                                                                      |      |> DigInp: battery, power, etc 
        // |                    |                                                                      |> StatusCode
        // |                    |> GPRMC
        // |> MobileID
        
        /* pre-validate */
        if (StringTools.isBlank(s)) {
            Print.logError("String is null");
            return null;
        }

        /* first parse on '&', then try ';' */
        String imeiField = "";
        String fmiField  = "";
        String dataField = "";
        {
            int p = s.indexOf("&");
            if (p < 0) { 
                p = s.indexOf(";"); 
                if (p < 0) {
                    Print.logError("No data");
                    return null;
                }
            }
            imeiField = s.substring(0,p);
            dataField = s.substring(p+1);   // "rmc="
            int e = imeiField.indexOf(",");
            if (e >= 0) {
                fmiField  = imeiField.substring(e+1);   // "OK", "FAIL", "FMI="
                imeiField = imeiField.substring(0,e);   // "imei=
            }
        }

        /* IMEI/mobileId */
        String imei = "";
        if (imeiField.startsWith("imei=")) {
            imei = imeiField.substring(5).trim();
        } else
        if (imeiField.startsWith("id=")) {
            imei = imeiField.substring(3).trim();
        }
        if (StringTools.isBlank(imei)) {
            Print.logError("Missing IMEI/MobileID");
            return null;
        }

        /* search for "ACK####" */
        byte ackPkt[] = null;
        int ackPos = dataField.indexOf(GS818_ACK);
        if (ackPos >= 0) {
            int ackS = ackPos + GS818_ACK.length();
            int ackE = ackS + 4; // length of hex serial#
            if (ackE > dataField.length()) { ackE = dataField.length(); }
            String serialNum = dataField.substring(ackS,ackE);
            String ackStr = "@PCack," + imei + "," + serialNum + "\r\n";
            Print.logInfo("ACK Packet: " + ackStr.trim());
            ackPkt = ackStr.getBytes();
        }

        /* find Device */
        this.device = DCServerFactory.loadDeviceByPrefixedModemID(UNIQUEID_PREFIX, imei);
        if (this.device == null) {
            return ackPkt; // errors already displayed
        }
        String accountID = this.device.getAccountID();
        String deviceID  = this.device.getDeviceID();
        Print.logInfo("UniqueID  : " + this.device.getUniqueID());
        Print.logInfo("DeviceID  : " + accountID + "/" + deviceID);

        /* check IP address */
        this.dataXPort = this.device.getDataTransport();
        if (this.hasIPAddress() && !this.dataXPort.isValidIPAddress(this.getIPAddress())) {
            DTIPAddrList validIPAddr = this.dataXPort.getIpAddressValid(); // may be null
            Print.logError("Invalid IP Address from device: " + this.getIPAddress() + " [expecting " + validIPAddr + "]");
            return ackPkt;
        }
        this.dataXPort.setIpAddressCurrent(this.getIPAddress());    // FLD_ipAddressCurrent
        this.dataXPort.setRemotePortCurrent(this.getRemotePort());  // FLD_remotePortCurrent
        this.dataXPort.setListenPortCurrent(this.getLocalPort());       // FLD_listenPortCurrent
        this.dataXPort.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime
        if (!this.dataXPort.getDeviceCode().equalsIgnoreCase(Constants.DEVICE_CODE)) {
            this.dataXPort.setDeviceCode(Constants.DEVICE_CODE); // FLD_deviceCode
        }

        /* fields */
        long      fixtime     = 0L;
        boolean   validGPS    = false;
        double    latitude    = 0.0;
        double    longitude   = 0.0;
        GeoPoint  geoPoint    = GeoPoint.INVALID_GEOPOINT;
        long      gpsAge      = 0L;
        double    speedKPH    = 0.0;
        double    headingDeg  = 0.0;
        double    altitudeM   = 0.0;
        double    odomKM      = 0.0;
        int       statusCode  = StatusCodes.STATUS_LOCATION;
        double    batteryV    = 0.0;
        long      gpioInput   = 0L;
        double    HDOP        = 0.0;
        int       numSats     = 0;
        int       RSSI        = 0;
        double    analog1     = 0.0;
        double    analog2     = 0.0;
        double    boardTempC  = 0.0;
        CellTower cellTower[] = null;

        /* GS-818: Garmin FMI */
        if (fmiField.equalsIgnoreCase("OK")) {
            // GS-818 "OK"
        } else
        if (fmiField.equalsIgnoreCase("FAIL")) {
            // GS-818 "FAIL"
        } else
        if (fmiField.startsWith("FMI=")) {
            // GS-818 "FMI=" : <DLE:1> <PackedID:1> <PacketLen:1> <Data:*> <Chksum:1> <DLE:1> <EOT:1>
            byte DLE    = (byte)0x10;
            byte fmiB[] = StringTools.parseHex(fmiField.substring(4),null);
            if (ListTools.size(fmiB) >= 6) {
                // "Un-stuff"
                // (unknown whether the GS-818 already performs this "unstuffing" before sending the packet
                byte fmiBU[]  = new byte[fmiB.length];
                int  fmiBULen = 0;
                fmiBU[fmiBULen++] = fmiB[0];
                for (int i = 1; i < fmiB.length - 2; i++) {
                    fmiBU[fmiBULen++] = fmiB[i];
                    if (fmiB[i] == DLE) { 
                        if (fmiB[i+1] == DLE) {
                            i++;
                        } else {
                            Print.logWarn("Expected DLE, found 0x" + StringTools.toHexString(fmiB[i+1]));
                        }
                    }
                }
                fmiBU[fmiBULen++] = fmiB[fmiB.length - 2];
                fmiBU[fmiBULen++] = fmiB[fmiB.length - 1];
                // parse FMI packet
                Print.logInfo("FMI Packet: 0x" + StringTools.toHexString(fmiBU));
                Payload fmi = new Payload(fmiBU,0,fmiBULen);
                byte DLE_1     = (byte)fmi.readUInt(1,0,"FMI:DLE-1");
                int  pktID     = fmi.readUInt(      1,0,"FMI:PacketID");
                int  pktLen    = fmi.readUInt(      1,0,"FMI:PackeLen");
                byte pktData[] = fmi.readBytes(pktLen,  "FMI:Data");   // Note: Little-Endian format!!!
                int  chksum    = fmi.readUInt(      1,0,"FMI:ChkSum"); // byte 1..(n-4)
                byte DLE_2     = (byte)fmi.readUInt(1,0,"FMI:DLE-2");
                byte EOT       = (byte)fmi.readUInt(1,0,"FMI:EOT");
                if (fmi.getAvailableReadLength() > 0) {
                    Print.logWarn("Unexpected bytes remaining in FMI payload: " + fmi.getAvailableReadLength());
                } else
                if ((DLE_1 != DLE) || (DLE_2 != DLE) || (EOT != (byte)0x03)) {
                    Print.logWarn("Unexpected FMI packet framing");
                }
                // refer to the following Garmin manuals for additional parsing information:
                // - Garmin Fleet Management Interface Control Specification [001-00096-00_0F_web.pdf]
                // - Garmin FMI Control Specification
            } else {
                Print.logWarn("Unexpected FMI packet length: 0x" + StringTools.toHexString(fmiB));
            }
        }

        /* GPRMC */
        if (dataField.startsWith("rmc=")) {
            Nmea0183 gprmc = null;
            String gprmcStr = dataField.substring(4);
            if (gprmcStr.startsWith("$GPRMC")) {
                gprmc = new Nmea0183(gprmcStr, true);
            } else
            if (gprmcStr.startsWith("GPRMC")) {
                gprmc = new Nmea0183("$" + gprmcStr, true);
            } else {
                Print.logError("Missing '$GPRMC'");
                return ackPkt;
            }
            fixtime     = gprmc.getFixtime();
            validGPS    = gprmc.isValidGPS();
            latitude    = validGPS? gprmc.getLatitude()  : 0.0;
            longitude   = validGPS? gprmc.getLongitude() : 0.0;
            geoPoint    = new GeoPoint(latitude, longitude);
            speedKPH    = validGPS? gprmc.getSpeedKPH()  : 0.0;
            headingDeg  = validGPS? gprmc.getHeading()   : 0.0;
            // ExtraData: status code, battery volts, etc
            String extraData[] = gprmc.getExtraData();
            // CODE[-XXXXmv]
            // "3722mV,VIBRATION,..."
            // "AUTO-3893mv"
            if (!ListTools.isEmpty(extraData)) {
                Print.logInfo("Extra data: " + StringTools.join(extraData,','));
                if ((extraData[0].length() > 0) && Character.isDigit(extraData[0].charAt(0))) {
                    // "3893mV,VIBRATION"
                    batteryV    = StringTools.parseDouble(extraData[0],0.0) / 1000.0;
                    statusCode  = (extraData.length > 1)? this.parseStatusCode(extraData[1]) : StatusCodes.STATUS_LOCATION;
                } else {
                    // "AUTO" or "AUTO-3893mv"
                    int ep = extraData[0].indexOf('-');
                    String stat = (ep >= 0)? extraData[0].substring(0,ep) : extraData[0];
                    String batt = (ep >= 0)? extraData[0].substring(ep+1) : null;
                    statusCode  = this.parseStatusCode(stat);
                    batteryV    = StringTools.parseDouble(batt,0.0) / 1000.0;
                    // GS-818 may have additional fields here:
                    //  0:1 "AUTO"  StatusCode
                    //  1:1 "0300"  Hex GPIO (battery, power, etc)
                    //  2:1 "2.1"   HDOP
                    //  3:1 "10"    # Satellites
                    //  4:5 "466,97,34E7,3391,74"   #1 CellID,Strength,etc ...
                    //  9:5 "466,97,3F2D,3391,65"   #2 CellID,Strength,etc ...
                    // 14:5 "466,97,39C9,3391,79"   #3 CellID,Strength,etc ...
                    // 19:5 "466,97,3F2C,3391,81"   #4 CellID,Strength,etc ...
                    // 24:5 "466,97,0000,0000,83"   #5 CellID,Strength,etc ...
                    // 29:5 "466,97,0000,0000,85"   #6 CellID,Strength,etc ...
                    // 34:4 "466,97,0000,0000,85"   #7 CellID,Strength,etc ...
                    // 39:1 "1"     TA ??? (timing-advance?)
                    // 40:1 "24"    RSSI
                    gpioInput   = (extraData.length >  1)? StringTools.parseHexLong(extraData[ 1], 0L) :  0L; 
                    // bit0  Input1 status 
                    // bit1  Input2 status
                    // bit2  Input3 status
                    // bit3  Input4 status
                    // bit4  Input5 status
                    // bit5  ACC Status
                    // bit6  Output1
                    // bit7  Output2
                    // bit8  Power source (0=no external power; 1=external power source)
                    // bit9  Battery status (0=power is low; 1=power is normal.)
                    // bit10 reserve
                    // bit11 reserve
                    // bit12 reserve
                    // bit13 reserve
                    // bit14 reserve
                    // bit15 reserve

                    HDOP        = (extraData.length >  2)? StringTools.parseDouble( extraData[ 2],0.0) : 0.0; 
                    numSats     = (extraData.length >  3)? StringTools.parseInt(    extraData[ 3],  0) :   0;
                    if (extraData.length > 39) {
                        Vector<CellTower> ctList = new Vector<CellTower>();
                        for (int c = 4, x = 0; c <= 34; c += 5, x++) {
                            int MCC = (extraData.length > (c+0))? StringTools.parseInt(   extraData[c+0],  0) :   0; 
                            int MNC = (extraData.length > (c+1))? StringTools.parseInt(   extraData[c+1],  0) :   0; 
                            int LAC = (extraData.length > (c+2))? StringTools.parseHexInt(extraData[c+2],  0) :   0; 
                            int CID = (extraData.length > (c+3))? StringTools.parseHexInt(extraData[c+3],  0) :   0; 
                            int RxL = (extraData.length > (c+4))? StringTools.parseInt(   extraData[c+4],  0) :   0; 
                            ctList.add(new CellTower(MCC, MNC, -1/*TAV*/, CID, LAC, -1/*ARFCN*/, RxL));
                        }
                        if (ctList.size() > 0) {
                            int tav = (extraData.length >    39)? StringTools.parseInt(   extraData[ 39],  0) :   0; 
                            cellTower = ctList.toArray(new CellTower[ctList.size()]);
                            cellTower[0].setTimingAdvance(tav);
                        }
                    }
                    RSSI        = (extraData.length > 40)? StringTools.parseInt(    extraData[40],  0) :   0; 
                    analog1     = (extraData.length > 41)? StringTools.parseDouble( extraData[41],0.0) : 0.0; 
                    analog2     = (extraData.length > 42)? StringTools.parseDouble( extraData[42],0.0) : 0.0; 
                    boardTempC  = (extraData.length > 43)? StringTools.parseDouble( extraData[43],0.0) : 0.0; 
                }
            }
        } else
        if (dataField.startsWith("data=")) {
            // %Timestamp,StatusCode,Latitude,Longitude,Speed,Heading,Altitude,GpsAge,HDOP,SatCount
            //  0         1          2        3         4     5       6        7      8    9
            String dataStr = dataField.substring(5);
            String fld[] = StringTools.split(dataStr,',');
            fixtime     =  (fld.length > 0)? StringTools.parseLong(  fld[0], 0L) :  0L;
            statusCode  =  (fld.length > 1)? this.parseStatusCode(   fld[1]    ) : StatusCodes.STATUS_LOCATION;
            latitude    =  (fld.length > 2)? StringTools.parseDouble(fld[2],0.0) : 0.0;
            longitude   =  (fld.length > 3)? StringTools.parseDouble(fld[3],0.0) : 0.0;
            validGPS    = GeoPoint.isValid(latitude,longitude);
            geoPoint    = validGPS? new GeoPoint(latitude,longitude) : GeoPoint.INVALID_GEOPOINT;
            speedKPH    = ((fld.length > 4) && validGPS)? StringTools.parseDouble(fld[4],0.0) : 0.0;
            headingDeg  = ((fld.length > 5) && validGPS)? StringTools.parseDouble(fld[5],0.0) : 0.0;
            altitudeM   = ((fld.length > 6) && validGPS)? StringTools.parseDouble(fld[6],0.0) : 0.0;
            gpsAge      = ((fld.length > 7) && validGPS)? StringTools.parseLong  (fld[7], 0L) :  0L;
            HDOP        = ((fld.length > 8) && validGPS)? StringTools.parseDouble(fld[8],0.0) : 0.0;
            numSats     = ((fld.length > 9) && validGPS)? StringTools.parseInt   (fld[9],  0) :   0;
        } else {
            Print.logError("Missing 'rmc='");
            return ackPkt;
        }

        /* battery level (percent) */
        double batteryLvl = CalcBatteryPercent(batteryV);

        /* status code check for motion */
        if (statusCode == StatusCodes.STATUS_NONE) {
            statusCode = (speedKPH > 0.0)? StatusCodes.STATUS_MOTION_IN_MOTION : StatusCodes.STATUS_LOCATION;
        } else
        if (XLATE_LOCATON_INMOTION && (statusCode == StatusCodes.STATUS_LOCATION) && (speedKPH > 0.0)) {
            statusCode = StatusCodes.STATUS_MOTION_IN_MOTION;
        }

        /* display */
        Print.logInfo("Timestamp : " + fixtime + " [" + new DateTime(fixtime) + "]");
        Print.logInfo("GPS       : " + geoPoint + " [" + numSats + "]");
        Print.logInfo("Speed     : " + StringTools.format(speedKPH,"#0.0") + " kph " + headingDeg);
        Print.logInfo("Altitude  : " + StringTools.format(altitudeM,"#0.0") + " meters");
        Print.logInfo("Battery%  : " + (batteryLvl*100.0) + " %  ["+batteryV+" volts]");
        Print.logInfo("gpioInput : 0x" + StringTools.toHexString(gpioInput,16));
        Print.logInfo("HDOP      : " + HDOP);
        Print.logInfo("RSSI      : " + RSSI);

        /* reject invalid GPS fixes? */
        if (!validGPS && (statusCode == StatusCodes.STATUS_LOCATION)) {
            // ignore invalid GPS fixes that have a simple 'STATUS_LOCATION' status code
            Print.logWarn("Ignoring event with invalid latitude/longitude");
            return ackPkt;
        }

        /* adjustments to received values */
        if (speedKPH < MINIMUM_SPEED_KPH) {
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) {
            headingDeg = 0.0;
        }

        /* updated Device attributes */
        this.dataXPort.setIpAddressCurrent(this.getIPAddress()); // FLD_ipAddressCurrent
        this.dataXPort.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime
        if (!this.dataXPort.getDeviceCode().equalsIgnoreCase(Constants.DEVICE_CODE)) {
            this.dataXPort.setDeviceCode(Constants.DEVICE_CODE); // FLD_deviceCode
        }

        /* odometer */
        if (this.device != null) {
            odomKM = this.device.calculateOdometerKM(odomKM, fixtime, validGPS, geoPoint,
                ESTIMATE_ODOMETER, true/*log*/);
        }

        /* simulate Geozone arrival/departure */
        if (SIMEVENT_GEOZONES && validGPS && (this.device != null)) {
            java.util.List<Device.GeozoneTransition> zone = this.device.checkGeozoneTransitions(fixtime, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    this.insertEventRecord(this.device, 
                        z.getTimestamp(), z.getStatusCode(), z.getGeozone(),
                        geoPoint, HDOP, numSats, RSSI,
                        gpioInput, batteryLvl,
                        speedKPH, headingDeg, 
                        altitudeM, odomKM,
                        analog1, analog2, boardTempC,
                        cellTower);
                    Print.logInfo("Geozone    : " + z);
                    if (z.getStatusCode() == statusCode) {
                        // suppress 'statusCode' event if we just added it here
                        Print.logDebug("StatusCode already inserted: 0x" + StatusCodes.GetHex(statusCode));
                        statusCode = StatusCodes.STATUS_IGNORE;
                    }
                }
            }
        }

        /* status code checks */
        if (statusCode < 0) { // StatusCodes.STATUS_IGNORE
            // skip (event ignored)
        } else
        if (statusCode == StatusCodes.STATUS_IGNORE) {
            // skip (event ignored)
        } else
        if ((statusCode == StatusCodes.STATUS_LOCATION) && this.hasSavedEvents()) {
            // skip (already inserted an event)
        } else
        if (statusCode != StatusCodes.STATUS_LOCATION) {
            this.insertEventRecord(this.device, 
                fixtime, statusCode, null/*Geozone*/,
                geoPoint, HDOP, numSats, RSSI,
                gpioInput, batteryLvl,
                speedKPH, headingDeg, 
                altitudeM, odomKM,
                analog1, analog2, boardTempC,
                cellTower);
        } else
        if (validGPS && (this.device != null) && 
            !this.device.isNearLastValidLocation(geoPoint,MINIMUM_MOVED_METERS)) {
            this.insertEventRecord(this.device, 
                fixtime, statusCode, null/*Geozone*/,
                geoPoint, HDOP, numSats, RSSI,
                gpioInput, batteryLvl,
                speedKPH, headingDeg, 
                altitudeM, odomKM,
                analog1, analog2, boardTempC,
                cellTower);
        }

        /* save device changes */
        if (!DEBUG_MODE && (this.device != null)) {
            try {
                //DBConnection.pushShowExecutedSQL();
                this.device.updateChangedEventFields();
            } catch (DBException dbe) {
                Print.logException("Unable to update Device: " + accountID + "/" + deviceID, dbe);
            } finally {
                //DBConnection.popShowExecutedSQL();
            }
        }

        return ackPkt;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the EventData record previous to the specified fixtime
    *** @param device  The Device record handle
    *** @param fixtime The current event fixtime
    *** @return The previous event, or null if there is no previous event
    **/
    /*
    private EventData getPreviousEventData(Device device, long fixtime)
    {
        try {
            long startTime = -1L;
            long endTime   = fixtime - 1L;
            EventData ed[] = EventData.getRangeEvents(
                device.getAccountID(), device.getDeviceID(),
                startTime, endTime,
                null, // statusCodes
                true, // validGPS
                EventData.LimitType.LAST, 1, true,
                null); // additionalSelect
            if ((ed != null) && (ed.length > 0)) {
                return ed[0];
            } else {
                return null;
            }
        } catch (DBException dbe) {
            return null;
        }
    }
    */

    // ------------------------------------------------------------------------
    
    private EventData createEventRecord(Device device, 
        long      fixtime, int statusCode, Geozone geozone,
        GeoPoint  geoPoint, double HDOP, int numSats, int RSSI,
        long      gpioInput, double batteryLevel,
        double    speedKPH, double heading, 
        double    altitude, double odomKM,
        double    analog1, double analog2, double tempC,
        CellTower cellTower[])
    {
        String accountID    = device.getAccountID();
        String deviceID     = device.getDeviceID();
        EventData.Key evKey = new EventData.Key(accountID, deviceID, fixtime, statusCode);
        EventData evdb      = evKey.getDBRecord();
        evdb.setGeozone(geozone);
        evdb.setGeoPoint(geoPoint);
        evdb.setHDOP(HDOP);
        evdb.setSatelliteCount(numSats);
        evdb.setSignalStrength((double)RSSI);
        evdb.setInputMask(gpioInput);
        evdb.setBatteryLevel(batteryLevel);
        evdb.setSpeedKPH(speedKPH);
        evdb.setHeading(heading);
        evdb.setAltitude(altitude);
        evdb.setOdometerKM(odomKM);
        // analog data
        evdb.setAnalog0(analog1);
        evdb.setAnalog1(analog2);
        evdb.setThermoAverage0(tempC);
        // Cell-Tower data
        if (!ListTools.isEmpty(cellTower)) {
            evdb.setServingCellTower(cellTower[0]);
            for (int c = 1; c < cellTower.length; c++) {
                evdb.setNeighborCellTower(c - 1, cellTower[c]);
            }
        }
        return evdb;
    }

    /* create and insert an event record */
    private void insertEventRecord(Device device, 
        long      fixtime, int statusCode, Geozone geozone,
        GeoPoint  geoPoint, double HDOP, int numSats, int RSSI,
        long      gpioInput, double batteryLevel,
        double    speedKPH, double heading, 
        double    altitude, double odomKM,
        double    analog1, double analog2, double tempC,
        CellTower cellTower[])
    {

        /* create event */
        EventData evdb = this.createEventRecord(device, 
            fixtime, statusCode, geozone,
            geoPoint, HDOP, numSats, RSSI,
            gpioInput, batteryLevel,
            speedKPH, heading, 
            altitude, odomKM,
            analog1, analog2, tempC,
            cellTower);

        /* insert event */
        // this will display an error if it was unable to store the event
        Print.logInfo("Event     : [0x" + StringTools.toHexString(statusCode,16) + "] " + StatusCodes.GetDescription(statusCode,null));
        device.insertEventData(evdb);
        this.incrementSavedEventCount();

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void configInit() 
    {

        // common
        DCServerConfig dcsc     = Main.getServerConfig();
        if (dcsc != null) {
        UNIQUEID_PREFIX         = dcsc.getUniquePrefix();
        MINIMUM_SPEED_KPH       = dcsc.getMinimumSpeedKPH(MINIMUM_SPEED_KPH);
        ESTIMATE_ODOMETER       = dcsc.getEstimateOdometer(ESTIMATE_ODOMETER);
        SIMEVENT_GEOZONES       = dcsc.getSimulateGeozones(SIMEVENT_GEOZONES);
        MINIMUM_MOVED_METERS    = dcsc.getMinimumMovedMeters(MINIMUM_MOVED_METERS);
        XLATE_LOCATON_INMOTION  = dcsc.getStatusLocationInMotion(XLATE_LOCATON_INMOTION);
        SAVE_SESSION_STATISTICS = dcsc.getSaveSessionStatistics(SAVE_SESSION_STATISTICS);
        }

    }

}
