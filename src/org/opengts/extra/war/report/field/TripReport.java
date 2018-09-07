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

public class TripReport
    extends ReportData
    implements DBRecordHandler<EventData>
{

    // ------------------------------------------------------------------------
    // Properties

    private static final String PROP_tripStartType          = "tripStartType";
    private static final String PROP_minimumStoppedTime     = "minimumStoppedTime"; // TRIP_ON_SPEED only
    private static final String PROP_minimumSpeedKPH        = "minimumSpeedKPH";
    private static final String PROP_stopOnIgnitionOff      = "stopOnIgnitionOff";
    private static final String PROP_estimateFuelTotal      = "estimateFuelTotal";
    private static final String PROP_kilometersPerLiter     = "kilometersPerLiter";
    private static final String PROP_tripStartStopOnly      = "tripStartStopOnly";

    // ------------------------------------------------------------------------
    // Trip start types
    
    private static final String MOTION_DEFAULT[]            = new String[] { "default" };
    private static final String MOTION_SPEED[]              = new String[] { "speed", "motion" };
    private static final String MOTION_IGNITION[]           = new String[] { "ignition" };
    private static final String MOTION_ENGINE[]             = new String[] { "engine" };
    private static final String MOTION_STARTSTOP[]          = new String[] { "start", "startstop" };

    private static final int    TRIP_ON_SPEED               = 0;
    private static final int    TRIP_ON_IGNITION            = 1;
    private static final int    TRIP_ON_ENGINE              = 2;
    private static final int    TRIP_ON_START               = 3;

    /**
    *** Minimum speed used for determining in-motion when the device does not
    *** support start/stop events
    **/
    private static final double MIN_SPEED_KPH               = 5.0;

    /**
    *** Default mimimum stopped elapsed time to be considered stopped
    **/
    private static final long   MIN_STOPPED_TIME_SEC        = DateTime.MinuteSeconds(5);

    /**
    *** Delimit stop with ignition off? (if this occurs before the minimum stopped time)
    **/
    private static final boolean STOP_ON_IGNITION_OFF       = false;

    /**
    *** Display only trip "Start" and "Stop" events (ie. omit interleaving detail)?
    **/
    private static final boolean TRIP_START_STOP_ONLY       = false;

    /**
    *** Default to estimate fuel usage total (should be 'false')
    **/
    private static final boolean ESTIMATE_FUEL_TOTAL        = false;
    private static final double  KILOMETERS_PER_LITER       = 20.0 * GeoPoint.KILOMETERS_PER_MILE * Account.US_GALLONS_PER_LITER;
    // mi/G * 1.609344 km/mi * 0.264172052 G/L = km/L
    // mi/G * 0.425144 ((km*G)/(mi*L)) = km/L
    
    /**
    *** Non-zero Liters value (but still near-equivalent to zero)
    *** (Used to trick layout into displaying a "0.0" value, rather than blank)
    **/
    private static final double  ZERO_LITERS                = 0.001;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int                         ignitionCodes[]     = null;

    private int                         tripStartType       = TRIP_ON_SPEED;
    private EventData                   tripStartEvent      = null;
    private EventData                   firstEvent          = null;
    private boolean                     approxStart         = true;
    
    private boolean                     devEstFuelTotal     = ESTIMATE_FUEL_TOTAL;
    private double                      devFuelEconomy      = 0.0;

    private boolean                     isMoving            = false; // TRIP_ON_SPEED only
    private long                        lastMovingTime      = 0L;
    private long                        lastStoppedTime     = 0L;

    private double                      minSpeedKPH         = MIN_SPEED_KPH;
    private long                        minStoppedTimeSec   = MIN_STOPPED_TIME_SEC;
    private boolean                     stopOnIgnitionOff   = STOP_ON_IGNITION_OFF;
    private boolean                     estimateFuelTotal   = ESTIMATE_FUEL_TOTAL;
    private double                      kilometersPerLiter  = KILOMETERS_PER_LITER;
    private boolean                     tripStartStopOnly   = TRIP_START_STOP_ONLY;

    private java.util.List<FieldData>   rowData             = null;
    
    private I18N                        i18n                = null;

    // ------------------------------------------------------------------------

    /**
    *** Trip Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public TripReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        this.i18n = reqState.getPrivateLabel().getI18N(TripReport.class);

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

        /* Trip type vars */
        this.minSpeedKPH        = this.getProperties().getDouble( PROP_minimumSpeedKPH   , MIN_SPEED_KPH);
        this.minStoppedTimeSec  = this.getProperties().getLong(   PROP_minimumStoppedTime, MIN_STOPPED_TIME_SEC);
        this.stopOnIgnitionOff  = this.getProperties().getBoolean(PROP_stopOnIgnitionOff , STOP_ON_IGNITION_OFF);
        this.estimateFuelTotal  = this.getProperties().getBoolean(PROP_estimateFuelTotal , ESTIMATE_FUEL_TOTAL);
        this.kilometersPerLiter = this.getProperties().getDouble( PROP_kilometersPerLiter, KILOMETERS_PER_LITER);
        this.tripStartStopOnly  = this.getProperties().getBoolean(PROP_tripStartStopOnly , TRIP_START_STOP_ONLY);

        /* debug */
        if (this.estimateFuelTotal) {
            Print.logInfo("Estimating Fuel Usage: default = " + this.kilometersPerLiter + " km/L");
        }
        if (this.tripStartStopOnly) {
            Print.logInfo("Trip detail will be omitted (per 'tripStartStopOnly')");
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
        this.rowData = new Vector<FieldData>();

        /* loop through devices */
        String accountID = this.getAccountID();
        String devID = "";
        ReportDeviceList devList = this.getReportDeviceList();
        for (Iterator i = devList.iterator(); i.hasNext();) {
            devID = (String)i.next();
            try {

                // get device
                Device device = devList.getDevice(devID);
                if (device == null) {
                    continue;
                }

                // trip init
                this.firstEvent      = null;
                this.tripStartType   = TRIP_ON_SPEED; // reset below
                this.tripStartEvent  = null;
                this.approxStart     = true;
                this.isMoving        = false; // TRIP_ON_SPEED only
                this.lastMovingTime  = 0L;
                this.lastStoppedTime = 0L;
                
                /* estimate fuel consumption? */
                this.devFuelEconomy  = 0.0;
                this.devEstFuelTotal = false;
                if (this.estimateFuelTotal) {
                    double fuelEcon = device.getFuelEconomy();
                    if (fuelEcon > 0.0) { // device has fuel economy
                        this.devFuelEconomy  = fuelEcon;
                        this.devEstFuelTotal = true;
                    } else
                    if (this.kilometersPerLiter > 0.0) { // default fuel economy
                        this.devFuelEconomy  = this.kilometersPerLiter;
                        this.devEstFuelTotal = true;
                    }
                }

                // Device ignition statusCodes
                this.ignitionCodes = device.getIgnitionStatusCodes();
                boolean hasIgnition = (this.ignitionCodes != null);
                
                // trip start/stop type
                String tt = this.getProperties().getString(PROP_tripStartType,MOTION_SPEED[0]).toLowerCase();
                //Print.logInfo("Trip type: " + tt);
                if (ListTools.contains(MOTION_DEFAULT,tt)) {
                    // "default" (TRIP_ON_ENGINE will not be selected when using "default")
                    String devCode = device.getDeviceCode();
                    DCServerConfig dcs = DCServerFactory.getServerConfig(devCode);
                    if ((dcs == null) && StringTools.isBlank(devCode) && Account.IsDemoAccount(accountID)) {
                        // special case for "demo" account when 'deviceCode' is blank
                        dcs = DCServerFactory.getServerConfig(DCServerFactory.OPENDMTP_NAME);
                        if (dcs == null) {
                            Print.logWarn("Account 'demo' DCServerConfig not found: " + DCServerFactory.OPENDMTP_NAME);
                        }
                    }
                    if (dcs != null) {
                        // DCServerConfig found
                        if (dcs.getStartStopSupported(false)) {
                            // Device supports start/stop
                            this.tripStartType = TRIP_ON_START;
                        } else
                        if (hasIgnition) {
                            // Device supports ignition state
                            this.tripStartType = TRIP_ON_IGNITION;
                        } else {
                            // Default to speed
                            this.tripStartType = TRIP_ON_SPEED;
                        }
                    } else {
                        // DCServerConfig not found ('deviceCode' is either blank or invalid)
                        if (hasIgnition) {
                            // Device supports ignition state
                            this.tripStartType = TRIP_ON_IGNITION;
                        } else {
                            // Default
                            this.tripStartType = TRIP_ON_SPEED;
                        }
                    }
                } else
                if (ListTools.contains(MOTION_STARTSTOP,tt)) {
                    // "startstop"
                    this.tripStartType = TRIP_ON_START;
                    //Print.logInfo("Trip delimiter: start/stop [ignition = " + hasIgnition + "]");
                } else
                if (ListTools.contains(MOTION_IGNITION,tt)/* && hasIgnition */) {
                    // "ignition"
                    this.tripStartType = TRIP_ON_IGNITION;
                    if (!hasIgnition) {
                         this.ignitionCodes = new int[] { StatusCodes.STATUS_IGNITION_OFF, StatusCodes.STATUS_IGNITION_ON };
                         hasIgnition = true;
                    }
                    //Print.logInfo("Trip delimiter: ignition");
                } else
                if (ListTools.contains(MOTION_ENGINE,tt)) {
                    // "engine"
                    this.tripStartType = TRIP_ON_ENGINE;
                    //Print.logInfo("Trip delimiter: engine");
                } else {
                    // "speed", "motion"
                    this.tripStartType = TRIP_ON_SPEED;
                    //Print.logInfo("Trip delimiter: speed");
                }

                // get events
                this.getEventData(device, this); // <== callback to 'handleDBRecord'

                // if we never found a 'tripStart' we need to clear 'this.rowData'
                if (this.approxStart) {
                    // we never found a tripStart'
                    this.rowData.clear();
                }
                
            } catch (DBException dbe) {
                Print.logError("Error retrieving EventData for Device: " + devID);
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

        /* trip delimiter */
        boolean tripStart = this.isTripStart(ev);
        boolean tripStop  = tripStart? false : this.isTripStop(ev);

        /* debug */
        /*
        Print.logInfo("Event: " + 
            rcd.getAccountID() + ", " +
            rcd.getDeviceID() + ", " +
            StatusCodes.ToString(rcd.getStatusCode()) + ", " +
            (new DateTime(rcd.getTimestamp())) + ", " +
            rcd.getGeoPoint()
            );
        */

        /* first event */
        if (this.firstEvent == null) {
            this.firstEvent = ev;
            // if we start in the middle of a trip, designate the first record as a start
            if (!tripStop && !tripStart) {
                this.tripStartEvent = ev;
                this.approxStart    = true; // we may be in the middle of a 'stop'
            }
            if (this.devEstFuelTotal) {
                if (this.firstEvent.getOdometerKM() <= 0.0) {
                    Print.logWarn("First trip event missing odometer: " + ev.getDeviceID());
                    this.devEstFuelTotal = false;
                } else 
                if (this.firstEvent.getFuelTotal() > ZERO_LITERS) {
                    Print.logWarn("First trip event already has fuel-total: " + ev.getDeviceID());
                    this.devEstFuelTotal = false;
                }
            }
        }

        /* estimate fuel total (skip first event) */
        if (this.devEstFuelTotal) { // "this.devFuelEconomy" is greater than 0.0
            if (ev.getFuelTotal() > ZERO_LITERS) {
                Print.logWarn("Skipping FuelTotal estimate: CurrentEvent already has a fuel-total");
            } else
            if (ev.getOdometerKM() <= 0.0) {
                Print.logInfo("Skipping FuelTotal estimate: CurrentEvent does not have an odometer value");
            } else {
                double km = ev.getOdometerKM() - this.firstEvent.getOdometerKM();
                double liters = (km * (1.0 / this.devFuelEconomy)); // km * L/km = L
                ev.setFuelTotal((liters > 0.0)? liters : ZERO_LITERS);  // non-zero
                Print.logInfo("Setting FuelTotal estimate: " + ev.getFuelTotal() + " Liters");
            }
        }

        /* trip start */
        if (tripStart) {
            if (this.tripStartEvent != null) {
                // already have a trip start event
                if (this.approxStart) {
                    // This 'start' isn't real anyway, we likely started in the middle of a 'stop'
                    // Remove everything up to this point from the Row Data list
                    Print.logInfo("Clearing existing row data ...");
                    this.rowData.clear();
                } else {
                    // two real 'start' events received without an interleaving 'stop'
                    this.rowData.add(new BlankRow()); // add blank row
                }
            }
            this.tripStartEvent = ev;
            this.approxStart    = false;
        }

        /* trip record */
        if (this.tripStartEvent != null) {
            // we have a trip 'start' for this event
            if (!this.tripStartStopOnly) {
                // save all events between start and stop
                Print.logInfo("Saving event row data ...");
                this.rowData.add(new TripDetail(this.tripStartEvent, ev, this.i18n));
            } else
            if (tripStart || tripStop) {
                // always save trip 'book-ends'
                Print.logInfo("Saving event start/stop row data ...");
                this.rowData.add(new TripDetail(this.tripStartEvent, ev, this.i18n));
            }
            // also end of trip?
            if (tripStop) {
                // end of trip add item to total
                Print.logInfo("Saving event end-of-trip ...");
                this.rowData.add(new TripTotal(this.tripStartEvent, ev, this.i18n));
                this.tripStartEvent = null;
            }
        }

        /* return record limit status */
        return (this.rowData.size() < this.getReportLimit())? DBRH_SKIP : DBRH_STOP;
        
    }
    
    private boolean isTripStart(EventData ev)
    {
        if (this.tripStartType == TRIP_ON_IGNITION) {
            int sc = ev.getStatusCode();
            if (sc == StatusCodes.STATUS_IGNITION_ON) {
                return true;
            } else
            if ((this.ignitionCodes != null) && (sc == this.ignitionCodes[1])) {
                return true;
            }
        } else
        if (this.tripStartType == TRIP_ON_ENGINE) {
            int sc = ev.getStatusCode();
            if (sc == StatusCodes.STATUS_ENGINE_START) {
                return true;
            }
        } else
        if (this.tripStartType == TRIP_ON_START) {
            int sc = ev.getStatusCode();
            if (sc == StatusCodes.STATUS_MOTION_START) {
                return true;
            }
        } else 
        if (this.tripStartType == TRIP_ON_SPEED) {
            if (ev.getSpeedKPH() >= this.minSpeedKPH) {
                this.lastStoppedTime = 0L;
                if (!this.isMoving) {
                    //Print.logInfo("TripStart: moving="+this.isMoving + ", speed="+ev.getSpeedKPH() + ", minSpeed="+this.minSpeedKPH);
                    this.isMoving = true;
                    this.lastMovingTime = ev.getTimestamp();
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isTripStop(EventData ev)
    {
        if (this.tripStartType == TRIP_ON_IGNITION) {
            int sc = ev.getStatusCode();
            if (sc == StatusCodes.STATUS_IGNITION_OFF) {
                return true;
            } else
            if ((this.ignitionCodes != null) && (sc == this.ignitionCodes[0])) {
                return true;
            }
        } else
        if (this.tripStartType == TRIP_ON_ENGINE) {
            int sc = ev.getStatusCode();
            if (sc == StatusCodes.STATUS_ENGINE_STOP) {
                return true;
            }
        } else
        if (this.tripStartType == TRIP_ON_START) {
            int sc = ev.getStatusCode();
            if (sc == StatusCodes.STATUS_MOTION_STOP) {
                return true;
            } else
            if (sc == StatusCodes.STATUS_IGNITION_OFF) {
                // "Stop" inferred by IgnitionOff
                return true;
            } else
            if (this.stopOnIgnitionOff && (this.ignitionCodes != null) && (sc == this.ignitionCodes[0])) {
                // "Stop" inferred by IgnitionOff
                return true;
            }
        } else
        if (this.tripStartType == TRIP_ON_SPEED) {
            //Print.logInfo("TripStop: moving="+this.isMoving + ", speed="+ev.getSpeedKPH() + ", minSpeed="+this.minSpeedKPH);
            if (this.isMoving) {
                int sc = ev.getStatusCode();
                if (this.stopOnIgnitionOff && (this.ignitionCodes != null) && (sc == this.ignitionCodes[0])) {
                    // "Stop" inferred by IgnitionOff
                    this.isMoving        = false;
                    this.lastMovingTime  = 0L;
                    this.lastStoppedTime = 0L;
                    return true;
                } else
                if (ev.getSpeedKPH() < this.minSpeedKPH) {
                    // we're stopped, check minimum stopped time
                    if (this.lastStoppedTime == 0L) {
                        this.lastStoppedTime = ev.getTimestamp();
                    } else {
                        long stoppedDeltaSec = ev.getTimestamp() - this.lastStoppedTime; // lastMovingTime;
                        if (stoppedDeltaSec >= this.minStoppedTimeSec) {
                            //Print.logInfo("TripStop: stopAge="+stoppedDeltaSec + ", minStopAge="+this.minStoppedTimeSec);
                            this.isMoving        = false;
                            this.lastMovingTime  = 0L;
                            this.lastStoppedTime = 0L;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Custom BlankRow class
    **/
    private static class BlankRow
        extends FieldData
    {
        public BlankRow() {
            super();
            this.setCssClass(ReportLayout.CSS_CLASS_BODY_TOTAL);
            this.setValue(FieldLayout.DATA_ADDRESS, "");
        }
    }

    /**
    *** Custom TripDetail class
    **/
    private static class TripDetail
        extends FieldData
    {
        public TripDetail(EventData startEv, EventData tripEv, I18N i18n) {
            super();
            // Account/Device
            Account account = tripEv.getAccount();
            Device  device  = tripEv.getDevice();
            this.setAccount(account);
            this.setDevice(device);
            // EventData fields
            this.setValue(FieldLayout.DATA_TIMESTAMP  , tripEv.getTimestamp());
            this.setValue(FieldLayout.DATA_STATUS_CODE, tripEv.getStatusCode());
            this.setValue(FieldLayout.DATA_LATITUDE   , tripEv.getLatitude());
            this.setValue(FieldLayout.DATA_LONGITUDE  , tripEv.getLongitude());
            this.setValue(FieldLayout.DATA_ALTITUDE   , tripEv.getAltitude());
            this.setValue(FieldLayout.DATA_SPEED      , tripEv.getSpeedKPH());
            this.setValue(FieldLayout.DATA_HEADING    , tripEv.getHeading());
            this.setValue(FieldLayout.DATA_ODOMETER   , tripEv.getOdometerKM());
            this.setValue(FieldLayout.DATA_ADDRESS    , tripEv.getAddress());
            // accumulator fields
            double odomDelta   = tripEv.getOdometerKM() - startEv.getOdometerKM();    // Kilometers
            long   driveElapse = tripEv.getTimestamp()  - startEv.getTimestamp();     // Seconds
            double fuelTotal   = tripEv.getFuelTotal();
            double fuelTrip    = 0.0;                                                 // Liters
            double fuelEcon    = 0.0;                                                 // km / L
            if (fuelTotal > 0.0) {
                // calculate trip fuel used/economy based on total fuel used
                fuelTrip       = fuelTotal - startEv.getFuelTotal();                  // Liters
                fuelEcon       = (fuelTrip > 0.0)? (odomDelta / fuelTrip) : 0.0;      // km / L
            } else
            if (device.getFuelEconomy() > 0.0) {
                // calculate trip fuel used based on device's estimated economy
                fuelEcon       = device.getFuelEconomy();                             // km / L
                fuelTrip       = odomDelta / fuelEcon;                                // Liters
            }
            this.setValue(FieldLayout.DATA_FUEL_TOTAL     , fuelTotal);
            this.setValue(FieldLayout.DATA_FUEL_TRIP      , ((fuelTrip > 0.0)? fuelTrip : 0.0001));
            this.setValue(FieldLayout.DATA_ODOMETER_DELTA , odomDelta);
            this.setValue(FieldLayout.DATA_FUEL_ECONOMY   , fuelEcon);
            this.setValue(FieldLayout.DATA_DRIVING_ELAPSED, driveElapse);
        }
    }

    /**
    *** Custom TripTotal class
    **/
    private static class TripTotal
        extends FieldData
    {
        public TripTotal(EventData startEv, EventData stopEv, I18N i18n) {
            super();
            this.setRowType(DBDataRow.RowType.SUBTOTAL);
            this.setCssClass(ReportLayout.CSS_CLASS_BODY_TOTAL);
            if ((startEv != null) && (stopEv != null)) {
                // Trip time/distance
                Device device      = stopEv.getDevice();
                long   driveElapse = stopEv.getTimestamp()  - startEv.getTimestamp();     // Seconds
                double odomDelta   = stopEv.getOdometerKM() - startEv.getOdometerKM();    // Kilometers
                double fuelTotal   = stopEv.getFuelTotal();
                double fuelTrip    = 0.0;                                                 // Liters
                double fuelEcon    = 0.0;                                                 // km / L
                if (fuelTotal > 0.0) {
                    // calculate trip fuel used/economy based on total fuel used
                    fuelTrip       = fuelTotal - startEv.getFuelTotal();                  // Liters
                    fuelEcon       = (fuelTrip > 0.0)? (odomDelta / fuelTrip) : 0.0;      // km / L
                } else
                if (device.getFuelEconomy() > 0.0) {
                    // calculate trip fuel used based on device's estimated economy
                    fuelEcon       = device.getFuelEconomy();                             // km / L
                    fuelTrip       = odomDelta / fuelEcon;                                // Liters
                }
                this.setValue(FieldLayout.DATA_FUEL_TOTAL     , fuelTotal);
                this.setValue(FieldLayout.DATA_FUEL_TRIP      , ((fuelTrip > 0.0)? fuelTrip : 0.0001));
                this.setValue(FieldLayout.DATA_ODOMETER_DELTA , odomDelta);
                this.setValue(FieldLayout.DATA_FUEL_ECONOMY   , fuelEcon);
                this.setValue(FieldLayout.DATA_DRIVING_ELAPSED, driveElapse);
                this.setValue(FieldLayout.DATA_ADDRESS        , i18n.getString("TripReport.tripTimeDistance","Trip Time/Distance"));
            } else {
                // blank row
                this.setValue(FieldLayout.DATA_ADDRESS, "");
            }
        }
    }
        
}
