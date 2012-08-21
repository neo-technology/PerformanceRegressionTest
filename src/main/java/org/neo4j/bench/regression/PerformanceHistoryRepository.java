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
package org.neo4j.bench.regression;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.neo4j.bench.cases.CaseResult;

public class PerformanceHistoryRepository
{
    private String location;

    public PerformanceHistoryRepository( String location )
    {
        this.location = location;
    }

    public void save( CaseResult result )
    {
        PrintStream historyFile = null;
        try
        {
            historyFile = new PrintStream( new FileOutputStream(
                    location, true ) );
            historyFile.println( result.serialize() );
        }
        catch ( FileNotFoundException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            if(historyFile != null) historyFile.close();
        }
    }

    public SortedSet<CaseResult> getResultsForGAReleases()
    {
        SortedSet<CaseResult> results = getAllResults();
        Iterator<CaseResult> resultIter = results.iterator();
        while(resultIter.hasNext())
        {
            if(!resultIter.next().isGARelease())
            {
                resultIter.remove();
            }
        }
        return results;
    }

    public SortedSet<CaseResult> getAllResults()
    {
        BufferedReader reader = null;
        SortedSet<CaseResult> result = new TreeSet<CaseResult>();
        CaseResult currentStat = null;

        File dataFile = new File( location );
        if ( !dataFile.exists() )
        {
            return result;
        }
        try
        {
            reader = new BufferedReader( new FileReader( dataFile ) );
            String line; // The current line
            while ( ( line = reader.readLine() ) != null )
            {
                currentStat = CaseResult.deserialize( line );
                if ( currentStat != null )
                {
                    result.add( currentStat );
                }
            }

            // Add the latest result, even if it was not a GA
            if(currentStat != null && !currentStat.isGARelease()) {
                result.add(currentStat);
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            if ( reader != null )
            {
                try
                {
                    reader.close();
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
