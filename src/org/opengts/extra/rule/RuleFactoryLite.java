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
// Notes:
//  - This is a very simple implementation of the 'RulesFactory' interface,
//    which can send email notification based on Geozone arrival/departure,
//    provided that the device communication server can provide the proper
//    STATUS_GEOFENCE_ARRIVE and/or STATUS_GEOFENCE_DEPART status codes.  
//  - This RuleFactory implementation may be installed into the Device record
//    class from within the "<StartupInit>.addTableFactories()" with the 
//    following code specification:
//        Device.setRuleFactory(new org.opengts.extra.rule.RuleFactoryLite());
//    Or by adding the following line to your 'common.conf' file:
//        RuleFactory.class=org.opengts.extra.rule.RuleFactoryLite
//  - To use this RulesFactoryLite implementation, you will need to make sure 
//    that your outbound SMTP email service is set up properly in the runtime
//    config file "common.conf" (then rebuild/redeploy the 'track.war' and any
//    servlets, as necessary).
//  - This RuleFactoryLite supports the following rule selector values:
//      "true"          - always triggers a notification
//      "false"         - never triggeres a notification
//      "arrive"        - triggers a notification for STATUS_GEOFENCE_ARRIVE events.
//      "depart"        - triggers a notification for STATUS_GEOFENCE_DEPART events.
//      "speed(S)"      - triggers a notification if event speed km/h is > S
//      "speedmph(S)"   - triggers a notification if event speed mph is > S
//      "code(C)"       - triggers a notification if event statusCode == C
//  - The supported rule "selectors" must be specified in the device record
//    "Notification Selector" field to be effective for incoming events for 
//    that specific device.  Multiple selector items may be specified by
//    separating them with commas.  For instance, to cause an event trigger
//    for geozone arrival or departure, you may specify "arrive,depart" as
//    the value for the "Notification Selector" field.
//  - EMails are sent to the Device and Account "Notification EMail Addresses".
// ----------------------------------------------------------------------------
//
// We have an available fully functional "Event Notification Rules Engine" module
// that allows specifying a much more feature rich set of "rule selectors".  It 
// can also be used for selecting specific icons to display on a map based on the 
// specific type of event.
//
// The PDF document for this fully functional "Rules Engine" may be obtained here:
//  http://www.geotelematic.com/download/docs/RulesEngine.pdf
//
// Please contact us for more details.
//
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.extra.rule;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class RuleFactoryLite
    extends RuleFactoryAdapter
{

    private static final String VERSION                 = "0.1.2";
    
    // ------------------------------------------------------------------------
    
    private static final String SELECT_ALL              = "ALL";
    private static final String SELECT_NONE             = "NONE";

    // ------------------------------------------------------------------------

    private static final String START_DELIM             = StringTools.KEY_START; // "${";
    private static final String END_DELIM               = StringTools.KEY_END;   // "}";
    private static final String DFT_DELIM               = StringTools.DFT_DELIM; // "="
    
    private static final String KEY_RULENAME[]          = new String[] { "rule"         , "ruleName"         };
    private static final String KEY_SUBJECT[]           = new String[] { "subject"      , "ruleSubject"      };
    private static final String KEY_MESSAGE[]           = new String[] { "message"      , "ruleMessage"      };

    // This should be defined in 'private.xml'
    private static final String DEFAULT_SUBJECT = 
        "Device ${device}: [${statuscode}] ${status}";
    
    private static final String DEFAULT_BODY    = 
        "Rule      : ${ruleName}\n" +
        "Account   : [${accountid}] ${account}\n" +
        "Device    : [${deviceid}] ${device}\n" +
        "Date/Time : ${datetime}\n" +
        "Status    : [${statuscode}] ${status}\n" +
        "Location  : ${geopoint}\n" +
        "Address   : ${address}\n" +
        "Speed     : ${speed}  ${direction}\n" +
        "Altitude  : ${altitude}\n" +
        //"Message   : ${message}\n" +
        "\n";

    // ------------------------------------------------------------------------
    
    public static final String PROP_RuleFactoryLite_emailEnabled    = "RuleFactoryLite.emailEnabled";
    
    private static int SEND_NOTIFICATION = -1; // set to '1' (true) for production

    /* debug testing */
    public static void DebugSetSendNotification(boolean send)
    {
        SEND_NOTIFICATION = send? 1 : 0;
    }
    
    /* Returns true if ok to send email notifications */
    public static boolean SendNotification()
    {
        if (SEND_NOTIFICATION < 0) {
            SEND_NOTIFICATION = RTConfig.getBoolean(PROP_RuleFactoryLite_emailEnabled,true)? 1 : 0;
        }
        return (SEND_NOTIFICATION == 1);
    }

    // ------------------------------------------------------------------------
    
    private static String SEL_CRON                  = "cron";

    private static String SEL_TRUE                  = "true";
    private static String SEL_FALSE                 = "false";
    private static String SEL_ARRIVE                = "arrive";
    private static String SEL_DEPART                = "depart";
    private static String SEL_SPEED                 = "speed";
    private static String SEL_SPEEDMPH              = "speedmph";
    private static String SEL_CODE                  = "code";
    private static String SEL_PANIC                 = "panic";
    private static String RULE_FUNCTIONS[]          = new String[] {
        SEL_TRUE,
        SEL_FALSE,
        SEL_ARRIVE,
        SEL_DEPART,
        SEL_SPEED,
        SEL_SPEEDMPH,
        SEL_CODE,
        SEL_PANIC,
    };

    private static String ARG_SEPARATOR[]           = new String[] { "(", ")" };
    
    private static char   SELECTOR_SEPARATOR_CHAR   = ',';

    // ------------------------------------------------------------------------

    /* interface for rule evaluation functions */
    private static abstract class RuleFunction
    {
        public abstract Object evaluate(EventData ev, String arg);
        public abstract String getUsage();
        public abstract String getName(Locale locale);
        public abstract String getDescription();
        public String _convertToENRE(String arg) { return ""; } // EXPERIMENTAL (may be omitted)
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Map<String,RuleFunction> ftnMap  = null;
    
    /* instance of RuleFactory */
    public RuleFactoryLite() 
    {
        super();
        this.ftnMap = new HashMap<String,RuleFunction>();

        /* I18N */

        /* "True" */
        this.ftnMap.put(SEL_TRUE, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                return Boolean.TRUE;
            }
            public String getUsage() {
                return SEL_TRUE;
            }
            public String getName(Locale locale) {
                I18N i18n = I18N.getI18N(RuleFactoryLite.class, locale);
                return i18n.getString("RuleFactoryLite.function.true", "True");
            }
            public String getDescription() {
                return "True";
            }
            public String _convertToENRE(String arg) { // EXPERIMENTAL (may be omitted)
                return "true";
            }
        });

        /* "False" */
        this.ftnMap.put(SEL_FALSE, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                return Boolean.FALSE;
            }
            public String getUsage() {
                return SEL_FALSE;
            }
            public String getName(Locale locale) {
                I18N i18n = I18N.getI18N(RuleFactoryLite.class, locale);
                return i18n.getString("RuleFactoryLite.function.false", "False");
            }
            public String getDescription() {
                return "False";
            }
            public String _convertToENRE(String arg) { // EXPERIMENTAL (may be omitted)
                return "false";
            }
        });

        /* "arrive" */
        this.ftnMap.put(SEL_ARRIVE, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                if (ev.getStatusCode() == StatusCodes.STATUS_GEOFENCE_ARRIVE) {
                    String gzID = ev.getGeozoneID();
                    if (StringTools.isBlank(arg) || StringTools.isBlank(gzID)) {
                        return Boolean.TRUE;
                    } else
                    if (ListTools.contains(StringTools.split(arg,'|'),gzID)) {
                        return Boolean.TRUE;
                    }
                }
                return Boolean.FALSE;
            }
            public String getUsage() {
                return SEL_ARRIVE;
            }
            public String getName(Locale locale) {
                I18N i18n = I18N.getI18N(RuleFactoryLite.class, locale);
                return i18n.getString("RuleFactoryLite.function.geozoneArrive", "Geozone Arrive");
            }
            public String getDescription() {
                return "True if Event code is STATUS_GEOFENCE_ARRIVE";
            }
            public String _convertToENRE(String arg) { // EXPERIMENTAL (may be omitted)
                if (StringTools.isBlank(arg)) {
                    return "$ARRIVE";
                } else {
                    StringBuffer sb = new StringBuffer();
                    String gz[] = StringTools.split(arg,'|');
                    for (int i = 0; i < gz.length; i++) {
                        if (!StringTools.isBlank(gz[i])) {
                            if (sb.length() > 0) { sb.append("||"); }
                            sb.append("$ARRIVE(\"").append(gz[i]).append("\")");
                        }
                    }
                    if (sb.length() > 0) {
                        return "(" + sb + ")";
                    } else {
                        return "$ARRIVE";
                    }
                }
            }
        });

        /* "depart" */
        this.ftnMap.put(SEL_DEPART, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                if (ev.getStatusCode() == StatusCodes.STATUS_GEOFENCE_DEPART) {
                    String gzID = ev.getGeozoneID();
                    if (StringTools.isBlank(arg) || StringTools.isBlank(gzID)) {
                        return Boolean.TRUE;
                    } else
                    if (ListTools.contains(StringTools.split(arg,'|'),gzID)) {
                        return Boolean.TRUE;
                    }
                }
                return Boolean.FALSE;
            }
            public String getUsage() {
                return SEL_DEPART;
            }
            public String getName(Locale locale) {
                I18N i18n = I18N.getI18N(RuleFactoryLite.class, locale);
                return i18n.getString("RuleFactoryLite.function.geozoneDepart", "Geozone Depart");
            }
            public String getDescription() {
                return "True if Event code is STATUS_GEOFENCE_DEPART";
            }
            public String _convertToENRE(String arg) { // EXPERIMENTAL (may be omitted)
                if (StringTools.isBlank(arg)) {
                    return "$DEPART";
                } else {
                    StringBuffer sb = new StringBuffer();
                    String gz[] = StringTools.split(arg,'|');
                    for (int i = 0; i < gz.length; i++) {
                        if (!StringTools.isBlank(gz[i])) {
                            if (sb.length() > 0) { sb.append("||"); }
                            sb.append("$DEPART(\"").append(gz[i]).append("\")");
                        }
                    }
                    if (sb.length() > 0) {
                        return "(" + sb + ")";
                    } else {
                        return "$DEPART";
                    }
                }
            }
        });

        /* "speed(limitKPH)" */
        this.ftnMap.put(SEL_SPEED, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                double evSpeedKPH = ev.getSpeedKPH();
                // check Device speed limit
                Device dev = ev.getDevice();
                if (dev != null) {
                    double devSpeedLimKPH = dev.getSpeedLimitKPH();
                    if ((devSpeedLimKPH > 0.0) && (evSpeedKPH > devSpeedLimKPH)) {
                        return Boolean.TRUE;
                    }
                }
                // check Geozone speed limit
                if (ev.hasGeozone()) {
                    Geozone gz = ev.getGeozone();
                    double gzSpeedLimKPH = (gz != null)? gz.getSpeedLimitKPH() : 0.0;
                    if ((gzSpeedLimKPH > 0.0) && (evSpeedKPH > gzSpeedLimKPH)) {
                        return Boolean.TRUE;
                    }
                }
                // check EventData speed limit
                double evSpeedLimKPH = ev.getSpeedLimitKPH();
                if ((evSpeedLimKPH > 0.0) && (evSpeedKPH > evSpeedLimKPH)) {
                    return Boolean.TRUE;
                }
                // check default speed (km/h)
                double dftSpeedLimKPH = StringTools.parseDouble(arg, 0.0);
                if ((dftSpeedLimKPH > 0.0) && (evSpeedKPH > dftSpeedLimKPH)) {
                    return Boolean.TRUE;
                }
                // return default
                return Boolean.FALSE;
            }
            public String getUsage() {
                return SEL_SPEED;
            }
            public String getName(Locale locale) {
                I18N i18n = I18N.getI18N(RuleFactoryLite.class, locale);
                return i18n.getString("RuleFactoryLite.function.excessSpeedKPH", "Excess Speed (km/h)");
            }
            public String getDescription() {
                return "True if Event speed km/h is greater than the specified parameter";
            }
            public String _convertToENRE(String arg) { // EXPERIMENTAL (may be omitted)
                double dftKPH = StringTools.parseDouble(arg, 0.0);
                return (dftKPH > 0.0)? ("$SPEEDING("+StringTools.format(dftKPH,"0.0")+")") : "$SPEEDING";
            }
        });

        /* "speedmph(limitMPH)" */
        this.ftnMap.put(SEL_SPEEDMPH, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                double evSpeedKPH = ev.getSpeedKPH();
                // check device speed limit
                Device dev = ev.getDevice();
                if (dev != null) {
                    double devSpeedKPH = dev.getSpeedLimitKPH();
                    if ((devSpeedKPH > 0.0) && (evSpeedKPH > devSpeedKPH)) {
                        return Boolean.TRUE;
                    }
                }
                // check default speed (mph)
                double dftSpeedKPH = StringTools.parseDouble(arg, 0.0) * GeoPoint.KILOMETERS_PER_MILE;
                if ((dftSpeedKPH > 0.0) && (evSpeedKPH > dftSpeedKPH)) {
                    return Boolean.TRUE;
                }
                // return default
                return Boolean.FALSE;
            }
            public String getUsage() {
                return SEL_SPEEDMPH;
            }
            public String getName(Locale locale) {
                I18N i18n = I18N.getI18N(RuleFactoryLite.class, locale);
                return i18n.getString("RuleFactoryLite.function.excessSpeedMPH", "Excess Speed (mph)");
            }
            public String getDescription() {
                return "True if Event speed mph is greater than the specified parameter";
            }
            public String _convertToENRE(String arg) { // EXPERIMENTAL (may be omitted)
                double dftKPH = StringTools.parseDouble(arg, 0.0) * GeoPoint.KILOMETERS_PER_MILE;
                return (dftKPH > 0.0)? ("$SPEEDING("+StringTools.format(dftKPH,"0.0")+")") : "$SPEEDING";
            }
        });

        /* "code(statusCode)" */
        this.ftnMap.put(SEL_CODE, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                int statusCode = StringTools.parseInt(arg, StatusCodes.STATUS_NONE);
                if (ev.getStatusCode() == statusCode) {
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            }
            public String getUsage() {
                return SEL_CODE;
            }
            public String getName(Locale locale) {
                I18N i18n = I18N.getI18N(RuleFactoryLite.class, locale);
                return i18n.getString("RuleFactoryLite.function.statusCode", "Status Code");
            }
            public String getDescription() {
                return "True if Event status code matches the specified parameter";
            }
            public String _convertToENRE(String arg) { // EXPERIMENTAL (may be omitted)
                int statusCode = StringTools.parseInt(arg, StatusCodes.STATUS_NONE);
                return "(statusCode==0x" + StringTools.toHexString(statusCode,16) + ")";
            }
        });

        /* "panic" (specific status code) */
        this.ftnMap.put(SEL_PANIC, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                int statusCode = StringTools.parseInt(arg, StatusCodes.STATUS_PANIC_ON);
                if (ev.getStatusCode() == statusCode) {
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            }
            public String getUsage() {
                return SEL_PANIC;
            }
            public String getName(Locale locale) {
                I18N i18n = I18N.getI18N(RuleFactoryLite.class, locale);
                return i18n.getString("RuleFactoryLite.function.panicCode", "Panic");
            }
            public String getDescription() {
                return "True if Event status code matches StatusCodes.STATUS_PANIC_ON";
            }
            public String _convertToENRE(String arg) { // EXPERIMENTAL (may be omitted)
                int statusCode = StringTools.parseInt(arg, StatusCodes.STATUS_PANIC_ON);
                return "(statusCode==0x" + StringTools.toHexString(statusCode,16) + ")";
            }
        });

        // --------------------------------------------------------------------

        /* Cron test */
        this.ftnMap.put(SEL_CRON, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                // TODO: fill this section in with your custom 'cron' test
                Print.logInfo("'Cron' test default returns FALSE");
                return Boolean.FALSE;
            }
            public String getUsage() {
                return SEL_CRON;
            }
            public String getName(Locale locale) {
                I18N i18n = I18N.getI18N(RuleFactoryLite.class, locale);
                return i18n.getString("RuleFactoryLite.function.cronTest", "Cron Test");
            }
            public String getDescription() {
                return "True if the implemented Cron test returns True";
            }
        });

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return the RuleFactory name */
    public String getName()
    {
        return "RuleFactoryLite";
    }

    /* return the RuleFactory version */
    public String getVersion()
    {
        return RuleFactoryLite.VERSION;
    }

    /* consitancy check */
    public boolean checkRuntime()
    {
        // return true if this module has all of its required components
        return true;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return an array of available identifiers */
    public java.util.List<String> getIdentifierNames()
    {
        return new Vector<String>();
    }

    /* return the function description */
    public String getIdentifierDescription(String idName)
    {
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return an array of available functions */
    public java.util.List<String> getFunctionNames()
    {
        return ListTools.toList(RULE_FUNCTIONS);
    }

    /* return the function 'usage' String */
    public String getFunctionUsage(String ftnName)
    {
        RuleFunction ruleFtn = this.getFunction(ftnName);
        return (ruleFtn != null)? ruleFtn.getUsage() : "";
    }

    /* return the function description */
    public String getFunctionDescription(String ftnName)
    {
        RuleFunction ruleFtn = this.getFunction(ftnName);
        return (ruleFtn != null)? ruleFtn.getDescription() : "";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return RuleFunction for specified selector item */
    private RuleFunction getFunction(String selItem)
    {
        if (StringTools.isBlank(selItem)) {
            return null;
        } else {
            int p = selItem.indexOf(ARG_SEPARATOR[0]);
            String selFtn = (p >= 0)? selItem.substring(0,p) : selItem;
            RuleFunction ftn = this.ftnMap.get(selFtn); // case sensitive
            if (ftn == null) {
                Print.logWarn("Function for selector not found: " + selItem);
            }
            return ftn;
        }
    }

    /* extract the argument from the specified selector */
    private String getArgument(String selector)
    {
        if (StringTools.isBlank(selector)) {
            return null;
        } else {
            int p = selector.indexOf(ARG_SEPARATOR[0]);
            if (p < 0) {
                return null;
            } else {
                String a = selector.substring(p + ARG_SEPARATOR[0].length());
                if (a.endsWith(ARG_SEPARATOR[1])) {
                    a = a.substring(0, a.length() - ARG_SEPARATOR[1].length());
                }
                return a;
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return the selector for the specified rule-id
    *** @param account the Account
    *** @param ruleID  the rule-id
    *** @return The rule selector, or null if the rule-id is not defined
    **/
    public String getRuleSelector(Account account, String ruleID)
    {
        return null;
    }
    
    // ------------------------------------------------------------------------

    /* check selector syntax */
    public boolean checkSelectorSyntax(String selector) 
    {
        if (StringTools.isBlank(selector)) {
            // assume that an empty selector is valid
            // (however, note that an empty/null selector is not a 'match')
            return true;
        } else {
            String sel[] = StringTools.split(selector, SELECTOR_SEPARATOR_CHAR);
            for (int i = 0; i < sel.length; i++) {
                if (!this._checkSelectorSyntax(sel[i])) {
                    return false;
                }
            }
            return true;
        }
    }
    
    private boolean _checkSelectorSyntax(String selItem)
    {
        if (StringTools.isBlank(selItem)) {
            return true;
        } else {
            int p = selItem.indexOf(ARG_SEPARATOR[0]);
            String selFtn = (p < 0)? selItem : selItem.substring(0,p);
            return this.ftnMap.containsKey(selFtn);
        }
    }

    // ------------------------------------------------------------------------

    /* return true if the specified selector matches the specified event record */
    public boolean isSelectorMatch(String selector, EventData event) 
    {
        if ((event != null) && !StringTools.isBlank(selector)) {
            String sel[] = StringTools.split(selector, SELECTOR_SEPARATOR_CHAR);
            RuleFunction ftn = this._getSelectorMatch(sel, event);
            return (ftn != null)? true : false;
        }
        return false;
    }

    /* return true if the specified selector matches the specified event record */
    private RuleFunction _isSelectorMatch(String selector, EventData event) 
    {
        if ((event != null) && !StringTools.isBlank(selector)) {
            String sel[] = StringTools.split(selector, SELECTOR_SEPARATOR_CHAR);
            RuleFunction ftn = this._getSelectorMatch(sel, event);
            return ftn;
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /* return true if the specified selector matches the specified event record */
    private RuleFunction _getSelectorMatch(String sel[], EventData event) 
    {
        if ((event != null) && !ListTools.isEmpty(sel)) {
            for (int i = 0; i < sel.length; i++) {
                RuleFunction ftn = this._getSelectorMatch(sel[i], event);
                if (ftn != null) {
                    return ftn;
                }
            }
        }
        return null;
    }

    /* return true if the specified selector matches the specified event record */
    private RuleFunction _getSelectorMatch(String sel, EventData event) 
    {
        
        /* valid selector */
        if (StringTools.isBlank(sel)) {
            return null;
        }

        /* get function */
        RuleFunction ftn = this.getFunction(sel);
        if (ftn == null) {
            // no function, no match
            return null;
        }

        /* evaluate function */
        String arg = this.getArgument(sel);
        Object eval = ftn.evaluate(event, arg);
        if (eval == null) {
            // no valid result, return false
            return null;
        }
        
        /* return boolean results */
        if (eval instanceof Boolean) {
            // return Boolean value
            return ((Boolean)eval).booleanValue()? ftn : null;
        } else
        if (eval instanceof Number) {
            // return true if Number is non-zero
            return (((Number)eval).longValue() != 0L)? ftn : null;
        } else {
            // return true for everything else
            return ftn;
        }

    }

    // ------------------------------------------------------------------------

    /* return true if the specified selector matches the specified event record */
    public boolean isSelectorMatch(String selector, Account account) 
    {
        return false;
    }

    // ------------------------------------------------------------------------

    /* EXPERIMENTAL: Convert "RuleFactoryLite" selector to "GTSRuleFactory" */
    // Not part of the "RuleFactory" interface
    public String convertToENRE(String selector) 
    {
        String sel[] = StringTools.split(selector, SELECTOR_SEPARATOR_CHAR);
        StringBuffer enre = new StringBuffer();
        boolean compoundSel = false;
        for (int i = 0; i < sel.length; i++) {
            RuleFunction ftn = this.getFunction(sel[i]);
            if (ftn != null) {
                String arg = this.getArgument(sel[i]);
                String rf  = ftn._convertToENRE(arg);
                if (!StringTools.isBlank(rf)) {
                    if (enre.length() > 0) {
                        enre.append("||");
                        compoundSel = true;
                    }
                    enre.append(rf);
                }
            }
        }
        String enreStr = StringTools.blankDefault(enre.toString().trim(),"false");
        if (enreStr.startsWith("(") && enreStr.endsWith(")")) {
            return enreStr;
        } else {
            return "("+enreStr+")";
        }
    }

    // ------------------------------------------------------------------------

    /* return the result of the specified selector */
    public Object evaluateSelector(String selector, EventData event) 
    {
        String sel[] = StringTools.split(selector, SELECTOR_SEPARATOR_CHAR);
        Object eval = null;
        for (int i = 0; i < sel.length; i++) {
            eval = this._evaluateSelector(sel[i],event);
            if ((eval instanceof Boolean) && ((Boolean)eval).booleanValue()) {
                // return Boolean value
                return eval;
            } else
            if ((eval instanceof Number) && (((Number)eval).longValue() != 0L)) {
                // return Number value
                return eval;
            }
        }
        return eval; // return last value
    }

    /* return the result of the specified selector */
    private Object _evaluateSelector(String sel, EventData event) 
    {

        /* get function */
        RuleFunction ftn = this.getFunction(sel);
        if (ftn == null) {
            // no function
            return null;
        }

        /* evaluate and return */
        String arg = this.getArgument(sel);
        return ftn.evaluate(event, arg);

    }

    // ------------------------------------------------------------------------

    /* check rule selector and perform action */
    public Object evaluateSelector(String selector, Account account) 
    {
        return Boolean.FALSE;
    }

    // ------------------------------------------------------------------------

    /**
    *** Checks the specified rule 'selector' against the specified event, and executes
    *** the default action devices by the current Device.
    *** @param sel  The rule selector
    *** @param event  The current event
    *** @return The executed action-mask (or -1 if no action was triggered)
    **/
    public int executeSelector(String sel, EventData event) 
    {

        /* skip if no event */
        if (event == null) {
            Print.logError("Specified event is null");
            return -1;
        }

        /* get device */
        String accountID = event.getAccountID();
        Device device    = event.getDevice();
        if (device == null) {
            Print.logError("EventData Device not found: " + accountID + "/" + event.getDeviceID());
            return -1;
        }

        /* check for match */
        RuleFunction ftn = this._isSelectorMatch(sel, event);
        if (ftn != null) {
            Print.logInfo("Selector matched: " + sel + " [" + ftn.getName(null) + "]");
            int actionMask = device.getNotifyAction();
            this._performAction(event, ftn, 
                actionMask, 
                null, null, null);
            return actionMask;
        } else {
            Print.logDebug("Selector match returned false: " + sel);
            return -1;
        }

    }

    /**
    *** Checks the specified rule 'selector' against the specified event, and executes
    *** the default action devices by the current Device.
    *** @param sel  The rule selector
    *** @param event  The current event
    *** @return The executed action-mask (or -1 if no action was triggered)
    **/
    private int _cronExecuteSelector(String sel, EventData event,
        int actionMask, 
        String emailTo, String emailSubj, String emailBody) 
    {

        /* skip if no event */
        if (event == null) {
            Print.logError("Specified event is null");
            return -1;
        }

        /* get device */
        String accountID = event.getAccountID();
        Device device    = event.getDevice();
        if (device == null) {
            Print.logError("EventData Device not found: " + accountID + "/" + event.getDeviceID());
            return -1;
        }

        /* check for match */
        RuleFunction ftn = this._isSelectorMatch(sel, event);
        if (ftn != null) {
            Print.logInfo("Selector matched: " + sel + " [" + ftn.getName(null) + "]");
            this._performAction(event, ftn, 
                actionMask, 
                emailTo, emailSubj, emailBody);
            return actionMask;
        } else {
            Print.logDebug("Selector match returned false: " + sel);
            return -1;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** May call an extended set of rule definitions
    *** @param event  The current incoming EventData record
    *** @return True if at least one of the exectued rules was triggered
    **/
    public int executeRules(EventData event) 
    {
        // not supported in this 'Lite' implementation
        return -1;
    }

    // ------------------------------------------------------------------------

    /* example notification action */
    protected boolean _performAction(EventData evdb, RuleFunction ftn, 
        int actionMask, 
        String emailTo, String emailSubj, String emailBody)
    {
        int actCnt = 0;

        /* default action */
        if ((actionMask < 0) || (actionMask == RuleFactory.ACTION_NONE)) {
            actionMask = RuleFactory.ACTION_DEFAULT;
        }
        if ((actionMask & ACTION_NOTIFY_MASK) != 0) {
            actionMask |= RuleFactory.ACTION_VIA_EMAIL;
        }

        /* email */
        if ((actionMask & RuleFactory.ACTION_VIA_EMAIL) != 0) {
            boolean sent = this._sendNotification(evdb, ftn, 
                actionMask, 
                emailTo, emailSubj, emailBody);
            if (sent) {
                actCnt++;
            }
        }

        /* queue */

        /* listener */

        /* return true if an action was triggered */
        return (actCnt > 0)? true : false;

    }

    // ------------------------------------------------------------------------

    /* example notification action */
    protected boolean _sendNotification(EventData evdb, RuleFunction ftn, 
        int actionMask, 
        String devEmailTo, String devEmailSubj, String devEmailBody)
    {

        /* EvendData specified? */
        if (evdb == null) {
            // should not occur
            Print.logWarn("EventData is null ...");
            return false;
        }

        /* account/device */
        Account account = evdb.getAccount();
        Device  device  = evdb.getDevice();
        if ((account == null) || (device == null)) {
            // unlikely to occur
            Print.logWarn("Account/Device not found ...");
            return false;
        }

        /* private label */
        BasicPrivateLabel privLabel = account.getPrivateLabel();
        if (privLabel == null) {
            // unlikely to occur
            Print.logWarn("No PrivateLabel found for Account: " + account.getAccountID());
        }

        /* accumulate recipients */
        HashSet<String> emailTo = new HashSet<String>();
        // Account recipients
        if ((actionMask & RuleFactory.ACTION_NOTIFY_ACCOUNT) != 0) {
            String email = account.getNotifyEmail();
            if (!email.equals("")) {
                emailTo.add(email);
            }
        }
        // Device recipients
        if ((actionMask & RuleFactory.ACTION_NOTIFY_DEVICE) != 0) {
            String email = StringTools.blankDefault(devEmailTo, device.getNotifyEmail());
            if (!email.equals("")) {
                emailTo.add(email);
            }
        }
        // Accumulate recipients into a single string
        StringBuffer toEmailSB = new StringBuffer();
        for (Iterator i = emailTo.iterator(); i.hasNext();) {
            String emailAddr = (String)i.next();
            if (toEmailSB.length() > 0) { toEmailSB.append(","); }
            toEmailSB.append(emailAddr);
        }

        /* "To:" email address */
        String toSMS   = "";
        String toEmail = toEmailSB.toString();
        if (!StringTools.isBlank(toEmail)) {
            //Print.logInfo("EMail list before: " + toEmail);
            String toArray[] = StringTools.parseString(toEmail,',');
            // we generally expect all recipients to be an email address.
            // we look through the list specifically for SMS recipients
            for (String to : toArray) {
                // we only go through the following code if we find an explicit SMS address
                if (SMSOutboundGateway.StartsWithSMS(to)) {
                    // we've found at least one SMS recipient
                    StringBuffer sms   = new StringBuffer();
                    StringBuffer email = new StringBuffer();
                    for (String R : toArray) {
                        if (StringTools.isBlank(R)) {
                            // ignore blank entry
                        } else
                        if (SMSOutboundGateway.StartsWithSMS(R)) {
                            if (sms.length() > 0) { sms.append(","); }
                            sms.append(SMSOutboundGateway.RemovePrefixSMS(R));
                        } else {
                            if (email.length() > 0) { email.append(","); }
                            email.append(R);
                        }
                    }
                    toSMS   = sms.toString();
                    toEmail = email.toString();
                    break; // exit 'check' loop
                }
            }
            //Print.logInfo("EMail list after: " + toEmail);
        }

        /* "From:" email address */
        String frEmail = (privLabel != null)? privLabel.getEventNotificationFrom() : null;
        if (StringTools.isBlank(frEmail) || frEmail.endsWith("example.com")) {
            frEmail = (privLabel != null)? privLabel.getEMailAddress(BasicPrivateLabel.EMAIL_TYPE_NOTIFY) : null;
            if (StringTools.isBlank(frEmail) || frEmail.endsWith("example.com")) {
                frEmail = account.getContactEmail();
                // may still not be defined
            }
        }

        /* subject/body message template */
        String subjTemplate = null;
        String bodyTemplate = null;
        if (device.getNotifyUseWrapper()) {
            if (privLabel != null) {
                subjTemplate = privLabel.getEventNotificationSubject();
                bodyTemplate = privLabel.getEventNotificationBody();
            }
            if (StringTools.isBlank(subjTemplate)) {
                subjTemplate = DEFAULT_SUBJECT;
            }
            if (StringTools.isBlank(bodyTemplate)) {
                bodyTemplate = DEFAULT_BODY;
            }
        } else {
            subjTemplate = START_DELIM + KEY_SUBJECT[0] + END_DELIM;
            bodyTemplate = START_DELIM + KEY_MESSAGE[0] + END_DELIM;
        }

        /* template subject/body */
        String ntfySubj = StringTools.blankDefault(devEmailSubj, device.getNotifySubject());
        if (StringTools.isBlank(ntfySubj)) { ntfySubj = device.getNotifyDescription(); }
        String ntfyText = StringTools.blankDefault(devEmailBody, device.getNotifyText()   );
        if (StringTools.isBlank(ntfyText)) { ntfyText = device.getNotifyDescription(); }

        /* subject/body */
        Locale locale   = (privLabel != null)? privLabel.getLocale() : null;
        EventDataText edt = new EventDataText(evdb, ftn, ntfySubj, ntfyText, privLabel);
        String subj = edt.toString(subjTemplate);
        String body = edt.toString(bodyTemplate);
        String smsm = null;

        /* debug logging */
        Print.logDebug("From   : "  + frEmail);
        Print.logDebug("To     : "  + toEmail);
        Print.logDebug("Subject: "  + subj);
        Print.logDebug("Body   :\n" + body);
        if (!StringTools.isBlank(smsm)) {
        Print.logDebug("SMS    : " + smsm);
        }

        /* action results */
        boolean success = true;

        /* skip notification */
        if (!SendNotification()) {
            // typically used for debugging only
            Print.logWarn("EMail notification currently disabled");
            Print.logWarn("(ie. RT Property '"+PROP_RuleFactoryLite_emailEnabled+"' is 'false'");
            return success;
        }

        /* send email */
        if ((actionMask & RuleFactory.ACTION_VIA_EMAIL) != 0) {
            if (StringTools.isBlank(toEmail) && StringTools.isBlank(toSMS)) {
                
                // no-one to which to send email
                Print.logWarn("EMail/SMS requested, but no recipients specified");
                success = false;

            } else {

                /* SendMail */
                if (!StringTools.isBlank(toEmail)) {
                    if (StringTools.isBlank(frEmail)) {
                        // PROBLEM HERE: We haven't set a valid 'From' address in the config file.
                        // Issue a stack-trace to make sure we get noticed.
                        Print.logWarn("Cannot determine 'From' email address!!!");
                        success = false;
                    } else {
                        try {
                            Print.logInfo("Sending rule notification email ...");
                            SendMail.SmtpProperties smtpProps = privLabel.getSmtpProperties();
                            SendMail.send(frEmail,toEmail,null,null,subj,body,null,smtpProps);
                        } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                            // this will fail if JavaMail support for SendMail is not available.
                            Print.logWarn("SendMail error: " + t);
                            success = false;
                        }
                    }
                }

                /* Send SMS */
                if (!StringTools.isBlank(toSMS)) {
                    // message
                    String smsMessage = !StringTools.isBlank(smsm)?
                        smsm.trim().replace('\n',' ') :
                        (subj + " " + body).trim().replace('\n',' ');
                    smsMessage = SMSOutboundGateway.truncateTextMessageToMaximumLength(smsMessage);
                    // SMS gateway
                    String    smsGatewayName = SMSOutboundGateway.GetDefaultGatewayName();
                    SMSOutboundGateway smsGW = SMSOutboundGateway.GetSMSGateway(smsGatewayName);
                    if (smsGW != null) {
                        Print.logInfo("Sending SMS via gateway: " + smsGatewayName);
                        // list of SMS recipients
                        String smsPhoneList[] = StringTools.split(toSMS,',');
                        for (String smsPhone : smsPhoneList) {
                            smsPhone = SMSOutboundGateway.RemovePrefixSMS(smsPhone);
                            if (!StringTools.isBlank(smsPhone)) {
                                Print.logInfo("SMS: " + smsPhone + " --> " + smsMessage);
                                DCServerFactory.ResultCode result = smsGW.sendSMSMessage(account, smsMessage, smsPhone);
                                if (!result.isSuccess()) {
                                    Print.logWarn("SMS error: " + result);
                                    success = false;
                                }
                            }
                        }
                    } else {
                        Print.logWarn("SMS Gateway not found: " + smsGatewayName);
                    }
                }

            }
        }

        /* callback listener */
        if ((actionMask & RuleFactory.ACTION_VIA_LISTENER) != 0) {
            // TODO: implement callback listener
        }

        /* queue notification message */
        if ((actionMask & RuleFactory.ACTION_VIA_QUEUE) != 0) {
            // TODO: queue notofication message
        }

        /* return success */
        return success;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static class EventDataText
        implements StringTools.KeyValueMap // ReplacementMap
    {
        private EventData         event       = null;
        private RuleFunction      function    = null;
        private BasicPrivateLabel privLabel   = null;
        private String            notifySubj  = null;
        private String            notifyBody  = null;
        public EventDataText(EventData event, RuleFunction ftn, String msgSubj, String msgBody, BasicPrivateLabel privLabel) {
            this.event     = event;     // should not be null
            this.function  = ftn;
            this.privLabel = privLabel;
            if (!StringTools.isBlank(msgSubj)) {
                this.notifySubj = StringTools.insertKeyValues(msgSubj, 
                    START_DELIM, END_DELIM, DFT_DELIM,
                    this);
            } else {
                this.notifySubj = null;
            }
            if (!StringTools.isBlank(msgBody)) {
                String b = StringTools.insertKeyValues(msgBody, 
                    START_DELIM, END_DELIM, DFT_DELIM,
                    this);
                this.notifyBody = StringTools.replace(b, "\\n", "\n");
            } else {
                this.notifyBody = null;
            }
        }
        public String toString(String format) {
            return StringTools.insertKeyValues(format, 
                START_DELIM, END_DELIM, DFT_DELIM,
                this);
        }
        public String toString() {
            return this.toString(DEFAULT_BODY);
        }
        public String getKeyValue(String key, String arg, String dft) {
            if (ListTools.containsIgnoreCase(KEY_RULENAME,key)) {
                if (this.function != null) {
                    Account acct = (this.event != null)? this.event.getAccount() : null;
                    Locale locale = (acct != null)? acct.getLocale() : null;
                    return this.function.getName(locale);
                } else {
                    return dft;
                }
            } else
            if (ListTools.containsIgnoreCase(KEY_SUBJECT,key)) {
                return (this.notifySubj != null)? this.notifySubj : dft;
            } else
            if (ListTools.containsIgnoreCase(KEY_MESSAGE,key)) {
                return (this.notifyBody != null)? this.notifyBody : dft;
            } else {
                String fldStr = (this.event != null)? this.event.getKeyFieldValue(key,arg,this.privLabel) : null;
                return (fldStr != null)? fldStr : ("(" + key + ")"); // dft
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String CRON_noemail[]      = new String[] { "noemail" , "debug" };
    private static final String CRON_account[]      = new String[] { "account" , "acct"  };
    private static final String CRON_selector[]     = new String[] { "selector"          };
    private static final String CRON_emailTo[]      = new String[] { "emailTo"           };
    private static final String CRON_emailSubj[]    = new String[] { "emailSubj"         };
    private static final String CRON_emailBody[]    = new String[] { "emailBody"         };

    private static RuleFactoryLite CronRuleFactInstance = null;

    /* "crontab" entry point */
    public static int cron(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
      //RTConfig.setDebugMode(true);
        Print.setLogLevel(Print.LOG_ALL);
        Print.setLogHeaderLevel(Print.LOG_ALL);

        /* SendMail thread mode */
        if (RTConfig.getBoolean(CRON_noemail,false)) {
            SendMail.SetThreadModel(SendMail.THREAD_NONE);
        } else {
            SendMail.SetThreadModel(SendMail.THREAD_CURRENT);
        }

        /* rule selector */
        String selector = RTConfig.getString(CRON_selector, null);
        if (StringTools.isBlank(selector)) {
            Print.logWarn("Missing Rule Selector specification");
            return 1;
        }

        /* list of accounts */
        String accountIDs = RTConfig.getString(CRON_account, "");
        Collection<String> accountList = null;
        if (accountIDs.equals(SELECT_ALL)) {
            try {
                accountList = Account.getAllAccounts();
            } catch (DBException dbe) {
                Print.logException("Error getting list of Accounts", dbe);
                return 99;
            }
        } else {
            accountList = ListTools.toList(StringTools.split(accountIDs,','));
        }
        if (ListTools.isEmpty(accountList)) {
            Print.logWarn("No Accounts specified, skipping cron selector check ...");
            return 0; // not an error
        }

        /* Cron rule factory instance */
        if (CronRuleFactInstance == null) {
            CronRuleFactInstance = new RuleFactoryLite();
        }
        
        /* action */
        int    actionMask = RuleFactory.ACTION_DEFAULT;
        String emailTo    = RTConfig.getString(CRON_emailTo, null);
        String emailSubj  = RTConfig.getString(CRON_emailSubj, null);
        String emailBody  = StringTools.replace(RTConfig.getString(CRON_emailBody,null),"\\n","\n");

        /* execute rule selector */
        RuleFactoryLite crf = CronRuleFactInstance;
        int rtn = 0;
        for (String aid : accountList) {
            if (StringTools.isBlank(aid)) { continue; } // quietly skip blank account ids
            Print.logInfo("=============================");
            try {
                Account account = Account.getAccount(aid); // may throw DBException
                OrderedSet<String> devIDSet = Device.getDeviceIDsForAccount(aid, null, false);
                for (String devID : devIDSet) {
                    Print.logInfo("Checking cron-rule for "+aid+"/"+devID+" ["+selector+"]");
                    Device dev = Device.getDevice(account, devID);
                    if (dev != null) {
                        EventData lastEvent = dev.getLastEvent(-1L,false);
                        if (lastEvent != null) {
                            int am = crf._cronExecuteSelector(selector, lastEvent,
                                actionMask, 
                                emailTo, emailSubj, emailBody);
                        }
                    } else {
                        Print.logWarn("Device not found: " + aid + "/" + devID);
                    }
                }
            } catch (DBException dbe) {
                Print.logException("Reading Account/Device", dbe);
                rtn = 99;
            }
        }
        
        /* return result */
        return rtn;

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static void printMatch(RuleFactory ruleFact, EventData event, String sel)
    {
        Print.logInfo("Match: " + sel + " ==> " + ruleFact.isSelectorMatch(sel, event));
    }
    
    /* test example RuleFactory */
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);
        RuleFactory ruleFact = new RuleFactoryLite();
        RuleFactoryLite.DebugSetSendNotification(RTConfig.getBoolean("email",false));

        /* cron */
        if (RTConfig.getBoolean("cron",false)) {
            int rtn = RuleFactoryLite.cron(argv);
            Print.logInfo("Cron returned " + rtn);
            System.exit(0);
        }

        /* toenre */
        if (RTConfig.hasProperty("toenre")) {
            String lite = RTConfig.getString("toenre","false");
            String enre = ((RuleFactoryLite)ruleFact).convertToENRE(lite);
            Print.sysPrintln("Converting RuleFactoryLite selector to ENRE:");
            Print.sysPrintln("  Lite:  " + lite);
            Print.sysPrintln("  ENRE:  " + enre);
            System.exit(0);
        }

        /* test */
        if (RTConfig.getBoolean("test",false)) {
            /* EventData */
            int statusCode = StatusCodes.STATUS_GEOFENCE_ARRIVE;
            EventData.Key evKey = new EventData.Key("demo", "demo", DateTime.getCurrentTimeSec(), statusCode);
            EventData evRcd = evKey.getDBRecord();
            evRcd.setAddress("1234 Somewhere Ln, Somewhere CA, 98765");
            evRcd.setGeoPoint(new GeoPoint(35.12345, -142.12345));
            evRcd.setSpeedKPH(105.0);
            evRcd.setHeading(123.0);
            evRcd.setAltitude(457.0);
            evRcd.setOdometerKM(123456.0);
            evRcd.setDistanceKM(3456.0);
            evRcd.setGeozoneID("test");
            /* test match */
            printMatch(ruleFact, evRcd, SEL_ARRIVE+"()");
            printMatch(ruleFact, evRcd, SEL_ARRIVE+"(test1|test2)");
            printMatch(ruleFact, evRcd, SEL_ARRIVE+"(test1|test2|test)");
            printMatch(ruleFact, evRcd, SEL_DEPART+"()");
            printMatch(ruleFact, evRcd, SEL_DEPART+"(test1|test2)");
            printMatch(ruleFact, evRcd, SEL_DEPART+"(test1|test2|test)");
            printMatch(ruleFact, evRcd, SEL_ARRIVE+"(test),"+SEL_DEPART+"(test1)");
            printMatch(ruleFact, evRcd, SEL_ARRIVE+"(test1),"+SEL_DEPART+"(test)");
            /* execute selector */
            ruleFact.executeSelector(SEL_TRUE, evRcd);
            /* exit */
            System.exit(0);
        }

        /* parse */
        if (RTConfig.hasProperty("parse")) {
            String sel = RTConfig.getString("parse","");
            /* exit */
            System.exit(0);
        }

        /* nothing specified */
        Print.logWarn("Please specify an argument");

    }
    
}
