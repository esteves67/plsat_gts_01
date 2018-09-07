package org.opengts.war.track.page;

import java.util.TimeZone;
import java.util.Iterator;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class TrackMapDevice
    extends TrackMap
{
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // WebPage interface
    
    public TrackMapDevice()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_MAP_DEVICE);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
        this.setFleet(false);
    }

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_TRACK_DEVICE;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N   i18n            = privLabel.getI18N(TrackMapDevice.class);
        String devTitles[]     = reqState.getDeviceTitles();
        return super._getMenuDescription(reqState,i18n.getString("TrackMapDevice.menuDesc","Track {0} locations on a map", devTitles));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N   i18n            = privLabel.getI18N(TrackMapDevice.class);
        String devTitles[]     = reqState.getDeviceTitles();
        return super._getMenuHelp(reqState,i18n.getString("TrackMapDevice.menuHelp","Select and Track the location of a {0} on a map", devTitles));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N   i18n            = privLabel.getI18N(TrackMapDevice.class);
        String devTitles[]     = reqState.getDeviceTitles();
        return super._getNavigationDescription(reqState,i18n.getString("TrackMapDevice.navDesc","{0}", devTitles));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N   i18n            = privLabel.getI18N(TrackMapDevice.class);
        String devTitles[]     = reqState.getDeviceTitles();
        return super._getNavigationTab(reqState,i18n.getString("TrackMapDevice.navTab","{0} Map", devTitles));
    }

    // ------------------------------------------------------------------------

}
