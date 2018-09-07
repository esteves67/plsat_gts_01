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
package org.opengts.rulewar.report.field;

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

import org.opengts.rule.tables.*;

public class FuelRegisterReport
    extends ReportData
{

    // ------------------------------------------------------------------------

    private java.util.List<FieldData>   rowData = null;

    // ------------------------------------------------------------------------

    /**
    *** Device Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public FuelRegisterReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);

        /* Account check */
        if (this.getAccount() == null) {
            throw new ReportException("Account-ID not specified");
        }

        /* Device check */
        int deviceCount = this.getDeviceCount();
        if (deviceCount < 1) {
            throw new ReportException("At least 1 Device must be specified");
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
        Account account   = this.getAccount();
        String  accountID = this.getAccountID();
        long startTime    = this.getTimeStart();
        long endTime      = this.getTimeEnd();

        /* init */
        this.rowData = new Vector<FieldData>();

        /* loop through devices */
        String deviceID = "";
        ReportDeviceList devList = this.getReportDeviceList();
        for (Iterator i = devList.iterator(); i.hasNext();) {
            
            /* get Device */
            deviceID = (String)i.next();
            final Device device;
            try {
                device = devList.getDevice(deviceID);
                if (device == null) {
                    Print.logError("Returned DeviceList 'Device' is null: " + deviceID);
                    continue;
                }
            } catch (DBException dbe) {
                Print.logError("Error retrieving EventData count for Device: " + deviceID);
                continue;
            }

            /* record handler */
            DBRecordHandler<FuelRegister> rcdHandler = new DBRecordHandler<FuelRegister>() {
                public int handleDBRecord(FuelRegister rcd) throws DBException {
                    FieldData fd = new FieldData();
                    fd.setDevice(device);
                    fd.setLong(  FieldLayout.DATA_TIMESTAMP  , rcd.getTimestamp());
                    fd.setInt(   FieldLayout.DATA_STATUS_CODE, rcd.getStatusCode());
                    fd.setDouble(FieldLayout.DATA_LATITUDE   , rcd.getLatitude());
                    fd.setDouble(FieldLayout.DATA_LONGITUDE  , rcd.getLongitude());
                    fd.setDouble(FieldLayout.DATA_FUEL_LEVEL , rcd.getFuelLevel());
                    fd.setDouble(FieldLayout.DATA_FUEL_TOTAL , rcd.getFuelTotal());
                    fd.setDouble(FieldLayout.DATA_ODOMETER   , rcd.getOdometerKM());
                    fd.setString(FieldLayout.DATA_SUBDIVISION, rcd.getSubdivision());
                    FuelRegisterReport.this.rowData.add(fd);
                    return DBRH_SKIP;
                }
            };

            /* record handler callback */
            try {
                FuelRegister.getRecordCallback(
                    accountID, deviceID,
                    startTime, endTime,
                    rcdHandler);
            } catch (DBException dbe) {
                Print.logException("Error", dbe);
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
