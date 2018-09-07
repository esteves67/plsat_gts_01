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
    
public class AntxChannel
    extends AntxField
{
    
    // ------------------------------------------------------------------------

    public  static final int    UNITS_NONE                      =  0;
    public  static final int    UNITS_BOOLEAN                   =  1;
    public  static final int    UNITS_PERCENT                   =  2;
    public  static final int    UNITS_HOURS                     =  3;
    public  static final int    UNITS_TEMP                      =  4;
    public  static final int    UNITS_DISTANCE                  =  5;
    public  static final int    UNITS_SPEED                     =  6;
    public  static final int    UNITS_PRESSURE                  =  7;
    public  static final int    UNITS_FUEL_USED                 =  8;
    public  static final int    UNITS_ECON                      =  9;
    public  static final int    UNITS_VOLRATE                   = 10;

    public  static final double KILOMETERS_PER_MILE             = GeoPoint.KILOMETERS_PER_MILE;
    public  static final double LITERS_PER_GALLON               = Account.LITERS_PER_US_GALLON;
    public  static final double LPH_PER_GPS                     = Account.LITERS_PER_US_GALLON * 3600.0; // G/s * L/G * s/h = L/h
    public  static final double PSI_PER_KPA                     = Account.PSI_PER_KPA;
    public  static final double KPA_PER_PSI                     = 1.0 / PSI_PER_KPA;
    public  static final double KPL_PER_MPG                     = KILOMETERS_PER_MILE * (1.0 / LITERS_PER_GALLON);
    public  static final double MINUTES_PER_HOUR                = 1.0 / 60.0;
    public  static final double PERCENT_FRAC                    = 0.01;
    public  static final double C_GAIN                          = 0.55555556;
    public  static final double C_OFFSET                        = -17.77777778;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Channel Numbers
    
    // undefined channel
    public static final int     CHAN_UNDEFINED                  =   0;
    
    // digital channels
    public static final int     CHAN_GSM                        =   1;  // [Ofs   0] Internal status 1=NotNormal
    public static final int     CHAN_COMM                       =   2;  // [Ofs   1] Internal status 1=NotNormal
    public static final int     CHAN_CAN                        =   3;  // [Ofs   2] Internal status 1=NotNormal
    public static final int     CHAN_DIN1                       =   4;  // [Ofs   3] 1=Closed, 0=Open
    public static final int     CHAN_DIN2                       =   5;  // [Ofs   4] 1=Closed, 0=Open
    public static final int     CHAN_DIN4                       =   6;  // [Ofs   5] 1=Closed, 0=Open
    public static final int     CHAN_ENGINE_RUN                 =   9;  // [Ofs   8] 1=Running
    public static final int     CHAN_SHUTDOWN                   =  10;  // [Ofs   9] Red LED
    public static final int     CHAN_WARNING                    =  11;  // [Ofs  10] Amber LED
    public static final int     CHAN_PTO_STATE                  =  16;  // [Ofs  15] 1=Active, 0=Inactive
    public static final int     CHAN_FAN_DRIVE_STATE            =  17;  // [Ofs  16] 1=Active, 0=Inactive
    public static final int     CHAN_PTO_ENABLED                =  18;  // [Ofs  15] 1=Enabled, 0=Disabled

    // analog channels
    public static final int     CHAN_FUEL_LEVEL                 =  51;  // [Ofs   0] %
    public static final int     CHAN_RPM                        =  52;  // [Ofs   1] RPM
    public static final int     CHAN_TOTAL_ENGINE_HOURS         =  53;  // [Ofs   2] Hours
    public static final int     CHAN_COOLANT_TEMPERATURE        =  56;  // [Ofs   5] Def-F
    public static final int     CHAN_BATTERY_SWITCHED           =  57;  // [Ofs   6] Volts
    public static final int     CHAN_BATTERY_UNSWITCHED         =  58;  // [Ofs   7] Volts
    public static final int     CHAN_OIL_PRESSURE               =  59;  // [Ofs   8] PSI
    public static final int     CHAN_FUEL_RATE                  =  60;  // [Ofs   9] Gal/Sec
    public static final int     CHAN_ANALOG_INPUT               =  62;  // [Ofs  11] Counts
    public static final int     CHAN_VEHICLE_DISTANCE           =  79;  // [Ofs  28] Miles
    public static final int     CHAN_ENGINE_STARTS              =  81;  // [Ofs  30] Count
    public static final int     CHAN_IDLE_TIME_START            =  82;  // [Ofs  31] Minutes  (since last start)
    public static final int     CHAN_IDLE_FUEL_START            =  83;  // [Ofs  32] Gals     (since last start)
    public static final int     CHAN_WORK_TIME_START            =  84;  // [Ofs  33] Minutes  (since last start)
    public static final int     CHAN_WORK_FUEL_START            =  85;  // [Ofs  34] Gals     (since last start)
    public static final int     CHAN_IDLE_TIME_DAY              =  86;  // [Ofs  35] Minutes  (for the day)
    public static final int     CHAN_IDLE_FUEL_DAY              =  87;  // [Ofs  36] Gals     (for the day)
    public static final int     CHAN_WORK_TIME_DAY              =  88;  // [Ofs  37] Minutes  (for the day)
    public static final int     CHAN_WORK_FUEL_DAY              =  89;  // [Ofs  38] Gals     (for the day)
    public static final int     CHAN_OIL_LEVEL                  =  90;  // [Ofs  39] %
    public static final int     CHAN_OIL_TEMPERATURE            =  91;  // [Ofs  40] Def-F
    public static final int     CHAN_COOLANT_LEVEL              =  92;  // [Ofs  41] %
    public static final int     CHAN_AVERAGE_FUEL_ECON          =  93;  // [Ofs  42] MPG
    public static final int     CHAN_INSTANT_FUEL_ECON          =  94;  // [Ofs  43] MPG
    public static final int     CHAN_THROTTLE_POSITION          =  95;  // [Ofs  44] %
    public static final int     CHAN_VEHICLE_SPEED              =  96;  // [Ofs  45] MPH
    public static final int     CHAN_BAROMETRIC_PRESSURE        = 105;  // [Ofs  54] PSI
    public static final int     CHAN_CABIN_TEMPERATURE          = 106;  // [Ofs  55] Def-F
    public static final int     CHAN_AMBIENT_TEMPERATURE        = 107;  // [Ofs  56] Def-F
    public static final int     CHAN_ACCEL_PEDAL_POSITION       = 108;  // [Ofs  57] %
    public static final int     CHAN_AIR_FILTER_DIFF_PRESSUE    = 109;  // [Ofs  58] PSI
    public static final int     CHAN_ENGINE_LOAD                = 110;  // [Ofs  59] %
    public static final int     CHAN_ENGINE_TORQUE              = 111;  // [Ofs  60] %
    public static final int     CHAN_FUEL_BY_RPM                = 112;  // [Ofs  61] Gals
    public static final int     CHAN_DISTANCE_BY_RPM            = 113;  // [Ofs  62] Miles
    public static final int     CHAN_FUEL_BY_SPEED              = 114;  // [Ofs  63] Gals
    public static final int     CHAN_DISTANCE_BY_SPEED          = 115;  // [Ofs  64] Miles
    public static final int     CHAN_FUEL_BY_LOAD               = 116;  // [Ofs  65] Gals
    public static final int     CHAN_FUEL_BY_TORQUE             = 117;  // [Ofs  66] Gals
    public static final int     CHAN_DISTANCE_BY_DAY            = 118;  // [Ofs  67] Miles
    public static final int     CHAN_FUEL_BY_DAY                = 119;  // [Ofs  68] Gals
    public static final int     CHAN_PTO_FUEL_BY_DAY            = 150;  // [Ofs  99] Gals
    public static final int     CHAN_ENGINE_FUEL_TEMPERATURE    = 151;  // [Ofs 100] Deg-F
    public static final int     CHAN_ESTIMATED_FAN_SPEED        = 152;  // [Ofs 101] %
    public static final int     CHAN_TRANS_OIL_TEMPERATURE      = 153;  // [Ofs 102] Deg-F
    public static final int     CHAN_GEN_TOTAL_KW_HOURS         = 250;  // [Ofs 199] kWH
    public static final int     CHAN_GEN_TOTAL_REACTIVE_POWER   = 251;  // [Ofs 200] kVAr
    public static final int     CHAN_GEN_POWER_FACTOR           = 252;  // [Ofs 201] ??
    public static final int     CHAN_GEN_TOTAL_REAL_POWER       = 253;  // [Ofs 202] kW
    public static final int     CHAN_GEN_AVG_LINE_LINE_AC_RMS   = 254;  // [Ofs 203] Volts
    public static final int     CHAN_GEN_AVG_LINE_NEUT_AC_RMS   = 255;  // [Ofs 204] Volts
    public static final int     CHAN_GEN_AVG_AC_FREQUENCY       = 256;  // [Ofs 205] Hz
    public static final int     CHAN_GEN_AVG_AC_RMS_CURRENT     = 257;  // [Ofs 206] Amps
    
    public static final Map ChannelDescription = ListTools.toMap("toString", new AntxChannel[] {
        new AntxChannel(CHAN_UNDEFINED                  , "undefined"                 ),                                        // 
        new AntxChannel(CHAN_GSM                        , "GSM Status"                ),                                        // 
        new AntxChannel(CHAN_COMM                       , "Comm Status"               ),                                        // 
        new AntxChannel(CHAN_CAN                        , "CAN Status"                ),                                        // 
        new AntxChannel(CHAN_DIN1                       , "DIN1"                      , UNITS_BOOLEAN   , 1.0),                 // 
        new AntxChannel(CHAN_DIN2                       , "DIN2"                      , UNITS_BOOLEAN   , 1.0),                 // 
        new AntxChannel(CHAN_DIN4                       , "DIN4"                      , UNITS_BOOLEAN   , 1.0),                 // 
        new AntxChannel(CHAN_ENGINE_RUN                 , "Engine Running"            , UNITS_BOOLEAN   , 1.0),                 // 
        new AntxChannel(CHAN_SHUTDOWN                   , "Shutdown LED"              , UNITS_BOOLEAN   , 1.0),                 // 
        new AntxChannel(CHAN_WARNING                    , "Warning LED"               , UNITS_BOOLEAN   , 1.0),                 // 
        new AntxChannel(CHAN_PTO_STATE                  , "PTO State"                 , UNITS_BOOLEAN   , 1.0),                 // 
        new AntxChannel(CHAN_FAN_DRIVE_STATE            , "Fan Drive State"           , UNITS_BOOLEAN   , 1.0),                 // 
        new AntxChannel(CHAN_PTO_ENABLED                , "PTO Enabled State"         , UNITS_BOOLEAN   , 1.0),                 // 
        new AntxChannel(CHAN_FUEL_LEVEL                 , "Fuel Level"                , UNITS_PERCENT   , PERCENT_FRAC),        //  51
        new AntxChannel(CHAN_RPM                        , "RPM"                       ),                                        //  52
        new AntxChannel(CHAN_TOTAL_ENGINE_HOURS         , "Engine Hours"              , UNITS_HOURS     , 1.0),                 //  53
        new AntxChannel(CHAN_COOLANT_TEMPERATURE        , "Coolant Temp"              , UNITS_TEMP      , C_GAIN, C_OFFSET),    //  56  F==>C
        new AntxChannel(CHAN_BATTERY_SWITCHED           , "Battery Sw"                ),                                        //  57
        new AntxChannel(CHAN_BATTERY_UNSWITCHED         , "Battery Unsw"              ),                                        //  58
        new AntxChannel(CHAN_OIL_PRESSURE               , "Oil Pressure"              , UNITS_PRESSURE  , KPA_PER_PSI),         //  59
        new AntxChannel(CHAN_FUEL_RATE                  , "Fuel Rate"                 , UNITS_VOLRATE   , LPH_PER_GPS),         //  60
        new AntxChannel(CHAN_ANALOG_INPUT               , "Analog Input"              ),                                        //  62
        new AntxChannel(CHAN_VEHICLE_DISTANCE           , "Distance"                  , UNITS_DISTANCE  , KILOMETERS_PER_MILE), //  79
        new AntxChannel(CHAN_ENGINE_STARTS              , "Engine Starts"             ),                                        //  81
        new AntxChannel(CHAN_IDLE_TIME_START            , "Idle Time Start"           , UNITS_HOURS     , MINUTES_PER_HOUR),    //  82
        new AntxChannel(CHAN_IDLE_FUEL_START            , "Idle Fuel Start"           , UNITS_FUEL_USED , LITERS_PER_GALLON),   //  83
        new AntxChannel(CHAN_WORK_TIME_START            , "Work Time Start"           , UNITS_HOURS     , MINUTES_PER_HOUR),    //  84
        new AntxChannel(CHAN_WORK_FUEL_START            , "Work Fuel Start"           , UNITS_FUEL_USED , LITERS_PER_GALLON),   //  85
        new AntxChannel(CHAN_IDLE_TIME_DAY              , "Idle Time Day"             , UNITS_HOURS     , MINUTES_PER_HOUR),    //  86
        new AntxChannel(CHAN_IDLE_FUEL_DAY              , "Idle Fuel Day"             , UNITS_FUEL_USED , LITERS_PER_GALLON),   //  87
        new AntxChannel(CHAN_WORK_TIME_DAY              , "Work Time Day"             , UNITS_HOURS     , MINUTES_PER_HOUR),    //  88
        new AntxChannel(CHAN_WORK_FUEL_DAY              , "Work Fuel Day"             , UNITS_FUEL_USED , LITERS_PER_GALLON),   //  89
        new AntxChannel(CHAN_OIL_LEVEL                  , "Oil Level"                 , UNITS_PERCENT   , PERCENT_FRAC),        //  90
        new AntxChannel(CHAN_OIL_TEMPERATURE            , "Oil Temperature"           , UNITS_TEMP      , C_GAIN, C_OFFSET),    //  91
        new AntxChannel(CHAN_COOLANT_LEVEL              , "Coolant Level"             , UNITS_PERCENT   , PERCENT_FRAC),        //  92
        new AntxChannel(CHAN_AVERAGE_FUEL_ECON          , "Avg Fuel Econ"             , UNITS_ECON      , KPL_PER_MPG),         //  93
        new AntxChannel(CHAN_INSTANT_FUEL_ECON          , "Inst Fuel Econ"            , UNITS_ECON      , KPL_PER_MPG),         //  94
        new AntxChannel(CHAN_THROTTLE_POSITION          , "Throttle Pos"              , UNITS_PERCENT   , PERCENT_FRAC),        //  95
        new AntxChannel(CHAN_VEHICLE_SPEED              , "Speed"                     , UNITS_SPEED     , KILOMETERS_PER_MILE), //  96
        new AntxChannel(CHAN_BAROMETRIC_PRESSURE        , "Barometric Press"          , UNITS_PRESSURE  , KPA_PER_PSI),         // 105
        new AntxChannel(CHAN_CABIN_TEMPERATURE          , "Cabin Temp"                , UNITS_TEMP      , C_GAIN, C_OFFSET),    // 106
        new AntxChannel(CHAN_AMBIENT_TEMPERATURE        , "Ambient Temp"              , UNITS_TEMP      , C_GAIN, C_OFFSET),    // 107
        new AntxChannel(CHAN_ACCEL_PEDAL_POSITION       , "Accel Pdal Pos"            , UNITS_PERCENT   , PERCENT_FRAC),        // 108
        new AntxChannel(CHAN_AIR_FILTER_DIFF_PRESSUE    , "Air Filter Diff Press"     , UNITS_PRESSURE  , KPA_PER_PSI),         // 109
        new AntxChannel(CHAN_ENGINE_LOAD                , "Engine Load"               , UNITS_PERCENT   , PERCENT_FRAC),        // 110
        new AntxChannel(CHAN_ENGINE_TORQUE              , "Engine Torque"             , UNITS_PERCENT   , PERCENT_FRAC),        // 111
        new AntxChannel(CHAN_FUEL_BY_RPM                , "Fuel by RPM"               , UNITS_FUEL_USED , LITERS_PER_GALLON),   // 112
        new AntxChannel(CHAN_DISTANCE_BY_RPM            , "Distance by RPM"           , UNITS_DISTANCE  , KILOMETERS_PER_MILE), // 113
        new AntxChannel(CHAN_FUEL_BY_SPEED              , "Fuel by Speed"             , UNITS_FUEL_USED , LITERS_PER_GALLON),   // 114
        new AntxChannel(CHAN_DISTANCE_BY_SPEED          , "Distance by Speed"         , UNITS_DISTANCE  , KILOMETERS_PER_MILE), // 115
        new AntxChannel(CHAN_FUEL_BY_LOAD               , "Fuel by Load"              , UNITS_FUEL_USED , LITERS_PER_GALLON),   // 116
        new AntxChannel(CHAN_FUEL_BY_TORQUE             , "Fuel by Torque"            , UNITS_FUEL_USED , LITERS_PER_GALLON),   // 117
        new AntxChannel(CHAN_DISTANCE_BY_DAY            , "Distance by Day"           , UNITS_DISTANCE  , KILOMETERS_PER_MILE), // 118
        new AntxChannel(CHAN_FUEL_BY_DAY                , "Fuel by Day"               , UNITS_FUEL_USED , LITERS_PER_GALLON),   // 119
        new AntxChannel(CHAN_PTO_FUEL_BY_DAY            , "PTO Fuel by Day"           , UNITS_FUEL_USED , LITERS_PER_GALLON),   // 150
        new AntxChannel(CHAN_ENGINE_FUEL_TEMPERATURE    , "Eng Fuel Temp"             , UNITS_FUEL_USED , LITERS_PER_GALLON),   // 151
        new AntxChannel(CHAN_ESTIMATED_FAN_SPEED        , "Fan Speed"                 , UNITS_PERCENT   , PERCENT_FRAC),        // 152
        new AntxChannel(CHAN_TRANS_OIL_TEMPERATURE      , "Trans Oil Temp"            , UNITS_TEMP      , C_GAIN, C_OFFSET),    // 153
        new AntxChannel(CHAN_GEN_TOTAL_KW_HOURS         , "Gen Total KW Hours"        ),  // 
        new AntxChannel(CHAN_GEN_TOTAL_REACTIVE_POWER   , "Gen Total Reactive Pwr"    ),  // 
        new AntxChannel(CHAN_GEN_POWER_FACTOR           , "Gen Power Factor"          ),  // 
        new AntxChannel(CHAN_GEN_TOTAL_REAL_POWER       , "Gen Total Real Pwr"        ),  // 
        new AntxChannel(CHAN_GEN_AVG_LINE_LINE_AC_RMS   , "Gen Avg Line/Line AC RMS"  ),  // 
        new AntxChannel(CHAN_GEN_AVG_LINE_NEUT_AC_RMS   , "Gen Avg Line/Neut AC RMS"  ),  // 
        new AntxChannel(CHAN_GEN_AVG_AC_FREQUENCY       , "Gen Avg AC Freq"           ),  // 
        new AntxChannel(CHAN_GEN_AVG_AC_RMS_CURRENT     , "Gen Avg AC RMS Current"    ),  // 
    });

    public static AntxChannel GetAntxChannel(int ch)
    {
        AntxChannel ac = (AntxChannel)ChannelDescription.get(String.valueOf(ch));
        if (ac != null) {
            return ac;
        } else {
            return (AntxChannel)ChannelDescription.get(String.valueOf(CHAN_UNDEFINED));
        }
    }

    public static String GetChannelDescription(int ch)
    {
        return GetAntxChannel(ch).getDescription();
    }

    public static double ConvertChannelUnits(int ch, double value)
    {
        return ConvertChannelUnits(GetAntxChannel(ch), value);
    }

    public static double ConvertChannelUnits(AntxChannel ac, double value)
    {
        return (ac != null)? ac.convertUnits(value) : value;
    }

    public static String GetChannelUnitsDescription(Account a, int fld, int ch)
    {
        if (fld == AntxField.FIELD_ANALOG_BIN_COUNT_DATA) {
            return "Count"; // TODO: I18N
        } else
        if (fld == AntxField.FIELD_ANALOG_BIN_DURA_DATA) {
            return "Hours"; // TODO: I18N
        } else {
            AntxChannel ac = GetAntxChannel(ch);
            return (ac != null)? ac.accountUnitsDescription(a,null) : "";
        }
    }

    public static double GetAccountUnitsValue(Account a, int fld, int ch, double value)
    {
        if (fld == AntxField.FIELD_ANALOG_BIN_COUNT_DATA) {
            return value;
        } else
        if (fld == AntxField.FIELD_ANALOG_BIN_DURA_DATA) {
            return value;
        } else {
            AntxChannel ac = GetAntxChannel(ch);
            return (ac != null)? ac.toAccountUnits(a,value) : value;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private int         units    = UNITS_NONE;
    private double      gain     = 0.0;
    private double      offset   = 0.0;

    public AntxChannel(int id, String desc, int units, double gain, double offset) 
    {
        super(id, desc);
        this.units  = units;
        this.gain   = gain;
        this.offset = offset;
    }
    
    public AntxChannel(int id, String desc, int units, double gain) 
    {
        this(id, desc, units, gain, 0.0);
    }
    
    public AntxChannel(int id, String desc) 
    {
        this(id, desc, UNITS_NONE, 0.0, 0.0);
    }

    // ------------------------------------------------------------------------
    
    public int getUnits()
    {
        return this.units;
    }

    public String accountUnitsDescription(Account a, Locale locale) 
    {
        switch (this.units) {
            case UNITS_NONE     : return "";
            case UNITS_BOOLEAN  : return "Bool";
            case UNITS_PERCENT  : return "%";
            case UNITS_HOURS    : return "Hours";
            case UNITS_TEMP     : return Account.getTemperatureUnits(a).toString(locale);
            case UNITS_DISTANCE : return Account.getDistanceUnits(a).toString(locale);
            case UNITS_SPEED    : return Account.getSpeedUnits(a).toString(locale);
            case UNITS_PRESSURE : return Account.getPressureUnits(a).toString(locale);
            case UNITS_FUEL_USED: return Account.getVolumeUnits(a).toString(locale);
            case UNITS_ECON     : return Account.getEconomyUnits(a).toString(locale);
            case UNITS_VOLRATE  : return Account.getVolumeUnits(a).toString(locale) + "/H";
            default             : return "";
        }
    }

    public double toAccountUnits(Account a, double value) 
    {
        switch (this.units) {
            case UNITS_NONE     : return value;
            case UNITS_BOOLEAN  : return (value != 0.0)? 1.0 : 0.0;
            case UNITS_PERCENT  : return value;
            case UNITS_HOURS    : return value;
            case UNITS_TEMP     : return Account.getTemperatureUnits(a).convertFromC(value);
            case UNITS_DISTANCE : return Account.getDistanceUnits(a).convertFromKM(value);
            case UNITS_SPEED    : return Account.getSpeedUnits(a).convertFromKPH(value);
            case UNITS_PRESSURE : return Account.getPressureUnits(a).convertFromKPa(value);
            case UNITS_FUEL_USED: return Account.getVolumeUnits(a).convertFromLiters(value);
            case UNITS_ECON     : return Account.getEconomyUnits(a).convertFromKPL(value);
            case UNITS_VOLRATE  : return Account.getVolumeUnits(a).convertFromLiters(value);
            default             : return value;
        }
    }

    public double convertUnits(double value) 
    {
        if (this.units == UNITS_BOOLEAN) {
            return (value != 0.0)? 1.0 : 0.0;
        } else
        if (this.gain == 0.0) {
            return value;
        } else {
            return (value * this.gain) + this.offset;
            // C=>F   (C * 9/5) + 32
            // F=>C   (F - 32) * 5/9  ==> (F * 5/9) - (32 * 5/9) ==> (F * 5/9) - 17.77778   (gain=0.55556, offset=-17.77778)
        }                
    }

    // ------------------------------------------------------------------------

}
