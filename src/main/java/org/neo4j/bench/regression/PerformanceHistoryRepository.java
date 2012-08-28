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
import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.neo4j.bench.domain.RunResult;
import org.neo4j.bench.domain.RunResultSet;
import org.neo4j.bench.domain.filter.VersionFilters;

/**
 * Abstracts storage of performance history. Currently saves history in one json file per performance test run,
 * to simplify for external tools reading the data and generating charts and reports.
 */
public class PerformanceHistoryRepository
{

    private static class HistoryFileNaming
    {

        private File location;

        public HistoryFileNaming(File historyLocation)
        {
            this.location = historyLocation;
        }

        public String extractTestedVersion( String filename )
        {
            return filename.split( "(?<=[^-])-" )[0];
        }

        public File forResult( RunResult result )
        {
            return new File( location, result.getTimestamp().getTime() + "-" + result.getTestedVersion() + ".json");
        }
    }

    private File location;
    private ObjectMapper jsonMapper = new ObjectMapper();
    private ObjectWriter jsonWriter = jsonMapper.defaultPrettyPrintingWriter();

    private HistoryFileNaming fileNaming;

    public PerformanceHistoryRepository( String locationPath )
    {
        this.location = new File(locationPath);
        if(!location.exists())
        {
            location.mkdirs();
        }

        fileNaming = new HistoryFileNaming( location );
    }

    public void save( RunResult result )
    {
        try
        {
            jsonWriter.writeValue( fileNaming.forResult( result ), result );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    public RunResultSet getResultsForGAReleases()
    {
       return getResults().filter( VersionFilters.GA_ONLY );
    }

    public RunResultSet getResults()
    {
        BufferedReader reader = null;
        RunResultSet runResult = new RunResultSet();

        if ( location.exists() )
        {
            for(String path : location.list())
            {
                try
                {
                    runResult.add( jsonMapper.readValue( new File(location, path), RunResult.class ) );
                }
                catch ( IOException e )
                {
                    throw new RuntimeException( e );
                }
            }
        }
        return runResult;
    }

    /**
     * Dump performance history to a single file.
     * @param output
     */
    public void dumpTo(File output)
    {
        try
        {
            if(!output.exists())
            {
                if(output.getAbsoluteFile().getParentFile() != null)
                    output.getAbsoluteFile().getParentFile().mkdirs();
                output.createNewFile();
            }

            jsonWriter.writeValue( output, getResults() );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }


}
