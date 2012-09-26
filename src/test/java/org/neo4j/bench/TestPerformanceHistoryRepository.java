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

import org.junit.Before;
import org.junit.Test;
import org.neo4j.bench.domain.CaseResult;
import org.neo4j.bench.domain.RunResult;
import org.neo4j.bench.domain.RunResultSet;
import org.neo4j.bench.domain.Unit;
import org.neo4j.bench.regression.PerformanceHistoryRepository;
import org.neo4j.kernel.impl.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
        results.addResult( new CaseResult( "Hello", new CaseResult.Metric("The metric name", 1337.0, unit) )  );
        repo.save( results );

        assertThat(new File( "target/test/historyfilter/" + timestamp.getTime() + "-1.3.37.json" ).exists(), is(true));

    }

    @Test
    public void shouldLoadResults()
    {
        // Given
        PerformanceHistoryRepository repo = new PerformanceHistoryRepository( historyDir.getPath() );

        RunResult results = new RunResult( "1.3.37", new Date( ), "http://build/1");
        results.addResult( new CaseResult( "Hello", new CaseResult.Metric("The metric name", 1337.0, unit) )  );

        repo.save( results );
        repo.save( new RunResult( "1.3.37-SNAPSHOT", new Date( ), "http://build/1") );

        // When
        RunResultSet retrievedResults = repo.getResults();
        RunResultSet retrievedGAResults = repo.getResultsForGAReleases();

        // Then
        assertThat( retrievedResults.size(), is( 2 ) );
        assertThat( retrievedGAResults.size(), is( 1 ) );
    }
}
