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
package org.opengts.extra.service;

import java.util.*;
import java.io.*;
import java.net.*;

import java.lang.management.*; 
import javax.management.*; 
import javax.management.remote.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

public class GTSServiceRequest
    extends ServiceRequest
    implements ServiceXML
{

    // ------------------------------------------------------------------------

    /** 
    *** Constructor
    *** @param url  The service URL
    **/
    public GTSServiceRequest(String url) 
        throws MalformedURLException 
    {
        super(url);
        this.setJMXObjectName(JMXServiceObjectName);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a Document response for requrest that only require a command name
    *** @param command  The command name
    *** @return The XML Document response
    **/
    public Document getCommandOnly_Document(final String command)
        throws IOException 
    {
        return this.sendRequest(command, new ServiceRequest.RequestBody() {
            public StringBuffer appendRequestBody(StringBuffer sb, int indent) {
                return sb;
            }
        });
    }

    /**
    *** Appends a formatted "TimeFrom"/"TimeTo" tag element to the specified StringBuffer
    *** @param sb    The StringBuffer
    *** @param pfx   The prefixing spaces to prepend to the formatted tag elemeent
    *** @param dt    The date/time to format
    *** @param isTimeTo  True to create tag "TimeTo", false for "TimeFrom"
    *** @return The StringBuffer
    **/
    protected StringBuffer formatTime(StringBuffer sb, String pfx, DateTime dt, boolean isTimeTo)
    {
        if (dt != null) {
            String tag = isTimeTo? TAG_TimeTo : TAG_TimeFrom;
            sb.append(pfx);
            sb.append("<"+tag);
            sb.append(" "+ATTR_timezone+"=\"" + dt.getTimeZoneShortName() + "\"");
            sb.append(">");
            sb.append(dt.format("yyyy/MM/dd,HH:mm:ss"));
            sb.append("</"+tag+">\n");
        }
        return sb;
    }

    /**
    *** Reads the Document response and returns a list of the specified subnodes
    *** @param respDoc  The response XML Document
    *** @param tag      The tag for which a NodeList is returned
    *** @return The NodeList, or null if an error occurred
    **/
    protected NodeList getResponseNodeList(Document respDoc, String tag)
    {
        
        /* invalid Document/tag */
        if ((respDoc == null) || StringTools.isBlank(tag)) {
            return null;
        }
        
        /* check for GTSResponse */
        Element gtsResponse = respDoc.getDocumentElement();
        if (!gtsResponse.getTagName().equalsIgnoreCase(TAG_GTSResponse)) {
            return null;
        }

        /* check for "success" */
        String  result = XMLTools.getAttribute(gtsResponse, ATTR_result, "", false);
        if (result.equalsIgnoreCase("success")) {
            return null;
        }

        /* return node list */
        NodeList nodeList = XMLTools.getChildElements(gtsResponse,tag);
        return (nodeList.getLength() > 0)? nodeList : null;
        
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public Document getVersion_Document()
        throws IOException 
    {
        return this.getCommandOnly_Document(CMD_version);
    }

    public String getVersion()
        throws IOException 
    {
        NodeList versList = this.getResponseNodeList(this.getVersion_Document(), TAG_Version);
        if (versList != null) {
            Element versElem = (Element)versList.item(0);
            String version = XMLTools.getNodeText(versElem," ",false);
            return version;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    public Document getProperty_Document(final String propKey)
        throws IOException 
    {
        return this.sendRequest(CMD_propget, new ServiceRequest.RequestBody() {
            public StringBuffer appendRequestBody(StringBuffer sb, int indent) {
                String pfx1 = StringTools.replicateString(" ",indent);
                sb.append(pfx1).append("<"+TAG_Property+" "+ATTR_key+"=\""+propKey+"\"/>\n");
                return sb;
            }
        });
    }

    public String getProperty(String propKey)
        throws IOException 
    {
        NodeList propList = this.getResponseNodeList(this.getProperty_Document(propKey), TAG_Property);
        if (propList != null) {
            Element propElem = (Element)propList.item(0);
            String propVal = XMLTools.getNodeText(propElem,"\n",false);
            return propVal;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    public Document getTableSchema_Document(final String tableName)
        throws IOException 
    {
        return this.sendRequest(CMD_dbschema, new ServiceRequest.RequestBody() {
            public StringBuffer appendRequestBody(StringBuffer sb, int indent) {
                String pfx1 = StringTools.replicateString(" ",indent);
                sb.append(pfx1).append("<"+TAG_TableSchema+" "+ATTR_table+"=\""+tableName+"\"/>\n");
                return sb;
            }
        });
    }

    // ------------------------------------------------------------------------

    public Document getMapDataDevice_Document(
        final String deviceID, 
        final DateTime frDT, final DateTime toDT) 
        throws IOException 
    {
        return this.sendRequest(CMD_mapdata, new ServiceRequest.RequestBody() {
            public StringBuffer appendRequestBody(StringBuffer sb, int indent) {
                String pfx1 = StringTools.replicateString(" ",indent);
                String pfx2 = StringTools.replicateString(" ",indent*2);
                sb.append(pfx1).append("<"+TAG_MapData+">\n");
                sb.append(pfx2).append("<"+TAG_Device+">"+deviceID+"</"+TAG_Device+">\n");
                GTSServiceRequest.this.formatTime(sb, pfx2, frDT, false);
                GTSServiceRequest.this.formatTime(sb, pfx2, toDT, true);
                sb.append(pfx1).append("</"+TAG_MapData+">\n");
                return sb;
            }
        });
    }

    public Document getMapDataGroup_Document(
        final String groupID, 
        final DateTime frDT, final DateTime toDT) 
        throws IOException 
    {
        return this.sendRequest(CMD_mapdata, new ServiceRequest.RequestBody() {
            public StringBuffer appendRequestBody(StringBuffer sb, int indent) {
                String pfx1 = StringTools.replicateString(" ",indent);
                String pfx2 = StringTools.replicateString(" ",indent*2);
                sb.append(pfx1).append("<"+TAG_MapData+">\n");
                sb.append(pfx2).append("<"+TAG_DeviceGroup+">"+groupID+"</"+TAG_DeviceGroup+">\n");
                GTSServiceRequest.this.formatTime(sb, pfx2, frDT, false);
                GTSServiceRequest.this.formatTime(sb, pfx2, toDT, true);
                sb.append(pfx1).append("</"+TAG_MapData+">\n");
                return sb;
            }
        });
    }

    // ------------------------------------------------------------------------

    public Document getReportDevice_Document(
        final String reportName,
        final String deviceID, 
        final DateTime frDT, final DateTime toDT) 
        throws IOException 
    {
        return this.sendRequest(CMD_report, new ServiceRequest.RequestBody() {
            public StringBuffer appendRequestBody(StringBuffer sb, int indent) {
                String pfx1 = StringTools.replicateString(" ",indent);
                String pfx2 = StringTools.replicateString(" ",indent*2);
                sb.append(pfx1).append("<"+TAG_Report+" "+ATTR_name+"=\""+reportName+"\">\n");
                sb.append(pfx2).append("<"+TAG_Device+">"+deviceID+"</"+TAG_Device+">\n");
                GTSServiceRequest.this.formatTime(sb, pfx2, frDT, false);
                GTSServiceRequest.this.formatTime(sb, pfx2, toDT, true);
                sb.append(pfx1).append("</"+TAG_Report+">\n");
                return sb;
            }
        });
    }
    
    // ------------------------------------------------------------------------

    public Document getReportGroup_Document(
        final String reportName,
        final String groupID, 
        final DateTime frDT, final DateTime toDT) 
        throws IOException 
    {
        return this.sendRequest(CMD_mapdata, new ServiceRequest.RequestBody() {
            public StringBuffer appendRequestBody(StringBuffer sb, int indent) {
                String pfx1 = StringTools.replicateString("",indent);
                String pfx2 = StringTools.replicateString("",indent*2);
                sb.append(pfx1).append("<"+TAG_Report+" "+ATTR_name+"=\""+reportName+"\">\n");
                sb.append(pfx2).append("<"+TAG_DeviceGroup+">"+groupID+"</"+TAG_DeviceGroup+">\n");
                GTSServiceRequest.this.formatTime(sb, pfx2, frDT, false);
                GTSServiceRequest.this.formatTime(sb, pfx2, toDT, true);
                sb.append(pfx1).append("</"+TAG_Report+">\n");
                return sb;
            }
        });
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected void printDBRecordInfo(DBRecord dbr)
    {
        Print.sysPrintln("\n");
        Print.sysPrintln("Retrieved Table: [" + StringTools.className(dbr) + "] " + dbr.getRecordKey());
        Print.sysPrintln("  Description  : " + dbr.getDescription());
        Print.sysPrintln("\n");
    }

    // ------------------------------------------------------------------------

    /**
    *** Get a specific virtual Account
    **/
    public Account getAccount(String acctID) 
        throws DBException
    {
        Account.Key rcdKey = new Account.Key(acctID);
        DBRecord dbr = rcdKey.getVirtualDBRecord(this);
        if (RTConfig.isDebugMode()) {
            this.printDBRecordInfo(dbr);
        }
        return (Account)dbr;
    }

    /**
    *** Gets a list of Accounts
    **/
    public String[] getAccountIDs() 
        throws DBException
    {
        final boolean isSoapReq = false;
        String  CMD_dbget       = DBFactory.CMD_dbget;
        String  TAG_Response    = this.getTagResponse();
        String  TAG_RecordKey   = DBFactory.TAG_RecordKey;
        String  ATTR_command    = this.getAttrCommand();
        String  ATTR_result     = this.getAttrResult();

        /* send request / get response */
        Document xmlDoc = null;
        try {
            xmlDoc = this.sendRequest(CMD_dbget, new ServiceRequest.RequestBody() {
                public StringBuffer appendRequestBody(StringBuffer sb, int indent) {
                    String tableName = Account.TABLE_NAME();
                    String PFX1 = XMLTools.PREFIX(isSoapReq,indent);
                    sb.append(PFX1);
                    sb.append(XMLTools.startTAG(isSoapReq,DBFactory.TAG_RecordKey,
                        XMLTools.ATTR(DBFactory.ATTR_table,tableName) +
                        XMLTools.ATTR(DBFactory.ATTR_partial,"all"),
                        false,true));
                    sb.append(PFX1);
                    sb.append(XMLTools.endTAG(isSoapReq, DBFactory.TAG_RecordKey, true));
                    return sb;
                }
            });
        } catch (IOException ioe) {
            Print.logException("Error", ioe);
            throw new DBException("Request read error", ioe);
        }

        /* parse 'GTSResponse' */
        Element gtsResponse = xmlDoc.getDocumentElement();
        if (!gtsResponse.getTagName().equalsIgnoreCase(TAG_Response)) {
            Print.logError("Request XML does not start with '%s'", TAG_Response);
            throw new DBException("Response XML does not begin with '"+TAG_Response+"'");
        }
        String result = XMLTools.getAttribute(gtsResponse, ATTR_result, "error", false);
        if (!result.equals("success")) {
            Print.logError("GTSResponse error");
            throw new DBException("GTSResponse error");
        }

        /* RecordKey(s) */
        NodeList keyList = XMLTools.getChildElements(gtsResponse,TAG_RecordKey);
        if (keyList.getLength() <= 0) {
            Print.logError("No 'RecordKey' tags");
            throw new DBException("GTSResponse does not contain any 'RecordKey' tags");
        }

        /* extract Account IDs */
        String acctIDs[] = new String[keyList.getLength()];
        for (int k = 0; k < keyList.getLength(); k++) {
            Element keyElem = (Element)keyList.item(k);
            Account.Key acctKey = (Account.Key)DBFactory.parseXML_DBRecordKey(keyElem);
            acctIDs[k] = (String)acctKey.getFieldValue(Account.FLD_accountID);
        }
        return acctIDs;

    }

    // ------------------------------------------------------------------------

    public User getUser(String acctID, String userID) 
        throws DBException
    {
        User.Key rcdKey = new User.Key(acctID, userID);
        DBRecord dbr = rcdKey.getVirtualDBRecord(this);
        this.printDBRecordInfo(dbr);
        return (User)dbr;
    }

    // ------------------------------------------------------------------------

    public Device getDevice(String acctID, String devID) 
        throws DBException
    {
        Device.Key rcdKey = new Device.Key(acctID, devID);
        DBRecord dbr = rcdKey.getVirtualDBRecord(this);
        this.printDBRecordInfo(dbr);
        return (Device)dbr;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public String getTagResponse()
    {
        return TAG_GTSResponse;
    }

    public String getTagRequest()
    {
        return TAG_GTSRequest;
    }
    
    // ------------------------------------------------------------------------

}
