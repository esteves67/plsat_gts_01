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
// Simple .dbf file parser (only limited functionality is supported)
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.extra.shapefile;

import java.io.*;
import java.util.*;
import java.net.*;

import org.opengts.util.*;

public class DBFParser
{

    // ------------------------------------------------------------------------

    public static final int  VERSION                = 3;
    public static final int  TERMINATOR             = 0x0D;
    public static final int  END_OF_DATA            = 0x1A;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int                      version          = 0;
    
    private DateTime                 updateDate       = null;
    
    private int                      incomplete       = 0;
    private int                      encryption       = 0;
    private int                      mdxProduction    = 0;  // '0' or '1'
    private int                      langDriverID     = 0;

    private java.util.List<Field>    fieldList        = null;

    private java.util.List<String[]> dataRecords      = null;

    /**
    *** Default constuctor
    **/
    public DBFParser()
    {

        /* fields (shallow copy) */
        this.fieldList      = null;

        /* init */
        this.version        = VERSION;
        this.updateDate     = null;
        this.incomplete     = 0;
        this.encryption     = 0;
        this.mdxProduction  = 0;
        this.langDriverID   = 0;
        this.dataRecords    = null;

    }

    /**
    *** Copy constuctor
    *** @param other  The DBFParser instance to copy
    **/
    public DBFParser(DBFParser other)
    {
        this();

        /* fields (shallow copy) */
        this.fieldList      = (other.fieldList   != null)? new Vector<Field>(other.fieldList) : null;

        /* data records (shallow copy) */
        this.dataRecords    = (other.dataRecords != null)? new Vector<String[]>(other.dataRecords) : null;

        /* copy */
        this.version        = VERSION;
        this.updateDate     = other.updateDate;
        this.incomplete     = 0;
        this.encryption     = 0;
        this.mdxProduction  = 0;
        this.langDriverID   = 0;

    }
    
    /**
    *** Constuctor
    *** @param fields  The defined fields
    **/
    public DBFParser(java.util.List<Field> fields)
    {
        this();

        /* fields (shallow copy) */
        this.fieldList      = fields;

    }

    /**
    *** DBF file parser constuctor
    *** @param dbfData  The byte array containing the dbf data to parse
    **/
    public DBFParser(byte dbfData[])
        throws IOException
    {
        this();

        /* parse */
        boolean parseOK = this._parse(dbfData);
        if (!parseOK) {
            throw new IOException("Invalid DBF data");
        }
        
    }
    
    /**
    *** DBF file parser constuctor
    *** @param dbfFile  The file containing the dbf data to parse
    **/
    public DBFParser(File dbfFile)
        throws IOException
    {
        this();

        /* invalid file */
        if (!FileTools.isFile(dbfFile,"dbf")) {
            throw new IOException("Invalid file specification");
        }

        /* read */
        FileInputStream fis = null;
        byte dbfData[] = null;
        try {
            fis = new FileInputStream(dbfFile);
            dbfData = FileTools.readStream(fis);
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            if (fis != null) { try { fis.close(); } catch (IOException ioe) {/*ignore*/} }
        }

        /* parse */
        boolean parseOK = this._parse(dbfData);
        if (!parseOK) {
            throw new IOException("Invalid DBF data");
        }

    }
    
    // ------------------------------------------------------------------------

    public void setUpdateTime(DateTime updateTime)
    {
        this.updateDate = updateTime;
    }

    public void setUpdateTime(int updYYYY, int updMM, int updDD)
    {
        this.updateDate = new DateTime(DateTime.getGMTTimeZone(), updYYYY, updMM, updDD, 0, 0, 0);
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Return number of fields
    **/
    public int getFieldCount()
    {
        return ListTools.size(this.fieldList);
    }
    
    /**
    *** Returns true if fields/columns are present
    *** @return True if fields/columns are present
    **/
    public boolean hasFields()
    {
        return (this.getFieldCount() > 0);
    }
    
    /**
    *** Return list of fields 
    **/
    public java.util.List<Field> getFields()
    {
        return this.fieldList;
    }
    
    /**
    *** Adds a new field to the end of the list
    **/
    public void addField(Field fld)
    {
        if (fld != null) {
            if (this.fieldList == null) {
                this.fieldList = new Vector<Field>();
            }
            this.fieldList.add(fld);
        }
    }

    /**
    *** Return the field at the specified index
    **/
    public Field getFieldAt(int ndx)
    {
        if ((ndx >= 0) && (ndx < ListTools.size(this.fieldList))) {
            return this.fieldList.get(ndx);
        } else {
            return null;
        }
    }

    /**
    *** Return the field name at the specified index
    **/
    public String getFieldNameAt(int ndx)
    {
        Field fld = this.getFieldAt(ndx);
        return (fld != null)? fld.getName() : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Return the full record length
    **/
    public int getRecordSize()
    {
        int rcdLen = 0;
        if (this.fieldList != null) {
            for (Field fld : this.fieldList) {
                rcdLen += fld.getLength();
            }
        }
        return rcdLen + 1;
    }

    /**
    *** Adds a new record to the end of the list
    **/
    public void addRecord(String rcd[])
    {
        if (ListTools.isEmpty(rcd)) {
            // ignore record
        } else
        if (rcd.length != this.getFieldCount()) {
            Print.logError("Invalid number of data fields in record: " + rcd.length);
        } else {
            if (this.dataRecords == null) {
                this.dataRecords = new Vector<String[]>();
            }
            this.dataRecords.add(rcd);
        }
    }

    /**
    *** Return number of records
    **/
    public int getRecordCount()
    {
        return ListTools.size(this.dataRecords);
    }

    /**
    *** Returns true if this instance contains records
    **/
    public boolean hasRecords()
    {
        return !ListTools.isEmpty(this.dataRecords);
    }

    /**
    *** Get record at index
    *** @param ndx  The Index
    *** @return The record
    **/
    public String[] getRecordAt(int ndx)
    {
        if ((ndx >= 0) && (ndx < ListTools.size(this.dataRecords))) {
            return this.dataRecords.get(ndx);
        } else {
            return null;
        }
    }

    /**
    *** Return the record at the specified index
    **/
    public RTProperties getPropertiesAt(int ndx, boolean ignoreCase)
    {
        String rcd[] = this.getRecordAt(ndx);
        if (!ListTools.isEmpty(rcd) && this.hasFields()) {
            RTProperties rtp = new RTProperties();
            rtp.setIgnoreKeyCase(ignoreCase);
            java.util.List<Field> fields = this.getFields();
            for (int f = 0; f < fields.size(); f++) {
                String key = fields.get(f).getName();
                String val = (f < rcd.length)? rcd[f] : "";
                rtp.setString(key, val);
            }
            return rtp;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    public int getHeaderSize()
    {
        // headerSize = 32 + (fieldCount * 32) + 1
        int hdrLen = 32 + (this.getFieldCount() * 32) + 1;
        //this.headerSize = hdrLen;
        return hdrLen;
    }
    
    public int getFileLength()
    {
        int headerDataLength = 32 + (this.getFieldCount() * 32) + 1;
        int recordDataLength = this.getRecordSize() * this.getRecordCount();
        return headerDataLength + recordDataLength;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Parse DBF data
    **/
    private boolean _parse(byte data[])
        throws IOException
    {
        
        /* clear */
        this.fieldList   = null;
        this.dataRecords = null;

        /* invalid data */
        if (ListTools.isEmpty(data)) {
            throw new IOException("Empty/Null data");
        }
        
        /* payload */
        Payload dbfData = new Payload(data);
        
        /* header */
        if (dbfData.getAvailableReadLength() < 32) {
            throw new IOException("Invalid DBF data length: missing header");
        }
        this.version        = (int)dbfData.readULong(1,0L);                            //  0: 1
        int updYYYY         = (int)dbfData.readULong(1,0L) + 1900;                     //  1: 1
        int updMM           = (int)dbfData.readULong(1,0L);                            //  2: 1
        int updDD           = (int)dbfData.readULong(1,0L);                            //  3: 1
        this.updateDate     = new DateTime(DateTime.getGMTTimeZone(), updYYYY, updMM, updDD, 0, 0, 0);
        int  recordCount    = (int)dbfData.readULong(4,0L,false); // little-endian     //  4: 4
        int  headerSize     = (int)dbfData.readULong(2,0L,false); // little-endian     //  8: 2
        int  recordSize     = (int)dbfData.readULong(2,0L,false); // little-endian     // 10: 2
        dbfData.readSkip(2); // "filled with zeros"                                 // 12: 2
        this.incomplete     = (int)dbfData.readLong(1,0L);                             // 14: 1
        this.encryption     = (int)dbfData.readLong(1,0L);                             // 15: 1
        dbfData.readSkip(12); // "Reserved for Multi-user processing"               // 16:12
        this.mdxProduction  = (int)dbfData.readLong(1,0L); // 1 if .MDX file exist     // 27: 1
        this.langDriverID   = (int)dbfData.readLong(1,0L); // Language driver id       // 28: 1
        dbfData.readSkip(2); // "filled with zeros"                                 // 29: 2
        
        /* expect version '3' */
        if (this.version != 3) {
            Print.logWarn("Unexpected Version: " + this.version);
        }

        /* field count */
        // headerSize = 32 + (fieldCount * 32) + 1
        int fieldCount = (headerSize - 33) / 32;
        if (((headerSize - 33) % 32) != 0) {
            Print.logError("Invalid Header Size: " + headerSize);
        }

        /* data fields */
        this.fieldList = new Vector<Field>();
        for (int fi = 0; dbfData.getAvailableReadLength() > 0; fi++) {

            /* end of data? */
            int peekByte = dbfData.peekByte();
            if (peekByte == TERMINATOR) {
                dbfData.readSkip(1);
                break;
            }
            
            /* field count */
            if (fi >= fieldCount) {
                break;
            }

            /* verify that their is enough data to read */
            if (dbfData.getAvailableReadLength() < 32) {
                Print.logError("Invalid DBF data length: missing field definition");
                break;
            }

            /* read field */
            byte fieldbytes[] = dbfData.readBytes(32);
            //Print.sysPrintln("Field Hex: 0x" + StringTools.toHexString(fieldbytes));
            Payload pField = new Payload(fieldbytes);
            Field fld = new Field(pField);
            /*
            String name = pField.readString(11,false);
            int type = (int)pField.readULong(1);
            //Print.sysPrintln("Field Type: " + type);
            pField.readSkip(4);
            int len = (int)pField.readULong(1,false);
            int dec = (int)pField.readULong(1);
            pField.readSkip(2);
            int waid = (int)pField.readULong(1);
            pField.readSkip(2);
            int flags = (int)pField.readULong(1);
            pField.readSkip(7);
            int index = (int)pField.readULong(1);
            //Print.sysPrintln("Field name: " + name);
            Field fld = new Field(name, type, len, dec, waid, flags, index);
            */
            this.fieldList.add(fld);
            Print.logDebug("Field: " + fld);
            
        }
        
        // "Field Property Structure" not supported
        if (dbfData.getIndex() != headerSize) {
            if (dbfData.getIndex() < headerSize) {
                int skipLen = headerSize - dbfData.getIndex();
                dbfData.readSkip(skipLen);
            } else {
                Print.logError("We've somehow read more bytes that are in the header!");
            }
        }
        
        // test that we have the proper amount of remaining data bytes
        int dataByteLength = dbfData.getSize();
        int expectedLength = recordSize * recordCount;
        if (dataByteLength < expectedLength) {
            Print.logError("Invalid Record Byte length: " + dataByteLength + " [expecting " + expectedLength + "]");
        }
        
        /* record length */
        if (recordSize != this.getRecordSize()) {
            Print.logError("File record length does not match calculated record length: " + recordSize + " != " + this.getRecordSize());
        }

        /* read data records */
        for (int ri = 0; dbfData.getAvailableReadLength() > 0; ri++) {

            /* EOF? */
            int rcdType = (int)dbfData.readULong(1,0L);
            if (rcdType == END_OF_DATA) {
                break;
            } else
            if (rcdType == '*') {
                if (dbfData.getAvailableReadLength() < (recordSize - 1)) {
                    Print.logError("Invalid DBF data length: missing record definition");
                }
                dbfData.readSkip(recordSize - 1);
                continue;
            } else {
                // rcdType == ' '?
            }

            /* record count */
            if (ri >= recordCount) {
                break;
            }

            /* read record */
            String valueArray[] = new String[this.fieldList.size()];
            //RTProperties rtRcd = new RTProperties();
            for (int i = 0; (i < this.fieldList.size()) && (dbfData.getAvailableReadLength() > 0); i++) {
                Field f = this.fieldList.get(i);
                String key = f.getName();
                String val = null;
                if (dbfData.getAvailableReadLength() < f.getLength()) { 
                    Print.logError("Invalid DBF data length: missing field data [C]");
                }
                switch (f.getDataType()) {
                    case Field.DATATYPE_STRING:  { // 'C': String
                        val = StringTools.trim(dbfData.readString(f.getLength(),false));
                        } break;
                    case Field.DATATYPE_DATE:    { // 'D': Date
                        val = dbfData.readString(4,false) + "/" + dbfData.readString(2,false) + "/" + dbfData.readString(2,false);
                        } break;
                    case Field.DATATYPE_FLOAT:   { // 'F': Float
                        val = StringTools.trim(dbfData.readString(f.getLength(),false));
                        } break;
                    case Field.DATATYPE_DOUBLE:  { // 'N': Double
                        val = StringTools.trim(dbfData.readString(f.getLength(),false));
                        } break;
                    case Field.DATATYPE_BOOLEAN: { // 'L': Boolean
                        int B = (int)dbfData.readULong(1,0L);
                        val = ((B == 'Y') || (B == 'y') || (B == 'T') || (B == 't'))? "true" : "false";
                        } break;
                    case Field.DATATYPE_MEMO:    { // 'M' : Memo (10 bytes, right justified, blank padded, numeric value)
                        val = StringTools.trim(dbfData.readString(f.getLength(),false));
                        } break;
                    default: { 
                        val = "";
                        } break;
                }
                valueArray[i] = val;
                //rtRcd.setString(key,val);
            }

            /* save record and continue */
            this.addRecord(valueArray);

        }
        
        return this.hasRecords();

    }
       
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Write header/data to Payload
    **/
    public Payload write(Payload p)
    {

        /* initialize update date */
        if (this.updateDate == null) {
            this.updateDate = new DateTime(DateTime.getGMTTimeZone());
        }
        
        /* record count */
        int  recordCount    = this.getRecordCount();

        /* header size */
        int  headerSize     = this.getHeaderSize(); // 32 + (this.getFieldCount() * 32) + 1;

        /* record size */
        int  recordSize     = this.getRecordSize();

        /* set complete / non-encrypted / no-MDX / no-LangDriver */
        /* leave as-is
        this.incomplete     = 0;
        this.encryption     = 0;
        this.mdxProduction  = 0;
        this.langDriverID   = 0;
        */

        /* header */
        p.writeULong((long)VERSION, 1);
        p.writeULong((long)this.updateDate.getYear() - 1900, 1);
        p.writeULong((long)this.updateDate.getMonth1(), 1);
        p.writeULong((long)this.updateDate.getDayOfMonth(), 1);
        p.writeULong((long)recordCount       , 4, false);
        p.writeULong((long)headerSize        , 2, false);
        p.writeULong((long)recordSize        , 2, false);
        p.writeZeroFill(2);
        p.writeULong((long)this.incomplete   , 1);
        p.writeULong((long)this.encryption   , 1);
        p.writeZeroFill(12);
        p.writeULong((long)this.mdxProduction, 1);
        p.writeULong((long)this.langDriverID , 1);
        p.writeZeroFill(2);

        /* fields */
        if (!ListTools.isEmpty(this.fieldList)) {
            for (Field fld : this.fieldList) {
                fld.write(p);
            }
        }
        p.writeULong(TERMINATOR, 1);
        
        /* data */
        if (!ListTools.isEmpty(this.fieldList)) {
            int rcdCnt = this.getRecordCount();
            for (int r = 0; r < rcdCnt; r++) {
                // record type
                p.writeULong((long)' ', 1);
                String rcd[] = this.getRecordAt(r);
                // fields
                for (int fi = 0; fi < this.fieldList.size(); fi++) {
                    // field data
                    Field  fld = this.fieldList.get(fi);
                    int    len = fld.getLength();
                    String key = fld.getName();
                    String val = (fi < rcd.length)? rcd[fi] : "";
                    switch (fld.getDataType()) {
                        case Field.DATATYPE_STRING:  { // 'C': String
                            String s = StringTools.leftAlign(val,len); // padRight
                            p.writeString(s, len, false);
                            } break;
                        case Field.DATATYPE_DATE:    { // 'D': Date
                            String s[] = StringTools.split(val,'/');
                            if (s.length < 3) { s = new String[] { "0000", "00", "00" }; }
                            p.writeString(StringTools.padLeft(s[0],'0',4-s[0].length()), 4, false);
                            p.writeString(StringTools.padLeft(s[1],'0',2-s[1].length()), 2, false);
                            p.writeString(StringTools.padLeft(s[2],'0',2-s[2].length()), 2, false);
                            } break;
                        case Field.DATATYPE_FLOAT:   { // 'F': Float
                            String s = StringTools.leftAlign(val,len);
                            p.writeString(s, len, false);
                            } break;
                        case Field.DATATYPE_DOUBLE:  { // 'N': Double
                            String s = StringTools.leftAlign(val,len);
                            p.writeString(s, len, false);
                            } break;
                        case Field.DATATYPE_BOOLEAN: { // 'L': Boolean
                            String s = val.equalsIgnoreCase("true")? "T" : "F";
                            p.writeString(s, 1, false);
                            } break;
                        case Field.DATATYPE_MEMO:    { // 'M' : Memo (10 bytes, right justified, blank padded, numeric value)
                            String s = StringTools.leftAlign(val,len);
                            p.writeString(s, len, false);
                            } break;
                        default: { 
                            String s = StringTools.leftAlign(val,len);
                            p.writeString(s, len, false);
                            } break;
                    }
                }
            }
            // write END_OF_DATA
            p.writeULong((long)END_OF_DATA, 1);
        }

        return p;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Version      : " + this.version).append("\n");
        sb.append("Updated      : " + this.updateDate).append("\n");
        sb.append("Record Count : " + this.getRecordCount()).append("\n");
        sb.append("Header Size  : " + this.getHeaderSize()).append("\n");
        sb.append("Record Size  : " + this.getRecordSize()).append("\n");
        sb.append("Incomplete   : " + this.incomplete).append("\n");
        sb.append("Encryption   : " + this.encryption).append("\n");
        sb.append("MDX File?    : " + this.mdxProduction).append("\n");
        sb.append("LangDriverID : " + this.langDriverID).append("\n");
        // contents
        sb.append("Contents     : ").append("\n");
        java.util.List<Field> dbFields = this.getFields();
        if (!ListTools.isEmpty(dbFields)) {
            // header
            StringBuffer H = new StringBuffer();
            for (Field f : dbFields) {
                if (H.length() > 0) { H.append(","); }
                H.append(StringTools.quoteCSVString(f.getName()));
            }
            sb.append("   ").append(H.toString()).append("\n");
            // record values
            int rcdCount = this.getRecordCount();
            for (int r = 0; r < rcdCount; r++) {
                String rcd[] = this.getRecordAt(r);
                if (!ListTools.isEmpty(rcd)) {
                    StringBuffer R = new StringBuffer();
                    for (int c = 0; c < rcd.length; c++) {
                        if (R.length() > 0) { R.append(","); }
                        R.append(StringTools.quoteCSVString(rcd[c]));
                    }
                    sb.append("   ").append(R.toString()).append("\n");
                }
            }
        } else {
            sb.append("   ").append("none").append("\n");
        }
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String ARG_FILE[]      = new String[] { "file", "dbf" };

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        /* get file */
        File dbfFile = RTConfig.getFile(ARG_FILE,null);
        if (dbfFile == null) {
            Print.sysPrintln("Missing '-file' specification");
            System.exit(99);
        } else
        if (!FileTools.isFile(dbfFile,"dbf")) {
            Print.sysPrintln("Not a 'dbf' file: " + dbfFile);
            System.exit(99);
        }

        /* read */
        FileInputStream fis = null;
        byte dbfData[] = null;
        try {
            fis = new FileInputStream(dbfFile);
            dbfData = FileTools.readStream(fis);
        } catch (IOException ioe) {
            Print.logException("Invalid file specification", ioe);
            System.exit(99);
        } finally {
            if (fis != null) { try { fis.close(); } catch (IOException ioe) {/*ignore*/} }
        }

        /* parse */
        DBFParser dbfp = null;
        try {
            dbfp = new DBFParser(dbfData);
        } catch (Throwable th) {
            Print.logException("Error",th);
        }
        
        /* header */
        Print.sysPrintln(dbfp.toString());
        
        /* list contents */
        java.util.List<Field> dbFields = dbfp.getFields();
        if (!ListTools.isEmpty(dbFields)) {
            StringBuffer H = new StringBuffer();
            for (Field f : dbFields) {
                if (H.length() > 0) { H.append(","); }
                H.append(StringTools.quoteCSVString(f.getName()));
            }
            Print.sysPrintln(H.toString());
            int rcdCount = dbfp.getRecordCount();
            for (int r = 0; r < rcdCount; r++) {
                String rcd[] = dbfp.getRecordAt(r);
                if (!ListTools.isEmpty(rcd)) {
                    StringBuffer R = new StringBuffer();
                    for (int c = 0; c < rcd.length; c++) {
                        if (R.length() > 0) { R.append(","); }
                        R.append(StringTools.quoteCSVString(rcd[c]));
                    }
                    Print.sysPrintln(R.toString());
                }
            }
        }
        
        /* copy */
        Payload copyPayload = new Payload(dbfp.getFileLength() + 100);
        DBFParser copy = new DBFParser(dbfp);
        copy.write(copyPayload);
        byte copyBytes[] = copyPayload.getBytes();
        
        /* diff */
        /* */
        Print.sysPrintln("Original DBF file: ");
        Print.sysPrintln(StringTools.formatHexString(dbfData  ).toString());
        Print.sysPrintln("");
        Print.sysPrintln("Copy: ");
        Print.sysPrintln(StringTools.formatHexString(copyBytes).toString());
        /* */
        Print.sysPrintln("");
        Print.sysPrintln("Diff: " + StringTools.diff(dbfData,copyBytes));

    }
    
}
