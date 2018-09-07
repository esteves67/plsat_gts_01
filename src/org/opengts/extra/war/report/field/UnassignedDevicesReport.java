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
package org.opengts.extra.war.report.field;

import java.io.*;
import java.util.*;
import java.sql.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;
import org.opengts.war.report.field.*;

import org.opengts.extra.tables.UnassignedDevices;

public class UnassignedDevicesReport
    extends ReportData
    implements DBRecordHandler<UnassignedDevices>
{
    
    // ------------------------------------------------------------------------
    
    private static Comparator<FieldData> createDateTimeComparator = null;

    private static Comparator<FieldData> CreationDateTimeComparator() 
    {
        if (createDateTimeComparator == null) {
            createDateTimeComparator = new Comparator<FieldData>() {
                private long getTimestamp(FieldData fd) {
                    if (fd == null) { return 0L; }
                    long ts = fd.getLong(FieldLayout.DATA_CREATE_TIMESTAMP);
                    return (ts > 0L)? ts : fd.getLong(FieldLayout.DATA_TIMESTAMP);
                }
                public int compare(FieldData fd1, FieldData fd2) {
                    long ts1 = this.getTimestamp(fd1);
                    long ts2 = this.getTimestamp(fd2);
                    return (ts2 == ts1)? 0 : ((ts2 < ts1)? -1 : 1);
                }
            };
        }
        return createDateTimeComparator;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private I18N                        i18n                = null;

    // ------------------------------------------------------------------------

    /**
    *** Motion Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public UnassignedDevicesReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        this.i18n = reqState.getPrivateLabel().getI18N(UnassignedDevicesReport.class);
    }

    // ------------------------------------------------------------------------

    /**
    *** Post report initialization
    **/
    public void postInitialize()
    {
        //ReportConstraints rc = this.getReportConstraints();
        //Print.logInfo("LimitType=" + rc.getSelectionLimitType() + ", Limit=" + rc.getSelectionLimit());
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Override 'getEventData' to reset selected status codes
    *** @param device       The Device for which EventData records will be selected
    *** @param rcdHandler   The DBRecordHandler
    *** @return An array of EventData records for the device
    **/
    protected EventData[] getEventData(Device device, DBRecordHandler rcdHandler)
    {
        return new EventData[0];
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this report supports displaying a map
    *** @return True if this report supports displaying a map, false otherwise
    **/
    public boolean getSupportsMapDisplay()
    {
        return false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the bound ReportLayout singleton instance for this report
    *** @return The bound ReportLayout
    **/
    public static ReportLayout GetReportLayout()
    {
        // bind the report format to this data
        return FieldLayout.getReportLayout();
    }

    /**
    *** Gets the bound ReportLayout singleton instance for this report
    *** @return The bound ReportLayout
    **/
    public ReportLayout getReportLayout()
    {
        // bind the report format to this data
        return GetReportLayout();
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates and returns an iterator for the row data displayed in the body of this report.
    *** @return The body row data iterator
    **/
    public DBDataIterator getBodyDataIterator()
    {

        /* init */
        final java.util.List<FieldData> rowData = new Vector<FieldData>();

        /* record handler */
        DBRecordHandler<UnassignedDevices> rcdHandler = new DBRecordHandler<UnassignedDevices>() {
            public int handleDBRecord(UnassignedDevices rcd) throws DBException {
                UnassignedDevices ud = rcd;
                
                /* see if it now exists */
                String mobileID = ud.getMobileID();
                String serverID = ud.getServerID();
                DCServerConfig dcs = DCServerFactory._getServerConfig(serverID);
                if (dcs != null) {
                    String pfx[] = dcs.getUniquePrefix();
                    try {
                        for (int i = 0; i < pfx.length; i++) {
                            String uniqueID = pfx[i] + mobileID;
                            if (Transport.loadDeviceByUniqueID(uniqueID) != null) {
                                // record now exists
                                Print.logInfo("MobileID is now defined: [%s] %s", serverID, uniqueID);
                                return DBRH_SKIP;
                            }
                        }
                    } catch (DBException dbe) {
                        Print.logWarn("Error check for UniqueID existence: " + dbe);
                        // ignore
                    }
                }

                /* save record */
                FieldData fd = new FieldData();
                fd.setValue(FieldLayout.DATA_TIMESTAMP       , ud.getTimestamp());
                fd.setValue(FieldLayout.DATA_CREATE_TIMESTAMP, ud.getCreationTime());
                fd.setValue(FieldLayout.DATA_SERVER_ID       , serverID);
                fd.setValue(FieldLayout.DATA_UNIQUE_ID       , mobileID);
                fd.setValue(FieldLayout.DATA_IPADDRESS       , ud.getIpAddressString());
                fd.setValue(FieldLayout.DATA_ISDUPLEX        , ud.getIsDuplex());
                fd.setValue(FieldLayout.DATA_LATITUDE        , ud.getLatitude());
                fd.setValue(FieldLayout.DATA_LONGITUDE       , ud.getLongitude());
                rowData.add(fd);
                
                return DBRH_SKIP;
            }
        };
        
        /* iterate through records */
        try {
            UnassignedDevices.getRecordCallback(rcdHandler);
        } catch (DBException dbe) {
            Print.logException("Error", dbe);
        }
        
        /* sort descending by creation date/time */
        ListTools.sort(rowData, CreationDateTimeComparator());

        /* return row iterator */
        return new ListDataIterator(rowData);

    }

    /**
    *** Creates and returns an iterator for the row data displayed in the total rows of this report.
    *** @return The total row data iterator
    **/
    public DBDataIterator getTotalsDataIterator()
    {
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Custom DBRecord callback handler class
    *** @param rcd  The EventData record
    *** @return The returned status indicating whether to continue, or stop
    **/
    public int handleDBRecord(UnassignedDevices rcd)
        throws DBException
    {
        UnassignedDevices ud = rcd;
        return DBRH_SKIP;
    }
    
    // ------------------------------------------------------------------------

}
