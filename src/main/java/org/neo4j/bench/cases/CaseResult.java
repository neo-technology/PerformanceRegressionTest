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
package org.neo4j.bench.cases;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * In memory representation of a set of statistics from a bench run. Comparable
 * with its kind for sorting purposes and performance degradation detection.
 * Provides methods for reading in and writing out from/to file.
 *
 * TODO: Make this a generic Benchmark statistics result, capable of containing one or more named metrics.
 *
 */
public class CaseResult implements Comparable<CaseResult>
{
    private final String name;
    private double avgReadsPerSec;
    private double avgWritePerSec;
    private double peakReadsPerSec;
    private double peakWritesPerSec;
    private double sustainedReadsPerSec;
    private double sustainedWritesPerSec;
    private String neoVersion;
    private Date date;

    public CaseResult( String name )
    {
        this.name = name;
    }

    public CaseResult( String name, double avgReadsPerSec, double avgWritesPerSec,
                       double peakReadsPerSec, double peakWritesPerSec,
                       double sustainedReadsPerSec, double sustainedWritesPerSec )
    {
        this.name = name;
        this.avgReadsPerSec = avgReadsPerSec;
        this.avgWritePerSec = avgWritesPerSec;
        this.peakReadsPerSec = peakReadsPerSec;
        this.peakWritesPerSec = peakWritesPerSec;
        this.sustainedReadsPerSec = sustainedReadsPerSec;
        this.sustainedWritesPerSec = sustainedWritesPerSec;
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

    @Override
    public int compareTo( CaseResult o )
    {
        // NPE on purpose
        return this.name.compareTo( o.name );
    }

    public boolean isGARelease()
    {
        String version = neoVersion.toLowerCase();
        return !version.contains("-snapshot") && !version.contains("m") && !version.contains("rc") && !version.equals("n/a");
    }

    public void setNeoVersion( String neoVersion )
    {
        this.neoVersion = neoVersion;
    }

    public void setDate( Date date )
    {
        this.date = date;
    }

    // TODO: Replace below with either JSON serialization or a neo database.
    // Using a neo database is tricky, since the code needs to run against multiple different versions
    // of neo4j. Could be done tho.

    public String serialize()
    {
        return String.format( "%s\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f",
                new SimpleDateFormat( "MM-dd HH:mm" ).format( date ) + " [" + neoVersion + "]",
                avgReadsPerSec, avgWritePerSec, peakReadsPerSec,
                peakWritesPerSec, sustainedReadsPerSec, sustainedWritesPerSec );
    }

    public static CaseResult deserialize( String serialized )
    {
        CaseResult result = null;
        String dateAndVersionToken, // The current version token
                readsToken, // The current reads per second token
                writesToken, // The current writes per second token
                peakReadsToken, // The current peak reads token
                peakWritesToken, // The current peak writes token
                sustainedReadsToken, // The current peak reads token
                sustainedWritesToken // The current peak writes token
                        ;
        // The double values of the corresponding tokens
        double reads, writes, peakReads, peakWrites, sustainedReads, sustainedWrites;
        StringTokenizer tokenizer = new StringTokenizer( serialized, "\t" );
        if ( tokenizer.countTokens() < 7 )
        {
            return null;
        }
        // Grab the tokens
        dateAndVersionToken = tokenizer.nextToken();
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

        result = new CaseResult( dateAndVersionToken ); // TODO: The name field should store the name of the benchmark, not the date and version
        result.neoVersion = parseVersion( dateAndVersionToken );
        result.avgReadsPerSec = reads;
        result.avgWritePerSec = writes;
        result.peakReadsPerSec = peakReads;
        result.peakWritesPerSec = peakWrites;
        result.sustainedReadsPerSec = sustainedReads;
        result.sustainedWritesPerSec = sustainedWrites;
        return result;
    }

    private static String parseVersion( String dateAndVersionString ) {
        // 03-12 06:12 [1.7-SNAPSHOT]
        String [] parts = dateAndVersionString.split("\\[");
        if(parts.length == 2) {
            String version = parts[1].substring(0, parts[1].length() - 1);
            return version;
        }
        return "N/A";
    }

    public String getNeoVersion()
    {
        return neoVersion;
    }

}
