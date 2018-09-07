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

public class BoundingBox
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static BoundingBox getBoundingBox(GeoPoint... gp)
    {
        return new BoundingBox(new GeoBounds(gp));
    }

    public static BoundingBox getBoundingBox(GeoPolygon gp)
    {
        return new BoundingBox(new GeoBounds(gp));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private double Xmin = 0.0;
    private double Xmax = 0.0;
    private double Ymin = 0.0;
    private double Ymax = 0.0;

    public BoundingBox(Shape sh) 
    {
        if (sh != null) {
            GeoBounds gb = new GeoBounds();
            gb.extendByPoint(sh.getShapePoints());
            this.Xmin = gb.getMinX();
            this.Ymin = gb.getMinY();
            this.Xmax = gb.getMaxX();
            this.Ymax = gb.getMaxY();
            //Print.logInfo("Shape BB: " + this);
        }
    }
    
    public BoundingBox(Collection<Shape> shapes) 
    {
        if (shapes != null) {
            GeoBounds gb = new GeoBounds();
            for (Shape sh : shapes) {
                Point pts[] = sh.getShapePoints();
                if (!ListTools.isEmpty(pts)) {
                    //Print.logInfo("Point[0]: " + pts[0] + " ==> " + pts[0].getGeoPoint());
                    gb.extendByPoint(pts);
                }
            }
            this.Xmin = gb.getMinX();
            this.Ymin = gb.getMinY();
            this.Xmax = gb.getMaxX();
            this.Ymax = gb.getMaxY();
        }
    }

    public BoundingBox(GeoBounds gb) 
    {
        if (gb != null) {
            this.Xmin = gb.getMinX();
            this.Ymin = gb.getMinY();
            this.Xmax = gb.getMaxX();
            this.Ymax = gb.getMaxY();
        }
    }
    
    public BoundingBox(BoundingBox bb)
    {
        if (bb == null) {
            Print.logError("Invalid SHP data length: missing bounding box");
        }
        this.Xmin = bb.Xmin;
        this.Ymin = bb.Ymin;
        this.Xmax = bb.Xmax;
        this.Ymax = bb.Ymax;
    }
    
    public BoundingBox(Payload p) 
    {
        if ((p == null) || (p.getAvailableReadLength() < 32)) { 
            Print.logError("Invalid SHP data length: missing bounding box");
        }
        this.Xmin = p.readDouble(8,0.0,false);
        this.Ymin = p.readDouble(8,0.0,false);
        this.Xmax = p.readDouble(8,0.0,false);
        this.Ymax = p.readDouble(8,0.0,false);
    }
    
    public BoundingBox(double xmin, double ymin, double xmax, double ymax) 
    {
        this.Xmin = xmin;
        this.Ymin = ymin;
        this.Xmax = xmax;
        this.Ymax = ymax;
    }

    // ------------------------------------------------------------------------

    public double getXMin() 
    {
        return this.Xmin;
    }
    
    public double getXMax() 
    {
        return this.Xmax;
    }
    
    public double getYMin() 
    {
        return this.Ymin;
    }
    
    public double getYMax() 
    {
        return this.Ymax;
    }
    
    public GeoPoint getMinGeoPoint() 
    {
        return new GeoPoint(this.getYMin(), this.getXMin());
    }
    
    public GeoPoint getMaxGeoPoint() 
    {
        return new GeoPoint(this.getYMax(), this.getXMax());
    }

    // ------------------------------------------------------------------------

    public GeoBounds getGeoBounds() 
    {
        return new GeoBounds(this.getMinGeoPoint(), this.getMaxGeoPoint());
    }

    // ------------------------------------------------------------------------

    public Payload write(Payload p) 
    {
        p.writeDouble(this.getXMin(),8,false);
        p.writeDouble(this.getYMin(),8,false);
        p.writeDouble(this.getXMax(),8,false);
        p.writeDouble(this.getYMax(),8,false);
        return p;
    }

    // ------------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("X min/max = " + this.getXMin() + "/" + this.getXMax());
        sb.append(", ");
        sb.append("Y min/max = " + this.getYMin() + "/" + this.getYMax());
        return sb.toString();
    }

    // ------------------------------------------------------------------------

}
