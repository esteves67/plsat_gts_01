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

public class Field
{
    
    // ------------------------------------------------------------------------

    public static final int  DATATYPE_STRING        = (int)'C';
    public static final int  DATATYPE_DATE          = (int)'D';
    public static final int  DATATYPE_FLOAT         = (int)'F';
    public static final int  DATATYPE_DOUBLE        = (int)'N';
    public static final int  DATATYPE_BOOLEAN       = (int)'L';
    public static final int  DATATYPE_MEMO          = (int)'M';
    public static final int  DATATYPE_TIMESTAMP     = (int)'@';

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String TRIM(String str)
    {
        String s = StringTools.trim(str);
        int z = s.indexOf((char)0);
        if (z >= 0) {
            s = s.substring(0,z);
        }
        return s;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String  name        = "";
    private int     dataType    = 0; // C=String, D=Date, F=Float, N=Double, L=Boolean, M=?
    private int     length      = 0;
    private int     decimal     = 0;
    private int     workAreaID  = 0;
    private int     flags       = 0;
    private int     index       = 0;
    
    public Field(Payload p) 
    {
        this.name       = TRIM(p.readString(11,false));
        this.dataType   = (int)p.readULong(1,0L);
        p.readSkip(4);
        this.length     = (int)p.readULong(1,0L,false);
        this.decimal    = (int)p.readULong(1,0L);
        p.readSkip(2);
        this.workAreaID = (int)p.readULong(1,0L);
        p.readSkip(2);
        this.flags      = (int)p.readULong(1,0L);
        p.readSkip(7);
        this.index      = (int)p.readULong(1,0L);
    }
    
    public Field(String name, int type, int len, int dec, int waid, int flags, int ndx) 
    {
        this.name       = TRIM(name);
        this.dataType   = type;
        this.length     = len;
        this.decimal    = dec;
        this.workAreaID = waid;
        this.flags      = flags;
        this.index      = ndx;
    }
    
    public Field(String name, int len) 
    {
        this.name       = TRIM(name);
        this.dataType   = DATATYPE_STRING;
        this.length     = len;
        this.decimal    = 0;
        this.workAreaID = 0;
        this.flags      = 0;
        this.index      = 0;
    }

    // ------------------------------------------------------------------------

    public String getName() 
    {
        return this.name;
    }

    // ------------------------------------------------------------------------

    public int getDataType() 
    {
        return this.dataType;
    }

    // ------------------------------------------------------------------------

    public int getLength()
    {
        return this.length;
    }

    // ------------------------------------------------------------------------

    public int getDecimal() 
    {
        return this.decimal;
    }

    // ------------------------------------------------------------------------

    public int getWorkAreaID() 
    {
        return this.workAreaID;
    }

    // ------------------------------------------------------------------------

    public int getFlags() 
    {
        return this.flags;
    }

    // ------------------------------------------------------------------------

    public int getIndex() 
    {
        return this.index;
    }

    // ------------------------------------------------------------------------

    public String toString() 
    {
        StringBuffer sb = new StringBuffer();
        int dt = this.getDataType();
        sb.append( "name=").append(this.getName());
        sb.append(" type=").append(dt).append(",").append(((dt > ' ') && (dt < 127))? (char)dt : '?');
        sb.append(" len=" ).append(this.getLength());
        if ((dt == DATATYPE_FLOAT) || (dt == DATATYPE_DOUBLE) || (this.getDecimal() > 0)) {
            sb.append(" dec=" ).append(this.getDecimal());
        }
        if (this.getWorkAreaID() != 0) {
            sb.append(" waid=").append(this.getWorkAreaID());
        }
        if (this.getFlags() != 0) {
            sb.append(" flag=").append(this.getFlags());
        }
        if (this.getIndex() != 0) {
            sb.append(" ndx=" ).append(this.getIndex());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    public Payload write(Payload p) 
    {
        p.writeString(this.name, 11, false);
        p.writeULong((long)this.dataType, 1);
        p.writeZeroFill(4);
        p.writeULong((long)this.length, 1);
        p.writeULong((long)this.decimal, 1);
        p.writeZeroFill(2);
        p.writeULong((long)this.workAreaID, 1);
        p.writeZeroFill(2);
        p.writeULong((long)this.flags, 1);
        p.writeZeroFill(7);
        p.writeULong((long)this.index, 1);
        return p;
    }

    // ------------------------------------------------------------------------

}
