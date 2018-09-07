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

import java.util.TimeZone;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.extra.dbtools.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class J1587Display
    extends WebPageAdaptor
    implements Constants
{
    
    // ------------------------------------------------------------------------

    public  static final String PARM_MID              = "mid";
    public  static final String PARM_PID              = "pid";
    public  static final String PARM_SID              = "sid";
    public  static final String PARM_FMI              = "fmi";

    // ------------------------------------------------------------------------

    public J1587Display()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_J1587_SHOW);
        this.setPageNavigation(null);
        this.setLoginRequired(true);
    }

    // ------------------------------------------------------------------------

    public String getPageNavigationHTML(RequestProperties reqState)
    {
        return "";
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

    public String getNavigationDescription(RequestProperties reqState)
    {
        return super._getNavigationDescription(reqState,"");
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        return "";
    }
    
    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState, 
        final String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n = privLabel.getI18N(J1587Display.class);
        HttpServletRequest request = reqState.getHttpServletRequest();
        HttpServletResponse response = reqState.getHttpServletResponse();

        /* mid/pid/sid/fmi */
        int mid = StringTools.parseInt(AttributeTools.getRequestAttribute(request,PARM_MID,"0"),0);
        int pid = StringTools.parseInt(AttributeTools.getRequestAttribute(request,PARM_PID,"0"),0);
        int sid = StringTools.parseInt(AttributeTools.getRequestAttribute(request,PARM_SID,"0"),0);
        int fmi = StringTools.parseInt(AttributeTools.getRequestAttribute(request,PARM_FMI,"0"),0);

        /* descriptions */
        String midDesc = J1587.getMIDDescription(mid);
        String pidDesc = (pid > 0)? J1587.getPIDDescription(pid) : null;
        String sidDesc = (sid > 0)? J1587.getSIDDescription(mid,sid) : null;
        String fmiDesc = J1587.getFMIDescription(fmi);

        /* write frame */
        response.setContentType(HTMLTools.MIME_HTML());
        PrintWriter out = response.getWriter();

        // HTML start
        out.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n");
        out.write("<html xmlns='http://www.w3.org/1999/xhtml' xmlns:v='urn:schemas-microsoft-com:vml'>\n");
        
        // HTML head
        out.write("\n");
        out.write("<head>\n");
        out.write("  <meta http-equiv='content-type' content='text/html; charset=UTF-8'/>\n");
        out.write("  <meta http-equiv='cache-control' content='no-cache'/>\n");
        out.write("  <meta http-equiv='expires' content='0'/>\n"); // expires 'now'
        out.write("  <meta name='copyright' content='" + privLabel.getCopyright() + "'/>\n");
        out.write("  <meta name='robots' content='none'/>\n");
        out.write("  <title>" + privLabel.getPageTitle() + "</title>\n");
        out.write("</head>\n");
        out.write("\n");
        
        // HTML Body
        //   MID: 128 Engine #1
        //   PID: 43  Ignition Switch Status
        //   FMI: 3   Voltage above normal range
        out.write("<body>\n"); //  onBlur="window.close()">
        out.write("<table>\n");
        if (midDesc != null) { out.write("  <tr><td>MID:</td><td>"+mid+"</td><td>"+midDesc+"</td></tr>\n"); }
        if (pidDesc != null) { out.write("  <tr><td>PID:</td><td>"+pid+"</td><td>"+pidDesc+"</td></tr>\n"); }
        if (sidDesc != null) { out.write("  <tr><td>SID:</td><td>"+sid+"</td><td>"+sidDesc+"</td></tr>\n"); }
        if (fmiDesc != null) { out.write("  <tr><td>FMI:</td><td>"+fmi+"</td><td>"+fmiDesc+"</td></tr>\n"); }
        out.write("</table>\n");
        out.write("</body>\n");
        
        // HTML end
        out.write("</html>\n");
        out.close();

    }

    // ------------------------------------------------------------------------

}
