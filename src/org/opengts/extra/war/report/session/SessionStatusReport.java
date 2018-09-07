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
package org.opengts.extra.war.report.session;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;
import org.opengts.war.report.field.*;

public class SessionStatusReport
    extends ReportData
{
    
    // ------------------------------------------------------------------------
    // Summary report
    // 1 record per device
    // ------------------------------------------------------------------------

    private java.util.List<FieldData>       rowData         = null;
    private SessionStatsFactory             statsFactory    = null;

    // ------------------------------------------------------------------------

    public SessionStatusReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        if (this.getAccount() == null) {
            throw new ReportException("Account must be specified");
        }
        //if (this.getDeviceCount() < 1) {
        //    throw new ReportException("At least 1 Device must be specified");
        //}
        // report on all authorized devices
        //this.getReportDeviceList().addAllAuthorizedDevices();
        // SessionStatsFactory
        this.statsFactory = Device.getSessionStatsFactory();
    }

    // ------------------------------------------------------------------------

    public void postInitialize()
    {
        //ReportConstraints rc = this.getReportConstraints();
        //Print.logInfo("LimitType=" + rc.getSelectionLimitType() + ", Limit=" + rc.getSelectionLimit());
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

    /* return an object that will iterate through the selected row object */
    public DBDataIterator getBodyDataIterator()
    {

        /* init */
        this.rowData = new Vector<FieldData>();

        /* loop through devices */
        String devID = "";
        ReportDeviceList devList = this.getReportDeviceList();
        for (Iterator i = devList.iterator(); i.hasNext();) {
            devID = (String)i.next();
            try {
                Device device  = devList.getDevice(devID);
                if (device != null) {
                    long eventCount     = this.countEventData(device);
                    long connCount[]    = this.countConnections(device);
                    long byteCount[]    = this.countDataBytes(device);
                    long connTotal      = connCount[0] + connCount[1]; // TCP + UDP connections
                    long bytesRead      = byteCount[0];
                    long bytesWritten   = byteCount[1];
                    long bytesOverhead  = byteCount[2];
                    long bytesTotal     = bytesRead + bytesWritten + bytesOverhead;
                    long bytesRounded   = byteCount[3];
                    FieldData fd = new FieldData();
                    fd.setDevice(device);
                    fd.setString(FieldLayout.DATA_DEVICE_ID         , devID);           // device
                    fd.setLong(  FieldLayout.DATA_EVENTS_RECEIVED   , eventCount);      // event count
                    fd.setLong(  FieldLayout.DATA_TCP_CONNECTIONS   , connCount[0]);    // TCP connect count
                    fd.setLong(  FieldLayout.DATA_UDP_CONNECTIONS   , connCount[1]);    // UDP connect count
                    fd.setLong(  FieldLayout.DATA_CONNECTIONS       , connTotal);       // TCP/UDP connect count
                    fd.setLong(  FieldLayout.DATA_BYTES_READ        , bytesRead);       // bytesRead
                    fd.setLong(  FieldLayout.DATA_BYTES_WRITTEN     , bytesWritten);    // bytesWritten
                    fd.setLong(  FieldLayout.DATA_BYTES_OVERHEAD    , bytesOverhead);   // bytesOverhead
                    fd.setLong(  FieldLayout.DATA_BYTES_TOTAL       , bytesTotal);      // bytesTotal
                    fd.setLong(  FieldLayout.DATA_BYTES_ROUNDED     , bytesRounded);    // bytesRounded
                    this.rowData.add(fd);
                } else {
                    // should never occur
                    Print.logError("Returned DeviceList 'Device' is null: " + devID);
                }
            } catch (DBException dbe) {
                Print.logError("Error retrieving EventData count for Device: " + devID);
            }
        }

        /* return data iterator */
        FieldData.sortByDeviceDescription(this.rowData);
        return new ListDataIterator(this.rowData);
        
    }

    /* return the totals data list */
    public DBDataIterator getTotalsDataIterator()
    {
        return null;
    }

    // ------------------------------------------------------------------------

    /* return bytes read/written */
    private long[] countConnections(Device device)
    {
        try {
            long timeStart = super.getTimeStart();
            long timeEnd   = super.getTimeEnd();
            long connCounts[] = (this.statsFactory != null)? 
                this.statsFactory.getConnectionCounts(device,timeStart,timeEnd) : 
                null;
            return (connCounts != null)? connCounts : new long[] { 0L, 0L };
        } catch (DBException dbe) {
            Print.logException("Counting Connections", dbe);
            return new long[] { 0L, 0L };
        }
    }

    /* return bytes read/written */
    private long[] countDataBytes(Device device)
    {
        try {
            long timeStart = super.getTimeStart();
            long timeEnd   = super.getTimeEnd();
            long byteCounts[] = (this.statsFactory != null)? 
                this.statsFactory.getByteCounts(device,timeStart,timeEnd) : 
                null;
            return (byteCounts != null)? byteCounts : new long[] { 0L, 0L, 0L, 0L };
        } catch (DBException dbe) {
            Print.logException("Counting Data Bytes", dbe);
            return new long[] { 0L, 0L, 0L, 0L };
        }
    }

    // ------------------------------------------------------------------------

}
