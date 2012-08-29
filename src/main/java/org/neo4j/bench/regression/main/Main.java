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
package org.neo4j.bench.regression.main;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.bench.cases.BenchmarkCase;
import org.neo4j.bench.cases.mixedload.MixedLoadBenchCase;
import org.neo4j.bench.domain.RunResult;
import org.neo4j.bench.regression.PerformanceHistoryRepository;
import org.neo4j.bench.regression.RegressionDetector;
import org.neo4j.bench.regression.RegressionReport;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSetting;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.helpers.Args;

/* @SuppressWarnings( "restriction" ) // for the signal */
public class Main
{

    public static final String OPS_PER_SECOND_FILE_ARG = "ops-per-sec-file";
    public static final String CHART_FILE_ARG = "chart-file";

    public static void main( String[] args ) throws Exception
    {
        // Arguments
        Args argz = new Args( args );
        long timeToRun = Long.parseLong( argz.get( "time-to-run", "60" ) ); /* Time in minutes */
        String chartFilename = argz.get( CHART_FILE_ARG, "chart.png" );
        double threshold = Double.parseDouble( argz.get( "threshold", "0.1" ) );
        String neoVersion = argz.get( "neo4j-version", "N/A" );
        String buildUrl = argz.get( "build-url", "Unknown build url" );
        boolean onlyCompareToGAReleases = Boolean.parseBoolean( argz.get( "only-compare-to-ga", "true" ) ); /* Compare performance only to GA releases */

        // Components
        final GraphDatabaseService db = createDb();
        PerformanceHistoryRepository history = new PerformanceHistoryRepository(argz.get(OPS_PER_SECOND_FILE_ARG, "ops-per-second"));
        RegressionDetector regressionDetector = new RegressionDetector(threshold);

        // Benchmark
        BenchmarkCase [] benchmarks = new BenchmarkCase[] {
            new MixedLoadBenchCase( timeToRun, db )
        };

        RunResult results = new RunResult(neoVersion, new Date(), buildUrl);
        for(BenchmarkCase benchCase : benchmarks)
        {
            results.addResult( benchCase.run() );
        }

        // TODO: Each test should be expected to set up and tear down its own infrastructure
        db.shutdown();
        //ConsistencyCheck.main("db");          // Commented out to allow testing against 1.7

        // Save results
        history.save( results );
        history.dumpTo( new File( "performance-history.json" ) );

        // Check for regression
        RegressionReport regressionReport = regressionDetector.detectRegression( history.getResults(), results );

        if(regressionReport.regressionDetected())
        {
            System.out.println(regressionReport);
            System.exit(1);
        }
    }

    private static GraphDatabaseService createDb()
    {
        Map<String, String> props = new HashMap<String, String>();
        props.put( GraphDatabaseSettings.use_memory_mapped_buffers.name(), GraphDatabaseSetting.TRUE );
        return new GraphDatabaseFactory().newEmbeddedDatabaseBuilder( "db" ).
                setConfig( GraphDatabaseSettings.use_memory_mapped_buffers, GraphDatabaseSetting.TRUE ).
                loadPropertiesFromFile( "../config.props" ).
                newGraphDatabase();
    }

    /*
    * Commented out because it breaks windows but it is nice to have for
    * testing on real OSes
   SignalHandler handler = new SignalHandler()
   {
       @Override
       public void handle( Signal arg0 )
       {
           System.out.println( "Queued nodes currently : "
                   + myCase.getNodeQueue().size() );
       }
   };
   // SIGUSR1 is used by the JVM and INT, ABRT and friends
   // are all defined for specific usage by POSIX. While SIGINT
   // is conveniently issued by Ctrl-C, SIGUSR2 is for user defined
   // behavior so this is what I use.

   Signal signal = new Signal( "USR2" );
   Signal.handle( signal, handler );
    */
}
