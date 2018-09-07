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

public class FuelSummaryReport
    extends ReportData
{

    // ------------------------------------------------------------------------
    // Summary report
    // 1 'count' record per device
    // ------------------------------------------------------------------------

    private java.util.List<FieldData>   rowData        = null;
    private java.util.List<FieldData>   totData        = null;
    private int                         deviceCount    = 0;
    private double                      totEngineHours = 0.0;    // engineHours
    private double                      totFuelTotal   = 0.0;    // fuelTotal
    private double                      totIdleHours   = 0.0;    // idleHours
    private double                      totFuelIdle    = 0.0;    // fuelIdle
    private double                      totWorkHours   = 0.0;    // 
    private double                      totFuelWork    = 0.0;    // 
    private double                      totPTOHours    = 0.0;    // ptoHours
    private double                      totFuelPTO     = 0.0;    // fuelPTO
    private double                      totOdometerKM  = 0.0;    // odometerKM

    // ------------------------------------------------------------------------

    /**
    *** Fuel Summary Report Constructor
    *** @param rptEntry The ReportEntry
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public FuelSummaryReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        if (this.getAccount() == null) {
            throw new ReportException("Account-ID not specified");
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Post report initialization
    **/
    public void postInitialize()
    {
        //
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
        this.rowData        = new Vector<FieldData>();
        this.deviceCount    = 0;
        this.totEngineHours = 0.0;    // engineHours
        this.totFuelTotal   = 0.0;    // fuelTotal
        this.totIdleHours   = 0.0;    // idleHours
        this.totFuelIdle    = 0.0;    // fuelIdle
        this.totWorkHours   = 0.0;    // 
        this.totFuelWork    = 0.0;    // 
        this.totPTOHours    = 0.0;    // ptoHours
        this.totFuelPTO     = 0.0;    // fuelPTO
        this.totOdometerKM  = 0.0;    // odometerKM

        /* report date range */
        long startTime = this.getTimeStart();
        long endTime   = this.getTimeEnd();
        long limit     = 10L;

        /* loop through devices */
        String devID = "";
        ReportDeviceList devList = this.getReportDeviceList();
        for (Iterator i = devList.iterator(); i.hasNext();) {
            devID = (String)i.next();
            try {

                /* get device */
                Device device = devList.getDevice(devID);
                if (device == null) {
                    // should never occur
                    Print.logError("Returned DeviceList 'Device' is null: " + devID);
                    continue;
                }
                String accountID = device.getAccountID();
                String deviceID  = device.getDeviceID();

                // Starting values
                double strEngineHours = 0.0;    // engineHours
                double strFuelTotal   = 0.0;    // fuelTotal
                double strIdleHours   = 0.0;    // idleHours
                double strFuelIdle    = 0.0;    // fuelIdle
                double strPTOHours    = 0.0;    // ptoHours
                double strFuelPTO     = 0.0;    // fuelPTO
                double strOdometerKM  = 0.0;    // odometerKM
                try {
                    EventData eda[] = EventData.getRangeEvents(
                        accountID, deviceID,
                        this.getTimeStart(), -1L/*endTime*/,
                        null/*statusCodes*/,
                        true/*validGPS*/,
                        EventData.LimitType.FIRST, limit, true/*ascending*/,
                        null/*addtnlSelect*/,
                        null/*rcdHandler*/);
                    if (!ListTools.isEmpty(eda)) {
                        for (EventData ed : eda) {
                            if (strEngineHours <= 0.0) { strEngineHours = ed.getEngineHours(); }
                            if (strFuelTotal   <= 0.0) { strFuelTotal   = ed.getFuelTotal();   }
                            if (strIdleHours   <= 0.0) { strIdleHours   = ed.getIdleHours();   }
                            if (strFuelIdle    <= 0.0) { strFuelIdle    = ed.getFuelIdle();    }
                            if (strPTOHours    <= 0.0) { strPTOHours    = ed.getPtoHours();    }
                            if (strFuelPTO     <= 0.0) { strFuelPTO     = ed.getFuelPTO();     }
                            if (strOdometerKM  <= 0.0) { strOdometerKM  = ed.getOdometerKM();  }
                        }
                    }
                    /*
                    Print.logInfo("Starting Values: " + accountID + "/" + deviceID);
                    Print.logInfo("  strEngineHours  : " + strEngineHours);
                    Print.logInfo("  strFuelTotal    : " + strFuelTotal);
                    Print.logInfo("  strIdleHours    : " + strIdleHours);
                    Print.logInfo("  strFuelIdle     : " + strFuelIdle);
                    Print.logInfo("  strPTOHours     : " + strPTOHours);
                    Print.logInfo("  strFuelPTO      : " + strFuelPTO);
                    Print.logInfo("  strOdometerKM   : " + strOdometerKM);
                    */
                } catch (DBException dbe) {
                    Print.logException("Unable to obtain EventData records", dbe);
                }
                    
                // Ending values
                double endEngineHours = 0.0;
                double endFuelTotal   = 0.0;
                double endIdleHours   = 0.0;
                double endFuelIdle    = 0.0;
                double endPTOHours    = 0.0;
                double endFuelPTO     = 0.0;
                double endOdometerKM  = 0.0;
                try {
                    EventData eda[] = EventData.getRangeEvents(
                        accountID, deviceID,
                        this.getTimeStart(), -1L/*endTime*/,
                        null/*statusCodes*/,
                        true/*validGPS*/,
                        EventData.LimitType.LAST, limit, false/*ascending*/,
                        null/*addtnlSelect*/,
                        null/*rcdHandler*/);
                    if (!ListTools.isEmpty(eda)) {
                        for (EventData ed : eda) {
                            if (endEngineHours <= 0.0) { endEngineHours = ed.getEngineHours(); }
                            if (endFuelTotal   <= 0.0) { endFuelTotal   = ed.getFuelTotal();   }
                            if (endIdleHours   <= 0.0) { endIdleHours   = ed.getIdleHours();   }
                            if (endFuelIdle    <= 0.0) { endFuelIdle    = ed.getFuelIdle();    }
                            if (endPTOHours    <= 0.0) { endPTOHours    = ed.getPtoHours();    }
                            if (endFuelPTO     <= 0.0) { endFuelPTO     = ed.getFuelPTO();     }
                            if (endOdometerKM  <= 0.0) { endOdometerKM  = ed.getOdometerKM();  }
                        }
                    }
                    /*
                    Print.logInfo("Ending Values: " + accountID + "/" + deviceID);
                    Print.logInfo("  endEngineHours  : " + endEngineHours);
                    Print.logInfo("  endFuelTotal    : " + endFuelTotal);
                    Print.logInfo("  endIdleHours    : " + endIdleHours);
                    Print.logInfo("  endFuelIdle     : " + endFuelIdle);
                    Print.logInfo("  endPTOHours     : " + endPTOHours);
                    Print.logInfo("  endFuelPTO      : " + endFuelPTO);
                    Print.logInfo("  endOdometerKM   : " + endOdometerKM);
                    */
                } catch (DBException dbe) {
                    Print.logException("Unable to obtain EventData records", dbe);
                }

                // Report Fields
                double useEngineHours = endEngineHours - strEngineHours;
                double useFuelTotal   = endFuelTotal   - strFuelTotal;
                double useIdleHours   = endIdleHours   - strIdleHours;
                double useFuelIdle    = endFuelIdle    - strFuelIdle;
                double useWorkHours   = useEngineHours - useIdleHours;
                double useFuelWork    = useFuelTotal   - useFuelIdle;
                double usePTOHours    = endPTOHours    - strPTOHours;
                double useFuelPTO     = endFuelPTO     - strFuelPTO;
                double useOdometerKM  = endOdometerKM  - strOdometerKM;
                double useFuelEconomy = (useFuelTotal > 0.0)? (useOdometerKM / useFuelTotal) : 0.0;
                /*
                Print.logInfo("Used Values: " + accountID + "/" + deviceID);
                Print.logInfo("  useEngineHours  : " + useEngineHours);
                Print.logInfo("  useFuelTotal    : " + useFuelTotal);
                Print.logInfo("  useIdleHours    : " + useIdleHours);
                Print.logInfo("  useFuelIdle     : " + useFuelIdle);
                Print.logInfo("  useWorkHours    : " + useWorkHours);
                Print.logInfo("  useFuelWork     : " + useFuelWork);
                Print.logInfo("  usePTOHours     : " + usePTOHours);
                Print.logInfo("  useFuelPTO      : " + useFuelPTO);
                Print.logInfo("  useOdometerKM   : " + useOdometerKM);
                Print.logInfo("  useFuelEconomy  : " + useFuelEconomy);
                */
                FieldData fd = new FieldData();
                fd.setDevice(device);
                fd.setDouble(FieldLayout.DATA_ENGINE_HOURS      , useEngineHours);
                fd.setDouble(FieldLayout.DATA_FUEL_TOTAL        , useFuelTotal);
                fd.setDouble(FieldLayout.DATA_IDLE_HOURS        , useIdleHours);
                fd.setDouble(FieldLayout.DATA_FUEL_IDLE         , useFuelIdle);
                fd.setDouble(FieldLayout.DATA_WORK_HOURS        , useWorkHours);
                fd.setDouble(FieldLayout.DATA_FUEL_WORK         , useFuelWork);
                fd.setDouble(FieldLayout.DATA_PTO_HOURS         , usePTOHours);
                fd.setDouble(FieldLayout.DATA_FUEL_PTO          , useFuelPTO);
                fd.setDouble(FieldLayout.DATA_ODOMETER_DELTA    , useOdometerKM);
                fd.setDouble(FieldLayout.DATA_FUEL_ECONOMY      , useFuelEconomy);
                this.rowData.add(fd);
                this.deviceCount++;

                // save totals
                this.totEngineHours += useEngineHours;
                this.totFuelTotal   += useFuelTotal;
                this.totIdleHours   += useIdleHours;
                this.totFuelIdle    += useFuelIdle;
                this.totWorkHours   += useWorkHours;
                this.totFuelWork    += useFuelWork;
                this.totPTOHours    += usePTOHours;
                this.totFuelPTO     += useFuelPTO;
                this.totOdometerKM  += useOdometerKM;

            } catch (DBException dbe) {
                Print.logError("Error generating report for Device: " + devID);
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
        
        /* no devices? */
        if (this.deviceCount <= 0) {
            return null;
        }

        /* init */
        this.totData = new Vector<FieldData>();
        String devTitles[] = this.getRequestProperties().getDeviceTitles();
        I18N i18n = this.getPrivateLabel().getI18N(FuelSummaryReport.class);

        /* totals */
        FieldData fdTot = new FieldData();
        fdTot.setRowType(DBDataRow.RowType.TOTAL);
        fdTot.setString(FieldLayout.DATA_DEVICE_DESC       , i18n.getString("FuelSummaryReport.total","Total",devTitles));
        fdTot.setDouble(FieldLayout.DATA_ENGINE_HOURS      , this.totEngineHours);
        fdTot.setDouble(FieldLayout.DATA_FUEL_TOTAL        , this.totFuelTotal);
        fdTot.setDouble(FieldLayout.DATA_IDLE_HOURS        , this.totIdleHours);
        fdTot.setDouble(FieldLayout.DATA_FUEL_IDLE         , this.totFuelIdle);
        fdTot.setDouble(FieldLayout.DATA_WORK_HOURS        , this.totWorkHours);
        fdTot.setDouble(FieldLayout.DATA_FUEL_WORK         , this.totFuelWork);
        fdTot.setDouble(FieldLayout.DATA_PTO_HOURS         , this.totPTOHours);
        fdTot.setDouble(FieldLayout.DATA_FUEL_PTO          , this.totFuelPTO);
        fdTot.setDouble(FieldLayout.DATA_ODOMETER_DELTA    , this.totOdometerKM);
        this.totData.add(fdTot);

        /* average */
        double avgEngineHours = this.totEngineHours / (double)this.deviceCount;
        double avgFuelTotal   = this.totFuelTotal   / (double)this.deviceCount;
        double avgIdleHours   = this.totIdleHours   / (double)this.deviceCount;
        double avgFuelIdle    = this.totFuelIdle    / (double)this.deviceCount;
        double avgWorkHours   = this.totWorkHours   / (double)this.deviceCount;
        double avgFuelWork    = this.totFuelWork    / (double)this.deviceCount;
        double avgPTOHours    = this.totPTOHours    / (double)this.deviceCount;
        double avgFuelPTO     = this.totFuelPTO     / (double)this.deviceCount;
        double avgOdometerKM  = this.totOdometerKM  / (double)this.deviceCount;
        double avgFuelEconomy = (this.totFuelTotal > 0.0)? (this.totOdometerKM / this.totFuelTotal) : 0.0;
        FieldData fdAvg = new FieldData();
        fdAvg.setRowType(DBDataRow.RowType.TOTAL);
        fdAvg.setString(FieldLayout.DATA_DEVICE_DESC       , i18n.getString("FuelSummaryReport.average","Average/{0}",devTitles));
        fdAvg.setDouble(FieldLayout.DATA_ENGINE_HOURS      , avgEngineHours);
        fdAvg.setDouble(FieldLayout.DATA_FUEL_TOTAL        , avgFuelTotal);
        fdAvg.setDouble(FieldLayout.DATA_IDLE_HOURS        , avgIdleHours);
        fdAvg.setDouble(FieldLayout.DATA_FUEL_IDLE         , avgFuelIdle);
        fdAvg.setDouble(FieldLayout.DATA_WORK_HOURS        , avgWorkHours);
        fdAvg.setDouble(FieldLayout.DATA_FUEL_WORK         , avgFuelWork);
        fdAvg.setDouble(FieldLayout.DATA_PTO_HOURS         , avgPTOHours);
        fdAvg.setDouble(FieldLayout.DATA_FUEL_PTO          , avgFuelPTO);
        fdAvg.setDouble(FieldLayout.DATA_ODOMETER_DELTA    , avgOdometerKM);
        fdAvg.setDouble(FieldLayout.DATA_FUEL_ECONOMY      , avgFuelEconomy);
        this.totData.add(fdAvg);

        /* return totals */
        return new ListDataIterator(this.totData);
        
    }


}
