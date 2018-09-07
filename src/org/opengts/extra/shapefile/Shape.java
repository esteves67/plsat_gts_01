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

import org.opengts.util.*;

public class Shape
{

    public static final int MAX_SHAPE_SIZE              = 60000;

    // ------------------------------------------------------------------------

    public static final int SHAPETYPE_UNDEFINED         = -1;
    public static final int SHAPETYPE_NULL              =  0;
    public static final int SHAPETYPE_POINT             =  1;
    public static final int SHAPETYPE_POLYLINE          =  3;
    public static final int SHAPETYPE_POLYGON           =  5;
    public static final int SHAPETYPE_MULTIPOINT        =  8;
    public static final int SHAPETYPE_POINT_Z           = 11;
    public static final int SHAPETYPE_POLYLINE_Z        = 13;
    public static final int SHAPETYPE_POLYGON_Z         = 15;
    public static final int SHAPETYPE_MULTIPOINT_Z      = 18;
    public static final int SHAPETYPE_POINT_M           = 21;
    public static final int SHAPETYPE_POLYLINE_M        = 23;
    public static final int SHAPETYPE_POLYGON_M         = 25;
    public static final int SHAPETYPE_MULTIPOINT_M      = 28;
    public static final int SHAPETYPE_MULTIPATCH        = 31;
    
    public static String GetShapeTypeDescription(int shapeType)
    {
        switch (shapeType) {
            case SHAPETYPE_NULL         : return "Null";
            case SHAPETYPE_POINT        : return "Point";
            case SHAPETYPE_POLYLINE     : return "Polyline";
            case SHAPETYPE_POLYGON      : return "Polygon";
            case SHAPETYPE_MULTIPOINT   : return "MultiPoint";
            case SHAPETYPE_POINT_Z      : return "Point-Z";
            case SHAPETYPE_POLYLINE_Z   : return "Polyline-Z";
            case SHAPETYPE_POLYGON_Z    : return "Polygon-Z";
            case SHAPETYPE_MULTIPOINT_Z : return "MultiPoint-Z";
            case SHAPETYPE_POINT_M      : return "Point-M";
            case SHAPETYPE_POLYLINE_M   : return "Polyline-M";
            case SHAPETYPE_POLYGON_M    : return "Polygon-M";
            case SHAPETYPE_MULTIPOINT_M : return "MultiPoint-M";
            case SHAPETYPE_MULTIPATCH   : return "MultiPatch";
        }
        return "?UNKNOWN?";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final int PARTTYPE_TRIANGLE_STRIP     = 0;
    public static final int PARTTYPE_TRIANGLE_FAN       = 1;
    public static final int PARTTYPE_OUTER_RING         = 2;
    public static final int PARTTYPE_INNER_RING         = 3;
    public static final int PARTTYPE_FIRST_RING         = 4;
    public static final int PARTTYPE_RING               = 5;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static GeoPolygon[] CreateGeoPolygon(Point shapePoints[], int shapeParts[])
    {
    
        /* no points? */
        if (shapePoints == null) {
            return null;
        }
    
        /* validate parts */
        int parts[] = !ListTools.isEmpty(shapeParts)? shapeParts : new int[] { 0 };
        if (parts[0] != 0) {
            Print.logWarn("Polygon Parts does not begin at '0': " + StringTools.join(parts,","));
            return null;
        }
        for (int i = 1; i < parts.length; i++) {
            if (parts[i] <= parts[i-1]) {
                Print.logError("Invalid 'Parts': " + StringTools.join(parts,","));
                return null;
            }
            if (parts[i] >= shapePoints.length) {
                Print.logError("'Parts' beyond end of Points: " + StringTools.join(parts,","));
                return null;
            }
        }
    
        /* parse polygons */
        java.util.List<GeoPolygon> polyOuter = new Vector<GeoPolygon>();
        java.util.List<GeoPolygon> polyInner = new Vector<GeoPolygon>();
        for (int p = 0; p < parts.length; p++) {
            int ps = parts[p];
            int pe = ((p + 1) < parts.length)? parts[p+1] : shapePoints.length;
            Point ringPoints[] = new Point[pe - ps];
            System.arraycopy(shapePoints, ps, ringPoints, 0, ringPoints.length);
            GeoPolygon poly = new GeoPolygon(ringPoints);
            if (poly.isClockwise()) {
                polyOuter.add(poly);
            } else {
                polyInner.add(poly);
            }
        }
        
        /* analyze polygons */
        if (polyOuter.size() == 0) {
            // no polygons?
            if (polyInner.size() != 0) {
                Print.logError("Found 'inner' polygons, but no 'outer' polygons!");
            }
        } else
        if (polyInner.size() == 0) {
            // nothing to do, just continue
        } else
        if (polyOuter.size() == 1) {
            // simple case
            polyOuter.get(0).addNegativeRings(polyInner);
        } else {
            for (GeoPolygon inner : polyInner) {
                GeoPoint gp = inner.getGeoPoint(0);
                for (GeoPolygon outer : polyOuter) {
                    if (outer.containsPoint(gp)) {
                        outer.addNegativeRing(inner);
                        gp = null;
                        break;
                    }
                }
                if (gp != null) {
                    Print.logError("No outer polygon found for inner polygon");
                }
            }
        }
        
        return polyOuter.toArray(new GeoPolygon[polyOuter.size()]);
    
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int         rcdIndex         = 0;
    private int         shapeType        = 0;
    private BoundingBox shapeBB          = null;
    private Point       shapePoints[]    = null;
    private int         shapeParts[]     = null;
    private int         shapePartTypes[] = null;
    private Range       shapeZRange      = null;
    private Range       shapeMRange      = null;

    private GeoPolygon  shapePolygon[]   = null;
    
    private Shape()
    {
        this.rcdIndex         = 0;
        this.shapeType        = 0;
        this.shapeBB          = null;
        this.shapePoints      = null;
        this.shapeParts       = null; // index into shapePoints
        this.shapePartTypes   = null;
        this.shapeMRange      = null;
        this.shapeZRange      = null;
        this.shapePolygon     = null;
    }

    public Shape(Shape s) 
    {
        this();
        this.rcdIndex         = s.rcdIndex;
        this.shapeType        = s.shapeType;
        this.shapeBB          = s.shapeBB;
        this.shapePoints      = s.shapePoints;
        this.shapeParts       = s.shapeParts;
        this.shapePartTypes   = s.shapePartTypes;
        this.shapeMRange      = s.shapeMRange;
        this.shapeZRange      = s.shapeZRange;
        this.shapePolygon     = s.shapePolygon;
    }

    public Shape(Payload p)
    {
        this();
        this.rcdIndex = (int)p.readLong(4, 0L); // record index
        long len      = p.readLong(4, 0L);      // in 16 bit words (x2 for bytes)
        byte cb[]     = p.readBytes((int)len * 2);
        Payload c     = new Payload(cb);
        /* header */
        //Print.sysPrintln("------------------------------------------");
        //Print.sysPrintln("Record: " + rcd + " [length="+(len*2)+"]");
        //Print.sysPrintln("  Data=> 0x"+ StringTools.toHexString(cb));
        /* accumulators */
        this.shapeType        = (int)c.readLong(4, 0L, false);    // shape type
        this.shapeBB          = null;
        this.shapePoints      = null;
        this.shapeParts       = null;
        this.shapePartTypes   = null;
        this.shapeMRange      = null;
        this.shapeZRange      = null;
        this.shapePolygon     = null;
        /* shape type */
        switch (this.shapeType) {
            case SHAPETYPE_NULL         : {
                    // nothing else expected
                } break;
            case SHAPETYPE_POINT        : {
                    this.shapePoints = new Point[] { new Point(c) };
                } break;
            case SHAPETYPE_POLYLINE     : {
                    this.shapeBB = new BoundingBox(c);
                    int numParts  = (int)c.readLong(4, 0L, false);
                    int numPoints = (int)c.readLong(4, 0L, false);
                    this.shapeParts = new int[numParts];
                    for (int i = 0; i < numParts; i++) {
                        this.shapeParts[i] = (int)c.readLong(4, 0L, false);
                    }
                    this.shapePoints = new Point[numPoints];
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i] = new Point(c);
                    }
                } break;
            case SHAPETYPE_POLYGON      : {
                    this.shapeBB = new BoundingBox(c);
                    int numParts  = (int)c.readLong(4, 0L, false);
                    int numPoints = (int)c.readLong(4, 0L, false);
                    this.shapeParts = new int[numParts];
                    for (int i = 0; i < numParts; i++) {
                        this.shapeParts[i] = (int)c.readLong(4, 0L, false);
                    }
                    this.shapePoints = new Point[numPoints];
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i] = new Point(c);
                    }
                } break;
            case SHAPETYPE_MULTIPOINT   : {
                    this.shapeBB = new BoundingBox(c);
                    int numPoints = (int)c.readLong(4, 0L, false);
                    this.shapePoints = new Point[numPoints];
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i] = new Point(c);
                    }
                } break;
            // Z
            case SHAPETYPE_POINT_Z      : {
                    this.shapePoints = new Point[] { new Point(c) };
                    this.shapePoints[0].setM(c.readDouble(8, 0.0, false));
                    this.shapePoints[0].setZ(c.readDouble(8, 0.0, false));
                } break;
            case SHAPETYPE_POLYLINE_Z   : {
                    this.shapeBB = new BoundingBox(c);
                    int numParts  = (int)c.readLong(4, 0L, false);
                    int numPoints = (int)c.readLong(4, 0L, false);
                    this.shapeParts = new int[numParts];
                    for (int i = 0; i < numParts; i++) {
                         this.shapeParts[i] = (int)c.readLong(4, 0L, false);
                    }
                    this.shapePoints = new Point[numPoints];
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i] = new Point(c);
                    }
                    this.shapeZRange = new Range(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].setZ(c.readDouble(8, 0.0, false));
                    }
                    this.shapeMRange = new Range(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].setM(c.readDouble(8, 0.0, false));
                    }
                } break;
            case SHAPETYPE_POLYGON_Z    : {
                    this.shapeBB = new BoundingBox(c);
                    int numParts  = (int)c.readLong(4, 0L, false);
                    int numPoints = (int)c.readLong(4, 0L, false);
                    this.shapeParts = new int[numParts];
                    for (int i = 0; i < numParts; i++) {
                        this.shapeParts[i] = (int)c.readLong(4, 0L,false);
                    }
                    this.shapePoints = new Point[numPoints];
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i] = new Point(c);
                    }
                    this.shapeZRange = new Range(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].setZ(c.readDouble(8, 0.0, false));
                    }
                    this.shapeMRange = new Range(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].setM(c.readDouble(8, 0.0, false));
                    }
                } break;
            case SHAPETYPE_MULTIPOINT_Z : {
                    this.shapeBB = new BoundingBox(c);
                    int numPoints = (int)c.readLong(4, 0L, false);
                    this.shapePoints = new Point[numPoints];
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i] = new Point(c);
                    }
                    this.shapeZRange = new Range(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].setZ(c.readDouble(8, 0.0, false));
                    }
                    this.shapeMRange = new Range(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].setM(c.readDouble(8, 0.0, false));
                    }
                } break;
            // M
            case SHAPETYPE_POINT_M      : {
                    this.shapePoints = new Point[] { new Point(c) };
                    this.shapePoints[0].setM(c.readDouble(8, 0.0, false));
               } break;
            case SHAPETYPE_POLYLINE_M   : {
                    this.shapeBB = new BoundingBox(c);
                    int numParts  = (int)c.readLong(4, 0L, false);
                    int numPoints = (int)c.readLong(4, 0L, false);
                    this.shapeParts = new int[numParts];
                    for (int i = 0; i < numParts; i++) {
                        this.shapeParts[i] = (int)c.readLong(4, 0L, false);
                    }
                    this.shapePoints = new Point[numPoints];
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i] = new Point(c);
                    }
                    this.shapeMRange = new Range(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].setM(c.readDouble(8, 0.0, false));
                    }
                } break;
            case SHAPETYPE_POLYGON_M    : {
                    this.shapeBB = new BoundingBox(c);
                    int numParts  = (int)c.readLong(4, 0L, false);
                    int numPoints = (int)c.readLong(4, 0L, false);
                    this.shapeParts = new int[numParts];
                    for (int i = 0; i < numParts; i++) {
                        this.shapeParts[i] = (int)c.readLong(4, 0L, false);
                    }
                    this.shapePoints = new Point[numPoints];
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i] = new Point(c);
                    }
                    this.shapeMRange = new Range(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].setM(c.readDouble(8, 0.0, false));
                    }
                } break;
            case SHAPETYPE_MULTIPOINT_M : {
                    this.shapeBB = new BoundingBox(c);
                    int numPoints = (int)c.readLong(4, 0L, false);
                    this.shapePoints = new Point[numPoints];
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i] = new Point(c);
                    }
                    this.shapeMRange = new Range(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].setM(c.readDouble(8, 0.0, false));
                    }
                } break;
            // Patch
            case SHAPETYPE_MULTIPATCH   : {
                    this.shapeBB = new BoundingBox(c);
                    int numParts  = (int)c.readLong(4, 0L, false);
                    int numPoints = (int)c.readLong(4, 0L, false);
                    this.shapeParts = new int[numParts];
                    for (int i = 0; i < numParts; i++) {
                        this.shapeParts[i] = (int)c.readLong(4, 0L, false);
                    }
                    this.shapePartTypes = new int[numParts];
                    for (int i = 0; i < numParts; i++) {
                        this.shapePartTypes[i] = (int)c.readLong(4, 0L, false);
                    }
                    this.shapePoints = new Point[numPoints];
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i] = new Point(c);
                    }
                    this.shapeZRange = new Range(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].setZ(c.readDouble(8, 0.0, false));
                    }
                    this.shapeMRange = new Range(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].setM(c.readDouble(8, 0.0, false));
                    }
                } break;
            default: 
                Print.logError("Unrecognized shape type: " + shapeType);
                break;
        }
        if (this.shapeParts != null) {
            this.shapePolygon = CreateGeoPolygon(this.shapePoints, this.shapeParts);
        }
    }

    public Shape(int rcdNdx, int type, 
        BoundingBox bb, Point pp[], 
        int parts[], int partTypes[], 
        Range mRange, Range zRange) 
    {
        this();
        this.rcdIndex         = rcdNdx;
        this.shapeType        = type;
        this.shapeBB          = bb;
        this.shapePoints      = pp;
        this.shapeParts       = parts;
        this.shapePartTypes   = partTypes;
        this.shapeMRange      = mRange;
        this.shapeZRange      = zRange;
        if (this.shapeParts != null) {
            this.shapePolygon = CreateGeoPolygon(this.shapePoints, this.shapeParts);
        }
    }

    /**
    *** Constructor
    *** Initialize with single GeoPoint
    **/
    public Shape(int rcdNdx, GeoPoint gp) 
    {
        this();
        this.rcdIndex         = rcdNdx;
        this.shapeType        = SHAPETYPE_POINT;
        this.shapeBB          = null;
        this.shapePoints      = Point.getPoints(gp);
        this.shapeParts       = null;
        this.shapePartTypes   = null;
        this.shapeMRange      = null;
        this.shapeZRange      = null;
        this.shapePolygon     = null;
    }

    /**
    *** Constructor
    *** Initialize with single GeoPolygon
    **/
    public Shape(int rcdNdx, GeoPolygon gp) 
    {
        this();
        this.rcdIndex         = rcdNdx;
        this.shapeType        = SHAPETYPE_POLYGON;
        this.shapeBB          = BoundingBox.getBoundingBox(gp);
        this.shapePoints      = Point.getPoints(gp);
        this.shapeParts       = new int[] { 0 };
        this.shapePartTypes   = null;
        this.shapeMRange      = null;
        this.shapeZRange      = null;
        this.shapePolygon     = CreateGeoPolygon(this.shapePoints, this.shapeParts);
      //this.shapePolygon     = new GeoPolygon[] { gp };
    }

    // ------------------------------------------------------------------------

    public int getRecordIndex() 
    {
        return this.rcdIndex;
    }

    // ------------------------------------------------------------------------

    public int getShapeType() 
    {
        return this.shapeType;
    }

    // ------------------------------------------------------------------------

    public BoundingBox getBoundingBox() 
    {
        return this.shapeBB;
    }

    // ------------------------------------------------------------------------

    public Point[] getShapePoints() 
    {
        return this.shapePoints;
    }

    // ------------------------------------------------------------------------

    public int[] getShapeParts() 
    {
        return this.shapeParts;
    }

    public int[] getShapePartTypes() {
        return this.shapePartTypes;
    }

    // ------------------------------------------------------------------------

    public Range getShapeMRange() {
        return this.shapeMRange;
    }

    public Range getShapeZRange() {
        return this.shapeZRange;
    }

    // ------------------------------------------------------------------------

    public GeoPolygon[] getPolygons() 
    {
        return this.shapePolygon;
    }

    // ------------------------------------------------------------------------

    protected byte[] createShapeBytes() 
    {
        Payload c = this.createShapePayload(true);
        return c.getBytes();
    }

    protected Payload createShapePayload(boolean saveData) 
    {
        Payload c = saveData? new Payload(MAX_SHAPE_SIZE) : new Payload(-1);
        c.writeLong(this.shapeType,4,false);
        switch (this.shapeType) {
            case SHAPETYPE_NULL         : {
                    // nothing else expected
                } break;
            case SHAPETYPE_POINT        : {
                    this.shapePoints[0].write(c);
                } break;
            case SHAPETYPE_POLYLINE     : {
                    this.shapeBB.write(c);
                    int numParts  = this.shapeParts.length;
                    c.writeLong((long)numParts ,4,false);
                    int numPoints = this.shapePoints.length;
                    c.writeLong((long)numPoints,4,false);
                    for (int i = 0; i < numParts; i++) {
                        c.writeLong((long)this.shapeParts[i],4,false);
                    }
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].write(c);
                    }
                } break;
            case SHAPETYPE_POLYGON      : {
                    this.shapeBB.write(c);
                    int numParts  = this.shapeParts.length;
                    c.writeLong((long)numParts ,4,false);
                    int numPoints = this.shapePoints.length;
                    c.writeLong((long)numPoints,4,false);
                    for (int i = 0; i < numParts; i++) {
                        c.writeLong((long)this.shapeParts[i],4,false);
                    }
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].write(c);
                    }
                } break;
            case SHAPETYPE_MULTIPOINT   : {
                    this.shapeBB.write(c);
                    int numPoints = this.shapePoints.length;
                    c.writeLong((long)numPoints,4,false);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].write(c);
                    }
                } break;
            // Z
            case SHAPETYPE_POINT_Z      : {
                    this.shapePoints[0].write(c);
                    this.shapePoints[0].writeM(c);
                    this.shapePoints[0].writeZ(c);
                } break;
            case SHAPETYPE_POLYLINE_Z   : {
                    this.shapeBB.write(c);
                    int numParts  = this.shapeParts.length;
                    c.writeLong((long)numParts ,4,false);
                    int numPoints = this.shapePoints.length;
                    c.writeLong((long)numPoints,4,false);
                    for (int i = 0; i < numParts; i++) {
                        c.writeLong((long)this.shapeParts[i],4,false);
                    }
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].write(c);
                    }
                    this.shapeZRange.write(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].writeZ(c);
                    }
                    this.shapeMRange.write(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].writeM(c);
                    }
                } break;
            case SHAPETYPE_POLYGON_Z    : {
                    this.shapeBB.write(c);
                    int numParts  = this.shapeParts.length;
                    c.writeLong((long)numParts ,4,false);
                    int numPoints = this.shapePoints.length;
                    c.writeLong((long)numPoints,4,false);
                    for (int i = 0; i < numParts; i++) {
                        c.writeLong((long)this.shapeParts[i],4,false);
                    }
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].writeZ(c);
                    }
                    this.shapeZRange.write(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].writeZ(c);
                    }
                    this.shapeMRange.write(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].writeM(c);
                    }
                } break;
            case SHAPETYPE_MULTIPOINT_Z : {
                    this.shapeBB.write(c);
                    int numPoints = this.shapePoints.length;
                    c.writeLong((long)numPoints,4,false);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].write(c);
                    }
                    this.shapeZRange.write(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].writeZ(c);
                    }
                    this.shapeMRange.write(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].writeM(c);
                    }
                } break;
            // M
            case SHAPETYPE_POINT_M      : {
                    this.shapePoints[0].write(c);
                    this.shapePoints[0].writeM(c);
               } break;
            case SHAPETYPE_POLYLINE_M   : {
                    this.shapeBB.write(c);
                    int numParts  = this.shapeParts.length;
                    c.writeLong((long)numParts ,4,false);
                    int numPoints = this.shapePoints.length;
                    c.writeLong((long)numPoints,4,false);
                    for (int i = 0; i < numParts; i++) {
                        c.writeLong((long)this.shapeParts[i],4,false);
                    }
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].write(c);
                    }
                    this.shapeMRange.write(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].writeM(c);
                    }
                } break;
            case SHAPETYPE_POLYGON_M    : {
                    this.shapeBB.write(c);
                    int numParts  = this.shapeParts.length;
                    c.writeLong((long)numParts ,4,false);
                    int numPoints = this.shapePoints.length;
                    c.writeLong((long)numPoints,4,false);
                    for (int i = 0; i < numParts; i++) {
                        c.writeLong((long)this.shapeParts[i],4,false);
                    }
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].write(c);
                    }
                    this.shapeMRange.write(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].writeM(c);
                    }
                } break;
            case SHAPETYPE_MULTIPOINT_M : {
                    this.shapeBB.write(c);
                    int numPoints = this.shapePoints.length;
                    c.writeLong((long)numPoints,4,false);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].write(c);
                    }
                    this.shapeMRange.write(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].writeM(c);
                    }
                } break;
            // Patch
            case SHAPETYPE_MULTIPATCH   : {
                    this.shapeBB.write(c);
                    int numParts  = this.shapeParts.length;
                    c.writeLong((long)numParts ,4,false);
                    int numPoints = this.shapePoints.length;
                    c.writeLong((long)numPoints,4,false);
                    for (int i = 0; i < numParts; i++) {
                        c.writeLong((long)this.shapeParts[i],4,false);
                    }
                    for (int i = 0; i < numParts; i++) {
                        c.writeLong((long)this.shapePartTypes[i],4,false);
                    }
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].write(c);
                    }
                    this.shapeZRange.write(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].writeZ(c);
                    }
                    this.shapeMRange.write(c);
                    for (int i = 0; i < numPoints; i++) {
                        this.shapePoints[i].writeM(c);
                    }
                } break;
            default: 
                Print.logError("Unrecognized shape type: " + shapeType);
                break;
        }
        return c;
    }
    
    public int getShapeLength() 
    {
        Payload c = this.createShapePayload(false);
        return 8 + c.getSize(); // (header + bytes)
    }

    public int writeShape(Payload p, int rcdNdx) 
    {
        byte cb[] = this.createShapeBytes();
        int  rndx = (rcdNdx >= 0)? rcdNdx : this.rcdIndex;
        p.writeLong(rndx,4);
        p.writeLong((long)(cb.length / 2),4);
        p.writeBytes(cb);
        return cb.length;
    }

    // ------------------------------------------------------------------------

    public String toString(StringBuffer sb) 
    {
        if (sb == null) { sb = new StringBuffer(); }
        // Type
        sb.append("  Shape      : [" + this.shapeType + "] " + GetShapeTypeDescription(this.shapeType)).append("\n");
        // Record Index
        sb.append("  Record#    : " + this.rcdIndex).append("\n");
        // Bounding box
        if (this.shapeBB != null) {
            sb.append("  BoundingBox: " + this.shapeBB).append("\n");
        }
        // Z range
        if (this.shapeZRange != null) {
            sb.append("  Z Range: " + this.shapeZRange).append("\n");
        }
        // M range
        if (this.shapeMRange != null) {
            sb.append("  M Range: " + this.shapeMRange).append("\n");
        }
        // Parts?
        if (!ListTools.isEmpty(this.shapeParts)) {
            if (this.shapeParts.length > 1) {
                sb.append("  Parts: " + StringTools.join(this.shapeParts,",")).append("\n");
            }
        } else {
            //sb.append("  Parts: none").append("\n");
        }
        // Part types
        if (!ListTools.isEmpty(this.shapePartTypes)) {
            sb.append("  PartTypes: " + StringTools.join(this.shapePartTypes,",")).append("\n");
        }
        // Count
        if (this.shapePolygon != null) {
            for (int i = 0; i < this.shapePolygon.length; i++) {
                sb.append("  ShapePoly count ["+i+"]: " + this.shapePolygon[i].toString()).append("\n");
            }
        } else
        if (this.shapePoints != null) {
            int max = 2;
            sb.append("  ShapePoints count: " + this.shapePoints.length);
            for (int p = 0; (p < this.shapePoints.length) && (p < max); p++) {
                if (p > 0) { sb.append(", "); }
                sb.append(" (").append(this.shapePoints[p].toString()).append(")");
            }
            if (this.shapePoints.length > max) {
                sb.append(", ...");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    public String toString() {
        return this.toString(new StringBuffer());
    }

    // ------------------------------------------------------------------------

}
