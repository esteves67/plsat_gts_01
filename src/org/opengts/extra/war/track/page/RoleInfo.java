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
import org.opengts.db.AclEntry.AccessLevel;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class RoleInfo
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------

    private static final String ACL_DEFAULT             = "default";

    // ------------------------------------------------------------------------
    // Parameters

    // forms 
    public  static final String FORM_ROLE_SELECT        = "RoleInfoSelect";
    public  static final String FORM_ROLE_EDIT          = "RoleInfoEdit";
    public  static final String FORM_ROLE_NEW           = "RoleInfoNew";

    // commands
    public  static final String COMMAND_INFO_UPDATE     = "update";
    public  static final String COMMAND_INFO_SELECT     = "select";
    public  static final String COMMAND_INFO_NEW        = "new";
    public  static final String COMMAND_INFO_ROLE       = "rerole";

    // submit
    public  static final String PARM_SUBMIT_EDIT        = "r_subedit";
    public  static final String PARM_SUBMIT_VIEW        = "r_subview";
    public  static final String PARM_SUBMIT_CHG         = "r_subchg";
    public  static final String PARM_SUBMIT_DEL         = "r_subdel";
    public  static final String PARM_SUBMIT_NEW         = "r_subnew";

    // buttons
    public  static final String PARM_BUTTON_CANCEL          = "d_btncan";
    public  static final String PARM_BUTTON_BACK            = "d_btnbak";

    // parameters
    public  static final String PARM_NEW_NAME           = "u_newname";
    public  static final String PARM_GLOBAL_ROLE        = "u_global";
    public  static final String PARM_ROLE_SELECT        = "u_role";
    public  static final String PARM_ROLE_DESC          = "u_desc";
    public  static final String PARM_ACL_               = "u_acl_";

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public RoleInfo()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_ROLE_INFO);
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
        I18N i18n = privLabel.getI18N(RoleInfo.class);
        return super._getMenuDescription(reqState,i18n.getString("RoleInfo.editMenuDesc","View/Edit Role Information"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(RoleInfo.class);
        return super._getMenuHelp(reqState,i18n.getString("RoleInfo.editMenuHelp","View and Edit Role information"));
    }
    
    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(RoleInfo.class);
        return super._getNavigationDescription(reqState,i18n.getString("RoleInfo.navDesc","Role"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(RoleInfo.class);
        return i18n.getString("RoleInfo.navTab","Role Admin");
    }

    // ------------------------------------------------------------------------

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
        final I18N    i18n     = privLabel.getI18N(RoleInfo.class);
        final Locale  locale   = reqState.getLocale();
        final Account currAcct = reqState.getCurrentAccount(); // never null
        final User    currUser = reqState.getCurrentUser(); // may be null
        final String  pageName = this.getPageName();
        String m = pageMsg;
        boolean error = false;

        /* argument role-id */
        String roleList[] = null;
        try {
            roleList = Role.getRolesForAccount(currAcct.getAccountID(),false);
        } catch (DBException dbe) {
            roleList = new String[0];
        }
        String selRoleID = AttributeTools.getRequestString(reqState.getHttpServletRequest(), PARM_ROLE_SELECT, "");
        if (StringTools.isBlank(selRoleID)) {
            if ((roleList.length > 0) && (roleList[0] != null)) {
                selRoleID = roleList[0];
            } else {
                selRoleID = "";
            }
        }
        if (roleList.length == 0) {
            roleList = new String[] { selRoleID };
        }

        /* role db */
        Role selRole = null;
        try {
            selRole = !StringTools.isBlank(selRoleID)? Role.getRole(currAcct, selRoleID) : null; // may still be null
        } catch (DBException dbe) {
            // ignore
        }

        /* ACL allow edit/view */
        boolean allowNew    = privLabel.hasAllAccess(currUser, this.getAclName());
        boolean allowDelete = allowNew;
        boolean allowEdit   = allowNew || privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView   = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());

        /* command */
        String  roleCmd     = reqState.getCommandName();
        boolean listRoles   = false;
        boolean updateRole  = roleCmd.equals(COMMAND_INFO_UPDATE);
        boolean selectRole  = roleCmd.equals(COMMAND_INFO_SELECT);
        boolean newRole     = roleCmd.equals(COMMAND_INFO_NEW);
        boolean deleteRole  = false;
        boolean editRole    = false;
        boolean viewRole    = false;

        /* submit buttons */
        String submitEdit   = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT, "");
        String submitView   = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW, "");
        String submitChange = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG , "");
        String submitNew    = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW , "");
        String submitDelete = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL , "");

        /* sub-command */
        String newRoleID = null;
        if (newRole) {
            if (!allowNew) {
                newRole   = false; // not authorized
                newRoleID = null;
            } else {
                HttpServletRequest httpReq = reqState.getHttpServletRequest();
                newRoleID = AttributeTools.getRequestString(httpReq,PARM_NEW_NAME,"").trim();
                newRoleID = newRoleID.toLowerCase();
                //Print.logInfo("Global Role: " + AttributeTools.getRequestString(request,PARM_GLOBAL_ROLE,null));
                boolean globalRole = !StringTools.isBlank(AttributeTools.getRequestString(request,PARM_GLOBAL_ROLE,null));
                if (StringTools.isBlank(newRoleID)) {
                    m         = i18n.getString("RoleInfo.enterNewRole","Please enter a new role name.");
                    error     = true;
                    newRole   = false;
                    newRoleID = null;
                } else 
                if (!WebPageAdaptor.isValidID(reqState,/*PrivateLabel.PROP_RoleInfo_validateNewIDs,*/newRoleID)) {
                    m         = i18n.getString("RoleInfo.invalidIDChar","ID contains invalid characters");
                    error     = true;
                    newRole   = false;
                    newRoleID = null;
                } else
                if (globalRole) {
                    if (!currAcct.isSystemAdmin()) {
                        m         = i18n.getString("RoleInfo.invalidGlobalRole","Global role specification not valid.");
                        error     = true;
                        newRole   = false;
                        newRoleID = null;
                    } else {
                        newRoleID = RoleRecord.SYSTEM_ROLE_PREFIX + newRoleID;
                    }
                } else {
                    // new role ID is ok
                }
            }
        } else
        if (updateRole) {
            //Print.logInfo("Change Role ...");
            if (!allowEdit) {
                // not authorized to update roles
                updateRole = false;
            } else
            if (!SubmitMatch(submitChange,i18n.getString("RoleInfo.change","Change"))) {
                updateRole = false;
            } else
            if (selRole == null) {
                // should not occur
                m = i18n.getString("RoleInfo.unableToUpdate","Unable to update Role, ID not found");
                error = true;
                updateRole = false;
            }
        } else
        if (selectRole) {
            //Print.logInfo("Select Role: " + submit);
            if (SubmitMatch(submitDelete,i18n.getString("RoleInfo.delete","Delete"))) {
                if (!allowDelete) {
                    deleteRole = false;
                } else
                if (selRole == null) {
                    m = i18n.getString("RoleInfo.pleaseSelectRole","Please select a Role");
                    error = true;
                    deleteRole = false; // not selected
                } else {
                    deleteRole = true;
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("RoleInfo.edit","Edit"))) {
                if (allowEdit) {
                    if (selRole == null) {
                        m = i18n.getString("RoleInfo.pleaseSelectRole","Please select a Role");
                        error = true;
                        listRoles = true;
                    } else {
                        editRole = true;
                        viewRole = true;
                    }
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("RoleInfo.view","View"))) {
                if (allowView) {
                    if (selRole == null) {
                        m = i18n.getString("RoleInfo.pleaseSelectRole","Please select a Role");
                        error = true;
                        listRoles = true;
                    } else {
                        viewRole = true;
                    }
                }
            } else {
                listRoles = true;
            }
        } else {
            listRoles = true;
        }

        /* delete role? */
        if (deleteRole) {
            if (selRole == null) {
                m = i18n.getString("RoleInfo.pleaseSelectRole","Please select a Role");
                error = true;
            } else {
                try {
                    Role.Key roleKey = (Role.Key)selRole.getRecordKey();
                    Print.logWarn("Deleting Role: " + roleKey);
                    roleKey.delete(true); // will also delete dependencies
                    selRoleID = "";
                    selRole = null;
                    roleList = Role.getRolesForAccount(currAcct.getAccountID(),false);
                    if (!ListTools.isEmpty(roleList)) {
                        selRoleID = roleList[0];
                        try {
                            selRole = !selRoleID.equals("")? Role.getRole(currAcct, selRoleID) : null; // may still be null
                        } catch (DBException dbe) {
                            // ignore
                        }
                    }
                } catch (DBException dbe) {
                    Print.logException("Deleting Role", dbe);
                    m = i18n.getString("RoleInfo.errorDelete","Internal error deleting Role");
                    error = true;
                }
            }
            listRoles = true;
        }

        /* new role? */
        if (newRole) {
            boolean createRoleOK = true;
            for (int u = 0; u < roleList.length; u++) {
                if (newRoleID.equalsIgnoreCase(roleList[u])) {
                    m = i18n.getString("RoleInfo.alreadyExists","This role already exists");
                    error = true;
                    createRoleOK = false;
                    break;
                }
            }
            if (createRoleOK) {
                try {
                    Role role = Role.createNewRole(currAcct, newRoleID); // already saved
                    roleList = Role.getRolesForAccount(currAcct.getAccountID(),false);
                    selRole = role;
                    selRoleID = role.getRoleID();
                    m = i18n.getString("RoleInfo.createdRole","New role has been created");
                } catch (DBException dbe) {
                    Print.logException("Creating Role", dbe);
                    m = i18n.getString("RoleInfo.errorCreate","Internal error creating Role");
                    error = true;
                }
            }
            listRoles = true;
        }

        /* change/update the role info? */
        if (updateRole) {
            // 'selRole' guaranteed non-null here
            String roleDesc     = AttributeTools.getRequestString(request, PARM_ROLE_DESC    , "");
            String aclKeys[]    = AttributeTools.getMatchingKeys( request, PARM_ACL_);
            listRoles = true;
            try {
                if (selRole != null) {
                    boolean saveOK = true;
                    // description
                    if (!roleDesc.equals("")) {
                        selRole.setDescription(roleDesc);
                    }
                    // ACLs
                    if (!ListTools.isEmpty(aclKeys)) {
                        for (int i = 0; i < aclKeys.length; i++) {
                            String      aclID    = aclKeys[i].substring(PARM_ACL_.length());
                            String      aclVal   = AttributeTools.getRequestString(request, aclKeys[i], "");
                            if (aclVal.equalsIgnoreCase(i18n.getString("RoleInfo.default","Default"))) {
                                try {
                                    RoleAcl.deleteAccessLevel(selRole, aclID);
                                } catch (DBException dbe) {
                                    Print.logException("Deleting RoleAcl: "+ selRole.getAccountID() + "/" + selRole.getRoleID() + "/" + aclID, dbe);
                                    m = i18n.getString("RoleInfo.errorDeleteAcl","Internal error deleting RoleAcl");
                                    error = true;
                                }
                            } else {
                                AccessLevel selLevel = EnumTools.getValueOf(AccessLevel.class, aclVal, locale);
                                AccessLevel dftLevel = privLabel.getDefaultAccessLevel(aclID);
                                //Print.logInfo("Found ACL: " + aclID + " ==> " + level);
                                try {
                                    //if (dftLevel.equals(selLevel)) {
                                    //    // matches the default, no reason for this ACL to be in the RoleAcl table, delete if present
                                    //    RoleAcl.deleteAccessLevel(selRole, aclID);
                                    //} else {
                                        // differs from the RoleAcl table, set ACL
                                        RoleAcl.setAccessLevel(selRole, aclID, selLevel);
                                    //}
                                } catch (DBException dbe) {
                                    Print.logException("Updating RoleAcl: "+ selRole.getAccountID() + "/" + selRole.getRoleID() + "/" + aclID, dbe);
                                    m = i18n.getString("RoleInfo.errorUpdate","Internal error updating Role");
                                    error = true;
                                }
                            }
                        }
                    }
                    // save
                    if (saveOK) {
                        selRole.save();
                        //Track.writeMessageResponse(reqState, 
                        //    i18n.getString("RoleInfo.roleUpdated","Role information updated"));
                        m = i18n.getString("RoleInfo.roleUpdated","Role information updated");
                    } else {
                        // should stay on this page
                        editRole = true;
                    }
                } else {
                    //Track.writeErrorResponse(reqState, 
                    //    i18n.getString("RoleInfo.noRoles","There are currently no defined roles for this account."));
                    m = i18n.getString("RoleInfo.noRoles","There are currently no defined roles for this account.");
                }
                //return;
            } catch (Throwable t) {
                Print.logException("Updating Role", t);
                //Track.writeErrorResponse(reqState, 
                //    i18n.getString("RoleInfo.errorUpdate","Internal error updating Role"));
                m = i18n.getString("RoleInfo.errorUpdate","Internal error updating Role");
                error = true;
                //return;
            }
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = RoleInfo.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "RoleInfo.css", cssDir);
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
        final String  _selRoleID   = selRoleID;
        final Role    _selRole     = selRole;
        final String  _roleList[]  = roleList;
        final boolean _allowEdit   = allowEdit;
        final boolean _allowView   = allowView;
        final boolean _allowNew    = allowNew;
        final boolean _allowDelete = allowDelete;
        final boolean _editRole    = _allowEdit && editRole;
        final boolean _viewRole    = _editRole || viewRole;
        final boolean _listRoles   = listRoles || (!_editRole && !_viewRole);
        final String  _tzList[]    = privLabel.getTimeZones(); // PrivateLabel only!
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName = RoleInfo.this.getPageName();

                // frame header
              //String menuURL    = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                String editURL    = RoleInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String selectURL  = RoleInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String newURL     = RoleInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String frameTitle = _allowEdit? 
                    i18n.getString("RoleInfo.viewEditRole","View/Edit Role Information") : 
                    i18n.getString("RoleInfo.viewRole","View Role Information");
              //String selectRoleJS = "javascript:dinfoSelectRole()";
                out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                out.write("<hr>\n");

                // role selection table (Select, Role ID, Role Name)
                if (_listRoles) {

                    // role selection table (Select, Role ID, Role Name)
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("RoleInfo.selectRole","Select a Role")+":</h1>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form name='"+FORM_ROLE_SELECT+"' method='post' action='"+selectURL+"' target='_top'>");
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SELECT+"'/>");
                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+"' cellspacing=0 cellpadding=0 border=0>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"' nowrap>"+i18n.getString("RoleInfo.select","Select")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("RoleInfo.roleID","Role ID")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("RoleInfo.description","Description")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("RoleInfo.hasUsers","Has Users")+"</th>\n");
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    out.write(" <tbody>\n");
                    StringBuffer roleCountJS = new StringBuffer();
                    for (int u = 0; u < _roleList.length; u++) {
                        if ((u & 1) == 0) {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD+"'>\n");
                        } else {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN+"'>\n");
                        }
                        try {
                            Role role = Role.getRole(currAcct, _roleList[u]);
                            if (role != null) {
                                String roleID   = RoleInfo.this.filter(role.getRoleID());
                                String roleDesc = RoleInfo.this.filter(role.getDescription());
                                String hasUsers = RoleInfo.this.filter(ComboOption.getYesNoText(locale,Role.hasUsers(role)));
                                String checked  = _selRoleID.equals(role.getRoleID())? " checked" : "";
                                String dispID   = Role.getDisplayRoleID(roleID);
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+u+"'><input type='radio' name='"+PARM_ROLE_SELECT+"' id='"+roleID+"' value='"+roleID+"' "+checked+"></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+roleID+"'>"+dispID+"</label></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+roleDesc+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+hasUsers+"</td>\n");
                                roleCountJS.append(" { role:'"+roleID+"', count:"+Role.getUserCount(role)+" },\n");
                            }
                        } catch (DBException dbe) {
                            // 
                        }
                        out.write("  </tr>\n");
                    }
                    out.write(" </tbody>\n");
                    out.write("</table>\n");

                    // JavaScript for deletion checking
                    JavaScriptTools.writeStartJavaScript(out);
                    String roleRefWarning = i18n.getString("RoleInfo.roleRefWarning","WARNING: THIS ROLE IS REFERENCED BY OTHER USERS!");
                    String confirmDelete  = i18n.getString("RoleInfo.confirmDelete","Are you sure you want to delete the selected role?");
                    out.write("var RoleUserCounts = [\n");
                    out.write(roleCountJS.toString());
                    out.write("];\n");
                    out.write("function roleGetUserCount(role) {\n");
                    out.write("  for (var i = 0; i < RoleUserCounts.length; i++) {\n");
                    out.write("    if (role == RoleUserCounts[i].role) { return RoleUserCounts[i].count; }\n");
                    out.write("  }\n");
                    out.write("  return -1;\n");
                    out.write("}\n");
                    out.write("function roleConfirmDelete() {\n");
                    out.write("  var selRole = '';\n");
                    out.write("  try {\n");
                    out.write("    for (var i = 0; i < document."+FORM_ROLE_SELECT+"."+PARM_ROLE_SELECT+".length; i++) {\n");
                    out.write("      if (document."+FORM_ROLE_SELECT+"."+PARM_ROLE_SELECT+"[i].checked) {\n");
                    out.write("        selRole = document."+FORM_ROLE_SELECT+"."+PARM_ROLE_SELECT+"[i].value;\n");
                    out.write("        //alert('Found Role ID ' + selRole);\n");
                    out.write("      }\n");
                    out.write("    }\n");
                    out.write("  } catch(err) {\n");
                    out.write("    selRole = '';\n");
                    out.write("  }\n");
                    out.write("  var userCount = roleGetUserCount(selRole);\n");
                    out.write("  if (userCount > 0) {\n");
                    out.write("    return confirm(\"" + roleRefWarning + " [\"+userCount+\"]\\n" + confirmDelete + "\");\n");
                    out.write("  } else {\n");
                    out.write("    return confirm(\"" + confirmDelete + "\");\n");
                    out.write("  }\n");
                    out.write("}\n");
                    JavaScriptTools.writeEndJavaScript(out);

                    out.write("<table cellpadding='0' cellspacing='0' border='0' style='width:95%; margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<tr>\n");
                    if (_allowView  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("RoleInfo.view","View")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowEdit  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("RoleInfo.edit","Edit")+"'>");
                        out.write("</td>\n"); 
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_DEL+"' value='"+i18n.getString("RoleInfo.delete","Delete")+"' onclick=\"return roleConfirmDelete();\">");
                    } else {
                        out.write("&nbsp;"); 
                    }
                    out.write("</td>\n"); 
                    out.write("</tr>\n");
                    out.write("</table>\n");

                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");
                    
                    /* new role */
                    if (_allowNew) {
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("RoleInfo.createNewRole","Create a new role")+":</h1>\n");
                    out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<form name='"+FORM_ROLE_NEW+"' method='post' action='"+newURL+"' target='_top'>");
                    out.write(" <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW+"'/>");
                    out.write(i18n.getString("RoleInfo.roleID","Role ID")+": <input type='text' class='"+CommonServlet.CSS_TEXT_INPUT+"' name='"+PARM_NEW_NAME+"' value='' size='32' maxlength='32'><br>\n");
                    if (currAcct.isSystemAdmin()) {
                        out.write("<div style='margin: 4px 0px 0px 0px;'>");
                        out.write(i18n.getString("RoleInfo.createGlobalRole","Check to create global role") + "&nbsp;");
                        out.write("<input type='checkbox' style='vertical-align: middle;' name='"+PARM_GLOBAL_ROLE+"' checked>");
                        out.write("</div>\n");
                    }
                    out.write(" <input type='submit' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("RoleInfo.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");
                    }

                } else {
                    // role view/edit form

                    /* start of form */
                    out.write("<form name='"+FORM_ROLE_EDIT+"' method='post' action='"+editURL+"' target='_top'>\n");
                    out.write("  <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPDATE+"'/>\n");

                    /* Role fields */
                    String roleRole = (_selRole != null)? _selRole.getRoleID() : "";
                    out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                    out.println(FormRow_TextField(PARM_ROLE_SELECT   , false    , i18n.getString("RoleInfo.roleID","Role ID")+":"            , _selRoleID, 16, 32));
                    out.println(FormRow_TextField(PARM_ROLE_DESC     , _editRole, i18n.getString("RoleInfo.description","Description")+":"   , (_selRole!=null)?_selRole.getDescription() :"", 50, 80));
                    out.println("</table>");
                    //out.write("<hr>\n");

                    /* ACL entries */
                    out.write("<span style='margin-left: 4px; margin-top: 8px; font-weight: bold;'>");
                    out.write(i18n.getString("RoleInfo.roleAccessControl","Role Access Control") + ":</span>\n");
                    out.write("<div class='roleAclViewDiv'>\n");
                    out.write("<table>\n");
                    boolean _editAcl = _editRole;
                    AclEntry aclEntries[] = privLabel.getAllAclEntries();
                    String aclLevels[] = EnumTools.getValueNames(AccessLevel.class, locale);
                    for (int a = 0; a < aclEntries.length; a++) {
                        AclEntry    acl      = aclEntries[a];
                        String      aclName  = acl.getName();
                        String      argKey   = PARM_ACL_ + aclName;
                        AccessLevel dftAcc   = acl.getDefaultAccessLevel(); // is not null
                        AccessLevel valAcc[] = acl.getAccessLevelValues();  // is not null
                        AccessLevel maxAcc   = acl.getMaximumAccessLevel(); // is not null
                        String      desc     = acl.getDescription(locale);
                        ComboMap    aclMap   = privLabel.getEnumComboMap(AccessLevel.class, valAcc);
                        aclMap.insert(ACL_DEFAULT, i18n.getString("RoleInfo.default","Default"));
                        AccessLevel usrAcc   = (_selRole != null)? RoleAcl.getAccessLevel(_selRole,aclName,null) : null;
                        if ((usrAcc != null) && !ListTools.contains(valAcc,usrAcc)) { usrAcc = maxAcc; } // to prevent selecting non-existant levels
                        ComboOption accSel   = (usrAcc != null)? 
                            privLabel.getEnumComboOption(usrAcc) :
                            new ComboOption(ACL_DEFAULT,i18n.getString("RoleInfo.default","Default"));
                        String      dftHtml  = i18n.getString("RoleInfo.defaultIsAcl","[Default is ''{0}'']","<b>"+dftAcc.toString(locale)+"</b>");
                        out.write(FormRow_ComboBox(argKey, _editAcl, desc+":", accSel, aclMap, "", 18, dftHtml) + "\n");
                    }
                    out.write("</table>\n");
                    out.write("</div>\n");

                    /* end of form */
                    out.write("<hr style='margin-bottom:5px;'>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_editRole) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("RoleInfo.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("RoleInfo.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                    } else {
                        out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("RoleInfo.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                    }
                    out.write("</form>\n");
                    
                    /* end of edit/view fields */
                    //out.write("<hr>\n");
                    //out.write("<a href='"+editURL+"'>"+(_editRole?i18n.getString("RoleInfo.cancel","Cancel"):i18n.getString("RoleInfo.back","Back"))+"</a>\n");

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
