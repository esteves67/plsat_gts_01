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
package org.opengts.extra.war.report.entity;

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

import org.opengts.extra.tables.*;

public class EntityReport
    extends ReportData
    implements DBRecordHandler<Entity>
{

    // ------------------------------------------------------------------------
    // Summary report
    // ------------------------------------------------------------------------
    // Columns:
    //   index timestamp entityId entityDesc geoPoint/latitude/longitude deviceID odometer address attached
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
        
    private java.util.List<FieldData>   rowData             = null;
    
    private Map<String,EventData>       lastDeviceEvents    = new HashMap<String,EventData>();

    // ------------------------------------------------------------------------

    public EntityReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        if (this.getAccount() == null) {
            throw new ReportException("Account must be specified");
        }
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
        this.rowData = new Vector<FieldData>();
        String acctID = this.getAccountID();
        long limit = 0L; // no limit

        /* get Entities */
        try {
            Entity.getEntitiesForAccount(acctID, limit, this);
        } catch (DBException dbe) {
            Print.logException("Getting Entities ...", dbe);
        }

        /* return data iterator */
        return new ListDataIterator(this.rowData);
    }

    /* return the totals data list */
    public DBDataIterator getTotalsDataIterator()
    {
        return null;
    }

    // ------------------------------------------------------------------------

    private EventData getLastDeviceEvent(String devID, long endTime)
    {
        if (devID != null) {
            try {
                EventData event = this.lastDeviceEvents.get(devID);
                if (event == null) {
                    Device device = Device.getDevice(this.getAccount(), devID);
                    if (device != null) {
                        event = device.getLastEvent(endTime, true);
                        if (event != null) {
                            this.lastDeviceEvents.put(devID, event);
                        }
                    }
                }
                return event; // may still be null
            } catch (Throwable th) { // DBException
                Print.logException("Error retrieving last EventData for Device: " + devID, th);
            }
        }
        return null;
    }
    
    public int handleDBRecord(Entity rcd)
        throws DBException
    {
        Entity    ent = rcd;
        FieldData fd  = new FieldData();
        boolean   isAttached = ent.getIsAttached();
        String    entDev     = isAttached? ent.getDeviceID() : "";
        EventData evt        = isAttached? this.getLastDeviceEvent(entDev,-1L) : null;
        long      entTime    = ent.getTimestamp();

        /* fill data */
        fd.setString(  FieldLayout.DATA_ENTITY_ID       , ent.getEntityID());
        fd.setDouble(  FieldLayout.DATA_ODOMETER        , ent.getOdometerKM()); // entity is not associated with a single device
        fd.setString(  FieldLayout.DATA_ENTITY_DESC     , ent.getDescription());
        fd.setBoolean( FieldLayout.DATA_ATTACHED        , isAttached);
        fd.setString(  FieldLayout.DATA_DEVICE_ID       , entDev);
        if ((evt != null) && (evt.getTimestamp() >= entTime)) {
            // use the last location of the Device towing the trailer
            fd.setLong(    FieldLayout.DATA_TIMESTAMP   , evt.getTimestamp());
            fd.setGeoPoint(FieldLayout.DATA_GEOPOINT    , evt.getGeoPoint());
            fd.setString(  FieldLayout.DATA_ADDRESS     , evt.getAddress());
        } else {
            // use the last "drop" location
            fd.setLong(    FieldLayout.DATA_TIMESTAMP   , entTime);
            fd.setGeoPoint(FieldLayout.DATA_GEOPOINT    , ent.getGeoPoint());
            fd.setString(  FieldLayout.DATA_ADDRESS     , ent.getAddress());
        }

        /* add record to list */
        this.rowData.add(fd);

        /* return */
        return (this.rowData.size() < this.getReportLimit())? DBRH_SKIP : DBRH_STOP;
    }

    // ------------------------------------------------------------------------

}
