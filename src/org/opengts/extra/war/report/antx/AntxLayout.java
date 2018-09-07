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
//  Report definition based on generic field definitions
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.extra.war.report.antx;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.report.*;

import org.opengts.extra.tables.*;

public class AntxLayout
    extends ReportLayout
{

    // ------------------------------------------------------------------------
    // Data keys
    // - These define what data is available (see 'AntxDataRow') and what columns will be 
    //   displayed in the table.
    // - Column names must only contain <alpha>/<numeric>/'_' characters

    public static final String  DATA_INDEX              = "index";
    
    public static final String  DATA_DEVICE_ID          = "deviceID";       // field
    public static final String  DATA_DEVICE_DESC        = "deviceDesc";     // derived
    
    public static final String  DATA_TIMESTAMP          = "timestamp";      // field
    public static final String  DATA_DATETIME           = "dateTime";       // derived
    public static final String  DATA_DATE               = "date";           // derived

    public static final String  DATA_FIELD_ID           = "fieldID";        // field
    public static final String  DATA_FIELD_DESC         = "fieldDesc";      // derived
    
    public static final String  DATA_CHANNEL_ID         = "channelID";      // field
    public static final String  DATA_CHANNEL_DESC       = "channelDesc";    // derived
    
    public static final String  DATA_TYPE               = "type";           // field
    
    public static final String  DATA_DATA_0             = "data0";          // field
    public static final String  DATA_DATA_1             = "data1";          // field
    public static final String  DATA_DATA_2             = "data2";          // field
    public static final String  DATA_DATA_3             = "data3";          // field
    public static final String  DATA_DATA_4             = "data4";          // field
    
    public static final String  DATA_FUEL_TOTAL         = "totalFuel";      // field
    public static final String  DATA_FUEL_PTO           = "ptoFuel";        // field
    public static final String  DATA_FUEL_IDLE          = "idleFuel";       // field
    public static final String  DATA_FUEL_WORK          = "workFuel";       // field
    public static final String  DATA_DISTANCE           = "distance";       // field
    public static final String  DATA_SPEED              = "speed";          // field

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // AntxLayout is a singleton
    
    private static AntxLayout reportDef = null;

    public static ReportLayout getReportLayout()
    {
        if (reportDef == null) {
            reportDef = new AntxLayout();
        }
        return reportDef;
    }

    private AntxLayout()
    {
        super();
        this.setDataRowTemplate(new AntxDataRow());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static class AntxDataRow
        extends DataRowTemplate
    {
        public AntxDataRow() {
            super();

            // Index
            this.addColumnTemplate(new DataColumnTemplate(DATA_INDEX) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    if (rowNdx < 0) {
                        return "";
                    } else {
                        int ofs = 1;
                        if ((arg != null) && (arg.length() > 0) && (arg.charAt(0) == '0')) {
                            ofs = 0;
                        }
                        return String.valueOf(rowNdx + ofs);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    return "#";
                }
            });

            // Device-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    AntxData ad = (AntxData)obj;
                    String devID = ad.getString(DATA_DEVICE_ID);
                    if (StringTools.isBlank(devID)) {
                        Device dev = ad.getDevice();
                        if (dev != null) {
                            devID = dev.getDeviceID();
                        }
                    }
                    return devID;
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.deviceID","Device-ID");
                }
            });

            // Device Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String  arg = rc.getArg();
                    AntxData ad = (AntxData)obj;
                    String desc = ad.getString(DATA_DEVICE_DESC);
                    if (StringTools.isBlank(desc)) {
                        Device dev = ad.getDevice();
                        if (dev != null) {
                            desc = dev.getDescription();
                        }
                    }
                    return desc;
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.deviceDescription","Device\nDescription");
                }
            });
            
            /* Date/Time */
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    AntxData fd = (AntxData)obj;
                    long ts = fd.getLong(DATA_TIMESTAMP);
                    return String.valueOf(ts);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.timestamp","Timestamp") + "\n(Epoch)";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    AntxData fd = (AntxData)obj;
                    long ts = fd.getLong(DATA_TIMESTAMP);
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        return dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                    } else {
                        return "";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.dateTime","Date/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    AntxData fd = (AntxData)obj;
                    long ts = fd.getLong(DATA_TIMESTAMP);
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        return dt.format(rl.getDateFormat(rd.getPrivateLabel()), tz);
                    } else {
                        return "";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.date","Date");
                }
            });

            // Field ID/Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_FIELD_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    AntxData fd = (AntxData)obj;
                    String fid = fd.getString(DATA_FIELD_ID);
                    return StringTools.trim(fid);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.fieldID","Field-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FIELD_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String  arg = rc.getArg();
                    AntxData fd = (AntxData)obj;
                    String desc = fd.getString(DATA_FIELD_DESC);
                    if (StringTools.isBlank(desc)) {
                        int fid = fd.getInt(DATA_FIELD_ID);
                        desc = AntxField.GetFieldDescription(fid);
                        if ("id".equalsIgnoreCase(arg) && !StringTools.isBlank(desc)) {
                            desc = desc + " (" + fid + ")";
                        }
                    }
                    return StringTools.trim(desc);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.fieldDesc","Field\nDescription");
                }
            });

            // Channel ID/Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_CHANNEL_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    AntxData fd = (AntxData)obj;
                    String cid = fd.getString(DATA_CHANNEL_ID);
                    return StringTools.trim(cid);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.channelID","Channel-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CHANNEL_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String  arg = rc.getArg();
                    AntxData fd = (AntxData)obj;
                    String desc = fd.getString(DATA_CHANNEL_DESC);
                    if (StringTools.isBlank(desc)) {
                        int chan = fd.getInt(DATA_CHANNEL_ID);
                        desc = AntxChannel.GetChannelDescription(chan);
                        if ("id".equalsIgnoreCase(arg) && !StringTools.isBlank(desc)) {
                            desc = desc + " (" + chan + ")";
                        }
                    }
                    return StringTools.trim(desc);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.channelDesc","Channel\nDescription");
                }
            });

            // Type
            this.addColumnTemplate(new DataColumnTemplate(DATA_TYPE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    AntxData fd = (AntxData)obj;
                    int type = fd.getInt(DATA_TYPE);
                    switch (type) {
                        case Antx.TYPE_VALUE  : return "Value";
                        case Antx.TYPE_MINMAX : return "Min/Max";
                        case Antx.TYPE_BIN    : return "Bin";
                        default               : return "?";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.type","Type");
                }
            });

            // Data 0
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATA_0) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    Account acct = rd.getAccount();
                    AntxData  fd = (AntxData)obj;
                    int     type = fd.getInt(DATA_TYPE);
                    int     fld  = fd.getInt(DATA_FIELD_ID);
                    int     chan = fd.getInt(DATA_CHANNEL_ID);
                    String  unit = AntxChannel.GetChannelUnitsDescription(acct, fld, chan);
                    double  data = AntxChannel.GetAccountUnitsValue(acct, fld, chan, fd.getDouble(DATA_DATA_0));
                    return StringTools.format(data, getDataFormat(arg)) + (!StringTools.isBlank(unit)?(" "+unit):"");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.data0","Data #0");
                }
            });

            // Data 1
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATA_1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    Account acct = rd.getAccount();
                    AntxData  fd = (AntxData)obj;
                    int     type = fd.getInt(DATA_TYPE);
                    if (type == Antx.TYPE_VALUE) {
                        return "--";
                    }
                    int     fld  = fd.getInt(DATA_FIELD_ID);
                    int     chan = fd.getInt(DATA_CHANNEL_ID);
                    String  unit = AntxChannel.GetChannelUnitsDescription(acct, fld, chan);
                    double  data = AntxChannel.GetAccountUnitsValue(acct, fld, chan, fd.getDouble(DATA_DATA_1));
                    return StringTools.format(data, getDataFormat(arg)) + (!StringTools.isBlank(unit)?(" "+unit):"");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.data1","Data #1");
                }
            });

            // Data 2
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATA_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    Account acct = rd.getAccount();
                    AntxData  fd = (AntxData)obj;
                    int     type = fd.getInt(DATA_TYPE);
                    if ((type == Antx.TYPE_VALUE) || (type == Antx.TYPE_MINMAX)) {
                        return "--";
                    }
                    int     fld  = fd.getInt(DATA_FIELD_ID);
                    int     chan = fd.getInt(DATA_CHANNEL_ID);
                    String  unit = AntxChannel.GetChannelUnitsDescription(acct, fld, chan);
                    double  data = AntxChannel.GetAccountUnitsValue(acct, fld, chan, fd.getDouble(DATA_DATA_2));
                    return StringTools.format(data, getDataFormat(arg)) + (!StringTools.isBlank(unit)?(" "+unit):"");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.data2","Data #2");
                }
            });

            // Data 3
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATA_3) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    Account acct = rd.getAccount();
                    AntxData  fd = (AntxData)obj;
                    int     type = fd.getInt(DATA_TYPE);
                    if ((type == Antx.TYPE_VALUE) || (type == Antx.TYPE_MINMAX)) {
                        return "--";
                    }
                    int     fld  = fd.getInt(DATA_FIELD_ID);
                    int     chan = fd.getInt(DATA_CHANNEL_ID);
                    String  unit = AntxChannel.GetChannelUnitsDescription(acct, fld, chan);
                    double  data = AntxChannel.GetAccountUnitsValue(acct, fld, chan, fd.getDouble(DATA_DATA_3));
                    return StringTools.format(data, getDataFormat(arg)) + (!StringTools.isBlank(unit)?(" "+unit):"");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.data3","Data #3");
                }
            });

            // Data 4
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATA_4) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    Account acct = rd.getAccount();
                    AntxData  fd = (AntxData)obj;
                    int     type = fd.getInt(DATA_TYPE);
                    if ((type == Antx.TYPE_VALUE) || (type == Antx.TYPE_MINMAX)) {
                        return "--";
                    }
                    int     fld  = fd.getInt(DATA_FIELD_ID);
                    int     chan = fd.getInt(DATA_CHANNEL_ID);
                    String  unit = AntxChannel.GetChannelUnitsDescription(acct, fld, chan);
                    double  data = AntxChannel.GetAccountUnitsValue(acct, fld, chan, fd.getDouble(DATA_DATA_4));
                    return StringTools.format(data, getDataFormat(arg)) + (!StringTools.isBlank(unit)?(" "+unit):"");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.data4","Data #4");
                }
            });

            // Speed
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    AntxData  ad = (AntxData)obj;
                    if (ad.hasValue(DATA_SPEED)) {
                        Account a = rd.getAccount();
                        double kph = Account.getSpeedUnits(a).convertFromKPH(ad.getDouble(DATA_SPEED));
                        return StringTools.format(kph, getDataFormat(arg));
                    }
                    return "--";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.speed","Speed") + "\n${speedUnits}";
                }
            });

            // Distance
            this.addColumnTemplate(new DataColumnTemplate(DATA_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    AntxData  ad = (AntxData)obj;
                    if (ad.hasValue(DATA_DISTANCE)) {
                        Account a = rd.getAccount();
                        double km = Account.getDistanceUnits(a).convertFromKM(ad.getDouble(DATA_DISTANCE));
                        return StringTools.format(km, getDataFormat(arg));
                    }
                    return "--";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.distance","Distance") + "\n${distanceUnits}";
                }
            });

            // Fuel
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_TOTAL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    AntxData  ad = (AntxData)obj;
                    if (ad.hasValue(DATA_FUEL_TOTAL)) {
                        Account a = rd.getAccount();
                        double liters = Account.getVolumeUnits(a).convertFromLiters(ad.getDouble(DATA_FUEL_TOTAL));
                        return StringTools.format(liters, getDataFormat(arg));
                    }
                    return "--";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.totalFuel","Total Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_PTO) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    AntxData  ad = (AntxData)obj;
                    if (ad.hasValue(DATA_FUEL_PTO)) {
                        Account a = rd.getAccount();
                        double liters = Account.getVolumeUnits(a).convertFromLiters(ad.getDouble(DATA_FUEL_PTO));
                        return StringTools.format(liters, getDataFormat(arg));
                    }
                    return "--";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.ptoFuel","PTO Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_IDLE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    AntxData  ad = (AntxData)obj;
                    if (ad.hasValue(DATA_FUEL_IDLE)) {
                        Account a = rd.getAccount();
                        double liters = Account.getVolumeUnits(a).convertFromLiters(ad.getDouble(DATA_FUEL_IDLE));
                        return StringTools.format(liters, getDataFormat(arg));
                    }
                    return "--";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.idleFuel","Idle Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_WORK) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    AntxData  ad = (AntxData)obj;
                    if (ad.hasValue(DATA_FUEL_WORK)) {
                        Account a = rd.getAccount();
                        double liters = Account.getVolumeUnits(a).convertFromLiters(ad.getDouble(DATA_FUEL_WORK));
                        return StringTools.format(liters, getDataFormat(arg));
                    }
                    return "--";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(AntxLayout.class);
                    return i18n.getString("AntxLayout.workFuel","Work Fuel") + "\n${volumeUnits}";
                }
            });

        }

        public String getDataFormat(String arg) {
            String  fmt = "#0";
            if ((arg != null) && (arg.length() > 0)) {
                switch (arg.charAt(0)) {
                    case '0': fmt = "#0";      break;
                    case '1': fmt = "#0.0";    break;
                    case '2': fmt = "#0.00";   break;
                    case '3': fmt = "#0.000";  break;
                    case '4': fmt = "#0.0000"; break;
                }
            }
            return fmt;
        }

    }
   
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
