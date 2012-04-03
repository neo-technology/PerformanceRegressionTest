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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.bench.cases.mixedload.MixedLoadBenchCase;
import org.neo4j.bench.cases.mixedload.Stats;
import org.neo4j.bench.chart.GenerateOpsPerSecChart;
import org.neo4j.helpers.Args;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.configuration.Config;

/* @SuppressWarnings( "restriction" ) // for the signal */
public class Main
{
    public static void main( String[] args ) throws Exception
    {
        Args argz = new Args( args );
        long timeToRun = Long.parseLong( argz.get( "time-to-run", "120" ) );  // Time in minutes
        Map<String, String> props = new HashMap<String, String>();
        props.put( Config.USE_MEMORY_MAPPED_BUFFERS, "true" );
        final EmbeddedGraphDatabase db = new EmbeddedGraphDatabase( "db" );
        final MixedLoadBenchCase myCase = new MixedLoadBenchCase( timeToRun );
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
        myCase.run( db );
        db.shutdown();
        
        // 
        // Handle test results
        //
        
        double[] results = myCase.getResults();
        String statsFileName = argz.get(GenerateOpsPerSecChart.OPS_PER_SECOND_FILE_ARG, "ops-per-second");
        String chartFilename = argz.get( GenerateOpsPerSecChart.CHART_FILE_ARG, "chart.png" );
        double threshold = Double.parseDouble( argz.get( "threshold", "0.05" ) );
        String neoVersion = argz.get( "neo4j-version", "N/A" );
        
        appendNewStatsToFile(results, statsFileName, neoVersion);
        
        GenerateOpsPerSecChart aggregator = new GenerateOpsPerSecChart(statsFileName, chartFilename, threshold );
        
        aggregator.process();
        
        aggregator.generateChart();
        
        if(aggregator.performanceHasDegraded()) {
            Stats trumpStats = aggregator.getTrumpingStats();
            Stats currentStats = aggregator.getLatestStats();

            double trumpReads = trumpStats.getAvgReadsPerSec();
            double trumpWrites = trumpStats.getAvgWritePerSec();

            double currentReads = currentStats.getAvgReadsPerSec();
            double currentWrites = currentStats.getAvgWritePerSec();
            
            System.out.println();
            System.out.println("================ FAILURE ================");
            System.out.println("Stastically significant performance degradation detected, see chart for comparison to older runs.");
            System.out.println();
            if(trumpReads > currentReads) {
                System.out.println("Avg. read performance for " + trumpStats.getName() + " : " + trumpReads + " reads/second" );
                System.out.println("Avg. read performance for " + currentStats.getName() + " (now) : " + currentReads + " reads/second" );
                System.out.println();
            }
            if(trumpWrites > currentWrites) {
                System.out.println("Avg. write performance for " + trumpStats.getName() + " : " + trumpWrites + " writes/second" );
                System.out.println("Avg. write performance for " + currentStats.getName() + " (now) : " + currentWrites + " writes/second" );
                System.out.println();
            }
            System.out.println("=========================================");
            
            System.exit(1);
        }
    }

    private static void appendNewStatsToFile(double[] results, String statsFileName, String neoVersion) throws FileNotFoundException {
        Stats newStats = new Stats(
                new SimpleDateFormat( "MM-dd HH:mm" ).format( new Date() ) + " [" + neoVersion + "]" );
        newStats.setAvgReadsPerSec( results[0] );
        newStats.setAvgWritePerSec( results[1] );
        newStats.setPeakReadsPerSec( results[2] );
        newStats.setPeakWritesPerSec( results[3] );
        newStats.setSustainedReadsPerSec( results[4] );
        newStats.setSustainedWritesPerSec( results[5] );


        PrintStream opsPerSecOutFile = new PrintStream( new FileOutputStream(
                statsFileName, true ) );
        newStats.write( opsPerSecOutFile, true );
    }
}
