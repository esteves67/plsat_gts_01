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

public class DeviceReport
    extends ReportData
{

    // ------------------------------------------------------------------------
    // Properties

    private static final String PROP_includeDeltaValues     = "includeDeltaValues";

    // ------------------------------------------------------------------------
    // Summary report
    // 1 'maintenance' record per device
    // ------------------------------------------------------------------------

    private java.util.List<FieldData>   rowData             = null;
    
    private boolean                     includeDeltaValues  = false;

    // ------------------------------------------------------------------------

    /**
    *** Device Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public DeviceReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        if (this.getAccount() == null) {
            throw new ReportException("Account-ID not specified");
        }
        //if (this.getDeviceCount() < 1) {
        //    throw new ReportException("At least 1 Device must be specified");
        //}
        // report on all authorized devices
        //this.getReportDeviceList().addAllAuthorizedDevices();
        this.includeDeltaValues = this.getProperties().getBoolean(PROP_includeDeltaValues, false);
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
        String devID = "";
        ReportDeviceList devList = this.getReportDeviceList();
        for (Iterator i = devList.iterator(); i.hasNext();) {
            devID = (String)i.next();
            try {
                Device device = devList.getDevice(devID);
                if (device != null) {
                    FieldData fd = new FieldData();
                    fd.setDevice(device);
                    this.rowData.add(fd);
                } else {
                    // should never occur
                    Print.logError("Returned DeviceList 'Device' is null: " + devID);
                }
            } catch (DBException dbe) {
                Print.logError("Error retrieving EventData count for Device: " + devID);
            }
        }

        /* add delta values? */
        if (this.includeDeltaValues) {
            long startTime = this.getTimeStart();
            long endTime   = this.getTimeEnd();
            for (FieldData fd : this.rowData) {
                Device dev = fd.getDevice(); // should not be null
                double startHours = 0.0;
                double stopHours  = 0.0;
                double startOdom  = 0.0;
                double stopOdom   = 0.0;
                double startFuel  = 0.0;
                double stopFuel   = 0.0;

                /* first event following startTime containing a valid odometer/fuel */
                try {
                    EventData ed[] = dev.getRangeEvents(
                        startTime, -1L, 
                        false, // validGPS?
                        EventData.LimitType.FIRST, 4);
                    if (!ListTools.isEmpty(ed)) {
                        for (int e = 0; e < ed.length; e++) {
                            // Hours
                            if (startHours <= 0.0) {
                                double hours = ed[e].getEngineHours(); // hours
                                if (hours > 0.0) {
                                    startHours = hours;
                                }
                            }
                            // Odometer
                            if (startOdom <= 0.0) {
                                double odom = ed[e].getOdometerKM(); // kilometers
                                if (odom > 0.0) {
                                    startOdom = odom;
                                }
                            }
                            // Fuel
                            if (startFuel <= 0.0) {
                                double fuel = ed[e].getFuelTotal(); // Liters
                                if (fuel > 0.0) {
                                    startFuel = fuel;
                                }
                            }
                            // break?
                            if ((startHours > 0.0) && (startOdom > 0.0) && (startFuel > 0.0)) {
                                break;
                            }
                        }
                    }
                } catch (DBException dbe) {
                    Print.logException("Getting FIRST Device Event Records", dbe);
                }

                /* last event prior to endTime containing a valid odometer */
                if (endTime <= 0L) {
                    // end of time
                    stopHours = dev.getLastEngineHours();
                    stopOdom  = dev.getLastOdometerKM();
                    stopFuel  = dev.getLastFuelTotal();
                } else {
                    try {
                        EventData ed[] = dev.getRangeEvents(
                            -1L, endTime, 
                            false, // validGPS?
                            EventData.LimitType.LAST, 4);
                        if (!ListTools.isEmpty(ed)) {
                            for (int e = ed.length - 1; e > 0; e--) {
                                // Hours
                                if (stopHours <= 0.0) {
                                    double hours = ed[e].getEngineHours(); // hours
                                    if (hours > 0.0) {
                                        stopHours = hours;
                                    }
                                }
                                // Odometer
                                if (stopOdom <= 0.0) {
                                    double odom = ed[e].getOdometerKM(); // kilometers
                                    if (odom > 0.0) {
                                        stopOdom = odom;
                                    }
                                }
                                // Fuel
                                if (stopFuel <= 0.0) {
                                    double fuel = ed[e].getFuelTotal(); // Liters
                                    if (fuel > 0.0) {
                                        stopFuel = fuel;
                                    }
                                }
                                // break?
                                if ((stopHours > 0.0) && (stopOdom > 0.0) && (stopFuel > 0.0)) {
                                    break;
                                }
                            }
                        }
                    } catch (DBException dbe) {
                        Print.logException("Getting FIRST Device Event Records", dbe);
                    }
                }

                /* save engine-hour values */
                if (startHours > 0) {
                    fd.setDouble(FieldLayout.DATA_START_HOURS, startHours);
                }
                if (stopHours > 0) {
                    fd.setDouble(FieldLayout.DATA_START_HOURS, stopHours);
                }
                if ((startHours > 0.0) && (stopHours > 0.0)) {
                    double deltaHours = stopHours - startHours; // hours
                    if (deltaHours < 0.0) { deltaHours = 0.0; }
                    //fd.setDouble(FieldLayout.DATA_HOURS_DELTA, deltaHours);
                }

                /* save odometer values */
                if (startOdom > 0.0) {
                    fd.setDouble(FieldLayout.DATA_START_ODOMETER, startOdom);
                }
                if (stopOdom > 0.0) {
                    fd.setDouble(FieldLayout.DATA_STOP_ODOMETER, stopOdom);
                }
                if ((startOdom > 0.0) && (stopOdom > 0.0)) {
                    double deltaOdom = stopOdom - startOdom; // kilometers
                    if (deltaOdom < 0.0) { deltaOdom = 0.0; }
                    fd.setDouble(FieldLayout.DATA_ODOMETER_DELTA, deltaOdom);
                    fd.setDouble(FieldLayout.DATA_DISTANCE      , deltaOdom);
                }

                /* planned distance */
                // If DATA_PLAN_DISTANCE is left undefined, FieldLayout will pull
                // the value from "dev.getPlanDistanceKM()" by default.
                double planDelta = dev.getPlanDistanceKM();
                fd.setDouble(FieldLayout.DATA_PLAN_DISTANCE     , planDelta);

                /* save fuel values */
                if (startFuel > 0.0) {
                    fd.setDouble(FieldLayout.DATA_START_FUEL, startFuel);
                }
                if (stopFuel > 0.0) {
                    fd.setDouble(FieldLayout.DATA_STOP_FUEL, stopFuel);
                }
                if ((startFuel > 0.0) && (stopFuel > 0.0)) {
                    double deltaFuel = stopFuel - startFuel; // Liters
                    fd.setDouble(FieldLayout.DATA_FUEL_DELTA, deltaFuel);
                }

            }
        }

        /* return data iterator */
        FieldData.sortByDeviceDescription(this.rowData);
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

}
