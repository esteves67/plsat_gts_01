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
package org.opengts.extra.dbtools;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;

public class J1587
{

    // ------------------------------------------------------------------------

    private static final String ARG_MID         = "MID";
    private static final String ARG_MID_DESC    = ARG_MID + ".desc";
    private static final String ARG_SID         = "SID";
    private static final String ARG_SID_DESC    = ARG_SID + ".desc";
    private static final String ARG_PID         = "PID";
    private static final String ARG_PID_DESC    = ARG_PID + ".desc";
    private static final String ARG_FMI         = "FMI";
    private static final String ARG_FMI_DESC    = ARG_FMI + ".desc";

    // ------------------------------------------------------------------------
    // Get Description for J1708 Fault
    
    /**
    *** Gets the J1587 description
    **/
    public static Properties GetJ1587Description(Properties p)
    {

        /* invalid properties specified */
        if (p == null) {
            return new Properties();
        }

        /* type */
        String type = p.getProperty("TYPE",null);
        if (type == null) {
            p.setProperty("TYPE", "J1587");
        } else
        if (!type.equalsIgnoreCase("J1587")) {
            return p;
        }

        /* extract MID/PID/SID/FMI */
        int mid = StringTools.parseInt(p.getProperty(ARG_MID),-1);
        int pid = StringTools.parseInt(p.getProperty(ARG_PID),-1);
        int sid = StringTools.parseInt(p.getProperty(ARG_SID),-1);
        int fmi = StringTools.parseInt(p.getProperty(ARG_FMI),-1);

        /* fill descriptions */
        p.setProperty(ARG_MID_DESC, J1587.getMIDDescription(mid));
        if (sid >= 0) {
            p.setProperty(ARG_SID_DESC, J1587.getSIDDescription(mid, sid));
        }
        if (pid >= 0) {
            p.setProperty(ARG_PID_DESC, J1587.getPIDDescription(pid));
        }
        p.setProperty(ARG_FMI_DESC, J1587.getFMIDescription(fmi));

        /* return properties */
        return p;

    }

    // ------------------------------------------------------------------------
    // FMI: Failure Mode Identifiers

    public static String getFMIDescription(int fmi)
    {
        switch (fmi) {
            case   0: return "Data above normal range";
            case   1: return "Data below normal range";
            case   2: return "Data invalid";
            case   3: return "Voltage above normal range";
            case   4: return "Voltage below normal range";
            case   5: return "Current below normal range";
            case   6: return "Current above normal range";
            case   7: return "Mechanical system failure";
            case   8: return "Abnormal frequency";
            case   9: return "Abnormal update rate";
            case  10: return "Abnormal rate of change";
            case  11: return "Failure unidentifiable";
            case  12: return "Bad device or component";
            case  13: return "Out of calibration";
            case  14: return "Special Instructions";
            case  15: return "FMI Reserved";
        }
        return "FMI Invalid! " + fmi;
    }

    // ------------------------------------------------------------------------
    // MIDs

    public static String getMIDDescription(int mid)
    {
        switch (mid) {
            case   0:
            case   1:
            case   2:
            case   3:
            case   4:
            case   5:
            case   6:
            case   7: return "ENGINE";
            case   8:
            case   9:
            case  10:
            case  11: return "BRAKES";
            case  12:
            case  13:
            case  14:
            case  15: return "TIRES";
            case  16:
            case  17:
            case  18:
            case  19: return "SUSPENSION";
            case  20:
            case  21:
            case  22:
            case  23:
            case  24:
            case  25:
            case  26:
            case  27: return "TRANSMISSION";
            case  28:
            case  29: return "ELECTRICAL CHARGING SYSTEM";
            case  30:
            case  31:
            case  32: return "ELECTRICAL";
            case  33:
            case  34:
            case  35: return "CARGO REFRIGERATION / HEATING";
            case  36:
            case  37:
            case  38:
            case  39:
            case  40: return "INSTRUMENT CLUSTER";
            case  41:
            case  42:
            case  43:
            case  44:
            case  45: return "DRIVER INFORMATION CENTER";
            case  46:
            case  47: return "CAB CLIMATE CONTROL";
            case  48:
            case  49:
            case  50:
            case  51:
            case  52:
            case  53:
            case  54:
            case  55: return "DIAGNOSTIC SYSTEMS";
            case  56:
            case  57:
            case  58:
            case  59:
            case  60:
            case  61: return "TRIP RECORDER";
            case  62:
            case  63: return "TURBOCHARGER";
            case  64:
            case  65:
            case  66:
            case  67:
            case  68: return "OFF-BOARD DIAGNOSTICS";
            case  69:
            case  70:
            case  71:
            case  72:
            case  73:
            case  74:
            case  75:
            case  76:
            case  77:
            case  78:
            case  79:
            case  80:
            case  81:
            case  82:
            case  83:
            case  84:
            case  85:
            case  86: return "SET ASIDE FOR SAE J1922";
            case  87: return "SET ASIDE FOR SAE J2497";
            case  88:
            case  89:
            case  90:
            case  91:
            case  92:
            case  93:
            case  94:
            case  95:
            case  96:
            case  97:
            case  98:
            case  99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110: return "DEFINED BY SAE LOW SPEED COMMUNICATIONS NETWORK SUBCOMMITTEE";
            case 111: return "RESERVED -- FACTORY ELECTRONIC MODULE TESTER (OFF VEHICLE)";
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127: return "UNASSIGNED -- AVAILABLE FOR USE";
            case 128: return "Engine #1";
            case 129: return "Turbocharger";
            case 130: return "Transmission";
            case 131: return "Power Takeoff";
            case 132:
            case 133:
            case 134:
            case 135: return "Axle";
            case 136:
            case 137:
            case 138:
            case 139: return "Brakes";
            case 140: return "Instrument Cluster";
            case 141: return "Trip Recorder";
            case 142: return "Vehicle Management System";
            case 143: return "Fuel System";
            case 144: return "Cruise Control";
            case 145: return "Road Speed Indicator";
            case 146: return "Cab Climate Control";
            case 147:
            case 148:
            case 149: return "Cargo Refrigeration/Heating";
            case 150:
            case 151:
            case 152:
            case 153: return "Suspension";
            case 154:
            case 155:
            case 156:
            case 157: return "Diagnostic Systems";
            case 158: return "Electrical Charging System";
            case 159:
            case 160: return "Proximity Detector";
            case 161: return "Aerodynamic Control Unit";
            case 162: return "Vehicle Navigation Unit";
            case 163: return "Vehicle Security";
            case 164: return "Multiplex";
            case 165: return "Communication Unit -- Ground";
            case 166:
            case 167:
            case 168:
            case 169: return "Tires";
            case 170: return "Electrical";
            case 171: return "Driver Information Center";
            case 172: return "Off-board Diagnostics #1";
            case 173: return "Engine Retarder";
            case 174: return "Cranking/Starting System";
            case 175: return "Engine #2";
            case 176: return "Transmission";
            case 177: return "Particulate Trap System";
            case 178: return "Vehicle Sensors to Data Converter";
            case 179: return "Data Logging Computer";
            case 180: return "Off-board Diagnostics #2";
            case 181: return "Communication Unit -- Satellite";
            case 182: return "Off-board Programming Station";
            case 183: return "Engine #3";
            case 184: return "Engine #4";
            case 185: return "Engine #5";
            case 186: return "Engine #6";
            case 187: return "Vehicle Control Head Unit/Vehicle Management System #2";
            case 188: return "Vehicle Control Head Unit/Vehicle Management System #3";
            case 189: return "Vehicle Head Signs";
            case 190: return "Refrigerant Management Protection and Diagnostics";
            case 191: return "Vehicle Location Unit -- Differential Correction";
            case 192: return "Front Door Status Unit";
            case 193: return "Middle Door Status Unit";
            case 194: return "Rear Door Status Unit";
            case 195: return "Annunciator Unit";
            case 196: return "Fare Collection Unit";
            case 197: return "Passenger Counter Unit #1";
            case 198: return "Schedule Adherence Unit";
            case 199: return "Route Adherence Unit";
            case 200: return "Environment Monitor Unit/Auxiliary Cab Climate Control";
            case 201: return "Vehicle Status Points Monitor Unit";
            case 202: return "High Speed Communications Unit";
            case 203: return "Mobile Data Terminal Unit";
            case 204:
            case 205: return "Vehicle Proximity";
            case 206: return "Base Unit (Radio Gateway to Fixed End)";
            case 207: return "Bridge from SAE J1708 Drivetrain Link";
            case 208: return "Maintenance Printer";
            case 209: return "Vehicle Turntable";
            case 210: return "Bus Chassis Identification Unit";
            case 211: return "Smart Card Terminal";
            case 212: return "Mobile Data Terminal";
            case 213: return "Vehicle Control Head Touch Screen";
            case 214: return "Silent Alarm Unit";
            case 215: return "Surveillance Microphone";
            case 216: return "Lighting Control Administrator Unit";
            case 217:
            case 218: return "Tractor/Trailer Bridge";
            case 219: return "Collision Avoidance Systems";
            case 220: return "Tachograph";
            case 221: return "Driver Information Center #2";
            case 222: return "Driveline Retarder";
            case 223: return "Transmission Shift Console -- Primary";
            case 224: return "Parking Heater";
            case 225:
            case 226:
            case 227:
            case 228:
            case 229:
            case 230: return "Weighing System";
            case 231: return "Communication Unit -- Cellular";
            case 232: return "Safety Restraint System";
            case 233: return "Intersection Preemption Emitter";
            case 234: return "Instrument Cluster #2";
            case 235: return "Engine Oil Control System";
            case 236: return "Entry Assist Control #1";
            case 237: return "Entry Assist Control #2";
            case 238: return "Idle Adjust System";
            case 239: return "Passenger Counter Unit #2";
            case 240: return "Passenger Counter Unit #3";
            case 241: return "Fuel Tank Monitor";
            case 242:
            case 243: return "Axles";
            case 244:
            case 245: return "Diagnostic Systems";
            case 246:
            case 247: return "Brakes";
            case 248: return "Forward Road Image Processor";
            case 249: return "Body Controller";
            case 250: return "Steering Column Unit";
            case 251:
            case 252:
            case 253:
            case 254:
            case 255: return "Reserved to be assigned";
        }
        return "?" + mid;
    }

    // ------------------------------------------------------------------------
    // MID PIDs

    public static String getPIDDescription(int ext, int pid)
    {
        int extPid = ((ext <= 1)? 0 : 256) + pid;
        return getPIDDescription(extPid);
    }
    
    public static String getPIDDescription(int extPid)
    {
        switch (extPid) {
            case   0+  0: return "Request Parameter";
            case   0+  1: return "Invalid Data Parameter";
            case   0+  2: return "Transmitter System Status";
            case   0+  3: return "Transmitter System Diagnostic";
            case   0+  4: return "Reserved - to be assigned";
            case   0+  5: return "Underrange Warning Condition";
            case   0+  6: return "Overrange Warning Condition";
            case   0+  7: return "Axle #2 Lift Air Pressure";
            case   0+  8: return "Brake System Air Pressure Low Warning Switch Status";
            case   0+  9: return "Axle Lift Status";
            case   0+ 10: return "Axle Slider Status";
            case   0+ 11: return "Cargo Securement";
            case   0+ 12: return "Brake Stroke Status";
            case   0+ 13: return "Entry Assist Position/Deployment";
            case   0+ 14: return "Entry Assist Motor Current";
            case   0+ 15: return "Fuel Supply Pump Inlet Pressure";
            case   0+ 16: return "Suction Side Fuel Filter Differential Pressure";
            case   0+ 17: return "Engine Oil Level Remote Reservoir";
            case   0+ 18: return "Extended Range Fuel Pressure";
            case   0+ 19: return "Extended Range Engine Oil Pressure";
            case   0+ 20: return "Extended Range Engine Coolant Pressure";
            case   0+ 21: return "Engine ECU Temperature";
            case   0+ 22: return "Extended Engine Crank Blow-by Pressure";
            case   0+ 23: return "Generator Oil Pressure";
            case   0+ 24: return "Generator Coolant Temperature";
            case   0+ 25: return "Air Conditioner System Status #2";
            case   0+ 26: return "Estimated Percent Fan Speed";
            case   0+ 27: return "Percent Exhaust Gas Recirculation Valve #1 Position";
            case   0+ 28: return "Percent Accelerator Position #3";
            case   0+ 29: return "Percent Accelerator Position #2";
            case   0+ 30: return "Crank Blow-by Pressure";
            case   0+ 31: return "Transmission Range Position";
            case   0+ 32: return "Transmission Splitter Position";
            case   0+ 33: return "Clutch Cylinder Position";
            case   0+ 34: return "Clutch Cylinder Actuator Status";
            case   0+ 35: return "Shift Finger Actuator Status #2";
            case   0+ 36: return "Clutch Plates Wear Condition";
            case   0+ 37: return "Transmission Tank Air Pressure";
            case   0+ 38: return "Second Fuel Level (Right Side)";
            case   0+ 39: return "Tire Pressure Check Interval";
            case   0+ 40: return "Engine Retarder Switches Status";
            case   0+ 41: return "Cruise Control Switches Status";
            case   0+ 42: return "Pressure Switch Status";
            case   0+ 43: return "Ignition Switch Status";
            case   0+ 44: return "Attention/Warning Indicator Lamps Status";
            case   0+ 45: return "Inlet Air Heater Status";
            case   0+ 46: return "Vehicle Wet Tank Pressure";
            case   0+ 47: return "Retarder Status";
            case   0+ 48: return "Extended Range Barometric Pressure";
            case   0+ 49: return "ABS Control Status";
            case   0+ 50: return "Air Conditioner System Clutch Status/Command #1";
            case   0+ 51: return "Throttle Position";
            case   0+ 52: return "Engine Intercooler Temperature";
            case   0+ 53: return "Transmission Synchronizer Clutch Value";
            case   0+ 54: return "Transmission Synchronizer Brake Value";
            case   0+ 55: return "Shift Finger Positional Status";
            case   0+ 56: return "Transmission Range Switch Status";
            case   0+ 57: return "Transmission Actuator Status #2";
            case   0+ 58: return "Shift Finger Actuator Status";
            case   0+ 59: return "Shift Finger Gear Position";
            case   0+ 60: return "Shift Finger Rail Position";
            case   0+ 61: return "Parking Brake Actuator Status";
            case   0+ 62: return "Retarder Inhibit Status";
            case   0+ 63: return "Transmission Actuator Status #1";
            case   0+ 64: return "Direction Switch Status";
            case   0+ 65: return "Service Brake Switch Status";
            case   0+ 66: return "Vehicle Enabling Component Status";
            case   0+ 67: return "Shift Request Switch Status";
            case   0+ 68: return "Torque Limiting Factor";
            case   0+ 69: return "Two Speed Axle Switch Status";
            case   0+ 70: return "Parking Brake Switch Status";
            case   0+ 71: return "Idle Shutdown Timer Status";
            case   0+ 72: return "Blower Bypass Value Position";
            case   0+ 73: return "Auxiliary Water Pump Pressure";
            case   0+ 74: return "Maximum Road Speed Limit";
            case   0+ 75: return "Steering Axle Temperature";
            case   0+ 76: return "Axle #1 Lift Air Pressure";
            case   0+ 77: return "Forward Rear Drive Axle Temperature";
            case   0+ 78: return "Rear Rear-Drive Axle Temperature";
            case   0+ 79: return "Road Surface Temperature";
            case   0+ 80: return "Washer Fluid Level";
            case   0+ 81: return "Particulate Trap Inle Pressure";
            case   0+ 82: return "Air Start Pressure";
            case   0+ 83: return "Road Speed Limit Status";
            case   0+ 84: return "Road Speed";
            case   0+ 85: return "Cruise Control Status";
            case   0+ 86: return "Cruise Control Set Speed";
            case   0+ 87: return "Cruise Control High-Set Limit Speed";
            case   0+ 88: return "Cruise Control Low-Set Limit Speed";
            case   0+ 89: return "Power Takeoff Status";
            case   0+ 90: return "PTO Oil Temperature";
            case   0+ 91: return "Percent Accelerator Pedal Position";
            case   0+ 92: return "Percent Engine Load";
            case   0+ 93: return "Output Torque";
            case   0+ 94: return "Fuel Delivery Pressure";
            case   0+ 95: return "Fuel Filter Differential Pressure";
            case   0+ 96: return "Fuel Level";
            case   0+ 97: return "Water in Fuel Indicator";
            case   0+ 98: return "Engine Oil Level";
            case   0+ 99: return "Engine Oil Filter Differential Pressure";
            case   0+100: return "Engine Oil Pressure";
            case   0+101: return "Crank Pressure";
            case   0+102: return "Boost Pressure";
            case   0+103: return "Turbo Speed";
            case   0+104: return "Turbo Oil Pressure";
            case   0+105: return "Intake Manifold Temperature";
            case   0+106: return "Air Inlet Pressure";
            case   0+107: return "Air Filter Differential Pressure";
            case   0+108: return "Barometric Pressure";
            case   0+109: return "Coolant pressure";
            case   0+110: return "Engine Coolant Temperature";
            case   0+111: return "Coolant Level";
            case   0+112: return "Coolant Filter Differential Pressure";
            case   0+113: return "Governor Droop";
            case   0+114: return "Net Battery Current";
            case   0+115: return "Alternator Current";
            case   0+116: return "Brake Application Pressure";
            case   0+117: return "Brake Primary Pressure";
            case   0+118: return "Brake Secondary Pressure";
            case   0+119: return "Hydraulic Retarder Pressure";
            case   0+120: return "Hydraulic Retarder Oil Temperature";
            case   0+121: return "Engine Retarder Status";
            case   0+122: return "Engine Retarder Percent";
            case   0+123: return "Clutch Pressure";
            case   0+124: return "Transmission Oil Level";
            case   0+125: return "Transmission Oil Level High/Low";
            case   0+126: return "Transmission Filter Differential Pressure";
            case   0+127: return "Transmission Oil Pressure";
            case   0+128: return "Component-specific request";
            case   0+129: return "Injector Metering Rail #2 Pressure";
            case   0+130: return "Power Specific Fuel Economy";
            case   0+131: return "Exhaust Back Pressure";
            case   0+132: return "Mass Air Flow";
            case   0+133: return "Average Fuel Rate";
            case   0+134: return "Wheel Speed Sensor Status";
            case   0+135: return "Extended Range Fuel Delivery Pressure (Absolute)";
            case   0+136: return "Auxiliary Vacuum Pressure Reading";
            case   0+137: return "Auxiliary Gage Pressure Reading #1";
            case   0+138: return "Auxiliary Absolute Pressure Reading";
            case   0+139: return "Tire Pressure Control System Channel Functional Mode";
            case   0+140: return "Tire Pressure Control System Solenoid Status";
            case   0+141: return "Trailer #1";
            case   0+142: return "Drive Channel Tire Pressure Target";
            case   0+143: return "Steer Channel Tire Pressure Target";
            case   0+144: return "Trailer #1";
            case   0+145: return "Drive Channel Tire Pressure";
            case   0+146: return "Steer Channel Tire Pressure";
            case   0+147: return "Average Fuel Economy (Natural Gas)";
            case   0+148: return "Instantaneous Fuel Economy (Natural Gas)";
            case   0+149: return "Fuel Mass Flow Rate (Natural Gas)";
            case   0+150: return "PTO Engagement Control Status";
            case   0+151: return "ATC Control Status";
            case   0+152: return "Number of ECU Resets";
            case   0+153: return "Crank Pressure";
            case   0+154: return "Auxiliary Input and Output Status #2";
            case   0+155: return "Auxiliary Input and Output Status #1";
            case   0+156: return "Injector Timing Rail Pressure";
            case   0+157: return "Injector Metering Rail Pressure";
            case   0+158: return "Battery Potential (Voltage) Switched";
            case   0+159: return "Gas Supply Pressure";
            case   0+160: return "Main Shaft Speed";
            case   0+161: return "Input Shaft Speed";
            case   0+162: return "Transmission Range Selected";
            case   0+163: return "Transmission Range Attained";
            case   0+164: return "Injection Control Pressure";
            case   0+165: return "Compass Bearing";
            case   0+166: return "Rated Engine Power";
            case   0+167: return "Alternator Potential (Voltage)";
            case   0+168: return "Battery Potential (Voltage)";
            case   0+169: return "Cargo Ambient Temperature";
            case   0+170: return "Cab Interior Temperature";
            case   0+171: return "Ambient Air Temperature";
            case   0+172: return "Air Inlet Temperature";
            case   0+173: return "Exhaust Gas Temperature";
            case   0+174: return "Fuel Temperature";
            case   0+175: return "Engine Oil Temperature";
            case   0+176: return "Turbo Oil Temperature";
            case   0+177: return "Transmission #1 Oil Temperature";
            case   0+178: return "Front Axel Weight";
            case   0+179: return "Rear Axel Weight";
            case   0+180: return "Trailer Weight";
            case   0+181: return "Cargo Weight";
            case   0+182: return "Trip Fuel";
            case   0+183: return "Fuel Rate (Instantaneous)";
            case   0+184: return "Instantaneous Fuel Economy";
            case   0+185: return "Average Fuel Economy";
            case   0+186: return "Power Takeoff Speed";
            case   0+187: return "Power Takeoff Set Speed";
            case   0+188: return "Idle Engine Speed";
            case   0+189: return "Rated Engine Speed";
            case   0+190: return "Engine Speed";
            case   0+191: return "Transmission Output Shaft Speed";
            case   0+192: return "Multisection Parameter";
            case   0+193: return "Transmitter System Diagnostic Table";
            case   0+194: return "Transmitter System Diagnostic Code and Occurrence Count Table";
            case   0+195: return "Diagnostic Data Request/Clear Count";
            case   0+196: return "Diagnostic Data/Count Clear Response";
            case   0+197: return "Connection Management";
            case   0+198: return "Connection Mode Data Transfer";
            case   0+199: return "Traction Control Disable State";
            case   0+200:
            case   0+201:
            case   0+202:
            case   0+203:
            case   0+204:
            case   0+205:
            case   0+206:
            case   0+207:
            case   0+208: return "Reserved--to be assigned";
            case   0+209: return "ABS Control Status";
            case   0+210: return "Tire Temperature (By Sequence Number)";
            case   0+211: return "Tire Pressure (By Sequence Number)";
            case   0+212: return "Tire Pressure Target (By Sequence Number)";
            case   0+213: return "Wheel End Assembly  Vibration Level";
            case   0+214: return "Vehicle Wheel Speeds";
            case   0+215: return "Brake Temperature";
            case   0+216: return "Wheel Bearing Temperature";
            case   0+217: return "Fuel Tank/Nozzle Identification";
            case   0+218: return "State Line Crossing";
            case   0+219: return "Current State and Country";
            case   0+220: return "Engine Torque History";
            case   0+221: return "Anti-theft Request";
            case   0+222: return "Anti-theft Status";
            case   0+223: return "Auxiliary A/D Counts";
            case   0+224: return "Immobilizer Security Code";
            case   0+225: return "Reserved for Text Message Acknowledged";
            case   0+226: return "Reserved for Text Message to Display";
            case   0+227: return "Reserved for Text Message Display Type";
            case   0+228: return "Speed Sensor Calibration";
            case   0+229: return "Total Fuel Used (Natural Gas)";
            case   0+230: return "Total Idle Fuel Used (Natural Gas)";
            case   0+231: return "Trip Fuel (Natural Gas)";
            case   0+232: return "DGPS Differential Correction";
            case   0+233: return "Unit Number (Power Unit)";
            case   0+234: return "Software Identification";
            case   0+235: return "Total Idle Hours";
            case   0+236: return "Total Idle Fuel Used";
            case   0+237: return "Vehicle Identification Number";
            case   0+238: return "Velocity Vector";
            case   0+239: return "Vehicle Position";
            case   0+240: return "Change Reference Number";
            case   0+241: return "Tire Pressure by Position";
            case   0+242: return "Tire Temperature by Position";
            case   0+243: return "Component Identification";
            case   0+244: return "Trip Distance";
            case   0+245: return "Total Vehicle Distance";
            case   0+246: return "Total Vehicle Hours";
            case   0+247: return "Total Engine Hours";
            case   0+248: return "Total PTO Hours";
            case   0+249: return "Total Engine Revolutions";
            case   0+250: return "Total Fuel Used";
            case   0+251: return "Clock";
            case   0+252: return "Date";
            case   0+253: return "Elapsed Time";
            case   0+254: return "Data Link Escape";
            case   0+255: return "Extension";
            case 256+  0: return "Request Parameter";
            case 256+  1: return "Cold Restart of Specific Component";
            case 256+  2: return "Warm Restart of Specific Component";
            case 256+  3: return "Component Restart Response";
            case 256+  4:
            case 256+  5:
            case 256+  6:
            case 256+  7:
            case 256+  8:
            case 256+  9:
            case 256+ 10:
            case 256+ 11:
            case 256+ 12:
            case 256+ 13:
            case 256+ 14:
            case 256+ 15:
            case 256+ 16:
            case 256+ 17:
            case 256+ 18:
            case 256+ 19:
            case 256+ 20:
            case 256+ 21:
            case 256+ 22:
            case 256+ 23:
            case 256+ 24:
            case 256+ 25:
            case 256+ 26:
            case 256+ 27:
            case 256+ 28:
            case 256+ 29:
            case 256+ 30:
            case 256+ 31:
            case 256+ 32:
            case 256+ 33:
            case 256+ 34:
            case 256+ 35:
            case 256+ 36:
            case 256+ 37:
            case 256+ 38:
            case 256+ 39:
            case 256+ 40:
            case 256+ 41:
            case 256+ 42:
            case 256+ 43:
            case 256+ 44:
            case 256+ 45:
            case 256+ 46:
            case 256+ 47:
            case 256+ 48:
            case 256+ 49:
            case 256+ 50:
            case 256+ 51:
            case 256+ 52:
            case 256+ 53:
            case 256+ 54:
            case 256+ 55:
            case 256+ 56:
            case 256+ 57:
            case 256+ 58:
            case 256+ 59:
            case 256+ 60:
            case 256+ 61:
            case 256+ 62:
            case 256+ 63:
            case 256+ 64:
            case 256+ 65:
            case 256+ 66:
            case 256+ 67:
            case 256+ 68:
            case 256+ 69:
            case 256+ 70:
            case 256+ 71:
            case 256+ 72:
            case 256+ 73:
            case 256+ 74:
            case 256+ 75:
            case 256+ 76:
            case 256+ 77:
            case 256+ 78:
            case 256+ 79:
            case 256+ 80:
            case 256+ 81:
            case 256+ 82:
            case 256+ 83:
            case 256+ 84:
            case 256+ 85:
            case 256+ 86:
            case 256+ 87:
            case 256+ 88:
            case 256+ 89:
            case 256+ 90:
            case 256+ 91:
            case 256+ 92:
            case 256+ 93:
            case 256+ 94:
            case 256+ 95:
            case 256+ 96:
            case 256+ 97:
            case 256+ 98:
            case 256+ 99:
            case 256+100:
            case 256+101:
            case 256+102:
            case 256+103:
            case 256+104:
            case 256+105: return "Reserved (Page 2) to be assigned";
            case 256+106: return "Percent Exhaust Gas Recirculation Valve #2 Position";
            case 256+107: return "Hydraulic Retarder Control Air Pressure";
            case 256+108: return "HVAC Unit Discharge Temperature";
            case 256+109: return "Weighing System Status Command";
            case 256+110: return "Engine Oil Level High/Low";
            case 256+111: return "Lane Tracking System Status";
            case 256+112: return "Lane Departure Indication";
            case 256+113: return "Distance to Rear Object (Reverse)";
            case 256+114: return "Trailer Pneumatic Brake Control Line Pressure";
            case 256+115: return "Trailer Pneumatic Supply Line Pressure";
            case 256+116: return "Remote Accelerator";
            case 256+117: return "Center Rear Drive Axle Temperature";
            case 256+118: return "Alternator AC Voltage";
            case 256+119: return "Fuel Return Pressure";
            case 256+120: return "Fuel Pump Inlet Vacuum";
            case 256+121: return "Compression Unbalance";
            case 256+122: return "Fare Collection Unit Status";
            case 256+123: return "Door Status";
            case 256+124: return "Articulation Angle";
            case 256+125: return "Vehicle Use Status";
            case 256+126: return "Transit Silent Alarm Status";
            case 256+127: return "Vehicle Acceleration";
            case 256+128: return "Component-Specific Request";
            case 256+129:
            case 256+130:
            case 256+131:
            case 256+132:
            case 256+133:
            case 256+134:
            case 256+135:
            case 256+136:
            case 256+137:
            case 256+138:
            case 256+139:
            case 256+140:
            case 256+141:
            case 256+142:
            case 256+143:
            case 256+144:
            case 256+145:
            case 256+146:
            case 256+147:
            case 256+148:
            case 256+149: return "Reserved (page 2) - to be assigned";
            case 256+150: return "HVAC Blower Motor Speed";
            case 256+151: return "Axle Group Full Weight Calibration";
            case 256+152: return "Axle Group Empty Weight Calibration";
            case 256+153: return "Axle Group Weight";
            case 256+154: return "Extended Range Road Surface Temperature";
            case 256+155: return "Recirculated Engine Exhaust Gas Differential Pressure";
            case 256+156: return "Recirculated Engine Exhaust Gas Temperature";
            case 256+157: return "Net Vehicle Weight Change";
            case 256+158: return "Air Conditioner Refrigerant Low Side Pressure";
            case 256+159: return "Air Conditioner Refrigerant High Side Pressure";
            case 256+160: return "Evaporator Temperature";
            case 256+161: return "Gross Vehicle Weight";
            case 256+162: return "Transmission #2 Oil Temperature";
            case 256+163: return "Starter Circuit Resistance";
            case 256+164: return "Starter Current (average)";
            case 256+165: return "Alternator/Generator Negative Cable Voltage";
            case 256+166: return "Auxiliary Current";
            case 256+167: return "Extended Range Net Battery Current";
            case 256+168: return "DC Voltage";
            case 256+169: return "Auxiliary Frequency";
            case 256+170: return "Alternator/Generator Field Voltage";
            case 256+171: return "Battery Resistance Change";
            case 256+172: return "Battery Internal Resistance";
            case 256+173: return "Starter Current Peak";
            case 256+174: return "Starter Solenoid Voltage";
            case 256+175: return "Starter Negative Cable Voltage";
            case 256+176: return "Starter Motor Voltage";
            case 256+177: return "Fuel Shutoff Solenoid Voltage";
            case 256+178: return "AC Voltage";
            case 256+179: return "Cargo Ambient Temperature (by location)";
            case 256+180: return "Trip Sudden Decelerations";
            case 256+181:
            case 256+182: return "Trailer #2";
            case 256+183: return "Extended Range Boost Pressure #1";
            case 256+184: return "Extended Range Boost Pressure #2";
            case 256+185: return "Auxiliary Temperature #1";
            case 256+186: return "Auxiliary Temperature #2";
            case 256+187: return "Auxiliary Gage Pressure Reading #2";
            case 256+188: return "Battery #2 Potential (Voltage)";
            case 256+189: return "Cylinder Head Temperature Bank B (right bank)";
            case 256+190: return "Cylinder Head Temperature Bank A (left bank)";
            case 256+191: return "Passenger Counter";
            case 256+192: return "Page 2 Multisection Parameter";
            case 256+193: return "Reporting Interval Request";
            case 256+194: return "Bridge Filter Control";
            case 256+195:
            case 256+196:
            case 256+197:
            case 256+198:
            case 256+199:
            case 256+200:
            case 256+201:
            case 256+202:
            case 256+203:
            case 256+204:
            case 256+205:
            case 256+206:
            case 256+207:
            case 256+208:
            case 256+209:
            case 256+210:
            case 256+211:
            case 256+212:
            case 256+213:
            case 256+214:
            case 256+215:
            case 256+216:
            case 256+217:
            case 256+218:
            case 256+219:
            case 256+220:
            case 256+221:
            case 256+222:
            case 256+223:
            case 256+224:
            case 256+225:
            case 256+226:
            case 256+227:
            case 256+228:
            case 256+229:
            case 256+230:
            case 256+231:
            case 256+232:
            case 256+233:
            case 256+234:
            case 256+235:
            case 256+236:
            case 256+237:
            case 256+238:
            case 256+239:
            case 256+240:
            case 256+241: return "Reserved (page 2) - to be assigned";
            case 256+242: return "Send Keypress Command";
            case 256+243: return "Driver Interface Unit (DIU) Object/Form Command";
            case 256+244: return "Intersection Preemption Status and Configuration";
            case 256+245: return "Signage Message";
            case 256+246: return "Fare Collection Unit - Point of Sale)";
            case 256+247: return "Fare Collection Unit - Service Detail";
            case 256+248: return "Annunciator Voice Message";
            case 256+249: return "Vehicle Control Head Keyboard Message";
            case 256+250: return "Vehicle Control Head Display Message";
            case 256+251: return "Driver Identification";
            case 256+252: return "Transit Route Identification";
            case 256+253: return "Mile Post Identification";
            case 256+254: return "Page 2 Data Link Escape";
            case 256+255: return "Page 2 Extension";
        }
        return "?" + extPid;
    }

    // ------------------------------------------------------------------------
    // SID Common
    
    private static String getSIDDescription_common(int sid)
    {
        switch (sid) {
            case 151: return "System Diagnostic Code #1";
            case 152: return "System Diagnostic Code #2";
            case 153: return "System Diagnostic Code #3";
            case 154: return "System Diagnostic Code #4";
            case 155: return "System Diagnostic Code #5";
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 172:
            case 173:
            case 174:
            case 175:
            case 176:
            case 177:
            case 178:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
            case 198:
            case 199:
            case 200:
            case 201:
            case 202:
            case 203:
            case 204:
            case 205:
            case 206: return "Reserved for future assignment by SAE Subcommitte";
            case 207: return "Battery #1 Temperature";
            case 208: return "Battery #2 Temperature";
            case 209: return "Start Enable Device #2";
            case 210: return "Oil Temperature Sensor";
            case 211: return "Sensor Supply Voltage #2 (+5V DC)";
            case 212: return "Sensor Supply Voltage #1 (+5V DC)";
            case 213: return "PLC Data Link";
            case 214: return "ECU Backup Battery";
            case 215: return "Cab Interior Temperature Thermostat";
            case 216: return "Other ECUs Have Reported Fault Codes Affecting Operation";
            case 217: return "Anti-theft Start Inhibit (Password Valid Indicator)";
            case 218: return "ECM Main Relay";
            case 219: return "Start Signal Indicator";
            case 220: return "Electronic Tractor/Trailer Interface (ISO 11992)";
            case 221: return "Internal Sensor Voltage Supply";
            case 222: return "Protect Lamp";
            case 223: return "Ambient Light Sensor";
            case 224: return "Audible Alarm";
            case 225: return "Green Lamp";
            case 226: return "Transmission Neutral Switch";
            case 227: return "Auxiliary Analog Input #1";
            case 228: return "High Side Refrigerant Pressure Switch";
            case 229: return "Kickdown Switch";
            case 230: return "Idle Validation Switch";
            case 231: return "SAE J1939 Data Link";
            case 232: return "5 Volts DC Supply";
            case 233: return "Controller #2";
            case 234: return "Parking Brake On Actuator";
            case 235: return "Parking Brake Off Actuator";
            case 236: return "Power Connect Device";
            case 237: return "Start Enable Device";
            case 238: return "Diagnostic Lamp - Red";
            case 239: return "Diagnostic Lamp - Amber";
            case 240: return "Program Memory";
            case 241: return "Set aside for Systems Diagnostics";
            case 242: return "Cruise Control Resume Switch";
            case 243: return "Cruise Control Set Switch";
            case 244: return "Cruise Control Enable Switch";
            case 245: return "Clutch Pedal Switch #1";
            case 246: return "Brake Pedal Switch #1";
            case 247: return "Brake Pedal Switch #2";
            case 248: return "Proprietary Data Link";
            case 249: return "SAE J1922 Data Link";
            case 250: return "SAE J1708 (J1587) Data Link";
            case 251: return "Power Supply";
            case 252: return "Calibration Module";
            case 253: return "Calibration Memory";
            case 254: return "Controller #1";
            case 255: return "Reserved";
        }
        return "?" + sid;
    }
    
    // ------------------------------------------------------------------------
    // MID[128,175,183,184,185,186] SIDs
    
    private static String getSIDDescription_128_175_183_184_185_186(int sid)
    {
        switch (sid) {
            case   0: return "Reserved";
            case   1: return "Injector Cylinder #1";
            case   2: return "Injector Cylinder #2";
            case   3: return "Injector Cylinder #3";
            case   4: return "Injector Cylinder #4";
            case   5: return "Injector Cylinder #5";
            case   6: return "Injector Cylinder #6";
            case   7: return "Injector Cylinder #7";
            case   8: return "Injector Cylinder #8";
            case   9: return "Injector Cylinder #9";
            case  10: return "Injector Cylinder #10";
            case  11: return "Injector Cylinder #11";
            case  12: return "Injector Cylinder #12";
            case  13: return "Injector Cylinder #13";
            case  14: return "Injector Cylinder #14";
            case  15: return "Injector Cylinder #15";
            case  16: return "Injector Cylinder #16";
            case  17: return "Fuel Shutoff Valve";
            case  18: return "Fuel Control Valve";
            case  19: return "Throttle bypass Valve";
            case  20: return "Timing Actuator";
            case  21: return "Engine Position Sensor";
            case  22: return "Timing Sensor";
            case  23: return "Rack Actuator";
            case  24: return "Rack Position Sensor";
            case  25: return "External Engine Protection Input";
            case  26: return "Auxiliary Output Device Driver #1";
            case  27: return "Variable Geometry Turbocharger Actuator #1";
            case  28: return "Variable Geometry Turbocharger Actuator #2";
            case  29: return "External Fuel Command Input";
            case  30: return "External Speed Command Input";
            case  31: return "Tachometer Signal Output";
            case  32: return "Turbocharger #1 Wastegate Drive";
            case  33: return "Fan Clutch Output Device Driver";
            case  34: return "Exhaust Back Pressure Sensor";
            case  35: return "Exhaust Back Pressure Regulator Solenoid";
            case  36: return "Glow Plug Lamp";
            case  37: return "Electronic Drive Unit Power Relay";
            case  38: return "Glow Plug Relay";
            case  39: return "Engine Starter Motor Relay";
            case  40: return "Auxiliary Output Device Driver #2";
            case  41: return "ECM 8 Volts DC Supply";
            case  42: return "Injection Control Pressure Regulator";
            case  43: return "Autoshift High Dear Actuator";
            case  44: return "Autoshift Low Gear Actuator";
            case  45: return "Autoshift Neutral Actuator";
            case  46: return "Autoshift Common Low Side (Return)";
            case  47: return "Injector Cylinder #17";
            case  48: return "Injector Cylinder #18";
            case  49: return "Injector Cylinder #19";
            case  50: return "Injector Cylinder #20";
            case  51: return "Auxiliary Output Device Driver #3";
            case  52: return "Auxiliary Output Device Driver #4";
            case  53: return "Auxiliary Output Device Driver #5";
            case  54: return "Auxiliary Output Device Driver #6";
            case  55: return "Auxiliary Output Device Driver #7";
            case  56: return "Auxiliary Output Device Driver #8";
            case  57: return "Auxiliary PWM Driver #1";
            case  58: return "Auxiliary PWM Driver #2";
            case  59: return "Auxiliary PWM Driver #3";
            case  60: return "Auxiliary PWM Driver #4";
            case  61: return "Variable Swirl System Valve";
            case  62: return "Prestroke Sensor";
            case  63: return "Prestroke Actuator";
            case  64: return "Engine Speed Sensor #2";
            case  65: return "Heated Oxygen Sensor";
            case  66: return "Ignition Control Mode Signal";
            case  67: return "Ignition Control Timing Signal";
            case  68: return "Secondary Turbo Inlet Pressure";
            case  69: return "After Cooler-Oil Cooler Coolant Temperature";
            case  70: return "Inlet Air Heater Driver #1";
            case  71: return "Inlet Air Heater Driver #2";
            case  72: return "Injector Cylinder #21";
            case  73: return "Injector Cylinder #22";
            case  74: return "Injector Cylinder #23";
            case  75: return "Injector Cylinder #24";
            case  76: return "Knock Sensor";
            case  77: return "Gas Metering Valve";
            case  78: return "Fuel Supply Pump Actuator";
            case  79: return "Engine (Compression) Brake Output #1";
            case  80: return "Engine (Compression) Brake Output #2";
            case  81: return "Engine (Exhaust) Brake Output";
            case  82: return "Engine (Compression) Brake Output #3";
            case  83: return "Fuel Control Valve #2";
            case  84: return "Timing Actuator #2";
            case  85: return "Engine Oil Burn Valve";
            case  86: return "Engine Oil Replacement Valve";
            case  87: return "Idle Shutdown Vehicle Accessories Relay Driver";
            case  88: return "Turbocharger #2 Wastegate Drive";
            case  89: return "Air Compressor Actuator Circuit";
            case  90: return "Engine Cylinder #1 Knock Sensor";
            case  91: return "Engine Cylinder #2 Knock Sensor";
            case  92: return "Engine Cylinder #3 Knock Sensor";
            case  93: return "Engine Cylinder #4 Knock Sensor";
            case  94: return "Engine Cylinder #5 Knock Sensor";
            case  95: return "Engine Cylinder #6 Knock Sensor";
            case  96: return "Engine Cylinder #7 Knock Sensor";
            case  97: return "Engine Cylinder #8 Knock Sensor";
            case  98: return "Engine Cylinder #9 Knock Sensor";
            case  99: return "Engine Cylinder #10 Knock Sensor";
            case 100: return "Engine Cylinder #11 Knock Sensor";
            case 101: return "Engine Cylinder #12 Knock Sensor";
            case 102: return "Engine Cylinder #13 Knock Sensor";
            case 103: return "Engine Cylinder #14 Knock Sensor";
            case 104: return "Engine Cylinder #15 Knock Sensor";
            case 105: return "Engine Cylinder #16 Knock Sensor";
            case 106: return "Engine Cylinder #17 Knock Sensor";
            case 107: return "Engine Cylinder #18 Knock Sensor";
            case 108: return "Engine Cylinder #19 Knock Sensor";
            case 109: return "Engine Cylinder #20 Knock Sensor";
            case 110: return "Engine Cylinder #21 Knock Sensor";
            case 111: return "Engine Cylinder #22 Knock Sensor";
            case 112: return "Engine Cylinder #23 Knock Sensor";
            case 113: return "Engine Cylinder #24 Knock Sensor";
            case 114: return "Multiple Unit Synchronization Switch";
            case 115: return "Engine Oil Change Interval";
            case 116: return "Engine was Shut Down Hot";
            case 117: return "Engine has been Shut Down from Data Link Information";
            case 118: return "Injector Needle Lift Sensor #1";
            case 119: return "Injector needle Lift Sensor #2";
            case 120: return "Coolant System Thermostat";
            case 121: return "Engine Automatic Start Alarm";
            case 122: return "Engine Automatic Start Lamp";
            case 123: return "Engine Automatic Start Safety Interlock Circuit";
            case 124: return "Engine Automatic Start Failed (Engine)";
            case 125: return "Fuel heater Driver Signal";
            case 126: return "Fuel Pump Pressurizing Assembly #1";
            case 127: return "Fuel Pump Pressurizing Assembly #2";
            case 128: return "Starter Solenoid Lockout Relay Driver Circuit";
            case 129: return "Cylinder #1 Exhaust Gas Port Temperature";
            case 130: return "Cylinder #2 Exhaust Gas Port Temperature";
            case 131: return "Cylinder #3 Exhaust Gas Port Temperature";
            case 132: return "Cylinder #4 Exhaust Gas Port Temperature";
            case 133: return "Cylinder #5 Exhaust Gas Port Temperature";
            case 134: return "Cylinder #6 Exhaust Gas Port Temperature";
            case 135: return "Cylinder #7 Exhaust Gas Port Temperature";
            case 136: return "Cylinder #8 Exhaust Gas Port Temperature";
            case 137: return "Cylinder #9 Exhaust Gas Port Temperature";
            case 138: return "Cylinder #10 Exhaust Gas Port Temperature";
            case 139: return "Cylinder #11 Exhaust Gas Port Temperature";
            case 140: return "Cylinder #12 Exhaust Gas Port Temperature";
            case 141: return "Cylinder #13 Exhaust Gas Port Temperature";
            case 142: return "Cylinder #14 Exhaust Gas Port Temperature";
            case 143: return "Cylinder #15 Exhaust Gas Port Temperature";
            case 144: return "Cylinder #16 Exhaust Gas Port Temperature";
            case 145: return "Adaptive Cruise Control Mode";
            case 146: return "Exhaust Gas Re-Circulation (EGR) Valve Mechanism";
            case 147: return "Variable Nozzle Turbocharger (VNT) Mechanism";
            case 148: return "Engine (Compression) Brake Output #4";
            case 149: return "Engine (Compression) Brake Output #5";
            case 150: return "Engine (Compression) Brake Output #6";
        }
        return "?" + sid;
    }
    
    // ------------------------------------------------------------------------
    // MID[130] SIDs
    
    private static String getSIDDescription_130(int sid)
    {
        switch (sid) {
            case   0: return "Reserved";
            case   1: return "C1 Solenoid Valve";
            case   2: return "C2 Solenoid Valve";
            case   3: return "C3 Solenoid Valve";
            case   4: return "C4 Solenoid Valve";
            case   5: return "C5 Solenoid Valve";
            case   6: return "C6 Solenoid Valve";
            case   7: return "Lockup Solenoid Valve";
            case   8: return "Forward Solenoid Valve";
            case   9: return "Low Signal Solenoid Valve";
            case  10: return "Retarder Enable Solenoid Valve";
            case  11: return "Retarder Modulation Solenoid Valve";
            case  12: return "Retarder Response Solenoid Valve";
            case  13: return "Differential Lock Solenoid Valve";
            case  14: return "Engine/Transmission Match";
            case  15: return "Retarder Modulation Request Sensor";
            case  16: return "Neutral Start Output";
            case  17: return "Turbine Speed Sensor";
            case  18: return "Primary Shift Selector";
            case  19: return "Secondary Shift Selector";
            case  20: return "Special Function Inputs";
            case  21: return "C1 Clutch Pressure Indicator";
            case  22: return "C2 Clutch Pressure Indicator";
            case  23: return "C3 Clutch Pressure Indicator";
            case  24: return "C4 Clutch Pressure Indicator";
            case  25: return "C5 Clutch Pressure Indicator";
            case  26: return "C6 Clutch Pressure Indicator";
            case  27: return "Lockup Clutch Pressure Indicator";
            case  28: return "Forward Range Pressure Indicator";
            case  29: return "Neutral Range Pressure Indicator";
            case  30: return "Reverse Range Pressure Indicator";
            case  31: return "Retarder Response System Pressure Indicator";
            case  32: return "Differential Lock Clutch Pressure Indicator";
            case  33: return "Multiple Pressure Indicators";
            case  34: return "Reverse Switch";
            case  35: return "Range High Actuator";
            case  36: return "Range Low Actuator";
            case  37: return "Splitter Direct Actuator";
            case  38: return "Splitter Indirect Actuator";
            case  39: return "Shift Finger Rail Actuator 1";
            case  40: return "Shift Finger Gear Actuator 1";
            case  41: return "Upshift Request Switch";
            case  42: return "Downshift Request Switch";
            case  43: return "Torque Converter Interrupt Actuator";
            case  44: return "Torque Converter Lockup Actuator";
            case  45: return "Range High Indicator";
            case  46: return "Range Low Indicator";
            case  47: return "Shift Finger Neutral Indicator";
            case  48: return "Shift Finger Engagement Indicator";
            case  49: return "Shift Finger Center Rail Indicator";
            case  50: return "Shift Finger Rail Actuator 2";
            case  51: return "Shift Finger Gear Actuator 2";
            case  52: return "Hydraulic System";
            case  53: return "Defuel Actuator";
            case  54: return "Inertia Brake Actuator";
            case  55: return "Clutch Actuator";
            case  56: return "Auxiliary Range Mechanical System";
            case  57: return "Shift Console Data Link";
            case  58: return "Main Box Shift Engagement System";
            case  59: return "Main Box Rail Selection System";
            case  60: return "Main Box Shift Neutralization System";
            case  61: return "Auxiliary Splitter Mechanical System";
            case  62: return "Transmission Controller Power Relay";
            case  63: return "Output Shaft Speed Sensor";
            case  64: return "Throttle Position Device";
            case  65:
            case  66:
            case  67:
            case  68:
            case  69:
            case  70:
            case  71:
            case  72:
            case  73:
            case  74:
            case  75:
            case  76:
            case  77:
            case  78:
            case  79:
            case  80:
            case  81:
            case  82:
            case  83:
            case  84:
            case  85:
            case  86:
            case  87:
            case  88:
            case  89:
            case  90:
            case  91:
            case  92:
            case  93:
            case  94:
            case  95:
            case  96:
            case  97:
            case  98:
            case  99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 130:
            case 131:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150: return "Reserved for future assignment by SAE";
        }
        return "?" + sid;
    }
    
    // ------------------------------------------------------------------------
    // MID[136,137,138,139,246,247] SIDs
    
    private static String getSIDDescription_136_137_138_139_246_247(int sid)
    {
        switch (sid) {
            case   0: return "Reserved";
            case   1: return "Wheel Sensor ABS Axle 1 Left";
            case   2: return "Wheel Sensor ABS Axle 1 Right";
            case   3: return "Wheel Sensor ABS Axle 2 Left";
            case   4: return "Wheel Sensor ABS Axle 2 Right";
            case   5: return "Wheel Sensor ABS Axle 3 Left";
            case   6: return "Wheel Sensor ABS Axle 3 Right";
            case   7: return "Pressure Modulation Valve ABS Axle 1 Left";
            case   8: return "Pressure Modulation Valve ABS Axle 1 Right";
            case   9: return "Pressure Modulation Valve ABS Axle 2 Left";
            case  10: return "Pressure Modulation Valve ABS Axle 2 Right";
            case  11: return "Pressure Modulation Valve ABS Axle 3 Left";
            case  12: return "Pressure Modulation Valve ABS Axle 3 Right";
            case  13: return "Retarder Control Relay";
            case  14: return "Relay Diagonal 1";
            case  15: return "Relay Diagonal 2";
            case  16: return "Mode Switch ABS";
            case  17: return "Mode Switch ASR";
            case  18: return "DIF 1--ASR Valve";
            case  19: return "DIF 2--ASR Valve";
            case  20: return "Pneumatic Engine Control";
            case  21: return "Electronic Engine Control (Servomotor)";
            case  22: return "Speed Signal Input";
            case  23: return "Tractor ABS Warning Light Bulb";
            case  24: return "ASR Light Bulb";
            case  25:
            case  26:
            case  27: return "Wheel Sensor";
            case  28: return "Pressure Modulator";
            case  29: return "Pressure Transducer";
            case  30: return "Master Control Relay";
            case  31: return "Trailer Brake Slack Out of Adjustment Forward Axle Left";
            case  32: return "Trailer Brake Slack Out of Adjustment Forward Axle Right";
            case  33: return "Trailer Brake Slack Out of Adjustment Rear Axle Left";
            case  34: return "Trailer Brake Slack Out of Adjustment Rear Axle Right";
            case  35: return "Tractor Brake Slack Out of Adjustment Axle 1 Left";
            case  36: return "Tractor Brake Slack Out of Adjustment Axle 1 Right";
            case  37: return "Tractor Brake Slack Out of Adjustment Axle 2 Left";
            case  38: return "Tractor Brake Slack Out of Adjustment Axle 2 Right";
            case  39: return "Tractor Brake Slack Out of Adjustment Axle 3 Left";
            case  40: return "Tractor Brake Slack Out of Adjustment Axle 3 Right";
            case  41: return "Ride Height Relay";
            case  42: return "Hold modulator Valve Solenoid Axle 1 Left";
            case  43: return "Hold modulator Valve Solenoid Axle 1 Right";
            case  44: return "Hold modulator Valve Solenoid Axle 2 Left";
            case  45: return "Hold modulator Valve Solenoid Axle 2 Right";
            case  46: return "Hold modulator Valve Solenoid Axle 3 Left";
            case  47: return "Hold modulator Valve Solenoid Axle 3 Right";
            case  48: return "Dump Modulator Valve Solenoid Axle 1 Left";
            case  49: return "Dump Modulator Valve Solenoid Axle 1 Right";
            case  50: return "Dump Modulator Valve Solenoid Axle 2 Left";
            case  51: return "Dump Modulator Valve Solenoid Axle 2 Right";
            case  52: return "Dump Modulator Valve Solenoid Axle 3 Left";
            case  53: return "Dump Modulator Valve Solenoid Axle 3 Right";
            case  54: return "Hydraulic Pump Motor";
            case  55: return "Brake Light Switch 1";
            case  56: return "Brake Light Switch 2";
            case  57: return "Electronic Pressure Control";
            case  58: return "Pneumatic Back-up Pressure Control";
            case  59: return "Brake Pressure Sensing";
            case  60: return "Electronic Pressure Control";
            case  61: return "Pneumatic Back-up Pressure Control";
            case  62: return "Brake Pressure Sensing";
            case  63: return "Electronic Pressure Control";
            case  64: return "Pneumatic Back-up Pressure Control";
            case  65: return "Brake Pressure Sensing";
            case  66: return "Electronic Pressure Control";
            case  67: return "Pneumatic Back-up Pressure Control";
            case  68: return "Brake Pressure Sensing";
            case  69: return "Axle Load Sensor";
            case  70:
            case  71:
            case  72:
            case  73:
            case  74:
            case  75: return "Lining Wear Sensor";
            case  76: return "Brake Signal Transmitter";
            case  77: return "Brake Signal Sensor 1";
            case  78: return "Brake Signal Sensor 2";
            case  79: return "Tire Dimension Supervision";
            case  80: return "Vehicle Deceleration Control";
            case  81: return "Trailer ABS Warning Light Bulb";
            case  82: return "Brake Torque Output Axle 1 Left";
            case  83: return "Brake Torque Output Axle 1 Right";
            case  84: return "Brake Torque Output Axle 2 Left";
            case  85: return "Brake Torque Output Axle 2 Right";
            case  86: return "Brake Torque Output Axle 3 Left";
            case  87: return "Brake Torque Output Axle 3 Right";
            case  88: return "Vehicle Dynamic Stability Control System (VDC)";
            case  89: return "Steering Angle Sensor";
            case  90: return "Voltage Supply for Stability Control System";
            case  91: return "Brake Lining Display";
            case  92: return "Pressure Limitation Valve";
            case  93: return "Auxiliary Valve";
            case  94: return "Hill holder System";
            case  95:
            case  96:
            case  97: return "Voltage Supply";
            case  98:
            case  99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 130:
            case 131:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150: return "Reserved for future assignment by SAE";
        }
        return "?" + sid;
    }
    
    // ------------------------------------------------------------------------
    // MID[140,234] SIDs
    
    private static String getSIDDescription_140_234(int sid)
    {
        switch (sid) {
            case   0: return "Reserved";
            case   1: return "Left Fuel Level Sensor";
            case   2: return "Right Fuel Level Sensor";
            case   3: return "Fuel Feed Rate Sensor";
            case   4: return "Fuel Return Rate Sensor";
            case   5: return "Tachometer Gauge Coil";
            case   6: return "Speedometer Gauge Coil";
            case   7: return "Turbocharger Air Pressure Gauge Coil";
            case   8: return "Fuel Pressure Gauge Coil";
            case   9: return "Fuel Level Gauge Coil";
            case  10: return "Second Fuel Level Gauge Coil";
            case  11: return "Engine Oil Pressure Gauge Coil";
            case  12: return "Engine Oil Temperature Gauge Coil";
            case  13: return "Engine Coolant Temperature Gauge Coil";
            case  14: return "Pyrometer Gauge Coil";
            case  15: return "Transmission Oil Pressure Gauge Coil";
            case  16: return "Transmission Oil Temperature Gauge Coil";
            case  17: return "Forward Rear Axle Temperature Gauge Coil";
            case  18: return "Rear Rear Axle Temperature Gauge Coil";
            case  19: return "Voltmeter Gauge Coil";
            case  20: return "Primary Air Pressure Gauge Coil";
            case  21: return "Secondary Air Pressure Gauge Coil";
            case  22: return "Ammeter Gauge Coil";
            case  23: return "Air Application Gauge Coil";
            case  24: return "Air Restriction Gauge Coil";
            case  25:
            case  26:
            case  27:
            case  28:
            case  29:
            case  30:
            case  31:
            case  32:
            case  33:
            case  34:
            case  35:
            case  36:
            case  37:
            case  38:
            case  39:
            case  40:
            case  41:
            case  42:
            case  43:
            case  44:
            case  45:
            case  46:
            case  47:
            case  48:
            case  49:
            case  50:
            case  51:
            case  52:
            case  53:
            case  54:
            case  55:
            case  56:
            case  57:
            case  58:
            case  59:
            case  60:
            case  61:
            case  62:
            case  63:
            case  64:
            case  65:
            case  66:
            case  67:
            case  68:
            case  69:
            case  70:
            case  71:
            case  72:
            case  73:
            case  74:
            case  75:
            case  76:
            case  77:
            case  78:
            case  79:
            case  80:
            case  81:
            case  82:
            case  83:
            case  84:
            case  85:
            case  86:
            case  87:
            case  88:
            case  89:
            case  90:
            case  91:
            case  92:
            case  93:
            case  94:
            case  95:
            case  96:
            case  97:
            case  98:
            case  99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 130:
            case 131:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150: return "Reserved for future assignment by SAE";
        }
        return "?" + sid;
    }
    
    // ------------------------------------------------------------------------
    // MID[142] SIDs
    
    private static String getSIDDescription_142(int sid)
    {
        switch (sid) {
            case   0: return "Reserved";
            case   1: return "Timing Sensor";
            case   2: return "Timing Actuator";
            case   3: return "Fuel Rack Position Sensor";
            case   4: return "Fuel Rack Actuator";
            case   5: return "Oil Level Indicator Output";
            case   6: return "Tachometer Drive Output";
            case   7: return "Speedometer Drive Output";
            case   8: return "PWM Input (ABS/ASR)";
            case   9: return "PWM Output";
            case  10: return "Auxiliary Output #1";
            case  11: return "Auxiliary Output #2";
            case  12: return "Auxiliary Output #3";
            case  13:
            case  14:
            case  15:
            case  16:
            case  17:
            case  18:
            case  19:
            case  20:
            case  21:
            case  22:
            case  23:
            case  24:
            case  25:
            case  26:
            case  27:
            case  28:
            case  29:
            case  30:
            case  31:
            case  32:
            case  33:
            case  34:
            case  35:
            case  36:
            case  37:
            case  38:
            case  39:
            case  40:
            case  41:
            case  42:
            case  43:
            case  44:
            case  45:
            case  46:
            case  47:
            case  48:
            case  49:
            case  50:
            case  51:
            case  52:
            case  53:
            case  54:
            case  55:
            case  56:
            case  57:
            case  58:
            case  59:
            case  60:
            case  61:
            case  62:
            case  63:
            case  64:
            case  65:
            case  66:
            case  67:
            case  68:
            case  69:
            case  70:
            case  71:
            case  72:
            case  73:
            case  74:
            case  75:
            case  76:
            case  77:
            case  78:
            case  79:
            case  80:
            case  81:
            case  82:
            case  83:
            case  84:
            case  85:
            case  86:
            case  87:
            case  88:
            case  89:
            case  90:
            case  91:
            case  92:
            case  93:
            case  94:
            case  95:
            case  96:
            case  97:
            case  98:
            case  99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 130:
            case 131:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150: return "Reserved for future assignment by SAE";
        }
        return "?" + sid;
    }
    
    // ------------------------------------------------------------------------
    // MID SIDs

    public static String getSIDDescription(int mid, int sid)
    {
        
        /* common SID? */
        if (sid >= 151) {
            return J1587.getSIDDescription_common(sid);
        }
        
        switch (mid) {
            case 128:
            case 175:
            case 183:
            case 184:
            case 185:
            case 186: return getSIDDescription_128_175_183_184_185_186(sid);
            case 130: return getSIDDescription_130(sid);
            case 136:
            case 137:
            case 138:
            case 139:
            case 246:
            case 247: return getSIDDescription_136_137_138_139_246_247(sid);
            case 140:
            case 234: return getSIDDescription_140_234(sid);
            case 142: return getSIDDescription_142(sid);
        }
        
        return "?" + sid;
    }

}
