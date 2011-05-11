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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.neo4j.bench.cases.mixedload.workers.BulkCreateWorker;
import org.neo4j.bench.cases.mixedload.workers.CreateWorker;
import org.neo4j.bench.cases.mixedload.workers.DeleteWorker;
import org.neo4j.bench.cases.mixedload.workers.PropertyAddWorker;
import org.neo4j.bench.cases.mixedload.workers.SampleReadWorker;
import org.neo4j.bench.cases.mixedload.workers.BulkReaderWorker;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 * The main driver for the operation performer threads. Keeps the probabilities
 * with which each thread type is launched and aggregates the results of their
 * runs.
 */
public class MixedLoadBenchCase
{
    private enum WorkerType
    {
        SIMPLE,
        BULK
    }

    // Keeps the list of simple workers futures
    private final List<Future<int[]>> simpleTasks;
    // Keeps the list of bulk workers futures
    private final List<Future<int[]>> bulkTasks;
    private int readTasksExecuted;
    private int writeTasksExecuted;
    // The queue of nodes created/deleted
    private final Queue<Node> nodes;

    private long totalReads = 0;
    private long totalWrites = 0;
    // Current max values
    private double peakReads = 0;
    private double peakWrites = 0;
    private double sustainedReads = 0;
    private double sustainedWrites = 0;
    // Time to run, in minutes
    private final long timeToRun;
    private long startTime;

    public MixedLoadBenchCase( long timeToRun )
    {
        simpleTasks = new LinkedList<Future<int[]>>();
        bulkTasks = new LinkedList<Future<int[]>>();
        this.timeToRun = timeToRun;
        nodes = new ConcurrentLinkedQueue<Node>();
        readTasksExecuted = 0;
        writeTasksExecuted = 0;
    }

    public double[] getResults()
    {
        double[] totals = new double[6];
        // totals[0] = totalReads * 1.0 / totalReadTime;
        // totals[1] = totalWrites * 1.0 / totalWriteTime;
        totals[0] = totalReads * 1.0
        / ( System.currentTimeMillis() - startTime );
        totals[1] = totalWrites * 1.0
        / ( System.currentTimeMillis() - startTime );
        totals[2] = peakReads;
        totals[3] = peakWrites;
        totals[4] = sustainedReads;
        totals[5] = sustainedWrites;
        return Arrays.copyOf( totals, totals.length );
    }

    public Queue<Node> getNodeQueue()
    {
        return nodes;
    }

    public void run( GraphDatabaseService graphDb )
    {
        Random r = new Random();
        // Outside of measured stuff, just to populate the db
        try
        {
            new BulkCreateWorker( graphDb, nodes, 100000 ).call();
            System.out.println( "Starting indexing" );
            long beforeIndex = System.currentTimeMillis();
            new PropertyAddWorker( graphDb, nodes, 10000, true ).call();
            System.out.println( "Indexing took "
                    + ( System.currentTimeMillis() - beforeIndex )
                    + " ms" );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        startTime = System.currentTimeMillis();

        runConcurrentLoad( graphDb, r );
        runBulkLoad( graphDb, r );

        printOutResults( "Final results" );
    }

    private void runConcurrentLoad( GraphDatabaseService graphDb, Random r )
    {
        int print = 0;
        int maxThreads = Runtime.getRuntime().availableProcessors() + 2;
        ExecutorService service = Executors.newFixedThreadPool( maxThreads );
        while ( System.currentTimeMillis() - startTime < ( timeToRun * 60 * 1000 * 2 / 3 ) )
        {
            /*
             * With prob 1/8 add some primitives
             * With prob 1/8 add some properties
             * With prob 3/20 delete some primitives
             * With prob 3/5 read some stuff
             */
            double dice = r.nextDouble();
            if ( dice > 0.75 )
            {
                // Half the time start an entity create worker, the rest a
                // property create worker
                if ( dice > 0.825 )
                {
                    simpleTasks.add( service.submit( new CreateWorker( graphDb,
                            nodes, 100 ) ) );
                }
                else
                {
                    simpleTasks.add( service.submit( new PropertyAddWorker(
                            graphDb, nodes, 100, /*write to index */false ) ) );
                }
            }
            else if ( dice > 0.6 )
            {
                simpleTasks.add( service.submit( new DeleteWorker( graphDb,
                        nodes, 20 ) ) );
            }
            else
            {
                simpleTasks.add( service.submit( new SampleReadWorker( graphDb,
                        nodes, 400, /* read from index */true ) ) );
            }
            try
            {
                /*
                 * A (naive?) attempt to keep the number of running threads
                 * bounded
                 */
                while ( simpleTasks.size() > maxThreads - 2 )
                {
                    gatherUp( simpleTasks, WorkerType.SIMPLE, false );
                    Thread.sleep( 100 );
                }
                if ( print++ % 20 == 0 )
                    printOutResults( "Intermediate results for simple" );
            }
            catch ( InterruptedException e )
            {
                // wut?
                e.printStackTrace();
            }
        }
        service.shutdown();
        try
        {
            gatherUp( simpleTasks, WorkerType.SIMPLE, true );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }

    private void runBulkLoad( GraphDatabaseService graphDb, Random r )
    {
        int print = 0;
        ExecutorService service = Executors.newFixedThreadPool( 2 );
        while ( System.currentTimeMillis() - startTime < ( timeToRun * 60 * 1000 ) )
        {
            double dice = r.nextDouble();
            if ( dice > 0.4 )
            {
                bulkTasks.add( service.submit( new BulkReaderWorker( graphDb ) ) );
            }
            else
            {
                bulkTasks.add( service.submit( new BulkCreateWorker( graphDb,
                        nodes, 2000 ) ) );
            }
            try
            {
                /*
                 * A (naive?) attempt to keep the number of running threads
                 * bounded
                 */
                while ( bulkTasks.size() > 2 )
                {
                    gatherUp( bulkTasks, WorkerType.BULK, false );
                    Thread.sleep( 100 );
                }
                if ( print++ % 20 == 0 )
                    printOutResults( "Intermediate results for Bulk" );
            }
            catch ( InterruptedException e )
            {
                // wut?
                e.printStackTrace();
            }
        }
        service.shutdown();
        try
        {
            gatherUp( bulkTasks, WorkerType.BULK, true );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }

    /**
     * @param tasks The list of Futures to gather
     * @param type The type of tasks - used for statistics generation
     * @param sweepUp True if this method should wait for unfinished tasks if
     *            false, not done tasks are skipped
     * @throws InterruptedException
     */
    private void gatherUp( List<Future<int[]>> tasks, WorkerType type,
            boolean sweepUp ) throws InterruptedException
            {
        Iterator<Future<int[]>> it = tasks.iterator();
        while ( it.hasNext() )
        {
            Future<int[]> task = it.next();
            // Short circuit - if sweepUp is true, we gather everything up, else
            // only finished
            if ( sweepUp || task.isDone() )
            {
                try
                {
                    int[] taskRes = task.get();
                    // These are the means for this run
                    double thisReads = taskRes[0]
                                               / ( taskRes[2] == 0 ? 1 : taskRes[2] );
                    double thisWrites = taskRes[1]
                                                / ( taskRes[2] == 0 ? 1 : taskRes[2] );
                    if ( taskRes[0] > 0 )
                    {
                        readTasksExecuted++;
                    }
                    if ( taskRes[1] > 0 )
                    {
                        writeTasksExecuted++;
                    }
                    switch ( type )
                    {
                    case SIMPLE:
                        if ( taskRes[0] > 0 )
                        {
                            totalReads += taskRes[0];
                        }
                        if ( taskRes[1] > 0 )
                        {
                            totalWrites += taskRes[1];
                        }
                        break;
                    case BULK:
                        // Sustained operations must be at least as long as
                        // 9/10ths the average runtime
                        if ( taskRes[2] > ( System.currentTimeMillis() - startTime )
                                * 0.9 / readTasksExecuted )
                        {
                            if ( thisReads > sustainedReads )
                            {
                                sustainedReads = thisReads;
                            }
                            if ( thisWrites > sustainedWrites )
                            {
                                sustainedWrites = thisWrites;
                            }
                        }
                        break;
                    }
                    // The test run for more than 10% of the average time, long
                    // enough for getting a peak value
                    if ( thisReads > peakReads
                            && taskRes[2] > ( ( System.currentTimeMillis() - startTime ) )
                            * 0.1 / readTasksExecuted )
                    {
                        peakReads = thisReads;
                    }
                    if ( thisWrites > peakWrites
                            && taskRes[2] > ( ( System.currentTimeMillis() - startTime ) )
                            * 0.1 / writeTasksExecuted )
                    {
                        peakWrites = thisWrites;
                    }
                }
                catch ( ExecutionException e )
                {
                    // It threw an exception, print and continue
                    e.printStackTrace();
                    continue;
                }
                finally
                {
                    it.remove();
                }

            }
        }
            }

    private void printOutResults( String header )
    {
        System.out.println( header );
        System.out.println( "Average reads: " + totalReads * 1.0
                / ( System.currentTimeMillis() - startTime ) );
        System.out.println( "Average writes: " + totalWrites * 1.0
                / ( System.currentTimeMillis() - startTime ) );
        System.out.println( "Peak reads per ms: " + peakReads );
        System.out.println( "Peak writes per ms: " + peakWrites );
        System.out.println( "Peak Sustained reads per ms: " + sustainedReads );
        System.out.println( "Peak Sustained writes per ms: " + sustainedWrites );
        System.out.println();
    }
}
