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

public class Range
{

    // ------------------------------------------------------------------------

    private double Min  = 0.0;
    private double Max  = 0.0;
    
    public Range(Range r) 
    {
        if (r == null) {
            Print.logError("Invalid SHP data length: missing range");
        }
        this.Min = r.Min;
        this.Max = r.Max;
    }
    
    public Range(Payload p) 
    {
        if (p.getAvailableReadLength() < 16) { 
            Print.logError("Invalid SHP data length: missing range");
        }
        this.Min = p.readDouble(8, 0.0, false);
        this.Max = p.readDouble(8, 0.0, false);
    }
    
    public Range(double min, double max) 
    {
        this.Min = min;
        this.Max = max;
    }

    // ------------------------------------------------------------------------

    public double getMin() 
    {
        return this.Min;
    }
    
    public double getMax() 
    {
        return this.Max;
    }

    // ------------------------------------------------------------------------

    public Payload write(Payload p) 
    {
        p.writeDouble(this.getMin(),8,false);
        p.writeDouble(this.getMax(),8,false);
        return p;
    }

    // ------------------------------------------------------------------------

    public String toString() 
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Min=").append(this.getMin());
        sb.append(", ");
        sb.append("Max=").append(this.getMax());
        return sb.toString();
    }

    // ------------------------------------------------------------------------

}
