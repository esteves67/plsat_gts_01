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
// Description:
//  Java Cron scheduler
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.extra.util;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.net.*;
import java.security.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;

/**
*** Cron job handling tools
**/

public class Cron
{

    // ------------------------------------------------------------------------
    // <Crontab 
    //    [timeZone="US/Pacific"]                                       - Relevant timezone
    //    [interval="60"]                                               - Cron expiration check interval (seconds)
    //    [autoReload="false"]                                          - Autoreload xron XML file if changed
    //    [threadPoolSize="-1"]                                         - Maximum thread pool size [-1 = unlimited]
    //    >
    //    <Job name="SomeJob" 
    //      [thread="true"]                                             - 'true' to run in separate thread
    //      [active="true"]                                             - 'true' if this Job entry is active
    //      >
    //      <Classpath>build/;/otherdir/.</Classpath>                   - Classpath
    //      <Class>com.example.SomeJob</Class>                          - Class
    //      <Method>cron</Method>                                       - Method to execute
    //      <Arg>-account=smith</Arg>                                   - Argument
    //      <Arg>-device=jones</Arg>                                    - Argument
    //      <!-- On the first day of the month, and it is a Sunday, then every 15 minutes of every even hour -->
    //      <When monthDay="1" weekDay="sun" hour="*/2" minute="*/15"/>
    //    </Job>
    //    <Job name="SomeJob2" thread="false">
    //      <Class>com.example.SomeJob2</Class>
    //      <Method>cron</Method>
    //      <Args>-account=smith -device=jones</Args>
    //      <!-- On the first day of the month, and it is a Sunday, then every 15 minutes of every even hour -->
    //      <When>*/15 */2 * 1 sun</When>  <!-- see "man 5 crontab" -->
    //    </Job>
    //    <Job name="LS" active="false">
    //      <Class>org.opengts.opt.util.Cron</Class>
    //      <Method>exec</Method>
    //      <Arg>c:\bin\ls</Arg>
    //      <Arg>-laF</Arg>
    //      <!-- Every minute of every hour -->
    //      <When hour="*" minute="*/1"/>
    //    </Job>
    // </Crontab>
    // ------------------------------------------------------------------------

    /* title */
    private static final String  VERSION                = "0.1.7";
    private static final String  COPYRIGHT              = "Copyright(C) 2007-2012 GeoTelematic Solutions, Inc.";

    // ------------------------------------------------------------------------

    private static final String  CRONTAB_DIR            = "crontab";
    private static final String  CRONTAB_XML            = "crontab.xml";

    private static final long    DEFAULT_INTERVAL_SEC   = DateTime.MinuteSeconds(1);
    
    private static final boolean DEFAULT_AUTO_RELOAD    = false;
    private static final boolean DEFAULT_STOP_ON_ERROR  = true;

    // ------------------------------------------------------------------------

    private static final String TAG_Crontab             = "Crontab";
    private static final String TAG_Job                 = "Job";
    private static final String TAG_Title               = "Title";
    private static final String TAG_Class               = "Class";
    private static final String TAG_Classpath           = "Classpath";
    private static final String TAG_Method              = "Method";
    private static final String TAG_Args                = "Args";
    private static final String TAG_Arg                 = "Arg";
    private static final String TAG_When                = "When";
    private static final String TAG_Include             = "Include";

    private static final String ATTR_interval           = "interval";       // seconds
    private static final String ATTR_timeZone           = "timeZone";
    private static final String ATTR_stopOnError        = "stopOnError";
    private static final String ATTR_autoReload         = "autoReload";
    private static final String ATTR_threadPoolSize     = "threadPoolSize";

    private static final String ATTR_name               = "name";
    private static final String ATTR_thread             = "thread";
    private static final String ATTR_active             = "active";

    private static final String ATTR_minute             = "minute";
    private static final String ATTR_hour               = "hour";
    private static final String ATTR_monthDay           = "monthDay";
    private static final String ATTR_month              = "month";
    private static final String ATTR_weekDay            = "weekDay";

    private static final String ATTR_file               = "file";

    // ------------------------------------------------------------------------

    private static final int    TYPE_MINUTE             = 1;
    private static final int    TYPE_HOUR               = 2;
    private static final int    TYPE_MONTH_DAY          = 3;
    private static final int    TYPE_MONTH              = 4;
    private static final int    TYPE_WEEK_DAY           = 5;

    // ------------------------------------------------------------------------

    private static final String PROCESS_INLINE_VAL      = "inline";
    private static final String PROCESS_THREAD_VAL      = "thread";
    private static final String PROCESS_NEW_VAL         = "new";

    private static final int    PROCESS_INLINE          = 0;
    private static final int    PROCESS_THREAD          = 1;
    private static final int    PROCESS_NEW             = 2; // not yet supported
    
    private static final int    DEFAULT_PROCESS         = PROCESS_THREAD;
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private long                intervalSec          = DEFAULT_INTERVAL_SEC;
    private boolean             autoReload           = DEFAULT_AUTO_RELOAD;
    private long                lastCrontabsReadMS   = 0L;
    private File                crontabXMLFile       = null;
    private OrderedSet<CronJob> cronJobs             = null;
    private ThreadPool          threadPool           = null;
    private TimeZone            timeZone             = null;

    /**
    *** Cron scheduler default constructor.  The default cron configuration xml file 
    *** will be loaded.
    **/
    public Cron()
        throws FileNotFoundException
    {
        this(null);
    }
    
    /**
    *** Cron scheduler default constructor
    *** @param xmlFile  The crontab xml file to load
    **/
    public Cron(File xmlFile)
        throws FileNotFoundException
    {
        super();
        this.setCrontabXMLFile(xmlFile);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the default crontab xml file
    *** @return The default crontab xml file
    **/
    private static File _getDefaultCrontabXMLFile()
        throws FileNotFoundException
    {
        File cfgFile = RTConfig.getLoadedConfigFile();
        if (cfgFile == null) {
            throw new FileNotFoundException("Default Crontab XML file directory not found");
        }
        File cronXml = new File(cfgFile.getParentFile(), CRONTAB_XML);
        try {
            return cronXml.getCanonicalFile();
        } catch (IOException ioe) {
            // ignore error, just return 'cronXml' as-is
            return cronXml;
        }
    }

    /** 
    *** Sets the crontab xml file to load
    *** @param xmlFile  The crontab file to load
    ***/
    public void setCrontabXMLFile(File xmlFile)
        throws FileNotFoundException
    {
        this.lastCrontabsReadMS = 0L;
        if ((xmlFile == null) || xmlFile.toString().equals("")) {
            this.crontabXMLFile = Cron._getDefaultCrontabXMLFile();
            if (!this.crontabXMLFile.isFile()) {
                throw new FileNotFoundException("Default Crontab XML file does not exist: " + this.crontabXMLFile);
            }
        } else
        if (xmlFile.isFile()) {
            this.crontabXMLFile = xmlFile;
        } else {
            this.crontabXMLFile = null;
            throw new FileNotFoundException("Crontab XML file does not exist: " + xmlFile);
        }
    }
    
    /**
    *** Returns the current crontab xml file
    *** @return the current crontab xml file
    **/
    public File getCrontabXMLFile()
        throws FileNotFoundException
    {
        if (this.crontabXMLFile == null) {
            throw new FileNotFoundException("Crontab XML file has not been specified");
        }
        return this.crontabXMLFile;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the cron wakeup interval (in seconds).
    *** @param intervSec  The cron wakeup interval (in seconds).
    **/
    public void setCronIntervalSec(long intervSec)
    {
        this.intervalSec = (intervSec > 0L)? intervSec : DEFAULT_INTERVAL_SEC;
    }

    /**
    *** Gets the current cron wakeup interval (in seconds).
    *** @return  The current cron wakeup interval (in seconds).
    **/
    public long getCronIntervalSec()
    {
        if (this.intervalSec <= 0) {
            this.intervalSec = DEFAULT_INTERVAL_SEC;
        }
        return this.intervalSec;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the current TimeZone
    *** @param tmzStr The TimeZone (String representation)
    **/
    public void setTimeZone(String tmzStr)
    {
        this.setTimeZone(DateTime.getTimeZone(tmzStr));
    }

    /**
    *** Sets the current TimeZone
    *** @param tmz The TimeZone
    **/
    public void setTimeZone(TimeZone tmz)
    {
        this.timeZone = tmz;
    }
    
    /**
    *** Gets the current TimZone
    *** @return The current TimeZone
    **/
    public TimeZone getTimeZone()
    {
        if (this.timeZone == null) {
            this.timeZone = DateTime.getDefaultTimeZone();
        }
        return this.timeZone;
    }
    
    // ------------------------------------------------------------------------

    /** 
    *** Gets the ThreadPool used to run threaded jobs
    *** @return The ThreadPool used to run threaded jobs
    **/
    protected ThreadPool getThreadPool()
    {
        if (this.threadPool == null) {
            this.threadPool = new ThreadPool("Cron");
        }
        return this.threadPool;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the current CronJob list
    *** @return The current CronJob list
    **/
    protected java.util.List<CronJob> getJobList()
    {
        if (this.cronJobs == null) {
            this.cronJobs = new OrderedSet<CronJob>();
        }
        return this.cronJobs;
    }
    
    /**
    *** Clears the current CronJob list
    **/
    public void cleanJobList()
    {
        if (this.cronJobs != null) {
            this.cronJobs.clear();
        }
    }
    
    /** 
    *** Returns the current number of CronJobs
    *** @return the current number of CronJobs
    **/
    public int getJobCount()
    {
        return (this.cronJobs != null)? this.cronJobs.size() : 0;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Start the Cron process.  
    *** This method blocks forever, or until an error occurs (whichever comes first)
    **/
    public void runCron()
        throws IOException
    {

        /* loop forever */
        for (;;) {

            // check for updated crontab XML
            this._load(false);

            // check/run jobs
            TimeZone tz   = this.getTimeZone();
            ThreadPool tp = this.getThreadPool();
            for (Iterator i = this.getJobList().iterator(); i.hasNext();) {
                CronJob cj = (CronJob)i.next();
                cj.testAndRun(tz, tp);
            }

            // Sleep: wake up at the beginning of the next interval (seconds)
            long intervalSec = this.getCronIntervalSec();
            long thisTimeSec = DateTime.getCurrentTimeSec();
            long nextTimeSec = ((thisTimeSec / intervalSec) + 1L) * intervalSec;
            long sleepSec    = nextTimeSec - thisTimeSec;
            try { 
                Thread.sleep((sleepSec * 1000L) + 10L); 
            } catch (Throwable th) {
                Print.logWarn("Sleep interrupted ...");
            }

        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Loads the specified crontab xml file
    *** @param xmlFile  The crontab xml file to load
    **/
    public void load(File xmlFile)
        throws IOException
    {
        this.setCrontabXMLFile(xmlFile);
        this._load(true);
    }
    
    /** 
    *** Loads/Reloads the current crontab xml file
    *** @param forceReload  True to force a reload of the current crontab xml file
    **/
    private void _load(boolean forceReload)
        throws IOException
    {

        /* XML file */
        File xmlFile = this.getCrontabXMLFile();
        if (this.lastCrontabsReadMS > 0L) {
            // we are reloading
            if (!forceReload && (this.lastCrontabsReadMS == xmlFile.lastModified())) {
                // already up-to-date
                return;
            } else
            if (!this.autoReload) {
                // ignore the reload
                Print.logDebug("File changed, but 'autoReload' has been disabled");
                this.lastCrontabsReadMS = xmlFile.lastModified();
                return;
            }
            Print.logInfo("Reloading Crontab XML file: " + xmlFile);
        } else {
            Print.logInfo("Loading Crontab XML file: " + xmlFile);
        }

        /* clear all existing jobs */
        this.cleanJobList();

        /* get XML document */
        this.lastCrontabsReadMS = xmlFile.lastModified();
        Document xmlDoc = this.getDocument(xmlFile);
        if (xmlDoc == null) {
            throw new FileNotFoundException("Crontab XML file open/parse error: " + xmlFile);
        }

        /* get top-level tag */
        Element crontabs = xmlDoc.getDocumentElement();
        if (!crontabs.getTagName().equalsIgnoreCase(TAG_Crontab)) {
            throw new IOException("Invalid root XML tag (expected 'Crontab'): " + xmlFile);
        }

        /* runtime properties prefix: "CronTab." ... */
        String rtPrefix_ = TAG_Crontab + ".";

        /* stop on error: eg "Crontab.stopOnError=true" */
        boolean stopOnError;
        {
            String soeKey = rtPrefix_ + ATTR_stopOnError;
            stopOnError = RTConfig.hasProperty(soeKey)?
                RTConfig.getBoolean(soeKey, DEFAULT_STOP_ON_ERROR) :
                this.getAttributeBoolean(crontabs, ATTR_stopOnError, DEFAULT_STOP_ON_ERROR);
        }

        /* reload ok? eg "Crontab.autoReload=false" */
        {
            String autoKey = rtPrefix_ + ATTR_autoReload;
            this.autoReload = RTConfig.hasProperty(autoKey)?
                RTConfig.getBoolean(autoKey, DEFAULT_AUTO_RELOAD) :
                this.getAttributeBoolean(crontabs, ATTR_autoReload, DEFAULT_AUTO_RELOAD);
        }

        /* threadPool size: eg "Crontab.threadPoolSize=5" */
        {
            String tpsKey = rtPrefix_ + ATTR_threadPoolSize;
            long threadPoolSize = RTConfig.hasProperty(tpsKey)?
                RTConfig.getLong(tpsKey, -1L) :
                this.getAttributeLong(crontabs, ATTR_threadPoolSize, -1L);
            if (threadPoolSize > 0L) {
                this.getThreadPool().setMaxSize((int)threadPoolSize);
            }
        }

        /* cron schedule interval (seconds): eg "Crontab.interval=60" */
        {
            String intvKey = rtPrefix_ + ATTR_threadPoolSize;
            long interval = RTConfig.hasProperty(intvKey)?
                RTConfig.getLong(intvKey, -1L) :
                this.getAttributeLong(crontabs, ATTR_interval, -1L);
            this.setCronIntervalSec(interval);
        }

        /* timezone: eg "Crontab.timezone=system" */
        {
            String tmzKey = rtPrefix_ + ATTR_timeZone;
            String tmzStr = RTConfig.hasProperty(tmzKey)?
                RTConfig.getString(tmzKey, null) :
                this.getAttributeString(crontabs, ATTR_timeZone, null);
            if (!StringTools.isBlank(tmzStr)) {
                if (tmzStr.equalsIgnoreCase("system" ) || 
                    tmzStr.equalsIgnoreCase("local"  ) || 
                    tmzStr.equalsIgnoreCase("default")   ) {
                    TimeZone tmz = DateTime.getDefaultTimeZone();
                    this.setTimeZone(tmz);
                    Print.logInfo("TimeZone = " + tmz.getID());
                } else {
                    if (!DateTime.isValidTimeZone(tmzStr)) {
                        throw new IOException("Invalid TimeZone specified: " + tmzStr);
                    }
                    this.setTimeZone(tmzStr);
                    Print.logInfo("TimeZone = " + tmzStr);
                }
            }
        }

        /* parse <Job>s */
        NodeList jobNodeList = XMLTools.getChildElements(crontabs,TAG_Job);
        for (int j = 0; j < jobNodeList.getLength(); j++) {
            Element jobTag = (Element)jobNodeList.item(j);

            /* job name */
            String jobName = this.getAttributeString(jobTag, ATTR_name, null);
            if (StringTools.isBlank(jobName)) {
                jobName = TAG_Job + "_" + j;
                Print.logWarn("Job 'name' attribute missing/blank, assigning name '"+jobName+"'");
            }

            /* active? eg "Crontab.HourlyCron.active=true" */
            {
                String actvKey = rtPrefix_ + jobName + "." + ATTR_active;
                boolean active = RTConfig.hasProperty(actvKey)?
                    RTConfig.getBoolean(actvKey, true) :
                    this.getAttributeBoolean(jobTag, ATTR_active, true);
                if (!active) {
                    Print.logWarn("Skipping inactive Job: " + jobName);
                    continue;
                }
            }

            /* process mode: eg "Crontab.HourlyCron.thread=true" */
            int procMode;
            {
                String threadKey = rtPrefix_ + jobName + "." + ATTR_thread;
                boolean threadMode = RTConfig.hasProperty(threadKey)?
                    RTConfig.getBoolean(threadKey, true) :
                    this.getAttributeBoolean(jobTag, ATTR_thread, true);
                procMode = threadMode? PROCESS_THREAD : PROCESS_INLINE;
            }

            /* Job vars */
            String title       = "";
            String className   = "";
            String classPath[] = null;
            String methName    = "";
            java.util.List<String> argList = new Vector<String>();
            StringBuffer argSB = new StringBuffer();
            When   when        = null;

            /* parse Job node */
            NodeList attrList = jobTag.getChildNodes();
            for (int c = 0; c < attrList.getLength(); c++) {

                /* get Node (only interested in 'Element's) */
                Node attrNode = attrList.item(c);
                if (!(attrNode instanceof Element)) {
                    continue;
                }

                /* parse node */
                String attrName = attrNode.getNodeName();
                Element attrElem = (Element)attrNode;
                if (attrName.equalsIgnoreCase(TAG_Title)) {
                    // save title
                    String t = this.getNodeText(attrElem);
                    if (t != null) {
                        String ttl[] = StringTools.parseString(StringTools.replace(t.trim(),"\\n","\n"),'\n');
                        for (int i = 0; i < ttl.length; i++) { ttl[i] = ttl[i].trim(); }
                        title = StringTools.join(ttl,'\n');
                    } else {
                        title = null;
                    }
                } else
                if (attrName.equalsIgnoreCase(TAG_Class)) {
                    // class name
                    className = this.getNodeText(attrElem);
                    if (className.equals("Cron")) { // case sensitive
                        // This Cron class (typically used for "exec" method call)
                        className = StringTools.className(Cron.class);
                    }
                } else
                if (attrName.equalsIgnoreCase(TAG_Classpath)) {
                    // class path
                    classPath = StringTools.parseArray(this.getNodeText(attrElem),';');
                } else
                if (attrName.equalsIgnoreCase(TAG_Method)) {
                    // method name
                    methName = this.getNodeText(attrElem);
                } else
                if (attrName.equalsIgnoreCase(TAG_Arg)) {
                    // trim and add argument
                    String arg = RTConfig.insertKeyValues(this.getNodeText(attrElem).trim());
                    argList.add(arg);
                } else
                if (attrName.equalsIgnoreCase(TAG_Args)) {
                    // break arguments on space
                    String argStr = this.getNodeText(attrElem);
                    String args[] = ListTools.toArray(new StringTokenizer(argStr," ",false),String.class);
                    for (int i = 0; i < args.length; i++) {
                        String arg = RTConfig.insertKeyValues(args[i]);
                        argList.add(arg);
                    }
                } else
                if (attrName.equalsIgnoreCase(TAG_When)) {
                    // save 'when'
                    String cron = this.getNodeText(attrElem);
                    if ((cron != null) && !cron.equals("")) {
                        when = new When(cron);
                    } else {
                        String minute   = this.getAttributeString(attrElem, ATTR_minute  , WHEN_ALL);
                        String hour     = this.getAttributeString(attrElem, ATTR_hour    , WHEN_ALL);
                        String monthDay = this.getAttributeString(attrElem, ATTR_monthDay, WHEN_ALL);
                        String month    = this.getAttributeString(attrElem, ATTR_month   , WHEN_ALL);
                        String weekDay  = this.getAttributeString(attrElem, ATTR_weekDay , WHEN_ALL);
                        when = new When(minute, hour, monthDay, month, weekDay);
                    }
                } else {
                    // unrecognized tag
                    if (stopOnError) {
                        throw new IOException("Unrecognized tag: " + attrName);
                    } else {
                        Print.logWarn("Unrecognized tag: " + attrName);
                    }
                }

            }

            /* create cron-job */
            String args[] = argList.toArray(new String[argList.size()]);
            try {
                CronJob cronJob = new CronJob(jobName, title, 
                    classPath, className, 
                    methName, args, 
                    when, 
                    procMode, "");
                Print.logInfo("Job: " + cronJob);
                this.getJobList().add(cronJob);
            } catch (Throwable th) { // NoSuchMethodException, ClassNotFoundException
                Print.logException("Error", th);
                if (stopOnError) {
                    throw new IOException("Invalid Job specification: " + th);
                }
            }
            
        }

        /* do we have anything to do? */
        if (this.getJobList().size() <= 0) {
            if (stopOnError) {
                throw new IOException("No Jobs to perform!");
            }
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Creates/returns an XML Document for the specific xml file
    *** @param xmlFile The XML file
    **/
    protected Document getDocument(File xmlFile)
    {
        
        /* xmlFIle must be specified */
        if (xmlFile == null) {
            return null;
        }

        /* create XML document */
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(xmlFile);
        } catch (ParserConfigurationException pce) {
            Print.logError("Parse error: " + pce);
        } catch (SAXException se) {
            Print.logError("Parse error: " + se);
        } catch (IOException ioe) {
            Print.logError("IO error: " + ioe);
        }

        /* return */
        return doc;

    }

    /**
    *** Returns the String text for the specified node
    *** @param root  The node for which the node text will be returned
    **/
    protected String getNodeText(Node root)
    {
        StringBuffer sb = new StringBuffer();
        if (root != null) {
            NodeList list = root.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);
                if (n.getNodeType() == Node.CDATA_SECTION_NODE) { // CDATASection
                    sb.append(n.getNodeValue());
                } else
                if (n.getNodeType() == Node.TEXT_NODE) {
                    sb.append(n.getNodeValue());
                } else {
                    //Print.logWarn("Unrecognized node type: " + n.getNodeType());
                }
            }
        }
        return sb.toString();
    }

    /**
    *** Gets a named attribute from the specified element.
    *** @param elem   The element from which the attribute value will be returned
    *** @param key    The name of the attribute
    *** @param dft    The default value to return if the named attribute doesn't exist
    *** @return The value of the named attribute, or the default value if the attribute does
    ***         not exist.
    **/
    protected String getAttributeString(Element elem, String key, String dft)
    {
        
        /* invalid element/key */
        if ((elem == null) || (key == null)) {
            return dft;
        }
        
        /* simple test for element with exact name */
        if (elem.hasAttribute(key)) {
            // either exists, or has a default value
            return elem.getAttribute(key);
        }
        
        /* scan through attributes for matching (case insensitive) string */
        NamedNodeMap nnm = elem.getAttributes();
        if (nnm != null) {
            int len = nnm.getLength();
            for (int i = 0; i < len; i++) {
                Attr attr = (Attr)nnm.item(i);
                String attrName = attr.getName();
                if (key.equalsIgnoreCase(attrName)) {
                    // found a case-insensitive match
                    Print.logWarn("Expected attribute '" + key + "', but found '" + attrName + "'");
                    return attr.getValue();
                }
            }
        }
        
        /* still not found, return default */
        return dft;

    }

    /**
    *** Returns the boolean value of the named attribute from the specified element
    *** @param elem   The element from which the attribute value will be returned
    *** @param key    The name of the attribute
    *** @param dft    The default boolean value to return if the named attribute doesn't exist
    *** @return The boolean value of the named attribute, or the default value if the attribute 
    ***         does not exist.
    **/
    protected boolean getAttributeBoolean(Element elem, String key, boolean dft)
    {
        return StringTools.parseBoolean(this.getAttributeString(elem,key,null),dft);
    }

    /**
    *** Returns the long value of the named attribute from the specified element
    *** @param elem   The element from which the attribute value will be returned
    *** @param key    The name of the attribute
    *** @param dft    The default long value to return if the named attribute doesn't exist
    *** @return The long value of the named attribute, or the default value if the attribute 
    ***         does not exist.
    **/
    protected long getAttributeLong(Element elem, String key, long dft)
    {
        return StringTools.parseLong(this.getAttributeString(elem,key,null),dft);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Prints the specified input string to stdout
    *** @param in  The InputStream to read/print
    *** @param sb  A StringBuffer used to accumulate stream bytes and print
    *** @return True if any data was read from the stream
    **/
    private static boolean _printOutput(InputStream in, StringBuffer sb)
        throws IOException
    {
        boolean didRead = false;
        for (;;) {
            int avail = in.available();
            if (avail > 0) {
                didRead = true;
                for (; avail > 0; avail--) {
                    int b = in.read();
                    if (b >= 0) {
                        if (b == '\n') {
                            Print.sysPrintln(sb.toString());
                            sb.setLength(0);
                        } else {
                            sb.append((char)b);
                        }
                    } else {
                        // error?
                        return didRead;
                    }
                }
            } else {
                break;
            }
        }
        return didRead;
    }

    /**
    *** Executes the specified command in a separate process
    *** @param cmdArgs  The command and arguments to execute
    **/
    public static void exec(String cmdArgs[])
    {
        if ((cmdArgs != null) && (cmdArgs.length > 0)) {
            Process process = null;
            try {
                
                /* start process */
                if (cmdArgs.length > 1) {
                    process = Runtime.getRuntime().exec(cmdArgs);
                } else {
                    String cmd = StringTools.join(cmdArgs,' ');
                    process = Runtime.getRuntime().exec(cmd);
                }
                
                /* read available stdout/stderr */
                InputStream  stdout   = new BufferedInputStream(process.getInputStream());
                StringBuffer stdoutSB = new StringBuffer();
                InputStream  stderr   = new BufferedInputStream(process.getErrorStream());
                StringBuffer stderrSB = new StringBuffer();
                for (;;) {
                    try { Thread.sleep(75L); } catch (Throwable th) { /*ignore*/ }
                    boolean didReadOut = Cron._printOutput(stdout, stdoutSB);
                    boolean didReadErr = Cron._printOutput(stderr, stderrSB);
                    if (!didReadOut && !didReadErr) {
                        try {
                            process.exitValue();
                            break;
                        } catch (Throwable th) {
                            Print.logDebug("Not done yet");
                        }
                    }
                }
                
                /* flush any unprinted stdout/stderr */
                if (stdoutSB.length() > 0) {
                    Print.sysPrintln(stdoutSB.toString());
                }
                if (stderrSB.length() > 0) {
                    Print.sysPrintln(stderrSB.toString());
                }
                
                /* get process exit value */
                for (;;) {
                    try {
                        process.waitFor();
                        int status = process.exitValue();
                        if (status != 0) {
                            Print.logError("Job process terminated with status " + status);
                        }
                        return;
                    } catch (InterruptedException ie) {
                        // ignore
                    }
                }
                
            } catch (Throwable th) {
                Print.logException("Job process failed", th);
                if (process != null) {
                    process.destroy();
                }
            }
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String WHEN_ALL    = "*";
    
    /**
    *** This class determines when a cronjob should be triggered
    **/
    public static class When
    {
        private String minute   = null;
        private String hour     = null;
        private String monthDay = null;
        private String month    = null;
        private String weekDay  = null;
        public When(String minute, String hour, String monthDay, String month, String weekDay) {
            this._setWhen(minute, hour, monthDay, month, weekDay);
        }
        public When(String wh) {
            String c[] = ListTools.toArray(new StringTokenizer(wh," ",false), String.class);
            String minute   = (c.length > 0)? c[0] : null;
            String hour     = (c.length > 1)? c[1] : null;
            String monthDay = (c.length > 2)? c[2] : null;
            String month    = (c.length > 3)? c[3] : null;
            String weekDay  = (c.length > 4)? c[4] : null;
            this._setWhen(minute, hour, monthDay, month, weekDay);
        }
        private void _setWhen(String minute, String hour, String monthDay, String month, String weekDay) {
            this.minute   = ((minute   != null) && !minute.trim().equals("")  )? minute.trim()   : WHEN_ALL;
            this.hour     = ((hour     != null) && !hour.trim().equals("")    )? hour.trim()     : WHEN_ALL;
            this.monthDay = ((monthDay != null) && !monthDay.trim().equals(""))? monthDay.trim() : WHEN_ALL;
            this.month    = ((month    != null) && !month.trim().equals("")   )? month.trim()    : WHEN_ALL;
            this.weekDay  = ((weekDay  != null) && !weekDay.trim().equals("") )? weekDay.trim()  : WHEN_ALL;
        }
        public String getMinute() {
            return this.minute;
        }
        public String getHour() {
            return this.hour;
        }
        public String getMonthDay() {
            return this.monthDay;
        }
        public String getMonth() {
            return this.month;
        }
        public String getWeekDay() {
            return this.weekDay;
        }
        public boolean testNow(long timeSec, TimeZone tz) {
            DateTime dt = new DateTime(timeSec, tz);
            if (!TestRange(this.getMonth()   , dt.getMonth1()    , TYPE_MONTH)) {
                //Print.logDebug("Test Month failed: " + this.getMonth() + " != " + dt.getMonth1());
                return false; 
            } else
            if (!TestRange(this.getMonthDay(), dt.getDayOfMonth(), TYPE_MONTH_DAY)) { 
                //Print.logDebug("Test DayOfMonth failed: " + this.getMonthDay() + " != " + dt.getDayOfMonth());
                return false; 
            } else
            if (!TestRange(this.getWeekDay() , dt.getDayOfWeek() , TYPE_WEEK_DAY)) { 
                //Print.logDebug("Test DayOfWeek failed: " + DateTime.getDayIndex(this.getWeekDay(),-1) + " != " + dt.getDayOfWeek());
                return false; 
            } else
            if (!TestRange(this.getHour()    , dt.getHour24()    , TYPE_HOUR)) { 
                //Print.logDebug("Test Hour failed: " + this.getHour() + " != " + dt.getHour24());
                return false; 
            } else
            if (!TestRange(this.getMinute()  , dt.getMinute()    , TYPE_MINUTE)) { 
                //Print.logDebug("Test Minute failed: " + this.getMinute() + " != " + dt.getMinute());
                return false; 
            } else {
                return true;
            }
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getMinute());
            sb.append(" ");
            sb.append(this.getHour());
            sb.append(" ");
            sb.append(this.getMonthDay());
            sb.append(" ");
            sb.append(this.getMonth());
            sb.append(" ");
            sb.append(this.getWeekDay());
            return sb.toString();
        }
    }
    
    /**
    *** Tests to see if the specified value is within the specified range
    *** @param range  The valid range (String representation)
    *** @param value  The value tested for inclusion in the range
    *** @param type   The type of range test to perform
    *** @return True if the value is within the specified range
    **/
    public static boolean TestRange(String range, int value, int type)
    {
        // Valid values:
        //   #[-#][/#][,...]
        if ((range == null) || range.equals("") || range.equals(WHEN_ALL)) {
            
            // no range, assume true
            return true;
            
        } else {
            
            String va[] = StringTools.parseArray(range,',');
            for (int i = 0; i < va.length; i++) {
                
                // divisor
                int dsp = va[i].indexOf('/');
                String dss = (dsp >= 0)? va[i].substring(dsp+1) : WHEN_ALL;
                int div = (dss.equals(WHEN_ALL))? 1 : StringTools.parseInt(dss,1);
                if (div < 1) { div = 1; }
                if ((value % div) != 0) {
                    // value failed the divisor test
                    continue;
                }
    
                // range value
                String rv = (dsp >= 0)? va[i].substring(0,dsp).trim() : va[i];
                if (rv.equals(WHEN_ALL)) {
                    return true;
                }
                
                // range?
                int rp = rv.indexOf('-');
                if (rp >= 0) {
                    String loStr = rv.substring(0,rp);
                    String hiStr = rv.substring(rp+1);
                    int lo, hi;
                    if (type == TYPE_WEEK_DAY) {
                        lo = DateTime.getDayIndex(loStr,-1);
                        hi = DateTime.getDayIndex(hiStr,-1);
                    } else 
                    if (type == TYPE_MONTH) {
                        lo = DateTime.getMonthIndex1(loStr,-1);
                        hi = DateTime.getMonthIndex1(hiStr,-1);
                    } else {
                        lo = StringTools.parseInt(loStr,0);
                        hi = StringTools.parseInt(hiStr,999);
                    }
                    //Print.logInfo("LO=" + lo);
                    //Print.logInfo("HI=" + hi);
                    if ((value >= lo) && (value <= hi)) {
                        return true;
                    }
                } else {
                    int vv = (type == TYPE_WEEK_DAY)? DateTime.getDayIndex(rv,-1) : StringTools.parseInt(rv,-1);
                    //Print.logInfo("VV=" + vv);
                    if (value == vv)  {
                        return true;
                    }
                }
    
            }
            return false;
            
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Custom CronClassLoader
    **/
    private static class CronClassLoader
        extends URLClassLoader
    {
        public CronClassLoader(File cpFile[]) {
            super(new URL[0]);
            for (int i = 0; i < cpFile.length; i++) {
                try {
                    URL url = new URL("file:///" + cpFile[i].getAbsolutePath() + "/");
                    this.addURL(url);
                } catch (MalformedURLException mue) {
                    Print.logError("Invalid file specification: " + cpFile[i]);
                }
            }
        }
        public Class loadClassAndResolve(String className)
            throws ClassNotFoundException {
            return this.loadClass(className, true);
        }
    }

    /**
    *** Returns the Class for the specified className
    *** @param classPath  The CLASSPATH to search for the class
    *** @param className  The class name for which the Class object is returned
    *** @return The Class object for the specified class name
    **/
    private static Class getClass(String classPath[], String className)
        throws ClassNotFoundException
    {
        if ((classPath == null) || (classPath.length <= 0)) {
            Print.logDebug("Getting class from default ClassLoader: " + className);
            return Class.forName(className);
        } else {
            Print.logDebug("Getting class from custom ClassLoader: " + className);
            java.util.List<File> cpList = new Vector<File>();
            for (int i = 0; i < classPath.length; i++) {
                File file = new File(classPath[i]);
                if (file.exists()) {
                    cpList.add(file);
                } else {
                    Print.logWarn("ClassPath item not found: " + file);
                }
            }
            File filePath[] = cpList.toArray(new File[cpList.size()]);
            CronClassLoader ccl = new CronClassLoader(filePath);
            return ccl.loadClassAndResolve(className);
        }
    }
    
    /**
    *** CronJob class
    **/
    public class CronJob
        implements Runnable
    {
        private String       name           = "";
        private String       title          = "";
        private long         lastStartSec   = 0L;
        private long         lastStopSec    = 0L;
        private String       classPath[]    = null; // not currently used
        private String       className      = null;
        private String       methName       = null;
        private String       args[]         = null;
        private When         when           = null;
        private MethodAction method         = null;
        private int          processMode    = PROCESS_THREAD;
        private String       processName    = null;
        public CronJob(String name, String title,
            String classPath[], String className, 
            String methName, String args[], 
            When when, 
            int processMode, String processName) 
            throws NoSuchMethodException, ClassNotFoundException {
            this.name        = (name != null)? name : "cron";
            this.title       = title;
            this.classPath   = classPath;
            this.className   = className;
            this.methName    = methName;
            this.args        = (args != null)? args : new String[0];
            this.when        = when; // may be null
            Class clazz      = Cron.getClass(this.classPath, this.className);
            this.method      = new MethodAction(clazz, this.methName, new Class[] { String[].class });
            this.processMode = processMode;
            this.processName = processName;
        }
        public String getName() {
            return this.name;
        }
        public String[] getClassPath() {
            return this.classPath;
        }
        public String getClassName() {
            return this.className;
        }
        public String getMethodName() {
            return this.methName;
        }
        public String[] getArgs() {
            return this.args;
        }
        public int getProcessMode() {
            return this.processMode;
        }
        public String getProcessName() {
            return this.processName;
        }
        public boolean runInThread() {
            int p = this.getProcessMode();
            if (p == PROCESS_INLINE) {
                return false;
            } else
            if (p == PROCESS_NEW) {
                return true;
            } else {
                return true;
            }
        }
        public boolean runInProcess() {
            return false;
        }
        public boolean testAndRun(TimeZone tz, ThreadPool threadPool) {
            long nowSec      = DateTime.getCurrentTimeSec();
            long lastStart   = this.getLastStartSec();
            long intervalSec = Cron.this.getCronIntervalSec();
            if ((lastStart / intervalSec) == (nowSec / intervalSec)) {
                Print.logWarn("Time schedule already Tested: " + this);
                return false;
            } else
            if ((this.when == null) && (lastStart > 0L)) {
                // no "When" specified, and we've already run once
                return false;
            } else
            if ((this.when == null) || this.when.testNow(nowSec,tz)) {
                // Run once, or timer has elapsed
                if ((lastStart > 0L) && (lastStart > this.getLastStopSec())) {
                    // the last time we started is _after_ the last time we stopped, so we are still running
                    Print.logError("Previous Job has not completed!: " + this);
                } else {
                    switch (this.getProcessMode()) {
                        case PROCESS_NEW:
                            // process (not supported here)
                            // Separate processes handled by "Cron.exec(...)" method
                        case PROCESS_THREAD:
                            // thread
                            if (threadPool != null) {
                                // thread
                                threadPool.run(this);
                            } else {
                                // inline
                                this.run();
                            }
                            return true; // the job may not have started yet
                        default:
                            // inline
                            this.run();
                            return true;
                    }
                }
            }
            return false;
        }
        public void run() {
            Print.sysPrintln("==============================================================================");
            Print.sysPrintln("Time: " + (new DateTime()));
            Print.sysPrintln("Cron: " + this);
            this.lastStartSec = DateTime.getCurrentTimeSec();
            try {
                if ((this.title != null) && !this.title.equals("")) {
                    Print.sysPrintln(this.title);
                }
                this.method.invoke(new Object[] { this.args });
            } catch (Throwable th) { // InvocationException, RuntimeException
                Print.sysPrintln("");
                Print.logException(this.toString(), th);
            }
            this.lastStopSec = DateTime.getCurrentTimeSec();
            Print.sysPrintln("Done: " + this);
            //Print.sysPrintln("------------------------------------------------------------------------------");
        }
        public long getLastStartSec() {
            return this.lastStartSec;
        }
        public long getLastStopSec() {
            return this.lastStopSec;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("[").append(this.getName()).append("] ");
            sb.append(this.className).append(".").append(this.methName);
            sb.append("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) { sb.append(","); }
                sb.append("\"").append(args[i]).append("\"");
            }
            sb.append(") : ");
            if (this.when != null) {
                sb.append(this.when.toString());
            } else {
                sb.append("<RunOnce>");
            }
            switch (this.getProcessMode()) {
                case PROCESS_INLINE:
                    sb.append(" {inline}");
                    break;
                case PROCESS_NEW:
                    sb.append(" {process}");
                    break;
                case PROCESS_THREAD:
                    sb.append(" {thread}");
                    break;
                default:
                    sb.append(" {unknown}");
                    break;
            }
            return sb.toString();
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // System.setSecurityManager(new SystemExitSecurityManager());
    private static SecurityManager OldSecurityManager = null;
    private static boolean EnableSystemExit = false;
    
    /**
    *** SystemExitSecurityManager class
    **/
    private static class SystemExitSecurityManager
        extends SecurityManager
    {
        public void checkExit(int status) {
            if (!EnableSystemExit) { 
                //throw new SecurityException(); 
                throw new RuntimeException("Job invoked 'System.exit'"); 
            }
        }
        public void checkPermission(Permission perm) {
            if (OldSecurityManager != null) {
                OldSecurityManager.checkPermission(perm);
            }
        }
    }
        
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static int  RecursionCheck = 0;
    
    private static final String ARG_CRONTAB[]  = { "crontab", "cron", "xml" };

    /**
    *** Main entry point
    *** @param argv  The command line arguments
    **/
    public static void main(String argv[])
    {

        /* runtime vars (if not already initialized) */
        if (!RTConfig.isInitialized()) {
            RTConfig.setCommandLineArgs(argv);
        }

        /* logging */
        RTConfig.setDebugMode(true);
        Print.setLogLevel(Print.LOG_ALL);
        Print.setLogHeaderLevel(Print.LOG_ALL);

        /* crontab */
        FileNotFoundException crontabErr = null;
        File crontab = RTConfig.getFile(ARG_CRONTAB,null);
        if ((crontab == null) || crontab.toString().equals("")) {
            try {
                crontab = Cron._getDefaultCrontabXMLFile();
            } catch (FileNotFoundException fnfe) {
                crontabErr = fnfe;
            }
        }
        if ((crontabErr == null) && !crontab.isFile()) {
            crontabErr = new FileNotFoundException("Crontab file does not exist: " + crontab);
        }

        /* header */
        Print.logInfo("----------------------------------------------------------------");
        Print.logInfo("Cron Server, Version " + VERSION);
        Print.logInfo(COPYRIGHT);
        Print.logInfo("Crontab: " + ((crontab!=null)?crontab:"<unknown>"));
        Print.logInfo("----------------------------------------------------------------");
        if (crontabErr != null) {
            Print.logException("Crontab file not found", crontabErr);
            Print.logInfo("Use \"-cron <file>\" option to override default");
            System.exit(1);
        }

        /* check resursion */
        if (RecursionCheck > 0) {
            Print.logStackTrace("Recursion not allowed in Cron scheduler!");
            System.exit(2);
        }
        
        /* save old SecurityManager and install new */
        OldSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new SystemExitSecurityManager());

        /* init/run cron */
        RecursionCheck++;
        try {
            Cron cron = new Cron(crontab);
            cron.runCron(); // blocks forever
        } catch (Throwable th) {
            Print.logException("Cron Error", th);
        }
        RecursionCheck--;

        /* if we get here, then an error has occurred */
        EnableSystemExit = true;
        System.exit(1);

    }

}
