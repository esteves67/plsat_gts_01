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
package org.opengts.extra.cellid.ericsson;

import java.util.*;
import java.io.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;

import org.opengts.db.*;
import org.opengts.cellid.*;

public class MobileLocateAPI
    extends MobileLocationProviderAdapter
    implements MobileLocationProvider
{

    // ------------------------------------------------------------------------
    //
    // References:
    //   - https://labs.ericsson.com/apis/mobile-location/documentation#Extended_Cell-ID_Look-up_API
    //
    // ------------------------------------------------------------------------

    private static final String  MOBILE_LOCATION_URI            = "http://cellid.labs.ericsson.net/xml/lookup";
    private static final String  MOBILE_LOCATION_EXENDED_URI    = "http://cellid.labs.ericsson.net/xml/elookup";
    
    private static final String  VERSION                        = "0.1.1";

    // ------------------------------------------------------------------------

    private static final String  PROP_timeoutMS                 = "timeoutMS";
    
    // ------------------------------------------------------------------------

    private static final long    DefaultServiceTimeout          = 5000L; // milliseconds

    // ------------------------------------------------------------------------

    private static final String  TAG_position                   = "position";
    private static final String  TAG_latitude                   = "latitude";
    private static final String  TAG_longitude                  = "longitude";
    private static final String  TAG_accuracy                   = "accuracy";
    private static final String  TAG_error_message              = "error_message";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Document _getMobileLocateXML(String url, long timeoutMS)
    {
        // Error Resonse:
        //   200 OK           The request was successful. The result is placed in the body of the message.
        //   204 No Content   The cell could not be found in the database.
        //   400 Bad Request  Check if some parameter is missing or misspelled.
        //   401 Unauthorized Make sure the API key is present and valid.
        //   403 Forbidden    You have reached the limit for the number of requests.
        // Success Resonse:
        //   <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        //   <position>
        //      <accuracy>859</accuracy>
        //      <latitude>35.122155</latitude>
        //      <longitude>-101.846008</longitude>
        //      <error_message>The Developer key is unknown or maximum number of requests limit has be exceeded</error_message>
        //   </position>
        try {
            //Print.logInfo("HTTP User-Agent: " + HTMLTools.getHttpUserAgent());
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputStream input = HTMLTools.inputStream_GET(url, (int)timeoutMS);
            InputStreamReader reader = new InputStreamReader(input, StringTools.CharEncoding_UTF_8);
            InputSource inSrc = new InputSource(reader);
            inSrc.setEncoding(StringTools.CharEncoding_UTF_8);
            return db.parse(inSrc);
        } catch (ParserConfigurationException pce) {
            Print.logError("Parse error: " + pce);
            return null;
        } catch (SAXException se) {
            Print.logError("Parse error: " + se);
            return null;
        } catch (IOException ioe) {
            Print.logError("IO error: " + ioe);
            return null;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String _getCellLocationURL(CellTower servCT, CellTower nborCT[], String key)
    {
        // http://cellid.labs.ericsson.net/xml/lookup?cellid=0008c257&mnc=08&mcc=240&lac=017c&key=YOUR_API_KEY
        // http://cellid.labs.ericsson.net/xml/lookup?cellid=565110&mnc=08&mcc=240&lac=318&base=10&key=YOUR_API_KEY
        
        /* no serving CellTower? */
        if (servCT == null) {
            return null;
        }
        
        /* skip if no key (required) */
        if (StringTools.isBlank(key)) {
            Print.logError("Required 'key' is not specified");
            return null;
        }
        
        /* standard */
        if (ListTools.isEmpty(nborCT)) {
            URIArg url = new URIArg(MOBILE_LOCATION_URI);
            url.addArg("key"    , key);
            url.addArg("verbose", "true");
            url.addArg("base"   , 10);
            // - Service Cell
            url.addArg("cellid" , servCT.getCellTowerID());
            url.addArg("mnc"    , servCT.getMobileNetworkCode());
            url.addArg("mcc"    , servCT.getMobileCountryCode());
            url.addArg("lac"    , servCT.getLocationAreaCode());
            return url.toString();
        } else {
            URIArg url = new URIArg(MOBILE_LOCATION_EXENDED_URI);
            url.addArg("key"    , key);
            url.addArg("verbose", "true");
            url.addArg("base"   , 10);
            // - Service Cell
            StringBuffer serving = new StringBuffer();
            serving.append(servCT.getRadioAccessTechnologyString());
            serving.append(",");
            serving.append(servCT.getMobileCountryCodeString());
            serving.append(",");
            serving.append(servCT.getMobileNetworkCodeString());
            serving.append(",");
            serving.append(servCT.getLocationAreaCodeString());
            serving.append(",");
            serving.append(servCT.getCellTowerIDString());
            serving.append(",");
            serving.append(servCT.getReceptionLevelString());
            serving.append(",");
            serving.append(servCT.getTimingAdvanceString());
            url.addArg("serving", serving.toString());
            // - Neightbor Cell(s)
            for (int n = 0; n < nborCT.length; n++) {
                StringBuffer neighbor = new StringBuffer();
                neighbor.append(nborCT[n].getRadioAccessTechnologyString());
                neighbor.append(",");
                neighbor.append(nborCT[n].getMobileCountryCodeString());
                neighbor.append(",");
                neighbor.append(nborCT[n].getMobileNetworkCodeString());
                neighbor.append(",");
                neighbor.append(nborCT[n].getLocationAreaCodeString());
                neighbor.append(",");
                neighbor.append(nborCT[n].getCellTowerIDString());
                neighbor.append(",");
                neighbor.append(nborCT[n].getReceptionLevelString());
                neighbor.append(",");
                neighbor.append(nborCT[n].getTimingAdvanceString());
                url.addArg("neighbor", neighbor.toString());
            }
            return url.toString();
        }

    }
    
    private static MobileLocation _getMobileLocation(
        CellTower servCT, CellTower nborCT[], 
        String key, long timeoutMS)
    {

        /* URL */
        String url = MobileLocateAPI._getCellLocationURL(servCT, nborCT, key);
        if (StringTools.isBlank(url)) {
            return null;
        }
        Print.logInfo("Ericsson CelTower URL: " + url);

        /* get HTTP result */
        Document xmlDoc = MobileLocateAPI._getMobileLocateXML(url, timeoutMS);
        if (xmlDoc == null) {
            return null;
        }

        /* parse "position" */
        double latitude  = 999.0;
        double longitude = 999.0;
        double accuracyM = 0.0;
        String errMsg    = null;
        Element position = xmlDoc.getDocumentElement();
        if (position.getTagName().equalsIgnoreCase(TAG_position)) {
            NodeList nodeList = position.getChildNodes();
            for (int a = 0; a < nodeList.getLength(); a++) {
                Node node = nodeList.item(a);
                if (!(node instanceof Element)) { continue; }
                Element elem = (Element)node;
                String name = elem.getNodeName();
                if (name.equalsIgnoreCase(TAG_latitude)) {
                    latitude = StringTools.parseDouble(XMLTools.getNodeText(elem,",",false),0.0);
                } else
                if (name.equalsIgnoreCase(TAG_longitude)) {
                    longitude = StringTools.parseDouble(XMLTools.getNodeText(elem,",",false),0.0);
                } else
                if (name.equalsIgnoreCase(TAG_accuracy)) {
                    accuracyM = StringTools.parseDouble(XMLTools.getNodeText(elem,",",false),0.0);
                } else
                if (name.equalsIgnoreCase(TAG_error_message)) {
                    errMsg = XMLTools.getNodeText(elem," ",false);
                } else {
                    // not not recognized
                }
            }
        }
        
        /* error message? */
        if (!StringTools.isBlank(errMsg)) {
            Print.logError("ERROR: " + errMsg);
        }

        /* valid GeoPoint? */
        if (GeoPoint.isValid(latitude,longitude)) {
            return new MobileLocation(latitude,longitude,accuracyM);
        } else {
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // MobileLocationProvider interface

    public MobileLocateAPI(String name, String key, RTProperties rtProps)
    {
        super(name, key, rtProps);
    }

    public MobileLocation getMobileLocation(CellTower servCT, CellTower nborCT[]) 
    {
        long tmoMS = this.getProperties().getLong(PROP_timeoutMS, DefaultServiceTimeout);
        return MobileLocateAPI._getMobileLocation(servCT, nborCT, this.getAuthorization(), tmoMS);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Main entery point for debugging/testing
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        Print.setAllOutputToStdout(true);
        Print.setEncoding(StringTools.CharEncoding_UTF_8);

        /* geocode lookup */
        CellTower ct = new CellTower();
        ct.setCellTowerID(565110);
        ct.setMobileNetworkCode(8);
        ct.setMobileCountryCode(240);
        ct.setLocationAreaCode(318);

        /* get CellTower location */
        String key = RTConfig.getString("key","");
        MobileLocateAPI mobLoc = new MobileLocateAPI("ericsson", key, null);
        MobileLocation ml = mobLoc.getMobileLocation(ct, null);
        Print.logInfo("Mobile Location: " + ml);

    }

}
