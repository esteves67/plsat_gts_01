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
// Notes:
// - Wireless service provider billing systems may round up data transmitted
//   during a PDP context [http://en.wikipedia.org/wiki/GPRS_Core_Network] to the
//   next higher 1K (1000?/1024?)
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

public class SessionStats
    extends DeviceRecord<SessionStats>
{

    // ------------------------------------------------------------------------

    public static long UDP_OVERHEAD             = SessionStatsFactory.UDP_OVERHEAD;         // per packet (includes IP overhead)
    public static long TCP_OVERHEAD             = SessionStatsFactory.TCP_OVERHEAD;         // per packet (includes IP overhead)
    public static long TCP_SESSION_OVERHEAD     = SessionStatsFactory.TCP_SESSION_OVERHEAD; // per TCP session (setup/teardown)

    // ------------------------------------------------------------------------

    private static SessionStatsFactory statsFactory = null;

    private static void initSessionStatsFactory()
    {
        if (SessionStats.statsFactory == null) {
            Print.logDebug("Initializing SessionStatsFactory ...");
            SessionStats.statsFactory = new SessionStatsFactory() {
                public void addSessionStatistic(Device device, long timestamp, String ipAddr, boolean isDuplex,
                    long bytesRead, long bytesWritten, long eventsRecv) throws DBException {
                    SessionStats.addStatistic(device,timestamp,ipAddr,isDuplex,bytesRead,bytesWritten,eventsRecv);
                }
                public long[] getByteCounts(Device device, long timeStart, long timeEnd) throws DBException {
                    return SessionStats.getByteCounts(device,timeStart,timeEnd);
                }
                public long[] getConnectionCounts(Device device, long timeStart, long timeEnd) throws DBException {
                    return SessionStats.getConnectionCounts(device,timeStart,timeEnd);
                }
            };
            Device.setSessionStatsFactory(SessionStats.statsFactory);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "SessionStats";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_timestamp            = "timestamp";
    public static final String FLD_ipAddress            = "ipAddress";
    public static final String FLD_isDuplex             = "isDuplex";
    public static final String FLD_bytesRead            = "bytesRead";
    public static final String FLD_bytesWritten         = "bytesWritten";
    public static final String FLD_bytesOverhead        = "bytesOverhead";
    public static final String FLD_bytesRounded         = "bytesRounded";
    public static final String FLD_eventsReceived       = "eventsReceived";
    private static DBField FieldInfo[] = {
        // SessionStats fields
        AccountRecord.newField_accountID(true),
        DeviceRecord.newField_deviceID(true),
        new DBField(FLD_timestamp       , Long.TYPE         , DBField.TYPE_UINT32      , "Timestamp"        , "key=true"),
        new DBField(FLD_ipAddress       , DTIPAddress.class , DBField.TYPE_STRING(32)  , "IP Address"       , null),
        new DBField(FLD_isDuplex        , Boolean.TYPE      , DBField.TYPE_BOOLEAN     , "Is Duplex"        , null),
        new DBField(FLD_bytesRead       , Long.TYPE         , DBField.TYPE_UINT32      , "Bytes Read"       , null),
        new DBField(FLD_bytesWritten    , Long.TYPE         , DBField.TYPE_UINT32      , "Bytes Written"    , null),
        new DBField(FLD_bytesOverhead   , Long.TYPE         , DBField.TYPE_UINT32      , "Bytes Overhead"   , null),
        new DBField(FLD_bytesRounded    , Long.TYPE         , DBField.TYPE_UINT32      , "Bytes Rounded"    , null),
        new DBField(FLD_eventsReceived  , Long.TYPE         , DBField.TYPE_UINT32      , "Events Received"  , null),
        // Common fields
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends DeviceKey<SessionStats>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String devId, long timestamp) {
            super.setFieldValue(FLD_accountID , ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID  , ((devId  != null)? devId.toLowerCase()  : ""));
            super.setFieldValue(FLD_timestamp , timestamp);
        }
        public DBFactory<SessionStats> getFactory() {
            return SessionStats.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<SessionStats> factory = null;
    public static DBFactory<SessionStats> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                SessionStats.TABLE_NAME(), 
                SessionStats.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                SessionStats.class, 
                SessionStats.Key.class,
                false/*editable*/,false/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(Device.TABLE_NAME());
            SessionStats.initSessionStatsFactory();
        }
        return factory;
    }

    /* Bean instance */
    public SessionStats()
    {
        super();
    }

    /* database record */
    public SessionStats(SessionStats.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(SessionStats.class, loc);
        return i18n.getString("SessionStats.description", 
            "This table contains " +
            "Device specific session connection statistics."
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
    
    // ------------------------------------------------------------------------

    public DTIPAddress getIpAddress()
    {
        DTIPAddress v = (DTIPAddress)this.getFieldValue(FLD_ipAddress);
        return v; // May return null!!
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

    public long getBytesRead()
    {
        Long v = (Long)this.getFieldValue(FLD_bytesRead);
        return (v != null)? v.longValue() : 0L;
    }
    
    public void setBytesRead(long v)
    {
        this.setFieldValue(FLD_bytesRead, v);
    }
    
    // ------------------------------------------------------------------------

    public long getBytesWritten()
    {
        Long v = (Long)this.getFieldValue(FLD_bytesWritten);
        return (v != null)? v.longValue() : 0L;
    }
    
    public void setBytesWritten(long v)
    {
        this.setFieldValue(FLD_bytesWritten, v);
    }
    
    // ------------------------------------------------------------------------

    public long getBytesOverhead()
    {
        Long v = (Long)this.getFieldValue(FLD_bytesOverhead);
        return (v != null)? v.longValue() : 0L;
    }
    
    public void setBytesOverhead(long v)
    {
        this.setFieldValue(FLD_bytesOverhead, v);
    }
    
    // ------------------------------------------------------------------------

    public long getBytesRounded()
    {
        Long v = (Long)this.getFieldValue(FLD_bytesRounded);
        return (v != null)? v.longValue() : 0L;
    }
    
    public void setBytesRounded(long v)
    {
        this.setFieldValue(FLD_bytesRounded, v);
    }

    // ------------------------------------------------------------------------

    public long getEventsReceived()
    {
        Long v = (Long)this.getFieldValue(FLD_eventsReceived);
        return (v != null)? v.longValue() : 0L;
    }
    
    public void setEventsReceived(long v)
    {
        this.setFieldValue(FLD_eventsReceived, v);
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void addStatistic(Device device, long timestamp, 
        String ipAddr, boolean isDuplex,
        long bytesRead, long bytesWritten, 
        long eventsRecv)
        throws DBException
    {
        if (device != null) {
            String a = device.getAccountID();
            String d = device.getDeviceID();
            SessionStats.addStatistic(a,d,timestamp,ipAddr,isDuplex,bytesRead,bytesWritten,eventsRecv);
        }
    }

    public static void addStatistic(String acctID, String devID, long timestamp, 
        String ipAddr, boolean isDuplex,
        long bytesRead, long bytesWritten, 
        long eventsRecv)
        throws DBException
    {
        SessionStats.Key connKey = new SessionStats.Key(acctID, devID, timestamp);
        SessionStats conn = connKey.getDBRecord();
        conn.setIpAddress(ipAddr);
        conn.setIsDuplex(isDuplex);
        conn.setBytesRead(bytesRead);
        conn.setBytesWritten(bytesWritten);
        conn.setEventsReceived(eventsRecv);
        long overhead = isDuplex? ((TCP_OVERHEAD * 2L) + TCP_SESSION_OVERHEAD) : UDP_OVERHEAD;
        conn.setBytesOverhead(overhead);
        long rounded  = (((bytesRead + bytesWritten + overhead) + 1023L) / 1024L) * 1024L;
        conn.setBytesRounded(rounded);
        conn.save();
    }

    // ------------------------------------------------------------------------

    /* return number of connections */
    public static long[] getConnectionCounts(Device dev, long timeStart, long timeEnd)
    {
        
        /* device specified? */
        if (dev == null) {
            return null;
        }
        String acctID  = dev.getAccountID();
        String devID   = dev.getDeviceID();

        /* default range */
        if (timeStart <= 0L) { timeStart = 1L; }
        if (timeEnd   <= 0L) { timeEnd   = Long.MAX_VALUE; }
        if (timeEnd < timeStart) {
            return null;
        }

        /* initialize count */
        long count[] = new long[] { -1L, -1L };

        /* TCP connections */
        {
            DBWhere dwhTCP = new DBWhere(SessionStats.getFactory());
            dwhTCP.append(dwhTCP.AND(
                dwhTCP.EQ(SessionStats.FLD_accountID, acctID),
                dwhTCP.EQ(SessionStats.FLD_deviceID , devID),
                dwhTCP.EQ(SessionStats.FLD_isDuplex , true),
                dwhTCP.GE(SessionStats.FLD_timestamp, timeStart),
                dwhTCP.LE(SessionStats.FLD_timestamp, timeEnd)
            ));
            try {
                count[0] = DBRecord.getRecordCount(SessionStats.getFactory(), dwhTCP.WHERE(dwhTCP.toString()));
            } catch (DBException dbe) {
                Print.logException("Counting TCP Connections", dbe);
            }
        }

        /* UDP connections */
        {
            DBWhere dwhUDP = new DBWhere(SessionStats.getFactory());
            dwhUDP.append(dwhUDP.AND(
                dwhUDP.EQ(SessionStats.FLD_accountID, acctID),
                dwhUDP.EQ(SessionStats.FLD_deviceID , devID),
                dwhUDP.EQ(SessionStats.FLD_isDuplex , false),
                dwhUDP.GE(SessionStats.FLD_timestamp, timeStart),
                dwhUDP.LE(SessionStats.FLD_timestamp, timeEnd)
            ));
            try {
                count[1] = DBRecord.getRecordCount(SessionStats.getFactory(), dwhUDP.WHERE(dwhUDP.toString()));
            } catch (DBException dbe) {
                Print.logException("Counting UDP Connections", dbe);
            }
        }
        
        /* return count */
        return count;

    }

    // ------------------------------------------------------------------------

    /* return bytesRead/bytesWritten */
    public static long[] getByteCounts(Device dev, long timeStart, long timeEnd)
        throws DBException
    {
        
        /* device specified? */
        if (dev == null) {
            return null;
        }
        String acctID = dev.getAccountID();
        String devID  = dev.getDeviceID();
        
        /* return byte counts */
        return SessionStats.getByteCounts(acctID, devID, timeStart, timeEnd);

    }
    
    /* return bytesRead/bytesWritten */
    public static long[] getByteCounts(String acctID, String devID, long timeStart, long timeEnd)
        throws DBException
    {

        /* device specified? */
        if ((acctID == null) || (devID == null)) {
            return null;
        }

        /* default range */
        if (timeStart <= 0L) { timeStart = 1L; }
        if (timeEnd   <= 0L) { timeEnd   = Long.MAX_VALUE; }
        if (timeEnd < timeStart) {
            return null;
        }

        /* SUM columns */
        String SUM_bytesRead     = DBProvider.FLD_SUM(SessionStats.FLD_bytesRead);
        String SUM_bytesWritten  = DBProvider.FLD_SUM(SessionStats.FLD_bytesWritten);
        String SUM_bytesOverhead = DBProvider.FLD_SUM(SessionStats.FLD_bytesOverhead);
        String SUM_bytesRounded  = DBProvider.FLD_SUM(SessionStats.FLD_bytesRounded);

        /* read byte counts */
        long byteCount[] = null;
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet   rs   = null;
        try {

            /* select clause */
            DBSelect<SessionStats> dsel = new DBSelect<SessionStats>(SessionStats.getFactory());
            dsel.setSelectedFields(new String[] {
                SUM_bytesRead,
                SUM_bytesWritten,
                SUM_bytesOverhead,
                SUM_bytesRounded
            });
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(SessionStats.FLD_accountID, acctID),
                    dwh.EQ(SessionStats.FLD_deviceID , devID),
                    dwh.GE(SessionStats.FLD_timestamp, timeStart),
                    dwh.LE(SessionStats.FLD_timestamp, timeEnd)
                )
            ));

            /* get record (will be only one) */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            if (rs.next()) {
                long sumBytesRead     = rs.getLong(SUM_bytesRead);
                long sumBytesWritten  = rs.getLong(SUM_bytesWritten);
                long sumBytesOverhead = rs.getLong(SUM_bytesOverhead);
                long sumBytesRounded  = rs.getLong(SUM_bytesRounded);
                byteCount = new long[] { sumBytesRead, sumBytesWritten, sumBytesOverhead, sumBytesRounded };
            }
            
        } catch (SQLException sqe) {
            throw new DBException("Get Byte Count", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }
        
        /* return results */
        return byteCount; // may be null

    }
    
    // ------------------------------------------------------------------------
    
    /* delete range of records */
    public static void deleteRange(Device device, long timeStart, long timeEnd)
        throws DBException
    {

        /* device/time valid */
        if (device == null) {
            return;
        }
        String acctID = device.getAccountID();
        String devID  = device.getDeviceID();
        
        /* delete range */
        SessionStats.deleteRange(acctID, devID, timeStart, timeEnd);

    }
    
    /* delete range of records */
    public static void deleteRange(String acctID, String devID, long timeStart, long timeEnd)
        throws DBException
    {

        /* device specified? */
        if ((acctID == null) || (devID == null)) {
            return;
        }
        
        /* date range */
        if (timeStart <= 0L) { timeStart = 1L; }
        if (timeEnd   <= 0L) { timeEnd   = Long.MAX_VALUE; }
        if (timeEnd < timeStart) {
            return;
        }

        /* SQL statement */
        // DBDelete: DELETE FROM SessionStats WHERE ((accountID='acct) AND (deviceID='dev') AND (timestamp>=timeStart) AND (timestamp<=timeEnd))
        DBDelete ddel = new DBDelete(SessionStats.getFactory());
        DBWhere dwh = ddel.createDBWhere();
        ddel.setWhere(dwh.WHERE_(
            dwh.AND(
                dwh.EQ(SessionStats.FLD_accountID, acctID),
                dwh.EQ(SessionStats.FLD_deviceID , devID),
                dwh.GE(SessionStats.FLD_timestamp, timeStart),
                dwh.LE(SessionStats.FLD_timestamp, timeEnd)
            )
        ));

        /* delete */
        DBConnection dbc = null;
        try {
            dbc = DBConnection.getDefaultConnection();
            dbc.executeUpdate(ddel.toString());
        } catch (SQLException sqe) {
            throw new DBException("Deleting SessionStats range", sqe);
        } finally {
            DBConnection.release(dbc);
        }
        
    }

    // ------------------------------------------------------------------------

    public String toString()
    {
        return this.getAccountID() + "/" + this.getDeviceID() + "/" + this.getTimestamp();
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

    /**
    *** Callback when record is about to be inserted into the table
    **/
    protected void recordWillInsert()
    {
        // override to optimize (DBRecordListnener not allowed)
    }

    /**
    *** Callback after record has been be inserted into the table
    **/
    protected void recordDidInsert()
    {
        // override to optimize (DBRecordListnener not allowed)
    }

    /**
    *** Callback when record is about to be updated in the table
    **/
    protected void recordWillUpdate()
    {
        // override to optimize (DBRecordListnener not allowed)
    }

    /**
    *** Callback after record has been be updated in the table
    **/
    protected void recordDidUpdate()
    {
        // override to optimize (DBRecordListnener not allowed)
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]   = new String[] { "account" , "acct" };
    private static final String ARG_COUNT[]     = new String[] { "count"            };
    private static final String ARG_TRIM[]      = new String[] { "trim"             };
    private static final String ARG_CALC[]      = new String[] { "calc"             };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + UserAcl.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>      Acount ID which owns SessionStats");
        Print.logInfo("  -count[=<deltaMo>] Return byte counts for current month");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main

        /* calc */
        if (RTConfig.hasProperty(ARG_CALC)) {
            // -calc=udp|tcp,eventsPerSession
            String calc[] = StringTools.parseArray(RTConfig.getString(ARG_CALC,""),',');
            if (calc.length < 3) {
                Print.logError("Missing 'calc' arguments: udp|tcp,eventsPerSession,inMotionInterval");
                System.exit(1);
            }
            String tcpUdp = calc[0];
            long eventsPerSession = StringTools.parseLong(calc[1],1L);
            long inMotionInterval = StringTools.parseLong(calc[2],DateTime.MinuteSeconds(3));

            /* assumed constants, for now */
            long pdpRounding        = 1024L; // service provider dependent
            long dmtpOverhead       = 26L;
            long dmtpEventSize      = 60L;
            
            /* motion/dormant intervals */
            long inMotionDuration   = DateTime.HourSeconds(12);   // per day
            long dormantInterval    = DateTime.MinuteSeconds(60);
            long dormantCount       = 2L;
            long stopsPerDay        = 6L;   // ignition[on|off]/start/stop
            long extraEventsPerDay  = 0L;   // faults/etc.
            long daysPerMonth       = 23L;
            
            /* calculate events-per-day */
            long eventsPerDay       = 0L;
            eventsPerDay += inMotionDuration / inMotionInterval;
            if (dormantCount >= 0L) {
                eventsPerDay += dormantCount;
            } else {
                long dormantDuration = DateTime.DaySeconds(1) - inMotionDuration;
                eventsPerDay += dormantDuration / dormantInterval;
            }
            eventsPerDay += stopsPerDay * 4L; // ignition[on|off]/start/stop
            eventsPerDay += extraEventsPerDay;

            Print.sysPrintln("InMotion interval : " + inMotionInterval);
            Print.sysPrintln("InMotion duration : " + inMotionDuration);
            Print.sysPrintln("Dormant interval  : " + dormantInterval);
            Print.sysPrintln("Dormant count     : " + dormantCount);
            Print.sysPrintln("Stops per day     : " + stopsPerDay);
            Print.sysPrintln("Events per day    : " + eventsPerDay);
            
            if (calc[0].equalsIgnoreCase("udp")) {
                long udpEventsPerSession = eventsPerSession;
                long udpSessionsPerDay   = eventsPerDay / udpEventsPerSession;
                long udpSessionOverhead  = UDP_OVERHEAD;
                long udpSessionSize      = udpSessionOverhead + dmtpOverhead + (udpEventsPerSession * dmtpEventSize);
                long udpRoundedSize      = ((udpSessionSize + (pdpRounding - 1L)) / pdpRounding) * pdpRounding;
                Print.sysPrintln("Sessions per day  : " + udpSessionsPerDay);
                Print.sysPrintln("Events per session: " + udpEventsPerSession);
                Print.sysPrintln("UDP bytes per day : " + (udpRoundedSize * udpSessionsPerDay));
                Print.sysPrintln("UDP bytes per mon : " + ((udpRoundedSize * udpSessionsPerDay) * daysPerMonth));
            } else
            if (calc[0].equalsIgnoreCase("tcp")) {
                long tcpEventsPerSession = eventsPerSession;
                long tcpSessionsPerDay   = eventsPerDay / tcpEventsPerSession;
                long tcpSessionOverhead  = (TCP_OVERHEAD * 2L) + TCP_SESSION_OVERHEAD;
                long tcpSessionSize      = tcpSessionOverhead + dmtpOverhead + (tcpEventsPerSession * dmtpEventSize);
                long tcpRoundedSize      = ((tcpSessionSize + (pdpRounding - 1L)) / pdpRounding) * pdpRounding;
                Print.sysPrintln("Sessions per day  : " + tcpSessionsPerDay);
                Print.sysPrintln("Events per session: " + tcpEventsPerSession);
                Print.sysPrintln("TCP bytes per day : " + (tcpRoundedSize * tcpSessionsPerDay));
                Print.sysPrintln("TCP bytes per mon : " + ((tcpRoundedSize * tcpSessionsPerDay) * daysPerMonth));
            } else {
                Print.logError("Invalid 'calc' arguments: " + RTConfig.getString(ARG_CALC,""));
            }
            
            System.exit(0);
        }

        /* account-id specified? */
        String acctID = RTConfig.getString(ARG_ACCOUNT, "");
        if ((acctID == null) || acctID.equals("")) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account account = null;
        try {
            account = Account.getAccount(acctID); // may return DBException
            if (account == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            //dbe.printException();
            System.exit(99);
        }
        
        /* devices owned by account */
        OrderedSet<String> devList = null;
        try {
            devList = Device.getDeviceIDsForAccount(acctID, null, true);
        } catch (DBException dbe) {
            Print.logException("Unable to obtain Device list for Account", dbe);
            System.exit(99);
        }

        /* option count */
        int opts = 0;

        /* count */
        if (RTConfig.getBoolean(ARG_COUNT, false)) {
            // display counts for the current month
            opts++;
            DateTime nowDate = account.getCurrentDateTime();
            int deltaMo = RTConfig.getInt(ARG_TRIM,0); // default to current month
            if (deltaMo > 0) { deltaMo = 0; } // start at least with current month
            DateTime fromDate = new DateTime(nowDate.getMonthStart(deltaMo));
            for (int d = 0; d < devList.size(); d++) {
                String devID = devList.get(0);
                try {
                    long bc[] = bc = SessionStats.getByteCounts(acctID,devID,fromDate.getTimeSec(),-1L);
                    if (bc != null) {
                        String ds = StringTools.leftAlign(devID,16);
                        Print.logInfo("Device: " + ds + " => R=" + bc[0] + ", W=" + bc[1] + ", O=" + bc[2] + ", R=" + bc[3]);
                    } else {
                        Print.logError("Error retrieving counts");
                        System.exit(2);
                    }
                } catch (DBException dbe) {
                    Print.logException("Error obtaining SessionStats byte count", dbe);
                    System.exit(1);
                }
            }
            System.exit(0);
        }

        /* trim */
        if (RTConfig.hasProperty(ARG_TRIM)) {
            opts++;
            DateTime nowDate = account.getCurrentDateTime();
            int deltaMo = RTConfig.getInt(ARG_TRIM,-2); // default save 2 months, plus current month
            if (deltaMo > 0) { deltaMo = 0; } // save at least up to current month
            DateTime upToDate = new DateTime(nowDate.getMonthEnd(deltaMo - 1)); // up to end of previous month
            Print.logInfo("Deleting SessionStats records up to " + upToDate);
            for (int d = 0; d < devList.size(); d++) {
                String devID = devList.get(0);
                Print.logInfo("  => Trimming '" + devID + "' ...");
                try {
                    SessionStats.deleteRange(acctID, devID, -1L, upToDate.getTimeSec());
                } catch (DBException dbe) {
                    Print.logException("Unable to delete SessionStats records", dbe);
                    System.exit(1);
                }
            }
            System.exit(0);
        }
        
        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }

    // ------------------------------------------------------------------------

}
