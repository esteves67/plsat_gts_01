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
package org.opengts.rulewar.track.page;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.geocoder.GeocodeProvider;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

// assume that GeoCorridor table is available
import org.opengts.rule.tables.GeoCorridor;

public class CorridorInfo
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------
    // 'private.xml' properties

    // PrivateLabel.PROP_CorridorInfo_mapControlLocation
    private static final String CONTROLS_ON_LEFT[]              = new String[] { "left", "true" };

    // ------------------------------------------------------------------------

    private static final double MIN_RADIUS_METERS               = GeoCorridor.MIN_RADIUS_METERS;
    private static final double MAX_RADIUS_METERS               = GeoCorridor.MAX_RADIUS_METERS;
    
    private static final double DEFAULT_ZONE_RADIUS             = 20000.0;

    // ------------------------------------------------------------------------
    // Parameters
    
    // CSS class 
    public  static final String CSS_LATLON_INPUT                = "latlonInput";
    
    // forms 
    public  static final String FORM_ZONE_SELECT                = "CorridorInfoSelect";
    public  static final String FORM_ZONE_EDIT                  = "CorridorInfoEdit";
    public  static final String FORM_ZONE_NEW                   = "CorridorInfoNew";

    // commands
    public  static final String COMMAND_INFO_UPDATE             = "update";
    public  static final String COMMAND_INFO_SELECT             = "select";
    public  static final String COMMAND_INFO_NEW                = "new";

    // submit
    public  static final String PARM_SUBMIT_EDIT                = "z_subedit";
    public  static final String PARM_SUBMIT_VIEW                = "z_subview";
    public  static final String PARM_SUBMIT_CHG                 = "z_subchg";
    public  static final String PARM_SUBMIT_DEL                 = "z_subdel";
    public  static final String PARM_SUBMIT_NEW                 = "z_subnew";

    // buttons
    public  static final String PARM_BUTTON_CANCEL              = "u_btncan";
    public  static final String PARM_BUTTON_BACK                = "u_btnbak";

    // parameters
    public  static final String PARM_NEW_ID                     = "z_newid";

    // parameters
    public  static final String PARM_ZONE_SELECT                = "z_zone";
    public  static final String PARM_ZONE_DESC                  = "z_desc";
    public  static final String PARM_ZONE_RADIUS                = "z_radius";
    public  static final String PARM_ZONE_INDEX                 = "z_index";
  //public  static final String PARM_ZONE_COLOR                 = "z_color";

    // ------------------------------------------------------------------------

    private static final int    CORRIDOR_POINT_COUNT            = 15;
    
    public  static final String PARM_ZONE_LATITUDE_             = "z_lat";
    private static String PARM_ZONE_LATITUDE(int ndx)
    {
        return PARM_ZONE_LATITUDE_ + ndx;
    }
    
    public  static final String PARM_ZONE_LONGITUDE_            = "z_lon";
    public static final String  PARM_ZONE_LONGITUDE(int ndx)
    {
        return PARM_ZONE_LONGITUDE_ + ndx;
    }
    
    // point index
    private static final int    DEFAULT_POINT_INDEX             = 0;

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public CorridorInfo()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_CORRIDOR_INFO);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
    }

    // ------------------------------------------------------------------------

    public boolean getIsEnabled()
    {
        boolean hasRule = DBConfig.hasRulePackage();
        if (!hasRule) {
            Print.logWarn("CorridorInfo present, but Rules Engine not found");
        }
        return hasRule;
    }

    // ------------------------------------------------------------------------
   
    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_ADMIN;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(CorridorInfo.class);
        return super._getMenuDescription(reqState,i18n.getString("CorridorInfo.editMenuDesc","View/Edit GeoCorridor Information"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(CorridorInfo.class);
        return super._getMenuHelp(reqState,i18n.getString("CorridorInfo.editMenuHelp","View and Edit GeoCorridor information"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(CorridorInfo.class);
        return super._getNavigationDescription(reqState,i18n.getString("CorridorInfo.navDesc","GeoCorridor"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(CorridorInfo.class);
        return super._getNavigationTab(reqState,i18n.getString("CorridorInfo.navTab","GeoCorridor Admin"));
    }

    // ------------------------------------------------------------------------

    /*
    private static ComboMap GetColorComboMap(I18N i18n)
    {
        ComboMap cc = new ComboMap();
        cc.add(""                                   ,i18n.getString("CorridorInfo.color.default","Default"));
        cc.add(ColorTools.BLACK.toString(true)      ,i18n.getString("CorridorInfo.color.black"  ,"Black"  ));
        cc.add(ColorTools.BROWN.toString(true)      ,i18n.getString("CorridorInfo.color.brown"  ,"Brown"  ));
        cc.add(ColorTools.RED.toString(true)        ,i18n.getString("CorridorInfo.color.red"    ,"Red"    ));
        cc.add(ColorTools.ORANGE.toString(true)     ,i18n.getString("CorridorInfo.color.orange" ,"Orange" ));
        cc.add(ColorTools.YELLOW.toString(true)     ,i18n.getString("CorridorInfo.color.yellow" ,"Yellow" ));
        cc.add(ColorTools.GREEN.toString(true)      ,i18n.getString("CorridorInfo.color.green"  ,"Green"  ));
        cc.add(ColorTools.BLUE.toString(true)       ,i18n.getString("CorridorInfo.color.blue"   ,"Blue"   ));
        cc.add(ColorTools.PURPLE.toString(true)     ,i18n.getString("CorridorInfo.color.purple" ,"Purple" ));
        cc.add(ColorTools.DARK_GRAY.toString(true)  ,i18n.getString("CorridorInfo.color.gray"   ,"Gray"   ));
        cc.add(ColorTools.WHITE.toString(true)      ,i18n.getString("CorridorInfo.color.white"  ,"White"  ));
        cc.add(ColorTools.CYAN.toString(true)       ,i18n.getString("CorridorInfo.color.cyan"   ,"Cyan"   ));
        cc.add(ColorTools.PINK.toString(true)       ,i18n.getString("CorridorInfo.color.pink"   ,"Pink"   ));
        return cc;
    }
    */
    
    private static java.util.List<GeoPoint> GetCorridorPoints(GeoCorridor corr)
    {
        java.util.List<GeoPoint> gpList = null;
        try {
            gpList = (corr != null)? corr.getGeoPoints() : null;
        } catch (DBException dbe) {
            gpList = null;
        }
        return (gpList != null)? gpList : new Vector<GeoPoint>();
    }

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel = reqState.getPrivateLabel(); // never null
        final I18N    i18n     = privLabel.getI18N(CorridorInfo.class);
        final Locale  locale   = reqState.getLocale();
        final Account currAcct = reqState.getCurrentAccount(); // never null
        final User    currUser = reqState.getCurrentUser(); // may be null
        final String  pageName = this.getPageName();
        String  m = pageMsg;
        boolean error = false;

        /* list of GeoCorridors */
        String zoneList[] = null;
        try {
            zoneList = GeoCorridor.getCorridorIDsForAccount(currAcct.getAccountID());
        } catch (DBException dbe) {
            zoneList = new String[0];
        }

        /* selected GeoCorridor */
        String selZoneID = AttributeTools.getRequestString(reqState.getHttpServletRequest(), PARM_ZONE_SELECT, "");
        if (StringTools.isBlank(selZoneID)) {
            if ((zoneList.length > 0) && (zoneList[0] != null)) {
                selZoneID = zoneList[0];
            } else {
                selZoneID = "";
            }
            //Print.logWarn("No Zone selected, choosing first zone: %s", selZoneID);
        }
        if (zoneList.length == 0) {
            zoneList = new String[] { selZoneID };
        }
        
        /* GeoCorridor db */
        GeoCorridor selZone = null;
        try {
            selZone = !selZoneID.equals("")? GeoCorridor.getGeoCorridor(currAcct,selZoneID,false) : null;
        } catch (DBException dbe) {
            // ignore
        }

        /* ACL */
        boolean allowNew    = privLabel.hasAllAccess(currUser, this.getAclName());
        boolean allowDelete = allowNew;
        boolean allowEdit   = allowNew || privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView   = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());

        /* command */
        String zoneCmd      = reqState.getCommandName();
        boolean listZones   = false;
        boolean updateZone  = zoneCmd.equals(COMMAND_INFO_UPDATE);
        boolean selectZone  = zoneCmd.equals(COMMAND_INFO_SELECT);
        boolean newZone     = zoneCmd.equals(COMMAND_INFO_NEW);
        boolean deleteZone  = false;
        boolean editZone    = false;
        boolean viewZone    = false;
        
        /* submit buttons */
        String submitEdit   = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT, "");
        String submitView   = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW, "");
        String submitChange = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG , "");
        String submitNew    = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW , "");
        String submitDelete = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL , "");

        /* MapProvider support */
        final MapProvider mapProvider = reqState.getMapProvider(); // check below to make sure this is not null
        final boolean mapSupportsCursorLocation = ((mapProvider != null) && mapProvider.isFeatureSupported(MapProvider.FEATURE_LATLON_DISPLAY));
        final boolean mapSupportsDistanceRuler  = ((mapProvider != null) && mapProvider.isFeatureSupported(MapProvider.FEATURE_DISTANCE_RULER));
        final boolean mapSupportsCorridors      = ((mapProvider != null) && mapProvider.isFeatureSupported(MapProvider.FEATURE_CORRIDORS));

        /* sub-command */
        String newZoneID = null;
        if (newZone) {
            if (!allowNew) {
                // not authorized to create new GeoCorridors
                Print.logInfo("Not authorized to create a new GeoCorridor ...");
                newZone = false;
            } else {
                HttpServletRequest httpReq = reqState.getHttpServletRequest();
                newZoneID   = AttributeTools.getRequestString(httpReq,PARM_NEW_ID,"").trim().toLowerCase();
                if (StringTools.isBlank(newZoneID)) {
                    m = i18n.getString("CorridorInfo.enterNewZone","Please enter a new GeoCorridor name.");
                    error = true;
                    newZone = false;
                } else
                if (!WebPageAdaptor.isValidID(reqState,/*PrivateLabel.PROP_CorridorInfo_validateNewIDs,*/newZoneID)) {
                    m = i18n.getString("CorridorInfo.invalidIDChar","ID contains invalid characters");
                    error = true;
                    newZone = false;
                }
            }
        } else
        if (updateZone) {
            if (!allowEdit) {
                // not authorized to update GeoCorridors
                updateZone = false;
            } else
            if (!SubmitMatch(submitChange,i18n.getString("CorridorInfo.change","Change"))) {
                updateZone = false;
            }
        } else
        if (selectZone) {
            if (SubmitMatch(submitDelete,i18n.getString("CorridorInfo.delete","Delete"))) {
                if (allowDelete) {
                    deleteZone = true;
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("CorridorInfo.edit","Edit"))) {
                if (allowEdit) {
                    if (selZone == null) {
                        m = i18n.getString("CorridorInfo.pleaseSelectCorridor","Please select a GeoCorridor");
                        error = true;
                        listZones = true;
                    } else {
                        editZone = true;
                        viewZone = true;
                    }
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("CorridorInfo.view","View"))) {
                if (allowView) {
                    if (selZone == null) {
                        m = i18n.getString("CorridorInfo.pleaseSelectCorridor","Please select a GeoCorridor");
                        error = true;
                        listZones = true;
                    } else {
                        viewZone = true;
                    }
                }
            } else {
                listZones = true;
            }
        } else {
            listZones = true;
        }

        /* delete GeoCorridor? */
        if (deleteZone) {
            if (selZone == null) {
                m = i18n.getString("CorridorInfo.pleaseSelectCorridor","Please select a GeoCorridor");
                error = true;
            } else {
                try {
                    GeoCorridor.Key zoneKey = (GeoCorridor.Key)selZone.getRecordKey();
                    Print.logWarn("Deleting GeoCorridor: " + zoneKey);
                    zoneKey.delete(true); // will also delete dependencies
                    selZoneID = "";
                    selZone = null;
                    zoneList = GeoCorridor.getCorridorIDsForAccount(currAcct.getAccountID());
                    if ((zoneList != null) && (zoneList.length > 0)) {
                        selZoneID = zoneList[0];
                        try {
                            selZone = !selZoneID.equals("")? GeoCorridor.getGeoCorridor(currAcct,selZoneID,false) : null;
                        } catch (DBException dbe) {
                            // ignore
                        }
                    }
                } catch (DBException dbe) {
                    Print.logException("Deleting GeoCorridor", dbe);
                    m = i18n.getString("CorridorInfo.errorDelete","Internal error deleting GeoCorridor");
                    error = true;
                }
            }
            listZones = true;
        }

        /* new GeoCorridor? */
        if (newZone) {
            boolean createZoneOK = true;
            //Print.logInfo("Creating new GeoCorridor: %s", newZoneID);
            for (int u = 0; u < zoneList.length; u++) {
                if (newZoneID.equalsIgnoreCase(zoneList[u])) {
                    m = i18n.getString("CorridorInfo.alreadyExists","This GeoCorridor already exists");
                    error = true;
                    createZoneOK = false;
                    break;
                }
            }
            if (createZoneOK) {
                try {
                    GeoCorridor zone = GeoCorridor.getGeoCorridor(currAcct, newZoneID, true); // create
                    zone.setRadius(1000); // meters
                    zone.save(); // needs to be saved to be created
                    zoneList = GeoCorridor.getCorridorIDsForAccount(currAcct.getAccountID());
                    selZone = zone;
                    selZoneID = selZone.getCorridorID();
                    m = i18n.getString("CorridorInfo.createdZone","New GeoCorridor has been created");
                } catch (DBException dbe) {
                    Print.logException("Error Creating GeoCorridor", dbe);
                    m = i18n.getString("CorridorInfo.errorCreate","Internal error creating GeoCorridor");
                    error = true;
                }
            }
            listZones = true;
        }

        /* change/update the GeoCorridor info? */
        if (updateZone) {
            long    zoneRadius     = StringTools.parseLong(AttributeTools.getRequestString(request,PARM_ZONE_RADIUS,null),100L);
          //String  zoneColor      = AttributeTools.getRequestString(request,PARM_ZONE_COLOR,null);
            String  zoneDesc       = AttributeTools.getRequestString(request,PARM_ZONE_DESC,"");
            //Print.logInfo("Updating Zone: %s - %s", selZoneID, zoneDesc);
            try {
                 if (selZone != null) {
                     boolean saveOK = true;
                     // Radius (meters)
                     if (zoneRadius > 0L) {
                         selZone.setRadius((int)zoneRadius);
                     }
                     // description
                     /*
                     if (!StringTools.isBlank(zoneColor)) {
                         selZone.setShapeColor(zoneColor);
                     }
                     */
                     // GeoPoints
                     selZone.clearGeoPoints();
                     int pointCount = privLabel.getIntProperty(PrivateLabel.PROP_CorridorInfo_pointCount,CORRIDOR_POINT_COUNT);
                     java.util.List<GeoPoint> gpList = new Vector<GeoPoint>();
                     for (int z = 0, p = 0; z < pointCount; z++) {
                         double zoneLat = StringTools.parseDouble(AttributeTools.getRequestString(request,PARM_ZONE_LATITUDE (z),null),0.0);
                         double zoneLon = StringTools.parseDouble(AttributeTools.getRequestString(request,PARM_ZONE_LONGITUDE(z),null),0.0);
                         if (GeoPoint.isValid(zoneLat,zoneLon)) {
                             gpList.add(new GeoPoint(zoneLat, zoneLon));
                         }
                     }
                     selZone.setGeoPoints(gpList);
                     // description
                     if (!StringTools.isBlank(zoneDesc)) {
                         selZone.setDescription(zoneDesc);
                     }
                     // save
                     if (saveOK) {
                         selZone.save();
                         m = i18n.getString("CorridorInfo.corridorUpdated","GeoCorridor information updated");
                     } else {
                         // error occurred, should stay on this page
                         editZone = true;
                     }
                 } else {
                     m = i18n.getString("CorridorInfo.noCorridors","There are currently no defined GeoCorridors for this Account.");
                 }
            } catch (Throwable t) {
                Print.logException("Updating GeoCorridor", t);
                m = i18n.getString("CorridorInfo.errorUpdate","Internal error updating GeoCorridor");
                error = true;
            }
            listZones = true;
        }

        /* final vars */
        final String      _selZoneID   = selZoneID;
        final GeoCorridor _selZone     = selZone;
        final String      _zoneList[]  = zoneList;
        final boolean     _allowEdit   = allowEdit;
        final boolean     _allowView   = allowView;
        final boolean     _allowNew    = allowNew;
        final boolean     _allowDelete = allowDelete;
        final boolean     _editZone    = _allowEdit && editZone;
        final boolean     _viewZone    = _editZone || viewZone;
        final boolean     _listZones   = listZones || (!_editZone && !_viewZone);

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = CorridorInfo.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "ZoneInfo.css", cssDir);
                WebPageAdaptor.writeCssLink(out, reqState, "CorridorInfo.css", cssDir);
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(SORTTABLE_JS), request);
                if (!_listZones && mapSupportsCorridors) {

                    // MapProvider JavaScript
                    if (mapProvider != null) {
                        mapProvider.writeJavaScript(out, reqState);
                    }

                    /* start JavaScript */
                    JavaScriptTools.writeStartJavaScript(out);

                    // GeoCorridor Javascript
                    double radiusMeters = DEFAULT_ZONE_RADIUS;
                    String zoneColor = ColorTools.GREEN.toString(true);
                    if (_selZone != null) {
                        //zoneColor    = _selZone.getShapeColor();
                        radiusMeters = _selZone.getRadiusMeters(MIN_RADIUS_METERS,MAX_RADIUS_METERS);
                    }
                    MapDimension mapDim = (mapProvider != null)? mapProvider.getZoneDimension() : new MapDimension(0,0);
                    out.println("// GeoCorridor vars");
                    out.println("jsvGeozoneMode = true;");
                    out.println("MAP_WIDTH  = " + mapDim.getWidth()  + ";");
                    out.println("MAP_HEIGHT = " + mapDim.getHeight() + ";");

                    JavaScriptTools.writeJSVar(out, "DEFAULT_ZONE_RADIUS", DEFAULT_ZONE_RADIUS);
                    JavaScriptTools.writeJSVar(out, "jsvZoneEditable"    , _editZone);
                    JavaScriptTools.writeJSVar(out, "jsvShowVertices"    , true);
                    JavaScriptTools.writeJSVar(out, "jsvZoneType"        , Geozone.GeozoneType.SWEPT_POINT_RADIUS.getIntValue());
                    JavaScriptTools.writeJSVar(out, "jsvZoneRadiusMeters", radiusMeters);
                    JavaScriptTools.writeJSVar(out, "jsvZoneColor"       , zoneColor);

                    int pointCount = privLabel.getIntProperty(PrivateLabel.PROP_CorridorInfo_pointCount,CORRIDOR_POINT_COUNT);
                    java.util.List<GeoPoint> gpList = GetCorridorPoints(_selZone);
                    out.write("// GeoCorridor points\n");
                    JavaScriptTools.writeJSVar(out, "jsvZoneCount"       , pointCount);
                    JavaScriptTools.writeJSVar(out, "jsvZoneIndex"       , DEFAULT_POINT_INDEX);
                    out.write("var jsvZoneList = new Array(\n"); // consistent with JSMapPoint
                    for (int z = 0; z < pointCount; z++) {
                        GeoPoint gp = ((gpList != null) && (z < gpList.size()))? gpList.get(z) : null;
                        if (gp == null) { gp = GeoPoint.INVALID_GEOPOINT; }
                        out.write("    { lat:" + gp.getLatitude() + ", lon:" + gp.getLongitude() + " }");
                        if ((z+1) < pointCount) { out.write(","); }
                        out.write("\n");
                    }
                    out.write("    );\n");

                    /* end JavaScript */
                    JavaScriptTools.writeEndJavaScript(out);

                    /* GeoCorridor.js */
                    JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("Geozone.js"), request);

                }
            }
        };

        /* Content */
        final boolean mapControlsOnLeft = 
            ListTools.containsIgnoreCase(CONTROLS_ON_LEFT,privLabel.getStringProperty(PrivateLabel.PROP_CorridorInfo_mapControlLocation,""));
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName = CorridorInfo.this.getPageName();

                // frame header
                String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                String editURL    = CorridorInfo.this.encodePageURL(reqState);
                String selectURL  = CorridorInfo.this.encodePageURL(reqState);
                String newURL     = CorridorInfo.this.encodePageURL(reqState);

                if (_listZones) {
                    
                    // GeoCorridor selection table (Select, GeoCorridor ID, Zone Name)
                    String frameTitle = _allowEdit? 
                        i18n.getString("CorridorInfo.list.viewEditZone","View/Edit GeoCorridor Information") : 
                        i18n.getString("CorridorInfo.list.viewZone","View GeoCorridor Information");
                    out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                    out.write("<hr>\n");

                    // GeoCorridor selection table (Select, Zone ID, Zone Name)
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+FilterText(i18n.getString("CorridorInfo.list.selectZone","Select a GeoCorridor"))+":</h1>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form name='"+FORM_ZONE_SELECT+"' method='post' action='"+selectURL+"' target='_self'>"); // target='_top'
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SELECT+"'/>");
                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+"' cellspacing=0 cellpadding=0 border=0>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"'>"+FilterText(i18n.getString("CorridorInfo.list.select","Select"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("CorridorInfo.list.zoneID","GeoCorridor ID"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("CorridorInfo.list.description","Description"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("CorridorInfo.list.radiusMeters","Radius\n(meters)"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("CorridorInfo.list.startPoint","Starting\nLatitude/Longitude"))+"</th>\n");
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    
                    /* GeoCorridor list */
                    out.write(" <tbody>\n");
                    for (int z = 0, r = 0; z < _zoneList.length; z++) {
                        
                        /* get GeoCorridor */
                        GeoCorridor zone = null;
                        try {
                            zone = GeoCorridor.getGeoCorridor(currAcct, _zoneList[z], false);
                        } catch (DBException dbe) {
                            // error
                        }
                        if (zone == null) {
                            continue; // skip 
                        }
                        
                        /* get GeoPoints */
                        java.util.List<GeoPoint> gpList = GetCorridorPoints(zone);

                        /* GeoCorridor vars */
                        String zoneID      = FilterText(zone.getCorridorID());
                        String zoneDesc    = FilterText(zone.getDescription());
                        String zoneRadius  = String.valueOf(zone.getRadius());
                        GeoPoint firstPt   = !ListTools.isEmpty(gpList)? gpList.get(0) : null;
                        if (firstPt == null) { firstPt = new GeoPoint(0.0, 0.0); }
                        String zoneCenter  = firstPt.getLatitudeString(GeoPoint.SFORMAT_DEC_5,null) + " "+GeoPoint.PointSeparator+" " + firstPt.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null);
                        String checked     = _selZoneID.equals(zone.getCorridorID())? "checked" : "";
                        String styleClass  = ((r++ & 1) == 0)? CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD : CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN;

                        out.write("  <tr class='" + styleClass + "'>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+z+"'>");
                        out.write(      "<input type='radio' name='"+PARM_ZONE_SELECT+"' id='"+zoneID+"' value='"+zoneID+"' "+checked+">");
                        out.write(      "</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+zoneID+"'>"+zoneID+"</label></td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+zoneDesc+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+zoneRadius+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+zoneCenter+"</td>\n");
                        out.write("  </tr>\n");

                    }
                    out.write(" </tbody>\n");
                    out.write("</table>\n");
                    out.write("<table cellpadding='0' cellspacing='0' border='0' style='width:95%; margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<tr>\n");
                    if (_allowView  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("CorridorInfo.list.view","View")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowEdit  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("CorridorInfo.list.edit","Edit")+"'>");
                        out.write("</td>\n"); 
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) { 
                        out.write("<input type='submit' name='"+PARM_SUBMIT_DEL+"' value='"+i18n.getString("CorridorInfo.list.delete","Delete")+"' "+Onclick_ConfirmDelete(locale)+">");
                    } else {
                        out.write("&nbsp;"); 
                    }
                    out.write("</td>\n"); 
                    out.write("</tr>\n");
                    out.write("</table>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");

                    /* new GeoCorridor */
                    if (_allowNew) {
                        out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+FilterText(i18n.getString("CorridorInfo.list.createNewZone","Create a new GeoCorridor"))+":</h1>\n");
                        out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                        out.write("<form name='"+FORM_ZONE_NEW+"' method='post' action='"+newURL+"' target='_self'>"); // target='_top'
                        out.write(" <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW+"'/>");
                        out.write(FilterText(i18n.getString("CorridorInfo.list.zoneID","GeoCorridor ID"))+": <input type='text' class='"+CommonServlet.CSS_TEXT_INPUT+"' name='"+PARM_NEW_ID+"' value='' size='32' maxlength='32'>");
                        out.write("<br>\n");
                        out.write(" <input type='submit' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("CorridorInfo.list.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                        out.write("</form>\n");
                        out.write("</div>\n");
                        out.write("<hr>\n");
                    }

                } else {
                    // view/edit
                    
                    // begin form
                    out.println("<form name='"+FORM_ZONE_EDIT+"' method='post' action='"+editURL+"' target='_self'>"); // target='_top'
                    
                    // GeoCorridor view/edit form
                    out.write("<table cellspacing='0' cellpadding='0' border='0'><tr>\n");
                    out.write("<td nowrap>");
                    String frameTitle = _editZone? 
                        i18n.getString("CorridorInfo.map.editZone","Edit GeoCorridor") : 
                        i18n.getString("CorridorInfo.map.viewZone","View GeoCorridor");
                    out.print  ("<span style='font-size:9pt; font-weight:bold;'>"+frameTitle+" &nbsp;</span>");
                    out.print  (Form_TextField(PARM_ZONE_SELECT, false, _selZoneID, 16, 20));
                    out.write("</td>");
                    out.write("<td nowrap style=\"width:100%; text-align:right;\">");
                    //out.println("<span style='width:100%;'>&nbsp;</span>");  <-- causes IE to NOT display the following description
                    String i18nAddressTooltip = i18n.getString("CorridorInfo.map.description.tooltip", "This description is used for custom reverse-geocoding");
                    out.print  ("<span class='zoneDescription' style='width:100%;' title=\""+i18nAddressTooltip+"\">");
                    out.print  ("<b>"+i18n.getString("CorridorInfo.map.description","Description")+"</b>:&nbsp;");
                    out.print  (Form_TextField(PARM_ZONE_DESC, _editZone, (_selZone!=null)?_selZone.getDescription():"", 30, 64));
                    out.println("</span>");
                    out.write("</td>");
                    out.write("</tr></table>");

                    //out.println("<br/>");
                    out.println("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPDATE+"'/>");

                    out.println("<table border='0' cellpadding='0' cellspacing='0' style='padding-top:3px'>"); // {
                    out.println("<tr>");

                    /* map (controls on right) */
                    MapDimension mapDim = (mapProvider != null)? mapProvider.getZoneDimension() : new MapDimension(0,0);
                    if (!mapControlsOnLeft) {
                        if (mapSupportsCorridors) {
                            out.println("<td style='width:"+mapDim.getWidth()+"px; height:"+mapDim.getHeight()+"px; padding-right:5px;'>");
                            out.println("<!-- Begin Map -->");
                            mapProvider.writeMapCell(out, reqState, mapDim);
                            out.println("<!-- End Map -->");
                            out.println("</td>");
                        } else {
                            out.println("<td style='width:"+mapDim.getWidth()+"px; height:"+mapDim.getHeight()+"px; padding-right:5px; border: 1px solid black;'>");
                            out.println("<!-- GeoCorridors not yet supported for this MapProvider -->");
                            out.println("<center>");
                            out.println("<span style='font-size:12pt;'>");
                            out.println(i18n.getString("CorridorInfo.map.notSupported","GeoCorridor map not yet supported for this MapProvider"));
                            out.println("&nbsp;</span>");
                            out.println("</center>");
                            out.println("</td>");
                        }
                    }

                    /* GeoCorridor fields */
                    out.println("<td valign='top' style='border-top: solid #CCCCCC 1px;'>");
                    
                    // GeoCorridor points section
                    out.println("<hr>");
                    if (_editZone && mapSupportsCorridors) {
                        out.println("<div class='zoneNotesBasic'>");
                        //out.println("<i>"+i18nx.getString("CorridorInfo.map.notes.basic", "The GeoCorridor loc/size may be changed here, click 'RESET' to update.")+"</i>");
                        out.println("<i>"+i18n.getString("CorridorInfo.map.notes.basic", "GeoCorridor attributes:")+"</i>");
                        out.println("</div>");
                    }

                    /* shape color */
                    /*
                    if (privLabel.getBooleanProperty(PrivateLabel.PROP_CorridorInfo_showShapeColor,false)) {
                        ComboMap colorCombo = GetColorComboMap(i18n);
                        String color = (_selZone != null)? _selZone.getShapeColor() : "";
                        String onchange = _editZone? "javascript:jsvZoneColor=document."+FORM_ZONE_EDIT+"."+PARM_ZONE_COLOR+".value;_zoneReset();" : null;
                        out.println("<div class='zoneColorSelect' title=\""+""+"\">");
                        out.println("<b>"+i18n.getString("CorridorInfo.map.shapeColor","Zone Color")+": </b>");
                        out.println(Form_ComboBox(PARM_ZONE_COLOR, PARM_ZONE_COLOR, _editZone, colorCombo, color, onchange, 10));
                        out.println("</div>");
                    }
                    */

                    /* radius */
                    String i18nRadiusTooltip = i18n.getString("CorridorInfo.map.radius.tooltip", "Radius may be between {0} and {1} meters",
                        String.valueOf((long)MIN_RADIUS_METERS), String.valueOf((long)MAX_RADIUS_METERS));
                    out.println("<div class='zoneRadius' title=\""+i18nRadiusTooltip+"\">");
                    out.print  ("<b>"+i18n.getString("CorridorInfo.map.radiusMeters","Radius (meters)")+":</b>&nbsp;");
                    out.println(Form_TextField(MapProvider.ID_ZONE_RADIUS_M, PARM_ZONE_RADIUS, _editZone, (_selZone!=null)?String.valueOf(_selZone.getRadius()):"", 7, 7));
                    out.println("</div>");

                    out.println("<div class='zoneLatLon'>");
                    out.println("<b>"+i18n.getString("CorridorInfo.map.latLon","Lat/Lon")+"</b>:&nbsp;&nbsp;");
                    if (_editZone && mapSupportsCorridors) {
                        String i18nResetBtn = i18n.getString("CorridorInfo.map.reset","Reset Map");
                        String i18nResetTooltip = i18n.getString("CorridorInfo.map.reset.tooltip", "Click to update the map with the specified radius/latitude/longitude");
                        out.print("<input class='formButton' type='button' name='reset' value='"+i18nResetBtn+"' title=\""+i18nResetTooltip+"\" onclick=\"javascript:_zoneReset();\">");
                    }
                    out.println("<br>");
                    out.println("<div style='height:200px; overflow-y:auto;'>");
                    int pointCount = privLabel.getIntProperty(PrivateLabel.PROP_CorridorInfo_pointCount,CORRIDOR_POINT_COUNT);
                    java.util.List<GeoPoint> gpList = GetCorridorPoints(_selZone);
                    for (int z = 0; z < pointCount; z++) {
                        GeoPoint   gp = ((gpList != null) && (z < gpList.size()))? gpList.get(z) : GeoPoint.INVALID_GEOPOINT;
                        String latStr = (_selZone != null)? String.valueOf(gp.getLatitude() ) : "";
                        String lonStr = (_selZone != null)? String.valueOf(gp.getLongitude()) : "";
                        // id='"+PARM_ZONE_INDEX+"'
                        if (pointCount > 1) {
                            String chk = (z == 0)? " checked" : "";
                            out.println("<input type='radio'  name='"+PARM_ZONE_INDEX+"' value='" + z + "' "+chk+" onchange=\"javascript:_zonePointSelectionChanged("+z+")\"/>");
                        } else {
                            out.println("<input type='hidden' name='"+PARM_ZONE_INDEX+"' value='" + z + "'/>");
                        }
                        out.println(Form_TextField(MapProviderAdapter.ID_ZONE_LATITUDE (z), PARM_ZONE_LATITUDE (z), _editZone, latStr, null/*onclick*/,  9,  9, CSS_LATLON_INPUT));
                        out.println(Form_TextField(MapProviderAdapter.ID_ZONE_LONGITUDE(z), PARM_ZONE_LONGITUDE(z), _editZone, lonStr, null/*onclick*/, 10, 10, CSS_LATLON_INPUT));
                        if ((z+1) < pointCount) { out.println("<br>"); }
                    }
                    out.println("</div>");
                    if (_editZone && mapSupportsCorridors) {
                        // "ZipCode" button
                        if (privLabel.getBooleanProperty(PrivateLabel.PROP_CorridorInfo_enableGeocode,false)) {
                            GeocodeProvider gcp = privLabel.getGeocodeProvider();
                            String i18nZipBtn = "";
                            if ((gcp == null) || gcp.getName().startsWith("geonames")) {
                                i18nZipBtn = i18n.getString("CorridorInfo.map.geocodeZip","Center On City/ZipCode");
                            } else {
                                i18nZipBtn = i18n.getString("CorridorInfo.map.geocodeAddress","Center On Address", gcp.getName());
                            }
                            String i18nZipTooltip = i18n.getString("CorridorInfo.map.geocode.tooltip", "Click to reset GeoCorridor to spcified Address/ZipCode");
                            String rgZipCode_text = "rgZipCode";
                            out.print("<hr>\n");
                          //out.print("<br>");
                            out.print("<input class='formButton' type='button' name='tozip' value='"+i18nZipBtn+"' title=\""+i18nZipTooltip+"\" onclick=\"javascript:_zoneGotoAddr(jsmGetIDValue('"+rgZipCode_text+"'),'US');\">");
                            out.print("<br>");
                            out.println(Form_TextField(rgZipCode_text, rgZipCode_text, _editZone, "",  27, 60));
                        }
                    }
                    out.println("</div>");

                    out.println("<hr>");
                    out.println("<div class='zoneInstructions'>");
                    out.println("<b>"+i18n.getString("CorridorInfo.map.notes.header","GeoCorridor Notes/Instructions")+":</b><br>");
                    if (_editZone && mapSupportsCorridors) {
                        String instr[] = mapProvider.getCorridorInstructions(locale);
                        if ((instr != null) && (instr.length > 0)) {
                            for (int i = 0; i < instr.length; i++) {
                                if (!StringTools.isBlank(instr[i])) { 
                                    out.println("- " + FilterText(instr[i]) + "<br>"); 
                                }
                            }
                        }
                    }
                    out.println("- " + i18n.getString("CorridorInfo.map.notes.lengthInMeters", "Distances are always in meters.") + "<br>");

                    out.println("<hr>");
                    if (mapSupportsCursorLocation || mapSupportsDistanceRuler) {
                        if (mapSupportsCursorLocation) {
                            out.println("<b>"+i18n.getString("CorridorInfo.map.cursorLoc","Cursor")+"</b>:");
                            out.println("<span id='"+MapProvider.ID_LAT_LON_DISPLAY +"' style='margin-left:6px; margin-bottom:3px;'>0.00000, 0.00000</span>");
                        }
                        if (mapSupportsDistanceRuler) {
                            out.println("<b>"+i18n.getString("CorridorInfo.map.distanceRuler","Distance")+"</b>:");
                            out.println("<span id='"+MapProvider.ID_DISTANCE_DISPLAY+"' style='margin-left:6px;'>0 "+GeoPoint.DistanceUnits.METERS.toString(locale)+"</span>");
                        }
                        out.println("<hr>");
                    }

                    out.println("</div>");

                    out.write("<div width='100%'>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_editZone) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("CorridorInfo.map.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("CorridorInfo.map.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_self');\">\n"); // target='_top'
                    } else {
                        out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("CorridorInfo.map.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_self');\">\n"); // target='_top'
                    }
                    out.write("</div>\n");

                    out.println("<div width='100%' height='100%'>");
                    out.println("&nbsp;");
                    out.println("</div>");

                    out.println("</td>");

                    /* map (controls on left) */
                    if (mapControlsOnLeft) {
                        if (mapSupportsCorridors) {
                            out.println("<td style='width:"+mapDim.getWidth()+"px; height:"+mapDim.getHeight()+"px; padding-left:5px;'>");
                            out.println("<!-- Begin Map -->");
                            mapProvider.writeMapCell(out, reqState, mapDim);
                            out.println("<!-- End Map -->");
                            out.println("</td>");
                        } else {
                            out.println("<td style='width:"+mapDim.getWidth()+"px; height:"+mapDim.getHeight()+"px; padding-left:5px; border: 1px solid black;'>");
                            out.println("<!-- GeoCorridors not yet supported for this MapProvider -->");
                            out.println("<center>");
                            out.println("<span style='font-size:12pt;'>");
                            out.println(i18n.getString("CorridorInfo.map.notSupported","GeoCorridor map not yet supported for this MapProvider"));
                            out.println("&nbsp;</span>");
                            out.println("</center>");
                            out.println("</td>");
                        }
                    }

                    /* end of form */
                    out.println("</tr>");
                    out.println("</table>"); // }
                    out.println("</form>");

                }

            }
        };

        /* map load? */
        String mapOnLoad   = _listZones? "" : "javascript:_zoneMapOnLoad();";
        String mapOnUnload = _listZones? "" : "javascript:_zoneMapOnUnload();";

        /* write frame */
        String onload = error? (mapOnLoad + JS_alert(false,m)) : mapOnLoad;
        CommonServlet.writePageFrame(
            reqState,
            onload,mapOnUnload,         // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // Javascript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }
    
    // ------------------------------------------------------------------------
}
