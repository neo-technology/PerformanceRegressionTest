/**
 * Copyright (c) 2002-2013 "Neo Technology,"
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
package org.neo4j.bench.cases.cypher;

import static java.lang.System.currentTimeMillis;
import static org.neo4j.bench.PrepopulatedGraphDatabaseFactory.create;
import static org.neo4j.bench.domain.CaseResult.MetricComparer.SMALLER_IS_BETTER;

import java.util.ArrayList;
import java.util.Map;

import org.neo4j.bench.GraphDatabaseAndUnderlyingStore;
import org.neo4j.bench.PrepopulatedGraphDatabaseFactory;
import org.neo4j.bench.cases.BenchmarkCase;
import org.neo4j.bench.domain.CaseResult;
import org.neo4j.bench.domain.Units;
import org.neo4j.cypher.javacompat.ExecutionEngine;

public class CineastsQueriesBenchmark implements BenchmarkCase
{
    public static final String SINGLE_PATH_WITH_MANY_START_POINTS_QUERY =
            "START lisa=node:Person(\"name:Lisa*\"), " +
            "      kevin=node(759)\n" +
            "match lisa-[:ACTS_IN]->movie<-[:ACTS_IN]-()-[:ACTS_IN]->movie2<-[:ACTS_IN]-kevin\n" +
            "RETURN count(*)";


    private GraphDatabaseAndUnderlyingStore dbWithStore;
    private ExecutionEngine cypher;

    private ArrayList<CaseResult.Metric> metrics = new ArrayList<CaseResult.Metric>();

    @Override
    public void setUp()
    {
        dbWithStore = create( PrepopulatedGraphDatabaseFactory.DataSet.CINEASTS );
        cypher = new ExecutionEngine( dbWithStore.database );
    }

    @Override
    public void tearDown()
    {
        dbWithStore.tearDown();
    }

    @Override
    public CaseResult run()
    {
        benchmarkQuery( "Single path with many start points", SINGLE_PATH_WITH_MANY_START_POINTS_QUERY, 100 );

        return new CaseResult( getClass().getSimpleName(), metrics.toArray( new CaseResult.Metric[metrics.size()] ) );
    }

    private void benchmarkQuery( String name, String query, int timesToRun )
    {
        long begin = currentTimeMillis();

        for(int i=0; i<timesToRun; i++)
        {
            // Iterate to make cypher run the query
            for ( Map<String, Object> resultRow : cypher.execute( query ) )
            {
                // Ignore
            }
        }

        long end = currentTimeMillis();

        double deltaTime = (end - begin*1.0 ) / timesToRun;
        metrics.add( new CaseResult.Metric( "Average for: " + name, deltaTime, Units.MILLISECOND, /* track regression = */ true, SMALLER_IS_BETTER  ) );
    }
}
