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
package org.opengts.extra.war.report.antx;

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

public class AntxDayDetailReport
    extends ReportData
{

    // ------------------------------------------------------------------------
    // Detail report
    // ------------------------------------------------------------------------
    // Columns:
    //   index timestamp dateTime deviceID/deviceDesc fieldID/fieldDesc channelID/Desc type data0/1/2/3/4
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
        
    private java.util.List<AntxData>    rowData             = null;
    
    // ------------------------------------------------------------------------

    public AntxDayDetailReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
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

    /* return ReportLayout singleton instance */
    public ReportLayout getReportLayout()
    {
        // bind the report format to this data
        return AntxLayout.getReportLayout();
    }

    // ------------------------------------------------------------------------

    /* return an object that will iterate through the selected row object */
    public DBDataIterator getBodyDataIterator()
    {

        /* init */
        this.rowData = new Vector<AntxData>();

        /* report constraints */
        ReportConstraints rc = this.getReportConstraints();
        long startTime = rc.getTimeStart();
        long endTime   = rc.getTimeEnd();
        long limit     = rc.getSelectionLimit();

        /* account/device information */
        Account currAcct   = this.getAccount();
        String  currAcctID = currAcct.getAccountID();
        ReportDeviceList devList = this.getReportDeviceList();

        /* record handler */
        DBRecordHandler<Antx> rcdHandler = new DBRecordHandler<Antx>() {
            public int handleDBRecord(Antx a) throws DBException {

                /* save record */
                Print.logInfo("Processing record ...");
                AntxData fd = new AntxData();
                fd.setDevice(a.getDevice());
                int chan = a.getChannelID();
                int type = a.getType();
                
                /* header */
                fd.setValue(AntxLayout.DATA_DEVICE_ID       , a.getDeviceID());
                fd.setValue(AntxLayout.DATA_TIMESTAMP       , a.getTimestamp());
                fd.setValue(AntxLayout.DATA_FIELD_ID        , a.getFieldID());
                fd.setValue(AntxLayout.DATA_CHANNEL_ID      , chan);
                fd.setValue(AntxLayout.DATA_TYPE            , type);
                
                /* data */
                fd.setValue(AntxLayout.DATA_DATA_0          , a.getData0());
                fd.setValue(AntxLayout.DATA_DATA_1          , a.getData1());
                fd.setValue(AntxLayout.DATA_DATA_2          , a.getData2());
                fd.setValue(AntxLayout.DATA_DATA_3          , a.getData3());
                fd.setValue(AntxLayout.DATA_DATA_4          , a.getData4());
                
                /* specific data types */
                if (type == Antx.TYPE_VALUE) {
                    AntxChannel af = AntxChannel.GetAntxChannel(chan);
                    int afUnits = (af != null)? af.getUnits() : AntxChannel.UNITS_NONE;
                    if (afUnits == AntxChannel.UNITS_SPEED) {
                        fd.setValue(AntxLayout.DATA_SPEED, a.getData0());
                    } else
                    if (afUnits == AntxChannel.UNITS_DISTANCE) {
                        fd.setValue(AntxLayout.DATA_DISTANCE, a.getData0());
                    } else
                    if (afUnits == AntxChannel.UNITS_FUEL_USED) {
                        fd.setValue(AntxLayout.DATA_FUEL_TOTAL, a.getData0());
                    }
                }

                /* add */
                AntxDayDetailReport.this.rowData.add(fd);

                return DBRH_SKIP;
            }
        };

        /* iterate through records */
        for (Iterator i = devList.iterator(); i.hasNext();) {
            String deviceID = (String)i.next();
            try {
                Antx.getRecordCallback(rcdHandler, currAcctID, deviceID, startTime, endTime, limit);
            } catch (DBException dbe) {
                Print.logException("Error", dbe);
            }
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

}
