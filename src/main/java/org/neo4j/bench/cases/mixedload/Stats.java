/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.bench.cases.mixedload;

import java.io.PrintStream;
import java.util.StringTokenizer;

/**
 * In memory representation of a set of statistics from a bench run. Comparable
 * with its kind for sorting purposes and performance degradation detection.
 * Provides methods for reading in and writing out from/to file.
 */
public class Stats implements Comparable<Stats>
{
    private final String name;
    private double avgReadsPerSec;
    private double avgWritePerSec;
    private double peakReadsPerSec;
    private double peakWritesPerSec;
    private double sustainedReadsPerSec;
    private double sustainedWritesPerSec;

    public Stats( String name )
    {
        this.name = name;
    }

    public double getAvgReadsPerSec()
    {
        return avgReadsPerSec;
    }

    public void setAvgReadsPerSec( double avgReadsPerSec )
    {
        this.avgReadsPerSec = avgReadsPerSec;
    }

    public double getAvgWritePerSec()
    {
        return avgWritePerSec;
    }

    public void setAvgWritePerSec( double avgWritePerSec )
    {
        this.avgWritePerSec = avgWritePerSec;
    }

    public double getPeakReadsPerSec()
    {
        return peakReadsPerSec;
    }

    public void setPeakReadsPerSec( double peakReadsPerSec )
    {
        this.peakReadsPerSec = peakReadsPerSec;
    }

    public double getPeakWritesPerSec()
    {
        return peakWritesPerSec;
    }

    public void setPeakWritesPerSec( double peakWritesPerSec )
    {
        this.peakWritesPerSec = peakWritesPerSec;
    }

    public double getSustainedReadsPerSec()
    {
        return sustainedReadsPerSec;
    }

    public void setSustainedReadsPerSec( double sustainedReadsPerSec )
    {
        this.sustainedReadsPerSec = sustainedReadsPerSec;
    }

    public double getSustainedWritesPerSec()
    {
        return sustainedWritesPerSec;
    }

    public void setSustainedWritesPerSec( double sustainedWritesPerSec )
    {
        this.sustainedWritesPerSec = sustainedWritesPerSec;
    }

    public String getName()
    {
        return name;
    }

    public void write( PrintStream out, boolean newLine )
    {
        out.print( String.format( "%s\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f",
                name, avgReadsPerSec, avgWritePerSec, peakReadsPerSec,
                peakWritesPerSec, sustainedReadsPerSec, sustainedWritesPerSec ) );
        if ( newLine )
        {
            out.println();
        }
    }

    @Override
    public int compareTo( Stats o )
    {
        // NPE on purpose
        return this.name.compareTo( o.name );
    }

    public static Stats parse( String line )
    {
        Stats result = null;
        String nameToken, // The current version token
        readsToken, // The current reads per second token
        writesToken, // The current writes per second token
        peakReadsToken, // The current peak reads token
        peakWritesToken, // The current peak writes token
        sustainedReadsToken, // The current peak reads token
        sustainedWritesToken // The current peak writes token
        ;
        // The double values of the corresponding tokens
        double reads, writes, peakReads, peakWrites, sustainedReads, sustainedWrites;
        StringTokenizer tokenizer = new StringTokenizer( line, "\t" );
        if ( tokenizer.countTokens() < 7 )
        {
            return null;
        }
        // Grab the tokens
        nameToken = tokenizer.nextToken();
        readsToken = tokenizer.nextToken();
        writesToken = tokenizer.nextToken();
        peakReadsToken = tokenizer.nextToken();
        peakWritesToken = tokenizer.nextToken();
        sustainedReadsToken = tokenizer.nextToken();
        sustainedWritesToken = tokenizer.nextToken();
        // Parse the integer values, check for validity
        try
        {
            reads = Double.valueOf( readsToken );
            writes = Double.valueOf( writesToken );
            peakReads = Double.valueOf( peakReadsToken );
            peakWrites = Double.valueOf( peakWritesToken );
            sustainedReads = Double.valueOf( sustainedReadsToken );
            sustainedWrites = Double.valueOf( sustainedWritesToken );
        }
        catch ( NumberFormatException e )
        {
            // This is stupid but there is no other way
            return null;
        }
        result = new Stats( nameToken );
        result.avgReadsPerSec = reads;
        result.avgWritePerSec = writes;
        result.peakReadsPerSec = peakReads;
        result.peakWritesPerSec = peakWrites;
        result.sustainedReadsPerSec = sustainedReads;
        result.sustainedWritesPerSec = sustainedWrites;
        return result;
    }

    public boolean isGARelease()
    {
        String version = getVersion().toLowerCase();
        return !version.contains("-snapshot") && !version.contains("m") && !version.contains("rc") && !version.equals("n/a");
    }
    
    private String getVersion() {
        // 03-12 06:12 [1.7-SNAPSHOT]
        String [] parts = name.split("\\[");
        if(parts.length == 2) {
            String version = parts[1].substring(0, parts[1].length() - 1);
            return version;
        }
        return "N/A";
    }
}
