// ----------------------------------------------------------------------------
// Copyright 2007-2014, OpenGTS
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// FullMap Device:
//  - Device selection
//  - From/To Calendar [timezone]
//  - Device Map
//  - Auto Update
//  - Replay
//  - Ruler
//  - Send Device Commands
//  - Pushpin legend (ideally auto generated)
//  - Device attributes (batteryLevel, etc)
// FullMap Fleet
//  - Group selection
//  - [From]/To Calendar [timezone]
//  - Fleet Map
//  - Auto Update
//  - Ruler
//  - Pushpin legend (ideally auto generated)
// ----------------------------------------------------------------------------
package org.opengts.extra.war.track.page;

/* explicit imports required (due to conflict with "Calendar") */
import java.util.Locale;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map;
import java.util.Collection;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.geocoder.GeocodeProvider;

import org.opengts.war.tools.*;
import org.opengts.war.track.Calendar;
import org.opengts.war.track.*;
import org.opengts.war.maps.JSMap;
import org.opengts.war.report.ReportPresentation;

public abstract class FullMap
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------

    private static final String  ID_DEVICE_ID                   = "deviceSelector";
    private static final String  ID_DEVICE_DESCR                = "deviceDescription";

    // ------------------------------------------------------------------------

    public  static final String  _ACL_AUTO                      = "auto";
    private static final String  _ACL_LIST[]                    = new String[] { _ACL_AUTO };

    // ------------------------------------------------------------------------
    // Properties

    public static final String   PROP_statusCodes               = "statusCodes";
    public static final String   PROP_showFleetFromCalendar     = "showFleetFromCalendar";
    public static final String   PROP_fleetDeviceEventCount     = "fleetDeviceEventCount";
    public static final String   PROP_mapTypeTitle              = "mapTypeTitle";

    public static final String   PROP_autoUpdate_enable         = "autoUpdate.enable";
    public static final String   PROP_autoUpdate_onload         = "autoUpdate.onload";
    public static final String   PROP_autoUpdate_interval       = "autoUpdate.interval";
    public static final String   PROP_autoUpdate_count          = "autoUpdate.count";

    // ------------------------------------------------------------------------
    // forms

    public  static final String  FORM_SELECT_DEVICE             = "SelectDeviceForm";
    public  static final String  FORM_GOTO_ADDRESS              = "GotoAddress";
    public  static final String  FORM_PING_DEVICE               = "PingDeviceForm";
    public  static final String  FORM_SELECT_TIMEZONE           = "TimeZoneSelect";

    // ------------------------------------------------------------------------
    // Commands

    public  static final String  COMMAND_DEVICE_LIST            = "devlist";                // arg=<N/A>
    public  static final String  COMMAND_GROUP_LIST             = "grplist";                // arg=<N/A>
    public  static final String  COMMAND_DEVICE_PING            = "devping";                // arg=<N/A>
    public  static final String  COMMAND_MAP_UPDATE             = "mapupd";                 // arg=<N/A>
    public  static final String  COMMAND_KML_UPDATE             = "kmlupd";                 // arg=<N/A>
    public  static final String  COMMAND_AUTO_UPDATE            = "auto";                   // arg=interval,maxcount

    // ------------------------------------------------------------------------
    // Calendar vars

    public  static final String  CALENDAR_FROM                  = "mapCal_fr";
    public  static final String  CALENDAR_TO                    = "mapCal_to";
    
    // ------------------------------------------------------------------------
    // Auto update map timer

    private static final boolean DFT_AUTO_ENABLED               = false;
    private static final long    DFT_AUTO_DURATION              = DateTime.MinuteSeconds(20);
    private static final long    DFT_AUTO_INTERVAL              = DateTime.MinuteSeconds(1);
    private static final long    DFT_AUTO_MAXCOUNT              = DFT_AUTO_DURATION / DFT_AUTO_INTERVAL;

    private static final String  ID_MAP_AUTOUPDATE_BTN          = "mapAutoUpdateButton";
    private static final String  ID_MAP_UPDATE_BTN              = "mapUpdateButton";
    private static final String  ID_MAP_LAST_BTN                = "mapLastButton";
    private static final String  ID_MAP_REPLAY_BTN              = "mapReplayButton";
    private static final String  ID_MAP_SHOW_INFO               = "mapShowInfoBox";
    private static final String  ID_PING_DEVICE_BTN             = "pingDeviceButton";
    private static final String  ID_GOTO_ADDR_BTN               = "gotoAddressButton";
    private static final String  ID_MAP_CONTROL                 = "mapControlCell";
    private static final String  ID_MAP_CONTROL_BAR             = "mapControlBar";

    // ------------------------------------------------------------------------
    // property values

    // PrivateLabel.PROP_FullMap_mapUpdateOnLoad
    private static final String MAP_UPDATE_ALL[]     = new String[] { "all"  , "true"  };
    private static final String MAP_UPDATE_LAST[]    = new String[] { "last" , "false" };

    // PrivateLabel.PROP_FullMap_autoUpdateRecenter
    private static final String AUTO_RECENTER_NONE[] = new String[] { "no"  , "0", "false", "none" };
    private static final String AUTO_RECENTER_LAST[] = new String[] { "last", "1"                  };
    private static final String AUTO_RECENTER_ZOOM[] = new String[] { "zoom", "2", "true" , "yes"  };
    private static final String AUTO_RECENTER_PAN[]  = new String[] { "pan" , "3"                  };

    // PrivateLabel.PROP_FullMap_showLocateNow
    private static final String SHOW_PING_FALSE[]    = new String[] { "false" , "no"  };
    private static final String SHOW_PING_TRUE[]     = new String[] { "true"  , "yes" };
    private static final String SHOW_PING_DEVICE[]   = new String[] { "device"        };

    // PrivateLabel.PROP_FullMap_calendarDateOnLoad
    private static final String CALENDAR_DATE_NOW[]  = new String[] { "current", "now"    };
    private static final String CALENDAR_DATE_LAST[] = new String[] { "last"   , "device" };

    // PrivateLabel.PROP_FullMap_mapControlLocation
    private static final String CONTROLS_ON_LEFT[]   = new String[] { "left", "true" };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // WebPage interface

    private boolean         isFleet = false;
    private int             statusCodes[] = null;
    private boolean         showFromCalendar = false;

    public FullMap()
    {
        super();
    }
    
    protected void postInit()
    {
        super.postInit();

        /* status codes */
        this.statusCodes = null;
        String statusCodesCSV = this.getStringProperty(null,PROP_statusCodes,null);
        if (!StringTools.isBlank(statusCodesCSV)) {
            String val[] = StringTools.parseArray(statusCodesCSV);
            this.statusCodes = new int[val.length];
            for (int i = 0; i < val.length; i++) {
                this.statusCodes[i] = StringTools.parseInt(val[i], StatusCodes.STATUS_NONE);
                //Print.logInfo("Map StatusCode: 0x" + StringTools.toHexString(this.statusCodes[i],16));
            }
        }
        
        /* Fleet: showFleetFromCalendar */
        if (this.isFleet()) {
            // fleet map
            String frCal = this.getStringProperty(null,PROP_showFleetFromCalendar,"");
            this.showFromCalendar = (StringTools.isBlank(frCal) || frCal.equalsIgnoreCase("default"))?
                false : StringTools.parseBoolean(frCal,false);
        } else {
            // device map
            this.showFromCalendar = true;
        }

    }

    // ------------------------------------------------------------------------
    
    public String[] getChildAclList()
    {
        return _ACL_LIST;
    }

    // ------------------------------------------------------------------------

    protected void setFleet(boolean fleet)
    {
        this.isFleet = fleet;
        this.showFromCalendar = !this.isFleet;
    }
    
    public boolean isFleet()
    {
        return this.isFleet;
    }

    // ------------------------------------------------------------------------

    protected int[] getStatusCodes()
    {
        return this.statusCodes;
    }

    // ------------------------------------------------------------------------

    protected void writeDeviceList(final RequestProperties reqState)
        throws IOException
    {
        HttpServletResponse response = reqState.getHttpServletResponse();
        PrintWriter out = response.getWriter();

        /* mime content type */
        String mimeType = HTMLTools.MIME_JSON();
        CommonServlet.setResponseContentType(response, mimeType, StringTools.CharEncoding_UTF_8);
        response.setHeader("CACHE-CONTROL", "NO-CACHE");
        response.setHeader("PRAGMA"       , "NO-CACHE");
        response.setDateHeader("EXPIRES"  , 0         );

        /* list of devices for account */
        Account account = reqState.getCurrentAccount();
        User    user    = reqState.getCurrentUser(); // may be null
        OrderedSet<String> list = reqState.getDeviceIDList(false/*inclInactv*/);

        /* create json object */
        // {
        //   "DeviceList" : [
        //      {
        //          "id"  : "deviceid",
        //          "desc": "description"
        //      },
        //      {
        //          "id"  : "deviceid",
        //          "desc": "description"
        //      }
        //    ]
        // }
        JSON._Array listArray = new JSON._Array();
        if (list != null) {
            for (String devID : list) {
                try {
                    Device device = Device.getDevice(account, devID);
                    if (device != null) {
                        JSON._Object devObj = new JSON._Object();
                        devObj.addKeyValue("id"  , device.getDeviceID());
                        devObj.addKeyValue("desc", device.getDescription());
                        listArray.addValue(devObj);
                    }
                } catch (DBException dbe) {
                    // skip
                }
            }
        }
        JSON._Object deviceList = new JSON._Object();
        deviceList.addKeyValue("DeviceList",listArray);

        /* write JSON object */
        String jsonStr = deviceList.toString(false);
        out.write(jsonStr);
        out.flush();

    }

    protected void writeDeviceGroupList(final RequestProperties reqState)
        throws IOException
    {
        // OrderedSet<String> list = reqState.getDeviceGroupIDList(true/*inclAll*/);
        HttpServletResponse response = reqState.getHttpServletResponse();
        PrintWriter out = response.getWriter();

        /* mime content type */
        String mimeType = HTMLTools.MIME_JSON();
        CommonServlet.setResponseContentType(response, mimeType, StringTools.CharEncoding_UTF_8);
        response.setHeader("CACHE-CONTROL", "NO-CACHE");
        response.setHeader("PRAGMA"       , "NO-CACHE");
        response.setDateHeader("EXPIRES"  , 0         );

        /* list of devices for account */
        Account account = reqState.getCurrentAccount();
        User    user    = reqState.getCurrentUser(); // may be null
        OrderedSet<String> list = reqState.getDeviceGroupIDList(true/*inclAll*/);

        /* create json object */
        // {
        //   "GroupList" : [
        //      {
        //          "id"  : "groupid",
        //          "desc": "description"
        //      },
        //      {
        //          "id"  : "groupid",
        //          "desc": "description"
        //      }
        //    ]
        // }
        JSON._Array listArray = new JSON._Array();
        if (list != null) {
            for (String grpID : list) {
                try {
                    DeviceGroup group = DeviceGroup.getDeviceGroup(account, grpID);
                    if (group != null) {
                        JSON._Object grpObj = new JSON._Object();
                        grpObj.addKeyValue("id"  , group.getGroupID());
                        grpObj.addKeyValue("desc", group.getDescription());
                        listArray.addValue(grpObj);
                    }
                } catch (DBException dbe) {
                    // skip
                }
            }
        }
        JSON._Object groupList = new JSON._Object();
        groupList.addKeyValue("GroupList",listArray);

        /* write JSON object */
        String jsonStr = groupList.toString(false);
        out.write(jsonStr);
        out.flush();

    }

    // ------------------------------------------------------------------------
    // GPS/Map JavaScript

    protected void writeJS_MapUpdate(
        final RequestProperties reqState, 
        PrintWriter out,
        String  mapUpdURL, String devicePingURL, String kmlUpdURL,
        boolean autoUpdateEnabled, boolean autoUpdateOnLoad, long autoInterval, long autoMaxCount,
        int showBatteryLevel, int devicePushpinNdx
        )
        throws IOException
    {
        // external JavaScript functions:
        //   - mapDevicePing(pingURL);
        //   - mapProviderParseXML(mapEventRecords)
        //   - mapProviderUpdateMap(mapDataURL,recenterMode,replay)
        //   - mapProviderUnload()
        //   - mapProviderToggleDetails()
        final boolean       isFleet    = this.isFleet();
        String              parmDevGrp = isFleet? PARM_GROUP : PARM_DEVICE;
        PrivateLabel        privLabel  = reqState.getPrivateLabel();
        I18N                i18n       = privLabel.getI18N(FullMap.class);
        HttpServletRequest  request    = reqState.getHttpServletRequest();

        /* start JavaScript */
        JavaScriptTools.writeStartJavaScript(out);

        /* Calendar OnLoad */
        String  calDateOnLoad = this.getStringProperty(privLabel,PrivateLabel.PROP_FullMap_calendarDateOnLoad,CALENDAR_DATE_NOW[0]).toLowerCase();
        JavaScriptTools.writeJSVar(out, "CalendarDateOnLoad"        , calDateOnLoad);

        /* points to display OnLoad or when AutoUpdate is clicked */
        String mapUpdateOnLoad;
        if (isFleet) {
            // all devices if in fleet mode
            mapUpdateOnLoad = MAP_UPDATE_ALL[0];
        } else {
            // last/all if in device mode
            String muol = this.getStringProperty(privLabel,PrivateLabel.PROP_FullMap_mapUpdateOnLoad,"");
            mapUpdateOnLoad = ListTools.containsIgnoreCase(MAP_UPDATE_LAST,muol)? MAP_UPDATE_LAST[0] : MAP_UPDATE_ALL[0];
        }
        
        /* auto-update attributes */
        int autoUpdateRecenterMode = 0;
        if (autoUpdateEnabled) {
            String mode = this.getStringProperty(privLabel,PrivateLabel.PROP_FullMap_autoUpdateRecenter,AUTO_RECENTER_ZOOM[0]);
            if (ListTools.containsIgnoreCase(AUTO_RECENTER_NONE,mode)) {
                autoUpdateRecenterMode = 0; // none
            } else
            if (ListTools.containsIgnoreCase(AUTO_RECENTER_LAST,mode)) {
                autoUpdateRecenterMode = 1; // last
            } else
            if (ListTools.containsIgnoreCase(AUTO_RECENTER_PAN,mode)) {
                autoUpdateRecenterMode = 3; // pan
            } else {
                autoUpdateRecenterMode = 2; // zoom
            }
        }

        /* write map attributes */
        out.write("// FullMap Update/AutoUpdate/Replay attributes\n");
        JavaScriptTools.writeJSVar(out, "MapUpdateOnLoad"           , mapUpdateOnLoad);
        JavaScriptTools.writeJSVar(out, "AutoUpdateEnable"          , autoUpdateEnabled);
        JavaScriptTools.writeJSVar(out, "AutoUpdateOnLoad"          , autoUpdateOnLoad);
        JavaScriptTools.writeJSVar(out, "AutoMaxCount"              , autoMaxCount);
        JavaScriptTools.writeJSVar(out, "AutoInterval"              , autoInterval);
        JavaScriptTools.writeJSVar(out, "AutoUpdateRecenterMode"    , autoUpdateRecenterMode);
        JavaScriptTools.writeJSVar(out, "AutoUpdateMapTimer"        , null);
        JavaScriptTools.writeJSVar(out, "AutoIntervalCount"         , 0);
        JavaScriptTools.writeJSVar(out, "AutoUpdateMapCount"        , 0);
        JavaScriptTools.writeJSVar(out, "LimitType"                 , this.getStringProperty(privLabel,PrivateLabel.PROP_FullMap_limitType,"last"));
        JavaScriptTools.writeJSVar(out, "ID_MAP_UPDATE_BTN"         , ID_MAP_UPDATE_BTN);
        JavaScriptTools.writeJSVar(out, "ID_MAP_AUTOUPDATE_BTN"     , ID_MAP_AUTOUPDATE_BTN);
        JavaScriptTools.writeJSVar(out, "ID_MAP_REPLAY_BTN"         , ID_MAP_REPLAY_BTN);

        /* Map Controls */
        JavaScriptTools.writeJSVar(out, "ID_MAP_CONTROL"            , null);
        JavaScriptTools.writeJSVar(out, "ID_MAP_CONTROL_BAR"        , null);
        JavaScriptTools.writeJSVar(out, "CLASS_CONTROL_BAR"         , null);

        /* Localized text */
        out.write("// FullMap localized text\n");
        JavaScriptTools.writeJSVar(out, "TEXT_autoUpdateStart"      , i18n.getString("FullMap.startAutoUpdate","Auto"));
        JavaScriptTools.writeJSVar(out, "TEXT_autoUpdateStop"       , i18n.getString("FullMap.stopAutoUpdate","Stop"));

        /* other vars */
        out.write("// FullMap misc vars\n");
        JavaScriptTools.writeJSVar(out, "IS_FLEET"                  , isFleet);
        JavaScriptTools.writeJSVar(out, "IS_DEVICE"                 , !isFleet);
        JavaScriptTools.writeJSVar(out, "MAP_UPDATE_URL"            , mapUpdURL);
        JavaScriptTools.writeJSVar(out, "DEVICE_PING_URL"           , devicePingURL);
        JavaScriptTools.writeJSVar(out, "DEVICE_PUSHPIN"            , devicePushpinNdx);
        JavaScriptTools.writeJSVar(out, "KML_UPDATE_URL"            , kmlUpdURL);
        JavaScriptTools.writeJSVar(out, "PARM_RANGE_FR"             , Calendar.PARM_RANGE_FR[0]);
        JavaScriptTools.writeJSVar(out, "PARM_RANGE_TO"             , Calendar.PARM_RANGE_TO[0]);
        JavaScriptTools.writeJSVar(out, "PARM_TIMEZONE"             , Calendar.PARM_TIMEZONE[0]);
        JavaScriptTools.writeJSVar(out, "PARM_LIMIT"                , PARM_MAP_LIMIT);
        JavaScriptTools.writeJSVar(out, "PARM_LIMIT_TYPE"           , PARM_MAP_LIMIT_TYPE);
        JavaScriptTools.writeJSVar(out, "PARM_DEVICE_GROUP"         , parmDevGrp);
        JavaScriptTools.writeJSVar(out, "PARM_DEVICE_COMMAND"       , PARM_DEVICE_COMMAND);
        JavaScriptTools.writeJSVar(out, "BATTERY_LEVEL_TYPE"        , showBatteryLevel);

        /* end JavaScript */
        JavaScriptTools.writeEndJavaScript(out);

        /* FullMap.js */
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("FullMap.js"), request);

        /* sorttable.js */
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(ReportPresentation.SORTTABLE_JS), request);

    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState, 
        String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N    i18n           = privLabel.getI18N(FullMap.class);
        final Locale  locale         = reqState.getLocale();
        final String  devTitles[]    = reqState.getDeviceTitles();
        final String  grpTitles[]    = reqState.getDeviceGroupTitles();
        final Account currAcct       = reqState.getCurrentAccount(); // guaranteed, since login is required
        final User    currUser       = reqState.getCurrentUser();    // may be null
        String m = pageMsg;

        HttpServletRequest request = reqState.getHttpServletRequest();
        String  rangeFr  = (String)AttributeTools.getRequestAttribute(request, Calendar.PARM_RANGE_FR, "");
        String  rangeTo  = (String)AttributeTools.getRequestAttribute(request, Calendar.PARM_RANGE_TO, "");
        String  tzStr    = (String)AttributeTools.getRequestAttribute(request, Calendar.PARM_TIMEZONE, "");
        String  cmdName  = reqState.getCommandName();
        String  cmdArg   = reqState.getCommandArg();

        /* limit info */
        long   limitCnt  = AttributeTools.getRequestLong(  request, PARM_MAP_LIMIT     , -1L);
        String limitType = AttributeTools.getRequestString(request, PARM_MAP_LIMIT_TYPE, "");

        /* set "fleet" request type */
        final boolean isFleet = this.isFleet();
        reqState.setFleet(isFleet);

        /* no defined Device? */
        final Device device;
        if (isFleet) {
            device = null;
        } else {
            device = reqState.getSelectedDevice();
            if (device == null) {
                String devID = reqState.getSelectedDeviceID();
                if (StringTools.isBlank(devID)) {
                    m = i18n.getString("FullMap.noDevices","There are currently no defined/authorized devices for this account.");
                    //Track.writeErrorResponse(reqState, m);
                    //return;
                } else {
                    m = i18n.getString("FullMap.invalidDevices","Specified device ''{0}'' does not exist, or is invalid.", devID);
                }
            }
        }

        /* device "Ping" */
        final Map<String,String> commandMap;
        final boolean deviceSupportsPing;
        String showLocateNow = this.getStringProperty(privLabel,PrivateLabel.PROP_FullMap_showLocateNow,"device");
        if (isFleet) {
            // no "ping" for fleet
            commandMap = null;
            deviceSupportsPing = false;
        } else
        if (device == null) {
            // unlikely - no "ping" if device is null
            commandMap = null;
            deviceSupportsPing = false;
        } else 
        if (ListTools.containsIgnoreCase(SHOW_PING_FALSE,showLocateNow)) {
            // explicit "false"
            commandMap = null;
            deviceSupportsPing = false;
        } else
        if (ListTools.containsIgnoreCase(SHOW_PING_TRUE,showLocateNow)) {
            // explicit "true"
            commandMap = device.getSupportedCommands(privLabel,currUser,"map");
            deviceSupportsPing = true;
        } else {
            // check for other device "ping"
            commandMap = device.getSupportedCommands(privLabel,currUser,"map");
            deviceSupportsPing = !ListTools.isEmpty(commandMap) || device.isPingSupported(privLabel,currUser);
        }

        /* device link */
        final boolean showDeviceLink = !isFleet && Device.supportsLinkURL() && 
            this.getBooleanProperty(privLabel,PrivateLabel.PROP_FullMap_showDeviceLink,true);

        /* battery level */
        final int showBatteryLevel; // 0=no, 1=icon, 2=percent
        if (isFleet) {
            // no battery level on fleet map
            showBatteryLevel = 0;
        } else
        if (device == null) {
            // no device, no battery level
            showBatteryLevel = 0;
        } else {
            // check for true,false,default
            String blvlProp = this.getStringProperty(privLabel,PrivateLabel.PROP_FullMap_showBatteryLevel,"").toLowerCase();
            if (StringTools.isBlank(blvlProp) || (blvlProp.indexOf("false") >= 0)) {
                showBatteryLevel = 0;
            } else {
                boolean icon     = (blvlProp.indexOf("icon") >= 0);
                boolean percent  = !icon && (blvlProp.indexOf("percent") >= 0);
                int     dispType = percent? 2 : 1;
                if ((blvlProp.indexOf("default") >= 0) || (blvlProp.indexOf("device") >= 0)) {
                    showBatteryLevel = (device.getLastBatteryLevel() > 0.0)? dispType : 0;
                } else {
                    showBatteryLevel = dispType;
                }
            }
        }

        /* page links */
        final String PageLinks[] = StringTools.split(this.getStringProperty(privLabel,PrivateLabel.PROP_FullMap_pageLinks,null),',');
        final boolean includePageLinks = (PageLinks != null) && (PageLinks.length > 0);

        /* Google KML [COMMAND_KML_UPDATE] */
        final String  googleKmlArg;
        String _googleKmlArg = this.getStringProperty(privLabel,PrivateLabel.PROP_FullMap_showGoogleKML,null);
        if (_googleKmlArg == null) {
            googleKmlArg     = null;
        } else
        if (_googleKmlArg.equalsIgnoreCase("last")) {
            googleKmlArg     = "last";
        } else {
            googleKmlArg     = null;
        }

        /* TimeZone */
        if (StringTools.isBlank(tzStr)) {
            if (currUser != null) {
                // try User timezone
                tzStr = currUser.getTimeZone(); // may be blank
                if (StringTools.isBlank(tzStr) || tzStr.equals(User.DEFAULT_TIMEZONE)) {
                    // override with Account timezone
                    tzStr = currAcct.getTimeZone();
                }
            } else {
                // get Account timezone
                tzStr = currAcct.getTimeZone();
            }
            if (StringTools.isBlank(tzStr)) {
                // make sure we have a timezone 
                // (unecessary, since Account/User will return a timezone)
                tzStr = Account.DEFAULT_TIMEZONE;
            }
        }
        TimeZone tz = DateTime.getTimeZone(tzStr); // will be GMT if invalid
        AttributeTools.setSessionAttribute(request, Calendar.PARM_TIMEZONE[0], tzStr);
        reqState.setTimeZone(tz, tzStr);
        DateTime now = new DateTime(tz);

        /* last event from device */
        DateTime lastEventTime = null;
        if (!isFleet) {
            try {
                EventData evd[] = (device != null)? device.getLatestEvents(1L,true) : null;
                if (!ListTools.isEmpty(evd)) {
                    lastEventTime = new DateTime(evd[0].getTimestamp());
                    reqState.setLastEventTime(lastEventTime);
                }
            } catch (DBException dbe) {
                // ignore
            }
        }

        /* range 'from' [keywords: frDate, startDate, dateRange] */
        // "YYYY/MM[/DD[/hh[:mm[:ss]]]]"  ie "YYYY/MM/DD/hh:mm:ss"
        DateTime dateFr; // initialized below
        String rangeFrFld[] = !StringTools.isBlank(rangeFr)? StringTools.parseString(rangeFr, "/:") : null;
        if (!this.showFromCalendar) {
            dateFr = null;
        } else
        if (ListTools.isEmpty(rangeFrFld)) {
            if (isFleet) {
                // one month ago
                // (only saved if displaying the 'From' Calendar
                dateFr = new DateTime(now.getMonthDelta(tz,-1), tz);
            } else {
                // beginning of today
                if (lastEventTime != null) {
                    dateFr = new DateTime(lastEventTime.getDayStart(tz), tz);
                } else {
                    dateFr = new DateTime(now.getDayStart(tz), tz);
                }
            }
        } else
        if (rangeFrFld.length == 1) {
            // parse as 'Epoch' time
            long epoch = StringTools.parseLong(rangeFrFld[0], now.getTimeSec());
            dateFr = new DateTime(epoch, tz);
        } else {
            // (rangeFrFld.length >= 2)
            int YY = StringTools.parseInt(rangeFrFld[0], now.getYear());
            int MM = StringTools.parseInt(rangeFrFld[1], now.getMonth1());
            int DD;     // initialized below
            int hh = 0; // default to beginning of day
            int mm = 0;
            int ss = 0;
            if (rangeFrFld.length >= 3) {
                // at least YYYY/MM/DD provided
                DD = StringTools.parseInt(rangeFrFld[2], now.getDayOfMonth());
                if (rangeFrFld.length >= 4) { hh = StringTools.parseInt(rangeFrFld[3], hh); }
                if (rangeFrFld.length >= 5) { mm = StringTools.parseInt(rangeFrFld[4], mm); }
                if (rangeFrFld.length >= 6) { ss = StringTools.parseInt(rangeFrFld[5], ss); }
            } else {
                // only YYYY/MM provided
                DD = 1;
            }
            dateFr = new DateTime(tz, YY, MM, DD, hh, mm, ss);
            //Print.logInfo("Fr: YY="+YY+", MM="+MM+", DD="+DD+", hh="+hh+", mm="+mm+", ss="+ss);
        }

        /* range 'to'  [keywords: toDate, endDate, dateRange] */
        // "YYYY/MM[/DD[/hh[:mm[:ss]]]]"  ie "YYYY/MM/DD/hh:mm:ss"
        DateTime dateTo; // initialized below
        String rangeToFld[] = !StringTools.isBlank(rangeTo)? StringTools.parseString(rangeTo, "/:") : null;
        if (ListTools.isEmpty(rangeToFld)) {
            if (isFleet) {
                // end of today
                dateTo = new DateTime(now.getDayEnd(tz), tz);
            } else {
                // end of today
                if (lastEventTime != null) {
                    dateTo = new DateTime(lastEventTime.getDayEnd(tz), tz);
                } else {
                    dateTo = new DateTime(now.getDayEnd(tz), tz);
                }
            }
        } else
        if (rangeToFld.length == 1) {
            // parse as 'Epoch' time
            long epoch = StringTools.parseLong(rangeToFld[0], now.getTimeSec());
            dateTo = new DateTime(epoch, tz);
        } else {
            // (rangeToFld.length >= 2)
            int YY = StringTools.parseInt(rangeToFld[0], now.getYear());
            int MM = StringTools.parseInt(rangeToFld[1], now.getMonth1());
            int DD;      // initialized below
            int hh = 23; // default to end of day
            int mm = 59;
            int ss = 59;
            if (rangeToFld.length >= 3) {
                // at least YYYY/MM/DD provided
                DD = StringTools.parseInt(rangeToFld[2], now.getDayOfMonth());
                if (rangeToFld.length >= 4) { hh = StringTools.parseInt(rangeToFld[3], hh); }
                if (rangeToFld.length >= 5) { mm = StringTools.parseInt(rangeToFld[4], mm); }
                if (rangeToFld.length >= 6) { ss = StringTools.parseInt(rangeToFld[5], ss); }
            } else {
                // only YYYY/MM provided
                DD = DateTime.getDaysInMonth(tz, MM, YY);
            }
            dateTo = new DateTime(tz, YY, MM, DD, hh, mm, ss);
            //Print.logInfo("To: YY="+YY+", MM="+MM+", DD="+DD+", hh="+hh+", mm="+mm+", ss="+ss);
        }

        /* save from/to dates */
        if ((dateFr != null) && dateFr.isAfter(dateTo)) { 
            dateFr = dateTo; 
        }
        if (this.showFromCalendar) {
            reqState.setEventDateFrom(dateFr);
            AttributeTools.setSessionAttribute(request, Calendar.PARM_RANGE_FR[0], Calendar.formatArgDateTime(dateFr));
        } else {
            reqState.setEventDateFrom(null);
            AttributeTools.setSessionAttribute(request, Calendar.PARM_RANGE_FR[0], "");
        }
        reqState.setEventDateTo(dateTo);
        AttributeTools.setSessionAttribute(request, Calendar.PARM_RANGE_TO[0], Calendar.formatArgDateTime(dateTo));
        //Print.logInfo("Date Range: " + dateFr + " ==> " + dateTo);

        /* map provider */
        final MapProvider mapProvider = reqState.getMapProvider();
        if (mapProvider == null) {
            Track.writeErrorResponse(reqState, i18n.getString("FullMap.noMapProvider","No Map Provider defined for this URL"));
            return;
        }

        /* map dimension */
        final MapDimension mapDim = mapProvider.getDimension();
        final boolean mapAutoSize = (mapDim.getHeight() < 0);

        /* event limit/type */
        long maxPushpins = mapProvider.getMaxPushpins(reqState);
        if ((limitCnt <= 0L) || (limitCnt > maxPushpins)) {
            limitCnt = maxPushpins;
        }
        reqState.setEventLimit(limitCnt);
        reqState.setEventLimitType(limitType);

        /* KML Display */
        if (cmdName.equals(COMMAND_KML_UPDATE)) {
            HttpServletResponse response = reqState.getHttpServletResponse();
            PrintWriter out = response.getWriter();
            try {
                int statCodes[]  = this.getStatusCodes();
                long perDevLimit = (!StringTools.isBlank(cmdArg) && cmdArg.equals("last"))? 1L : -1L;
                Collection<Device> devList = reqState.getMapEventsByDevice(statCodes, perDevLimit); // [KML] does not return null
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_KML());
                GoogleKML.getInstance().writeEvents(out, 
                    currAcct, devList, 
                    privLabel);
            } catch (DBException dbe) {
                Print.logException("Error reading Events", dbe);
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
                out.println("\nError reading Events");
            }
            return;
        }

        /* DeviceList/GroupList data request */
        if (cmdName.equals(COMMAND_DEVICE_LIST)) {
            this.writeDeviceList(reqState);
            return;
        } else
        if (cmdName.equals(COMMAND_GROUP_LIST)) {
            this.writeDeviceGroupList(reqState);
            return;
        }

        /* MapUpdate data request (special case of 'Map') */
        if (cmdName.equals(COMMAND_MAP_UPDATE)) {
            // This is how the displayed map gets its data
            int statCodes[] = this.getStatusCodes();
            mapProvider.writeMapUpdate(
                EventUtil.MAPDATA_JSON,
                reqState, 
                statCodes);
            return;
        }

        /* Device Ping request (special case of 'Map') */
        if (cmdName.equals(COMMAND_DEVICE_PING)) {
            HttpServletResponse response = reqState.getHttpServletResponse();
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN()); // UTF-8?
            PrintWriter out = response.getWriter();
            if (!isFleet) {
                String pingType   = DCServerConfig.COMMAND_CONFIG;
                String pingName   = (String)AttributeTools.getRequestAttribute(request, PARM_DEVICE_COMMAND, ""); // session or query
                String pingArgs[] = null;
                if (device == null) {
                    // no device? (unlikely here, but we must check anyway)
                    Print.logError("Ping Error: device is null!");
                    out.println(Track.DATA_RESPONSE_PING_ERROR);
                } else
                if (!device.sendDeviceCommand(pingType,pingName,pingArgs)) {
                    // unable to send ping? (not supported, etc.)
                    Print.logError("Ping Failed: %s/%s", device.getAccountID(), device.getDeviceID());
                    out.println(Track.DATA_RESPONSE_PING_ERROR);
                } else {
                    // 'ping' successful
                    Print.logInfo("Device Ping: %s/%s", device.getAccountID(), device.getDeviceID());
                    try {
                        // save ping count information
                        device.save();
                    } catch (DBException dbe) {
                        Print.logException("Saving Device 'Ping' count", dbe);
                    }
                    out.println(Track.DATA_RESPONSE_PING_OK);
                }
            } else {
                Print.logInfo("Invalid device ping while viewing fleet map ...");
                out.println(Track.DATA_RESPONSE_ERROR);
            }
            return;
        }

        /* auto-update attributes */
        boolean autoUpdateOk = privLabel.hasReadAccess(currUser, this.getAclName(_ACL_AUTO));
        final boolean autoUpdateEnabled;
        final boolean autoUpdateOnLoad;
        final long    autoInterval;
        final long    autoMaxCount;
        long _autoIntrv = 0L;
        long _autoMaxCt = 0L;
        if (cmdName.equals(COMMAND_AUTO_UPDATE)) {
            Print.logInfo("Auto-Update: arg = " + cmdArg);
            long v[] = StringTools.parseLong(StringTools.split(cmdArg,','),0L);
            if ((v != null) && (v.length == 2)) {
                _autoIntrv = v[0];
                _autoMaxCt = (v[1] > 0L)? v[1] : 2L;
            }
        }
        if (!autoUpdateOk) {
            // not authorized
            autoUpdateEnabled   = false;
            autoUpdateOnLoad    = false;
            autoInterval        = 0L;
            autoMaxCount        = 0L;
        } else
        if (_autoIntrv > 0L) {
            // overridden 
            autoUpdateEnabled   = true;
            autoUpdateOnLoad    = true;
            autoInterval        = _autoIntrv;
            autoMaxCount        = _autoMaxCt;
        } else {
            // Map properties check
            boolean dftAutoUpdateEnabled = mapProvider.getAutoUpdateEnabled(isFleet);
            boolean dftAutoUpdateOnLoad  = mapProvider.getAutoUpdateOnLoad(isFleet);
            long    dftAutoInterval      = mapProvider.getAutoUpdateInterval(isFleet);
            long    dftAutoMaxCount      = mapProvider.getAutoUpdateCount(isFleet);
            autoUpdateEnabled   = this.getBooleanProperty(privLabel, PROP_autoUpdate_enable  , dftAutoUpdateEnabled);
            autoUpdateOnLoad    = this.getBooleanProperty(privLabel, PROP_autoUpdate_onload  , dftAutoUpdateOnLoad);
            autoInterval        = this.getLongProperty(   privLabel, PROP_autoUpdate_interval, dftAutoInterval);
            autoMaxCount        = this.getLongProperty(   privLabel, PROP_autoUpdate_count   , dftAutoMaxCount);
        }
        
        /* MapShapes */
        final Map<String,String> zoomRegions;
        final Map<String,MapShape> mapShapes = reqState.getZoomRegionShapes();
        if (mapShapes != null) {
            zoomRegions = new OrderedMap<String,String>();
            for (MapShape ms : mapShapes.values()) {
                if (ms.isZoomTo()) {
                    //Print.logInfo("Adding ZoomRegion: " + ms.getName());
                    zoomRegions.put(ms.getName(), ms.getDescription());
                } else {
                    //Print.logInfo("Skipping ZoomRegion: " + ms.getName());
                }
            }
        } else {
            zoomRegions = null;
        }

        /* Map attributes */
        final boolean showTimezoneSelect        = 
            this.getBooleanProperty(privLabel,PrivateLabel.PROP_FullMap_showTimezoneSelection,true);
        final boolean showPushpinReplay         = !isFleet && mapProvider.getReplayEnabled() &&
            this.getBooleanProperty(privLabel,PrivateLabel.PROP_FullMap_showPushpinReplay,true);
        final boolean showUpdateAll             = 
            this.getBooleanProperty(privLabel,PrivateLabel.PROP_FullMap_showUpdateAll,true);
        final boolean showUpdateLast            = !isFleet && 
            mapProvider.isFeatureSupported(MapProvider.FEATURE_CENTER_ON_LAST) && 
            this.getBooleanProperty(privLabel,PrivateLabel.PROP_FullMap_showUpdateLast,false);
        final boolean mapSupportsCursorLocation = 
            mapProvider.isFeatureSupported(MapProvider.FEATURE_LATLON_DISPLAY) &&
            this.getBooleanProperty(privLabel,PrivateLabel.PROP_FullMap_showCursorLocation,true);
        final boolean mapSupportsDistanceRuler  = 
            mapProvider.isFeatureSupported(MapProvider.FEATURE_DISTANCE_RULER) &&
            this.getBooleanProperty(privLabel,PrivateLabel.PROP_FullMap_showDistanceRuler,true);

        /* Style Sheets */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                mapProvider.writeStyle(out, reqState);
                Calendar.writeStyle(out, reqState);
                DeviceChooser.writeStyle(out, reqState);
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String pageName = FullMap.this.getPageName();
                mapProvider.writeJavaScript(out, reqState);
                Calendar.writeJavaScript(out, reqState);
                DeviceChooser.writeJavaScript(out, locale, reqState,
                    privLabel.getWebPageURL(reqState, pageName, Track.COMMAND_DEVICE_LIST));
                int devicePushpinNdx = -99;
                if (!isFleet && (device != null) && device.hasPushpinID()) {
                    String devIcon = device.getPushpinID();
                    OrderedSet<String> iconKeys = reqState.getMapProviderIconKeys();
                    devicePushpinNdx = EventData._getPushpinIconIndex(devIcon, iconKeys, -1);
                }
                FullMap.this.writeJS_MapUpdate(reqState, out, 
                    //EncodeMakeURL(reqState, RequestProperties.TRACK_BASE_URI(), pageName, COMMAND_MAP_UPDATE ), 
                    privLabel.getWebPageURL(reqState, pageName, COMMAND_MAP_UPDATE ),
                    //EncodeMakeURL(reqState, RequestProperties.TRACK_BASE_URI(), pageName, COMMAND_DEVICE_PING),
                    privLabel.getWebPageURL(reqState, pageName, COMMAND_DEVICE_PING),
                    EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI()+".kml",pageName,COMMAND_KML_UPDATE,googleKmlArg),
                    autoUpdateEnabled, autoUpdateOnLoad, autoInterval, autoMaxCount,
                    showBatteryLevel, devicePushpinNdx
                    );
            }
        };

        /* content */
        HTMLOutput HTML_CONTENT = new HTMLOutput((mapAutoSize? CommonServlet.CSS_CONTENT_MAP_FULL : CommonServlet.CSS_CONTENT_MAP), m) {
            public void write(PrintWriter out) throws IOException {
                String pageName = FullMap.this.getPageName();

                /* Command Form */
                // This entire form is 'hidden'.  It's used by JS functions to submit specific commands 
                String actionURL = Track.GetBaseURL(reqState); // EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI());
                out.println("\n<!-- Command form -->");
                out.println("<form id='"+FORM_COMMAND+"' name='"+FORM_COMMAND+"' method='post' action=\""+actionURL+"\" target='_self'>"); // target='_top'
                out.println("  <input type='hidden' name='"+PARM_PAGE                 +"' value=''/>");
                out.println("  <input type='hidden' name='"+PARM_COMMAND              +"' value=''/>");
                out.println("  <input type='hidden' name='"+PARM_ARGUMENT             +"' value=''/>");
                out.println("  <input type='hidden' name='"+Calendar.PARM_RANGE_FR[0] +"' value=''/>");
                out.println("  <input type='hidden' name='"+Calendar.PARM_RANGE_TO[0] +"' value=''/>");
                out.println("  <input type='hidden' name='"+Calendar.PARM_TIMEZONE[0] +"' value=''/>");
                out.println("</form>");
                out.println("\n");

                /* Map table/row */
                out.println("<table border='0' cellspacing='0' cellpadding='0' style='width:100%; height:100%;'>");
                out.println("<tr>");
                out.println("<td style='width:100%; height:100%; padding:0px 0px 0px 0px;'>");
                MapDimension mapDim = new MapDimension(-1,mapProvider.getHeight());
                mapProvider.writeMapCell(out, reqState, mapDim);
                out.println("</td>\n");
                out.println("</tr>\n");
                out.println("</table>");

                /* write DeviceChooser DIV */
                IDDescription.SortBy dcSortBy = DeviceChooser.getSortBy(privLabel);
                java.util.List<IDDescription> idList = reqState.createIDDescriptionList(isFleet, dcSortBy);
                IDDescription list[] = idList.toArray(new IDDescription[idList.size()]);
                DeviceChooser.writeChooserDIV(out, reqState, list, null);

            }
        };

        /* write frame */
        CommonServlet.writePageFrame(
            reqState,
            "javascript:fmOnLoad();","javascript:fmOnUnload();",    // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // Javascript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

}
