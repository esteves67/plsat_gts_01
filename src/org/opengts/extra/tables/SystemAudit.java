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

public class SystemAudit
    extends AccountRecord<SystemAudit>
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static Audit.AuditHandler auditHandler = null;

    private static void initAuditHandler()
    {
        if (SystemAudit.auditHandler == null) {
            Print.logDebug("Initializing AuditHandler ...");
            SystemAudit.auditHandler = new Audit.AuditHandler() {
                public void addAuditEntry(
                    String accountID, long auditTime, int auditCode,
                    String userID, String deviceID, 
                    String ipAddress,
                    String privateLabelName) {
                    SystemAudit.AddAudit(
                        accountID, auditTime, auditCode, 
                        userID, deviceID, 
                        ipAddress, 
                        privateLabelName);
                }
            };
            Audit.SetAuditHandler(SystemAudit.auditHandler);
        }
    }

    public static void AddAudit(
        String accountID, long auditTime, int auditCode,
        String userID, String deviceID, 
        String ipAddress,
        String privateLabelName)
    {
        long timeSec = (auditTime > 0L)? auditTime : DateTime.getCurrentTimeSec();
        SystemAudit.Key key = new SystemAudit.Key(accountID, timeSec, auditCode);
        SystemAudit audit = key.getDBRecord();
        audit.setCreationDefaultValues();
        audit.setUserID(userID);
        audit.setDeviceID(deviceID);
        audit.setIpAddress(ipAddress);
        audit.setPrivateLabelName(privateLabelName);
        try {
            audit.save();
        } catch (DBException dbe) {
            Print.logError("Error saving SystemAudit record: " + dbe);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "SystemAudit";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_auditTime            = "auditTime";
    public static final String FLD_auditCode            = "auditCode";
    public static final String FLD_userID               = User.FLD_userID;
    public static final String FLD_deviceID             = Device.FLD_deviceID;
    public static final String FLD_ipAddress            = "ipAddress";
    public static final String FLD_privateLabelName     = "privateLabelName";
    private static DBField FieldInfo[] = {
        // Audit fields
        newField_accountID(true), // key
        new DBField(FLD_auditTime       , Long.TYPE         , DBField.TYPE_INT32      , "Audit timestamp"       , "key=true"),
        new DBField(FLD_auditCode       , Integer.TYPE      , DBField.TYPE_UINT16     , "Audit Reason"          , "key=true"),
        new DBField(FLD_userID          , String.class      , DBField.TYPE_USER_ID()  , "User ID"               , ""),
        new DBField(FLD_deviceID        , String.class      , DBField.TYPE_DEV_ID()   , "Device ID"             , ""),
        new DBField(FLD_ipAddress       , DTIPAddress.class , DBField.TYPE_STRING(32) , "IP Address"            , ""),
        new DBField(FLD_privateLabelName, String.class      , DBField.TYPE_STRING(32) , "PrivateLabel Name"     , ""),
        // Common fields
        newField_description(),
        newField_notes(),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends AccountKey<SystemAudit>
    {
        public Key() {
            super();
        }
        public Key(String accountID, long auditTime, int auditCode) {
            super.setFieldValue(FLD_accountID, accountID);
            super.setFieldValue(FLD_auditTime, auditTime);
            super.setFieldValue(FLD_auditCode, auditCode);
        }
        public DBFactory<SystemAudit> getFactory() {
            return SystemAudit.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<SystemAudit> factory = null;
    public static DBFactory<SystemAudit> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                SystemAudit.TABLE_NAME(), 
                SystemAudit.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                SystemAudit.class, 
                SystemAudit.Key.class,
                false/*editable*/, true/*viewable*/);
            SystemAudit.initAuditHandler();

        }
        return factory;
    }

    /* Bean instance */
    public SystemAudit()
    {
        super();
    }

    /* database record */
    public SystemAudit(SystemAudit.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(SystemAudit.class, loc);
        return i18n.getString("SystemAudit.description", 
            "This table contains " +
            "GTS System Audit information"
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    // ------------------------------------------------------------------------

    public long getAuditTime()
    {
        Long v = (Long)this.getFieldValue(FLD_auditTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void setAuditTime(long v)
    {
        this.setFieldValue(FLD_auditTime, v);
    }

    // ------------------------------------------------------------------------

    public int getAuditCode()
    {
        Integer v = (Integer)this.getFieldValue(FLD_auditCode);
        return (v != null)? v.intValue() : 0;
    }

    public void setAuditCode(int v)
    {
        this.setFieldValue(FLD_auditCode, v);
    }
         
    // ------------------------------------------------------------------------

    /* User ID */
    public String getUserID()
    {
        String v = (String)this.getFieldValue(FLD_userID);
        return (v != null)? v : "";
    }
    
    private void setUserID(String v)
    {
        this.setFieldValue(FLD_userID, ((v != null)? v : ""));
    }
         
    // ------------------------------------------------------------------------

    /* Device ID */
    public final String getDeviceID()
    {
        String v = (String)this.getFieldValue(FLD_deviceID);
        return (v != null)? v : "";
    }
    
    public /*final*/ void setDeviceID(String v)
    {
        this.setFieldValue(FLD_deviceID, ((v != null)? v : ""));
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

    /* get the PrivateLabel name assigned to this account */
    public String getPrivateLabelName()
    {
        String v = (String)this.getFieldValue(FLD_privateLabelName);
        return StringTools.trim(v);
    }

    /* set the PrivateLabel name assigned to this account */
    public void setPrivateLabelName(String v)
    {
        this.setFieldValue(FLD_privateLabelName, StringTools.trim(v));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public String toString()
    {
        return this.getAccountID() + "/" + this.getAuditTime() + "/" + this.getAuditCode();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static void getRecordCallback(DBRecordHandler<SystemAudit> rcdHandler,
        String accountID, 
        int auditCode,
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
        DBSelect<SystemAudit> dsel = new DBSelect<SystemAudit>(SystemAudit.getFactory());
        dsel.setOrderByFields(FLD_auditTime);
        dsel.setOrderAscending(true);
        dsel.setLimit(limit);

        /* Where */
        DBWhere dwh = new DBWhere(SystemAudit.getFactory());
        dwh.append(dwh.AND(
            dwh.EQ(SystemAudit.FLD_accountID,StringTools.trim(accountID)),
            dwh.EQ(SystemAudit.FLD_auditCode,auditCode)
        ));
        if (startTime >= 0L) {
            // AND (auditTime>=123436789)
            dwh.append(dwh.AND_(dwh.GE(SystemAudit.FLD_auditTime,startTime)));
        }
        if ((endTime >= 0L) && (endTime >= startTime)) {
            // AND (auditTime<=123456789)
            dwh.append(dwh.AND_(dwh.LE(SystemAudit.FLD_auditTime,endTime)));
        }
        dsel.setWhere(dwh.toString());
        //Print.logInfo("Where: " + dwh);

        /* iterate through records */
        DBRecord.select(dsel, rcdHandler);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]   = new String[] { "account", "acct"  };
    private static final String ARG_DEVICE[]    = new String[] { "device" , "dev"   };
    private static final String ARG_LIST[]      = new String[] { "list"             };
    private static final String ARG_ADD[]       = new String[] { "add"              };
    private static final String ARG_DELETE[]    = new String[] { "delete"           };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + SystemAudit.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>           Acount ID which owns the specified Device");
        Print.logInfo("  -device=<id>            Device ID to apply pending commands");
        Print.logInfo("  -list                   List all pending commands for Device");
        Print.logInfo("  -delete=<time>          Delete pending commands for Device");
        Print.logInfo("  -add=<cmd>              Add pending command for Device");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String devID   = RTConfig.getString(ARG_DEVICE , "");

        /* account-id specified? */
        if (StringTools.isBlank(acctID)) {
            Print.sysPrintln("ERROR: Account-ID not specified.");
            usage();
        }

        /* get account */
        Account account = null;
        try {
            account = Account.getAccount(acctID); // may return DBException
            if (account == null) {
                Print.sysPrintln("ERROR: Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* device-id specified? */
        if (StringTools.isBlank(devID)) {
            Print.sysPrintln("ERROR: Device-ID not specified.");
            usage();
        }

        /* get device */
        Device device = null;
        try {
            device = Device.getDevice(account, devID, false);
            if (device == null) {
                Print.sysPrintln("ERROR: Device-ID does not exist: " + devID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Device: " + devID, dbe);
            //dbe.printException();
            System.exit(99);
        }
        
        /* option count */
        int opts = 0;

        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }
    
}
