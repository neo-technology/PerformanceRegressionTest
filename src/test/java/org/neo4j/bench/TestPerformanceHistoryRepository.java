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
package org.neo4j.bench;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.neo4j.bench.domain.CaseResult.MetricComparer.BIGGER_IS_BETTER;
import static org.neo4j.bench.domain.CaseResult.MetricComparer.SMALLER_IS_BETTER;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.bench.domain.CaseResult;
import org.neo4j.bench.domain.RunResult;
import org.neo4j.bench.domain.RunResultSet;
import org.neo4j.bench.domain.Unit;
import org.neo4j.bench.regression.PerformanceHistoryRepository;
import org.neo4j.kernel.impl.util.FileUtils;

public class TestPerformanceHistoryRepository
{
    private Unit unit = new Unit("Some unit");
    private File historyDir = new File( "target/test/historyfilter" );

    @Before
    public void clearHistoryDir() throws IOException
    {
        FileUtils.deleteRecursively( historyDir );
    }

    @Test
    public void shouldSaveAndRetrieveSinglePerformanceResult()
    {

        PerformanceHistoryRepository repo = new PerformanceHistoryRepository( historyDir.getAbsolutePath() );

        Date timestamp = new Date( );

        RunResult results = new RunResult( "1.3.37", timestamp, "http://build/1" );
        results.addResult( new CaseResult( "Hello", new CaseResult.Metric("The metric name", 1337.0, unit, BIGGER_IS_BETTER) )  );
        repo.save( results );

        assertThat(new File( "target/test/historyfilter/" + timestamp.getTime() + "-1.3.37.json" ).exists(), is(true));

    }

    @Test
    public void shouldLoadResults()
    {
        // Given
        PerformanceHistoryRepository repo = new PerformanceHistoryRepository( historyDir.getPath() );

        RunResult results = new RunResult( "1.3.37", new Date( ), "http://build/1");
        results.addResult( new CaseResult( "Hello", new CaseResult.Metric("The metric name", 1337.0, unit, BIGGER_IS_BETTER) )  );

        repo.save( results );
        repo.save( new RunResult( "1.3.37-SNAPSHOT", new Date( ), "http://build/1") );

        // When
        RunResultSet retrievedResults = repo.getResults();
        RunResultSet retrievedGAResults = repo.getResultsForGAReleases();

        // Then
        assertThat( retrievedResults.size(), is( 2 ) );
        assertThat( retrievedGAResults.size(), is( 1 ) );
    }

    @Test
    public void shouldStoreAndRetrieveAllParameters() throws Exception
    {
        // Given
        RunResult results = new RunResult( "1.3.37", new Date( ), "http://build/1");
        results.addResult( new CaseResult( "TestCase 1", new CaseResult.Metric("The metric name", 100.0, unit, BIGGER_IS_BETTER) )  );
        results.addResult( new CaseResult( "TestCase 2", new CaseResult.Metric("The smaller name", 100.0, unit, SMALLER_IS_BETTER) )  );

        PerformanceHistoryRepository repo = new PerformanceHistoryRepository( historyDir.getPath() );
        repo.save( results );

        // When
        RunResultSet retrieved = repo.getResults();

        // Then
        Iterator<RunResult> iterator = retrieved.iterator();

        RunResult runResult = iterator.next();
        assertThat( "Should only contain one run result", iterator.hasNext(), is( false ) );

        CaseResult.Metric testcase1Metric = runResult.getMetric( "TestCase 1", "The metric name" );
        assertThat( "should be in 'bigger is better' mode", testcase1Metric.calculateAllowedRegression( 0.2 ), is( 80.0 ) );


        CaseResult.Metric testcase2Metric = runResult.getMetric( "TestCase 2", "The smaller name" );
        assertThat( "should be in 'smaller is better' mode", testcase2Metric.calculateAllowedRegression( 0.2 ), is( 120.0 ) );
    }
}
