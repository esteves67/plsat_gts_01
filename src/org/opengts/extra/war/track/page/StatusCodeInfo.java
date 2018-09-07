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

public class StatusCodeInfo
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------
    // Parameters

    // forms 
    public  static final String FORM_CODE_SELECT        = "CodeInfoSelect";
    public  static final String FORM_CODE_EDIT          = "CodeInfoEdit";
    public  static final String FORM_CODE_NEW           = "CodeInfoNew";

    // commands
    public  static final String COMMAND_INFO_UPDATE     = "update";
    public  static final String COMMAND_INFO_SELECT     = "select";
    public  static final String COMMAND_INFO_NEW        = "new";

    // submit
    public  static final String PARM_SUBMIT_EDIT        = "c_subedit";
    public  static final String PARM_SUBMIT_VIEW        = "c_subview";
    public  static final String PARM_SUBMIT_CHG         = "c_subchg";
    public  static final String PARM_SUBMIT_DEL         = "c_subdel";
    public  static final String PARM_SUBMIT_NEW         = "c_subnew";

    // buttons
    public  static final String PARM_BUTTON_CANCEL      = "u_btncan";
    public  static final String PARM_BUTTON_BACK        = "u_btnbak";

    // parameters
    public  static final String PARM_NEW_CODE           = "u_newcode";
    public  static final String PARM_CODE_SELECT        = "u_code";
    public  static final String PARM_CODE_ID            = "u_id";
    public  static final String PARM_CODE_NAME          = "u_name";
    public  static final String PARM_CODE_DESC          = "u_desc";
    public  static final String PARM_CODE_ICON_SEL      = "u_iconsel";
    public  static final String PARM_CODE_ICON_NAM      = "u_iconnam"; // pushpin
    public  static final String PARM_CODE_FG_COLOR      = "u_fgcolor"; // text color

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public StatusCodeInfo()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_CODE_INFO);
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
        I18N i18n = privLabel.getI18N(StatusCodeInfo.class);
        return super._getMenuDescription(reqState,i18n.getString("StatusCodeInfo.editMenuDesc","View/Edit StatusCode Information"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(StatusCodeInfo.class);
        return super._getMenuHelp(reqState,i18n.getString("StatusCodeInfo.editMenuHelp","View and Edit StatusCode information"));
    }
    
    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(StatusCodeInfo.class);
        return super._getNavigationDescription(reqState,i18n.getString("StatusCodeInfo.navDesc","StatusCode"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(StatusCodeInfo.class);
        return i18n.getString("StatusCodeInfo.navTab","StatusCode Admin");
    }

    // ------------------------------------------------------------------------

    private ComboMap getColorComboMap(boolean background, I18N i18n)
    {
        ComboMap c = new ComboMap();
        c.add(""                              ,i18n.getString("StatusCodeInfo.color_default","Default"));
        if (background) {
            // TODO: lighter color
            c.add(ColorTools.BLACK .toString(true),i18n.getString("StatusCodeInfo.color_black"  ,"Black"  ));
            c.add(ColorTools.BROWN .toString(true),i18n.getString("StatusCodeInfo.color_brown"  ,"Brown"  ));
            c.add(ColorTools.RED   .toString(true),i18n.getString("StatusCodeInfo.color_red"    ,"Red"    ));
            c.add(ColorTools.ORANGE.toString(true),i18n.getString("StatusCodeInfo.color_orange" ,"Orange" ));
            c.add(ColorTools.YELLOW.toString(true),i18n.getString("StatusCodeInfo.color_yellow" ,"Yellow" ));
            c.add(ColorTools.GREEN .toString(true),i18n.getString("StatusCodeInfo.color_green"  ,"Green"  ));
            c.add(ColorTools.BLUE  .toString(true),i18n.getString("StatusCodeInfo.color_blue"   ,"Blue"   ));
            c.add(ColorTools.PURPLE.toString(true),i18n.getString("StatusCodeInfo.color_purple" ,"Purple" ));
            c.add(ColorTools.GRAY  .toString(true),i18n.getString("StatusCodeInfo.color_gray"   ,"Gray"   ));
            c.add(ColorTools.WHITE .toString(true),i18n.getString("StatusCodeInfo.color_white"  ,"White"  ));
        } else {
            // standard color
            c.add(ColorTools.BLACK .toString(true),i18n.getString("StatusCodeInfo.color_black"  ,"Black"  ));
            c.add(ColorTools.BROWN .toString(true),i18n.getString("StatusCodeInfo.color_brown"  ,"Brown"  ));
            c.add(ColorTools.RED   .toString(true),i18n.getString("StatusCodeInfo.color_red"    ,"Red"    ));
            c.add(ColorTools.ORANGE.toString(true),i18n.getString("StatusCodeInfo.color_orange" ,"Orange" ));
            c.add(ColorTools.YELLOW.toString(true),i18n.getString("StatusCodeInfo.color_yellow" ,"Yellow" ));
            c.add(ColorTools.GREEN .toString(true),i18n.getString("StatusCodeInfo.color_green"  ,"Green"  ));
            c.add(ColorTools.BLUE  .toString(true),i18n.getString("StatusCodeInfo.color_blue"   ,"Blue"   ));
            c.add(ColorTools.PURPLE.toString(true),i18n.getString("StatusCodeInfo.color_purple" ,"Purple" ));
            c.add(ColorTools.GRAY  .toString(true),i18n.getString("StatusCodeInfo.color_gray"   ,"Gray"   ));
            c.add(ColorTools.WHITE .toString(true),i18n.getString("StatusCodeInfo.color_white"  ,"White"  ));
        }
        return c;
    }

    private String filter(String s)
    {
        if (StringTools.isBlank(s)) {
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
        final I18N    i18n           = privLabel.getI18N(StatusCodeInfo.class);
        final Locale  locale         = reqState.getLocale();
        final Account currAcct       = reqState.getCurrentAccount(); // never null
        final String  currAcctID     = currAcct.getAccountID(); // never null
        final User    currUser       = reqState.getCurrentUser(); // may be null
        final String  pageName       = this.getPageName();
        final boolean hasRuleFac     = Device.hasRuleFactory();
        final boolean showIconSel    = hasRuleFac && 
            privLabel.getBooleanProperty(PrivateLabel.PROP_StatusCodeInfo_showIconSelector,false);
        final boolean showTextColors = true;
        String m = pageMsg;
        boolean error = false;

        /* List of custom status codes */
        int codeList[] = null;
        try {
            codeList = StatusCode.getStatusCodes(currAcctID,null/*deviceID*/);
        } catch (DBException dbe) {
            codeList = new int[0];
        }
        
        /* selected status code */
        int selCodeID = AttributeTools.getRequestInt(request, PARM_CODE_SELECT, 0);
        if (selCodeID <= 0) {
            if (codeList.length > 0) {
                selCodeID = codeList[0];
            } else {
                selCodeID = 0;
            }
        }
        if (codeList.length == 0) {
            codeList = new int[] { selCodeID };
        }

        /* code db */
        StatusCode selCode = null;
        try {
            selCode = StatusCode.getStatusCode(currAcct, null, selCodeID); // may still be null
        } catch (DBException dbe) {
            // ignore
        }

        /* ACL allow edit/view */
        boolean allowNew    = privLabel.hasAllAccess(currUser, this.getAclName());
        boolean allowDelete = allowNew;
        boolean allowEdit   = allowNew  || privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView   = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());

        /* command */
        String  codeCmd     = reqState.getCommandName();
        boolean listCodes   = false;
        boolean updateCode  = codeCmd.equals(COMMAND_INFO_UPDATE);
        boolean selectCode  = codeCmd.equals(COMMAND_INFO_SELECT);
        boolean newCode     = codeCmd.equals(COMMAND_INFO_NEW);
        boolean deleteCode  = false;
        boolean editCode    = false;
        boolean viewCode    = false;

        /* submit buttons */
        String submitEdit   = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT, "");
        String submitView   = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW, "");
        String submitChange = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG , "");
        String submitNew    = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW , "");
        String submitDelete = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL , "");

        /* sub-command */
        if (newCode) {
            if (!allowNew) {
               newCode = false; // not authorized
            } else {
                String codeStr = AttributeTools.getRequestString(request, PARM_NEW_CODE, "");
                int code = StringTools.isInt(codeStr,true)? StringTools.parseInt(codeStr,0) : 0;
                if ((code <= 0) /*|| (code > 0xFFFF)*/) {
                    m = i18n.getString("StatusCodeInfo.enterNewCode","Please enter a valid new StatusCode ID\\n(numeric value in hex or decimal format).");
                    error = true;
                    newCode = false;
                }
            }
        } else
        if (updateCode) {
            if (!allowEdit) {
                // not authorized to update codes
                updateCode = false;
            } else
            if (!SubmitMatch(submitChange,i18n.getString("StatusCodeInfo.change","Change"))) {
                updateCode = false;
            } else
            if (selCode == null) {
                // should not occur
                m = i18n.getString("StatusCodeInfo.unableToUpdate","Unable to update StatusCode, ID not found");
                error = true;
                updateCode = false;
            }
        } else
        if (selectCode) {
            if (SubmitMatch(submitDelete,i18n.getString("StatusCodeInfo.delete","Delete"))) {
                if (allowDelete) {
                    deleteCode = true;
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("StatusCodeInfo.edit","Edit"))) {
                if (allowEdit) {
                    if (selCode == null) {
                        m = i18n.getString("StatusCodeInfo.pleaseSelectCode","Please select a StatusCode");
                        error = true;
                        listCodes = true;
                    } else {
                        editCode = true;
                        viewCode = true;
                    }
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("StatusCodeInfo.view","View"))) {
                if (allowView) {
                    if (selCode == null) {
                        m = i18n.getString("StatusCodeInfo.pleaseSelectCode","Please select a StatusCode");
                        error = true;
                        listCodes = true;
                    } else {
                        viewCode = true;
                    }
                }
            } else {
                listCodes = true;
            }
        } else {
            listCodes = true;
        }

        /* delete Code? */
        if (deleteCode) {
            if (selCode == null) {
                m = i18n.getString("StatusCodeInfo.pleaseSelectCode","Please select a StatusCode");
                error = true;
            } else {
                try {
                    StatusCode.Key codeKey = (StatusCode.Key)selCode.getRecordKey();
                    Print.logWarn("Deleting StatusCode: " + codeKey);
                    codeKey.delete(true); // will also delete dependencies
                    selCodeID = 0;
                    selCode = null;
                    codeList = StatusCode.getStatusCodes(currAcctID,null);
                    if (codeList.length > 0) {
                        selCodeID = codeList[0];
                        try {
                            selCode = StatusCode.getStatusCode(currAcct, null, selCodeID);
                        } catch (DBException dbe) {
                            // ignore
                        }
                    }
                } catch (DBException dbe) {
                    Print.logException("Deleting StatusCode", dbe);
                    m = i18n.getString("StatusCodeInfo.errorDelete","Internal error deleting StatusCode");
                    error = true;
                }
            }
            listCodes = true;
        }

        /* new code? */
        if (newCode) {
            boolean createCodeOK = true;
            int newCodeID = StringTools.parseInt(AttributeTools.getRequestString(request, PARM_NEW_CODE, ""), 0);
            for (int u = 0; u < codeList.length; u++) {
                if (newCodeID == codeList[u]) {
                    m = i18n.getString("StatusCodeInfo.alreadyExists","This StatusCode already exists");
                    error = true;
                    createCodeOK = false;
                    break;
                }
            }
            if (createCodeOK) {
                try {
                    StatusCode code = StatusCode.createNewStatusCode(currAcct, null, newCodeID); // already saved
                    codeList = StatusCode.getStatusCodes(currAcctID, null);
                    selCode = code;
                    selCodeID = code.getStatusCode();
                    m = i18n.getString("StatusCodeInfo.createdCode","New StatusCode has been created");
                } catch (DBException dbe) {
                    Print.logException("Creating StatusCode", dbe);
                    m = i18n.getString("StatusCodeInfo.errorCreate","Internal error creating StatusCode");
                    error = true;
                }
            }
            listCodes = true;
        }

        /* change/update the StatusCode info? */
        if (updateCode) {
            // 'selCode' guaranteed non-null here
            String codeName = AttributeTools.getRequestString(request, PARM_CODE_NAME    , "");
            String codeDesc = AttributeTools.getRequestString(request, PARM_CODE_DESC    , "");
            String iconName = AttributeTools.getRequestString(request, PARM_CODE_ICON_NAM, "");
            String iconSel  = AttributeTools.getRequestString(request, PARM_CODE_ICON_SEL, "");
            String fgColor  = AttributeTools.getRequestString(request, PARM_CODE_FG_COLOR, "");
            listCodes = true;
            try {
                if (selCode != null) {
                    boolean saveOK = true;
                    // name
                    if (!StringTools.isBlank(codeName)) {
                        selCode.setStatusName(codeName);
                    }
                    // description
                    if (!StringTools.isBlank(codeDesc)) {
                        selCode.setDescription(codeDesc);
                    }
                    // icon name selector/name
                    selCode.setIconName(iconName);
                    // icon selector
                    if (showIconSel) {
                        selCode.setIconSelector(iconSel);
                    }
                    // foreground color
                    if (showTextColors) {
                        selCode.setForegroundColor(fgColor);
                    }
                    // save
                    if (saveOK) {
                        selCode.save();
                        m = i18n.getString("StatusCodeInfo.codeUpdated","StatusCode information updated");
                    } else {
                        // should stay on this page
                        editCode = true;
                    }
                } else {
                    m = i18n.getString("StatusCodeInfo.noCodes","There are currently no defined StatusCodes for this account.");
                }
                //return;
            } catch (Throwable t) {
                Print.logException("Updating StatusCode", t);
                m = i18n.getString("StatusCodeInfo.errorUpdate","Internal error updating StatusCode");
                error = true;
                //return;
            }
        }

        /* PushpinChooser */
        final boolean showPushpinChooser = privLabel.getBooleanProperty(PrivateLabel.PROP_StatusCodeInfo_showPushpinChooser,false);
        //Print.logInfo("PushpinChooser enabled = " + showPushpinChooser);

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = StatusCodeInfo.this.getCssDirectory();
                //WebPageAdaptor.writeCssLink(out, reqState, "StatusCodeInfo.css", cssDir);
                if (showPushpinChooser) {
                    WebPageAdaptor.writeCssLink(out, reqState, "PushpinChooser.css", cssDir);
                }
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(SORTTABLE_JS), request);
                if (showPushpinChooser) {
                    PushpinIcon.writePushpinChooserJS(out, reqState, true);
                }
            }
        };

        /* Content */
        final int        _selCodeID   = selCodeID;
        final StatusCode _selCode     = selCode;
        final int        _codeList[]  = codeList;
        final boolean    _allowEdit   = allowEdit;
        final boolean    _allowView   = allowView;
        final boolean    _allowNew    = allowNew;
        final boolean    _allowDelete = allowDelete;
        final boolean    _editCode    = _allowEdit && editCode;
        final boolean    _viewCode    = _editCode || viewCode;
        final boolean    _listCodes   = listCodes || (!_editCode && !_viewCode);
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName = StatusCodeInfo.this.getPageName();

                // frame header
              //String menuURL    = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                String editURL    = StatusCodeInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String selectURL  = StatusCodeInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String newURL     = StatusCodeInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String frameTitle = _allowEdit? 
                    i18n.getString("StatusCodeInfo.viewEditCode","View/Edit StatusCode Descriptions") : 
                    i18n.getString("StatusCodeInfo.viewCode","View StatusCode Descriptions");
                out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                out.write("<hr>\n");

                // StatusCodeInfo selection table (Select, StatusCodeInfo ID, StatusCodeInfo Name)
                if (_listCodes) {
                    
                    // StatusCodeInfo selection table (Select, StatusCodeInfo ID, StatusCodeInfo Name)
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("StatusCodeInfo.selectCode","Select a StatusCode")+":</h1>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form name='"+FORM_CODE_SELECT+"' method='post' action='"+selectURL+"' target='_top'>");
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SELECT+"'/>");
                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+"' cellspacing=0 cellpadding=0 border=0>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"' nowrap>"+i18n.getString("StatusCodeInfo.select","Select")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("StatusCodeInfo.codeID","StatusCode ID")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("StatusCodeInfo.name","Name")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("StatusCodeInfo.description","Description")+"</th>\n");
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    out.write(" <tbody>\n");
                    for (int u = 0; u < _codeList.length; u++) {
                        if ((u & 1) == 0) {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD+"'>\n");
                        } else {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN+"'>\n");
                        }
                        try {
                            StatusCode code = StatusCode.getStatusCode(currAcct, null, _codeList[u]);
                            if (code != null) {
                                String codeID   = "0x" + StringTools.toHexString(code.getStatusCode(),16);
                                String codeName = StatusCodeInfo.this.filter(code.getStatusName());
                                String codeDesc = StatusCodeInfo.this.filter(code.getDescription());
                                String checked  = (_selCodeID == code.getStatusCode())? " checked" : "";
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+u+"'><input type='radio' name='"+PARM_CODE_SELECT+"' id='"+codeID+"' value='"+codeID+"' "+checked+"></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+codeID+"'>"+codeID+"</label></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+codeName+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+codeDesc+"</td>\n");
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
                        out.write("<input type='submit' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("StatusCodeInfo.view","View")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowEdit  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("StatusCodeInfo.edit","Edit")+"'>");
                        out.write("</td>\n"); 
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_DEL+"' value='"+i18n.getString("StatusCodeInfo.delete","Delete")+"' "+Onclick_ConfirmDelete(locale)+">");
                    } else {
                        out.write("&nbsp;"); 
                    }
                    out.write("</td>\n"); 
                    out.write("</tr>\n");
                    out.write("</table>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");
                    
                    /* new code */
                    if (_allowNew) {
                        Map<Integer,String> _scDescMap = privLabel.getStatusCodeDescriptionMap();
                        ComboMap scList = new ComboMap();
                        for (Integer sc : _scDescMap.keySet()) {
                            String key  = "0x"+StringTools.toHexString(sc.intValue(),16);
                            String desc = "[" + key + "] " + _scDescMap.get(sc);
                            scList.add(key, desc);
                        }
                        out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("StatusCodeInfo.createNewCode","Create a new StatusCode")+":</h1>\n");
                        out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                        out.write("<form name='"+FORM_CODE_NEW+"' method='post' action='"+newURL+"' target='_top'>");
                        out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW+"'/>");
                        out.write(i18n.getString("StatusCodeInfo.codeID","StatusCode ID")+": <input type='text' class='"+CommonServlet.CSS_TEXT_INPUT+"' name='"+PARM_NEW_CODE+"' value='' size='10' maxlength='32'>");
                        out.write("&nbsp;<span style='margin-left:10px;'>(&nbsp;"+i18n.getString("StatusCodeInfo.chooseStandardCode","or, select a standard code:")+"</span>");
                        out.println(Form_ComboBox(null,"CodeSelect",true,scList,"","javascript:document."+FORM_CODE_NEW+"."+PARM_NEW_CODE+".value=document."+FORM_CODE_NEW+".CodeSelect.value;", -1));
                        out.write("<span style='margin-left:1px;'>)</span>");
                        out.write("<br><input type='submit' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("StatusCodeInfo.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                        out.write("</form>\n");
                        out.write("</div>\n");
                        out.write("<hr>\n");
                    }

                } else {
                    // code view/edit form
                    boolean ppidOK   = true;

                    /* start of form */
                    out.write("<form name='"+FORM_CODE_EDIT+"' method='post' action='"+editURL+"' target='_top'>\n");
                    out.write("  <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPDATE+"'/>\n");

                    /* StatusCode fields */
                    String codeID = "0x" + StringTools.toHexString(_selCodeID,16);
                    out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                    out.println(FormRow_TextField(PARM_CODE_SELECT   , false    , i18n.getString("StatusCodeInfo.codeID","StatusCode ID")+":"      , codeID, 8, 8));
                    out.println(FormRow_TextField(PARM_CODE_NAME     , _editCode, i18n.getString("StatusCodeInfo.name","Name")+":"                 , (_selCode!=null)?_selCode.getStatusName()  :"", 18, 18));
                    out.println(FormRow_TextField(PARM_CODE_DESC     , _editCode, i18n.getString("StatusCodeInfo.description","Description")+":"   , (_selCode!=null)?_selCode.getDescription() :"", 50, 80));
                    if (ppidOK) {
                        String ppTitle = i18n.getString("StatusCodeInfo.iconName","Pushpin Name") + ":" ;
                        String ppid = (_selCode != null)? _selCode.getIconName() : "";
                        if (showPushpinChooser) {
                            String ID_ICONSEL = "PushpinChooser";
                            String onclick    = _editCode? "javascript:ppcShowPushpinChooser('"+ID_ICONSEL+"')" : null;
                            out.println(FormRow_TextField(ID_ICONSEL, PARM_CODE_ICON_NAM, _editCode, ppTitle, ppid, onclick, 25, 32, null));
                        } else {
                            ComboMap ppList = new ComboMap(reqState.getMapProviderPushpinIDs());
                            ppList.insert(""); // insert a blank as the first entry
                            out.println(FormRow_ComboBox( PARM_CODE_ICON_NAM , _editCode, ppTitle, ppid, ppList, "", -1));
                        }
                    }
                    if (showIconSel) {
                        out.println(FormRow_TextField(PARM_CODE_ICON_SEL , _editCode, i18n.getString("StatusCodeInfo.iconSelector","Icon Selector")+":", (_selCode!=null)?_selCode.getIconSelector():"", 70, 100));
                    }
                    if (showTextColors) {
                        ComboMap colorMap = StatusCodeInfo.this.getColorComboMap(false,i18n);
                        String fgTitle = i18n.getString("StatusCodeInfo.textColor","Text Color") + ":" ;
                        String fgSel   = (_selCode != null)? _selCode.getForegroundColor() : "";
                        out.println(FormRow_ComboBox( PARM_CODE_FG_COLOR , _editCode, fgTitle, fgSel, colorMap, "", -1));
                    }
                    out.println("</table>");

                    /* end of form */
                    out.write("<hr style='margin-bottom:5px;'>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_editCode) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("StatusCodeInfo.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("StatusCodeInfo.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                    } else {
                        out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("StatusCodeInfo.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                    }
                    out.write("</form>\n");

                }

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
