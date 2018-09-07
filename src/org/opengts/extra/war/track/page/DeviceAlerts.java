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
package org.opengts.extra.war.track.page;

/* explicit imports required (due to conflict with "Calendar") */
//import java.util.*;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Comparator;
import java.util.Vector;
import java.util.Map;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.AclEntry.AccessLevel;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.Calendar;
import org.opengts.war.track.*;
import org.opengts.war.track.page.TrackMap;

public class DeviceAlerts
    extends WebPageAdaptor
    implements Constants
{

    
    private static final boolean INCLUDE_ALL_DEVICES        = true;
    private static final long    MAX_ACTIVE_AGE_SEC         = DateTime.HourSeconds(6);

    // ------------------------------------------------------------------------

    private static final String _ACL_LIST[]                 = null;

    // ------------------------------------------------------------------------
    // Parameters
    
    // forms
    public  static final String  FORM_DEVICE_SELECT         = "DeviceAlertsSelect";

    // commands
    public  static final String  COMMAND_INFO_SEL_DEVICE    = "selectDev";

    // submit
    public  static final String  PARM_SUBMIT_REFRESH        = "d_subrefr";
    public  static final String  PARM_SUBMIT_VIEW           = "d_subview";
    public  static final String  PARM_SUBMIT_CLEAR          = "d_subclear";
    public  static final String  PARM_SUBMIT_CLEAR_ALL      = "d_subclrall";

    // buttons
    public  static final String  PARM_BUTTON_CANCEL         = "d_btncan";
    public  static final String  PARM_BUTTON_BACK           = "d_btnbak";
    
    // auto-refresh indicator
    public  static final String  PARM_IS_AUTO_REFRESH       = "isAutoRefresh";

    /* id's */
    private static final String  ID_ALERT_REFRESH_BTN       = "AlertRefreshButton";

    // ------------------------------------------------------------------------

    /**
    *** NotifyTimeComparator class
    **/
    private static class NotifyTimeComparator
        implements Comparator<Device>
    {
        public int compare(Device d1, Device d2) {
            long ts1 = (d1 != null)? d1.getLastNotifyTime() : 0L;
            long ts2 = (d1 != null)? d2.getLastNotifyTime() : 0L;
            return (ts2 > ts1)? 1 : (ts2 < ts1)? -1 : 0;
        }
        public boolean equals(Object other) {
            return (other instanceof NotifyTimeComparator);
        }
    }
    
    private static NotifyTimeComparator GetNotifyTimeComparator()
    {
        // TODO: optimize
        return new NotifyTimeComparator();
    }

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public DeviceAlerts()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_DEVICE_ALERTS);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
    }
    
    // ------------------------------------------------------------------------
   
    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_ADMIN;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DeviceAlerts.class);
        String devTitles[] = reqState.getDeviceTitles();
        return super._getMenuDescription(reqState,i18n.getString("DeviceAlerts.editMenuDesc","View {0} Alerts", devTitles));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DeviceAlerts.class);
        String devTitles[] = reqState.getDeviceTitles();
        return super._getMenuHelp(reqState,i18n.getString("DeviceAlerts.editMenuHelp","View {0} Alerts", devTitles));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DeviceAlerts.class);
        String devTitles[] = reqState.getDeviceTitles();
        return super._getNavigationDescription(reqState,i18n.getString("DeviceAlerts.navDesc","{0} Alerts", devTitles));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DeviceAlerts.class);
        String devTitles[] = reqState.getDeviceTitles();
        return super._getNavigationTab(reqState,i18n.getString("DeviceAlerts.navTab","{0} Alerts", devTitles));
    }

    // ------------------------------------------------------------------------
    
    public String[] getChildAclList()
    {
        return _ACL_LIST;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* is alert active? (active if less than 6 hours old) */
    private boolean isActiveAlert(long ts, long maxActiveAge)
    {
        return ((ts + maxActiveAge) > DateTime.getCurrentTimeSec());
    }

    /* Get the device alert status code description */
    private String getAlertStyle(long ts, long maxActiveAge)
    {
        if (ts <= 0L) {
            return "color: black;";
        } else
        if (this.isActiveAlert(ts,maxActiveAge)) {
            return "color: " + ColorTools.RED.toString(true);
        } else {
            return "color: " + ColorTools.DARK_YELLOW.toString(true);
        }
    }

    /* Get the device alert status code description */
    private String getAlertStatus(Device dev, PrivateLabel privLabel)
    {
        if ((dev != null) && (dev.getLastNotifyTime() > 0L)) {
            return StatusCode.getDescription(dev, dev.getLastNotifyCode(), privLabel, "");
        }
        return "";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* replace blank strings with html "&nbsp;" */
    private String filter(String s)
    {
        if ((s == null) || s.equals("")) {
            return "&nbsp;";
        } else {
            return s;
        }
    }

    /* write html */
    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel   = reqState.getPrivateLabel();
        final I18N         i18n        = privLabel.getI18N(DeviceAlerts.class);
        final Locale       locale      = reqState.getLocale();
        final String       devTitles[] = reqState.getDeviceTitles();
        final String       grpTitles[] = reqState.getDeviceGroupTitles();
        final Account      currAcct    = reqState.getCurrentAccount();
        final User         currUser    = reqState.getCurrentUser();
        final String       pageName    = this.getPageName();
        String m = pageMsg;
        boolean error = false;
        
        /* is auto-refresh? */
        final boolean isAutoRefresh = AttributeTools.getRequestBoolean(request, PARM_IS_AUTO_REFRESH, false);

        /* device */
        OrderedSet<String> devList = reqState.getDeviceIDList(false);
        if (devList == null) { devList = new OrderedSet<String>(); }
        Device selDev   = reqState.isActualSpecifiedDevice()? reqState.getSelectedDevice() : null;
        String selDevID = (selDev != null)? selDev.getDeviceID() : "";
        
        /* qualify device */
        // a bit of a hack to ignore the selected device if it isn't to be displayed on this page anyway
        final boolean showAllDevices = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceAlerts_showAllDevices,INCLUDE_ALL_DEVICES);
        if ((selDev != null) && (selDev.getLastNotifyTime() <= 0L) && !showAllDevices) {
            selDev   = null;
            selDevID = "";
        }

        /* ACL allow edit/view */
        boolean allowClear      = privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView       = allowClear || privLabel.hasReadAccess(currUser, this.getAclName());

        /* submit buttons */
        String  submitClearAll  = AttributeTools.getRequestString(request, PARM_SUBMIT_CLEAR_ALL, "");
        String  submitClear     = AttributeTools.getRequestString(request, PARM_SUBMIT_CLEAR    , "");
        String  submitView      = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW     , "");
        String  submitRefrsh    = AttributeTools.getRequestString(request, PARM_SUBMIT_REFRESH  , "");

        /* Refresh */
        if (SubmitMatch(submitRefrsh,i18n.getString("DeviceAlerts.refresh","Refresh"))) {
            /* we realy don't need to do anything, since we are already in the process of updating this page
            WebPage thisWP = privLabel.getWebPage(this.getPageName());
            String refreshURL = thisWP.encodePageURL(reqState);
            RequestDispatcher rd = request.getRequestDispatcher(refreshURL);
            if (rd != null) {
                // MUST clear PARM_SUBMIT_REFRESH attribute! Otherwise recursion havoc will ensue!
                try {
                    HttpServletResponse response = reqState.getHttpServletResponse();
                    rd.forward(request, response);
                    return;
                } catch (ServletException se) {
                    Print.logException("JSP error: " + refreshURL, se);
                    // continue below
                }
            } else {
                Print.logError("RequestDispatcher not found for URL: " + refreshURL);
            }
            */
        }

        /* command */
        String  deviceCmd     = reqState.getCommandName();
        boolean selectDevice  = deviceCmd.equals(COMMAND_INFO_SEL_DEVICE);
        boolean clearAlert    = false;
        boolean clearAlertAll = false;
        boolean viewMap       = false;

        /* pre-qualify commands */
        if (selectDevice) {
            if (SubmitMatch(submitClearAll,i18n.getString("DeviceAlerts.clearAll","Clear All"))) {
                if (!allowClear) {
                    clearAlertAll = false; // not authorized
                } else {
                    clearAlertAll = true;
                }
            } else
            if (SubmitMatch(submitClear,i18n.getString("DeviceAlerts.clearAlert","Clear Alert"))) {
                if (!allowClear) {
                    clearAlert = false; // not authorized
                } else
                if (selDev == null) {
                    m = i18n.getString("DeviceAlerts.pleaseSelectDevice","Please select a {0}", devTitles);
                    error = true;
                    clearAlert = false; // not selected
                } else {
                    clearAlert = true;
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("DeviceAlerts.viewMap","View Map"))) {
                if (!allowView) {
                    viewMap = false; // not authorized
                } else
                if (selDev == null) {
                    m = i18n.getString("DeviceAlerts.pleaseSelectDevice","Please select a {0}", devTitles);
                    error = true;
                    viewMap = false; // not selected
                } else {
                    viewMap = true;
                }
            }
        }

        /* view map? */
        if (viewMap) {
            // 'selDev' is not null
            String dftPage = PAGE_MAP_DEVICE;
            String mapPage = StringTools.blankDefault(privLabel.getStringProperty(PrivateLabel.PROP_DeviceAlerts_mapPageName,null),dftPage);
            WebPage  mapWP = privLabel.getWebPage(mapPage);
            if (mapWP != null) { // should make sure that (mapWP != this)?
                String devMapURL = mapWP.encodePageURL(reqState,TrackMap.COMMAND_AUTO_UPDATE,"60,30");
                URIArg devMapURIArg = new URIArg(devMapURL);
                
                /* device */
                devMapURIArg.addArg(Constants.PARM_DEVICE, selDevID);
                
                /* date range */
                TimeZone tmz     = currAcct.getTimeZone(null);
                long   nowTime   = DateTime.getCurrentTimeSec();
                long   alertTime = selDev.getLastNotifyTime();
                if (alertTime <= 0L) { alertTime = DateTime.getCurrentTimeSec(); }
                long   fromTime  = (new DateTime(alertTime - DateTime.HourSeconds(2),tmz)).getDayStart(tmz);
                long   toTime    = (new DateTime(nowTime   + DateTime.HourSeconds(2),tmz)).getDayEnd(tmz);
                devMapURIArg.addArg(Calendar.PARM_RANGE_FR[0], fromTime);
                devMapURIArg.addArg(Calendar.PARM_RANGE_TO[0], toTime  );
                devMapURIArg.addArg(Calendar.PARM_TIMEZONE[0], reqState.getTimeZoneString(null));

                /* forward to page */
                devMapURL = devMapURIArg.toString();
                Print.logInfo("Forwarding to URL: " + devMapURL);
                RequestDispatcher rd = request.getRequestDispatcher(devMapURL);
                if (rd != null) {
                    try {
                        HttpServletResponse response = reqState.getHttpServletResponse();
                        rd.forward(request, response);
                        return;
                    } catch (ServletException se) {
                        Print.logException("JSP error: " + devMapURL, se);
                        // continue below
                    }
                } else {
                    Print.logError("RequestDispatcher not found for URL: " + devMapURL);
                    m = i18n.getString("DeviceAlerts.errorViewMap","Internal error viewing map");
                    error = true;
                }
                
            } else {
                Print.logError("Device map page not found: " + mapPage);
                m = i18n.getString("DeviceAlerts.errorViewMap","Internal error viewing map");
                error = true;
            }
        }

        /* clear alert? */
        if (clearAlert) {
            // 'selDev' guaranteed non-null here
            try {
                Print.logWarn("Clearing Alert: " + selDevID);
                selDev.setLastNotifyTime(0L);
                selDev.setLastNotifyCode(StatusCodes.STATUS_NONE);
                selDev.update(Device.FLD_lastNotifyTime, Device.FLD_lastNotifyCode);
                /*
                selDevID = "";
                selDev = null;
                reqState.clearDeviceList();
                devList = reqState.getDeviceIDList(true); // include inactive
                if (!ListTools.isEmpty(devList)) {
                    selDevID = devList.get(0);
                    try {
                        selDev = !selDevID.equals("")? Device.getDevice(currAcct, selDevID) : null; // may still be null
                    } catch (DBException dbe) {
                        // ignore
                    }
                }
                */
            } catch (DBException dbe) {
                Print.logException("Clearing Device Alert", dbe);
                m = i18n.getString("DeviceAlerts.errorClear","Internal error clearing {0} alert", devTitles);
                error = true;
            }
        } else
        if (clearAlertAll) {
            try {
                Print.logWarn("Clearing All Alerts");
                for (int d = 0; d < devList.size(); d++) {
                    Device dev = Device.getDevice(currAcct, devList.get(d));
                    if (dev != null) {
                        dev.setLastNotifyTime(0L);
                        dev.setLastNotifyCode(StatusCodes.STATUS_NONE);
                        dev.update(Device.FLD_lastNotifyTime, Device.FLD_lastNotifyCode);
                    }
                }
            } catch (DBException dbe) {
                Print.logException("Clearing All Device Alert", dbe);
                m = i18n.getString("DeviceAlerts.errorClear","Internal error clearing {0} alert", devTitles);
                error = true;
            }
        }

        /* Get 'Alert' Device List */
        int activeAlertCount = 0;
        final long activeAlertAge = privLabel.getLongProperty(PrivateLabel.PROP_DeviceAlerts_maxActiveAlertAge,MAX_ACTIVE_AGE_SEC);
        final java.util.List<Device> alertDevList = new Vector<Device>();
        for (int d = 0; d < devList.size(); d++) {
            try {
                Device dev = Device.getDevice(currAcct, devList.get(d));
                if (dev != null) {
                    long lastNotifyTime = dev.getLastNotifyTime();
                    if ((lastNotifyTime > 0L) || showAllDevices) {
                        alertDevList.add(dev);
                        if (this.isActiveAlert(lastNotifyTime,activeAlertAge)) {
                            activeAlertCount++;
                        }
                    }
                }
            } catch (DBException dbe) {
                // ignore device
            }
        }
        ListTools.sort(alertDevList, GetNotifyTimeComparator()); // descending by last alert time
        
        /* update/reset selected device */
        if (!ListTools.isEmpty(alertDevList) && ((selDev == null) || !isAutoRefresh)) {
            selDev   = alertDevList.get(0);
            selDevID = selDev.getDeviceID();
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = DeviceAlerts.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "DeviceAlerts.css", cssDir);
            }
        };

        /* JavaScript */
        final int _actionAlertCount = activeAlertCount;
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(SORTTABLE_JS), request);

                /* Refresh URL */
                WebPage thisPage = privLabel.getWebPage(DeviceAlerts.this.getPageName());
                String refreshURL = (thisPage != null)? 
                    thisPage.encodePageURL(reqState) + "&" + PARM_IS_AUTO_REFRESH + "=true" : 
                    "";

                /* start JavaScript */
                int refreshInterval = isAutoRefresh? privLabel.getIntProperty(PrivateLabel.PROP_DeviceAlerts_refreshInterval,-1) : -1;
                JavaScriptTools.writeStartJavaScript(out);
                out.write("// DeviceAlerts vars\n");
                JavaScriptTools.writeJSVar(out, "ID_ALERT_REFRESH_BTN"      , ID_ALERT_REFRESH_BTN);
                JavaScriptTools.writeJSVar(out, "REFRESH_URL"               , refreshURL);
                JavaScriptTools.writeJSVar(out, "AlertActiveCount"          , _actionAlertCount);
                JavaScriptTools.writeJSVar(out, "AlertRefreshInterval"      , refreshInterval);
                JavaScriptTools.writeJSVar(out, "AlertRefreshCount"         , 0);
                JavaScriptTools.writeJSVar(out, "AlertRefreshTimer"         , null);
                JavaScriptTools.writeJSVar(out, "TEXT_Refresh"              , i18n.getString("DeviceAlerts.refresh","Refresh"));
                JavaScriptTools.writeEndJavaScript(out);

                /* DeviceAlerts.js */
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("DeviceAlerts.js"), request);

            }
        };

        /* Content */
        final Device _selDev       = selDev; // may be null !!!
        final String _selDevID     = selDevID;
        final boolean _allowView   = allowView;
        final boolean _allowClear  = allowClear;
        HTMLOutput HTML_CONTENT    = null;
        HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String selectURL = DeviceAlerts.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());

                // frame header
                String frameTitle = i18n.getString("DeviceAlerts.viewDeviceAlerts","View {0} Alerts", devTitles);
                out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                out.write("<hr>\n");

                // device selection table (Select, Device ID, Description, ...)
                out.write("<form id='"+FORM_DEVICE_SELECT+"' name='"+FORM_DEVICE_SELECT+"' method='post' action='"+selectURL+"' target='_top'>");
                out.write("<span class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"' style='padding-right: 5px'>"+i18n.getString("DeviceAlerts.selectDevice","Select a {0}",devTitles)+":</span>\n");
                out.write("<input type='submit' id='"+ID_ALERT_REFRESH_BTN+"' name='"+PARM_SUBMIT_REFRESH+"' value='"+i18n.getString("DeviceAlerts.refresh","Refresh")+"'>\n");

                // description/help
                out.write("<br>\n");
                out.write("<span class='descriptionHelp'>");
                out.write(i18n.getString("DeviceAlerts.descriptionHelp","(This list shows active alerts for {1} managed by this account)",devTitles));
                out.write("</span>\n");
                //out.write("<br>\n");

                /* device table selection */
                out.write("<div style='margin-top:5px; margin-left:25px;'>\n");
                out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SEL_DEVICE+"'/>\n");
                out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+"' cellspacing=0 cellpadding=0 border=0>\n");
                // table header
                out.write(" <thead>\n");
                out.write("  <tr class='" +CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"' nowrap>"+i18n.getString("DeviceAlerts.select","Select")+"</th>\n");
                out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("DeviceAlerts.deviceID","{0}\nID",devTitles)+"</th>\n");
                out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("DeviceAlerts.description","Description",devTitles)+"</th>\n");
                out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("DeviceAlerts.lastAlert","Last Alert")+"</th>\n");
                out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("DeviceAlerts.alertStatus","Alert Status")+"</th>\n");
                out.write("  </tr>\n");
                out.write(" </thead>\n");
                // table body
                out.write(" <tbody>\n");
                for (int d = 0; d < alertDevList.size(); d++) {
                    String rowClass = ((d & 1) == 0)? 
                        CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD : 
                        CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN;
                    out.write("  <tr class='"+rowClass+"'>\n");
                    Device dev = alertDevList.get(d);
                    long lastNotifyTime = dev.getLastNotifyTime();
                    String deviceID    = DeviceAlerts.this.filter(dev.getDeviceID());
                    String deviceDesc  = DeviceAlerts.this.filter(dev.getDescription());
                    String alertStyle  = DeviceAlerts.this.getAlertStyle(lastNotifyTime,activeAlertAge);
                    String alertTime   = DeviceAlerts.this.filter(reqState.formatDateTime(dev.getLastNotifyTime(),"--"));
                    String alertStatus = DeviceAlerts.this.filter(DeviceAlerts.this.getAlertStatus(dev,privLabel));
                    String checked     = !StringTools.isBlank(_selDevID)? (_selDevID.equals(dev.getDeviceID())?"checked":"") : ((d==0)?"checked":"");
                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+d+"'><input type='radio' name='"+PARM_DEVICE+"' id='"+deviceID+"' value='"+deviceID+"' "+checked+"></td>\n");
                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+deviceID+"'>"+deviceID+"</label></td>\n");
                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+deviceDesc+"</td>\n");
                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' style='"+alertStyle+"' nowrap>"+alertTime+"</td>\n");
                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' style='"+alertStyle+"' nowrap>"+alertStatus+"</td>\n");
                    out.write("  </tr>\n");
                }
                out.write(" </tbody>\n");
                out.write("</table>\n");
                out.write("<table cellpadding='0' cellspacing='0' border='0' style='width:95%; margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                out.write("<tr>\n");

                /* View Map */
                out.write("<td style='padding-left:5px;'>");
                out.write("<input type='submit' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("DeviceAlerts.viewMap","View Map")+"'>");
                out.write("</td>\n"); 
                
                out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                if (_allowClear) {
                    out.write("<input type='submit' name='"+PARM_SUBMIT_CLEAR+"' value='"+
                        i18n.getString("DeviceAlerts.clearAlert","Clear Alert")+"' "+
                        Onclick_ConfirmSelected(locale,i18n.getString("DeviceAlerts.clear","clear"))+">");
                    out.write("&nbsp;");
                    out.write("<input type='submit' name='"+PARM_SUBMIT_CLEAR_ALL+"' value='"+
                        i18n.getString("DeviceAlerts.clearAll","Clear All")+"' "+
                        Onclick_ConfirmOperation(locale,i18n.getString("DeviceAlerts.clearAllItems","clear all items"))+">");
                } else {
                    out.write("&nbsp;");
                }
                out.write("</td>\n"); 
                out.write("</tr>\n");
                out.write("</table>\n");
                out.write("</div>\n");
                
                out.write("</form>\n");

                out.write("<hr>\n");

            }
        };

        /* write frame */
        String onloadAlert = error? JS_alert(true,m) : null;
        String onload = "javascript:deviceAlertOnLoad();";
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // Javascript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }
    
    // ------------------------------------------------------------------------
}
