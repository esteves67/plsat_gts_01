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

public class GeozoneReport
    extends ReportData
    implements DBRecordHandler<EventData>
{

    // ------------------------------------------------------------------------
    // Properties

    private static final String PROP_geozoneType            = "geozoneType";        // geozone|job
    private static final String PROP_totalByGeozone         = "totalByGeozone";     // true|false
    private static final String PROP_estimateDepartTime     = "estimateDepartTime"; // true|false

    private static final String ZONETYPE_GEOZONE            = "geozone";
    private static final String ZONETYPE_JOB                = "job";
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    // global defined vars (per report instance)

    private boolean                     totalsByGeozone     = false;
    private int                         zoneCodes[]         = null;
    private String                      zoneType            = ZONETYPE_GEOZONE;
    private boolean                     isGeozoneType       = true;
    
    private I18N                        i18n                = null;
    private String                      unknownText         = null;

    // per device vars below (these need to be initialized in "getBodyDataIterator")
    
    private EventData                   firstEvent          = null;

    private long                        arriveTime          = 0L;
    private EventData                   arriveEvent         = null;
    private long                        lastDepartTime      = 0L;
    
    private boolean                     estimateDepartTime  = false;

    private GeozoneDetail               lastGeozoneDetail   = null;
    
    private Vector<FieldData>           rowData             = null;

    // ------------------------------------------------------------------------

    /**
    *** Geozone Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public GeozoneReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        this.i18n = reqState.getPrivateLabel().getI18N(GeozoneReport.class);
        this.unknownText = this.i18n.getString("GeozoneReport.unknownZone","(unknown)");

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

        /* properties */
        this.estimateDepartTime = this.getProperties().getBoolean(PROP_estimateDepartTime, false);
        this.totalsByGeozone    = this.getProperties().getBoolean(PROP_totalByGeozone, false);
        this.zoneType           = this.getProperties().getString(PROP_geozoneType, ZONETYPE_GEOZONE);
        this.isGeozoneType      = !this.zoneType.equalsIgnoreCase(ZONETYPE_JOB);

        /* zone delimiter status codes */
        if (this.isGeozoneType) {
            this.zoneCodes = new int[] {
                StatusCodes.STATUS_GEOFENCE_ARRIVE,
                StatusCodes.STATUS_GEOFENCE_DEPART
            };
        } else {
            this.zoneCodes = new int[] {
                StatusCodes.STATUS_JOB_ARRIVE,
                StatusCodes.STATUS_JOB_DEPART
            };
        }

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
        this.arriveTime         = 0L;
        this.arriveEvent        = null;
        this.lastDepartTime     = 0L;
        this.lastGeozoneDetail  = null;
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

        /* handle final record (if any) */
        if (this.arriveEvent != null) {
            // still in zone
            GeozoneDetail gzd = new GeozoneDetail(this.arriveTime, this.arriveEvent, null, this.i18n);
            this.rowData.add(gzd);
            this.lastDepartTime = 0L;
            this.arriveEvent    = null;
            this.arriveTime     = 0L;
        }

        /* first event is Departure? */
        // If the first event was a 'Departure', then search for the previous 'Arrival' event
        // and fixup the first Geozone accumulator information
        if ((this.rowData.size() > 0) && (this.firstEvent != null) && 
            (this.firstEvent.getStatusCode() == this.zoneCodes[1])) {
            // get arrival event prior to this first received departure
            Print.logInfo("First event was a departure, searching for prior arrival ...");
            try {
                EventData arrEv = this.firstEvent.getPreviousEventData(
                    new int[] { this.zoneCodes[0] },
                    false/*validGPS*/);
                if (arrEv != null) {
                    // update first Geozone accumulator record with actual arrival information
                    Print.logInfo("... Found prior arrival.");
                    GeozoneDetail gzd = (GeozoneDetail)this.rowData.get(0);
                    gzd.fixupArrivalEvent(arrEv);
                } else {
                    Print.logInfo("... Prior arrival not found!");
                }
            } catch (DBException dbe) {
                Print.logException("Error retreiving EventData record",dbe);
            }
        }

        /* totals by Geozone? */
        if (this.totalsByGeozone) {

            /* sort by geozoneID (must be a stable sort) */
            ListTools.sort(this.rowData,null); // StringComparator
    
            /* find the GeozoneID breaks */
            String  lastZoneTypeID    = null;
            boolean stillInZone       = false;
            long totalDwellElapsedSec = 0L;
            long totalDriveElapsedSec = 0L;
            long totalVisitCount      = 0L;
            for (int i = 0; i < this.rowData.size(); i++) {

                /* new record */
                GeozoneDetail gzd = (GeozoneDetail)this.rowData.get(i);

                /* zone break? (reset accumulators) */
                if (lastZoneTypeID == null) {
                    // first geozone
                    //  - reset accumulators
                    lastZoneTypeID       = gzd.getZoneTypeID();
                    stillInZone          = false;
                    totalDwellElapsedSec = 0L;
                    totalDriveElapsedSec = 0L;
                    totalVisitCount      = 0L;
                } else
                if (!lastZoneTypeID.equals(gzd.getZoneTypeID())) {
                    // geozone break
                    //  - save totals for prior zone
                    GeozoneTotal gzt = new GeozoneTotal(lastZoneTypeID, 
                        totalVisitCount, stillInZone, 
                        totalDwellElapsedSec, totalDriveElapsedSec, this.i18n);
                    this.rowData.insertElementAt(gzt,i++); // insert total, increment record pointer also
                    //  - reset accumulators
                    lastZoneTypeID       = gzd.getZoneTypeID();
                    stillInZone          = false;
                    totalDwellElapsedSec = 0L;
                    totalDriveElapsedSec = 0L;
                    totalVisitCount      = 0L;
                }

                /* still in zone */
                stillInZone |= gzd.stillInZone();

                /* count this Geozone visit */
                totalVisitCount++;

                /* inside zone accumulator */
                long insideZoneSec = gzd.getZoneElapseSec();
                if (insideZoneSec < 0L) {
                    // missing either the arrive or depart time
                    totalDwellElapsedSec = -1L; // skip accumulator, since it would be underestimated
                } else
                if ((totalDwellElapsedSec >= 0L) && (insideZoneSec >= 0L)) {
                    totalDwellElapsedSec += insideZoneSec;
                }

                /* outside zone accoumulator */
                long outsideZoneSec = gzd.getDriveElapseSec();
                if (outsideZoneSec < 0L) {
                    // missing arrive time, or last depart time
                    totalDriveElapsedSec = -1L; // skip accumulator
                } else
                if ((totalDriveElapsedSec >= 0L) && (outsideZoneSec >= 0L)) {
                    totalDriveElapsedSec += outsideZoneSec;
                }

            } // 'rowData' iterator

            /* final total record */
            if (lastZoneTypeID != null) {
                GeozoneTotal gzt = new GeozoneTotal(lastZoneTypeID, 
                    totalVisitCount, stillInZone, 
                    totalDwellElapsedSec, totalDriveElapsedSec, 
                    this.i18n);
                this.rowData.insertElementAt(gzt,this.rowData.size());
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
        if (ev == null) {
            throw new DBException("EventData record is null!!");
        }

        /* report initialized? */
        if (this.rowData == null) {
            throw new DBException("Report has not been properly initialized! (rowData)");
        } else
        if (this.zoneCodes == null) {
            throw new DBException("Report has not been properly initialized! (zoneCodes)");
        }

        /* status code */
        int code = ev.getStatusCode();

        /* first event */
        if (this.firstEvent == null) {
            if (code == this.zoneCodes[1]) { // Depart
                // we started out inside a zone, start at report start time
                this.arriveTime = this.getTimeStart();
            } else
            if (code == this.zoneCodes[0]) { // Arrive
                this.lastDepartTime = 0L; // we don't know the last depart time
            }
            this.firstEvent = ev;
        }

        /* geozone arrive */
        if (code == this.zoneCodes[0]) {

            /* save arrival event */
            this.arriveEvent = ev;
            this.arriveTime  = ev.getTimestamp();

            /* fixup prior GeozoneDetail with 'outside' time */
            if (this.lastGeozoneDetail != null) {
                long driveElapseSec = 
                    ((this.lastDepartTime > 0L) && (this.arriveTime > this.lastDepartTime))? 
                    (this.arriveTime - this.lastDepartTime) : -1L;
                this.lastGeozoneDetail.setDriveElapseSec(driveElapseSec);
                this.lastGeozoneDetail = null;
            }

        } else
        if (code == this.zoneCodes[1]) {

            /* save (explicit) departure event */
            this.lastGeozoneDetail = new GeozoneDetail(this.arriveTime, this.arriveEvent, ev, this.i18n);
            this.rowData.add(this.lastGeozoneDetail);
            this.lastDepartTime = ev.getTimestamp();
            this.arriveEvent    = null;
            this.arriveTime     = 0L;

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
        
    private static ColumnValue getColumnValue(Object val) {
        if (val == null) {
            return new ColumnValue("");
        } else
        if (val instanceof ColumnValue) {
            return (ColumnValue)val;
        } else {
            return new ColumnValue(val.toString());
        }
    }

    /**
    *** Custom GeozoneDetail class
    **/
    private class GeozoneDetail
        extends FieldData
    {
        
        private boolean  arriveApprox     = false;
        private long     arriveTime       = 0L;
        
        private boolean  departApprox     = false;
        private long     departTime       = 0L;
        
        private String   geozoneID        = null;
        private GeoPoint geozoneGPS       = null;
        private String   geozoneAddr      = null;
        
        private long     zoneElapseSec    = -1L;
        private long     driveElapseSec   = -1L;

        public GeozoneDetail(long arrTime, EventData arrEv, EventData depEv, I18N i18n) {
            super();
            EventData ev = (arrEv != null)? arrEv : depEv;
            // 'depEv' may be null for the last row in the data iterator

            /* Account/Device */
            this.setAccount(ev.getAccount());
            this.setDevice( ev.getDevice());

            /* GeozoneID/Address */
            this.geozoneID      = GeozoneReport.this.isGeozoneType? ev.getGeozoneID() : ev.getJobNumber();
            this.geozoneGPS     = ev.getGeoPoint();
            this.geozoneAddr    = ev.getAddress();

            /* arrive/depart times */
            this.arriveApprox   = (arrEv == null); // may not be an accurate arrive time
            this.arriveTime     = (arrEv != null)? arrEv.getTimestamp() : arrTime;
            this.departApprox   = (depEv == null); // may not be an accurate depart time
            this.departTime     = (depEv != null)? depEv.getTimestamp() : GeozoneReport.this.getReportEndTime();
            this.zoneElapseSec  = ((this.departTime > 0L) && (this.arriveTime > 0L))? 
                (this.departTime - this.arriveTime) : 
                -1;

            /* Geozone elapsed time */
            this.setValue(FieldLayout.DATA_GEOZONE_ID       , this.geozoneID);
            this.setValue(FieldLayout.DATA_ENTER_GEOZONE_ID , this.geozoneID);
            this.setValue(FieldLayout.DATA_ENTER_TIMESTAMP  , this.arriveTime);
            this.setValue(FieldLayout.DATA_GEOPOINT         , this.geozoneGPS);
            this.setValue(FieldLayout.DATA_ADDRESS          , this.geozoneAddr);
            this.setValue(FieldLayout.DATA_ELAPSE_SEC       , this.zoneElapseSec);
            this.setValue(FieldLayout.DATA_INSIDE_ELAPSED   , this.zoneElapseSec);
            this.setValue(FieldLayout.DATA_EXIT_GEOZONE_ID  , this.geozoneID);
            this.setValue(FieldLayout.DATA_EXIT_TIMESTAMP   , (!this.departApprox || GeozoneReport.this.estimateDepartTime)? this.departTime : 0L);
            this.setValue(FieldLayout.DATA_COUNT            , -1L);

        }

        public String getGeozoneID() {
            return this.geozoneID;
        }

        public String getZoneTypeID() {
            return this.geozoneID; // "geozoneID" or "jobID"
        }

        public void fixupArrivalEvent(EventData arrEv) {
            if (arrEv != null) {
                this.geozoneID      = arrEv.getGeozoneID();
                this.arriveTime     = arrEv.getTimestamp();
                this.geozoneGPS     = arrEv.getGeoPoint();
                this.geozoneAddr    = arrEv.getAddress();
                this.zoneElapseSec  = ((this.departTime > 0L) && (this.arriveTime > 0L))? 
                    (this.departTime - this.arriveTime) : 
                    -1L;
                this.setValue(FieldLayout.DATA_GEOZONE_ID       , this.geozoneID);
                this.setValue(FieldLayout.DATA_ENTER_GEOZONE_ID , this.geozoneID);
                this.setValue(FieldLayout.DATA_ENTER_TIMESTAMP  , this.arriveTime);
                this.setValue(FieldLayout.DATA_GEOPOINT         , this.geozoneGPS);
                this.setValue(FieldLayout.DATA_ADDRESS          , this.geozoneAddr);
                this.setValue(FieldLayout.DATA_ELAPSE_SEC       , this.zoneElapseSec);
                this.setValue(FieldLayout.DATA_INSIDE_ELAPSED   , this.zoneElapseSec);
            }
        }

        public void setDriveElapseSec(long elapseSec) {
            this.driveElapseSec = elapseSec;
            this.setValue(FieldLayout.DATA_DRIVING_ELAPSED  , this.driveElapseSec);
            this.setValue(FieldLayout.DATA_OUTSIDE_ELAPSED  , this.driveElapseSec);
        }

        public long getDriveElapseSec() {
            return this.driveElapseSec;
        }

        public long getZoneElapseSec() {
            return this.zoneElapseSec;
        }

        public String toString() {
            return this.getZoneTypeID();
        }

        public boolean stillInZone() {
            return this.departApprox;
        }

        public Object filterReturnedValue(String key, Object rtnVal) {
            if (rtnVal == null) {
                return "";
            } else
            if (key == null) {
                return rtnVal;
            } else
            if (key.equals(FieldLayout.DATA_GEOZONE_ID)) {
                if (StringTools.isBlank(rtnVal.toString())) {
                    return GeozoneReport.getColumnValue(rtnVal).setValue(GeozoneReport.this.unknownText);
                } else {
                    return rtnVal;
                }
            } else
            if (key.equals(FieldLayout.DATA_ENTER_TIMESTAMP) || 
                key.equals(FieldLayout.DATA_ENTER_DATETIME )   )  {
                if (this.arriveApprox) {
                    return GeozoneReport.getColumnValue(rtnVal).setForegroundColor(ColorTools.DARK_YELLOW);
                } else {
                    return rtnVal;
                }
            } else
            if (key.equals(FieldLayout.DATA_ELAPSE_SEC    ) || 
                key.equals(FieldLayout.DATA_INSIDE_ELAPSED)   )  {
                if (this.departApprox) {
                    return GeozoneReport.getColumnValue(rtnVal).setForegroundColor(ColorTools.DARK_YELLOW);
                } else {
                    return rtnVal;
                }
            } else
            if (key.equals(FieldLayout.DATA_EXIT_TIMESTAMP) || 
                key.equals(FieldLayout.DATA_EXIT_DATETIME )   )  {
                if (this.departApprox) {
                    return GeozoneReport.getColumnValue(rtnVal).setForegroundColor(ColorTools.DARK_YELLOW);
                } else {
                    return rtnVal;
                }
            } else {
                return rtnVal;
            }
        }

    }

    /**
    *** Custom GeozoneTotal class
    **/
    private static class GeozoneTotal
        extends FieldData
    {

        private boolean stillInZone = false;

        public GeozoneTotal(String zoneTypeID, 
            long totalVisitCount, boolean stillInZone,
            long totalDwellElapsed, long totalDriveElapsed, I18N i18n) {
            this.stillInZone = stillInZone;
            this.setCssClass(ReportLayout.CSS_CLASS_BODY_TOTAL);
            String totalText = stillInZone?
                i18n.getString("GeozoneReport.stillInZone","Still in Geozone") :
                i18n.getString("GeozoneReport.timeInZone" ,"Total Time in Geozone");
            this.setValue(FieldLayout.DATA_GEOZONE_ID     , zoneTypeID);
            this.setValue(FieldLayout.DATA_ADDRESS        , totalText);
            this.setValue(FieldLayout.DATA_ELAPSE_SEC     , totalDwellElapsed);
            this.setValue(FieldLayout.DATA_INSIDE_ELAPSED , totalDwellElapsed);
            this.setValue(FieldLayout.DATA_DRIVING_ELAPSED, totalDriveElapsed);
            this.setValue(FieldLayout.DATA_OUTSIDE_ELAPSED, totalDriveElapsed);
            this.setValue(FieldLayout.DATA_COUNT          , totalVisitCount);
        }

        public Object filterReturnedValue(String key, Object rtnVal) {
            if (key == null) {
                return rtnVal;
            } else
            if (rtnVal == null) {
                return "";
            } else
            if (key.equals(FieldLayout.DATA_ELAPSE_SEC    ) || 
                key.equals(FieldLayout.DATA_INSIDE_ELAPSED)   )  {
                if (this.stillInZone) {
                    return GeozoneReport.getColumnValue(rtnVal).setForegroundColor(ColorTools.DARK_YELLOW);
                } else {
                    return rtnVal;
                }
            } else {
                return rtnVal;
            }
        }

    }

}
