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
// Description:
//  Outbound SMS Gateway support
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

import java.io.IOException;
import java.util.Vector;

import org.apache.xmlrpc.*;
import org.apache.xmlrpc.XmlRpc;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

/**
*** Outbound SMS gateway handler
**/
public abstract class SMSOutboundGateway
{

    // ------------------------------------------------------------------------

    public  static final String SMS_Prefix = "SMS:";
    
    /**
    *** Return true if string starts with "SMS:"
    *** @param val  The string to test
    *** @return True is string starts with "SMS:", false otherwise
    **/
    public static boolean StartsWithSMS(String val)
    {
        return StringTools.startsWithIgnoreCase(val,SMS_Prefix);
    }
    
    /**
    *** Removes any prefixing "SMS:" from the specified string.
    *** @param val  The string from which the prefixing "SMS:" is removed.
    *** @return The specified string, sans the prefixing "SMS:"
    **/
    public static String RemovePrefixSMS(String val)
    {
        val = StringTools.trim(val);
        if (SMSOutboundGateway.StartsWithSMS(val)) {
            return val.substring(SMSOutboundGateway.SMS_Prefix.length()).trim();
        } else {
            return val; // leave as-is
        }
    }

    // ------------------------------------------------------------------------

    public static int getMaximumTextMessageLength()
    {
        return 160;
    }
    
    public static String truncateTextMessageToMaximumLength(String msg)
    {
        msg = StringTools.trim(msg);
        if (StringTools.isBlank(msg)) {
            return "";
        } else
        if (msg.length() > SMSOutboundGateway.getMaximumTextMessageLength()) {
            msg = msg.substring(0,SMSOutboundGateway.getMaximumTextMessageLength()).trim();
            return msg;
        } else {
            return msg;
        }
    }

    // ------------------------------------------------------------------------

    public static boolean isDeviceAuthorized(Device device)
    {

        /* invalid device */
        if (device == null) {
            return false;
        }

        /* invalid account */
        Account account = device.getAccount();
        if (account == null) {
            return false;
        }

        /* SMS enabled? */
        if (!account.getSmsEnabled()) {
            return false;
        }

        /* authorized */
        return true;

    }

    // ------------------------------------------------------------------------

    public  static final String REPL_mobile[]   = new String[] { "%{mobile}"  , "{MOBILE}"  , "${mobile}"   };
    public  static final String REPL_sender[]   = new String[] { "%{sender}"  , "{SENDER}"  , "${sender}"   };
    public  static final String REPL_dataKey[]  = new String[] { "%{dataKey}" , "{DATAKEY}" , "${dataKey}"  };
    public  static final String REPL_modemID[]  = new String[] { "%{modemID}" , "{MODEMID}" , "${modemID}"  };
    public  static final String REPL_imei[]     = new String[] { "%{imei}"    , "{IMEI}"    , "${imei}"     };
    public  static final String REPL_serial[]   = new String[] { "%{serial}"  , "{SERIAL}"  , "${serial}"   };
    public  static final String REPL_user[]     = new String[] { "%{user}"    , "{USER}"    , "${user}"     };
    public  static final String REPL_password[] = new String[] { "%{password}", "{PASSWORD}", "${password}" };
    public  static final String REPL_authID[]   = new String[] { "%{authID}"  , "{AUTHID}"  , "${authID}"   };
    public  static final String REPL_message[]  = new String[] { "%{message}" , "{MESSAGE}" , "${message}"  };

    public static String REPLACE(String s, String r[], String m)
    {
        for (int i = 0; i < r.length; i++) {
            s = StringTools.replace(s, r[i], m);
        }
        return s;
    }

    public static String REPLACE(String s, RTProperties rtp)
    {
        return StringTools.insertKeyValues(s, "%{", "}", "=", rtp);
    }

    // ------------------------------------------------------------------------

    private static final String PROP_SmsGatewayHandler_             = "SmsGatewayHandler.";
    public  static final String PROP_defaultName                    = PROP_SmsGatewayHandler_ + "defaultName";

    /* emailBody */
    public  static final String GW_emailBody                        = "emailBody";
    public  static final String PROP_emailBody_smsEmailAddress      = PROP_SmsGatewayHandler_ + "emailBody.smsEmailAddress";

    /* emailSubject */
    public  static final String GW_emailSubject                     = "emailSubject";
    public  static final String PROP_emailSubject_smsEmailAddress   = PROP_SmsGatewayHandler_ + "emailSubject.smsEmailAddress";

    /* httpURL */
    public  static final String GW_httpURL                          = "httpURL";
    public  static final String PROP_httpURL_url                    = PROP_SmsGatewayHandler_ + "httpURL.url";

    /* Click-A-Tell */
    public  static final String GW_clickatell                       = "clickatell";
    public  static final String PROP_clickatell_smsEmailAddress     = PROP_SmsGatewayHandler_ + "clickatell.smsEmailAddress";
    public  static final String PROP_clickatell_user                = PROP_SmsGatewayHandler_ + "clickatell.user";
    public  static final String PROP_clickatell_password            = PROP_SmsGatewayHandler_ + "clickatell.password";
    public  static final String PROP_clickatell_api_id              = PROP_SmsGatewayHandler_ + "clickatell.api_id";

         /* //MET (Mensajeria Empresarial Telcel) */
    public  static final String GW_met                              = "met";
    public  static final String PROP_met_url                        = PROP_SmsGatewayHandler_ + "met.url";
    public  static final String PROP_met_user                       = PROP_SmsGatewayHandler_ + "met.user";
    public  static final String PROP_met_password                   = PROP_SmsGatewayHandler_ + "met.password";

    /* TextAnywhere: mail2txt, mail2txt160, mail2txtid, mail2txt160id */
    public  static final String GW_mail2txt                         = "mail2txt";
    public  static final String PROP_mail2txt_smsEmailAddress       = PROP_SmsGatewayHandler_ + "mail2txt.smsEmailAddress";
    public  static final String GW_mail2txt160                      = "mail2txt160";
    public  static final String PROP_mail2txt160_smsEmailAddress    = PROP_SmsGatewayHandler_ + "mail2txt160.smsEmailAddress";
    public  static final String GW_mail2txtid                       = "mail2txtid";
    public  static final String PROP_mail2txtid_smsEmailAddress     = PROP_SmsGatewayHandler_ + "mail2txtid.smsEmailAddress";
    public  static final String PROP_mail2txtid_from                = PROP_SmsGatewayHandler_ + "mail2txtid.from";
    public  static final String GW_mail2txt160id                    = "mail2txt160id";
    public  static final String PROP_mail2txt160id_smsEmailAddress  = PROP_SmsGatewayHandler_ + "mail2txt160id.smsEmailAddress";
    public  static final String PROP_mail2txt160id_from             = PROP_SmsGatewayHandler_ + "mail2txt160id.from";

    /* ozekisms */
    public  static final String GW_ozekisms                         = "ozekisms";
    public  static final String PROP_ozekisms_hostPort              = PROP_SmsGatewayHandler_ + "ozekisms.hostPort";
    public  static final String PROP_ozekisms_originator            = PROP_SmsGatewayHandler_ + "ozekisms.originator";
    public  static final String PROP_ozekisms_user                  = PROP_SmsGatewayHandler_ + "ozekisms.user";
    public  static final String PROP_ozekisms_password              = PROP_SmsGatewayHandler_ + "ozekisms.password";

    // ------------------------------------------------------------------------

    public static String GetDefaultGatewayName()
    {
        return RTConfig.getString(PROP_defaultName,GW_emailBody);
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Map<String,SMSOutboundGateway> SmsGatewayHandlerMap = null;

    /**
    *** Add SMS Gateway support provider
    **/
    public static void AddSMSGateway(String name, SMSOutboundGateway smsGW)
    {

        /* validate name */
        if (StringTools.isBlank(name)) {
            Print.logWarn("SMS Gateway name is blank");
            return;
        } else
        if (smsGW == null) {
            Print.logWarn("SMS Gateway handler is null");
            return;
        }
        
        /* initialize map? */
        if (SmsGatewayHandlerMap == null) { 
            SmsGatewayHandlerMap = new HashMap<String,SMSOutboundGateway>(); 
        }

        /* save handler */
        SmsGatewayHandlerMap.put(name.toLowerCase(), smsGW);
        Print.logDebug("Added SMS Gateway Handler: " + name);

    }

    /**
    *** Gets the SMSoutboubdGateway for the specified name
    **/
    public static SMSOutboundGateway GetSMSGateway(String name)
    {

        /* get handler */
        if (StringTools.isBlank(name)) {
            return null;
        } else {
            if (name.equalsIgnoreCase("body")) { 
                name = GW_emailBody; 
            } else
            if (name.equalsIgnoreCase("subject")) { 
                name = GW_emailSubject; 
            }
            return SmsGatewayHandlerMap.get(name.toLowerCase());
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static void AddUrlRtpArg(RTProperties urlRTP, String key, String val)
    {
        if (!StringTools.isBlank(val)) {
            urlRTP.setString(key, URIArg.encodeArg(val));
        }
    }

    /**
    *** Initialize outbound SMS gateway handlers
    **/
    public static void _startupInit()
    {
        Print.logDebug("SMSOutboundGateway initializing ...");

        /* already initialized? */
        if (SmsGatewayHandlerMap != null) {
            return;
        }

        // -----------------------------------------------
        // The following shows several example of outbound SMS gateway support.
        // The only method that needs to be overridden and implemented is
        //   public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr)
        // The "device" is the Device record instance to which the SMS message should be sent,
        // and "commandStr" is the SMS text (device command) which is to be sent to the device.
        // -----------------------------------------------

        /* EMail: standard "Body" command */
        // Property:
        //   SmsGatewayHandler.emailBody.smsEmailAddress=@example.com
        // Notes:
        //   This outbound SMS method sends the SMS text in an email message body to the device
        //   "smsEmailAddress".  If the device "smsEmailAddress" is blank, then the "To" email
        //   address is constructed from the device "simPhoneNumber" and the email address
        //   specified on the property "SmsGatewayHandler.emailBody.smsEmailAddress".
        SMSOutboundGateway.AddSMSGateway(GW_emailBody, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                String frEmail = this.getFromEmailAddress(device);
                String toEmail = this.getSmsEmailAddress(device);
                if (StringTools.isBlank(toEmail)) {
                    String smsPhone = device.getSimPhoneNumber();
                    String smsEmail = this.getStringProperty(device,PROP_emailBody_smsEmailAddress,"");
                    toEmail = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                }
                return this.sendEmail(frEmail, toEmail, ""/*subject*/, commandStr);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                String frEmail  = this.getFromEmailAddress(account);
                String smsEmail = RTConfig.getString(PROP_emailBody_smsEmailAddress,"");
                String toEmail  = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                return this.sendEmail(frEmail, toEmail, ""/*subject*/, smsMessage);
            }
        });

        /* EMail: standard "Subject" command */
        // Property:
        //   SmsGatewayHandler.emailSubject.smsEmailAddress=
        // Notes:
        //   This outbound SMS method sends the SMS text in an email message subject to the device
        //   "smsEmailAddress".  If the device "smsEmailAddress" is blank, then the "To" email
        //   address is constructed from the device "simPhoneNumber" and the email address
        //   specified on the property "SmsGatewayHandler.emailSubject.smsEmailAddress".
        SMSOutboundGateway.AddSMSGateway(GW_emailSubject, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                String frEmail = this.getFromEmailAddress(device);
                String toEmail = this.getSmsEmailAddress(device);
                if (StringTools.isBlank(toEmail)) {
                    String smsPhone = device.getSimPhoneNumber();
                    String smsEmail = this.getStringProperty(device,PROP_emailSubject_smsEmailAddress,"");
                    toEmail = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                }
                return this.sendEmail(frEmail, toEmail, commandStr, ""/*body*/);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                String frEmail  = this.getFromEmailAddress(account);
                String smsEmail = RTConfig.getString(PROP_emailSubject_smsEmailAddress,"");
                String toEmail  = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                return this.sendEmail(frEmail, toEmail, smsMessage, ""/*body*/);
            }
        });

        /* HTTP: URL */
        // Property:
        //   SmsGatewayHandler.httpURL.url=http://localhost:12345/smsredirector/sendsms?flash=0&acctuser=user&tracking_Pwd=pass&source=5551212&destination=${mobile}&message=${message}
        //   SmsGatewayHandler.httpURL.url=http://localhost:12345/sendsms?user=%{user=pass}&pass=%{password=pass}&source=%{sender}&dest=${mobile}&text=${message}
        // Notes:
        //   This outbound SMS method sends the SMS text in an HTTP "GET" request to the URL 
        //   specified on the property "SmsGatewayHandler.httpURL.url".  The following replacement
        //   variables may be specified in the URL string:
        //      ${sender}  - replaced with the Account "contactPhone" field contents
        //      ${mobile}  - replaced with the Device "simPhoneNumber" field contents
        //      ${message} - replaced with the SMS text/command to be sent to the device.
        //   It is expected that the server handling the request understands how to parse and
        //   interpret the various fields in the URL.
        SMSOutboundGateway.AddSMSGateway(GW_httpURL, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                Account account = device.getAccount(); // not null
                RTProperties smsRTP = account.getSmsRTProperties(); // never null
                // replacement values
                String mobile   = device.getSimPhoneNumber();
                String mobile_1 = (mobile.startsWith("1") || (mobile.length() != 10))? mobile : ("1" + mobile);
                String dataKey  = device.getDataKey();
                String modemID  = device.getModemID();
                String imeiNum  = device.getImeiNumber();
                String serial   = device.getSerialNumber();
                String sender   = smsRTP.getString("sender"  ,""); // account.getContactPhone());
                String user     = smsRTP.getString("user"    ,""); // SMS authorization
                String password = smsRTP.getString("password","");
                String authID   = smsRTP.getString("authID"  ,"");
                // create URL
                String httpURL  = this.getStringProperty(device, PROP_httpURL_url, "");
                if (StringTools.isBlank(httpURL)) {
                    Print.logWarn("'"+PROP_httpURL_url+"' not specified");
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                // URL rtp args
                RTProperties urlRTP = new RTProperties();
                AddUrlRtpArg(urlRTP, "mobile"  , mobile  );
                AddUrlRtpArg(urlRTP, "1mobile" , mobile_1);
                AddUrlRtpArg(urlRTP, "sender"  , sender  );
                AddUrlRtpArg(urlRTP, "dataKey" , dataKey );
                AddUrlRtpArg(urlRTP, "modemID" , modemID );
                AddUrlRtpArg(urlRTP, "imei"    , imeiNum );
                AddUrlRtpArg(urlRTP, "serial"  , serial  );
                AddUrlRtpArg(urlRTP, "user"    , user    );
                AddUrlRtpArg(urlRTP, "password", password);
                AddUrlRtpArg(urlRTP, "authID"  , authID  );
                // apply replacement vars to command string
                String message = StringTools.insertKeyValues(commandStr, "%{", "}", "=", urlRTP);
                AddUrlRtpArg(urlRTP, "message" , message );
                // apply replacement vars to URL
                httpURL = StringTools.insertKeyValues(httpURL, "%{", "}", "=", urlRTP);
                // set URL replacement vars (not necessary if replacement vars were handled above)
                httpURL = REPLACE(httpURL, REPL_mobile  , URIArg.encodeArg(mobile  ));  // SIM phone number
                httpURL = REPLACE(httpURL, REPL_sender  , URIArg.encodeArg(sender  ));  // Account contact phone number
                httpURL = REPLACE(httpURL, REPL_dataKey , URIArg.encodeArg(dataKey ));  // Device data key
                httpURL = REPLACE(httpURL, REPL_modemID , URIArg.encodeArg(modemID ));  // Device modem ID
                httpURL = REPLACE(httpURL, REPL_imei    , URIArg.encodeArg(imeiNum ));  // Device IMEI number
                httpURL = REPLACE(httpURL, REPL_serial  , URIArg.encodeArg(serial  ));  // Device serial number
                httpURL = REPLACE(httpURL, REPL_user    , URIArg.encodeArg(user    ));  // SMS auth user
                httpURL = REPLACE(httpURL, REPL_password, URIArg.encodeArg(password));  // SMS auth password
                httpURL = REPLACE(httpURL, REPL_authID  , URIArg.encodeArg(authID  ));  // SMS auth ID
                httpURL = REPLACE(httpURL, REPL_message , URIArg.encodeArg(message ));  // command string
                // send SMS
                try {
                    Print.logInfo("SMS Gateway URL: " + httpURL);
                    byte response[] = HTMLTools.readPage_GET(httpURL, 10000);
                    String resp = StringTools.toStringValue(response);
                    int maxLen = 60;
                    if (resp.length() <= maxLen) {
                        Print.logInfo("SMS Gateway response: " + resp);
                    } else {
                        String R = resp.substring(0, maxLen);
                        Print.logInfo("SMS Gateway response (partial): " + R + " ...");
                    }
                    return DCServerFactory.ResultCode.SUCCESS;
                } catch (UnsupportedEncodingException uee) {
                    Print.logError("URL Encoding: " + uee);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                } catch (NoRouteToHostException nrthe) {
                    Print.logError("Unreachable Host: " + httpURL);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (UnknownHostException uhe) {
                    Print.logError("Unknown Host: " + httpURL);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (FileNotFoundException fnfe) {
                    Print.logError("Invalid URL (not found): " + httpURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (MalformedURLException mue) {
                    Print.logError("Invalid URL (malformed): " + httpURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (Throwable th) {
                    Print.logError("HTML SMS error: " + th);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                }
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                RTProperties smsRTP = account.getSmsRTProperties(); // never null
                // replacement values
                String mobile   = smsPhone;
                String mobile_1 = mobile.startsWith("1")? mobile : ("1" + mobile);
                String message  = smsMessage;
                String sender   = smsRTP.getString("sender"  ,""); // account.getContactPhone());
                String user     = smsRTP.getString("user"    ,""); // SMS authorization
                String password = smsRTP.getString("password","");
                String authID   = smsRTP.getString("authID"  ,"");
                // create URL
                String httpURL  = RTConfig.getString(PROP_httpURL_url, "");
                if (StringTools.isBlank(httpURL)) {
                    Print.logWarn("'"+PROP_httpURL_url+"' not specified");
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                // URL rtp args
                RTProperties urlRTP = new RTProperties();
                AddUrlRtpArg(urlRTP, "mobile"  , mobile  );
                AddUrlRtpArg(urlRTP, "1mobile" , mobile_1);
                AddUrlRtpArg(urlRTP, "sender"  , sender  );
                AddUrlRtpArg(urlRTP, "user"    , user    );
                AddUrlRtpArg(urlRTP, "password", password);
                AddUrlRtpArg(urlRTP, "authID"  , authID  );
                AddUrlRtpArg(urlRTP, "message" , message );
                httpURL = StringTools.insertKeyValues(httpURL, "%{", "}", "=", urlRTP);
                // set URL replacement vars (not necessary if replacement vars were handled above)
                httpURL = REPLACE(httpURL, REPL_mobile  , URIArg.encodeArg(mobile  ));
                httpURL = REPLACE(httpURL, REPL_sender  , URIArg.encodeArg(sender  ));
                httpURL = REPLACE(httpURL, REPL_user    , URIArg.encodeArg(user    ));  // SMS auth user
                httpURL = REPLACE(httpURL, REPL_password, URIArg.encodeArg(password));  // SMS auth password
                httpURL = REPLACE(httpURL, REPL_authID  , URIArg.encodeArg(authID  ));  // SMS auth ID
                httpURL = REPLACE(httpURL, REPL_message , URIArg.encodeArg(message ));
                // send SMS
                try {
                    Print.logInfo("SMS Gateway URL: " + httpURL);
                    byte response[] = HTMLTools.readPage_GET(httpURL, 10000);
                    String resp = StringTools.toStringValue(response);
                    int maxLen = 60;
                    if (resp.length() <= maxLen) {
                        Print.logInfo("SMS Gateway response: " + resp);
                    } else {
                        String R = resp.substring(0, maxLen);
                        Print.logInfo("SMS Gateway response (partial): " + R + " ...");
                    }
                    return DCServerFactory.ResultCode.SUCCESS;
                } catch (UnsupportedEncodingException uee) {
                    Print.logError("URL Encoding: " + uee);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                } catch (NoRouteToHostException nrthe) {
                    Print.logError("Unreachable Host: " + httpURL);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (UnknownHostException uhe) {
                    Print.logError("Unknown Host: " + httpURL);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (FileNotFoundException fnfe) {
                    Print.logError("Invalid URL (not found): " + httpURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (MalformedURLException mue) {
                    Print.logError("Invalid URL (malformed): " + httpURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (Throwable th) {
                    Print.logError("HTML SMS error: " + th);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                }
            }
        });

                // -----------------------------------------------------------------------------------------------------------
                /* Phone: MET (Mensajeria Empresarial Telcel) [ENTERPRISE] */
        // Properties:          
        //   SmsGatewayHandler.met.url=
        //   SmsGatewayHandler.met.user=
        //   SmsGatewayHandler.met.password=       
        SMSOutboundGateway.AddSMSGateway(GW_met, new SMSOutboundGateway() {

            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                                Print.logInfo("Initializing SMS Gateway. \n");
                            String URL      = this.getStringProperty(device,PROP_met_url,"");
                String user     = this.getStringProperty(device,PROP_met_user,"");
                String password = this.getStringProperty(device,PROP_met_password,"");

                String text     = commandStr;
                String to       = device.getSimPhoneNumber();
                                // ----------------------------- Code: XML-RPC -----------------------------
                                // -------------------------------------------------------------------------
                                String XmlRpcMethod = "Tiaxa.SendMsg";
                String code         = "";
                String text_error   = "";
                try {
                  XmlRpc.setDebug(false);
                  XmlRpcClient client = new XmlRpcClient(URL);
                  client.setBasicAuthentication(user, password);
                  Vector data = new Vector();
                  Hashtable ht1 = new Hashtable();
                  ht1.put("celular", to);
                  ht1.put("texto", text);
                  ht1.put("fecha", "");
                  data.add(ht1);
                  Vector vec = new Vector();
                  vec.add(data);
                  Object res = client.execute("Tiaxa.SendMsg",vec);
                  if (res == null) {
                    code = "101";
                    text_error = "DispatchMT::doDispatch : XmlRpc dispatch. Null response.";
                  }
                  Print.logInfo("(SMS Gateway: MET) Response : " + res + "\n");
                }
                catch (Exception e) {
                  code = "104";
                  text_error = "DispatchMT::doDispatch : XmlRpc dispatch. Exception: " + e.getMessage();
                  Print.logWarn("Exception: " + e.getMessage());
                }
                                // -------------------------------------------------------------------------                            
                                return DCServerFactory.ResultCode.SUCCESS;
                }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                            String URL      = RTConfig.getString(PROP_met_url,"");
                String user     = RTConfig.getString(PROP_met_user,"");
                String password = RTConfig.getString(PROP_met_password,"");
                String text     = "";
                String to       = "";
                                // ----------------------------- Code: XML-RPC -----------------------------
                                // -------------------------------------------------------------------------
                                String XmlRpcMethod = "Tiaxa.SendMsg";
                String code = "";
                String text_error = "";
                try {
                XmlRpc.setDebug(false);
                XmlRpcClient client = new XmlRpcClient(URL);
                client.setBasicAuthentication(user, password);
                Vector data = new Vector();
                Hashtable ht1 = new Hashtable();
                ht1.put("celular", to);
                ht1.put("texto", text);
                ht1.put("fecha", "");
                data.add(ht1);
                Vector vec = new Vector();
                vec.add(data);
                Object res = client.execute("Tiaxa.SendMsg",vec);
                if (res == null) {
                code = "101";
                text_error = "DispatchMT::doDispatch : XmlRpc dispatch. Null response.";
                }
                Print.logInfo("(SMS Gateway: MET) Response : " + res + "\n");
                }
                catch (Exception e) {
                code = "104";
                text_error = "DispatchMT::doDispatch : XmlRpc dispatch. Exception: " + e.getMessage();
                Print.logWarn("Exception: " + e.getMessage());
                }
                                // -------------------------------------------------------------------------                                    
                                return DCServerFactory.ResultCode.SUCCESS;
            }
        });
                // -----------------------------------------------------------------------------------------------------------

        /* EMail: ClickaTell [ENTERPRISE] */
        // Properties:
        //   SmsGatewayHandler.clickatell.smsEmailAddress=
        //   SmsGatewayHandler.clickatell.user=
        //   SmsGatewayHandler.clickatell.password=
        //   SmsGatewayHandler.clickatell.api_id=
        SMSOutboundGateway.AddSMSGateway(GW_clickatell, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                String frEmail  = this.getFromEmailAddress(device);
              //String toEmail  = this.getSmsEmailAddress(device);
                String toEmail  = this.getStringProperty(device,PROP_clickatell_smsEmailAddress,"");
                String user     = this.getStringProperty(device,PROP_clickatell_user           ,"");
                String password = this.getStringProperty(device,PROP_clickatell_password       ,"");
                String api_id   = this.getStringProperty(device,PROP_clickatell_api_id         ,"");
                String text     = commandStr;
                String to       = device.getSimPhoneNumber();
                String from     = "";
                StringBuffer sb = new StringBuffer();
                sb.append("user:"     + user    ).append("\n");   // user:username
                sb.append("password:" + password).append("\n");   // password:password
                sb.append("api_id:"   + api_id  ).append("\n");   // api_id:api_id
                sb.append("text:"     + text    ).append("\n");   // text:text
                sb.append("to:"       + to      ).append("\n");   // to:to
                return this.sendEmail(frEmail, toEmail, ""/*subject*/, sb.toString());
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                String frEmail  = this.getFromEmailAddress(account);
              //String toEmail  = this.getSmsEmailAddress(device);
                String toEmail  = RTConfig.getString(PROP_clickatell_smsEmailAddress,"");
                String user     = RTConfig.getString(PROP_clickatell_user           ,"");
                String password = RTConfig.getString(PROP_clickatell_password       ,"");
                String api_id   = RTConfig.getString(PROP_clickatell_api_id         ,"");
                String text     = smsMessage;
                String to       = smsPhone;
                String from     = "";
                StringBuffer sb = new StringBuffer();
                sb.append("user:"     + user    ).append("\n");   // user:username
                sb.append("password:" + password).append("\n");   // password:password
                sb.append("api_id:"   + api_id  ).append("\n");   // api_id:api_id
                sb.append("text:"     + text    ).append("\n");   // text:text
                sb.append("to:"       + to      ).append("\n");   // to:to
                return this.sendEmail(frEmail, toEmail, ""/*subject*/, sb.toString());
            }
        });
        /* EMail: TextAnywhere [ENTERPRISE] */
        // Properties:
        //   SmsGatewayHandler.mail2txt.smsEmailAddress=@mail2txt.net
        SMSOutboundGateway.AddSMSGateway(GW_mail2txt, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                String smsPhone = this.getSmsPhoneNumber(device);
                String smsEmail = this.getStringProperty(device,PROP_mail2txt_smsEmailAddress,"@mail2txt.net");
                String frEmail  = this.getFromEmailAddress(device);
                String toEmail  = this.getSmsEmailAddress(device);
                if (StringTools.isBlank(toEmail)) {
                    toEmail = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                } else
                if (!toEmail.endsWith(smsEmail)) {
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                return this.sendEmail(frEmail, toEmail, commandStr, "$$"/*body*/);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                String smsEmail = RTConfig.getString(PROP_mail2txt_smsEmailAddress,"@mail2txt.net");
                String frEmail  = this.getFromEmailAddress(account);
                String toEmail  = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                if (!toEmail.endsWith(smsEmail)) {
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                return this.sendEmail(frEmail, toEmail, smsMessage, "$$"/*body*/);
            }
        });
        // Properties:
        //   SmsGatewayHandler.mail2txt160.smsEmailAddress=@mail2txt160.net
        SMSOutboundGateway.AddSMSGateway(GW_mail2txt160, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                String smsPhone = this.getSmsPhoneNumber(device);
                String smsEmail = this.getStringProperty(device,PROP_mail2txt160_smsEmailAddress,"@mail2txt160.net");
                String frEmail  = this.getFromEmailAddress(device);
                String toEmail  = this.getSmsEmailAddress(device);
                if (StringTools.isBlank(toEmail)) {
                    toEmail = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                } else
                if (!toEmail.endsWith(smsEmail)) {
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                return this.sendEmail(frEmail, toEmail, commandStr, "$$"/*body*/);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                String smsEmail = RTConfig.getString(PROP_mail2txt160_smsEmailAddress,"@mail2txt160.net");
                String frEmail  = this.getFromEmailAddress(account);
                String toEmail  = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                if (!toEmail.endsWith(smsEmail)) {
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                return this.sendEmail(frEmail, toEmail, smsMessage, "$$"/*body*/);
            }
        });
        // Properties:
        //   SmsGatewayHandler.mail2txtid.smsEmailAddress=@mail2txtid.net
        //   SmsGatewayHandler.mail2txtid.from=
        SMSOutboundGateway.AddSMSGateway(GW_mail2txtid, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                String smsPhone = this.getSmsPhoneNumber(device);
                String smsEmail = this.getStringProperty(device,PROP_mail2txtid_smsEmailAddress,"@mail2txtid.net");
                String fromNum  = this.getStringProperty(device,PROP_mail2txtid_from           ,"");
                String frEmail  = this.getFromEmailAddress(device);
                String toEmail  = this.getSmsEmailAddress(device);
                if (StringTools.isBlank(toEmail)) {
                    toEmail = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                } else
                if (!toEmail.endsWith(smsEmail)) {
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                String message  = commandStr + "$$";
                return this.sendEmail(frEmail, toEmail, fromNum, message);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                String smsEmail = RTConfig.getString(PROP_mail2txtid_smsEmailAddress,"@mail2txtid.net");
                String fromNum  = RTConfig.getString(PROP_mail2txtid_from           ,"");
                String frEmail  = this.getFromEmailAddress(account);
                String toEmail  = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                if (!toEmail.endsWith(smsEmail)) {
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                String message  = smsMessage + "$$";
                return this.sendEmail(frEmail, toEmail, fromNum, message);
            }
        });
        // Properties:
        //   SmsGatewayHandler.mail2txt160id.smsEmailAddress=@mail2txt160id.net
        //   SmsGatewayHandler.mail2txt160id.from=
        SMSOutboundGateway.AddSMSGateway(GW_mail2txt160id, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                String smsPhone = this.getSmsPhoneNumber(device);
                String smsEmail = this.getStringProperty(device,PROP_mail2txt160id_smsEmailAddress,"@mail2txt160id.net");
                String fromNum  = this.getStringProperty(device,PROP_mail2txt160id_from           ,"");
                String frEmail  = this.getFromEmailAddress(device);
                String toEmail  = this.getSmsEmailAddress(device);
                if (StringTools.isBlank(toEmail)) {
                    toEmail = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                } else
                if (!toEmail.endsWith(smsEmail)) {
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                String message  = commandStr + "$$";
                return this.sendEmail(frEmail, toEmail, fromNum, message);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                String smsEmail = RTConfig.getString(PROP_mail2txt160id_smsEmailAddress,"@mail2txt160id.net");
                String fromNum  = RTConfig.getString(PROP_mail2txt160id_from           ,"");
                String frEmail  = this.getFromEmailAddress(account);
                String toEmail  = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                if (!toEmail.endsWith(smsEmail)) {
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                String message  = smsMessage + "$$";
                return this.sendEmail(frEmail, toEmail, fromNum, message);
            }
        });
        /* HTTP: Ozeki [ENTERPRISE] */
        // Documentation:
        //   http://www.ozekisms.com/index.php?owpn=159&sms_gateway=sms_Product_Manual
        // Properties:
        //   SmsGatewayHandler.ozekisms.hostPort=
        //   SmsGatewayHandler.ozekisms.originator=
        //   SmsGatewayHandler.ozekisms.user=
        //   SmsGatewayHandler.ozekisms.password=
        SMSOutboundGateway.AddSMSGateway(GW_ozekisms, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                String hostPort   = this.getStringProperty(device,PROP_ozekisms_hostPort,"localhost:9501");
                String originator = this.getStringProperty(device,PROP_ozekisms_originator,"1234567890");
                String recipient  = device.getSimPhoneNumber();
                String message    = commandStr;
                String username   = this.getStringProperty(device,PROP_ozekisms_user    ,"");
                String password   = this.getStringProperty(device,PROP_ozekisms_password,"");
                StringBuffer smsURL = new StringBuffer();
                try {
                    smsURL.append("http://").append(hostPort).append("/api?action=sendmessage");
                    smsURL.append("&username=").append(URLEncoder.encode(username,"UTF-8"));
                    smsURL.append("&password=").append(URLEncoder.encode(password,"UTF-8"));
                    smsURL.append("&recipient=").append(URLEncoder.encode(recipient,"UTF-8"));
                    smsURL.append("&messagetype=SMS:TEXT");
                    smsURL.append("&messagedata=").append(URLEncoder.encode(message,"UTF-8"));
                    smsURL.append("&originator=").append(URLEncoder.encode(originator,"UTF-8"));
                    smsURL.append("&serviceprovider=GSMModem1");
                    smsURL.append("&responseformat=html");
                    byte response[] = HTMLTools.readPage_GET(smsURL.toString(), 10000);
                    // TODO: authorized?
                    return DCServerFactory.ResultCode.SUCCESS;
                } catch (UnsupportedEncodingException uee) {
                    Print.logError("URL Encoding: " + uee);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                } catch (NoRouteToHostException nrthe) {
                    Print.logError("Unreachable Host: " + hostPort);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (UnknownHostException uhe) {
                    Print.logError("Unknown Host: " + hostPort);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (FileNotFoundException fnfe) {
                    Print.logError("Invalid URL (not found): " + smsURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (MalformedURLException mue) {
                    Print.logError("Invalid URL (malformed): " + smsURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (Throwable th) {
                    Print.logError("HTML SMS error: " + th);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                }
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                String hostPort   = RTConfig.getString(PROP_ozekisms_hostPort,"localhost:9501");
                String originator = RTConfig.getString(PROP_ozekisms_originator,"1234567890");
                String recipient  = smsPhone;
                String message    = smsMessage;
                String username   = RTConfig.getString(PROP_ozekisms_user    ,"");
                String password   = RTConfig.getString(PROP_ozekisms_password,"");
                StringBuffer smsURL = new StringBuffer();
                try {
                    smsURL.append("http://").append(hostPort).append("/api?action=sendmessage");
                    smsURL.append("&username=").append(URLEncoder.encode(username,"UTF-8"));
                    smsURL.append("&password=").append(URLEncoder.encode(password,"UTF-8"));
                    smsURL.append("&recipient=").append(URLEncoder.encode(recipient,"UTF-8"));
                    smsURL.append("&messagetype=SMS:TEXT");
                    smsURL.append("&messagedata=").append(URLEncoder.encode(message,"UTF-8"));
                    smsURL.append("&originator=").append(URLEncoder.encode(originator,"UTF-8"));
                    smsURL.append("&serviceprovider=GSMModem1");
                    smsURL.append("&responseformat=html");
                    byte response[] = HTMLTools.readPage_GET(smsURL.toString(), 10000);
                    // TODO: authorized?
                    return DCServerFactory.ResultCode.SUCCESS;
                } catch (UnsupportedEncodingException uee) {
                    Print.logError("URL Encoding: " + uee);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                } catch (NoRouteToHostException nrthe) {
                    Print.logError("Unreachable Host: " + hostPort);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (UnknownHostException uhe) {
                    Print.logError("Unknown Host: " + hostPort);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (FileNotFoundException fnfe) {
                    Print.logError("Invalid URL (not found): " + smsURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (MalformedURLException mue) {
                    Print.logError("Invalid URL (malformed): " + smsURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (Throwable th) {
                    Print.logError("HTML SMS error: " + th);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                }
            }
        });
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public SMSOutboundGateway()
    {
        // override
    }

    // ------------------------------------------------------------------------

    public abstract DCServerFactory.ResultCode sendSMSCommand(Device device, String command);
    public abstract DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone);

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected String getFromEmailAddress(Device device) 
    {
        if (device == null) { return null; }
        return CommandPacketHandler.getFromEmailCommand(device.getAccount());
    }

    protected String getFromEmailAddress(Account account) 
    {
        if (account == null) { return null; }
        return CommandPacketHandler.getFromEmailCommand(account);
    }
    
    // ------------------------------------------------------------------------

    protected String getSmsEmailAddress(Device device) 
    {
        if (device == null) { return null; }
        String toEmail = device.getSmsEmail();
        return toEmail;
    }

    protected String getSmsPhoneNumber(Device device) 
    {
        if (device == null) { return null; }
        String smsPhone = device.getSimPhoneNumber();
        return smsPhone;
    }

    // ------------------------------------------------------------------------

    protected String getStringProperty(Device device, String key, String dft) 
    {

        /* 1) check the BasicPrivateLabel properties */
        Account account = (device != null)? device.getAccount() : null;
        BasicPrivateLabel bpl = Account.getPrivateLabel(account);
        if (bpl != null) {
            String prop = bpl.getStringProperty(key,null);
            if (prop != null) {
                return prop;
            }
        }

        /* 2) check the DCServerConfig properties */
        DCServerConfig dcs = (device != null)? DCServerFactory.getServerConfig(device.getDeviceCode()) : null;
        if (dcs != null) {
            String prop = dcs.getStringProperty(key, dft); // will already check RTConfig properties
            Print.logInfo("DCServerConfig property '"+key+"' ==> " + prop);
            if (StringTools.isBlank(prop) && RTConfig.hasProperty(key)) {
                Print.logInfo("(RTConfig property '"+key+"' ==> " + RTConfig.getString(key,"") + ")");
            }
            return prop;
        }

        /* 3) check RTConfig properties */
        String prop = RTConfig.getString(key, dft);
        Print.logInfo("RTConfig property '"+key+"' ==> " + prop);
        return prop;

    }

    // ------------------------------------------------------------------------

    protected DCServerFactory.ResultCode sendEmail(String frEmail, String toEmail, String subj, String body) 
    {
        if (StringTools.isBlank(frEmail)) {
            Print.logError("'From' SMS Email address not specified");
            return DCServerFactory.ResultCode.INVALID_EMAIL_FR;
        } else
        if (StringTools.isBlank(toEmail) || !CommandPacketHandler.validateAddress(toEmail)) {
            Print.logError("'To' SMS Email address invalid, or not specified");
            return DCServerFactory.ResultCode.INVALID_EMAIL_TO;
        } else
        if (StringTools.isBlank(subj) && StringTools.isBlank(body)) {
            Print.logError("SMS Subject/Body string not specified");
            return DCServerFactory.ResultCode.INVALID_ARG;
        } else {
            try {
                Print.logInfo ("SMS email: to <" + toEmail + ">");
                Print.logDebug("  From   : " + frEmail);
                Print.logDebug("  To     : " + toEmail);
                Print.logDebug("  Subject: " + subj);
                Print.logDebug("  Message: " + body);
                SendMail.SmtpProperties smtpProps = null; // TODO:
                SendMail.send(frEmail,toEmail,null,null,subj,body,null,smtpProps);
                return DCServerFactory.ResultCode.SUCCESS;
            } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                // this will fail if JavaMail support for SendMail is not available.
                Print.logWarn("SendMail error: " + t);
                return DCServerFactory.ResultCode.TRANSMIT_FAIL;
            }
        }
    }

    // ------------------------------------------------------------------------

}
