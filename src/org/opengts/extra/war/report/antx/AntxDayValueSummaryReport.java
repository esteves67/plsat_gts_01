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

public class AntxDayValueSummaryReport
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

    public AntxDayValueSummaryReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
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
        final Account currAcct   = this.getAccount();
        final String  currAcctID = currAcct.getAccountID();
        ReportDeviceList devList = this.getReportDeviceList();

        /* accumulator */
        final Map<String,AntxData> summaryMap = new HashMap<String,AntxData>();

        /* record handler */
        DBRecordHandler<Antx> rcdHandler = new DBRecordHandler<Antx>() {
            public int handleDBRecord(Antx a) throws DBException {
                String devID = a.getDeviceID();
                int     chan = a.getChannelID();
                int     type = a.getType();
                long      ts = a.getTimestamp();
                long     day = DateTime.getDayNumberFromDate(new DateTime(ts,currAcct.getTimeZone(null)));

                /* not a "value" record? */
                if (type != Antx.TYPE_VALUE) {
                    return DBRH_SKIP;
                }

                /* save record */
                String summKey = devID + "|" + day;
                AntxData ad = summaryMap.get(summKey);
                if (ad == null) {
                    ad = new AntxData();
                    ad.setValue(AntxLayout.DATA_DEVICE_ID, devID);
                    ad.setDevice(a.getDevice());
                    ad.setValue(AntxLayout.DATA_TYPE, type);
                    ad.setValue(AntxLayout.DATA_TIMESTAMP, ts);
                    summaryMap.put(summKey, ad);
                    AntxDayValueSummaryReport.this.rowData.add(ad);
                }

                /* specific data types */
                if (chan == AntxChannel.CHAN_FUEL_BY_DAY) {
                    ad.setValue(AntxLayout.DATA_FUEL_TOTAL, a.getData0());
                } else
                if (chan == AntxChannel.CHAN_PTO_FUEL_BY_DAY) {
                    ad.setValue(AntxLayout.DATA_FUEL_PTO  , a.getData0());
                } else
                if (chan == AntxChannel.CHAN_IDLE_FUEL_DAY) {
                    ad.setValue(AntxLayout.DATA_FUEL_IDLE , a.getData0());
                } else
                if (chan == AntxChannel.CHAN_WORK_FUEL_DAY) {
                    ad.setValue(AntxLayout.DATA_FUEL_WORK , a.getData0());
                } else
                if (chan == AntxChannel.CHAN_DISTANCE_BY_DAY) {
                    ad.setValue(AntxLayout.DATA_DISTANCE  , a.getData0());
                }

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
