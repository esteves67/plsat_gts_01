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
import org.opengts.db.dmtp.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class LAFInfo
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------
    // Parameters

    // forms 
    public  static final String FORM_LAF_SELECT             = "LAFInfoSelect";
    public  static final String FORM_LAF_EDIT               = "LAFInfoEdit";
    public  static final String FORM_LAF_NEW                = "LAFInfoNew";

    // commands
    public  static final String COMMAND_INFO_UPD_LAF        = "updateLAF";
    public  static final String COMMAND_INFO_SELECT         = "selectLAF";
    public  static final String COMMAND_INFO_NEW            = "new";

    // submit
    public  static final String PARM_SUBMIT_EDIT            = "g_subedit";
    public  static final String PARM_SUBMIT_VIEW            = "g_subview";
    public  static final String PARM_SUBMIT_CHG             = "g_subchg";
    public  static final String PARM_SUBMIT_DEL             = "g_subdel";
    public  static final String PARM_SUBMIT_NEW             = "g_subnew";

    // buttons
    public  static final String PARM_BUTTON_CANCEL          = "g_btncan";
    public  static final String PARM_BUTTON_BACK            = "g_btnbak";

    // parameters
    public  static final String PARM_NEW_NAME               = "l_newname";
    public  static final String PARM_LAF_SELECT             = "l_lafid";
    public  static final String PARM_HOST_ID                = "l_host";
    public  static final String PARM_DESCRIPTION            = "l_desc";
    public  static final String PARM_PAGETITLE              = "l_title";
    public  static final String PARM_COPYRIGHT              = "l_cpyrght";
    public  static final String PARM_JSPENTRY_DEFAULT       = "j_jspdft";
    public  static final String PARM_BANNER_STYLE           = "b_style";
    public  static final String PARM_BANNER_WIDTH           = "b_width";
    public  static final String PARM_BANNER_IMG_SOURCE      = "b_imgsrc";
    public  static final String PARM_BANNER_IMG_WIDTH       = "b_imgwid";
    public  static final String PARM_BANNER_IMG_HEIGHT      = "b_imghght";
    public  static final String PARM_BANNER_IMG_ANCHOR      = "b_imganch";
    public  static final String PARM_BACKGROUND_COLOR       = "f_bgcolor";
    public  static final String PARM_BACKGROUND_IMAGE       = "f_bgimage";
    public  static final String PARM_BACKGROUND_POSITION    = "f_bgposit";
    public  static final String PARM_BACKGROUND_REPEAT      = "f_bgrepet";
    public  static final String PARM_CONTENTCELL_COLOR      = "f_cccolor";
    public  static final String PARM_ICONMENU_TITLE_COLOR   = "f_imcolor";

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public LAFInfo()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_LAF_INFO);
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
        I18N i18n = privLabel.getI18N(LAFInfo.class);
        return super._getMenuDescription(reqState,i18n.getString("LAFInfo.editMenuDesc","View/Edit Look-and-Feel Information"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(LAFInfo.class);
        return super._getMenuHelp(reqState,i18n.getString("LAFInfo.editMenuHelp","View and Edit Look-and-Feel information"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(LAFInfo.class);
        return super._getNavigationDescription(reqState,i18n.getString("LAFInfo.navDesc","Look-and-Feel Admin"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(LAFInfo.class);
        return super._getNavigationTab(reqState,i18n.getString("LAFInfo.navTab","Look-and-Feel Admin"));
    }

    // ------------------------------------------------------------------------

    /* true if this page iis for the system admin only */
    public boolean systemAdminOnly()
    {
        return true;
    }

    // ------------------------------------------------------------------------
    
    /* Gets a RTProperties instance for the specified Resource */
    private static RTProperties GetResourceProperties(Resource res)
    {
        RTProperties rtProps = new RTProperties();
        if (res != null) {
            String resProps = res.getProperties();
            if (resProps != null) {
                rtProps.setProperties(resProps, false);
            }
            byte resValue[] = res.getValue();
            if (resValue.length > 0) {
                rtProps.setProperties(StringTools.toStringValue(resValue), false);
            }
        }
        return rtProps;
    }
    
    /* Sets the specidied property value */
    private static void SetLafProperty(RTProperties rtProps, String key, String val)
    {
        if ((rtProps != null) && !StringTools.isBlank(key)) {
            if (StringTools.isBlank(val)) {
                rtProps.removeProperty(key);
            } else {
                rtProps.setString(key, val);
            }
        }
    }
    
    /* Returns the hostname portion of the specified resource ID */
    private static String FilterHostID(String resourceID)
    {
        String resID = StringTools.trim(resourceID).toLowerCase();
        String hostID = resID.startsWith(Resource.RESID_PrivateLabel_Properties_)? 
            resID.substring(Resource.RESID_PrivateLabel_Properties_.length()) : resID;
        return FilterText(hostID);
    }
    
    /* returns true if the specified host name is valud */
    private static boolean IsValidHostName(String host)
    {
        if (StringTools.isBlank(host)) {
            return false; // cannot be blank
        } else
        if (host.startsWith(".") || host.endsWith(".")) {
            return false; // cannot start/end with '.'
        } else {
            for (int i = 0; i < host.length(); i++) {
                char ch = host.charAt(i);
                if (Character.isLetterOrDigit(ch)) {
                    continue; // a..z,A..Z,0..9 ok
                } else
                if ((ch == '.') || (ch == '_') || (ch == '-')) {
                    continue; // ._- ok
                } else {
                    return false;
                }
            }
            return true;
        }
    }
    
    /* write the LAF admin page */
    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel   = reqState.getPrivateLabel();
        final I18N         i18n        = privLabel.getI18N(LAFInfo.class);
        final Locale       locale      = reqState.getLocale();
        final Account      currAcct    = reqState.getCurrentAccount(); // never null
        final User         currUser    = reqState.getCurrentUser(); // may be null
        final String       accountID   = currAcct.getAccountID(); // should be "sysadmin"
        final String       pageName    = this.getPageName();
        String m = pageMsg;
        boolean error = false;

        /* argument resource-id */
        OrderedSet<String> resourceList = null;
        try {
            resourceList = Resource.getResourcesForAccount(accountID,Resource.RESID_PrivateLabel_Properties_);
        } catch (DBException dbe) {
            resourceList = new OrderedSet<String>();
        }
        String selResourceID = AttributeTools.getRequestString(reqState.getHttpServletRequest(), PARM_LAF_SELECT, "");
        if (StringTools.isBlank(selResourceID) && (resourceList.size() > 0)) {
            selResourceID = resourceList.get(0);
        }
        //Print.logInfo("Selected ResourceID: " + selResourceID);

        /* authorized selected resource? */
        if (!StringTools.isBlank(selResourceID) && !resourceList.contains(selResourceID)) {
            selResourceID = "";
        }

        /* Resource db */
        Resource selResource = null;
        try {
            selResource = !StringTools.isBlank(selResourceID)? Resource.getResource(currAcct,selResourceID) : null; // may still be null
        } catch (DBException dbe) {
            // ignore
        }

        /* ACL allow view/new/delete */
        boolean allowNew     = privLabel.hasAllAccess(currUser, this.getAclName());
        boolean allowDelete  = allowNew;
        boolean allowEdit    = allowNew  || privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView    = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());

        /* submit buttons */
        String  submitEdit   = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT, "");
        String  submitView   = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW, "");
        String  submitChange = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG , "");
        String  submitNew    = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW , "");
        String  submitDelete = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL , "");

        /* command */
        String  resourceCmd  = reqState.getCommandName();
        boolean selectLAF    = resourceCmd.equals(COMMAND_INFO_SELECT);
        boolean newLAF       = resourceCmd.equals(COMMAND_INFO_NEW);
        boolean updateLAF    = resourceCmd.equals(COMMAND_INFO_UPD_LAF);
        boolean deleteLAF    = false;

        /* ui display */
        boolean uiList       = false;
        boolean uiEdit       = false;
        boolean uiView       = false;

        /* pre-qualify commands */
        String newResourceID = null;
        String newHostURL    = null;
        if (newLAF) {
            if (!allowNew) {
                newLAF = false; // not authorized
            } else {
                HttpServletRequest httpReq = reqState.getHttpServletRequest();
                String hostURL = AttributeTools.getRequestString(httpReq,PARM_NEW_NAME,"").trim().toLowerCase();
                int p = hostURL.indexOf("/");
                String hostID = (p >= 0)? hostURL.substring(0,p).trim() : hostURL;
                String path   = (p >= 0)? hostURL.substring(p).trim()   : "";
                if (StringTools.isBlank(hostID)) {
                    m = i18n.getString("LAFInfo.enterNewHostname","Please enter a new host name.");
                    error  = true;
                    newLAF = false;
                } else
                if (!LAFInfo.IsValidHostName(hostID)) {
                    m = i18n.getString("LAFInfo.invalidHostID","Host ID contains invalid characters");
                    error  = true;
                    newLAF = false;
                } else {
                    newHostURL    = hostID + path;
                    newResourceID = Resource.RESID_PrivateLabel_Properties_ + newHostURL;
                }
            }
        } else
        if (updateLAF) {
            if (!allowEdit) {
                updateLAF = false; // not authorized
            } else
            if (!SubmitMatch(submitChange,i18n.getString("LAFInfo.change","Change"))) {
                updateLAF = false;
            } else
            if (selResource == null) {
                // should not occur
                Print.logError("Resource ID not found: " + accountID + "/" + selResourceID);
                m = i18n.getString("LAFInfo.unableToUpdate","Unable to update entry, ID not found");
                error = true;
                updateLAF = false;
            }
        } else
        if (selectLAF) {
            if (SubmitMatch(submitDelete,i18n.getString("LAFInfo.delete","Delete"))) {
                if (!allowDelete) {
                    deleteLAF = false; // not authorized
                } else
                if (selResource == null) {
                    m = i18n.getString("LAFInfo.pleaseSelectEntry","Please select an entry");
                    error = true;
                    deleteLAF = false; // not selected
                } else {
                    deleteLAF = true;
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("LAFInfo.edit","Edit"))) {
                if (!allowEdit) {
                    uiEdit = false; // not authorized
                } else
                if (selResource == null) {
                    m = i18n.getString("LAFInfo.pleaseSelectEntry","Please select an entry");
                    error = true;
                    uiEdit = false; // not selected
                } else {
                    uiEdit = true;
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("LAFInfo.view","View"))) {
                if (!allowView) {
                    uiView = false; // not authorized
                } else
                if (selResource == null) {
                    m = i18n.getString("LAFInfo.pleaseSelectEntry","Please select an entry");
                    error = true;
                    uiView = false; // not selected
                } else {
                    uiView = true;
                }
            }
        }

        /* delete Resource? */
        if (deleteLAF) {
            // 'selResource' guaranteed non-null here
            try {
                Resource.Key resourceKey = (Resource.Key)selResource.getRecordKey();
                Print.logWarn("Deleting Resource: " + resourceKey);
                resourceKey.delete(true); // will also delete dependencies
                selResourceID = "";
                selResource = null;
                // select another resource
                resourceList = Resource.getResourcesForAccount(accountID,Resource.RESID_PrivateLabel_Properties_);
                if (!ListTools.isEmpty(resourceList)) {
                    selResourceID = resourceList.get(0);
                    try {
                        selResource = !selResourceID.equals("")? Resource.getResource(currAcct, selResourceID) : null; // may still be null
                    } catch (DBException dbe) {
                        // ignore
                    }
                }
            } catch (DBException dbe) {
                Print.logException("Deleting Resource", dbe);
                m = i18n.getString("LAFInfo.errorDelete","Internal error deleting Resource");
                error = true;
            }
            uiList = true;
        }

        /* update the device info? */
        if (newLAF) {
            boolean createResourceOK = true;
            for (int u = 0; u < resourceList.size(); u++) {
                if (newResourceID.equalsIgnoreCase(resourceList.get(u))) {
                    m = i18n.getString("LAFInfo.alreadyExists","This Host ID already exists");
                    error = true;
                    createResourceOK = false;
                    break;
                }
            }
            if (createResourceOK) {
                try {
                    Resource resource = Resource.createNewResource(currAcct, newResourceID);
                    resource.setDescription(i18n.getString("LAFInfo.resourceDescription","Host Look-and-Feel"));
                    resource.setType(Resource.TYPE_RTPROPS);
                    resource.save();
                    resourceList  = Resource.getResourcesForAccount(accountID,Resource.RESID_PrivateLabel_Properties_);
                    selResource   = resource;
                    selResourceID = resource.getResourceID();
                    //Print.logInfo("Created Resource '%s'", selResourceID);
                    m = i18n.getString("LAFInfo.createdResource","New Resource has been created");
                } catch (DBException dbe) {
                    Print.logException("Creating Resource", dbe);
                    m = i18n.getString("LAFInfo.errorCreate","Internal error creating Resource");
                    error = true;
                }
            }
            uiList = true;
        }

        /* update the Resource info? */
        if (updateLAF) {
            // 'selResource' guaranteed non-null here
            selResource.clearChanged();
            try {
                boolean saveOK = true;
                // description
                String description            = AttributeTools.getRequestString(request, PARM_DESCRIPTION         , "");
                String propPageTitle          = AttributeTools.getRequestString(request, PARM_PAGETITLE           , "");
                String propCopyright          = AttributeTools.getRequestString(request, PARM_COPYRIGHT           , "");
                String propJSPEntryDefault    = AttributeTools.getRequestString(request, PARM_JSPENTRY_DEFAULT    , "");
                String propBannerWidth        = AttributeTools.getRequestString(request, PARM_BANNER_WIDTH        , "");
                String propBannerStyle        = AttributeTools.getRequestString(request, PARM_BANNER_STYLE        , "");
                String propBannerImageSource  = AttributeTools.getRequestString(request, PARM_BANNER_IMG_SOURCE   , "");
                String propBannerImageWidth   = AttributeTools.getRequestString(request, PARM_BANNER_IMG_WIDTH    , "");
                String propBannerImageHeight  = AttributeTools.getRequestString(request, PARM_BANNER_IMG_HEIGHT   , "");
                String propBannerAnchorLink   = AttributeTools.getRequestString(request, PARM_BANNER_IMG_ANCHOR   , "");
                String propBackgroundColor    = AttributeTools.getRequestString(request, PARM_BACKGROUND_COLOR    , "");
                String propBackgroundImage    = AttributeTools.getRequestString(request, PARM_BACKGROUND_IMAGE    , "");
                String propContentCellColor   = AttributeTools.getRequestString(request, PARM_CONTENTCELL_COLOR   , "");
                String propIconMenuTitleColor = AttributeTools.getRequestString(request, PARM_ICONMENU_TITLE_COLOR, "");
              //String propBackgroundPosition = AttributeTools.getRequestString(request, PARM_BACKGROUND_POSITION , "");
              //String propBackgroundRepeat   = AttributeTools.getRequestString(request, PARM_BACKGROUND_REPEAT   , "");
                if (StringTools.isBlank(propJSPEntryDefault)) {
                    propJSPEntryDefault = "default";
                }
                // RTProperties
                RTProperties rtProps = new RTProperties();
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_PageTitle                     , propPageTitle);
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_Copyright                     , propCopyright);
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_JSPEntry_Default              , propJSPEntryDefault);
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_Banner_Width                  , propBannerWidth);
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_Banner_Style                  , propBannerStyle);
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_Banner_ImageSource            , propBannerImageSource);
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_Banner_ImageWidth             , propBannerImageWidth);
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_Banner_ImageHeight            , propBannerImageHeight);
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_Banner_AnchorLink             , propBannerAnchorLink);
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_Background_Color              , propBackgroundColor);
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_Background_Image              , propBackgroundImage);
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_ContentCell_Color             , propContentCellColor);
                SetLafProperty(rtProps, BasicPrivateLabel.LAF_IconMenu_GroupTitle_Color     , propIconMenuTitleColor);
              //SetLafProperty(rtProps, BasicPrivateLabel.LAF_Background_Position           , propBackgroundPosition);
              //SetLafProperty(rtProps, BasicPrivateLabel.LAF_Background_Repeat             , propBackgroundRepeat);
                // update
                selResource.setDescription(description);
                selResource.setProperties(rtProps);
                selResource.setType(Resource.TYPE_RTPROPS);
                // save
                if (saveOK) {
                    selResource.save();
                    m = i18n.getString("LAFInfo.resourceUpdated","Resource information updated");
                } else {
                    // should stay on this page
                }
            } catch (Throwable t) {
                Print.logException("Updating Resource", t);
                m = i18n.getString("LAFInfo.errorUpdating","Internal error updating Resource");
                error = true;
            }
            uiList = true;
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = LAFInfo.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "LAFInfo.css", cssDir);
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(SORTTABLE_JS), request);
            }
        };

        /* Content */
        final OrderedSet<String> _resourceList = resourceList;
        final Resource _selResource    = selResource;
        final String   _selResourceID  = selResourceID;
        final boolean  _allowEdit      = allowEdit;
        final boolean  _allowView      = allowView;
        final boolean  _allowDelete    = allowDelete;
        final boolean  _allowNew       = allowNew;
        final boolean  _uiEdit         = _allowEdit && uiEdit;
        final boolean  _uiView         = _uiEdit || uiView;
        final boolean  _uiList         = uiList || (!_uiEdit && !_uiView);
        HTMLOutput HTML_CONTENT = null;
        if (_uiList) {

            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {
                    String pageName = LAFInfo.this.getPageName();
    
                    // frame header
                  //String menuURL    = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                    String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                    String editURL    = LAFInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String selectURL  = LAFInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String newURL     = LAFInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String frameTitle = _allowEdit? 
                        i18n.getString("LAFInfo.viewEditResource","View/Edit Look-and-Feel Information") : 
                        i18n.getString("LAFInfo.viewResource","View Look-and-Feel Information");
                    out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                    out.write("<hr>\n");
                        
                    // Resource selection table (Select, Host ID, Description)
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("LAFInfo.selectResource","Select a Host ID")+":</h1>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form name='"+FORM_LAF_SELECT+"' method='post' action='"+selectURL+"' target='_self'>"); // target='_top'
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SELECT+"'/>");
                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+"' cellspacing=0 cellpadding=0 border=0>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"' nowrap>"+i18n.getString("LAFInfo.select","Select")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("LAFInfo.hostID","Host ID")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("LAFInfo.description","Description")+"</th>\n");
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    out.write(" <tbody>\n");
                    for (int u = 0; u < _resourceList.size(); u++) {
                        String resID = _resourceList.get(u);
                        if ((u & 1) == 0) {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD+"'>\n");
                        } else {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN+"'>\n");
                        }
                        try {
                            Resource res = Resource.getResource(currAcct, resID);
                            if (res != null) {
                                String resourceID    = res.getResourceID();
                                String filtResID     = FilterText(resourceID);
                                String filtHostID    = FilterHostID(resourceID);
                                String checked       = _selResourceID.equals(res.getResourceID())? "checked" : "";
                                RTProperties rtProps = GetResourceProperties(res);
                                String propPageTitle = rtProps.getString(BasicPrivateLabel.LAF_PageTitle,"");
                                String resDesc       = res.getDescription();
                                String filtDesc      = !StringTools.isBlank(resDesc)? FilterText(resDesc) : FilterText(propPageTitle);
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+u+"'><input type='radio' name='"+PARM_LAF_SELECT+"' id='"+filtResID+"' value='"+filtResID+"' "+checked+"></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+filtResID+"'>"+filtHostID+"</label></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+filtDesc+"</td>\n");
                            }
                        } catch (DBException dbe) {
                            // 
                        }
                        out.write("  </tr>\n");
                    }
                    out.write(" </tbody>\n");
                    out.write("</table>\n");
                    out.write("<table cellpadding='0' cellspacing='0' border='0' style='width:95%; margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<tr>\n");
                    if (_allowView  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("LAFInfo.view","View")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowEdit  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("LAFInfo.edit","Edit")+"'>");
                        out.write("</td>\n"); 
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) { 
                        out.write("<input type='submit' name='"+PARM_SUBMIT_DEL+"' value='"+i18n.getString("LAFInfo.delete","Delete")+"' "+Onclick_ConfirmDelete(locale)+">");
                    } else {
                        out.write("&nbsp;"); 
                    }
                    out.write("</td>\n");
                    out.write("</tr>\n");
                    out.write("</table>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");
                    
                    /* new Resource */
                    if (_allowNew) {
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("LAFInfo.createNewEntry","Create a new entry")+":</h1>\n");
                    out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<form name='"+FORM_LAF_NEW+"' method='post' action='"+newURL+"' target='_self'>"); // target='_top'
                    out.write(" <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW+"'/>");
                    out.write(i18n.getString("LAFInfo.hostID","Host ID")+": <input type='text' class='"+CommonServlet.CSS_TEXT_INPUT+"' name='"+PARM_NEW_NAME+"' value='' size='32' maxlength='40'><br>\n");
                    out.write(" <input type='submit' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("LAFInfo.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");
                    }

                }
            };
            
        } else
        if (_uiEdit || _uiView) {

            final boolean _editLAF    = _uiEdit;
            final boolean _viewDevice = _editLAF || _uiView;
            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {
                    String pageName = LAFInfo.this.getPageName();

                    // frame header
                  //String menuURL    = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                    String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                    String editURL    = LAFInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String selectURL  = LAFInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String newURL     = LAFInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String frameTitle = _allowEdit? 
                        i18n.getString("LAFInfo.viewEditResource","View/Edit Look-and-Feel Information") : 
                        i18n.getString("LAFInfo.viewResource","View Look-and-Feel Information");
                    out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                    out.write("<hr>\n");
    
                    // Resource view/edit form
    
                    /* start of form */
                    out.write("<form name='"+FORM_LAF_EDIT+"' method='post' action='"+editURL+"' target='_self'>\n"); // target='_top'
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPD_LAF+"'/>\n");
                    out.write("<input type='hidden' name='"+PARM_LAF_SELECT+"' value='"+_selResourceID+"'/>\n");

                    /* Resource properties */
                    RTProperties rtProps = GetResourceProperties(_selResource);
                    String propPageTitle          = rtProps.getString(BasicPrivateLabel.LAF_PageTitle                ,"");
                    String propCopyright          = rtProps.getString(BasicPrivateLabel.LAF_Copyright                ,"");
                    String propJSPEntryDefault    = rtProps.getString(BasicPrivateLabel.LAF_JSPEntry_Default         ,"");
                    String propBannerWidth        = rtProps.getString(BasicPrivateLabel.LAF_Banner_Width             ,"");
                    String propBannerStyle        = rtProps.getString(BasicPrivateLabel.LAF_Banner_Style             ,"");
                    String propBannerImageSource  = rtProps.getString(BasicPrivateLabel.LAF_Banner_ImageSource       ,"");
                    String propBannerImageWidth   = rtProps.getString(BasicPrivateLabel.LAF_Banner_ImageWidth        ,"");
                    String propBannerImageHeight  = rtProps.getString(BasicPrivateLabel.LAF_Banner_ImageHeight       ,"");
                    String propBannerAnchorLink   = rtProps.getString(BasicPrivateLabel.LAF_Banner_AnchorLink        ,"");
                    String propBackgroundColor    = rtProps.getString(BasicPrivateLabel.LAF_Background_Color         ,"");
                    String propBackgroundImage    = rtProps.getString(BasicPrivateLabel.LAF_Background_Image         ,"");
                    String propContentCellColor   = rtProps.getString(BasicPrivateLabel.LAF_ContentCell_Color        ,"");
                    String propIconMenuTitleColor = rtProps.getString(BasicPrivateLabel.LAF_IconMenu_GroupTitle_Color,"");
                  //String propBackgroundPosition = rtProps.getString(BasicPrivateLabel.LAF_Background_Position      ,"");
                  //String propBackgroundRepeat   = rtProps.getString(BasicPrivateLabel.LAF_Background_Repeat        ,"");
                    if (StringTools.isBlank(propJSPEntryDefault)) {
                        propJSPEntryDefault = "default";
                    }

                    /* JSPEntries */
                    OrderedMap<String,String> jspDesc = privLabel.getJSPEntryDescriptions(locale); // always a new map
                    jspDesc.remove("emailReport"); // special case
                    ComboMap    jspEntries  = new ComboMap(jspDesc);
                    ComboOption selJspEntry = new ComboOption(propJSPEntryDefault);

                    /* Resource fields */
                    String hostID = FilterHostID(_selResourceID);
                    out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                    out.println(FormRow_TextField(PARM_HOST_ID             , false   , i18n.getString("LAFInfo.hostID"            ,"Host ID")+":" , hostID, 40, 40));
                    out.println(FormRow_TextField(PARM_DESCRIPTION         , _editLAF, i18n.getString("LAFInfo.description"       ,"Description")+":", (_selResource!=null)?_selResource.getDescription():"", 64, 64));
                    out.println(FormRow_Separator());
                    out.println(FormRow_TextNote(null,i18n.getString("LAFInfo.note.selectJSPEntry",
                        "Select the name of a JSP entry (from the JSPEntries section in 'private.xml')\n" +
                        "('WebPages' tag, 'jsp' attribute, must reference the 'default' JSP entry)")));
                    out.println(FormRow_ComboBox (PARM_JSPENTRY_DEFAULT    , _editLAF, i18n.getString("LAFInfo.jspEntryDefault"   ,"Default JSP Entry")+":"   , selJspEntry, jspEntries, null, 28));
                    out.println(FormRow_Separator());
                    out.println(FormRow_TextNote(null,i18n.getString("LAFInfo.note.enterTitleCopyright",
                        "Enter the page Title and Copyright text to display.\n"+
                        "Depending on the selected JSP entry, the page Title may also be displayed within the banner image.")));
                    out.println(FormRow_TextField(PARM_PAGETITLE           , _editLAF, i18n.getString("LAFInfo.pageTitle"         ,"Page Title")+":"          , propPageTitle         , 72, 72));
                    out.println(FormRow_TextField(PARM_COPYRIGHT           , _editLAF, i18n.getString("LAFInfo.copyright"         ,"Page Copyright")+":"      , propCopyright         , 64, 64));
                    out.println(FormRow_Separator());
                    out.println(FormRow_TextNote(null,i18n.getString("LAFInfo.note.bannerAreaWidthStyle",
                        "Enter the banner area width and style")));
                    out.println(FormRow_TextField(PARM_BANNER_WIDTH        , _editLAF, i18n.getString("LAFInfo.bannerAreaWidth"   ,"Banner Area Width")+":"   , propBannerWidth       , 5, 5));
                    out.println(FormRow_TextField(PARM_BANNER_STYLE        , _editLAF, i18n.getString("LAFInfo.bannerStyle"       ,"Banner Style")+":"        , propBannerStyle       , 60, 60));
                    out.println(FormRow_Separator());
                    out.println(FormRow_TextNote(null,i18n.getString("LAFInfo.note.bannerImageInfo",
                        "Enter the banner image source/URL, width, height, and link (if applicable)")));
                    out.println(FormRow_TextField(PARM_BANNER_IMG_SOURCE   , _editLAF, i18n.getString("LAFInfo.bannerImageSource" ,"Banner Image Source")+":" , propBannerImageSource , 85, 120));
                    out.println(FormRow_TextField(PARM_BANNER_IMG_WIDTH    , _editLAF, i18n.getString("LAFInfo.bannerImageWidth"  ,"Banner Image Width")+":"  , propBannerImageWidth  ,  5,   5));
                    out.println(FormRow_TextField(PARM_BANNER_IMG_HEIGHT   , _editLAF, i18n.getString("LAFInfo.bannerImageHeight" ,"Banner Image Height")+":" , propBannerImageHeight ,  5,   5));
                    out.println(FormRow_TextField(PARM_BANNER_IMG_ANCHOR   , _editLAF, i18n.getString("LAFInfo.bannerImageLink"   ,"Banner Image Link")+":"   , propBannerAnchorLink  , 80,  80));
                    out.println(FormRow_Separator());
                    out.println(FormRow_TextNote(null,i18n.getString("LAFInfo.note.backgroundColor",
                        "Enter the background/contentCell colors in html format (eg. '#FFFFFF' is white)")));
                    out.println(FormRow_TextField(PARM_BACKGROUND_COLOR    , _editLAF, i18n.getString("LAFInfo.backgroundColor"   ,"Backgound Color")+":"     , propBackgroundColor   , 12,  12));
                    out.println(FormRow_TextField(PARM_BACKGROUND_IMAGE    , _editLAF, i18n.getString("LAFInfo.backgroundImage"   ,"Backgound Image URL")+":" , propBackgroundImage   , 85, 120));
                    out.println(FormRow_TextField(PARM_CONTENTCELL_COLOR   , _editLAF, i18n.getString("LAFInfo.contentCellColor"  ,"ContentCell Color")+":"   , propContentCellColor  , 12,  12));
                    out.println(FormRow_TextField(PARM_ICONMENU_TITLE_COLOR, _editLAF, i18n.getString("LAFInfo.iconMenuTitleColor","IconMenu Title Color")+":", propIconMenuTitleColor, 12,  12));
                    out.println(FormRow_Separator());
                    out.println(FormRow_TextNote(i18n.getString("LAFInfo.note","Note:"),i18n.getString("LAFInfo.note.summary",
                        "Any of the above values may be left blank, in which case their default value will be used")));
                    out.println("</table>");

                    /* end of form */
                    out.write("<hr style='margin-bottom:5px;'>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_editLAF) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("LAFInfo.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("LAFInfo.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_self');\">\n"); // target='_top'
                    } else {
                        out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("LAFInfo.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_self');\">\n"); // target='_top'
                    }
                    out.write("</form>\n");

                }
            };

        }

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
