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

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;
import org.opengts.war.report.field.*;

public class GeozoneDepartReport
    extends ReportData
    implements DBRecordHandler<EventData>
{
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    // global defined vars (per report instance)
    
    private int                         zoneCodes[]         = null;
    
    private I18N                        i18n                = null;
    private String                      unknownText         = null;

    // per device vars below (these need to be initialized in "getBodyDataIterator")
    
    private EventData                   firstEvent          = null;

    private EventData                   departEvent         = null;
    private EventData                   arriveEvent         = null;

    private DrivingDetail               lastDrivingDetail   = null;
    private DrivingDetail               fixupDrivingDetail  = null;
    
    private Vector<FieldData>           rowData             = null;

    // ------------------------------------------------------------------------

    /**
    *** Geozone Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public GeozoneDepartReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        this.i18n = reqState.getPrivateLabel().getI18N(GeozoneDepartReport.class);
        this.unknownText = this.i18n.getString("GeozoneDepartReport.unknownZone","(unknown)");

        /* Account check */
        if (this.getAccount() == null) {
            throw new ReportException("Account-ID not specified");
        }

        /* Device check */
        if (this.getDeviceCount() <= 0) {
            throw new ReportException("No Devices specified");
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this report handles only a single device at a time
    *** @return True If this report handles only a single device at a time
    **/
    public boolean isSingleDeviceOnly()
    {
        return true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Post report initialization
    **/
    public void postInitialize()
    {

        /* zone delimiter status codes */
        this.zoneCodes = new int[] {
            StatusCodes.STATUS_GEOFENCE_DEPART,
            StatusCodes.STATUS_GEOFENCE_ARRIVE
        };

    }
    
    // ------------------------------------------------------------------------

    /**
    *** Override 'getEventData' to reset selected status codes
    *** @param device       The Device for which EventData records will be selected
    *** @param rcdHandler   The DBRecordHandler
    *** @return An array of EventData records for the device
    **/
    protected EventData[] getEventData(Device device, DBRecordHandler<EventData> rcdHandler)
    {

        /* Device */
        if (device == null) {
            return EventData.EMPTY_ARRAY;
        }
        
        /* adjust report constraints */
        ReportConstraints rc = this.getReportConstraints();
        rc.setStatusCodes(this.zoneCodes);
        rc.setValidGPSRequired(false); // don't need just valid gps events

        /* get events */
        return super.getEventData(device, rcdHandler);
        
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
        this.firstEvent         = null;
        this.departEvent        = null;
        this.arriveEvent        = null;
        this.lastDrivingDetail  = null;
        this.fixupDrivingDetail = null;
        this.rowData            = new Vector<FieldData>();

        /* loop through devices (should be only 1) */
        String accountID = this.getAccountID();
        String devID     = "";
        ReportDeviceList devList = this.getReportDeviceList();
        for (Iterator i = devList.iterator(); i.hasNext();) {
            devID = (String)i.next();
            try {
                Device device = devList.getDevice(devID);
                this.getEventData(device, this); // <== callback to 'handleDBRecord'
            } catch (DBException dbe) {
                Print.logError("Error retrieving EventData for Device: " + devID);
            }
        }

        /* first event is Arrival? */
        // If the first event was a 'Arrival', then search for the previous 'Departure' event
        // and fixup the first Geozone accumulator information
        if ((this.fixupDrivingDetail != null) && this.fixupDrivingDetail.hasArriveEvent() &&
            !this.fixupDrivingDetail.hasDepartEvent()) {
            // get departure event prior to this first received arrive
            Print.logInfo("First event was an arrival, searching for prior departure ...");
            EventData arrEv = this.fixupDrivingDetail.getArriveEvent();
            try {
                EventData depEv = arrEv.getPreviousEventData(
                    new int[] { this.zoneCodes[0] }, // Depart
                    false/*validGPS*/);
                if (depEv != null) {
                    // update first Geozone accumulator record with actual departure information
                    Print.logInfo("... Found prior departure.");
                    this.fixupDrivingDetail.setDepartEvent(depEv);
                } else {
                    Print.logInfo("... Prior departure not found!");
                }
            } catch (DBException dbe) {
                Print.logException("Error retreiving EventData record",dbe);
            }
        }

        /* return row iterator */
        return new ListDataIterator(this.rowData);
        
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
    public int handleDBRecord(EventData rcd)
        throws DBException
    {
        EventData ev = rcd;
        int code = ev.getStatusCode();

        /* first event */
        if (this.firstEvent == null) {
            this.firstEvent = ev;
        }

        /* geozone depart/arrive */
        if (code == this.zoneCodes[0]) { // Depart
            // trip start

            /* save depart event */
            this.departEvent = ev;

            /* create/save drive detail */
            this.lastDrivingDetail = new DrivingDetail(ev.getDevice());
            this.lastDrivingDetail.setDepartEvent(this.departEvent);
            this.rowData.add(this.lastDrivingDetail);
            
        } else
        if (code == this.zoneCodes[1]) { // Arrive
            
            /* save arrive event */
            this.arriveEvent = ev;

            /* fixup prior DrivingDetail with driving time */
            if (this.lastDrivingDetail != null) {
                this.lastDrivingDetail.setArriveEvent(this.arriveEvent);
            } else {
                Print.logWarn("Found 'Arrive' without previous 'Depart'");
                if (this.rowData.size() == 0) {
                    this.fixupDrivingDetail = new DrivingDetail(ev.getDevice());
                    this.fixupDrivingDetail.setArriveEvent(this.arriveEvent);
                    this.rowData.add(this.fixupDrivingDetail);
                }
            }
            this.lastDrivingDetail = null; // start over

        }

        /* return record limit status */
        return (this.rowData.size() < this.getReportLimit())? DBRH_SKIP : DBRH_STOP;
        
    }
    
    protected long getReportEndTime()
    {
        long nowTime = DateTime.getCurrentTimeSec();
        long endTime = this.getTimeEnd();
        return (nowTime < endTime)? nowTime : endTime;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Custom DrivingDetail class
    **/
    private class DrivingDetail
        extends FieldData
    {

        private EventData   departEvent     = null;
        private EventData   arriveEvent     = null;

        public DrivingDetail(Device device) {
            this.setAccount(device.getAccount());
            this.setDevice( device);
        }
        
        // ----

        public boolean hasDepartEvent() {
            return (this.departEvent != null);
        }
        
        public void setDepartEvent(EventData ev) {
            if (ev != null) {
                this.departEvent = ev;
                this.setValue(FieldLayout.DATA_EXIT_GEOZONE_ID  , this.departEvent.getGeozoneID());
                this.setValue(FieldLayout.DATA_EXIT_TIMESTAMP   , this.departEvent.getTimestamp());
                this.setValue(FieldLayout.DATA_DRIVER_ID        , this.departEvent.getDriverID());
            } else {
                this.departEvent = null;
                this.setValue(FieldLayout.DATA_EXIT_GEOZONE_ID  , "?");
                this.setValue(FieldLayout.DATA_EXIT_TIMESTAMP   , 0L);
                this.setValue(FieldLayout.DATA_DRIVER_ID        , "?");
            }
            this.setDrivingElapsedTime();
        }
        
        public EventData getDepartEvent() {
            return this.departEvent;
        }

        // ----
        
        public boolean hasArriveEvent() {
            return (this.arriveEvent != null);
        }

        public void setArriveEvent(EventData ev) {
            if (ev != null) {
                this.arriveEvent = ev;
                this.setValue(FieldLayout.DATA_ENTER_GEOZONE_ID , this.arriveEvent.getGeozoneID());
                this.setValue(FieldLayout.DATA_ENTER_TIMESTAMP  , this.arriveEvent.getTimestamp());
            } else {
                this.arriveEvent = null;
                this.setValue(FieldLayout.DATA_ENTER_GEOZONE_ID , "?");
                this.setValue(FieldLayout.DATA_ENTER_TIMESTAMP  , 0L);
            }
            this.setDrivingElapsedTime();
        }
       
        public EventData getArriveEvent() {
            return this.arriveEvent;
        }

        // ----
        
        public void setDrivingElapsedTime() {
            if ((this.departEvent != null) && (this.arriveEvent != null)) {
                long drivingSec = this.arriveEvent.getTimestamp() - this.departEvent.getTimestamp();
                this.setValue(FieldLayout.DATA_DRIVING_ELAPSED  , drivingSec);
                this.setValue(FieldLayout.DATA_OUTSIDE_ELAPSED  , drivingSec);
                double drivingDistKM = this.arriveEvent.getOdometerKM() - this.departEvent.getOdometerKM();
                this.setValue(FieldLayout.DATA_ODOMETER_DELTA   , drivingDistKM);
            } else {
                this.setValue(FieldLayout.DATA_DRIVING_ELAPSED  , 0L);
                this.setValue(FieldLayout.DATA_OUTSIDE_ELAPSED  , 0L);
                this.setValue(FieldLayout.DATA_ODOMETER_DELTA   , 0.0);
            }
        }

        // ----
        
        public String toString() {
            return "";
        }

        public Object filterReturnedValue(String key, String rtnVal) {
            if (key == null) {
                return rtnVal;
            } else
            if (key.equals(FieldLayout.DATA_GEOZONE_ID)) {
                return StringTools.isBlank(rtnVal)?
                    GeozoneDepartReport.this.unknownText :
                    rtnVal;
            } else
            if (key.equals(FieldLayout.DATA_ENTER_TIMESTAMP) || 
                key.equals(FieldLayout.DATA_ENTER_DATETIME ))  {
                return /*this.arriveApprox*/false?
                    (new ColumnValue(rtnVal)).setForegroundColor(ColorTools.DARK_YELLOW) :
                    rtnVal;
            } else
            if (key.equals(FieldLayout.DATA_EXIT_TIMESTAMP) || 
                key.equals(FieldLayout.DATA_EXIT_DATETIME ))  {
                return /*this.departApprox*/false?
                    (new ColumnValue(rtnVal)).setForegroundColor(ColorTools.DARK_YELLOW) :
                    rtnVal;
            } else {
                return rtnVal;
            }
        }

    }

}
