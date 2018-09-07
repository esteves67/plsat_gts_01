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

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.Version;
import org.opengts.db.*;
import org.opengts.db.AclEntry.AccessLevel;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class SysAdminInfo
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------
    // Parameters

    // commands
    public  static final String COMMAND_INFO_UPDATE         = "update";

    // submit types
    public  static final String PARM_SUBMIT_CHANGE          = "s_subchg";

    // buttons
    public  static final String PARM_BUTTON_CANCEL          = "s_btncan";
    public  static final String PARM_BUTTON_BACK            = "s_btnbak";

    // parameters
    public  static final String PARM_GTS_ID                 = "a_gtsid";        // string
    public  static final String PARM_CONTEXT                = "a_context";      // string
    public  static final String PARM_GTS_VERSION            = "a_version";      // string
    public  static final String PARM_START_TIME             = "a_start";        // string
    public  static final String PARM_ACCT_COUNT             = "a_actcnt";       // string
    public  static final String PARM_DEV_COUNT              = "a_devcnt";       // string
    public  static final String PARM_EVENT_COUNT            = "a_evtcnt";       // string
    public  static final String PARM_EV_PER_SEC             = "a_evpersec";     // string
    public  static final String PARM_SESS_COUNT             = "a_sesscnt";      // string
    public  static final String PARM_SYS_ONLINE             = "a_online";       // boolean
    public  static final String PARM_OFFLINE_MSG            = "a_oflmsg";       // string

    // CSS class names
    private static final String CSS_CLASS_ONLINE_SELECTION  = "onlineSelection";
    private static final String CSS_CLASS_OFFLINE_MESSAGE   = "offlineMessage";

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public SysAdminInfo()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_SYSADMIN_INFO);
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
        I18N i18n = privLabel.getI18N(SysAdminInfo.class);
        return super._getMenuDescription(reqState,i18n.getString("SysAdminInfo.editMenuDesc","System Info"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminInfo.class);
        return super._getMenuHelp(reqState,i18n.getString("SysAdminInfo.editMenuHelp","View/Edit System Info"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminInfo.class);
        return super._getNavigationDescription(reqState,i18n.getString("SysAdminInfo.navDesc","System Info"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminInfo.class);
        return i18n.getString("SysAdminInfo.navTab","System Info");
    }

    // ------------------------------------------------------------------------

    /* true if this page iis for the system admin only */
    public boolean systemAdminOnly()
    {
        return true;
    }

    // ------------------------------------------------------------------------

    /* returns the total number of database records for the specific table */
    private <T extends DBRecord> String getTotalRecordCountString(DBFactory<T> fact, PrivateLabel bpl, String key)
    {
        if (fact == null) {
            return "?";
        } else
        if ((bpl != null) && !StringTools.isBlank(key) && !bpl.getBooleanProperty(key,true)) {
            // not available
            return ""; // "n/a";
        } else {
            try {
                return String.valueOf(DBRecord.getRecordCount(fact));
            } catch (DBException dbe) {
                Print.logException("Retrieving record count for table: " + fact.getUntranslatedTableName(), dbe);
                return "error";
            }
        }
    }

    /* returns the calculated events-per-second over the specified time period */
    private String getEventsPerSecondString(long deltaSec, PrivateLabel bpl)
    {
        String key = PrivateLabel.PROP_SysAdminInfo_showEventsPerSecond;
        if (deltaSec <= 0L) {
            // value shold actually be '0'
            return "?";
        } else
        if ((bpl != null) && !StringTools.isBlank(key) && !bpl.getBooleanProperty(key,false)) {
            // not available
            return ""; // "n/a";
        } else {
            // calculate
            try {
                long nowTime = DateTime.getCurrentTimeSec();
                long count = EventData.getRecordCount(
                    null, null,
                    nowTime - deltaSec, nowTime);
                return StringTools.format(((double)count/(double)deltaSec), "0.000");
            } catch (DBException dbe) {
                Print.logException("Retrieving record count for EventData table", dbe);
                return "error";
            }
        }
    }

    // ------------------------------------------------------------------------

    private int getSessionCount(RequestProperties reqState)
    {
        HttpSession session = AttributeTools.getSession(reqState.getHttpServletRequest());
        if (session != null) {
            int count = RTConfigContextListener.GetSessionCount(session.getServletContext(),
                new RTConfigContextListener.HttpSessionFilter() {
                    public boolean countSession(HttpSession session) {
                        String acctID = (String)AttributeTools.getSessionAttribute(session,Constants.PARM_ACCOUNT,null);
                        if (!StringTools.isBlank(acctID)) {
                            String userID = (String)AttributeTools.getSessionAttribute(session,Constants.PARM_USER,null);
                            Print.logInfo("Logged-in User: %s,%s", acctID, userID);
                            return true;
                        }
                        return false;
                    }
                }
            );
            return count;
        } else {
            return -1;
        }
    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel = reqState.getPrivateLabel(); // never null
        final I18N     i18n          = privLabel.getI18N(SysAdminInfo.class);
        final Locale   locale        = reqState.getLocale();
        final Account  currAcct      = reqState.getCurrentAccount(); // never null
        final User     currUser      = reqState.getCurrentUser();
        final boolean  isSysAdmin    = Account.isSystemAdmin(currAcct);
        final String   pageName      = this.getPageName();
        String m = pageMsg;
        boolean error = false;

        /* command */
        String infoCmd = reqState.getCommandName();
        boolean updateInfo = infoCmd.equals(COMMAND_INFO_UPDATE);

        /* submit buttons */
        String  submitChange = AttributeTools.getRequestString(request, PARM_SUBMIT_CHANGE, "");

        /* ACL allow edit/view */
        boolean allowEdit = isSysAdmin && (privLabel.hasWriteAccess(currUser, this.getAclName()));

        /* sub-command */
        if (updateInfo) {
            if (!allowEdit) {
                // not authorized to update
                updateInfo = false;
            } else
            if (!SubmitMatch(submitChange,i18n.getString("SysAdminInfo.change","Change"))) {
                updateInfo = false;
            }
        }

        /* change/update the account info? */
        if (updateInfo) {
            String sysOnline = AttributeTools.getRequestString(request, PARM_SYS_ONLINE, "");
            // update
            try {
                boolean saveOK = true;
                // system online
                if (StringTools.isBoolean(sysOnline,true/*strict*/)) {
                    boolean online = StringTools.parseBoolean(sysOnline,true/*default*/);
                    String offlineMsg = online? null : AttributeTools.getRequestString(request, PARM_OFFLINE_MSG, "");
                    PrivateLabel.SetContextOfflineMessage(offlineMsg);
                }
                // save
                if (saveOK) {
                    m = i18n.getString("SysAdminInfo.infoUpdated","System information updated");
                }
            } catch (Throwable t) {
                Print.logException("Updating System Information", t);
                m = i18n.getString("SysAdminInfo.errorUpdate","Internal error updating System Information");
                error = true;
            }
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = SysAdminInfo.this.getCssDirectory(); // "extra/css"
                WebPageAdaptor.writeCssLink(out, reqState, "SysAdminInfo.css", cssDir);
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                JavaScriptTools.writeStartJavaScript(out);
                out.println("function _offlineEnableMessage() {");
                out.println("  var selElem = document.getElementById('"+CSS_CLASS_ONLINE_SELECTION+"');");
                out.println("  var msgElem = document.getElementById('"+CSS_CLASS_OFFLINE_MESSAGE+"');");
                out.println("  if ((selElem != null) && (msgElem != null)) {");
                out.println("     if (selElem.value == 'true') {");
              //out.println("        msgElem.readOnly = true;");  // online, disable offline text message
                out.println("        msgElem.setAttribute('readOnly','readonly');"); // online
                out.println("        msgElem.className = '"+CommonServlet.CSS_TEXT_READONLY+"';");
                out.println("     } else {");
              //out.println("        msgElem.readOnly = false;");  // offline, enable offline text message
                out.println("        msgElem.removeAttribute('readOnly');"); // offline
                out.println("        msgElem.className = '"+CommonServlet.CSS_TEXT_INPUT+"';");
                out.println("     }");
                out.println("  }");
              //out.println("  alert('Offline setting changed! ' + selElem.value + ' [' + msgElem);");
                out.println("};");
                JavaScriptTools.writeEndJavaScript(out);
            }
        };

        /* Content */
        final boolean _allowEdit = allowEdit;
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName   = SysAdminInfo.this.getPageName();

                // frame header
                String editURL    = SysAdminInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String frameTitle = _allowEdit? 
                    i18n.getString("SysAdminInfo.editInfo","Edit/View System Information") : 
                    i18n.getString("SysAdminInfo.viewInfo","View System Information");
                out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                out.write("<hr>\n");

                /* start of form */
                out.write("<form name='SystemInfo' method='post' action='"+editURL+"' target='_top'>\n");
                out.write("  <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPDATE+"'/>\n");

                /* System info fields */
                // Current logged-in users
                // Last event time
                // Last login time
                TimeZone tmz       = currAcct.getTimeZone(null);
                String gtsID       = DBConfig.getServiceAccountID("unknown");
                String gtsVersion  = DBConfig.getVersion() + " (" + (new DateTime(Version.getCompileTimestamp(),tmz)).format("yyyy/MM/dd HH:mm:ss z") + ")";
                String contextURL  = request.getRequestURL().toString();
                String startupTime = (new DateTime(Track.GetInitializedTime(),tmz)).toString();
                String acctCount   = SysAdminInfo.this.getTotalRecordCountString(Account.getFactory()  ,privLabel, null);
                String devCount    = SysAdminInfo.this.getTotalRecordCountString(Device.getFactory()   ,privLabel, null);
                String eventCount  = SysAdminInfo.this.getTotalRecordCountString(EventData.getFactory(),privLabel, PrivateLabel.PROP_SysAdminInfo_showEventCount);
                String eventPerSec = SysAdminInfo.this.getEventsPerSecondString( DateTime.DaySeconds(1),privLabel);
                int    sessCount   = SysAdminInfo.this.getSessionCount(reqState);
                String sessCntStr  = (sessCount >= 1)? String.valueOf(sessCount) : "?";
                String ctxOffline  = PrivateLabel.GetContextOfflineMessage();
                String glbOffline  = PrivateLabel.GetGlobalOfflineMessage();
                boolean isOffline  = (ctxOffline != null) || (glbOffline != null);
                ComboOption ctxOpt = ComboOption.getYesNoOption(locale,!isOffline);
                out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                out.println(FormRow_TextField(PARM_GTS_ID     , false     , i18n.getString("SysAdminInfo.gtsID","GTS Installation ID")+":"             , gtsID      , 20, 20)); // read-only
                out.println(FormRow_TextField(PARM_CONTEXT    , false     , i18n.getString("SysAdminInfo.contextURL","Context URL")+":"                , contextURL , 55, 70)); // read-only
                out.println(FormRow_TextField(PARM_GTS_VERSION, false     , i18n.getString("SysAdminInfo.gtsVersion","Context GTS Version")+":"        , gtsVersion , 40, 40)); // read-only
                out.println(FormRow_TextField(PARM_START_TIME , false     , i18n.getString("SysAdminInfo.startTime","Context Service Start Time")+":"  , startupTime, 32, 32)); // read-only
                out.println(FormRow_TextField(PARM_ACCT_COUNT , false     , i18n.getString("SysAdminInfo.accountCount","Number of Accounts")+":"       , acctCount  ,  8, 12)); // read-only
                out.println(FormRow_TextField(PARM_DEV_COUNT  , false     , i18n.getString("SysAdminInfo.deviceCount","Number of Devices")+":"         , devCount   ,  8, 12)); // read-only
                if (!StringTools.isBlank(eventCount)) {
                out.println(FormRow_TextField(PARM_EVENT_COUNT, false     , i18n.getString("SysAdminInfo.eventCount","Number of Events")+":"           , eventCount , 12, 14)); // read-only
                }
                if (!StringTools.isBlank(eventPerSec)) {
                out.println(FormRow_TextField(PARM_EV_PER_SEC , false     , i18n.getString("SysAdminInfo.eventsPerSec","Events Per Second (24hrs)")+":", eventPerSec,  6, 12)); // read-only
                }
                out.println(FormRow_TextField(PARM_SESS_COUNT , false     , i18n.getString("SysAdminInfo.sessionCount","Users Currently Logged-In")+":", sessCntStr ,  5, 12)); // read-only
                out.println("</table>");
                out.println("<hr>");

                /* offline/online */
                out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                if (glbOffline == null) {
                    String onchange = "javascript:_offlineEnableMessage();";
                    String selectID = CSS_CLASS_ONLINE_SELECTION;
                    String oflMsgID = CSS_CLASS_OFFLINE_MESSAGE;
                    String msg = StringTools.trim(ctxOffline);
                    out.println(FormRow_ComboBox(selectID, PARM_SYS_ONLINE , true  , i18n.getString("SysAdminInfo.contextOnline","System Online")+":"    , ctxOpt  , 
                        ComboMap.getYesNoMap(locale), onchange, -1, i18n.getString("SysAdminInfo.contextOfflineComment","(Setting this to 'No' will disable login for this context)")));
                    out.println(FormRow_TextArea(oflMsgID, PARM_OFFLINE_MSG, isOffline, i18n.getString("SysAdminInfo.contextOfflineMessage","Offline Message")+":", msg, 4, 70));
                    URIArg adminURL = (new URIArg(contextURL)).addArg(Constants.PARM_ACCOUNT,RTConfig.getString(DBConfig.PROP_sysAdmin_account));
                    out.println(FormRow_TextNote(i18n.getString("SysAdminInfo.contextOfflineNote","Important Note")+":",
                        StringTools.replace(i18n.getString("SysAdminInfo.contextOfflineInstructions",
                            "After setting this login context '<B>OFFLINE</B>' and then logging-out, the only way to log back in <BR>" +
                            "and set the system back '<B>ONLINE</B>' is for the System Administrator to log-in using this URL:<BR>" +
                            "&nbsp;<span style='color:blue;'>{0}</span>", 
                            adminURL.toString()),"\n","<BR>")));
                } else {
                    // This may never be displayed if 'Track.java' does not allow the 'sysadmin' to log-in during global offline.
                    out.println(FormRow_ComboBox (PARM_SYS_ONLINE , false , i18n.getString("SysAdminInfo.globalOnline","System Online")+":"    , ctxOpt    , 
                        ComboMap.getYesNoMap(locale), ""/*onchange*/, -1, i18n.getString("SysAdminInfo.globalOfflineComment","(Global offline is in effect)")));
                    out.println(FormRow_TextArea( PARM_OFFLINE_MSG, false , i18n.getString("SysAdminInfo.globalOfflineMessage","Offline Message")+":" , glbOffline, 4, 70));
                    out.println(FormRow_TextNote(i18n.getString("SysAdminInfo.globalOfflineNote","Note")+":",
                        StringTools.replace(i18n.getString("SysAdminInfo.globalOfflineInstructions",
                            "Global '<B>OFFLINE</B>' is in effect.<BR>" +
                            "To re-enable Global '<B>ONLINE</B>' will require command-line removal of the file<BR>" +
                            "&nbsp;<span style='color:blue;'>{0}</span>", 
                            RTConfig.getString(DBConfig.PROP_track_offlineFile,"?")),"\n","<BR>")));
                }
                out.println("</table>");

                /* end of form */
                out.write("<hr style='margin-bottom:5px;'>\n");
                out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                if (_allowEdit) {
                    out.write("<input type='submit' name='"+PARM_SUBMIT_CHANGE+"' value='"+i18n.getString("SysAdminInfo.change","Change")+"'>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    //out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("SysAdminInfo.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                } else {
                    //out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("SysAdminInfo.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                }
                out.write("</form>\n");

            }
        };

        /* write frame */
        String onload = error? JS_alert(true,m) : null;
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
