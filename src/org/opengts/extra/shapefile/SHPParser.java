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
// Simple .shp file parser (not fully tested)
// References:
//  http://edndoc.esri.com/arcobjects/9.0/Samples/Geodatabase/Creating_and_Converting_Data/CreateShapefileFromText/CREATESHAPEFROMTEXT.htm
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.extra.shapefile;

import java.io.*;
import java.util.*;
import java.net.*;

import org.opengts.util.*;

public class SHPParser
{

    // ------------------------------------------------------------------------

    private int                     fileCode            = 9994;     // 9994
    private int                     fileLength16        = -1;       // in 16 bit words (x2 for bytes)
    private int                     version             = 1000;     // 1000
    private int                     shapeTypes          = 0;        // shape type
    private BoundingBox             fileBB              = null;     // 32 bytes
    private Range                   fileZRange          = null;     // 16 bytes
    private Range                   fileMRange          = null;     // 16 bytes

    private java.util.List<Shape>   shapeList           = null;

    /**
    *** SHPParser constuctor
    **/
    public SHPParser()
    {
        this.fileCode     = 9994;
        this.fileLength16 = -1;         // filled-in later
        this.version      = 1000;
        this.shapeTypes   = Shape.SHAPETYPE_UNDEFINED;
        this.fileBB       = null;
        this.fileZRange   = null;
        this.fileMRange   = null;
        this.shapeList    = null;
    }
    
    /**
    *** SHPParser constuctor
    **/
    public SHPParser(int shapeTypes, 
        BoundingBox bb, 
        Range zRange, Range mRange,
        java.util.List<Shape> shapes)
    {
        this();
        this.shapeTypes   = shapeTypes;
        this.fileBB       = bb;
        this.fileZRange   = zRange;
        this.fileMRange   = mRange;
        this.shapeList    = shapes;
    }

    /**
    *** Copy constuctor
    *** @param other  The other instance to copy
    **/
    public SHPParser(SHPParser other)
    {

        /* copy */
        this.fileCode     = 9994;
        this.fileLength16 = other.fileLength16;
        this.version      = 1000;
        this.shapeTypes   = other.shapeTypes;
        this.fileBB       = (other.fileBB     != null)? new BoundingBox(other.fileBB) : null;
        this.fileZRange   = (other.fileZRange != null)? new Range(other.fileZRange  ) : null;
        this.fileMRange   = (other.fileMRange != null)? new Range(other.fileMRange  ) : null;

        /* shapes (shallow copy) */
        this.shapeList    = (other.shapeList  != null)? new Vector<Shape>(other.shapeList) : null;

    }

    /**
    *** SHP file parser constuctor
    *** @param shpData  The byte array containing the dbf data to parse
    **/
    public SHPParser(byte shpData[])
        throws IOException
    {

        /* parse */
        this._parse(shpData);
        if (this.shapeList == null) {
            throw new IOException("No shapes defined");
        }

    }
    
    /**
    *** SHP file parser constuctor
    *** @param shpFile  The file containing the dbf data to parse
    **/
    public SHPParser(File shpFile)
        throws IOException
    {

        /* invalid file */
        if (!FileTools.isFile(shpFile,"shp")) {
            throw new IOException("Invalid file specification");
        }

        /* read */
        FileInputStream fis = null;
        byte shpData[] = null;
        try {
            fis = new FileInputStream(shpFile);
            shpData = FileTools.readStream(fis);
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            if (fis != null) { try { fis.close(); } catch (IOException ioe) {/*ignore*/} }
        }

        /* parse */
        this._parse(shpData);
        if (this.shapeList == null) {
            throw new IOException("Invalid SHP data");
        }

    }

    // ------------------------------------------------------------------------

    public int getFileCode()
    {
        return this.fileCode; // 9994
    }

    // ------------------------------------------------------------------------

    public void clearFileLength()
    {
        this.fileLength16 = -1;
    }
    
    public int getFileLength16(boolean isIndex)
    {
        if (isIndex) {
            return (100 + (4 * this.getShapeCount())) / 2;
        } else {
            if (this.fileLength16 <= 0) {
                int len = 100; // bytes (header size)
                if (this.shapeList != null) {
                    for (Shape s : this.shapeList) {
                        int shapeLen = s.getShapeLength(); //bytes
                        //Print.logInfo("Shape length: " + shapeLen);
                        len += shapeLen; //bytes
                    }
                }
                this.fileLength16 = (len + 1) / 2;
                //Print.logInfo("Returning calculated length: [len="+len+"] " + this.fileLength16);
            } else {
                //Print.logInfo("Returning pre-calculated length: " + this.fileLength16);
            }
            return this.fileLength16;
        }
    }

    public int getFileLength(boolean isIndex)
    {
        return this.getFileLength16(isIndex) * 2;
    }

    // ------------------------------------------------------------------------

    public int getVersion()
    {
        return this.version;
    }

    // ------------------------------------------------------------------------

    public int getShapeTypes()
    {
        return this.shapeTypes;
    }

    // ------------------------------------------------------------------------

    public BoundingBox getBoundingBox()
    {
        return this.fileBB;
    }

    // ------------------------------------------------------------------------

    public Range getFileZRange()
    {
        return this.fileZRange;
    }

    // ------------------------------------------------------------------------

    public Range getFileMRange()
    {
        return this.fileMRange;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Return number of shapes
    **/
    public int getShapeCount()
    {
        return ListTools.size(this.shapeList);
    }
    
    /**
    *** Returns true if this instance does not contain any shapes
    *** @return True if this instance does not contain any shapes
    **/
    public boolean isEmpty()
    {
        return ListTools.isEmpty(this.shapeList);
    }

    // ------------------------------------------------------------------------

    /**
    *** Return list of shapes 
    **/
    public java.util.List<Shape> getShapes()
    {
        return this.shapeList;
    }

    /**
    *** Return the Shape at the specified index
    **/
    public Shape getShapeAt(int ndx)
    {
        if ((ndx >= 0) && (ndx < ListTools.size(this.shapeList))) {
            return this.shapeList.get(ndx);
        } else {
            return null;
        }
    }
    
    /**
    *** Add a shape to the end of the list
    **/
    public void addShape(Shape sh)
    {
        if (sh != null) {
            // first shape type
            if (this.shapeTypes == Shape.SHAPETYPE_UNDEFINED) {
                this.shapeTypes = sh.getShapeType();
            }
            // all shape types must be the same
            if (this.shapeTypes != sh.getShapeType()) {
                Print.logError("Invalid shapeType: " + sh.getShapeType());
            } else {
                if (this.shapeList == null) {
                    this.shapeList = new Vector<Shape>();
                }
                this.shapeList.add(sh);
            }
            // reset length
            this.clearFileLength();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse SHP data
    **/
    private void _parse(byte data[])
        throws IOException
    {

        /* invalid data */
        if (ListTools.isEmpty(data)) {
            throw new IOException("Empty/Null data");
        } else
        if (data.length < 100) {
            throw new IOException("Invalid SHP data length: missing header");
        }

        /* start with header */
        Payload shpData     = new Payload(data);
        this.fileCode       = (int)shpData.readLong(4, 0L);         //  0: 4 - 9994
        shpData.readSkip(20);                                       //  4:20
        this.fileLength16   = (int)shpData.readLong(4, 0L);         // 24: 4 - in 16 bit words (x2 for bytes)
        this.version        = (int)shpData.readLong(4, 0L, false);  // 28: 4 - 1000
        this.shapeTypes     = (int)shpData.readLong(4, 0L, false);  // 32: 4 - shape type
        this.fileBB         = new BoundingBox(shpData);             // 36:32 -
        this.fileZRange     = new Range(shpData);                   // 68:16 -
        this.fileMRange     = new Range(shpData);                   // 84:16 -

        /* shapes */
        this.shapeList      = new Vector<Shape>();
        for (;(shpData.getAvailableReadLength() > 0);) {
            //Print.logInfo("SHPData read length ' " + shpData.getAvailableReadLength());
            Shape shape = new Shape(shpData);
            this.shapeList.add(shape);
            //Print.sysPrintln(shape.toString());
        }

        /* rebuild BoundingBox? */
        //this.fileBB = new BoundingBox(this.getShapes());

    }

    // ------------------------------------------------------------------------

    public void writeFile(Payload data, Payload index)
    {

        /* header */
        data.writeLong((long)this.fileCode,4);         //  0: 4 - 9994
        data.writeZeroFill(20);                        //  4:20 - 
        int fileLenIndex = data.getIndex();
        data.writeLong((long)0,4);                     // 24: 4 - fileLength 16 bit words (x2 for bytes)
        data.writeLong((long)this.version,4,false);    // 28: 4 - 1000
        data.writeLong((long)this.shapeTypes,4,false); // 32: 4 - shape type
        // BoundingBox
        if (this.fileBB == null) {
            this.fileBB = new BoundingBox(this.getShapes());
        }
        this.fileBB.write(data);                       // 36:32 -
        // Z Range
        if (this.fileZRange == null) {
            this.fileZRange = new Range(0.0,0.0);
        }
        this.fileZRange.write(data);                   // 68:16 -
        // M Range
        if (this.fileMRange == null) {
            this.fileMRange = new Range(0.0,0.0);
        }
        this.fileMRange.write(data);                   // 84:16 -
        
        /* copy data header to index */
        byte header[] = data.getBytes();  // 100 bytes in length
        index.writeBytes(header);

        /* Shapes */
        java.util.List<Shape> shapes = this.getShapes();
        if (!ListTools.isEmpty(shapes)) {
            //Print.logInfo("Writing Shapes: size = " + ListTools.size(shapes));
            int rcdNdx = 1;
            for (Shape s : shapes) {
                /* write data */
                int offset16 = (data.getIndex() + 1) / 2;
                int length16 = (s.writeShape(data, rcdNdx++) + 1) / 2;
                //Print.logInfo("Shape data offset = " + data.getIndex());
                //Print.logInfo("Offset="+offset16+"/"+(offset16*2) + "  Length="+length16+"/"+(length16*2));
                /* write index */
                index.writeULong((long)offset16, 4);
                index.writeULong((long)length16, 4);
            }
        } else {
            Print.logWarn("No shapes to write to stream");
        }

        /* Update SHP length */
        int dataSize = data.getSize();          // save current length
        data.resetIndex(fileLenIndex);          // set point to write file length
        int shpFileLen = (dataSize + 1) / 2;    // length / 2
        data.writeLong((long)shpFileLen, 4);
        data.resetIndex(dataSize);              // reset current length
        this.fileLength16 = shpFileLen;

        /* Update SHX length */
        int indexSize = index.getSize();        // save current length
        index.resetIndex(fileLenIndex);         // set point to write file length
        int shxFileLen = (indexSize + 1) / 2;   // length / 2
        index.writeLong((long)shxFileLen, 4);
        index.resetIndex(indexSize);            // reset current length

    }

    // ------------------------------------------------------------------------

    public String toString()
    {
        SHPParser shpp = this;
        StringBuffer sb = new StringBuffer();
        sb.append("FileCode   : " + shpp.getFileCode()).append("\n");
        sb.append("FileLength : " + shpp.getFileLength(false) + " bytes").append("\n");
        sb.append("Version    : " + shpp.getVersion()).append("\n");
        sb.append("ShapeTypes : " + shpp.getShapeTypes()).append("\n");
        sb.append("BoundingBox: " + shpp.getBoundingBox()).append("\n");
        sb.append("Z Range    : " + shpp.getFileZRange()).append("\n");
        sb.append("M Range    : " + shpp.getFileMRange()).append("\n");
        java.util.List<Shape> shapes = shpp.getShapes();
        if (!ListTools.isEmpty(shapes)) {
            for (Shape s : shapes) {
                sb.append(s.toString()).append("\n");
                sb.append("").append("\n");
            }
        }
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String ARG_FILE[] = new String[] { "file", "shp" };

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        /* get file */
        File shpFile = RTConfig.getFile(ARG_FILE,null);
        if (shpFile == null) {
            Print.sysPrintln("Missing '-file' specification");
            System.exit(99);
        } else
        if (!FileTools.isFile(shpFile,"shp")) {
            Print.sysPrintln("Not a 'shp' file: " + shpFile);
            System.exit(99);
        }

        /* read */
        FileInputStream fis = null;
        byte shpData[] = null;
        try {
            fis = new FileInputStream(shpFile);
            shpData = FileTools.readStream(fis);
        } catch (IOException ioe) {
            Print.logException("Invalid file specification", ioe);
            System.exit(99);
        } finally {
            if (fis != null) { try { fis.close(); } catch (IOException ioe) {/*ignore*/} }
        }

        /* parse */
        SHPParser shpp = null;
        try {
            shpp = new SHPParser(shpData);
        } catch (Throwable th) {
            Print.logException("Error",th);
        }

        /* list */
        Print.sysPrintln(shpp.toString());
        int origSHPLen = shpp.getFileLength(false);
        Print.sysPrintln("-------------------------------");

        /* copy */
        SHPParser shapeCopy = new SHPParser(shpp);
        Print.sysPrintln("-------------------------------");
        shapeCopy.clearFileLength();
        int copySHPLen = shapeCopy.getFileLength(false);
        Print.sysPrintln("Original Length " + origSHPLen + ", Copy Length " + copySHPLen);
        //System.exit(0);

        /* create copy bytes */
        Payload shapePayload = new Payload(shapeCopy.getFileLength(false) + 100);
        Payload indexPayload = new Payload(shapeCopy.getFileLength(true)  + 100);
        shapeCopy.writeFile(shapePayload, indexPayload);
        byte shapeBytes[] = shapePayload.getBytes();
        byte indexBytes[] = indexPayload.getBytes();

        /* diff */
        Print.sysPrintln("Original shapefile: ");
        Print.sysPrintln(StringTools.formatHexString(shpData   ).toString());
        Print.sysPrintln("");
        Print.sysPrintln("Copy: ");
        Print.sysPrintln(StringTools.formatHexString(shapeBytes).toString());
        Print.sysPrintln("Index: ");
        Print.sysPrintln(StringTools.formatHexString(indexBytes).toString());

        Print.sysPrintln("");
        Print.sysPrintln("Diff: " + StringTools.diff(shpData,shapeBytes));

    }
    
}
