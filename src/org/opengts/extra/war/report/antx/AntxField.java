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
package org.opengts.extra.war.report.antx;

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

import org.opengts.extra.tables.Antx;
    
public class AntxField
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // EventCode definitions
    
    public static final int     EVCODE_DEMAND_END_OF_DAY        = 104;  // On-Demand End-Of-Day Report
    public static final int     EVCODE_DEMAND_STANDARD          = 105;  // On-Demand Standard Report
    public static final int     EVCODE_STANDARD_REPORT          = 474;  // Periodic Report
    public static final int     EVCODE_END_OF_DAY               = 475;  // End-Of-Day Report
    public static final int     EVCODE_COG_HEADING              = 476;  // Course Change Report
    public static final int     EVCODE_SOG_SPEED                = 477;  // Accelerated Speed Report
    public static final int     EVCODE_RPM                      = 478;  // Accelerated RPM Report
    public static final int     EVCODE_POWER_ON                 = 480;  // Power-On Report
    public static final int     EVCODE_ENGINE_START             = 481;  // Engine Start Detected Report
    public static final int     EVCODE_ENGINE_STOP              = 482;  // Engine Stop Detected Report
    public static final int     EVCODE_POWER_OFF                = 484;  // Power-Off Report
    public static final int     EVCODE_DECELERATION             = 485;  // Hard Deceleration Report
    public static final int     EVCODE_ACCELERATION             = 486;  // Hard Acceleration Report
    public static final int     EVCODE_OUTPUT_CTL_ON            = 487;  // Output Control Set ON
    public static final int     EVCODE_OUTPUT_CTL_OFF           = 488;  // Output Control Set OFF
    public static final int     EVCODE_OFF_TO_IDLE              = 501;  // Transition
    public static final int     EVCODE_OFF_TO_WORK              = 502;  // Transition
    public static final int     EVCODE_IDLE_TO_OFF              = 503;  // Transition
    public static final int     EVCODE_IDLE_TO_WORK             = 504;  // Transition
    public static final int     EVCODE_WORK_TO_OFF              = 505;  // Transition
    public static final int     EVCODE_WORK_TO_IDLE             = 506;  // Transition
    public static final int     EVCODE_CONFIG_SET_OK            = 901;  // OTA Config 'set' Response
    public static final int     EVCODE_CONFIG_SET_ERROR         = 902;  // OTA Config 'set' Response
    public static final int     EVCODE_CONFIG_GET_OK            = 903;  // OTA Config 'get' Response
    public static final int     EVCODE_CONFIG_GET_ERROR         = 904;  // OTA Config 'get' Response
    public static final int     EVCODE_ACTION_CMD_OK            = 905;  // OTA Action Command Response
    public static final int     EVCODE_ACTION_CMD_ERROR         = 906;  // OTA Action Command Response
    public static final int     EVCODE_CONFIG_CHANGE            = 910;  // ??? config change? (always ACK)
    public static final int     EVCODE_GPS_ANT_UNAVAIL          = 956;  // GPS Antenna Unavailable
    public static final int     EVCODE_GPS_ANT_NORMAL           = 957;  // GPS Antenna Normal
    public static final int     EVCODE_GPS_ANT_OPEN             = 958;  // GPS Antenna Open
    public static final int     EVCODE_GPS_ANT_SHORT            = 959;  // GPS Antenna Short
    public static final int     EVCODE_DIAG_CODE_ACTIVE         = 961;  // An active diagnostic event
    public static final int     EVCODE_DIAG_CODE_NORMAL         = 962;  // A diagnostic event is no longer active
    public static final int     EVCODE_IDLE_EXCEPTION           = 978;  // A continuous idle period has exceeded the set threshold

    public static int getStatusCodeFromEventCode(int evCode)
    {
        switch (evCode) {
            case EVCODE_DEMAND_END_OF_DAY   : return StatusCodes.STATUS_DAY_SUMMARY;            //   104;  // On-Demand End-Of-Day Report
            case EVCODE_DEMAND_STANDARD     : return StatusCodes.STATUS_LOCATION;               //   105;  // On-Demand Standard Report
            case EVCODE_STANDARD_REPORT     : return StatusCodes.STATUS_LOCATION;               //   474;  // Periodic Report
            case EVCODE_END_OF_DAY          : return StatusCodes.STATUS_DAY_SUMMARY;            //   475;  // End-Of-Day Report
            case EVCODE_COG_HEADING         : return StatusCodes.STATUS_LOCATION;               //   476;  // Course Change Report
            case EVCODE_SOG_SPEED           : return StatusCodes.STATUS_MOTION_EXCESS_SPEED;    //   477;  // Accelerated Speed Report
            case EVCODE_RPM                 : return StatusCodes.STATUS_OBD_RPM_RANGE;          //   478;  // Accelerated RPM Report
            case EVCODE_POWER_ON            : return StatusCodes.STATUS_INITIALIZED;            //   480;  // Power-On Report
            case EVCODE_ENGINE_START        : return StatusCodes.STATUS_IGNITION_ON;            //   481;  // Engine Start Detected Report
            case EVCODE_ENGINE_STOP         : return StatusCodes.STATUS_IGNITION_OFF;           //   482;  // Engine Stop Detected Report
            case EVCODE_POWER_OFF           : return StatusCodes.STATUS_POWER_FAILURE;          //   484;  // Power-Off Report
            case EVCODE_DECELERATION        : return StatusCodes.STATUS_EXCESS_BRAKING;         //   485;  // Hard Deceleration Report
            case EVCODE_ACCELERATION        : return StatusCodes.STATUS_LOCATION;               //   486;  // Hard Acceleration Report
            case EVCODE_OUTPUT_CTL_ON       : return StatusCodes.STATUS_OUTPUT_ON;              //   487;  // Output Control Set ON
            case EVCODE_OUTPUT_CTL_OFF      : return StatusCodes.STATUS_OUTPUT_OFF;             //   488;  // Output Control Set OFF
            case EVCODE_OFF_TO_IDLE         : return StatusCodes.STATUS_IGNITION_ON;            //   501;  // Transition (engine start)
            case EVCODE_OFF_TO_WORK         : return StatusCodes.STATUS_IGNITION_ON;            //   502;  // Transition (engine start and RPM)
            case EVCODE_IDLE_TO_OFF         : return StatusCodes.STATUS_IGNITION_OFF;           //   503;  // Transition (engine off)
            case EVCODE_IDLE_TO_WORK        : return StatusCodes.STATUS_LOCATION;               //   504;  // Transition (RPM idle to work)
            case EVCODE_WORK_TO_OFF         : return StatusCodes.STATUS_IGNITION_OFF;           //   505;  // Transition (RPM work to off)
            case EVCODE_WORK_TO_IDLE        : return StatusCodes.STATUS_LOCATION;               //   506;  // Transition (RPM work to idle)
            case EVCODE_CONFIG_SET_OK       : return StatusCodes.STATUS_ACK;                    //   901;  // OTA Config 'set' Response
            case EVCODE_CONFIG_SET_ERROR    : return StatusCodes.STATUS_NAK;                    //   902;  // OTA Config 'set' Response
            case EVCODE_CONFIG_GET_OK       : return StatusCodes.STATUS_ACK;                    //   903;  // OTA Config 'get' Response
            case EVCODE_CONFIG_GET_ERROR    : return StatusCodes.STATUS_NAK;                    //   904;  // OTA Config 'get' Response
            case EVCODE_ACTION_CMD_OK       : return StatusCodes.STATUS_ACK;                    //   905;  // OTA Action Command Response
            case EVCODE_ACTION_CMD_ERROR    : return StatusCodes.STATUS_NAK;                    //   906;  // OTA Action Command Response
            case EVCODE_CONFIG_CHANGE       : return StatusCodes.STATUS_CONFIG_RESET;           //   910;  // ? Configuration change ?
            case EVCODE_GPS_ANT_UNAVAIL     : return StatusCodes.STATUS_GPS_FAILURE;            //   956;  // GPS Antenna Unavailable
            case EVCODE_GPS_ANT_NORMAL      : return StatusCodes.STATUS_LOCATION;               //   957;  // GPS Antenna Normal
            case EVCODE_GPS_ANT_OPEN        : return StatusCodes.STATUS_GPS_FAILURE;            //   958;  // GPS Antenna Open
            case EVCODE_GPS_ANT_SHORT       : return StatusCodes.STATUS_GPS_FAILURE;            //   959;  // GPS Antenna Short
            case EVCODE_DIAG_CODE_ACTIVE    : return StatusCodes.STATUS_OBD_FAULT;              //   961;  // An active diagnostic event
            case EVCODE_DIAG_CODE_NORMAL    : return StatusCodes.STATUS_LOCATION;               //   962;  // A diagnostic event is no longer active
            case EVCODE_IDLE_EXCEPTION      : return StatusCodes.STATUS_MOTION_IDLE;            //   978;  // A continuous idle period has exceeded the set threshold
        }
        if ((evCode >= 1001) && (evCode <= 1500)) {
            // Digital Channel in Alarm
            int eventCodeOffset = evCode - 1001;
            return StatusCodes.STATUS_OBD_RANGE;
        } else
        if ((evCode >= 1501) && (evCode <= 2000)) {
            // Digital Channel Normal
            int eventCodeOffset = evCode - 1501;
            return StatusCodes.STATUS_LOCATION;
        } else
        if ((evCode >= 2001) && (evCode <= 2500)) {
            // Digital Channel Duration Alarm
            int eventCodeOffset = evCode - 2001;
            return StatusCodes.STATUS_OBD_RANGE;
        } else
        if ((evCode >= 2501) && (evCode <= 3000)) {
            // Digital Channel Starts Alarm
            int eventCodeOffset = evCode - 2501;
            return StatusCodes.STATUS_OBD_RANGE;
        } else
        if ((evCode >= 3001) && (evCode <= 3500)) {
            // Analog Channel Low Warning
            int eventCodeOffset = evCode - 3001;
            return StatusCodes.STATUS_OBD_RANGE;
        } else
        if ((evCode >= 3501) && (evCode <= 4000)) {
            // Analog Channel Low Alarm
            int eventCodeOffset = evCode - 3501;
            return StatusCodes.STATUS_OBD_RANGE;
        } else
        if ((evCode >= 4001) && (evCode <= 4500)) {
            // Analog Channel Hign Warning
            int eventCodeOffset = evCode - 4001;
            return StatusCodes.STATUS_OBD_RANGE;
        } else
        if ((evCode >= 4501) && (evCode <= 5000)) {
            // Analog Channel Hign Alarm
            int eventCodeOffset = evCode - 4501;
            return StatusCodes.STATUS_OBD_RANGE;
        } else
        if ((evCode >= 5001) && (evCode <= 5500)) {
            // Analog Channel Normal
            int eventCodeOffset = evCode - 5001;
            return StatusCodes.STATUS_LOCATION;
        } else
        if ((evCode >= 6001) && (evCode <= 6100)) {
            // Geofence inside
            int eventCodeOffset = evCode - 6001;
            return StatusCodes.STATUS_GEOFENCE_ARRIVE;
        } else
        if ((evCode >= 6101) && (evCode <= 6200)) {
            // Geofence inside
            int eventCodeOffset = evCode - 6101;
            return StatusCodes.STATUS_GEOFENCE_DEPART;
        } else {
            return StatusCodes.STATUS_LOCATION;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Field definitions

    public static final int     FIELD_VEHICLE_ID                =   1;  // VehicleID, EventCode, Time(HHMMSS), Date(MMDDYY)
    public static final int     FIELD_ACK                       =  10;  // VehicleID, EventCode, ResultCode
    public static final int     FIELD_PING                      =  15;  // VehicleID, Count
    public static final int     FIELD_GPS_DATA                  =  20;  // Latitude, Longitude, SOG(SpeedMPH), COG(Heading), Age(Sec), Altitude(Feed)
    public static final int     FIELD_TEXT                      =  30;  // ParserID, String
    public static final int     FIELD_DIGITAL_CHAN_DATA         =  40;  // ChanNum, Data(Int)
    public static final int     FIELD_DIGITAL_BIN_DATA          =  41;  // ChanNum, TotalDuration(sec), BinCount, BinDuration(sec)
    public static final int     FIELD_ANALOG_CHAN_DATA          =  60;  // ChanNum, Data(Float)
    public static final int     FIELD_ANALOG_BIN_COUNT_DATA     =  61;  // ChanNum, Bin_1_Cnt, Bin_2_Cnt, Bin_3_Cnt, Bin_4_Cnt
    public static final int     FIELD_ANALOG_BIN_DURA_DATA      =  62;  // ChanNum, TotalDura(sec), Bin_1_Dura(sec), Bin_2_Dura(sec), Bin_2_Dura(sec), Bin_4_Dura(sec)
    public static final int     FIELD_ANALOG_MIN_MAX_VALUE      =  63;  // ChanNum, MaxValue(Float), MinValue(Float)
    public static final int     FIELD_ANALOG_DISTANCE           =  64;  // ChanNum, TotalDistance(Miles?), Bin_1_Dist, Bin_2_Dist, Bin_3_Dist, Bin_4_Dist
    public static final int     FIELD_ANALOG_FUEL               =  65;  // ChanNum, TotalFuel(Gal?), Bin_1_Fuel, Bin_2_Fuel, Bin_3_Fuel, Bin_4_Fuel
    public static final int     FIELD_J1939_DIAG                =  80;  // SPN, FMI, OccurCount
    public static final int     FIELD_J1708_DIAG                =  81;  // MID, PID/SID, FMI, OccurCount
    public static final int     FIELD_OBD2_DIAG                 =  82;  // DTC
    public static final int     FIELD_IDLE_EXCEPTION            =  86;  // Duration(sec), FuelUsed(Gal?)
    public static final int     FIELD_REQUEST_GET_CONFIG        = 255;  // ConfigLineType, ConfigIndex
    public static final int     FIELD_REQUEST_SET_CONFIG        = 258;  // ConfigLineType, ConfigIndex, ConfigListString
    public static final int     FIELD_ACTION_COMMAND            = 280;  // CommandNumber
    
    public static final Map FieldDescription = ListTools.toMap("toString", new AntxField[] {
        new AntxField(FIELD_VEHICLE_ID                , "VehicleID"             ),
        new AntxField(FIELD_ACK                       , "ACL"                   ),
        new AntxField(FIELD_PING                      , "Ping"                  ),
        new AntxField(FIELD_GPS_DATA                  , "GPS"                   ),
        new AntxField(FIELD_TEXT                      , "Text"                  ),
        new AntxField(FIELD_DIGITAL_CHAN_DATA         , "Digital"               ),
        new AntxField(FIELD_DIGITAL_BIN_DATA          , "Digital[Dura]"         ),
        new AntxField(FIELD_ANALOG_CHAN_DATA          , "Analog"                ),
        new AntxField(FIELD_ANALOG_BIN_COUNT_DATA     , "Analog[Count]"         ),
        new AntxField(FIELD_ANALOG_BIN_DURA_DATA      , "Analog[Dura]"          ),
        new AntxField(FIELD_ANALOG_MIN_MAX_VALUE      , "Analog[MinMax]"        ),
        new AntxField(FIELD_ANALOG_DISTANCE           , "Analog[Dist]"          ),
        new AntxField(FIELD_ANALOG_FUEL               , "Analog[Fuel]"          ),
        new AntxField(FIELD_J1939_DIAG                , "J1939_Fault"           ),
        new AntxField(FIELD_J1708_DIAG                , "J1708_Fault"           ),
        new AntxField(FIELD_OBD2_DIAG                 , "OBDII_Fault"           ),
        new AntxField(FIELD_IDLE_EXCEPTION            , "Idle"                  ),
        new AntxField(FIELD_REQUEST_GET_CONFIG        , "Get_Config"            ),
        new AntxField(FIELD_REQUEST_SET_CONFIG        , "Set_Config"            ),
        new AntxField(FIELD_ACTION_COMMAND            , "Command"               ),
    });

    public static AntxField GetAntxField(int fld)
    {
        return (AntxField)FieldDescription.get(String.valueOf(fld));
    }

    public static String GetFieldDescription(int fld)
    {
        AntxField af = GetAntxField(fld);
        return (af != null)? af.getDescription() : "?";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int         id       = 0;
    private String      desc     = "";
    
    public AntxField(int id, String desc) 
    {
        this.id   = id;
        this.desc = desc;
    }
    
    // ------------------------------------------------------------------------

    public int getID() 
    {
        return this.id;
    }
    
    public String getDescription() 
    {
        return this.desc;
    }
    
    // ------------------------------------------------------------------------

    public String toString() 
    {
        return String.valueOf(this.getID());
    }
    
    // ------------------------------------------------------------------------

}
