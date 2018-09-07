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
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class Offline
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------

    public  static final String  CSS_OFFLINE[]  = new String[] { "offlineTable", "offlineCell" };

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public Offline()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_OFFLINE);
        this.setPageNavigation(new String[] { PAGE_LOGIN });
        this.setLoginRequired(false);
    }

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return "";
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        return super._getMenuDescription(reqState,"");
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        return super._getMenuHelp(reqState,"");
    }

    // ------------------------------------------------------------------------

    public String getNavigationTab(RequestProperties reqState)
    {
        return "";
    }
        
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n = privLabel.getI18N(Offline.class);

        /* default message */
        String m = pageMsg;
        if (StringTools.isBlank(m)) {
            m = i18n.getString("Offline.message","The system is currently offline for maintenance\nPlease check back later.");
        }
        
        /* HTML filtering */
        m = StringTools.trim(m); // remove leading/trailing spaces
        if (m.startsWith("<") || (m.toUpperCase().indexOf("<BR>") >= 0)) {
            // use message as-is
        } else {
            // html encode message (newlines converted to "<BR>")
            m = StringTools.htmlFilterText(m);
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = Offline.this.getCssDirectory(); 
                WebPageAdaptor.writeCssLink(out, reqState, "Offline.css", cssDir);
            }
        };

        /* write frame */
        final String motd = m;
        HTMLOutput HTML_CONTENT = new HTMLOutput(CSS_OFFLINE, "") {
            public void write(PrintWriter out) throws IOException {
                out.write("<div style='font-size:10pt; padding-bottom:10px;'>");
                out.write(motd);
                out.write("</div>");
                out.write("<hr>\n");
                //String loginURL = EncodeMakeURL(reqState, RequestProperties.TRACK_BASE_URI(), PAGE_LOGIN);
                String loginURL = privLabel.getWebPageURL(reqState, PAGE_LOGIN);
                if (!StringTools.isBlank(loginURL)) {
                    out.write("<a href='"+loginURL+"'>"+i18n.getString("Offline.login","Login")+"</a>\n");
                }
            }
        };

        /* write frame */
        String onload = null;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTMLOutput.NOOP,            // JavaScript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

}
