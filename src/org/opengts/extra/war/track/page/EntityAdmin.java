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

import org.opengts.extra.tables.*;

public class EntityAdmin
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------
    // Parameters

    // forms 
    public  static final String FORM_ENTITY_SELECT      = "EntityAdminSelect";
    public  static final String FORM_ENTITY_EDIT        = "EntityAdminEdit";
    public  static final String FORM_ENTITY_NEW         = "EntityAdminNew";

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
    public  static final String PARM_NEW_NAME           = "e_newid";
    public  static final String PARM_ENTITY_SELECT      = "e_entity";
    public  static final String PARM_ENTITY_DESC        = "e_desc";
    // --
    public  static final String PARM_RO_TIMESTAMP       = "l_time";
    public  static final String PARM_RO_STATUSCODE      = "l_code";
    public  static final String PARM_RO_ATTACHED        = "l_attch";
    public  static final String PARM_RO_DEVICE_ID       = "l_devid";
    public  static final String PARM_RO_GEOPOINT        = "l_latlon";
    public  static final String PARM_RO_ALTITUDE        = "l_altm";
    public  static final String PARM_RO_DISTANCE        = "l_odom";

    // ------------------------------------------------------------------------
    
    /* properties */
    public  static final String PROP_entityType         = "entityType";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // WebPage interface
    
    public EntityAdmin()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_ENTITY_INFO);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
    }

    // ------------------------------------------------------------------------

    public EntityManager.EntityType getEntityType(RequestProperties reqState)
    {
        String typeStr = this.getProperties().getString(PROP_entityType,null);
        if (StringTools.isBlank(typeStr)) {
            PrivateLabel privLabel = reqState.getPrivateLabel(); // never null
            typeStr = privLabel.getStringProperty(PrivateLabel.PROP_EntityAdmin_entityType,null);
        }
        return EntityManager.getEntityTypeFromName(typeStr);
    }

    public String getEntityName(RequestProperties reqState)
    {
        EntityManager.EntityType type = this.getEntityType(reqState);
        return type.toString(reqState.getLocale());
    }

    // ------------------------------------------------------------------------
   
    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_ADMIN;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(EntityAdmin.class);
        String name = this.getEntityName(reqState);
        return super._getMenuDescription(reqState,i18n.getString("EntityAdmin.editMenuDesc","View/Edit {0} Information",name));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(EntityAdmin.class);
        String name = this.getEntityName(reqState);
        return super._getMenuHelp(reqState,i18n.getString("EntityAdmin.editMenuHelp","View and Edit {0} information",name));
    }
    
    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(EntityAdmin.class);
        String name = this.getEntityName(reqState);
        return super._getNavigationDescription(reqState,i18n.getString("EntityAdmin.navDesc","{0}",name));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(EntityAdmin.class);
        String name = this.getEntityName(reqState);
        return i18n.getString("EntityAdmin.navTab","{0} Admin",name);
    }

    // ------------------------------------------------------------------------

    private static String Filter(String s)
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
        final I18N    i18n       = privLabel.getI18N(EntityAdmin.class);
        final Locale  locale     = reqState.getLocale();
        final Account currAcct   = reqState.getCurrentAccount(); // never null
        final String  currAcctID = currAcct.getAccountID(); // never null
        final User    currUser   = reqState.getCurrentUser(); // may be null
        final String  pageName   = this.getPageName();
        String m = pageMsg;
        boolean error = false;

        /* entity type */
        final EntityManager.EntityType entityType = this.getEntityType(reqState);
        final int entityTypeInt = entityType.getIntValue();
        final String entityName = entityType.toString(locale);

        /* List of entities */
        OrderedSet<String> entityList = null;
        try {
            entityList = Entity.getEntityIDsForAccount(currAcctID);
        } catch (DBException dbe) {
            entityList = new OrderedSet<String>();
        }

        /* selected entities */
        String selEntityID = AttributeTools.getRequestString(reqState.getHttpServletRequest(), PARM_ENTITY_SELECT, "");
        if (!StringTools.isBlank(selEntityID) && !entityList.contains(selEntityID)) {
            selEntityID = "";
        }

        /* Entity db */
        Entity selEntity = null;
        try {
            selEntity = !selEntityID.equals("")? 
                Entity.getEntity(currAcct, selEntityID, entityTypeInt) :  // may still be null
                null;
        } catch (DBException dbe) {
            // ignore
        }

        /* ACL allow edit/view */
        boolean allowNew     = privLabel.hasAllAccess(currUser, this.getAclName());
        boolean allowDelete  = allowNew;
        boolean allowEdit    = allowNew  || privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView    = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());

        /* submit buttons */
        String submitEdit    = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT, "");
        String submitView    = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW, "");
        String submitChange  = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG , "");
        String submitNew     = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW , "");
        String submitDelete  = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL , "");

        /* command */
        String  entityCmd    = reqState.getCommandName();
        boolean selectEntity = entityCmd.equals(COMMAND_INFO_SELECT);
        boolean newEntity    = entityCmd.equals(COMMAND_INFO_NEW);
        boolean updateEntity = entityCmd.equals(COMMAND_INFO_UPDATE);
        boolean deleteEntity = false;

        /* ui display */
        boolean uiList       = false;
        boolean uiEdit       = false;
        boolean uiView       = false;

        /* sub-command */
        String newEntityID = null;
        if (newEntity) {
            if (!allowNew) {
               newEntity = false; // not authorized
            } else {
                newEntityID = AttributeTools.getRequestString(request,PARM_NEW_NAME,"").trim();
                newEntityID = newEntityID.toLowerCase();
                if (StringTools.isBlank(newEntityID)) {
                    m = i18n.getString("EntityAdmin.enterNewID","Please enter a valid new {0} ID.",entityName);
                    error = true;
                    newEntity = false;
                } else
                if (!WebPageAdaptor.isValidID(reqState,newEntityID)) {
                    m = i18n.getString("EntityAdmin.invalidIDChar","ID contains invalid characters");
                    error = true;
                    newEntity = false;
                }
            }
        } else
        if (updateEntity) {
            if (!allowEdit) {
                // not authorized to update entities
                updateEntity = false;
            } else
            if (!SubmitMatch(submitChange,i18n.getString("EntityAdmin.change","Change"))) {
                updateEntity = false;
            } else
            if (selEntity == null) {
                // should not occur
                m = i18n.getString("EntityAdmin.unableToUpdate","Unable to update {0}, ID not found",entityName);
                error = true;
                updateEntity = false;
            }
        } else
        if (selectEntity) {
            if (SubmitMatch(submitDelete,i18n.getString("EntityAdmin.delete","Delete"))) {
                if (!allowDelete) {
                    deleteEntity = false;
                } else
                if (selEntity == null) {
                    m = i18n.getString("EntityAdmin.pleaseSelectEntity","Please select {0}",entityName);
                    error = true;
                    deleteEntity = false; // not selected
                } else {
                    deleteEntity = true;
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("EntityAdmin.edit","Edit"))) {
                if (!allowEdit) {
                    uiEdit = false; // not authorized
                } else
                if (selEntity == null) {
                    m = i18n.getString("EntityAdmin.pleaseSelectEntity","Please select {0}",entityName);
                    error = true;
                    uiEdit = false; // not selected
                } else {
                    uiEdit = true;
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("EntityAdmin.view","View"))) {
                if (!allowView) {
                    uiView = false; // not authorized
                } else
                if (selEntity == null) {
                    m = i18n.getString("EntityAdmin.pleaseSelectEntity","Please select {0}",entityName);
                    error = true;
                    uiView = false; // not selected
                } else {
                    uiView = true;
                }
            } else {
                uiList = true;
            }
        } else {
            uiList = true;
        }

        /* delete Entity? */
        if (deleteEntity) {
            try {
                Entity.Key entityKey = (Entity.Key)selEntity.getRecordKey();
                Print.logWarn("Deleting Entity: " + entityKey);
                entityKey.delete(true); // will also delete dependencies
                selEntityID = "";
                selEntity = null;
                // select another entity
                entityList = Entity.getEntityIDsForAccount(currAcctID);
                if (!ListTools.isEmpty(entityList)) {
                    selEntityID = entityList.get(0);
                    try {
                        selEntity = !selEntityID.equals("")? 
                            Entity.getEntity(currAcct, selEntityID, entityTypeInt) :  // may still be null
                            null;
                    } catch (DBException dbe) {
                        // ignore
                    }
                }
            } catch (DBException dbe) {
                Print.logException("Deleting Entity", dbe);
                m = i18n.getString("EntityAdmin.errorDelete","Internal error deleting {0}",entityName);
                error = true;
            }
            uiList = true;
        }

        /* new Entity? */
        if (newEntity) {
            boolean createEntityOK = true;
            for (int u = 0; u < entityList.size(); u++) {
                if (newEntityID.equalsIgnoreCase(entityList.get(u))) {
                    m = i18n.getString("EntityAdmin.alreadyExists","This {0} already exists",entityName);
                    error = true;
                    createEntityOK = false;
                    break;
                }
            }
            if (createEntityOK) {
                try {
                    Entity entity = Entity.createNewEntity(currAcct, newEntityID, entityTypeInt, ""); // already saved
                    entityList = Entity.getEntityIDsForAccount(currAcctID);
                    selEntity = entity;
                    selEntityID = entity.getEntityID();
                    Print.logInfo("Created Entity '%s'", selEntityID);
                    m = i18n.getString("EntityAdmin.createdEntity","New {0} has been created",entityName);
                } catch (DBException dbe) {
                    Print.logException("Creating Entity", dbe);
                    m = i18n.getString("EntityAdmin.errorCreate","Internal error creating {0}",entityName);
                    error = true;
                }
            }
            uiList = true;
        }

        /* change/update the Entity info? */
        if (updateEntity) {
            selEntity.clearChanged();
            String entityDesc    = AttributeTools.getRequestString(request, PARM_ENTITY_DESC   ,"");
            try {
                boolean saveOK = true;
                // description
                if (!entityDesc.equals(selEntity.getDescription())) {
                    selEntity.setDescription(entityDesc);
                }
                // save
                if (saveOK) {
                    selEntity.save();
                    m = i18n.getString("EntityAdmin.entityUpdated","{0} information updated",entityName);
                } else {
                    // should stay on this page
                }
            } catch (Throwable t) {
                Print.logException("Updating Entity", t);
                m = i18n.getString("EntityAdmin.errorUpdate","Internal error updating {0}",entityName);
                error = true;
                //return;
            }
            uiList = true;
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = EntityAdmin.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "EntityAdmin.css", cssDir);
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
        final OrderedSet<String> _entityList = entityList;
        final Entity     _selEntity     = selEntity;
        final boolean    _allowEdit     = allowEdit;
        final boolean    _allowView     = allowView;
        final boolean    _allowDelete   = allowDelete;
        final boolean    _allowNew      = allowNew;
        final boolean    _uiEdit        = _allowEdit && uiEdit;
        final boolean    _uiView        = _uiEdit || uiView;
        final boolean    _uiList        = uiList || (!_uiEdit && !_uiView);
        HTMLOutput HTML_CONTENT = null;
        if (_uiList) {
            final String _selEntityID = (selEntityID.equals("") && (entityList.size() > 0))? entityList.get(0) : selEntityID;

            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {
                    String pageName = EntityAdmin.this.getPageName();

                    // frame header
                  //String menuURL    = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                    String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                    String editURL    = EntityAdmin.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String selectURL  = EntityAdmin.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String newURL     = EntityAdmin.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String frameTitle = _allowEdit? 
                        i18n.getString("EntityAdmin.viewEditEntity","View/Edit {0} Information",entityName) : 
                        i18n.getString("EntityAdmin.viewEntity","View {0} Information",entityName);
                    out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                    out.write("<hr>\n");
                    
                    // EntityAdmin selection table (Select, EntityAdmin ID, EntityAdmin Name)
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("EntityAdmin.selectEntity","Select {0}",entityName)+":</h1>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form name='"+FORM_ENTITY_SELECT+"' method='post' action='"+selectURL+"' target='_top'>");
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SELECT+"'/>");
                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+"' cellspacing=0 cellpadding=0 border=0>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"' nowrap>"+i18n.getString("EntityAdmin.select","Select")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("EntityAdmin.entityID","{0} ID",entityName)+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("EntityAdmin.description","Description")+"</th>\n");
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    out.write(" <tbody>\n");
                    for (int u = 0; u < _entityList.size(); u++) {
                        String entid = _entityList.get(u);
                        if ((u & 1) == 0) {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD+"'>\n");
                        } else {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN+"'>\n");
                        }
                        try {
                            Entity entity = Entity.getEntity(currAcct, entid, entityTypeInt);
                            if (entity != null) {
                                String entityID   = entity.getEntityID();
                                String entityDesc = Filter(entity.getDescription());
                                String checked    = _selEntityID.equals(entity.getEntityID())? "checked" : "";
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+u+"'><input type='radio' name='"+PARM_ENTITY_SELECT+"' id='"+entityID+"' value='"+entityID+"' "+checked+"></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+entityID+"'>"+entityID+"</label></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+entityDesc+"</td>\n");
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
                        out.write("<input type='submit' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("EntityAdmin.view","View")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowEdit  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("EntityAdmin.edit","Edit")+"'>");
                        out.write("</td>\n"); 
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_DEL+"' value='"+i18n.getString("EntityAdmin.delete","Delete")+"' "+Onclick_ConfirmDelete(locale)+">");
                    } else {
                        out.write("&nbsp;"); 
                    }
                    out.write("</td>\n"); 
                    out.write("</tr>\n");
                    out.write("</table>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");
                    
                    /* new Entity */
                    if (_allowNew) {
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("EntityAdmin.createNewEntity","Create a new {0}",entityName)+":</h1>\n");
                    out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<form name='"+FORM_ENTITY_NEW+"' method='post' action='"+newURL+"' target='_self'>"); // target='_top'
                    out.write(" <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW+"'/>");
                    out.write(i18n.getString("EntityAdmin.entityID","{0} ID",entityName)+": <input type='text' class='"+CommonServlet.CSS_TEXT_INPUT+"' name='"+PARM_NEW_NAME+"' value='' size='32' maxlength='32'><br>\n");
                    out.write(" <input type='submit' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("EntityAdmin.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");
                    }
                    
                }
            };

        } else
        if (_uiEdit || _uiView) {
            final String _selEntityID = selEntityID;

            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {
                    String pageName = EntityAdmin.this.getPageName();
    
                    // frame header
                  //String menuURL    = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                    String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                    String editURL    = EntityAdmin.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String selectURL  = EntityAdmin.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String newURL     = EntityAdmin.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String frameTitle = _allowEdit? 
                        i18n.getString("EntityAdmin.viewEditEntity","View/Edit {0} Information",entityName) : 
                        i18n.getString("EntityAdmin.viewEntity","View {0} Information",entityName);
                    out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                    out.write("<hr>\n");

                    /* start of form */
                    out.write("<form name='"+FORM_ENTITY_EDIT+"' method='post' action='"+editURL+"' target='_top'>\n");
                    out.write("  <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPDATE+"'/>\n");

                    /* Entity fields */
                    out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");

                    out.println(FormRow_TextField(PARM_ENTITY_SELECT  , false  , i18n.getString("EntityAdmin.entityID","{0} ID",entityName)+":"    , _selEntityID, 8, 8));
                    out.println(FormRow_TextField(PARM_ENTITY_DESC    , _uiEdit, i18n.getString("EntityAdmin.entityName","{0} Name",entityName)+":", (_selEntity!=null)?_selEntity.getDescription()  :"", 50, 80));

                    out.println(FormRow_Separator());
                    String lastEventTime = reqState.formatDateTime(((_selEntity!=null)?_selEntity.getTimestamp():0L),"--");
                    out.println(FormRow_TextField(PARM_RO_TIMESTAMP   , false  , i18n.getString("EntityAdmin.lastEventTime" ,"Last Event Time")+":"  , lastEventTime , 50, 50));
                    String lastStatusCode = StatusCodes.GetDescription(((_selEntity!=null)?_selEntity.getStatusCode():0),privLabel);
                    out.println(FormRow_TextField(PARM_RO_STATUSCODE  , false  , i18n.getString("EntityAdmin.lastStatusCode","Last Status Code")+":" , lastStatusCode, 30, 30));
                    String lastAttached = ComboOption.getYesNoText(locale,((_selEntity!=null)?_selEntity.getIsAttached():false));
                    out.println(FormRow_TextField(PARM_RO_ATTACHED    , false  , i18n.getString("EntityAdmin.lastAttached"  ,"Is Attached")+":"      , lastAttached  , 10, 10));
                    String lastDeviceID = (_selEntity!=null)? _selEntity.getDeviceID() : "";
                    out.println(FormRow_TextField(PARM_RO_DEVICE_ID   , false  , i18n.getString("EntityAdmin.lastDeviceID"  ,"Attached DeviceID")+":", lastDeviceID  , 20, 20));
                    GeoPoint lastGeoPoint = (_selEntity!=null)? _selEntity.getGeoPoint() : null;
                    String lastLocation = GeoPoint.isValid(lastGeoPoint)? lastGeoPoint.toString() : "";
                    out.println(FormRow_TextField(PARM_RO_GEOPOINT    , false  , i18n.getString("EntityAdmin.lastLocation"  ,"Last Location")+":"    , lastLocation  , 25, 25));
                    String lastAltitude = (_selEntity!=null)? String.valueOf(_selEntity.getAltitude()) : "";
                    out.println(FormRow_TextField(PARM_RO_ALTITUDE    , false  , i18n.getString("EntityAdmin.lastAltitude"  ,"Last Altitude")+":"    , lastAltitude  , 10, 10));
                    String lastDistance = (_selEntity!=null)? String.valueOf(_selEntity.getOdometerKM()) : "";
                    out.println(FormRow_TextField(PARM_RO_DISTANCE    , false  , i18n.getString("EntityAdmin.lastDistance"  ,"Distance Traveled")+":", lastDistance  , 12, 12));

                    out.println("</table>");

                    /* end of form */
                    out.write("<hr style='margin-bottom:5px;'>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_uiEdit) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("EntityAdmin.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("EntityAdmin.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                    } else {
                        out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("EntityAdmin.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
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
