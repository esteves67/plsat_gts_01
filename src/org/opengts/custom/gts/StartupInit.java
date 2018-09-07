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
// Startup Initialization:
//  org.opengts.dbtools.DBRecordListener
//  org.opengts.db.PingDispatcher
//  org.opengts.db.RuleFactory
//  org.opengts.db.EventUtil.OptionalEventFields
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.custom.gts;

import java.util.Locale;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.dbtypes.*;
import org.opengts.geocoder.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.db.dmtp.*;

/*
import org.opengts.rule.selector.FunctionMap;
import org.opengts.rule.EventRuleFactory;
import org.opengts.rule.event.EventFunctionMap;
import org.opengts.rule.event.EventFunctionHandler;
*/

/**
*** Provides startup initialization.<br>
*** This class is loaded by <code>DBConfig.java</code> at startup initialization time, and 
*** various methods are called within this class to allow custom DB initialization.<br>
*** The actual class loaded and executed by <code>DBConfig</code> can be overridden by placing 
*** the following line in the system 'default.conf' and 'webapp.conf' files:
*** <pre>
***   startup.initClass=org.opengts.custom.gts.StartupInit
*** </pre>
*** Where 'org.opengts.opt.StartupInit' is the name of the class you wish to have loaded in
*** place of this class file.
**/

public class StartupInit
    extends org.opengts.StartupInit
    implements DBConfig.DBInitialization, DBFactory.CustomFactoryHandler
{

    // ------------------------------------------------------------------------

    private static String  PROP_ENREInitialize_class        = "ENREInitialize.class";

    // ------------------------------------------------------------------------
    // Feature list:
    // Search for the 'constant' values below to find the location where these
    // features can be set.

    /* limit the list of displayed status codes */
    protected static boolean LIMIT_DISPLAYED_STATUS_CODES       = false;

    /* set global status code descriptions */
    protected static boolean SET_STATUS_CODE_DESCRIPTIONS       = false;

    /* add record insert/update call-back listener */
    protected static boolean ADD_RECORD_LISTENERS               = false;

    /* add additional EventData fields */
    protected static boolean ADD_ADDITIONAL_EVENTDATA_FIELDS    = false;

    /* add additional tables */
    protected static boolean ADD_ADDITIONAL_TABLES              = false;

    // ------------------------------------------------------------------------

    /**
    *** Constructor.<br>
    *** (Created with the DBConfig db startup initialization)
    **/
    public StartupInit()
    {
        super();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DBConfig.DBInitialization interface
 
    /**
    *** Pre-DBInitialization.<br>
    *** This method is called just before the standard database factory classes are initialized/added.
    **/
    public void preInitialization()
    {
        Print.logDebug("'StartupInit' Initialization for GTS Enterprise ...");
        super.preInitialization();
        //DTOBDFault.InitJ1587DescriptionProvider();
    }

    // ------------------------------------------------------------------------

    /**
    *** Opportunity to add custom DBFactory classes.<br>
    *** This method is called just after all standard database factory classes have been intialized/added.
    *** Additional database factories that are needed for the custom installation may be added here.
    **/
    public void addTableFactories()
    {
        super.addTableFactories(); // <-- add standard DBFactories
        
        /* add additional custom tables */
        if (ADD_ADDITIONAL_TABLES) {
            // The following will attempt to load the specified table class name
            // Note that this class must be a subclass of "DBRecord".
            //  String tableClassName = "org.opengts.custom.custompackage.Custom";
            //  DBAdmin.addTableFactory(tableClassName, true/*required*/);
        }

        /* add new Rule functions */
        // NOTE: Only valid for COMMERCIAL version of "Event Notification Rules Engine" 
        // (NOT valid for RuleFactoryLite!)
        // See "ENREFunctions.java" for the ENRE functions and callbacks that it initializes.
        if (DBConfig.hasRulePackage()) {
            String dft_enreInitClassName = "org.opengts.custom.gts.rule.ENREFunctions";
            String enreInitClassName = RTConfig.getString(PROP_ENREInitialize_class,dft_enreInitClassName);
            try {
                Class enreInitClass = Class.forName(enreInitClassName);
                try {
                    enreInitClass.newInstance(); // initialization occurs in constructor
                    Print.logInfo("Loaded custom Rule functions/identifiers");
                } catch (Throwable th) {
                    Print.logError("Error loading custom Rule functions/identifiers: " + th);
                }
            } catch (ClassNotFoundException cnfe) {
                // ignore - not supported
            } catch (NoClassDefFoundError ncfe) {
                // ignore - not supported
            }
        }

        /* ping dispatcher */
        // No longer necessary if a DCServerConfig is defined
        //Device.setPingDispatcher(new CommandPingDispatcher());

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** Called by "super.addTableFactories()"<br>
    *** Creates a generic custom EventUtil.OptionalEventFields instance
    *** @return An EventUtil.OptionalEventFields instance
    **/
    protected EventUtil.OptionalEventFields createOptionalEventFieldsHandler()
    {
        return super.createOptionalEventFieldsHandler();
        // override to return a custom OptionalEventFields instance
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Post-DBInitialization.<br>
    *** This method is called after all startup initialization has completed.
    **/
    public void postInitialization()
    {
        //super.postInitialization();  <== override everything below

        /* init StatusCode descriptions */
        if (LIMIT_DISPLAYED_STATUS_CODES) {
            // Limiting Displayed Status Codes
            // - The following specifies the list of specific status codes to include:
            //  StatusCodes.initStatusCodes(new int[] {
            //     StatusCodes.STATUS_LOCATION,
            //     StatusCodes.STATUS_MOTION_START,
            //     StatusCodes.STATUS_MOTION_IN_MOTION,
            //     StatusCodes.STATUS_MOTION_STOP,
            //     StatusCodes.STATUS_MOTION_DORMANT,
            //     // ... include other StatusCodes here ...
            //  });
        } else {
            // include all status codes
            StatusCodes.initStatusCodes(null);
        }

        /* This sets the description for all accounts, all 'private.xml' domains, and all Localizations. */
        if (SET_STATUS_CODE_DESCRIPTIONS) {
            //StatusCodes.SetDescription(StatusCodes.STATUS_LOCATION      , "Marker");
            //StatusCodes.SetDescription(StatusCodes.STATUS_MOTION_START  , "Start Point");
            //StatusCodes.SetDescription(StatusCodes.STATUS_MOTION_STOP   , "Stop Point");
        }

        /* add DBRecordListeners */
        if (ADD_RECORD_LISTENERS) {
            /* Account table record listener: * /
            DBFactory<Account> accountFact = Account.getFactory();
            accountFact.setRecordListener(new DBRecordListener<Account>() {
                public void recordWillInsert(Account rcd) {
                    // invoked just prior to Account record insertion
                }
                public void recordDidInsert(Account rcd) {
                    // invoked just after Account record insertion
                }
                public void recordWillUpdate(Account rcd) {
                    // invoked just prior to Account record update
                }
                public void recordDidUpdate(Account rcd) {
                    // invoked just after Account record update
                }
            });
            */
            // other record call-back listeners can be added below
        }

        /* Install custom PasswordHandler */
        this.initPasswordHandler();

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DBFactory.CustomFactoryHandler interface

    /**
    *** Create a DBFactory instance.  The DBFactory initialization process will call this method
    *** when creating a DBFactory for a given table, allowing this class to override/customize
    *** any specific table attributes.  If this method returns null, the default table DBFactory
    *** will be created.
    *** @param tableName  The name of the table
    *** @param field      The DBFields in the table
    *** @param keyType    The table key type
    *** @param rcdClass   The DBRecord subclass representing the table
    *** @param keyClass   The DBRecordKey subclass representing the table key
    *** @param editable   True if this table should be editable, false otherwise.  
    ***                   This value is used by the GTSAdmin application.
    *** @param viewable   True if this table should be viewable, false otherwise.  
    ***                   An 'editable' table is automatically considered viewable.
    ***                   This value is used by the GTSAdmin application.
    *** @return The DBFactory instance (or null to indicate that the default DBFactory should be created).
    ***/
    public <T extends DBRecord<T>> DBFactory<T> createDBFactory(
        String tableName, 
        DBField field[], 
        DBFactory.KeyType keyType, 
        Class<T> rcdClass, 
        Class<? extends DBRecordKey<T>> keyClass, 
        boolean editable, boolean viewable)
    {
        //Print.logInfo("Intercept creation of DBFactory: %s", tableName);
        return super.createDBFactory(tableName, field, keyType, rcdClass, keyClass, editable, viewable);
    }

    /**
    *** Augment DBFactory fields.  This method is called before fields have been added to any
    *** given DBFactory.  This method may alter the list of DBFields by adding new fields, or 
    *** altering/deleting existing fields.  However, deleting/altering fields that have other
    *** significant systems dependencies may cause unpredictable behavior.
    *** @param factory  The DBFactory
    *** @param fields   The list of fields scheduled to be added to the DBFactory
    *** @return The list of fields which will be added to the DBFactory
    **/
    public java.util.List<DBField> selectFields(DBFactory factory, java.util.List<DBField> fields)
    {
        String tblName = factory.getUntranslatedTableName();
        // These additional fields can be enabled by placing the appropriate/specified 
        // property key=value in a 'custom.conf' file.

        /* Account */
        if (tblName.equalsIgnoreCase(Account.TABLE_NAME())) {
            // startupInit.Account.AddressFieldInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_AddressFieldInfo               , false, Account.AddressFieldInfo);
            // startupInit.Account.MapLegendFieldInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_MapLegendFieldInfo             , true , Account.MapLegendFieldInfo);
            // startupInit.Account.AccountManagerInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_AccountManagerInfo             , true , Account.AccountManagerInfo);
            // startupInit.Account.DataPushInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_DataPushInfo                   , true , Account.DataPushInfo);
            return fields;
        }

        /* User */
        if (tblName.equalsIgnoreCase(User.TABLE_NAME())) {
            // startupInit.User.AddressFieldInfo=true
            addDBFields(tblName, fields, User.OPTCOLS_AddressFieldInfo                  , false, User.AddressFieldInfo);
            return fields;
        }

        /* Device */
        if (tblName.equalsIgnoreCase(Device.TABLE_NAME())) {
            boolean ruPkg = DBConfig.hasRulePackage();
            boolean bcPkg = Account.SupportsBorderCrossing();
            // startupInit.Device.NotificationFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_NotificationFieldInfo           , true , Device.NotificationFieldInfo);    // true
            // startupInit.Device.GeoCorridorFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_GeoCorridorFieldInfo            , ruPkg, Device.GeoCorridorFieldInfo);
            // startupInit.Device.FixedLocationFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_FixedLocationFieldInfo          , false, Device.FixedLocationFieldInfo);
            // startupInit.Device.LinkFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_LinkFieldInfo                   , false, Device.LinkFieldInfo);            // true
            // startupInit.Device.BorderCrossingFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_BorderCrossingFieldInfo         , bcPkg, Device.BorderCrossingFieldInfo);  // true
            // startupInit.Device.MaintOdometerFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_MaintOdometerFieldInfo          , ruPkg, Device.MaintOdometerFieldInfo);   // true
            // startupInit.Device.WorkOrderInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_WorkOrderInfo                   , false, Device.WorkOrderInfo);
            // startupInit.Device.DataPushInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_DataPushInfo                    , true , Device.DataPushInfo);
            return fields;
        }

        /* DeviceGroup */
        if (tblName.equalsIgnoreCase(DeviceGroup.TABLE_NAME())) {
            // startupInit.DeviceGroup.WorkOrderInfo=true
            addDBFields(tblName, fields, DeviceGroup.OPTCOLS_WorkOrderInfo              , false, DeviceGroup.WorkOrderInfo);
            return fields;
        }

        /* EventData */
        if (tblName.equalsIgnoreCase(EventData.TABLE_NAME())) {
            // startupInit.EventData.AutoIncrementIndex=true
            addDBFields(tblName, fields, EventData.OPTCOLS_AutoIncrementIndex           , false, EventData.AutoIncrementIndex);
            // startupInit.EventData.CreationTimeMillisecond=true
            addDBFields(tblName, fields, EventData.OPTCOLS_CreationTimeMillisecond      , false, EventData.CreationTimeMillisecond);
            // startupInit.EventData.AddressFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_AddressFieldInfo             , true , EventData.AddressFieldInfo);      // true
            // startupInit.EventData.GPSFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_GPSFieldInfo                 , true , EventData.GPSFieldInfo);          // true
            // startupInit.EventData.CustomFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_CustomFieldInfo              , false, EventData.CustomFieldInfo);
            // startupInit.EventData.GarminFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_GarminFieldInfo              , false, EventData.GarminFieldInfo);
            // startupInit.EventData.CANBUSFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_CANBUSFieldInfo              , false, EventData.CANBUSFieldInfo);
            // startupInit.EventData.AtmosphereFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_AtmosphereFieldInfo          , false, EventData.AtmosphereFieldInfo);
            // startupInit.EventData.ThermoFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_ThermoFieldInfo              , false, EventData.ThermoFieldInfo, 4);
            // startupInit.EventData.AnalogFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_AnalogFieldInfo              , false, EventData.AnalogFieldInfo);
            // startupInit.EventData.EndOfDaySummary=true
            addDBFields(tblName, fields, EventData.OPTCOLS_EndOfDaySummary              , false, EventData.EndOfDaySummary);
            // startupInit.EventData.ServingCellTowerData=true
            addDBFields(tblName, fields, EventData.OPTCOLS_ServingCellTowerData         , false, EventData.ServingCellTowerData);
            // startupInit.EventData.NeighborCellTowerData=true
            addDBFields(tblName, fields, EventData.OPTCOLS_NeighborCellTowerData        , false, EventData.NeighborCellTowerData);
            // startupInit.EventData.WorkZoneGridData=true
            addDBFields(tblName, fields, EventData.OPTCOLS_WorkZoneGridData             , false, EventData.WorkZoneGridData);
            if (ADD_ADDITIONAL_EVENTDATA_FIELDS) {
                Print.logInfo("Adding custom fields to EventData ...");
                addDBFields(tblName, fields, null, true, new DBField[] {
                    new DBField("customDoubleValue", Double.TYPE , DBField.TYPE_DOUBLE    , "Double value"   , "edit=2"),
                    new DBField("customBoolValue"  , Boolean.TYPE, DBField.TYPE_BOOLEAN   , "Boolean value"  , "edit=2"),
                    new DBField("custom32BitValue" , Long.TYPE   , DBField.TYPE_UINT32    , "32Bit Value"    , "edit=2"),
                    new DBField("customStringValue", String.class, DBField.TYPE_STRING(32), "String Value"   , "edit=2"),
                });
            }
            return fields;
        }

        /* Geozone */
        if (tblName.equalsIgnoreCase(Geozone.TABLE_NAME())) {
            // startupInit.Geozone.PriorityFieldInfo=true
            addDBFields(tblName, fields, "startupInit.Geozone.PriorityFieldInfo"        , true , Geozone.PriorityFieldInfo);
            // startupInit.Geozone.CorridorFieldInfo=true
            addDBFields(tblName, fields, "startupInit.Geozone.CorridorFieldInfo"        , true , Geozone.CorridorFieldInfo);
            return fields;
        }

        /* leave as-is */
        return super.selectFields(factory, fields);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
