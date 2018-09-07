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

public class SysAdminDevices
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------
    // Parameters

    // commands
    public  static final String COMMAND_FIND                = "find";

    // submit types
    public  static final String PARM_SUBMIT_FIND            = "s_find";

    // buttons
    public  static final String PARM_BUTTON_BACK            = "s_btnbak";

    // parameters
    public  static final String PARM_MOBILE_ID              = "a_mobileid";     // string

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public SysAdminDevices()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_SYSADMIN_DEVICES);
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
        I18N i18n = privLabel.getI18N(SysAdminDevices.class);
        return super._getMenuDescription(reqState,i18n.getString("SysAdminDevices.editMenuDesc","Find Assigned Unique-IDs"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminDevices.class);
        return super._getMenuHelp(reqState,i18n.getString("SysAdminDevices.editMenuHelp","Find Assigned Unique-IDs"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminDevices.class);
        return super._getNavigationDescription(reqState,i18n.getString("SysAdminDevices.navDesc","Find Unique-IDs"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminDevices.class);
        return i18n.getString("SysAdminDevices.navTab","Find Unique-IDs");
    }

    // ------------------------------------------------------------------------

    /* true if this page iis for the system admin only */
    public boolean systemAdminOnly()
    {
        return true;
    }

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

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel = reqState.getPrivateLabel(); // never null
        final I18N     i18n          = privLabel.getI18N(SysAdminDevices.class);
        final Locale   locale        = reqState.getLocale();
        final Account  currAcct      = reqState.getCurrentAccount(); // never null
        final User     currUser      = reqState.getCurrentUser();
        final boolean  isSysAdmin    = Account.isSystemAdmin(currAcct);
        final String   pageName      = this.getPageName();
        String m = pageMsg;
        boolean error = false;

        /* command */
        String  infoCmd    = reqState.getCommandName();
        boolean findDevice = infoCmd.equals(COMMAND_FIND);
        String  submitFind = AttributeTools.getRequestString(request, PARM_SUBMIT_FIND, "");

        /* mobile id */
        final String mobileID  = AttributeTools.getRequestString(request, PARM_MOBILE_ID  , "");
        final Device devices[] = DCServerFactory.lookupUniqueID(mobileID); // may be null

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = SysAdminDevices.this.getCssDirectory(); // "extra/css"
                WebPageAdaptor.writeCssLink(out, reqState, "SysAdminDevices.css", cssDir);
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
            }
        };

        /* Content */
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName   = SysAdminDevices.this.getPageName();

                // frame header
                String findURL    = SysAdminDevices.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String frameTitle = i18n.getString("SysAdminDevices.findAssignedUniqueID","Find Assigned Unique-IDs");
                out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                out.write("<hr>\n");

                /* start of form */
                out.write("<form name='SystemInfo' method='post' action='"+findURL+"' target='_top'>\n");
                out.write("  <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_FIND+"'/>\n");

                /* System info fields */
                out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                out.println(FormRow_TextField(PARM_MOBILE_ID, true, i18n.getString("SysAdminDevices.findMobileID","Search Mobile-ID")+": ", "", 40, 40));
                out.println("</table>");
                out.println("<input type='submit' name='"+PARM_SUBMIT_FIND+"' value='"+i18n.getString("SysAdminDevices.find","Find")+"'>");
                out.println("<hr>");

                /* list of devices */
                out.println(i18n.getString("SysAdminDevices.matchingDevicesTitle","Devices matching specified Mobile-ID")+":<br>");
                out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                out.println(" <thead>");
                out.println("  <tr class='" +CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                out.println("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL+"' nowrap>"+i18n.getString("SysAdminDevices.accountID","Account")+"</th>\n");
                out.println("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL+"' nowrap>"+i18n.getString("SysAdminDevices.deviceID","Device ID")+"</th>\n");
                out.println("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL+"' nowrap>"+i18n.getString("SysAdminDevices.uniqueID","Unique ID")+"</th>\n");
                out.println("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL+"' nowrap>"+i18n.getString("SysAdminDevices.deviceDesc","Device Description")+"</th>\n");
                out.println("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL+"' nowrap>"+i18n.getString("SysAdminDevices.serverID","Server ID")+"</th>\n");
                out.println("  </tr>");
                out.println(" </thead>");
                out.println(" <tbody>");
                for (int d = 0; d < devices.length; d++) {
                    Device dev = devices[d];
                    String rowClass = ((d & 1) == 0)? 
                        CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD : CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN;
                    out.write("  <tr class='"+rowClass+"'>\n");
                    String accountID    = SysAdminDevices.this.filter(dev.getAccountID());
                    String deviceID     = SysAdminDevices.this.filter(dev.getDeviceID());
                    String uniqueID     = SysAdminDevices.this.filter(dev.getUniqueID());
                    String deviceDesc   = SysAdminDevices.this.filter(dev.getDescription());
                    String serverID     = SysAdminDevices.this.filter(dev.getDeviceCode());
                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL+"' nowrap>"+accountID+"</td>\n");
                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL+"' nowrap>"+deviceID+"</td>\n");
                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL+"' nowrap>"+uniqueID+"</td>\n");
                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL+"' nowrap>"+deviceDesc+"</td>\n");
                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL+"' nowrap>"+serverID+"</td>\n");
                    out.write("  </tr>\n");
                }
                out.println(" </tbody>");
                out.println("</table>");
                if (ListTools.isEmpty(devices)) {
                    out.print(i18n.getString("SysAdminDevices.noDevicesFound","No Devices Found"));
                } else
                if (devices.length == 1) {
                    out.print(i18n.getString("SysAdminDevices.oneDeviceFound","1 Device Found"));
                } else {
                    out.print(i18n.getString("SysAdminDevices.multipleDevicesFound","Multiple Matching Devices Found!"));
                }
                out.println("<br>");

                /* end of form */
                out.write("<hr style='margin-top:7px;margin-bottom:5px;'>\n");
                out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
              //out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("SysAdminDevices.back","Back")+"' onclick=\"javascript:openURL('"+findURL+"','_top');\">\n");
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
