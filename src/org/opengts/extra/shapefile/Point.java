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

public class Point
    implements GeoPointProvider
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns Point array for single GeoPoint
    **/
    public static Point[] getPoints(GeoPoint gp)
    {
        if (gp != null) {
            return new Point[] { new Point(gp) };
        } else {
            return new Point[0];
        }
    }

    /**
    *** Returns Point array for an array of GeoPoints
    **/
    public static Point[] getPoints(GeoPoint... gp)
    {
        if (gp != null) {
            Point pt[] = new Point[gp.length];
            for (int i = 0; i < gp.length; i++) {
                pt[i] = new Point(gp[i]);
            }
            return pt;
        } else {
            return new Point[0];
        }
    }

    /**
    *** Returns Point array of the GeoPoints contained within the GeoPolygon
    **/
    public static Point[] getPoints(GeoPolygon gp)
    {
        if (gp != null) {
            return Point.getPoints(gp.getGeoPoints());
        } else {
            return new Point[0];
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private double  X       = 0.0;  // longitude
    private double  Y       = 0.0;  // latitude
    private boolean hasZ    = false;
    private double  Z       = 0.0;
    private boolean hasM    = false;
    private double  M       = 0.0;
    
    public Point(Point p) 
    {
        if (p == null) { 
            Print.logError("Invalid SHP data length: missing point");
        }
        this.X    = p.X;
        this.Y    = p.Y;
        this.hasZ = p.hasZ;
        this.Z    = p.Z;
        this.hasM = p.hasM;
        this.M    = p.M;
    }
    
    public Point(Payload p) 
    {
        if (p.getAvailableReadLength() < 16) { 
            Print.logError("Invalid SHP data length: missing point");
        }
        this.X = p.readDouble(8, 0.0, false);
        this.Y = p.readDouble(8, 0.0, false);
    }
    
    public Point(double x, double y) 
    {
        this.X = x;
        this.Y = y;
    }
    
    public Point(GeoPoint gp) 
    {
        this.X = (gp != null)? gp.getX() : 0.0;
        this.Y = (gp != null)? gp.getY() : 0.0;
    }

    // ------------------------------------------------------------------------

    public double getX() 
    {
        return this.X; // longitude
    }
    
    public double getLongitude() 
    {
        return this.getX(); // longitude
    }

    // ------------------------------------------------------------------------

    public double getY() 
    {
        return this.Y; // latitude
    }
    
    public double getLatitude() {
        return this.getY(); // latitude
    }

    // ------------------------------------------------------------------------

    public GeoPoint getGeoPoint() 
    {
        return new GeoPoint(this.getLatitude(), this.getLongitude());
    }

    // ------------------------------------------------------------------------

    public void setZ(double z) 
    {
        this.Z    = z;
        this.hasZ = true;
    }
    
    public boolean hasZ() 
    {
        return this.hasZ;
    }
    
    public double getZ() 
    {
        return this.Z;
    }

    // ------------------------------------------------------------------------

    public void setM(double m) 
    {
        this.M    = m;
        this.hasM = true;
    }
    
    public boolean hasM() 
    {
        return this.hasM;
    }
    
    public double getM() 
    {
        return this.M;
    }

    // ------------------------------------------------------------------------

    public Payload write(Payload p) 
    {
        //Print.logInfo("Writing point X=" + this.getX() + ", Y=" + this.getY());
        p.writeDouble(this.getX(),8,false);
        p.writeDouble(this.getY(),8,false);
        return p;
    }
    
    public Payload writeZ(Payload p) 
    {
        if (this.hasZ()) {
            p.writeDouble(this.getZ(),8,false);
        }
        return p;
    }
    
    public Payload writeM(Payload p) 
    {
        if (this.hasM()) {
            p.writeDouble(this.getM(),8,false);
        }
        return p;
    }

    // ------------------------------------------------------------------------

    public String toString() 
    {
        StringBuffer sb = new StringBuffer();
        sb.append("X=").append(this.getX());
        sb.append(", ");
        sb.append("Y=").append(this.getY());
        if (this.hasZ()) {
            sb.append(", ");
            sb.append("Z=").append(this.getZ());
        }
        if (this.hasM()) {
            sb.append(", ");
            sb.append("M=").append(this.getM());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

}
