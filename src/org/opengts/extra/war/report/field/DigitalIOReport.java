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

public class DigitalIOReport
    extends ReportData
    implements DBRecordHandler<EventData>
{

    // ------------------------------------------------------------------------
    // Properties

    private static final String         PROP_digitalInputIndex  = "digitalInputIndex";  // [ignition|#]
    private static final String         PROP_reportType         = "reportType";         // detail|summary
    
    private static final String         REPORT_TYPE_detail      = "detail";
    private static final String         REPORT_TYPE_summary     = "summary";
    
    private static final boolean        FIXUP_FIRST_DIGITIAL_ON = false;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private I18N                        i18n                    = null;
    private boolean                     isDetailReport          = true;
    
    private Device                      device                  = null;

    private int                         statusDigital_ON        = StatusCodes.STATUS_IGNITION_ON;
    private int                         statusDigital_OFF       = StatusCodes.STATUS_IGNITION_OFF;
    private String                      description             = null;

    private EventData                   firstEvent              = null;
    private EventData                   ioEvent_on              = null;
    private EventData                   ioEvent_off             = null;

    private Vector<FieldData>           rowData                 = null;
    private Vector<FieldData>           totalData               = null;

    private boolean                     stillOn                 = false;
    private long                        totalElapsedSec_on      = 0L;

    // ------------------------------------------------------------------------

    /**
    *** DigitalInput Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public DigitalIOReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        this.i18n = reqState.getPrivateLabel().getI18N(DigitalIOReport.class);

        /* Account check */
        if (this.getAccount() == null) {
            throw new ReportException("Account-ID not specified");
        }

        /* detail report */
        this.isDetailReport = this.getProperties().getString(PROP_reportType,REPORT_TYPE_detail).equalsIgnoreCase(REPORT_TYPE_detail);

        /* Device check */
        //if (this.isDetailReport && (this.getDeviceCount() != 1)) {
        //    throw new ReportException("1 and only 1 Device must be specified");
        //}
        if (this.getDeviceCount() <= 0) {
            throw new ReportException("No Devices specified");
        }

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
    *** Returns true if this report handles only a single device at a time
    *** @return True If this report handles only a single device at a time
    **/
    public boolean isSingleDeviceOnly()
    {
        return this.isDetailReport? true : false;
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
        rc.setStatusCodes(new int[] {
            this.statusDigital_ON,
            this.statusDigital_OFF
        });
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
        return true;
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
        this.rowData = this.isDetailReport? new Vector<FieldData>() : null;
        this.totalData = new Vector<FieldData>();

        /* loop through devices (should be only 1) */
        String accountID = this.getAccountID();
        String devID = "";
        ReportDeviceList devList = this.getReportDeviceList();
        for (Iterator i = devList.iterator(); i.hasNext();) {
            devID = (String)i.next();
            try {

                /* get device */
                this.device = devList.getDevice(devID);

                /* set status codes */
                String ioNdxStr = this.getProperties().getString(PROP_digitalInputIndex, "ignition");
                if (ioNdxStr.equalsIgnoreCase("ignition") || ioNdxStr.equalsIgnoreCase("ign")) {
                    int ignCodes[] = this.device.getIgnitionStatusCodes();
                    if (ignCodes != null) {
                        this.statusDigital_OFF = ignCodes[0];
                        this.statusDigital_ON  = ignCodes[1];
                        this.description = this.i18n.getString("DigitalIOReport.ignition","Ignition");
                    } else {
                        Print.logWarn("Device does not support 'ignition' status codes");
                        this.statusDigital_OFF = StatusCodes.STATUS_IGNITION_OFF;
                        this.statusDigital_ON  = StatusCodes.STATUS_IGNITION_ON ;
                        this.description = this.i18n.getString("DigitalIOReport.ignition","Ignition");
                    }
                } else
                if (ioNdxStr.equalsIgnoreCase("engine") || ioNdxStr.equalsIgnoreCase("eng")) {
                    this.statusDigital_OFF = StatusCodes.STATUS_ENGINE_STOP ;
                    this.statusDigital_ON  = StatusCodes.STATUS_ENGINE_START;
                    this.description = this.i18n.getString("DigitalIOReport.engine","Engine");
                } else {
                    int ioNdx = StringTools.parseInt(ioNdxStr, -1);
                    int scOFF = StatusCodes.GetDigitalInputStatusCode(ioNdx, false);
                    int scON  = StatusCodes.GetDigitalInputStatusCode(ioNdx, true );
                    if (scOFF != StatusCodes.STATUS_NONE) {
                        this.statusDigital_OFF = scOFF;
                        this.statusDigital_ON  = scON;
                      //this.description = "Input #" + ioNdx;
                        this.description = this.i18n.getString("DigitalIOReport.input","Input #{0}",ioNdxStr);
                    } else {
                        Print.logWarn("Invalid Digital Input Index: " + ioNdxStr);
                        this.statusDigital_OFF = StatusCodes.STATUS_IGNITION_OFF;
                        this.statusDigital_ON  = StatusCodes.STATUS_IGNITION_ON ;
                        this.description = this.i18n.getString("DigitalIOReport.ignition","Ignition");
                    }
                }

                /* reset */
                this.firstEvent     = null;
                this.ioEvent_on     = null;
                this.ioEvent_off    = null;
                this.stillOn        = false;
                this.totalElapsedSec_on = 0L;
            
                /* get events */
                this.getEventData(this.device, this); // <== callback to 'handleDBRecord'
                
            } catch (DBException dbe) {
                Print.logError("Error retrieving EventData for Device: " + devID);
            }

            /* handle final/last record (if any) */
            if (this.ioEvent_on != null) {
                // digital input is still ON
                this.totalElapsedSec_on += this.createDigitalInputDetail(this.ioEvent_on, null);
                this.ioEvent_on = null;
                this.stillOn    = true;
            }

            /* first event is 'Off'? */
            // If the first event was an 'Off', then search for the previous 'On' event
            // and fixup the first DigitalInput accumulator information
            if (FIXUP_FIRST_DIGITIAL_ON && this.isDetailReport &&
                (this.rowData.size() > 0) && (this.firstEvent != null) && 
                (this.firstEvent.getStatusCode() == this.statusDigital_OFF)) {
                // get DigitialInput 'On' event prior to this first received 'Off'
                Print.logInfo("First event was a DigitalInput 'Off', searching for prior 'On' ...");
                try {
                    EventData ioEv_on = this.firstEvent.getPreviousEventData(
                        new int[] { this.statusDigital_ON },
                        false/*validGPS*/);
                    if (ioEv_on != null) {
                        // update first DigitalInput accumulator record with actual 'On' information
                        Print.logInfo("... Found prior Digital Input On.");
                        DigitalInputDetail did = (DigitalInputDetail)this.rowData.get(0);
                        this.totalElapsedSec_on -= did.getIoElapsedSec_On(); // subtract old value
                        did.fixupIoEvent_On(ioEv_on);
                        this.totalElapsedSec_on += did.getIoElapsedSec_On(); // add new value
                    } else {
                        Print.logInfo("... Prior Digital Input 'On' not found!");
                    }
                } catch (DBException dbe) {
                    Print.logException("Error retreiving EventData record",dbe);
                }
            }

            /* save total */
            long elapsed = (this.isDetailReport || (this.totalElapsedSec_on > 0L))? this.totalElapsedSec_on : -1L;
            DigitalInputTotal dit = new DigitalInputTotal(this.stillOn, elapsed);
            this.totalData.add(dit);

        }

        
        /* return row iterator */
        if (this.isDetailReport) {
            return new ListDataIterator(this.rowData);
        } else {
            // TODO: sort by deviceID
            return new ListDataIterator(this.totalData);
        }
        
    }

    /**
    *** Creates and returns an iterator for the row data displayed in the total rows of this report.
    *** @return The total row data iterator
    **/
    public DBDataIterator getTotalsDataIterator()
    {

        /* return row iterator */
        return this.isDetailReport? 
            new ListDataIterator(this.totalData) : 
            null;

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
            if (code == this.statusDigital_OFF) {
                // we started out 'On', start at report start time
                // 'this.ioEvent_on' is null
            } else
            if (code == this.statusDigital_ON) {
                // normal beginning
            }
            this.firstEvent = ev;
        }

        /* DigitalInput 'On' */
        if (code == this.statusDigital_ON) {

            /* save DigitalInput 'On' event */
            if (this.ioEvent_on == null) {
                // normal ON ...
                this.ioEvent_on = ev;
            } else {
                // this is the second ON in a row
                //this.ioEvent_on = ev;    <-- ignore this ON
            }

        } else
        if (code == this.statusDigital_OFF) {

            /* save DigitalInput 'Off' event */
            if (this.ioEvent_on != null) {
                // normal ON..OFF transition
                this.totalElapsedSec_on += this.createDigitalInputDetail(this.ioEvent_on, ev);
            } else
            if (ev == this.firstEvent) {
                // 'ioEvent_on' is null, only allowed if this OFF event is the first event
                this.totalElapsedSec_on += this.createDigitalInputDetail(null, ev);
            }
            this.ioEvent_on = null;

        }

        /* return record limit status */
        if (this.isDetailReport) {
            return (this.rowData.size() < this.getReportLimit())? DBRH_SKIP : DBRH_STOP;
        } else {
            return (this.totalData.size() < this.getReportLimit())? DBRH_SKIP : DBRH_STOP;
        }
        
    }

    // ------------------------------------------------------------------------

    protected long getReportStartTime()
    {
        long startTime = this.getTimeStart();
        return startTime;
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

    private long createDigitalInputDetail(EventData ioEv_on, EventData ioEv_off)
    {
        // 'ioEv_on' may be null for the first report row (should never be null otherwise)
        // 'ioEv_off' may be null for the last report row (should never be null otherwise)

        /* DigitalInput "on" */
        boolean ioApprox_on  = (ioEv_on  == null); // may not be an accurate IO 'on' time
        long    ioTime_on    = (ioEv_on  != null)? ioEv_on.getTimestamp()  : this.getReportStartTime();
        GeoPoint ioLoc_on    = (ioEv_on  != null)? ioEv_on.getGeoPoint()   : null;
        String  ioAddr_on    = (ioEv_on  != null)? ioEv_on.getAddress()    : "";

        /* DigitalInput "off" */
        boolean ioApprox_off = (ioEv_off == null); // may not be an accurate IO 'off' time
        long    ioTime_off   = (ioEv_off != null)? ioEv_off.getTimestamp() : this.getReportEndTime();
        GeoPoint ioLoc_off   = (ioEv_off != null)? ioEv_off.getGeoPoint()  : null;
        String  ioAddr_off   = (ioEv_off != null)? ioEv_off.getAddress()   : "";
        
        /* elapsed time */
        long  ioElapseSec_on = ((ioTime_off > 0L) && (ioTime_on > 0L))? (ioTime_off - ioTime_on) : -1L;

        /* create report row */
        if (this.isDetailReport) {
            DigitalInputDetail did = new DigitalInputDetail(
                ioApprox_on , ioTime_on , ioLoc_on , ioAddr_on ,
                ioApprox_off, ioTime_off, ioLoc_off, ioAddr_off,
                ioElapseSec_on
                );
            this.rowData.add(did);
        }
        
        return (ioElapseSec_on > 0L)? ioElapseSec_on : 0L;
    }
    
    /**
    *** Custom DigitalInputDetail class
    **/
    private class DigitalInputDetail
        extends FieldData
        implements EventDataProvider
    {

        private boolean ioApprox_on      = false;
        private long    ioTime_on        = 0L;

        private boolean ioApprox_off     = false;
        private long    ioTime_off       = 0L;

        private long    ioElapseSec_on   = -1L;

        public DigitalInputDetail(
            boolean ioApprox_on,  long ioTime_on , GeoPoint ioLoc_on , String ioAddr_on ,
            boolean ioApprox_off, long ioTime_off, GeoPoint ioLoc_off, String ioAddr_off,
            long ioElapseSec_on
            ) {
            this.ioApprox_on    = ioApprox_on;  // may not be an accurate IO 'on' time
            this.ioTime_on      = ioTime_on;
            this.ioApprox_off   = ioApprox_off; // may not be an accurate IO 'off' time
            this.ioTime_off     = ioTime_off;
            this.ioElapseSec_on = ioElapseSec_on;
            this.setAccount(DigitalIOReport.this.device.getAccount());
            this.setDevice( DigitalIOReport.this.device);
            this.setValue(FieldLayout.DATA_ENTER_TIMESTAMP  , this.ioTime_on);
            this.setValue(FieldLayout.DATA_ELAPSE_SEC       , this.ioElapseSec_on);
            this.setValue(FieldLayout.DATA_INSIDE_ELAPSED   , this.ioElapseSec_on);
            this.setValue(FieldLayout.DATA_EXIT_TIMESTAMP   , this.ioTime_off);     // timestamp off
            this.setValue(FieldLayout.DATA_STOP_TIMESTAMP   , this.ioTime_off);     // timestamp off
            this.setValue(FieldLayout.DATA_STOP_GEOPOINT    ,      ioLoc_off);      // geopoint off
            this.setValue(FieldLayout.DATA_STOP_ADDRESS     ,      ioAddr_off);
            this.setValue(FieldLayout.DATA_ADDRESS          ,      ioAddr_off);
        }

        public String getAccountID() {
            return super.getAccountID();
        }
        public String getDeviceID() {
            return super.getDeviceID();
        }
        public String getDeviceDescription() {
            return super.getDeviceDescription();
        }
        public String getDeviceVIN() {
            return super.getDeviceVIN();
        }
        public long getTimestamp() {
            return super.getLong(FieldLayout.DATA_STOP_TIMESTAMP, 0L);
        }
        public int getStatusCode() {
            return StatusCodes.STATUS_INPUT_OFF;
        }
        public String getStatusCodeDescription(BasicPrivateLabel bpl) {
            return "InputOff";
        }
        public int getPushpinIconIndex(String iconSelector, OrderedSet<String> iconKeys, 
            boolean isFleet, BasicPrivateLabel bpl) {
            return EventData.ICON_PUSHPIN_RED;
        }
        public boolean isValidGeoPoint() {
            return GeoPoint.isValid(this.getLatitude(), this.getLongitude());
        }
        public double getLatitude() {
            GeoPoint gp = super.getGeoPoint(FieldLayout.DATA_STOP_GEOPOINT, null);
            return (gp != null)? gp.getLatitude() : 0.0;
        }
        public double getLongitude() {
            GeoPoint gp = super.getGeoPoint(FieldLayout.DATA_STOP_GEOPOINT, null);
            return (gp != null)? gp.getLongitude() : 0.0;
        }
        public GeoPoint getGeoPoint() {
            return new GeoPoint(this.getLatitude(), this.getLongitude());
        }
        public double getHorzAccuracy() {
            return -1.0; // not available
        }
        public GeoPoint getBestGeoPoint() {
            return this.getGeoPoint();
        }
        public double getBestAccuracy() {
            return this.getHorzAccuracy();
        }
        public int getSatelliteCount() {
            return 0;
        }
        public double getBatteryLevel() {
            return 0.0;
        }
        public double getSpeedKPH() {
            return 0.0;
        }
        public double getHeading() {
            return 0.0;
        }
        public double getAltitude() {
            return 0.0;
        }
        public double getOdometerKM() {
            return 0.0;
        }
        public String getGeozoneID() {
            return "";
        }
        public String getAddress() {
            return super.getString(FieldLayout.DATA_STOP_ADDRESS, "");
        }
        public long getInputMask() {
            return 0L;
        }
        public void setEventIndex(int ndx)
        {
            super.setInt(FieldLayout.DATA_EVENT_INDEX,ndx);
        }
        public int getEventIndex()
        {
            return super.getInt(FieldLayout.DATA_EVENT_INDEX,-1);
        }
        public boolean getIsFirstEvent()
        {
            return (this.getEventIndex() == 0);
        }
        public void setIsLastEvent(boolean isLast) {
            super.setBoolean(FieldLayout.DATA_LAST_EVENT,isLast);
        }
        public boolean getIsLastEvent() {
            return super.getBoolean(FieldLayout.DATA_LAST_EVENT,false);
        }

        public void fixupIoEvent_On(EventData ioEv_on) {
            if (ioEv_on != null) {
                this.ioTime_on      = ioEv_on.getTimestamp();
                this.ioElapseSec_on = ((this.ioTime_off > 0L) && (this.ioTime_on > 0L))? 
                    (this.ioTime_off - this.ioTime_on) : 
                    -1L;
                this.setValue(FieldLayout.DATA_ENTER_TIMESTAMP  , this.ioTime_on);
                this.setValue(FieldLayout.DATA_ELAPSE_SEC       , this.ioElapseSec_on);
                this.setValue(FieldLayout.DATA_INSIDE_ELAPSED   , this.ioElapseSec_on);
            }
        }

        public String toString() {
            return DigitalIOReport.this.description;
        }

        public long getIoElapsedSec_On() {
            return (this.ioElapseSec_on > 0L)? this.ioElapseSec_on : 0L;
        }

        public Object filterReturnedValue(String key, Object rtnVal) {
            if (rtnVal == null) {
                return "";
            } else
            if (key == null) {
                return rtnVal;
            } else
            if (key.equals(FieldLayout.DATA_ENTER_TIMESTAMP) || 
                key.equals(FieldLayout.DATA_ENTER_DATETIME ))  {
                if (this.ioApprox_on) {
                    return DigitalIOReport.getColumnValue(rtnVal).setForegroundColor(ColorTools.DARK_YELLOW);
                } else {
                    return rtnVal;
                }
            } else
            if (key.equals(FieldLayout.DATA_EXIT_TIMESTAMP) || 
                key.equals(FieldLayout.DATA_EXIT_DATETIME ))  {
                if (this.ioApprox_off) {
                    return DigitalIOReport.getColumnValue(rtnVal).setForegroundColor(ColorTools.DARK_YELLOW);
                } else {
                    return rtnVal;
                }
            } else {
                return rtnVal;
            }
        }

    }

    /**
    *** Custom DigitalInputTotal class
    **/
    private class DigitalInputTotal
        extends FieldData
    {
        public DigitalInputTotal(boolean stillOn, long totalElapsed_on) {
            String totalText = stillOn?
                DigitalIOReport.this.i18n.getString("DigitialIOReport.stillOn","Still 'On'") :
                DigitalIOReport.this.i18n.getString("DigitialIOReport.timeInZone","Total Time 'On'");
            this.setRowType(DBDataRow.RowType.TOTAL);
            this.setAccount(DigitalIOReport.this.device.getAccount());
            this.setDevice( DigitalIOReport.this.device);
            this.setValue(FieldLayout.DATA_ELAPSE_SEC     , totalElapsed_on);
            this.setValue(FieldLayout.DATA_INSIDE_ELAPSED , totalElapsed_on);
        }
    }

}
