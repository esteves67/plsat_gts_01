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
package org.opengts.extra.shapefile;

import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;

import org.opengts.util.*;

public class ShapefileSet
{

    // ------------------------------------------------------------------------
    
    public static final String MIME_TYPE_ZIP    = HTMLTools.CONTENT_TYPE_ZIP;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String          filePrefix  = null;
    private DBFParser       dbf         = null;
    private SHPParser       shp         = null;
    
    /**
    *** Create new shapefile 
    **/
    public ShapefileSet()
    {
        this.dbf = new DBFParser();
        this.shp = new SHPParser();
    }
    
    /** 
    *** Load existing shapefile
    **/
    public ShapefileSet(String filePfx)
        throws IOException
    {
        this.filePrefix = filePfx;
        this.dbf = new DBFParser(new File(this.filePrefix + ".dbf"));
        this.shp = new SHPParser(new File(this.filePrefix + ".shp"));
    }

    // ------------------------------------------------------------------------

    public void addField(Field fld)
    {
        this.dbf.addField(fld);
    }

    public void addStringField(String name, int len)
    {
        this.addField(new Field(name, len));
    }

    // ------------------------------------------------------------------------

    public void addPoint(GeoPoint gp, String... fieldValues)
    {
        this.dbf.addRecord(fieldValues);
        this.shp.addShape(new Shape(this.shp.getShapeCount()+1,gp));
        //Print.logInfo("New shape count: " + this.shp.getShapeCount());
    }
    
    public void addPolygon(GeoPolygon gp, String... fieldValues)
    {
        this.dbf.addRecord(fieldValues);
        this.shp.addShape(new Shape(this.shp.getShapeCount()+1,gp));
        //Print.logInfo("New shape count: " + this.shp.getShapeCount());
    }

    // ------------------------------------------------------------------------

    public int getShapeCount()
    {
        return this.shp.getShapeCount();
    }

    public boolean isEmpty()
    {
        return this.shp.isEmpty();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static class ZipOutput
    {
        private ByteArrayOutputStream baos = null;
        private ZipOutputStream       zos  = null;
        public ZipOutput() {
            this.baos = new ByteArrayOutputStream();
            this.zos  = new ZipOutputStream(this.baos);
        }
        public void writeZipEntry(String name, byte data[]) {
            if (this.zos != null) {
                try {
                    ZipEntry ze = new ZipEntry(name);
                    this.zos.putNextEntry(ze);
                    this.zos.write(data, 0, data.length);
                    this.zos.closeEntry();
                } catch (IOException ioe) {
                    // will not occur
                    Print.logException("Unexpected ZipOutput exception", ioe);
                }
            } else {
                Print.logError("ZipOutputStream is closed");
            }
        }
        public void close() {
            if (this.zos != null) {
                try {
                    this.zos.close();
                } catch (IOException ioe) {
                    // will not occur
                    Print.logException("Unexpected ZipOutput exception", ioe);
                }
                this.zos = null;
            }
        }
        public byte[] toByteArray() {
            this.close();
            return this.baos.toByteArray();
        }
        public boolean saveAs(File zipFile) throws IOException {
            if (zipFile == null) {
                throw new IOException("'saveAs' file is null");
            } else
            if (zipFile.exists()) {
                throw new IOException("'saveAs' file already exists: " + zipFile);
            } else
            if (!zipFile.toString().toLowerCase().endsWith(".zip")) {
                throw new IOException("'saveAs' file does not end with '.zip': " + zipFile);
            } else {
                byte zip[] = this.toByteArray();
                return FileTools.writeFile(zip, zipFile);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    public void writeToZipOutput(ZipOutput zo, String entryDirName, String entryFileName)
        throws IOException
    {

        /* invalid Zip output stream? */
        if (zo == null) {
            throw new IOException("No ZipOutput specified");
        }

        /* validate fileName */
        if (StringTools.isBlank(entryFileName)) {
            entryFileName = "shapefile_" + DateTime.getCurrentTimeSec();
        }
        String entryName = StringTools.isBlank(entryDirName)? entryFileName : (entryDirName + "/" + entryFileName);

        /* DBF */
        Payload dbfPayload = new Payload(this.dbf.getFileLength() + 100);
        this.dbf.write(dbfPayload);
        byte dbfBytes[] = dbfPayload.getBytes();

        /* SHP/SHX */
        Payload shpPayload = new Payload(this.shp.getFileLength(false) + 100);
        Payload shxPayload = new Payload(this.shp.getFileLength(true)  + 100);
        this.shp.writeFile(shpPayload, shxPayload);
        byte shpBytes[] = shpPayload.getBytes();
        byte shxBytes[] = shxPayload.getBytes();

        /* zip dbf */
        String dbfName = entryName + ".dbf";
        Print.logInfo("Creating DBF entry: " + dbfName + " [size="+dbfBytes.length+"]");
        zo.writeZipEntry(dbfName, dbfBytes);

        /* zip shp */
        String shpName = entryName + ".shp";
        Print.logInfo("Creating SHP entry: " + shpName + " [size="+shpBytes.length+"]");
        zo.writeZipEntry(shpName, shpBytes);

        /* zip shx */
        String shxName = entryName + ".shx";
        Print.logInfo("Creating SHX entry: " + shxName + " [size="+shxBytes.length+"]");
        zo.writeZipEntry(shxName, shxBytes);

    }

    public byte[] createZipByteArray(String entryDirName, String entryFileName)
    {

        /* write to zip and return byte array */
        try {
            ShapefileSet.ZipOutput zo = new ShapefileSet.ZipOutput();
            this.writeToZipOutput(zo, entryDirName, entryFileName);
            return zo.toByteArray();
        } catch (IOException ioe) {
            return null;
        }

    }

    // ------------------------------------------------------------------------

    public boolean saveAs(File zipFile)
    {

        /* valid file specification */
        if (zipFile == null) {
            Print.logError("Zip file specification is null");
            return false;
        } else
        if (zipFile.exists()) {
            Print.logError("File already exists: " + zipFile);
            return false;
        }

        /* extract name */
        String fileName = zipFile.getName();
        int p = fileName.lastIndexOf(".");
        String entryName = (p > 0)? fileName.substring(0,p) : this.filePrefix;
        if (StringTools.isBlank(entryName)) {
            entryName = "shapefile";
        }

        /* get zip bytes */
        try {
            byte zip[] = this.createZipByteArray(null,entryName);
            boolean ok = FileTools.writeFile(zip, zipFile);
            if (ok) {
                Print.logInfo("Shapefile created: " + zipFile);
            } else {
                Print.logError("Unable to create shapefile: " + zipFile);
            }
            return ok;
        } catch (IOException ioe) {
            Print.logException("Error creating shapefile", ioe);
            return false;
        }

    }

    // ------------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("-----------------------------------------------\n");
        if (this.dbf != null) {
            sb.append(this.dbf.toString());
        }
        if (this.shp != null) {
            sb.append(this.shp.toString());
        }
        sb.append("-----------------------------------------------\n");
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final String ARG_FILE[]       = new String[] { "file" };
    public static final String ARG_ENTRY_NAME[] = new String[] { "name" };
    public static final String ARG_SAVEAS[]     = new String[] { "saveAs", "save" };
    
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        String filePfx = RTConfig.getString(ARG_FILE,null);

        /* file */
        if (filePfx == null) {
            Print.sysPrintln("ERROR: missing 'file' specification");
            System.exit(99);
        }

        /* ShapefileSet */
        ShapefileSet sfs = null;
        try {
            sfs = new ShapefileSet(filePfx);
        } catch (IOException ioe) {
            Print.logException("Loading ShapefileSet zip file", ioe);
            System.exit(99);
        }
        /* list */
        Print.sysPrintln(sfs.toString());

        /* save as */
        String saveAsFile = RTConfig.getString(ARG_SAVEAS,null);
        if (!StringTools.isBlank(saveAsFile)) {
            String entryName = RTConfig.getString(ARG_ENTRY_NAME,"test");
            try {
                byte zipBytes[] = sfs.createZipByteArray(null, entryName);
                File zipFile = new File("/tmp/" + entryName + ".zip");
                Print.sysPrintln("Saving Zip file: " + zipFile);
                FileTools.writeFile(zipBytes, zipFile);
            } catch (IOException ioe) {
                Print.logException("Creating ShapefileSet zip file", ioe);
                System.exit(1);
            }
            System.exit(0);
        }

    }

}
