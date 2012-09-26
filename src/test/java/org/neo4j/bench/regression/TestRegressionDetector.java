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

import org.junit.Test;
import org.neo4j.bench.domain.CaseResult;
import org.neo4j.bench.domain.RunResult;
import org.neo4j.bench.domain.RunResultSet;
import org.neo4j.bench.domain.Unit;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class TestRegressionDetector
{
    private Unit unit = new Unit("Some unit");
    @Test
    public void shouldDetectRegression() throws Exception
    {
        // Given
        RegressionDetector detector = new RegressionDetector(0.1);

        RunResult oldAndBetterResult = new RunResult( "1.0", new Date( 337, 0, 1 ), "http://build/1" );
        oldAndBetterResult.addResult( new CaseResult( "Perftest 1", new CaseResult.Metric("Fastness metric", 10.0, unit, true) ) );

        RunResult oldOkResult = new RunResult( "1.0", new Date( 337, 0, 1 ), "http://build/2");
        oldOkResult.addResult( new CaseResult( "Perftest 1", new CaseResult.Metric("Fastness metric", 8.0, unit, true) ) );


        RunResult newAndShittyResult = new RunResult( "1.1", new Date( 337, 0, 1 ), "http://build/3");
        newAndShittyResult.addResult( new CaseResult( "Perftest 1", new CaseResult.Metric("Fastness metric", 1.0, unit, true) ) );

        RunResultSet historicResults = new RunResultSet( oldAndBetterResult, oldOkResult );

        // When
        RegressionReport report = detector.detectRegression( historicResults, newAndShittyResult );

        // Then
        assertThat(report.regressionDetected(), is(true));
        assertThat(report.toString(), containsString(
                "REGRESSION REPORT\n" +
                "-----------------\n" +
                "Tested version 1.1 on Sun Jan 01 00:00:00 CET 2237.\n" +
                "http://build/3\n" +
                "1 metric(s) have regressed.\n" +
                "\n" +
                "Case: 'Perftest 1'\n" +
                "  Metric: 'Fastness metric' has regressed since version 1.0 (http://build/1)\n" +
                "    Was: 10.0000\n" +
                "    Is now: 1.0000\n" +
                "    (Needs to be at least 9.0000)\n" ) );
    }

    @Test
    public void shouldNotDetectRegressionBelowThreshold() throws Exception
    {
        // Given
        RegressionDetector detector = new RegressionDetector( 1.0 );

        RunResult oldOkResult = new RunResult( "1.0", new Date( 337, 0, 1 ), "http://build/2");
        oldOkResult.addResult( new CaseResult( "Perftest 1", new CaseResult.Metric("Fastness metric", 10.0, unit, true) ) );


        RunResult newResult = new RunResult( "1.1", new Date( 337, 0, 1 ), "http://build/3");
        newResult.addResult( new CaseResult( "Perftest 1", new CaseResult.Metric( "Fastness metric", 1.0, unit, true ) ) );

        RunResultSet historicResults = new RunResultSet( oldOkResult );

        // When
        RegressionReport report = detector.detectRegression( historicResults, newResult );

        // Then
        assertThat( "no regression should have been detected", report.regressionDetected(), is( false ) );
    }
}
