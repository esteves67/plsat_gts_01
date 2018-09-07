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
import org.opengts.dbtypes.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

public class UnassignedDevices
    extends DBRecord<UnassignedDevices>
{

    // ------------------------------------------------------------------------

    public static final long   MAX_AGE_SEC              = DateTime.DaySeconds(7);
    
    /* MobileID separator for "account,device" */
    // separator may be a multi char sequence (ie. "::", etc)
    public static final String ACCOUNT_DEVICE_SEP       = ",";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "UnassignedDevices";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_serverID             = "serverID";
    public static final String FLD_mobileID             = "mobileID";
    public static final String FLD_timestamp            = "timestamp";
    public static final String FLD_ipAddress            = "ipAddress";
    public static final String FLD_isDuplex             = "isDuplex";
    public static final String FLD_latitude             = "latitude";
    public static final String FLD_longitude            = "longitude";
    public static final String FLD_data                 = "data";
    private static DBField FieldInfo[] = {
        // UnassignedDevices fields
        new DBField(FLD_serverID        , String.class      , DBField.TYPE_ID()        , "Server ID"        , "key=true"),
        new DBField(FLD_mobileID        , String.class      , DBField.TYPE_STRING(32)  , "Mobile ID"        , "key=true"),
        new DBField(FLD_timestamp       , Long.TYPE         , DBField.TYPE_UINT32      , "Timestamp"        , ""),
        new DBField(FLD_ipAddress       , DTIPAddress.class , DBField.TYPE_STRING(32)  , "IP Address"       , ""),
        new DBField(FLD_isDuplex        , Boolean.TYPE      , DBField.TYPE_BOOLEAN     , "Is Duplex"        , ""),
        new DBField(FLD_latitude        , Double.TYPE       , DBField.TYPE_DOUBLE      , "Latitude"         , "format=#0.00000"),
        new DBField(FLD_longitude       , Double.TYPE       , DBField.TYPE_DOUBLE      , "Longitude"        , "format=#0.00000"),
        new DBField(FLD_data            , String.class      , DBField.TYPE_STRING(255) , "Data"             , ""),
        // Common fields
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends DBRecordKey<UnassignedDevices>
    {
        public Key() {
            super();
        }
        public Key(String serverId, String mobileId) {
            super.setFieldValue(FLD_serverID, ((serverId != null)? serverId.toLowerCase() : ""));
            super.setFieldValue(FLD_mobileID, ((mobileId != null)? mobileId.toLowerCase() : ""));
        }
        public DBFactory<UnassignedDevices> getFactory() {
            return UnassignedDevices.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<UnassignedDevices> factory = null;
    public static DBFactory<UnassignedDevices> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                UnassignedDevices.TABLE_NAME(), 
                UnassignedDevices.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                UnassignedDevices.class, 
                UnassignedDevices.Key.class,
                true/*editable*/,true/*viewable*/);
        }
        return factory;
    }

    /* Bean instance */
    public UnassignedDevices()
    {
        super();
    }

    /* database record */
    public UnassignedDevices(UnassignedDevices.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(UnassignedDevices.class, loc);
        return i18n.getString("UnassignedDevices.description", 
            "This table contains " +
            "Mobile IDs for which no Device record was found."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    public String getServerID()
    {
        String v = (String)this.getFieldValue(FLD_serverID);
        return StringTools.trim(v);
    }
    
    public void seServerID(String v)
    {
        this.setFieldValue(FLD_serverID, StringTools.trim(v));
    }
    
    // ------------------------------------------------------------------------

    public String getMobileID()
    {
        String v = (String)this.getFieldValue(FLD_mobileID);
        return StringTools.trim(v);
    }
    
    public void seMobileID(String v)
    {
        this.setFieldValue(FLD_mobileID, StringTools.trim(v));
    }
    
    // ------------------------------------------------------------------------

    public long getTimestamp()
    {
        Long v = (Long)this.getFieldValue(FLD_timestamp);
        return (v != null)? v.longValue() : 0L;
    }
    
    public void setTimestamp(long v)
    {
        this.setFieldValue(FLD_timestamp, v);
    }
    
    // ------------------------------------------------------------------------

    public DTIPAddress getIpAddress()
    {
        DTIPAddress v = (DTIPAddress)this.getFieldValue(FLD_ipAddress);
        return v; // May return null!!
    }

    public String getIpAddressString()
    {
        return StringTools.trim(this.getIpAddress());
    }

    public void setIpAddress(DTIPAddress v)
    {
        this.setFieldValue(FLD_ipAddress, v);
    }

    public void setIpAddress(String v)
    {
        this.setIpAddress((v != null)? new DTIPAddress(v) : null);
    }

    // ------------------------------------------------------------------------

    public boolean getIsDuplex()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_isDuplex);
        return (v != null)? v.booleanValue() : false;
    }

    public void setIsDuplex(boolean v)
    {
        this.setFieldValue(FLD_isDuplex, v);
    }
    
    public boolean isDuplex()
    {
        return this.getIsDuplex();
    }

    // ------------------------------------------------------------------------

    public double getLatitude()
    {
        return this.getFieldValue(FLD_latitude, 0.0);
    }
    
    public void setLatitude(double v)
    {
        this.setFieldValue(FLD_latitude, v);
    }

    // ------------------------------------------------------------------------

    public double getLongitude()
    {
        return this.getFieldValue(FLD_longitude, 0.0);
    }
    
    public void setLongitude(double v)
    {
        this.setFieldValue(FLD_longitude, v);
    }
    
    public GeoPoint getGeoPoint()
    {
        return new GeoPoint(this.getLatitude(), this.getLongitude());
    }

    // ------------------------------------------------------------------------
    
    public String getData()
    {
        String v = (String)this.getFieldValue(FLD_data);
        return StringTools.trim(v);
    }

    public void setData(String v)
    {
        this.setFieldValue(FLD_data, StringTools.truncate(v,255));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public String toString()
    {
        return this.getServerID() + "/" + this.getMobileID();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        //super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void add(String serverID, String mobileID)
    {
        UnassignedDevices.add(serverID, mobileID, 0L, null, false, 0.0, 0.0, null);
    }
    
    public static void add(String serverID, String mobileID,
        double latitude, double longitude)
    {
        UnassignedDevices.add(serverID, mobileID, 0L, null, false, latitude, longitude, null);
    }
    
    public static void add(String serverID, String mobileID,
        String ipAddress, boolean isDuplex,
        double latitude, double longitude)
    {
        UnassignedDevices.add(serverID, mobileID, 0L, ipAddress, isDuplex, latitude, longitude, null);
    }
    
    /* This is the method called by DCServerFactory.addUnassignedDevice(...) */
    public static void add(String serverID, String mobileID,
        String ipAddress, boolean isDuplex,
        double latitude, double longitude,
        String data)
    {
        UnassignedDevices.add(serverID, mobileID, 0L, ipAddress, isDuplex, latitude, longitude, data);
    }

    public static void add(String serverID, String mobileID,
        long timestamp, String ipAddress, boolean isDuplex,
        double latitude, double longitude,
        String data)
    {
        
        try {

            /* create key */
            UnassignedDevices.Key rcdKey = new UnassignedDevices.Key(serverID, mobileID);

            /* update */
            long ts = (timestamp > 0L)? timestamp : DateTime.getCurrentTimeSec();
            boolean exists = rcdKey.exists();
            if (exists) {
                UnassignedDevices rcd = rcdKey.getDBRecord(true);
                rcd.setTimestamp(ts);
                rcd.setIpAddress(ipAddress);
                rcd.setIsDuplex(isDuplex);
                rcd.setLatitude(latitude);
                rcd.setLongitude(longitude);
                rcd.setData(data);
                rcd.update();
            } else {
                UnassignedDevices rcd = rcdKey.getDBRecord();
                rcd.setCreationDefaultValues();
                rcd.setTimestamp(ts);
                rcd.setIpAddress(ipAddress);
                rcd.setIsDuplex(isDuplex);
                rcd.setLatitude(latitude);
                rcd.setLongitude(longitude);
                rcd.setData(data);
                rcd.insert();
            }
        
        } catch (Throwable th) {
            Print.logException("Unable to add UnknownDevice",th);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static void getRecordCallback(DBRecordHandler<UnassignedDevices> rcdHandler)
        throws DBException
    {

        // DBSelect: SELECT * FROM UnassignedDevices 
        DBSelect<UnassignedDevices> dsel = new DBSelect<UnassignedDevices>(UnassignedDevices.getFactory());

        /* iterate through records */
        DBRecord.select(dsel, rcdHandler);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void _iterate(DBRecordHandler<UnassignedDevices> rcdHandler)
        throws DBException
    {

        /* DBRecordHandler is required */
        if (rcdHandler == null) {
            throw new DBException("Missing DBRecordHandler");
        }
        
        // DBSelect: SELECT * FROM UnassignedDevices 
        DBSelect<UnassignedDevices> dsel = new DBSelect<UnassignedDevices>(UnassignedDevices.getFactory());
        dsel.setOrderByFields(FLD_serverID,FLD_mobileID);
        
        /* iterate through records */
        Statement stmt = null;
        ResultSet rs = null;
        try {
            DBRecord.select(dsel, rcdHandler);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
        }

    }

    public static long udList()
    {
        final AccumulatorLong counter = new AccumulatorLong(0L);

        /* record handler */
        DBRecordHandler<UnassignedDevices> rcdHandler = new DBRecordHandler<UnassignedDevices>() {
            public int handleDBRecord(UnassignedDevices rcd) throws DBException {
                UnassignedDevices ud = rcd;
                String serverID = ud.getServerID();
                String mobileID = ud.getMobileID();
                long now  = DateTime.getCurrentTimeSec();
                long time = ud.getTimestamp();
                String ipAddr = StringTools.trim(ud.getIpAddress());
                GeoPoint gp = ud.getGeoPoint();
                StringBuffer sb = new StringBuffer();
                sb.append("ServerID=").append(serverID);
                sb.append(" MobileID=").append(mobileID);
                sb.append(" Time=").append(time);
                sb.append(" Age=").append(now - time);
                if (!StringTools.isBlank(ipAddr)) {
                    sb.append(" IP=").append(ipAddr);
                }
                sb.append(" GPS=").append(gp.toString());
                Print.sysPrintln(sb.toString());
                counter.increment();
                return DBRH_SKIP;
            }
        };

        /* iterate */
        try {
            UnassignedDevices._iterate(rcdHandler);
        } catch (DBException dbe) {
            Print.logException("Listing UnassignedDevices", dbe);
        }

        /* return count */
        return counter.get();

    }

    public static void udUpdate(final boolean okDelete)
    {

        /* record handler */
        DBRecordHandler<UnassignedDevices> rcdHandler = new DBRecordHandler<UnassignedDevices>() {
            public int handleDBRecord(UnassignedDevices rcd) throws DBException {
                UnassignedDevices ud = rcd;
                String serverID = ud.getServerID();
                String mobileID = ud.getMobileID();

                /* check age */
                long ageSec = DateTime.getCurrentTimeSec() - ud.getTimestamp();
                if ((MAX_AGE_SEC > 0L) && (ageSec > MAX_AGE_SEC)) {
                    Print.logInfo("Record exceeds maximum age: [%s] %s", serverID, mobileID);
                    if (okDelete) {
                        // TODO: delete record and continue
                    }
                    return DBRH_SKIP;
                }

                /* see if device now exists in the database */
                int p = mobileID.indexOf(ACCOUNT_DEVICE_SEP);
                if (p >= 0) {
                    String accountID = mobileID.substring(0,p);
                    String deviceID  = mobileID.substring(p+ACCOUNT_DEVICE_SEP.length());
                    if (Transport.exists(accountID,deviceID)) {
                        Print.logInfo("Record is now defined: [%s] %s/%s", serverID, accountID, deviceID);
                        if (okDelete) {
                            // TODO: delete record and continue
                        }
                        return DBRH_SKIP;
                    }
                } else {
                    DCServerConfig dcs = DCServerFactory._getServerConfig(serverID);
                    if (dcs != null) {
                        String pfx[] = dcs.getUniquePrefix();
                        for (int i = 0; i < pfx.length; i++) {
                            String uniqueID = pfx[i] + mobileID;
                            if (Transport.exists(uniqueID)) {
                                Print.logInfo("MobileID is now defined: [%s] %s", serverID, uniqueID);
                                if (okDelete) {
                                    // TODO: delete record and continue
                                }
                                return DBRH_SKIP;
                            }
                        }
                    }
                }
                return DBRH_SKIP;

            }
        };

        /* iterate */
        try {
            UnassignedDevices._iterate(rcdHandler);
        } catch (DBException dbe) {
            Print.logException("Listing UnassignedDevices", dbe);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_LIST[]      = new String[] { "list"   };
    private static final String ARG_UPDATE[]    = new String[] { "update" };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + UserAcl.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -list        List table contents");
        Print.logInfo("  -update      Update unassigned devices");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main

        /* list */
        if (RTConfig.getBoolean(ARG_LIST,false)) {
            UnassignedDevices.udList();
            System.exit(0);
        }

        /* update */
        if (RTConfig.hasProperty(ARG_UPDATE)) {
            boolean delete = RTConfig.getString(ARG_UPDATE,"").equals("delete");
            UnassignedDevices.udUpdate(delete);
            System.exit(0);
        }

        /* no options specified */
        Print.logWarn("Missing options ...");
        usage();

    }

    // ------------------------------------------------------------------------

}
