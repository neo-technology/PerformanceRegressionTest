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

import static org.neo4j.bench.domain.CaseResult.MetricComparer.BIGGER_IS_BETTER;
import static org.neo4j.bench.domain.Units.CORE_API_READ;
import static org.neo4j.bench.domain.Units.CORE_API_WRITE_TRANSACTION;
import static org.neo4j.bench.domain.Units.MILLISECOND;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.neo4j.bench.cases.BenchmarkCase;
import org.neo4j.bench.cases.mixedload.workers.BulkCreateWorker;
import org.neo4j.bench.cases.mixedload.workers.BulkReaderWorker;
import org.neo4j.bench.cases.mixedload.workers.CreateWorker;
import org.neo4j.bench.cases.mixedload.workers.DeleteWorker;
import org.neo4j.bench.cases.mixedload.workers.PropertyAddWorker;
import org.neo4j.bench.cases.mixedload.workers.SampleReadWorker;
import org.neo4j.bench.domain.CaseResult;
import org.neo4j.bench.domain.Unit;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSetting;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

/**
 * The main driver for the operation performer threads. Keeps the probabilities
 * with which each thread type is launched and aggregates the results of their
 * runs.
 */
public class MixedLoadBenchCase implements BenchmarkCase
{
    private GraphDatabaseService graphDb;

    private static enum WorkerType
    {
        SIMPLE,
        BULK
    }

    private static final Unit TX_PER_MS    = CORE_API_WRITE_TRANSACTION.per( MILLISECOND );
    private static final Unit READS_PER_MS = CORE_API_READ.per( MILLISECOND );

    private static final int PrintEvery = 500;

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

    private long concurrentFinishTime;

    public MixedLoadBenchCase( long timeToRun )
    {
        simpleTasks = new LinkedList<Future<int[]>>();
        bulkTasks = new LinkedList<Future<int[]>>();
        this.timeToRun = timeToRun;
        nodes = new ConcurrentLinkedQueue<Node>();
        readTasksExecuted = 0;
        writeTasksExecuted = 0;
    }

    public Queue<Node> getNodeQueue()
    {
        return nodes;
    }

    @Override
    public void setUp()
    {
        Map<String, String> props = new HashMap<String, String>();
        props.put( GraphDatabaseSettings.use_memory_mapped_buffers.name(), GraphDatabaseSetting.TRUE );
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder( "db" ).
                setConfig( GraphDatabaseSettings.use_memory_mapped_buffers, GraphDatabaseSetting.TRUE ).
                loadPropertiesFromFile( "../config.props" ).
                newGraphDatabase();
    }

    @Override
    public void tearDown()
    {
        graphDb.shutdown();
    }

    public CaseResult run( )
    {
        Random r = new Random();
        // Outside of measured stuff, just to populate the db
        try
        {
            new BulkCreateWorker( graphDb, nodes, 25000 ).call();
            new PropertyAddWorker( graphDb, nodes, 2500, true ).call();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        startTime = System.currentTimeMillis();

        runConcurrentLoad( graphDb, r );
        concurrentFinishTime = System.currentTimeMillis();
        runBulkLoad( graphDb, r );

        printOutResults( "Final results" );

        return createResults();
    }

    private void runConcurrentLoad( GraphDatabaseService graphDb, Random r )
    {
        int print = 0;
        int maxThreads = Runtime.getRuntime().availableProcessors() + 2;

        ExecutorService service = Executors.newFixedThreadPool( maxThreads,
                new ThreadFactory()
                {
                    @Override
                    public Thread newThread( Runnable r )
                    {
                        Thread thread = new Thread( r );
                        thread.setDaemon( true );
                        return thread;
                    }
                } );
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
                if ( print++ % PrintEvery == 0 )
                {
                    printOutResults( "Intermediate results for simple" );
                }
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
                        nodes, 7000 ) ) );
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
                if ( print++ % PrintEvery == 0 )
                {
                    printOutResults( "Intermediate results for Bulk" );
                }
            }
            catch ( InterruptedException e )
            {
                // wut?
                e.printStackTrace();
            }
        }

        service.shutdown();
        System.out.println( "Service shut-down complete" );
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
                    int[] taskRes = task.get( 5, TimeUnit.SECONDS );
                    totalReads += taskRes[0];
                    totalWrites += taskRes[1];
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
                    e.printStackTrace();
                }
                catch ( TimeoutException e )
                {
                    System.err.println( "Task failed to terminate: " + task );
                    e.printStackTrace();
                }
                finally
                {
                    it.remove();
                }

            }
        }
    }

    private CaseResult createResults()
    {
        double avgReads  = totalReads  * 1.0 / ( concurrentFinishTime - startTime );
        double avgWrites = totalWrites * 1.0 / ( concurrentFinishTime - startTime );

        return new CaseResult(getClass().getSimpleName(),
                new CaseResult.Metric("Average reads", avgReads,          READS_PER_MS, /* track regression = */ true, BIGGER_IS_BETTER ),
                new CaseResult.Metric("Sustained reads", sustainedReads,  READS_PER_MS, BIGGER_IS_BETTER  ),
                new CaseResult.Metric("Peak reads", peakReads,            READS_PER_MS, BIGGER_IS_BETTER  ),

                new CaseResult.Metric("Average writes", avgWrites,        TX_PER_MS, /* track regression = */ true, BIGGER_IS_BETTER ),
                new CaseResult.Metric("Sustained writes", sustainedReads, TX_PER_MS, BIGGER_IS_BETTER ),
                new CaseResult.Metric("Peak writes", peakReads,           TX_PER_MS, BIGGER_IS_BETTER ));
    }

    private void printOutResults( String header )
    {
        System.out.println( header );
        System.out.println( "Average reads: "
                            + totalReads
                            * 1.0
                            / ( ( concurrentFinishTime == 0 ? System.currentTimeMillis()
                                    : concurrentFinishTime ) - startTime ) );
        System.out.println( "Average writes: "
                            + totalWrites
                            * 1.0
                            / ( ( concurrentFinishTime == 0 ? System.currentTimeMillis()
                                    : concurrentFinishTime ) - startTime ) );
        System.out.println( "Peak reads per ms: " + peakReads );
        System.out.println( "Peak writes per ms: " + peakWrites );
        System.out.println( "Peak Sustained reads per ms: " + sustainedReads );
        System.out.println( "Peak Sustained writes per ms: " + sustainedWrites );
        System.out.println();
    }
}
