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
package org.opengts.extra.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class Antx
    extends DeviceRecord<Antx>
{

    // ------------------------------------------------------------------------

    public static final int   TYPE_VALUE                = 0;
    public static final int   TYPE_MINMAX               = 1;
    public static final int   TYPE_BIN                  = 2;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "Antx";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_timestamp            = "timestamp";          // timestamp
    public static final String FLD_fieldID              = "fieldID";            // field number
    public static final String FLD_channelID            = "channelID";          // channel number
    public static final String FLD_type                 = "type";               // 0=min/max, 1=bin
    public static final String FLD_data0                = "data0";              // Data 0 (minimum)
    public static final String FLD_data1                = "data1";              // Data 1 (maximum)
    public static final String FLD_data2                = "data2";              // Data 2
    public static final String FLD_data3                = "data3";              // Data 3
    public static final String FLD_data4                = "data4";              // Data 4
    private static DBField FieldInfo[] = {
        // Antx fields
        AccountRecord.newField_accountID(true),
        DeviceRecord.newField_deviceID(true),
        new DBField(FLD_timestamp       , Long.TYPE     , DBField.TYPE_UINT32      , "Timestamp"        , "key=true"),
        new DBField(FLD_fieldID         , Integer.TYPE  , DBField.TYPE_INT32       , "Field Number"     , "key=true"),
        new DBField(FLD_channelID       , Integer.TYPE  , DBField.TYPE_INT32       , "Channel Number"   , "key=true"),
        new DBField(FLD_type            , Integer.TYPE  , DBField.TYPE_INT16       , "Type"             , ""),
        new DBField(FLD_data0           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Data 0"           , "format=#0.0"),
        new DBField(FLD_data1           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Data 1"           , "format=#0.0"),
        new DBField(FLD_data2           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Data 2"           , "format=#0.0"),
        new DBField(FLD_data3           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Data 3"           , "format=#0.0"),
        new DBField(FLD_data4           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Data 4"           , "format=#0.0"),
        // Common fields
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends DeviceKey<Antx>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String devId, long timestamp, int field, int channel) {
            super.setFieldValue(FLD_accountID , ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID  , ((devId  != null)? devId.toLowerCase()  : ""));
            super.setFieldValue(FLD_timestamp , timestamp);
            super.setFieldValue(FLD_fieldID   , field);
            super.setFieldValue(FLD_channelID , channel);
        }
        public DBFactory<Antx> getFactory() {
            return Antx.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<Antx> factory = null;
    public static DBFactory<Antx> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                Antx.TABLE_NAME(), 
                Antx.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                Antx.class, 
                Antx.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(Device.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public Antx()
    {
        super();
    }

    /* database record */
    public Antx(Antx.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(Antx.class, loc);
        return i18n.getString("Antx.description", 
            "This table contains " + 
            "Antx Messenger information."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    public long getTimestamp()
    {
        Long v = (Long)this.getFieldValue(FLD_timestamp);
        return (v != null)? v.longValue() : 0L;
    }
    
    public void setTimestamp(long v)
    {
        this.setFieldValue(FLD_timestamp, v);
    }

    public String getTimestampString()
    {
        Account a = this.getAccount();
        String dateFmt = (a != null)? a.getDateFormat() : BasicPrivateLabel.getDefaultDateFormat();
        String timeFmt = (a != null)? a.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat();
        DateTime dt = new DateTime(this.getTimestamp());
        return dt.gmtFormat(dateFmt + " " + timeFmt + " z");
    }

    // ------------------------------------------------------------------------

    public int getFieldID()
    {
        Integer v = (Integer)this.getFieldValue(FLD_fieldID);
        return (v != null)? v.intValue() : 0;
    }
    
    public void setFieldID(int v)
    {
        this.setFieldValue(FLD_fieldID, v);
    }

    // ------------------------------------------------------------------------

    public int getChannelID()
    {
        Integer v = (Integer)this.getFieldValue(FLD_channelID);
        return (v != null)? v.intValue() : 0;
    }
    
    public void setChannelID(int v)
    {
        this.setFieldValue(FLD_channelID, v);
    }

    // ------------------------------------------------------------------------

    public int getType()
    {
        Integer v = (Integer)this.getFieldValue(FLD_type);
        return (v != null)? v.intValue() : 0;
    }
    
    public void setType(int v)
    {
        this.setFieldValue(FLD_type, v);
    }

    // ------------------------------------------------------------------------

    public double getData0()
    {
        Double v = (Double)this.getFieldValue(FLD_data0);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    public void setData0(double v)
    {
        this.setFieldValue(FLD_data0, v);
    }

    // ------------------------------------------------------------------------

    public double getData1()
    {
        Double v = (Double)this.getFieldValue(FLD_data1);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    public void setData1(double v)
    {
        this.setFieldValue(FLD_data1, v);
    }

    // ------------------------------------------------------------------------

    public double getData2()
    {
        Double v = (Double)this.getFieldValue(FLD_data2);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    public void setData2(double v)
    {
        this.setFieldValue(FLD_data2, v);
    }

    // ------------------------------------------------------------------------

    public double getData3()
    {
        Double v = (Double)this.getFieldValue(FLD_data3);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    public void setData3(double v)
    {
        this.setFieldValue(FLD_data3, v);
    }

    // ------------------------------------------------------------------------

    public double getData4()
    {
        Double v = (Double)this.getFieldValue(FLD_data4);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    public void setData4(double v)
    {
        this.setFieldValue(FLD_data4, v);
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static Antx.Key createKey(Device device, long timestamp,
        int field, int channel)
    {
        if (device != null) {
            String acctID = device.getAccountID();
            String devID  = device.getDeviceID();
            Antx.Key key  = new Antx.Key(acctID, devID, timestamp, field, channel);
            return key;
        } else {
            return null;
        }
    }

    public static boolean addEvent(Device device, long timestamp,
        int field, int channel, 
        double value)
    {
        return Antx.addEvent(device, timestamp, field, channel, TYPE_VALUE, value, 0.0, 0.0, 0.0, 0.0);
    }

    public static boolean addEvent(Device device, long timestamp,
        int field, int channel, 
        double minValue, double maxValue)
    {
        return Antx.addEvent(device, timestamp, field, channel, TYPE_MINMAX, minValue, maxValue, 0.0, 0.0, 0.0);
    }

    public static boolean addEvent(Device device, long timestamp,
        int field, int channel, 
        double data0, double data1, double data2, double data3, double data4)
    {
        return Antx.addEvent(device, timestamp, field, channel, TYPE_BIN, data0, data1, data2, data3, data4);
    }

    public static boolean addEvent(Device device, long timestamp,
        int field, int channel, int type,
        double data0, double data1, double data2, double data3, double data4)
    {
        Antx.Key key = Antx.createKey(device, timestamp, field, channel);
        if (key != null) {
            Antx rcd = key.getDBRecord();
            rcd.setType(type);
            rcd.setData0(data0);
            rcd.setData1(data1);
            rcd.setData2(data2);
            rcd.setData3(data3);
            rcd.setData4(data4);
            try {
                rcd.save();
                return true;
            } catch (DBException dbe) {
                Print.logError("Unable to save Antx record: " + dbe);
                return false;
            }
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static void getRecordCallback(DBRecordHandler<Antx> rcdHandler,
        String accountID, String deviceID,
        long startTime, long endTime,
        long limit)
        throws DBException
    {

        /* invalid time range */
        if ((startTime > 0L) && (endTime > 0L) && (startTime > endTime)) {
            //Print.logWarn("Invalid time range specified ...");
            return;
        }

        /* Select */
        DBSelect<Antx> dsel = new DBSelect<Antx>(Antx.getFactory());
        dsel.setOrderByFields(FLD_accountID, FLD_deviceID, FLD_timestamp);
        dsel.setOrderAscending(true);
        dsel.setLimit(limit);

        /* Where */
        DBWhere dwh = new DBWhere(Antx.getFactory());
        dwh.append(dwh.AND(
            dwh.EQ(Antx.FLD_accountID,StringTools.trim(accountID)),
            dwh.EQ(Antx.FLD_deviceID ,StringTools.trim(deviceID))
        ));
        if (startTime >= 0L) {
            // AND (timestamp>=123436789)
            dwh.append(dwh.AND_(dwh.GE(Antx.FLD_timestamp,startTime)));
        }
        if ((endTime >= 0L) && (endTime >= startTime)) {
            // AND (timestamp<=123456789)
            dwh.append(dwh.AND_(dwh.LE(Antx.FLD_timestamp,endTime)));
        }
        dsel.setWhere(dwh.toString());
        Print.logInfo("Where: " + dwh);

        /* iterate through records */
        DBRecord.select(dsel, rcdHandler);

    }

    // ------------------------------------------------------------------------

}
