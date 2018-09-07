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
package org.opengts.extra.war.report.event;

import java.io.*;
import java.util.*;
import java.awt.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.google.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;
import org.opengts.war.report.event.*;

public class EventThermoReport
    extends EventDetailReport
{

    // ------------------------------------------------------------------------

    private static final String PROP_TITLE_COLOR        = "graph.titleColor";
    private static final String PROP_TITLE_FONT_SIZE    = "graph.titleFontSize";
    private static final String PROP_X_TICK_COUNT       = "graph.xTickCount";
    private static final String PROP_Y_TICK_COUNT       = "graph.yTickCount";

    // ------------------------------------------------------------------------

    private static final String COL_THERMO[] = new String[] {
        EventDataLayout.DATA_THERMO_1,
        EventDataLayout.DATA_THERMO_2,
        EventDataLayout.DATA_THERMO_3,
        EventDataLayout.DATA_THERMO_4,
        EventDataLayout.DATA_THERMO_6,
        EventDataLayout.DATA_THERMO_7,
        EventDataLayout.DATA_THERMO_8,
    };
    
    private static Color TEMP_COLOR[] = new Color[] { 
        Color.red, Color.green, Color.blue, Color.cyan, Color.yellow, Color.gray, Color.black,
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* convert F to C */
    private static double F2C(double F)
    {
        return (F - 32.0) * 5 / 9;
    }

    /* convert C to F */
    private static double C2F(double C)
    {
        return (C * 9 / 5) + 32.0;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Event Detail Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public EventThermoReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates and returns an iterator for the row data displayed in the body of this report.
    *** @return The body row data iterator
    **/
    public DBDataIterator getBodyDataIterator()
    {
        DBDataIterator dbi = super.getBodyDataIterator();
        if (dbi instanceof ArrayDataIterator) {
            ArrayDataIterator adi = (ArrayDataIterator)dbi;
            Object oa[] = adi.getArray();
            if (!ListTools.isEmpty(oa)) {
                int tempMask = 0;
                // find temperature set with/without valid data
                for (int i = 0; i < oa.length; i++) {
                    EventData ev = (EventData)oa[i];
                    for (int t = 0; t < 8; t++) {
                        double C = ev.getThermoAverage(t);
                        if (EventData.isValidTemperature(C)/* && (C != 0.0)*/) {
                            tempMask |= 1 << t;
                        } else {
                            Print.logInfo("Valid TempC: " + t + " " + C);
                        }
                    }
                }
                // clear all temperatures with invalid data
                for (int i = 0; i < oa.length; i++) {
                    EventData ev = (EventData)oa[i];
                    for (int t = 0; t < 8; t++) {
                        if ((tempMask & (1 << t)) == 0) {
                            ev.setThermoAverage(t,EventData.INVALID_TEMPERATURE);
                        }
                    }
                }
            }
        }
        return dbi;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this report supports displaying a graph
    *** @return True if this report supports displaying a graph, false otherwise
    **/
    public boolean getSupportsGraphDisplay()
    {
        return true;
    }

    public URIArg getGraphURL()
    {
        try {
            return this._getGraphURL();
        } catch (Throwable th) {
            Print.logException("Error", th);
            return new URIArg("");
        }
    }
    
    private URIArg _getGraphURL()
        throws Exception
    {
        TimeZone tz        = this.getTimeZone();
        DateTime timeStart = new DateTime(this.getTimeStart(),tz);
        DateTime timeEnd   = new DateTime(this.getTimeEnd(),tz);
        ReportLayout rl    = this.getReportLayout();
        String dateFormat  = "MM/dd";
        String timeFormat  = rl.getTimeFormat(this.getPrivateLabel());
        MapDimension size  = this.getGraphWindowSize();
        Locale locale      = this.getPrivateLabel().getLocale();
        I18N i18n          = I18N.getI18N(EventThermoReport.class, locale);
        int tempUnits      = this.getAccount().getTemperatureUnits(); // 0=F, 1=C

        /* which "thermoX" columns does this report have */
        int thermoCount = 0;
        @SuppressWarnings("unchecked")
        java.util.List<GoogleChartTemperature.Data> dataSet[] = new Vector[COL_THERMO.length];
        boolean dataSetValid[] = new boolean[COL_THERMO.length];
        for (int i = 0; i < COL_THERMO.length; i++) {
            if (this.hasReportColumn(COL_THERMO[i])) {
                dataSet[i] = new Vector<GoogleChartTemperature.Data>();
                thermoCount++;
            } else {
                dataSet[i] = null;
            }
            dataSetValid[i] = false; // verified below
        }

        /* fill datasets */
        double maxTempC = -9999.0;
        double minTempC =  9999.0;
        if (thermoCount > 0) {
            int evNdx = 0;
            for (DBDataIterator dbi = this.getBodyDataIterator(); dbi.hasNext();) {
                Object ev = dbi.next().getRowObject();
                if (ev instanceof EventData) {
                    EventData ed = (EventData)ev;
                    ed.setEventIndex(evNdx++);
                    if (!dbi.hasNext()) { ed.setIsLastEvent(true); }
                    double lastTempC = EventData.INVALID_TEMPERATURE;
                    for (int d = 0; d < dataSet.length; d++) {
                        if (dataSet[d] != null) {
                            long ts = ed.getTimestamp();
                            double tempC = ed.getThermoAverage(d);
                            if (EventData.isValidTemperature(tempC)) {
                                if (tempC > maxTempC) { maxTempC = tempC; } // maximum temp encountered
                                if (tempC < minTempC) { minTempC = tempC; } // minimum temp encountered
                                dataSet[d].add(new GoogleChartTemperature.Data(ts, tempC));
                                if (tempC != 0.0) {
                                    // contains a valid temperature that is not 0.0
                                    dataSetValid[d] = true; // contains valid data
                                }
                                lastTempC = tempC;
                            } else
                            if (EventData.isValidTemperature(lastTempC)) {
                                dataSet[d].add(new GoogleChartTemperature.Data(ts, lastTempC));
                            } else {
                                dataSet[d].add(new GoogleChartTemperature.Data(ts, 0.0));
                            }
                        } 
                    }
                } else {
                    Print.logWarn("Not an EventData: " + StringTools.className(ev));
                }
            }
        }

        /* clear datasets with no data */
        for (int d = 0; d < dataSet.length; d++) {
            if (!dataSetValid[d]) {
                dataSet[d] = null;
            }
        }

        /* adjust min/max temperatures */
        if (!EventData.isValidTemperature(minTempC) || !EventData.isValidTemperature(maxTempC)) {
            minTempC =  0.0; //  32.0 F
            maxTempC = 54.0; // 122.0 F
        } else
        if (tempUnits == GoogleChartTemperature.TEMP_C) {
            minTempC = Math.floor(minTempC / 10.0) * 10.0;
            maxTempC = Math.ceil( maxTempC / 10.0) * 10.0;
        } else {
            minTempC = F2C(Math.floor(C2F(minTempC) / 10.0) * 10.0);
            maxTempC = F2C(Math.ceil( C2F(maxTempC) / 10.0) * 10.0);
        }

        /* init graph */
        Color titleColor  = ColorTools.parseColor(this.getProperties().getString(PROP_TITLE_COLOR,"black"),Color.black);
        int titleFontSize = this.getProperties().getInt(PROP_TITLE_FONT_SIZE, 16);
        int xTickCount    = this.getProperties().getInt(PROP_X_TICK_COUNT   , 10);
        int yTickCount    = this.getProperties().getInt(PROP_Y_TICK_COUNT   ,  8);
        GoogleChartTemperature gct = new GoogleChartTemperature();
        gct.setSize(size.getWidth() - 30, size.getHeight() - 40);
        gct.setTitle(titleColor, titleFontSize, i18n.getString("EventThermoReport.temperature","Temperature"));
        gct.setTemperatureRange(this.getAccount().getTemperatureUnits(), minTempC, maxTempC, xTickCount);
        gct.setDateRange(timeStart, timeEnd, yTickCount);
        gct.setDateFormat("MM/dd");
        gct.setTimeFormat(timeFormat);

        /* add datasets */
        if (thermoCount > 0) {
            Color tempColor[] = TEMP_COLOR;
            for (int d = 0; d < dataSet.length; d++) {
                if (dataSet[d] != null) {
                    Color color = tempColor[d % tempColor.length];
                    String title = i18n.getString("EventThermoReport.temp","Temp {0}",String.valueOf(d+1));
                    gct.addDataSet(color, title, dataSet[d].toArray(new GoogleChartTemperature.Data[dataSet[d].size()]));
                }
            }
        } else {
            gct._addRandomSampleData(4,15);
        }

        /* return URL */
        String url = gct.toString();
        //Print.logInfo("URL:\n" + url);
        return new URIArg(url);

    }

    // ------------------------------------------------------------------------

}
