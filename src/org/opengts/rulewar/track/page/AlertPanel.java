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
package org.opengts.rulewar.track.page;

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
import org.opengts.extra.war.track.page.DeviceAlerts;

public class AlertPanel
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------

    //public  static final String _ACL_RULES                  = "rules";
    //public  static final String _ACL_UNIQUEID               = "uniqueID";
    private static final String _ACL_LIST[]                 = null;// new String[] { _ACL_RULES, _ACL_UNIQUEID };

    // ------------------------------------------------------------------------
    // Parameters
    
    /* properties */
    public  static final String PROP_ruleName               = "ruleName";
    public  static final String PROP_pollInterval           = "pollInterval";
    public  static final String PROP_alertText_on           = "alertText.on";
    public  static final String PROP_alertText_off          = "alertText.off";
    public  static final String PROP_actionText_on          = "actionText.on";
    public  static final String PROP_actionText_off         = "actionText.off";
    public  static final String PROP_actionSound_on         = "actionSound.on";
    public  static final String PROP_actionSound_off        = "actionSound.off";
    public  static final String PROP_alertPageName          = "alertPageName";
    public  static final String PROP_bindToParentWindow     = "bindToParentWindow";

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public AlertPanel()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_ALERT_PANEL);
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
        I18N i18n = privLabel.getI18N(AlertPanel.class);
        return super._getMenuDescription(reqState,i18n.getString("AlertPanel.editMenuDesc","Monitor Alerts"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AlertPanel.class);
        return super._getMenuHelp(reqState,i18n.getString("AlertPanel.editMenuHelp","Monitor Alerts"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AlertPanel.class);
        return super._getNavigationDescription(reqState,i18n.getString("AlertPanel.navDesc","Monitor Alerts"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AlertPanel.class);
        return super._getNavigationTab(reqState,i18n.getString("AlertPanel.navTab","Monitor Alerts"));
    }

    // ------------------------------------------------------------------------
    
    public String[] getChildAclList()
    {
        return _ACL_LIST;                                                                             
    }

    // ------------------------------------------------------------------------

    public String getJspURI()
    {
        return super.getJspURI();
    }

    // ------------------------------------------------------------------------

    public String getTarget()
    {
        return "AlertPanel";
    }
    
    public PixelDimension getWindowDimension()
    {
        return new PixelDimension(411,130);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* replace blank strings with html "&nbsp;" */
    private String filter(String s)
    {
        if (StringTools.isBlank(s)) {
            return StringTools.HTML_SP;
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
        final PrivateLabel privLabel     = reqState.getPrivateLabel();
        final I18N         i18n          = privLabel.getI18N(AlertPanel.class);
        final Locale       locale        = reqState.getLocale();
        final String       devTitles[]   = reqState.getDeviceTitles();
        final Account      currAcct      = reqState.getCurrentAccount();
        final User         currUser      = reqState.getCurrentUser();
        final String       pageName      = this.getPageName();
        final String       ruleName      = this.getProperties().getString(PROP_ruleName,"alert");
        final int          pollInterval  = this.getProperties().getInt(PROP_pollInterval,10);
        final boolean      bindToParent  = this.getProperties().getBoolean(PROP_bindToParentWindow,true);

        /* rule exists (selector is defined) */
        final boolean      ruleExists;
        RuleFactory ruleFact = Device.getRuleFactory();
        if (ruleFact == null) {
            ruleExists = false;
        } else {
            String ruleSel = ruleFact.getRuleSelector(currAcct, ruleName);
            ruleExists = !StringTools.isBlank(ruleSel);
        }
        if (!ruleExists) {
            Print.logWarn("AlertPanel rule not yet defined: " + ruleName);
        }

        /* Device Alerts page */
        String alertPageName = this.getProperties().getString(PROP_alertPageName,PAGE_DEVICE_ALERTS);
        final WebPage devAlertPage = privLabel.getWebPage(alertPageName);
        final String devAlertPageURL = (devAlertPage != null)?
            (devAlertPage.encodePageURL(reqState) + "&" + DeviceAlerts.PARM_IS_AUTO_REFRESH + "=false") :
            "(Device Alerts)";
        final String devAlertTitle = (devAlertPage != null)? devAlertPage.getNavigationDescription(reqState) : "";
        if (devAlertPage == null) {
            Print.logInfo("Page '"+PAGE_DEVICE_ALERTS+"' not found");
        }

        /* Strings */
        final String alertOff      = this.getProperties().getString(PROP_alertText_off  ,i18n.getString("AlertPanel.alertOff" , "Alert Monitor"));
        final String alertOn       = this.getProperties().getString(PROP_alertText_on   ,i18n.getString("AlertPanel.alertOn"  , "Alert Detected"));
        final String noAlerts      = this.getProperties().getString(PROP_actionText_off ,i18n.getString("AlertPanel.actionOff", "No Alert Dectected"));
        final String gotoAlerts    = this.getProperties().getString(PROP_actionText_on  ,i18n.getString("AlertPanel.actionOn" , "View {0}", devAlertTitle));
        final String soundURL_on   = this.getProperties().getString(PROP_actionSound_on ,""); 
        final String soundURL_off  = this.getProperties().getString(PROP_actionSound_off,""); 
        final String updateSeconds = i18n.getString("AlertPanel.updateSeconds" , "seconds to update");
        final String pleaseLogin   = i18n.getString("AlertPanel.pleaseLogin"   , "Please Login");
        final String sessionExpire = i18n.getString("AlertPanel.sessionExpired", "Session Expired");
        final String openerClosed  = i18n.getString("AlertPanel.openerClosed"  , "Parent Browser Window Closed");
        final String noRuleDefined = i18n.getString("AlertPanel.ruleNotDefined", "Rule not defined: {0}", ruleName);

        String m = pageMsg;
        boolean error = false;

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = AlertPanel.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "AlertPanel.css", cssDir);
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {

                /* Refresh URL */
                String ruleURL = RequestProperties.TRACK_BASE_URI()+"?" +                     // ./Track?
                    CommonServlet.PARM_PAGE+"="+Track.PAGE_RULE_EVAL+"&" +  // page=RULE_EVAL&
                    Constants.PARM_RULE+"="+ruleName;                       // rule=alert

                /* start JavaScript */
                JavaScriptTools.writeStartJavaScript(out);
                out.write("// AlertPanel vars\n");
                JavaScriptTools.writeJSVar(out, "RULE_NAME"                 , ruleName);
                JavaScriptTools.writeJSVar(out, "RULE_EVAL_URL"             , ruleURL);
                JavaScriptTools.writeJSVar(out, "RuleRefreshInterval"       , pollInterval);
                JavaScriptTools.writeJSVar(out, "RuleRefreshCount"          , 0);
                JavaScriptTools.writeJSVar(out, "RuleRefreshTimer"          , null);
                JavaScriptTools.writeJSVar(out, "BIND_TO_PARENT"            , bindToParent);
                JavaScriptTools.writeJSVar(out, "TEXT_Refresh"              , i18n.getString("AlertPanel.refresh" ,"Refresh"));
                JavaScriptTools.writeJSVar(out, "TEXT_Alert"                , i18n.getString("AlertPanel.alert"   ,"Alert"));
                JavaScriptTools.writeJSVar(out, "TEXT_Alert_Off"            , alertOff);
                JavaScriptTools.writeJSVar(out, "TEXT_Alert_On"             , alertOn);
                JavaScriptTools.writeJSVar(out, "TEXT_No_Alerts"            , noAlerts);
                JavaScriptTools.writeJSVar(out, "TEXT_Goto_Alerts"          , gotoAlerts);
                JavaScriptTools.writeJSVar(out, "TEXT_Seconds_To_Update"    , updateSeconds);
                JavaScriptTools.writeJSVar(out, "TEXT_Please_Login"         , pleaseLogin);
                JavaScriptTools.writeJSVar(out, "TEXT_Session_Expired"      , sessionExpire);
                JavaScriptTools.writeJSVar(out, "TEXT_Opener_Closed"        , openerClosed);
                JavaScriptTools.writeJSVar(out, "URL_Alert_Page"            , devAlertPageURL);
                JavaScriptTools.writeJSVar(out, "SOUND_URL_Off"             , soundURL_off);
                JavaScriptTools.writeJSVar(out, "SOUND_URL_On"              , soundURL_on);
                JavaScriptTools.writeJSVar(out, "SOUND_URL_On_LOOP"         , false);
                JavaScriptTools.writeEndJavaScript(out);

                /* AlertPanel.js */
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("NotifyAlert.js"), request);

            }
        };

        /* Content */
        HTMLOutput HTML_CONTENT = null;
        HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String selectURL = AlertPanel.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                
                String CSS_ALERT_TABLE              = "alertTable";
                String CSS_ALERT_IMAGE_OFF          = "alertImage_off";
                String CSS_ALERT_STATE_TEXT_OFF     = "alertStateText_off";
                String CSS_ALERT_ACTION_TEXT_OFF    = "alertActionText_off";
                String CSS_ALERT_POLL_INTERVAL      = "alertPollInterval";
                String CSS_ALERT_RULE_NAME          = "alertRuleName";

                String ID_ALERT_IMAGE               = "alertImage";
                String ID_ALERT_STATE_TEXT          = "alertStateText";
                String ID_ALERT_ACTION_TEXT         = "alertActionText";
                String ID_ALERT_ACTION_SOUND        = "alertActionSound";
                String ID_ALERT_POLL_INTERVAL       = "alertPollInterval";

                // frame header
                out.println("<span id='"+ID_ALERT_ACTION_SOUND+"'></span>");
                out.println("<table class='"+ CSS_ALERT_TABLE +"'>");
                out.println("<tr>");
                
                out.println("<td id='"+ID_ALERT_IMAGE+"' class='"+ CSS_ALERT_IMAGE_OFF +"'>");
                out.println(  "&nbsp;");
                out.println("</td>");
                
                out.println("<td>");
                out.println( "<table border='0' cellspacing='0' cellpadding='0' height='100%' width='100%' style='padding: 7px 5px 7px 0px;'>");
                
                out.println(   "<tr><td id='"+ID_ALERT_STATE_TEXT+"' class='"+ CSS_ALERT_STATE_TEXT_OFF +"'>");
                out.println(     alertOff + " <span class='"+CSS_ALERT_RULE_NAME+"'>("+ruleName+")</span>");
                out.println(   "</td></tr>");

                out.println(   "<tr><td id='"+ID_ALERT_ACTION_TEXT+"' class='"+ CSS_ALERT_ACTION_TEXT_OFF +"'>");
                out.println(     noAlerts);
                out.println(   "</td></tr>");
                
                out.println(   "<tr><td id='"+ID_ALERT_POLL_INTERVAL+"' class='"+ CSS_ALERT_POLL_INTERVAL +"'>");
                out.println(     pollInterval + " " + updateSeconds);
                out.println(   "</td></tr>");
                
                out.println( "</table>");
                out.println("</td>");
                
                out.println("</tr>");
                out.println("</table>");

            }
        };

        /* write frame */
        String onloadAlert = error? JS_alert(true,m) : null;
        String onload = "javascript:ruleAlertOnLoad();";
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
