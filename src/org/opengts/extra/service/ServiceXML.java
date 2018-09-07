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
// Response:
// ---------
//
// Example Table Schema Response:
// <GTSResponse command="dbschema" result="success">
//    <TableSchema table="Device">
//       <Description><![CDATA[This table defines Device/Vehicle specific information for an Account.]]></Description>
//       <Field name="accountID" primaryKey="true" type="STRING[32]"/>
//       <Field name="deviceID" primaryKey="true" type="STRING[32]"/>
//       ...
//    </TableSchema>
// </GTSResponse>
//
// Example Report Response
// <GTSResponse command="report" result="success">
//    <Report name="EventDetail">
//       <Device>mydevice</Device>
//       <DeviceGroup>mydevice</DeviceGroup>
//       <TimeFrom timezone="GMT">2009/03/13,00:00:00</TimeFrom>
//       <TimeTo timezone="GMT">2009/03/13,23:59:59</TimeTo>
//       <Title>Report Title</Title>
//       <Subtitle>Subtitle</Subtitle>
//       <HeaderRow>
//          <HeaderColumn name="abc">title</Column>
//          ...
//       </HeaderRow>
//       <BodyRow>
//          <BodyColumn name="abc">value</Column>
//          ...
//       </BodyRow>
//    </Report>
// </GTSResponse>
//
// ----------------------------------------------------------------------------
//
// Error Message Response:
// <GTSResponse result="error">
//    <Message code="code"><![CDATA[ Error Message ]]></Error>
// </GTSResponse>
//
// Success Message Response:
// <GTSResponse result="success">
//    <Message code="code"><![CDATA[ Success Message ]]></Error>
// </GTSResponse>
//
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.extra.service;

import java.util.*;
import java.io.*;
import java.net.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;
import org.opengts.dbtools.DBFactory;

public interface ServiceXML
{
    
    public static final String  JMXServiceObjectName        = "org.opengts.extra.war.service:type=ServiceProxy";

    // ------------------------------------------------------------------------

    public static final String  CMD_version                 = "version";
    // <!-- get version -->
    // <GTSRequest command="version">
    //    <Authorization account="account" user="user" password="password"/>
    // </GTSRequest>
    //
    // <!-- response -->
    // <GTSResponse command="version" result="success">
    //    <Version><![CDATA[E2.1.3-B11]]></Version>
    // </GTSResponse>

    public static final String  CMD_dbget                   = DBFactory.CMD_dbget;
    // <!-- returns list of record keys -->
    // <GTSRequest command="dbget">
    //    <Authorization account="account" user="user" password="password"/>
    //    <RecordKey table="Device" partial="true">
    //       <Field name="accountID">demo</Field>
    //    </RecordKey>
    // </GTSRequest>
    //
    // <!-- return record keys with requested fields -->
    // <GTSRequest command="dbget">
    //    <Authorization account="account" user="user" password="password"/>
    //    <Record table="Device" partial="true">
    //       <Field name="accountID">demo</Field>
    //       <Field name="description"/>
    //    </RecordKey>
    // </GTSRequest>
   
    public static final String  CMD_dbcreate                = DBFactory.CMD_dbcreate;
    // TODO: not yet fully tested
    // <!-- create new record -->
    // <GTSRequest command="dbcreate">
    //    <Authorization account="account" user="user" password="password"/>
    //    <Record table="Device" partial="true">
    //       <Field name="accountID">demo</Field>
    //       <Field name="deviceID">demo2</Field>
    //       <Field name="description">Demo2 Test Device</Field>
    //       ...
    //    </RecordKey>
    // </GTSRequest>

    public static final String  CMD_dbput                   = DBFactory.CMD_dbput;
    // <!-- update existing record -->
    // <GTSRequest command="dbput">
    //    <Authorization account="account" user="user" password="password"/>
    //    <Record table="Device" partial="true">
    //       <Field name="accountID">demo</Field>
    //       <Field name="deviceID">demo2</Field>
    //       <Field name="description">Demo2 Test Device</Field>
    //    </RecordKey>
    // </GTSRequest>

    public static final String  CMD_dbdel                   = DBFactory.CMD_dbdel;
    // <!-- delete existing record -->
    // <GTSRequest command="dbdel">
    //    <Authorization account="account" user="user" password="password"/>
    //    <RecordKey table="Device">
    //       <Field name="accountID">demo</Field>
    //       <Field name="deviceID">demo2</Field>
    //    </RecordKey>
    // </GTSRequest>

    public static final String  CMD_dbschema                = DBFactory.CMD_dbschema;
    // <!-- returns schema for specified table -->
    // <GTSRequest command="dbschema">
    //    <Authorization account="account" user="user" password="password"/>
    //    <TableSchema table="Device"/>
    // </GTSRequest>

    public static final String  CMD_propget                 = "propget";
    // <!-- get property value ('sysadmin' only) -->
    // <GTSRequest command="propget">
    //    <Authorization account="account" user="user" password="password"/>
    //    <Property key="somekey"/>
    // </GTSRequest>

    public static final String  CMD_reportlist              = "reportlist";
    // <!-- get list of available reports -->
    // <GTSRequest command="reportlist">
    //    <Authorization account="account" user="user" password="password"/>
    // </GTSRequest>

    public static final String  CMD_report                  = "report";
    // <!-- get 'EventDetail' report -->
    // <GTSRequest command="report">
    //    <Authorization account="account" user="user" password="password"/>
    //    <Report name="EventDetail">
    //       <Device>mydevice</Device>
    //       <TimeFrom timezone="US/Pacific">2009/03/13,00:00:01</TimeFrom>
    //       <TimeTo timezone="US/Pacific">2009/03/13,00:00:01</TimeTo>
    //    </Report>
    // </GTSRequest>
    //
    // <!-- get 'EventSummary' report -->
    // <GTSRequest command="report">
    //    <Authorization account="account" user="user" password="password"/>
    //    <Report name="EventSummary">
    //       <DeviceGroup>mydevice</DeviceGroup>
    //       <TimeFrom>2009/03/13,00:00:01</TimeFrom>
    //       <TimeTo>2009/03/13,00:00:01</TimeTo>
    //    </Report>
    // </GTSRequest>

    public static final String  CMD_mapdata                 = "mapdata";
    // <!-- get map-data for device -->
    // <GTSRequest command="mapdata">
    //    <Authorization account="account" user="user" password="password"/>
    //    <MapData>
    //       <Device>mydevice</Device>
    //       <TimeFrom>2009/03/13,00:00:01</TimeFrom>
    //       <TimeTo>2009/03/13,00:00:01</TimeTo>
    //    </MapData>
    // </GTSRequest>
    //
    // <!-- get map-data for device -->
    // <GTSRequest command="mapdata">
    //    <Authorization account="account" user="user" password="password"/>
    //    <MapData>
    //       <DeviceGroup>mydevice</DeviceGroup>
    //       <TimeFrom>2009/03/13,00:00:01</TimeFrom>
    //       <TimeTo>2009/03/13,00:00:01</TimeTo>
    //       <Limit type="last">1000</Limit>
    //    </MapData>
    // </GTSRequest>

    public static final String  CMD_eventdata               = "eventdata";
    // <!-- get EventData records -->
    // <GTSRequest command="eventdata">
    //    <Authorization account="account" user="user" password="password"/>
    //    <EventData>
    //       <Device>mydevice</Device>
    //       <TimeFrom>2009/03/13,00:00:01</TimeFrom>
    //       <TimeTo>2009/03/13,00:00:01</TimeTo>
    //       <GPSRequired>false</GPSRequired>
    //       <StatusCode>false</StatusCode>
    //       <Limit type="last">1000</Limit>
    //       <Ascending>true</Ascending>
    //       <Field name="latitude"/>
    //       <Field name="longitude"/>
    //    </EventData>
    // </GTSRequest>
    // <GTSRequest command="eventdata">
    //    <Authorization account="account" user="user" password="password"/>
    //    <EventData>
    //      <AutoIndex>12345</AutoIndex>
    //    </EventData>
    // </GTSRequest>

    public static final String  CMD_pushpins                = "pushpins";
    // <!-- return list of available pushpins -->
    // <GTSRequest command="pushpins">
    //    <Authorization account="account" user="user" password="password"/>
    // </GTSRequest>
    
    public static final String  CMD_messages                = "messages";
    // <!-- return list of response messages -->
    // <GTSRequest command="messages">
    //    <Authorization account="account" user="user" password="password"/>
    // </GTSRequest>
    
    public static final String  CMD_commands                = "commands";
    // <!-- return list of available commands -->
    // <GTSRequest command="commands">
    //    <Authorization account="account" user="user" password="password"/>
    // </GTSRequest>
   
    public static final String  CMD_statuscodes             = "statuscodes";
    // TODO: not yet implemented
    // <!-- return list of statusCodes -->
    // <GTSRequest command="statusCodes">
    //    <Authorization account="account" user="user" password="password"/>
    // </GTSRequest>

    public static final String  CMD_devcmd                  = "devcmd";
    // <!-- send command to device -->
    // <GTSRequest command="devcmd">
    //    <Authorization account="account" user="user" password="password"/>
    //    <DeviceCommand type="config" name="EngineEnable">
    //       <Device>mydevice</Device>
    //    </DeviceCommand>
    // </GTSRequest>

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String TAG_GTSRequest               = "GTSRequest";     // ServiceRequest.TAG_GTSRequest
    public static final String TAG_GTSResponse              = "GTSResponse";    // ServiceRequest.TAG_GTSResponse
    public static final String TAG_Authorization            = "Authorization";  // ServiceRequest.TAG_Authorization
    public static final String TAG_Message                  = "Message";
    public static final String TAG_Comment                  = "Comment";
    public static final String TAG_Property                 = "Property";
    public static final String TAG_TableSchema              = "TableSchema";    // DBFactory.TAG_TableSchema;

    public static final String TAG_Version                  = "Version";
    
    public static final String TAG_RecordKey                = "RecordKey";      // DBFactory.TAG_RecordKey;
    public static final String TAG_Record                   = "Record";         // DBFactory.TAG_Record;
    public static final String TAG_Field                    = "Field";          // DBFactory.TAG_Field;
    
    public static final String TAG_ReportList               = "ReportList";
    public static final String TAG_ReportItem               = "ReportItem";
    public static final String TAG_Title                    = "Title";
    public static final String TAG_Subtitle                 = "Subtitle";
    public static final String TAG_MenuDescription          = "MenuDescription";
    public static final String TAG_Columns                  = "Columns";
    public static final String TAG_Column                   = "Column";

    public static final String TAG_Report                   = "Report";
    public static final String TAG_Device                   = "Device";
    public static final String TAG_DeviceGroup              = "DeviceGroup";
    public static final String TAG_TimeFrom                 = "TimeFrom";
    public static final String TAG_TimeTo                   = "TimeTo";
    public static final String TAG_Where                    = "Where";
    public static final String TAG_CreationFromMS           = "CreationFromMS";
    public static final String TAG_CreationToMS             = "CreationToMS";
    public static final String TAG_EmailAddress             = "EmailAddress";
  //public static final String TAG_URL                      = "URL";
  //public static final String TAG_HTML                     = "HTML";

    public static final String TAG_MapData                  = "MapData";
    public static final String TAG_EventData                = "EventData";
    public static final String TAG_AutoIndex                = "AutoIndex";
    public static final String TAG_Limit                    = "Limit";
    public static final String TAG_Ascending                = "Ascending";
    public static final String TAG_GPSRequired              = "GPSRequired";

    public static final String TAG_Pushpins                 = "Pushpins";
    public static final String TAG_Pushpin                  = "Pushpin";

    public static final String TAG_Messages                 = "Messages";

    public static final String TAG_Commands                 = "Commands";
    public static final String TAG_Command                  = "Command";
    
    public static final String TAG_DeviceCommand            = "DeviceCommand";
    
    public static final String TAG_StatusCode               = "StatusCode";

    // ----------------

    public static final String ATTR_account                 = "account";        // ServiceRequest.ATTR_account
    public static final String ATTR_user                    = "user";           // ServiceRequest.ATTR_user
    public static final String ATTR_password                = "password";       // ServiceRequest.ATTR_password
    public static final String ATTR_name                    = "name";
    public static final String ATTR_option                  = "option";
    public static final String ATTR_code                    = "code";
    public static final String ATTR_result                  = "result";         // ServiceRequest.ATTR_result
    public static final String ATTR_key                     = "key";
    public static final String ATTR_command                 = "command"; 
    public static final String ATTR_parameter               = "parameter";      // ServiceRequest.ATTR_parameter
    public static final String ATTR_table                   = "table";          // DBFactory.ATTR_table
    public static final String ATTR_partial                 = "partial";        // DBFactory.ATTR_partial
    public static final String ATTR_locale                  = "locale";
    public static final String ATTR_index                   = "index";
    public static final String ATTR_eval                    = "eval";
    public static final String ATTR_url                     = "url";
    public static final String ATTR_size                    = "size";
    public static final String ATTR_offset                  = "offset";
    public static final String ATTR_shadowUrl               = "shadowUrl";
    public static final String ATTR_shadowSize              = "shadowSize";
    public static final String ATTR_timezone                = "timezone";
    public static final String ATTR_type                    = "type";
    public static final String ATTR_urlOnly                 = "urlOnly";
    public static final String ATTR_format                  = "format";

    /* arguments */
    public static final String ATTR_arg                     = "arg";
    public static final String ATTR_arg0                    = "arg0";
    public static final String ATTR_arg1                    = "arg1";
    public static final String ATTR_arg2                    = "arg2";
    public static final String ATTR_arg3                    = "arg3";
    public static final String ATTR_arg4                    = "arg4";
    public static final String ATTR_arg5                    = "arg5";
    public static final String ATTR_arg6                    = "arg6";
    public static final String ATTR_arg7                    = "arg7";
    public static final String ATTR_args_[]                 = new String[] {
        ATTR_arg0,
        ATTR_arg1,
        ATTR_arg2,
        ATTR_arg3,
        ATTR_arg4,
        ATTR_arg5,
        ATTR_arg6,
        ATTR_arg7,
    };

    // ------------------------------------------------------------------------

}
