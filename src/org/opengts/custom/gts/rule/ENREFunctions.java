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
// Refer to "Event Notification Rules Engine Technical Manual" for more info:
//   http://www.geotelematic.com/docs/RulesEngine.pdf
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.custom.gts.rule;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.rule.selector.FunctionMap;
import org.opengts.rule.selector.IdentifierMap;
import org.opengts.rule.event.EventFunctionHandler;
import org.opengts.rule.event.EventIdentifierHandler;
import org.opengts.rule.EventRuleFactory;

import org.opengts.rule.tables.Rule;
import org.opengts.rule.RuleListener;
import org.opengts.rule.EventRuleAction;

/**
*** This class can be modified to install custom Rule functions to be used
*** within the Event Notification RUles Engine.
**/

public class ENREFunctions
{

    /* background thread handler for cannedRuleActions */
    // This is used by the custom "EventRuleAction.addRuleListener(...)" defined below
    private static ThreadPool ENREThreadPool = new ThreadPool("ENREFunctions", 5);

    public ENREFunctions()
    {

        /* get EventRuleFactory */
        RuleFactory ruleFact = Device.getRuleFactory();
        if (!(ruleFact instanceof EventRuleFactory)) {
            return;
        }
        EventRuleFactory erf = (EventRuleFactory)ruleFact;

        // --------------------------------------------------------------------

        /* add functions */
        erf.addFunction(new EventFunctionHandler("$SUM") {
            public String getUsage()        { return "Double $SUM(Double A, Double B [, Double C [, Double D]])"; }
            public String getDescription()  { return "Sum arguments and return value."; }
            public Class  getReturnType()   { return Double.class; }
            public Object getDefaultValue() { return new Double(0.0); }
            public ArgCk  getArgCheck()     { return new ArgCk(0,1,2,3,4,Double.class,Double.class,Double.class,Double.class); }
            public Object invokeFunction(FunctionMap fm, Object args[]) {
                // Useful method calls: (should always check for null/blank returned values)
                //  EventData   event     = GetEventData(fm);
                //  String      accountID = GetAccountID(fm);
                //  Account     account   = GetAccount(fm);
                //  String      deviceID  = GetDeviceID(fm);
                //  Device      device    = GetDevice(fm);
                if (!this.checkArgs(args)) {
                    return null; // invalid arguments
                } else {
                    double accum = 0.0;
                    for (int i = 0; i < args.length; i++) { accum += DoubleValue(args[i]); }
                    return new Double(accum);
                }
            }
        });

        // --------------------------------------------------------------------

        /* add identifiers */
        erf.addIdentifier(new EventIdentifierHandler("random") {
            public String getDescription() { return "Random number between 0.0 and 1.0"; }
            public Object getValue(IdentifierMap idm) {
                // Useful method calls:
                //  EventData   event     = GetEventData(idm);
                //  String      accountID = GetAccountID(idm);
                //  Account     account   = GetAccount(idm);
                //  String      deviceID  = GetDeviceID(idm);
                //  Device      device    = GetDevice(idm);
                return new Double(Math.random());
            }
        });

        // --------------------------------------------------------------------

        /* Add rule trigger action callback listener */
        EventRuleAction.addRuleListener(new RuleListener() {
            /**
            *** Callback to handle a rule notification trigger
            *** @param account    The account which triggered the rule
            *** @param device     The device which triggered the rule (may be null)
            *** @param event      The event which triggered the rule (may be null)
            *** @param isCronMode True is exectued fron the 'Cron' task, 
            ***                   False is executed from a normal incoming event analysis.
            *** @param selector   The rule selector that triggered this notification
            *** @param rule       The rule record from which the selector was obtained. May
            ***                   be null if the selector was not provided by a Rule record.
            **/
            public void handleRuleNotification(
                Account account, Device device, EventData event, 
                boolean isCronMode, String selector, Rule rule) {
                final String accountID = (account != null)? account.getAccountID()  : "";
                final String deviceID  = (device  != null)? device.getDeviceID()    : "";
                final String ruleID    = (rule    != null)? rule.getRuleID()        : "";
                final String cannedAct = (rule    != null)? rule.getCannedActions() : "";
                //Print.logInfo("Rule Action for Account/Device: " + accountID + "/" + deviceID);
                if (StringTools.isBlank(cannedAct)) {
                    String caList[] = StringTools.split(cannedAct,',');
                    for (String craItem : caList) {
                        final String cra[] = StringTools.split(craItem,':');
                        if (ListTools.size(cra) <= 0) {
                            continue;
                        }
                        final String craKey = cra[0];
                        final String craArg = (cra.length > 1)? cra[1] : "";

                        // Example custom rule 'cannedAction' handler that runs in the same
                        // thread as the EventData record insertion handler.  This method
                        // should only be used for processes which are quick to execute, which
                        // typically do not have any external network dependencies.
                        /*
                        if (craKey.equalsIgnoreCase("http.get")) {
                            // This example makes an attempt to send an http request on receipt
                            // of a rule trigger that has "http.get" as a 'cannedAction'
                            int timeoutMS = 2000; // (milliseconds) not in a separate thread, short timeout
                            String url = "http://www.example.com/devInfo?dev=" + deviceID;
                            try {
                                byte resp[] = HTMLTools.readPage_GET(url, timeoutMS);
                                // TODO: handle response?
                            } catch (Throwable th) {
                                Print.logError("Unable to send http 'GET' request: " + url);
                            }
                            return;
                        }
                        */

                        // Example custom rule 'cannedAction' handler that runs in a seaprate
                        // thread from the EventData record insertion handler.  Delays in
                        // procesing are not as critical in this version since it does not
                        // block the EventData record processing.
                        /*
                        if (craKey.equalsIgnoreCase("http.get")) {
                            if (ENREThreadPool != null) {
                                ENREThreadPool.run(new Runnable() {
                                    // This example makes an attempt to send an http request on receipt
                                    // of a rule trigger that has "http.get" as a 'cannedAction'
                                    int timeoutMS = 10000; // (milliseconds) separate thread, longer timeout
                                    String url = "http://www.example.com/devInfo?dev=" + deviceID;
                                    try {
                                        byte resp[] = HTMLTools.readPage_GET(url, timeoutMS);
                                        // TODO: handle response?
                                    } catch (Throwable th) {
                                        Print.logError("Unable to send http 'GET' request: " + url);
                                    }
                                });
                            } else {
                                Print.logError("'ENREThreadPool' is null");
                            }
                        }
                        */

                    }
                }
            }
        });

        // --------------------------------------------------------------------

    }
    
}
