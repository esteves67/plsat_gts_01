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
package org.opengts.geocoder.google;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

import org.opengts.db.*;
import org.opengts.geocoder.*;

public class GoogleGeocodeV3
    extends ReverseGeocodeProviderAdapter
    implements ReverseGeocodeProvider, GeocodeProvider
{

    // ------------------------------------------------------------------------
    // References:
    //   - http://code.google.com/apis/maps/documentation/services.html#Geocoding_Direct
    //   - http://code.google.com/apis/maps/documentation/geocoding/index.html
    //
    // API URLs:
    //   - http://maps.googleapis.com/maps/api/geocode/output?...
    //   - https://maps.googleapis.com/maps/api/geocode/output?...
    //
    // Nearest Address: V3 API
    //   - http://maps.googleapis.com/maps/api/geocode/json?latlng=40.479581,-117.773438&language=en&sensor=false
    //     {
    //        "results" : [
    //           {
    //              "address_components" : [
    //                 {
    //                    "long_name" : "Unnamed Rd",
    //                    "short_name" : "Unnamed Rd",
    //                    "types" : [ "route" ]
    //                 },
    //                 {
    //                    "long_name" : "Imlay",
    //                    "short_name" : "Imlay",
    //                    "types" : [ "sublocality", "political" ]
    //                 },
    //                 {
    //                    "long_name" : "Pershing",
    //                    "short_name" : "Pershing",
    //                    "types" : [ "administrative_area_level_2", "political" ]
    //                 },
    //                 {
    //                    "long_name" : "Nevada",
    //                    "short_name" : "NV",
    //                    "types" : [ "administrative_area_level_1", "political" ]
    //                 },
    //                 {
    //                    "long_name" : "United States",
    //                    "short_name" : "US",
    //                    "types" : [ "country", "political" ]
    //                 }
    //              ],
    //              "formatted_address" : "Unnamed Rd, Imlay, NV, USA",
    //              "geometry" : {
    //                 "bounds" : {
    //                    "northeast" : {
    //                       "lat" : 40.50201490,
    //                       "lng" : -117.7399320
    //                    },
    //                    "southwest" : {
    //                       "lat" : 40.49069360,
    //                       "lng" : -117.75710910
    //                    }
    //                 },
    //                 "location" : {
    //                    "lat" : 40.49852230,
    //                    "lng" : -117.74807860
    //                 },
    //                 "location_type" : "APPROXIMATE",
    //                 "viewport" : {
    //                    "northeast" : {
    //                       "lat" : 40.50201490,
    //                       "lng" : -117.7399320
    //                    },
    //                    "southwest" : {
    //                       "lat" : 40.49069360,
    //                       "lng" : -117.75710910
    //                    }
    //                 }
    //              },
    //              "types" : [ "route" ]
    //           },
    //           {
    //              "address_components" : [
    //                 {
    //                    "long_name" : "Pershing",
    //                    "short_name" : "Pershing",
    //                    "types" : [ "administrative_area_level_2", "political" ]
    //                 },
    //                 {
    //                    "long_name" : "Nevada",
    //                    "short_name" : "NV",
    //                    "types" : [ "administrative_area_level_1", "political" ]
    //                 },
    //                 {
    //                    "long_name" : "United States",
    //                    "short_name" : "US",
    //                    "types" : [ "country", "political" ]
    //                 }
    //              ],
    //              "formatted_address" : "Pershing, NV, USA",
    //              "geometry" : {
    //                 "bounds" : {
    //                    "northeast" : {
    //                       "lat" : 40.96115890,
    //                       "lng" : -117.2999130
    //                    },
    //                    "southwest" : {
    //                       "lat" : 39.9982940,
    //                       "lng" : -119.3392960
    //                    }
    //                 },
    //                 "location" : {
    //                    "lat" : 40.56859520,
    //                    "lng" : -118.48639630
    //                 },
    //                 "location_type" : "APPROXIMATE",
    //                 "viewport" : {
    //                    "northeast" : {
    //                       "lat" : 40.96115890,
    //                       "lng" : -117.2999130
    //                    },
    //                    "southwest" : {
    //                       "lat" : 39.9982940,
    //                       "lng" : -119.3392960
    //                    }
    //                 }
    //              },
    //              "types" : [ "administrative_area_level_2", "political" ]
    //           },
    //           {
    //              "address_components" : [
    //                 {
    //                    "long_name" : "Nevada",
    //                    "short_name" : "NV",
    //                    "types" : [ "administrative_area_level_1", "political" ]
    //                 },
    //                 {
    //                    "long_name" : "United States",
    //                    "short_name" : "US",
    //                    "types" : [ "country", "political" ]
    //                 }
    //              ],
    //              "formatted_address" : "Nevada, USA",
    //              "geometry" : {
    //                 "bounds" : {
    //                    "northeast" : {
    //                       "lat" : 42.0022070,
    //                       "lng" : -114.0396480
    //                    },
    //                    "southwest" : {
    //                       "lat" : 35.0018570,
    //                       "lng" : -120.0064730
    //                    }
    //                 },
    //                 "location" : {
    //                    "lat" : 38.80260970,
    //                    "lng" : -116.4193890
    //                 },
    //                 "location_type" : "APPROXIMATE",
    //                 "viewport" : {
    //                    "northeast" : {
    //                       "lat" : 42.0022070,
    //                       "lng" : -114.0396480
    //                    },
    //                    "southwest" : {
    //                       "lat" : 35.0018570,
    //                       "lng" : -120.0064730
    //                    }
    //                 }
    //              },
    //              "types" : [ "administrative_area_level_1", "political" ]
    //           },
    //           {
    //              "address_components" : [
    //                 {
    //                    "long_name" : "United States",
    //                    "short_name" : "US",
    //                    "types" : [ "country", "political" ]
    //                 }
    //              ],
    //              "formatted_address" : "United States",
    //              "geometry" : {
    //                 "bounds" : {
    //                    "northeast" : {
    //                       "lat" : 71.3898880,
    //                       "lng" : -66.94539480000002
    //                    },
    //                    "southwest" : {
    //                       "lat" : 18.91106430,
    //                       "lng" : 172.45469670
    //                    }
    //                 },
    //                 "location" : {
    //                    "lat" : 37.090240,
    //                    "lng" : -95.7128910
    //                 },
    //                 "location_type" : "APPROXIMATE",
    //                 "viewport" : {
    //                    "northeast" : {
    //                       "lat" : 71.3898880,
    //                       "lng" : -66.94539480000002
    //                    },
    //                    "southwest" : {
    //                       "lat" : 18.91106430,
    //                       "lng" : 172.45469670
    //                    }
    //                 }
    //              },
    //              "types" : [ "country", "political" ]
    //           }
    //        ],
    //        "status" : "OK"
    //     }
    //
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // V3 API
    
    protected static final String TAG_results                   = "results";        // main tag
    
    protected static final String TAG_status                    = "status";     
    
    protected static final String TAG_address_components        = "address_components";
    protected static final String TAG_long_name                 = "long_name";
    protected static final String TAG_short_name                = "short_name";
    protected static final String TAG_types                     = "types";
    protected static final String TAG_formatted_address         = "formatted_address";
    protected static final String TAG_geometry                  = "geometry";
    protected static final String TAG_bounds                    = "bounds";
    protected static final String TAG_northeast                 = "northeast";
    protected static final String TAG_southwest                 = "southwest";
    protected static final String TAG_lat                       = "lat";
    protected static final String TAG_lng                       = "lng";
    protected static final String TAG_location                  = "location";
    protected static final String TAG_location_type             = "location_type";
    protected static final String TAG_viewport                  = "viewport";

    /* V3 URLs */
    protected static final String URL_ReverseGeocode_           = "https://maps.googleapis.com/maps/api/geocode/json?";
    protected static final String URL_Geocode_                  = "http://maps.googleapis.com/maps/api/geocode/json?";
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static final String  PROP_reverseGeocodeURL       = "reverseGeocodeURL";
    protected static final String  PROP_geocodeURL              = "geocodeURL";
    protected static final String  PROP_sensor                  = "sensor";
    protected static final String  PROP_channel                 = "channel";
    protected static final String  PROP_countryCodeBias         = "countryCodeBias"; // http://en.wikipedia.org/wiki/CcTLD
    protected static final String  PROP_signatureKey            = "signatureKey";

    // ------------------------------------------------------------------------

    protected static final int     TIMEOUT_ReverseGeocode       = 2500; // milliseconds
    protected static final int     TIMEOUT_Geocode              = 5000; // milliseconds

    protected static final String  DEFAULT_COUNTRY              = "US"; // http://en.wikipedia.org/wiki/CcTLD

    protected static final String  CLIENT_ID_PREFIX             = "gme-";

    // ------------------------------------------------------------------------

    /* MUST BE FALSE IN PRODUCTION!!! */
    protected static final boolean FAILOVER_DEBUG               = false;

    // ------------------------------------------------------------------------

    // address has to be within this distance to qualify (cannot be greater than 5.0 kilometers)
    protected static final double  MAX_ADDRESS_DISTANCE_KM      = 1.1; 
   // protected static final double MAX_ADDRESS_DISTANCE_KM = 4.5;

    // ------------------------------------------------------------------------

    protected static final String  ENCODING_UTF8                = StringTools.CharEncoding_UTF_8;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean                 signature_init  = false;
    private GoogleSig               signature       = null;

    public GoogleGeocodeV3(String name, String key, RTProperties rtProps)
    {
        super(name, key, rtProps);
    }

    // ------------------------------------------------------------------------

    public boolean isFastOperation()
    {
        // this is a slow operation
        return super.isFastOperation();
    }

    // ------------------------------------------------------------------------

    public GoogleSig getSignature()
    {
        if (!this.signature_init) {
            this.signature_init = true;
            String key = this.getAuthorization();
            if (!StringTools.isBlank(key) && key.startsWith(CLIENT_ID_PREFIX)) {
                String sigKey = this.getProperties().getString(PROP_signatureKey,"");
                if (!StringTools.isBlank(sigKey)) {
                    //Print.logWarn("Setting SignatureKey: " + sigKey);
                    this.signature = new GoogleSig(sigKey);
                } else {
                    Print.logWarn("No signatureKey ...");
                }
            }
        }
        return this.signature;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the Geocode timeout
    **/
    protected int getGeocodeTimeout()
    {
        return TIMEOUT_Geocode;
    }

    /**
    *** Returns the ReverseGeocode timeout
    **/
    protected int getReverseGeocodeTimeout()
    {
        return TIMEOUT_ReverseGeocode;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return reverse-geocode */
    public ReverseGeocode getReverseGeocode(GeoPoint gp, String localeStr, boolean cache)
    {
        ReverseGeocode rg = this.getAddressReverseGeocode(gp, localeStr, cache);
        return rg;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* nearest address URI */
    protected String getAddressReverseGeocodeURI()
    {
        return URL_ReverseGeocode_;
    }

    /* encode GeoPoint into nearest address URI */
    protected String getAddressReverseGeocodeURL(GeoPoint gp, String localStr)
    {
        StringBuffer sb = new StringBuffer();
        GoogleSig sig = this.getSignature();

        /* predefined URL */
        String rgURL = this.getProperties().getString(PROP_reverseGeocodeURL,null);
        if (!StringTools.isBlank(rgURL)) {
            sb.append(rgURL);
            sb.append("&latlng=");
            if (gp != null) {
                sb.append(gp.getLatitudeString(GeoPoint.SFORMAT_DEC_5,null)).append(",").append(gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null));
            }
            String defURL = sb.toString();
            if (sig == null) {
                return defURL;
            } else {
                String urlStr = sig.signURL(defURL);
                return (urlStr != null)? urlStr : defURL;
            }
        }

        /* assemble URL */
        sb.append(this.getAddressReverseGeocodeURI());
        sb.append("oe=utf8");

        /* latitude/longitude */
        sb.append("&latlng=");
        if (gp != null) {
            sb.append(gp.getLatitudeString(GeoPoint.SFORMAT_DEC_5,null)).append(",").append(gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null));
        }

        /* sensor */
        String sensor = this.getProperties().getString(PROP_sensor, null);
        if (!StringTools.isBlank(sensor)) {
            sb.append("&sensor=").append(sensor);
        }

        /* key */
        String channel = this.getProperties().getString(PROP_channel, null);
        String auth    = this.getAuthorization();
        Print.logInfo("---------------------------------------------------------------- "+auth);
        if (StringTools.isBlank(auth) || auth.startsWith("*")) {
            // invalid key
        } else
        if (auth.startsWith(CLIENT_ID_PREFIX)) {
            sb.append("&client=").append(auth);
            if (StringTools.isBlank(channel)) {
                channel = DBConfig.getServiceAccountID(null);
            }
        } else {
            sb.append("&key=").append(auth);
        }

        /* channel */
        if (!StringTools.isBlank(channel)) {
            sb.append("&channel=").append(channel);
        }

        /* localization ("&language=") */
        if (!StringTools.isBlank(localStr)) {
            sb.append("&language=").append(localStr);
        }

        /* return url */
        String defURL = sb.toString();
        if (sig == null) {
            return defURL;
        } else {
            String urlStr = sig.signURL(defURL);
            return (urlStr != null)? urlStr : defURL;
        }

    }

    /* return reverse-geocode using nearest address */
    public ReverseGeocode getAddressReverseGeocode(GeoPoint gp, String localeStr, boolean cache)
    {

        /* check for failover mode */
        if (this.isReverseGeocodeFailoverMode()) {
            ReverseGeocodeProvider frgp = this.getFailoverReverseGeocodeProvider();
            return frgp.getReverseGeocode(gp, localeStr, cache);
        }

        /* URL */
        String url = this.getAddressReverseGeocodeURL(gp, localeStr);
        Print.logInfo("------------------------- Google RG URL: " + url);

        /* create JSON document */
        JSON jsonDoc = GetJSONDocument(url, this.getReverseGeocodeTimeout());
        JSON._Object jsonObj = (jsonDoc != null)? jsonDoc.getObject() : null;
        if (jsonObj == null) {
            return null;
        }

        /* parse "status" */
        String status = jsonObj.getStringValueForName(TAG_status, ""); // expect "OK"
        if (!status.equalsIgnoreCase("OK")) {
            Print.logInfo("Status : " + status);
        }

        /* parse address */
        String address = null;
        JSON._Array results = jsonObj.getArrayForName(TAG_results, null);
        if (!ListTools.isEmpty(results)) {
            JSON._Value val0 = results.getValueAt(0);
            JSON._Object addr = (val0 != null)? val0.getObjectValue(null) : null;
            if (addr != null) {
                address = addr.getStringValueForName(TAG_formatted_address, null);
                Print.logInfo("Address: " + address);
            }
        } else {
            Print.logInfo("No address found: null");
        }

        /* create address */
        if (FAILOVER_DEBUG) {
            status = "620";
        } else 
        if (!StringTools.isBlank(address)) {
            // address found 
            ReverseGeocode rg = new ReverseGeocode();
            rg.setFullAddress(address);
            return rg;
        }

        /* check for Google reverse-geocode limit exceeded */
        if ((status != null) && status.equals("620")) {
            Print.logError("!!!! Google Reverse-Geocode Limit Exceeded [Error 620] !!!!");
            if (this.hasFailoverReverseGeocodeProvider()) {
                this.startReverseGeocodeFailoverMode();
                ReverseGeocodeProvider frgp = this.getFailoverReverseGeocodeProvider();
                Print.logWarn("Failing over to '" + frgp.getName() + "'");
                return frgp.getReverseGeocode(gp, localeStr, cache);
            }
        }

        /* no reverse-geocode available */
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* nearest address URI */
    protected String getGeoPointGeocodeURI()
    {
        return URL_Geocode_;
    }

    /* encode GeoPoint into nearest address URI */
    protected String getGeoPointGeocodeURL(String address, String country)
    {
        StringBuffer sb = new StringBuffer();
        GoogleSig sig = this.getSignature();

        /* country */
        if (StringTools.isBlank(country)) { 
            country = this.getProperties().getString(PROP_countryCodeBias, DEFAULT_COUNTRY);
        }

        /* predefined URL */
        String gcURL = this.getProperties().getString(PROP_geocodeURL,null);
        if (!StringTools.isBlank(gcURL)) {
            sb.append(gcURL);
            sb.append("&address=").append(URIArg.encodeArg(address));
            if (!StringTools.isBlank(country)) {
                // country code bias: http://en.wikipedia.org/wiki/CcTLD
                sb.append("&region=").append(country);
            }
            String defURL = sb.toString();
            if (sig == null) {
                return defURL;
            } else {
                String urlStr = sig.signURL(defURL);
                return (urlStr != null)? urlStr : defURL;
            }
        }
        
        /* assemble URL */
        sb.append(this.getGeoPointGeocodeURI());
        sb.append("oe=utf8");

        /* address/country */
        sb.append("&address=").append(URIArg.encodeArg(address));
        if (!StringTools.isBlank(country)) {
            sb.append("&region=").append(country);
        }

        /* sensor */
        String sensor= this.getProperties().getString(PROP_sensor, null);
        if (!StringTools.isBlank(sensor)) {
            sb.append("&sensor=").append(sensor);
        }

        /* channel */
        String channel = this.getProperties().getString(PROP_channel, null);
        if (!StringTools.isBlank(channel)) {
            sb.append("&channel=").append(channel);
        }

        /* key */
        String auth = this.getAuthorization();
        Print.logInfo("*********************************************" + auth);
        if (StringTools.isBlank(auth) || auth.startsWith("*")) {
            // invalid key
        } else
        if (auth.startsWith(CLIENT_ID_PREFIX)) {
            sb.append("&client=").append(auth);
        } else {
            sb.append("&key=").append(auth);
        }

        /* return url */
        String defURL = sb.toString();
        if (sig == null) {
            return defURL;
        } else {
            String urlStr = sig.signURL(defURL);
            return (urlStr != null)? urlStr : defURL;
        }


    }

    /* return geocode */
    public GeoPoint getGeocode(String address, String country)
    {

        /* URL */
        String url = this.getGeoPointGeocodeURL(address, country);
        Print.logDebug("Google GC URL: " + url);

        /* create JSON document */
        JSON jsonDoc = GetJSONDocument(url, this.getReverseGeocodeTimeout());
        JSON._Object jsonObj = (jsonDoc != null)? jsonDoc.getObject() : null;
        if (jsonObj == null) {
            return null;
        }

        /* vars */
        String status = jsonObj.getStringValueForName(TAG_status, null); // expect "OK"
        Print.logInfo("Status : " + status);

        /* parse GeoPoint */
        GeoPoint geoPoint = null;
        JSON._Array results = jsonObj.getArrayForName(TAG_results, null);
        if (!ListTools.isEmpty(results)) {
            JSON._Value  val0 = results.getValueAt(0);
            JSON._Object obj0 = val0.getObjectValue(null);
            JSON._Object geometry = obj0.getObjectValueForName(TAG_geometry, null);
            JSON._Object location = (geometry != null)? geometry.getObjectValueForName(TAG_location,null) : null;
            if (location != null) {
                double lat = location.getDoubleValueForName(TAG_lat,0.0);
                double lng = location.getDoubleValueForName(TAG_lng,0.0);
                if (GeoPoint.isValid(lat,lng)) {
                    geoPoint = new GeoPoint(lat,lng);
                    Print.logInfo("GeoPoint: " + geoPoint);
                }
            }
        } else {
            Print.logInfo("No location found: null");
        }

        /* create address */
        if (geoPoint != null) {
            // GeoPoint found 
            return geoPoint;
        }
        
        /* check for Google reverse-geocode limit exceeded */
        if ((status != null) && status.equals("620")) {
            Print.logError("!!!! Google Reverse-Geocode Limit Exceeded [Error 620] !!!!");
        }

        /* no reverse-geocode available */
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static JSON GetJSONDocument(String url, int timeoutMS)
    {
        try {
            InputStream input = HTMLTools.inputStream_GET(url, timeoutMS);
            JSON jsonDoc = new JSON(input);
            return jsonDoc;
        } catch (JSON.JSONParsingException jpe) {
            Print.logError("Parse error: " + jpe);
            return null;
        } catch (IOException ioe) {
            Print.logError("IO error: " + ioe);
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String ARG_ACCOUNT[]       = new String[] { "account", "a"  };
    private static final String ARG_GEOCODE[]       = new String[] { "geocode", "gc" };
    private static final String ARG_REVGEOCODE[]    = new String[] { "revgeo" , "rg" };
    
    private static String FilterID(String id)
    {
        if (id == null) {
            return null;
        } else {
            StringBuffer newID = new StringBuffer();
            int st = 0;
            for (int i = 0; i < id.length(); i++) {
                char ch = Character.toLowerCase(id.charAt(i));
                if (Character.isLetterOrDigit(ch)) {
                    newID.append(ch);
                    st = 1;
                } else
                if (st == 1) {
                    newID.append("_");
                    st = 0;
                } else {
                    // ignore char
                }
            }
            while ((newID.length() > 0) && (newID.charAt(newID.length() - 1) == '_')) {
                newID.setLength(newID.length() - 1);
            }
            return newID.toString();
        }
    }

    /**
    *** Main entery point for debugging/testing
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        Print.setAllOutputToStdout(true);
        Print.setEncoding(ENCODING_UTF8);
        String accountID = RTConfig.getString(ARG_ACCOUNT,"demo");
        GoogleGeocodeV3 gn = new GoogleGeocodeV3("googleV3", null, null);

        /* [ENTERPRISE] geocode */
        if (RTConfig.hasProperty(ARG_GEOCODE)) {
            String address = RTConfig.getString(ARG_GEOCODE,null);
            if (FileTools.isFile(address)) {
                StringBuffer sb = new StringBuffer();
                sb.append("\"accountID\",");
                sb.append("\"geozoneID\",");
                sb.append("\"sortID\",");
                sb.append("\"description\",");
                sb.append("\"radius\",");
                sb.append("\"latitude1\",");
                sb.append("\"longitude1\"");
                Print.sysPrintln(sb.toString());
                String aList[] = StringTools.split(StringTools.toStringValue(FileTools.readFile(address)),'\n');
                int zoneCnt = 0;
                char stipChars[] = new char[] { '\"' };
                for (String a : aList) {
                    String dftzoneID = "zone_" + (++zoneCnt);
                    String geozoneID = dftzoneID;
                    int    sortID    = 0;
                    String descr     = "";
                    int    radiusM   = 200; 
                    String addr      = "";
                    String f[]       = StringTools.split(a,'|');
                    if (ListTools.size(f) == 1) {
                        geozoneID = dftzoneID;
                        descr     = StringTools.stripChars(StringTools.trim(f[0]),stipChars);
                        addr      = StringTools.stripChars(StringTools.trim(f[0]),stipChars);
                    } else 
                    if (ListTools.size(f) >= 3) {
                        geozoneID = StringTools.blankDefault(FilterID(f[0]),dftzoneID);
                        descr     = StringTools.stripChars(StringTools.trim(f[0]),stipChars);
                        sortID    = 0;
                        radiusM   = StringTools.parseInt(f[1],200); 
                        addr      = StringTools.stripChars(StringTools.trim(f[2]),stipChars);
                    }
                    // skip if no address
                    if (StringTools.isBlank(addr)) {
                        continue;
                    }
                    // truncate "geozoneID" to 32 characters max
                    int maxLen_geozoneID = 32;
                    if (geozoneID.length() > maxLen_geozoneID) {
                        // trucate length
                        geozoneID = geozoneID.substring(0, maxLen_geozoneID);
                        if (geozoneID.endsWith("_")) {
                            geozoneID = geozoneID.substring(0, geozoneID.length() - 1);
                        }
                    }
                    // geocode
                    GeoPoint gp = gn.getGeocode(addr, DEFAULT_COUNTRY);
                    if ((gp != null) && gp.isValid()) {
                        // accountID, geozoneID, sortID, radius, latitude1, longitude1
                        sb.setLength(0);
                        sb.append("\"").append(accountID        ).append("\",");
                        sb.append("\"").append(geozoneID        ).append("\",");
                        sb.append("")  .append(sortID           ).append(",");
                        sb.append("\"").append(descr            ).append("\",");
                        sb.append("")  .append(radiusM          ).append(",");
                        sb.append("")  .append(gp.getLatitude() ).append(",");
                        sb.append("")  .append(gp.getLongitude()).append("");
                        Print.sysPrintln(sb.toString());
                    } else {
                        Print.sysPrintln("// GPS location not found for address: " + addr);
                    }
                }
            } else {
                GeoPoint gp = gn.getGeocode(address, DEFAULT_COUNTRY);
                Print.sysPrintln("Location " + gp);
            }
            System.exit(0);
        }
        /* reverse geocode */
        if (RTConfig.hasProperty(ARG_REVGEOCODE)) {
            GeoPoint gp = new GeoPoint(RTConfig.getString(ARG_REVGEOCODE,null));
            if (!gp.isValid()) {
                Print.logInfo("Invalid GeoPoint specified");
                System.exit(1);
            }
            Print.logInfo("Reverse-Geocoding GeoPoint: " + gp);
            Print.sysPrintln("RevGeocode = " + gn.getReverseGeocode(gp,null/*localeStr*/,false/*cache*/));
            // Note: Even though the values are printed in UTF-8 character encoding, the
            // characters may not appear to be property displayed if the console display
            // does not support UTF-8.
            System.exit(0);
        }

        /* no options */
        Print.sysPrintln("No options specified");
        System.exit(1);

    }

}
