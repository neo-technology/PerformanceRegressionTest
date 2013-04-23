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
package org.neo4j.bench.regression;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.neo4j.bench.domain.CaseResult.MetricComparer.BIGGER_IS_BETTER;
import static org.neo4j.bench.domain.CaseResult.MetricComparer.SMALLER_IS_BETTER;

import java.util.Date;

import org.junit.Test;
import org.neo4j.bench.domain.CaseResult;
import org.neo4j.bench.domain.RunResult;
import org.neo4j.bench.domain.RunResultSet;
import org.neo4j.bench.domain.Unit;
import org.neo4j.bench.domain.filter.VersionFilter;

public class TestRegressionDetector
{
    private Unit unit = new Unit("Some unit");

    @Test
    public void shouldDetectRegressionInBiggerIsBetterMetric() throws Exception
    {
        // Given
        RegressionDetector detector = new RegressionDetector(0.1, VersionFilter.GA_ONLY );

        RunResult oldAndBetterResult = runResult("1.0", new Date( 337, 0, 1 ), "http://build/1", 10.0, BIGGER_IS_BETTER);
        RunResult oldOkResult = runResult( "1.0", new Date( 337, 0, 1 ), "http://build/2", 8.0, BIGGER_IS_BETTER );
        RunResult newAndShittyResult = runResult("1.1", new Date( 337, 0, 1 ), "http://build/3", 1.0, BIGGER_IS_BETTER);

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
        RegressionDetector detector = new RegressionDetector( 0.5, VersionFilter.GA_ONLY );

        RunResult oldOkResult = runResult("1.0", new Date( 337, 0, 1 ), "http://build/2", 10.0,  BIGGER_IS_BETTER);
        RunResult newResult = runResult( "1.1", new Date( 337, 0, 1 ), "http://build/3", 6.0, BIGGER_IS_BETTER );

        RunResultSet historicResults = new RunResultSet( oldOkResult );

        // When
        RegressionReport report = detector.detectRegression( historicResults, newResult );

        // Then
        assertThat( "no regression should have been detected", report.regressionDetected(), is( false ) );
    }

    @Test
    public void shouldDetectRegressionInSmallerIsBetterMetric() throws Exception
    {
        // Given
        RegressionDetector detector = new RegressionDetector(0.1, VersionFilter.GA_ONLY );

        RunResult oldAndBetterResult = runResult("1.0", new Date( 337, 0, 1 ), "http://build/1", 1.0, SMALLER_IS_BETTER);
        RunResult oldOkResult = runResult( "1.0", new Date( 337, 0, 1 ), "http://build/2", 1.1, SMALLER_IS_BETTER );
        RunResult newAndShittyResult = runResult("1.1", new Date( 337, 0, 1 ), "http://build/3", 10.0, SMALLER_IS_BETTER);

        RunResultSet historicResults = new RunResultSet( oldAndBetterResult, oldOkResult );

        // When
        RegressionReport report = detector.detectRegression( historicResults, newAndShittyResult );

        // Then
        assertThat( report.regressionDetected(), is( true ) );
        assertThat(report.toString(), containsString(
                "REGRESSION REPORT\n" +
                        "-----------------\n" +
                        "Tested version 1.1 on Sun Jan 01 00:00:00 CET 2237.\n" +
                        "http://build/3\n" +
                        "1 metric(s) have regressed.\n" +
                        "\n" +
                        "Case: 'Perftest 1'\n" +
                        "  Metric: 'Fastness metric' has regressed since version 1.0 (http://build/1)\n" +
                        "    Was: 1.0000\n" +
                        "    Is now: 10.0000\n" +
                        "    (Needs to be at least 1.1000)\n" ) );
    }

    @Test
    public void shouldNotDetectRegressionBelowThresholdForSmallerIsBetterMetric() throws Exception
    {
        // Given
        RegressionDetector detector = new RegressionDetector( 0.5, VersionFilter.GA_ONLY );

        RunResult oldOkResult = runResult("1.0", new Date( 337, 0, 1 ), "http://build/2", 1.0,  SMALLER_IS_BETTER);
        RunResult newResult = runResult( "1.1", new Date( 337, 0, 1 ), "http://build/3", 1.4,    SMALLER_IS_BETTER );

        RunResultSet historicResults = new RunResultSet( oldOkResult );

        // When
        RegressionReport report = detector.detectRegression( historicResults, newResult );

        // Then
        assertThat( "no regression should have been detected", report.regressionDetected(), is( false ) );
    }

    @Test
    public void shouldOnlyDetectRegressionAgainstStableReleases() throws Exception
    {
        // Given
        RegressionDetector detector = new RegressionDetector( 0.1, VersionFilter.GA_ONLY );

        RunResult oldAwesomeSnapshotResult = runResult("1.0-SNAPSHOT", new Date( 337, 0, 1 ), "http://build/2", 1.0, SMALLER_IS_BETTER);
        RunResult oldBadStableResult = runResult( "1.0", new Date( 337, 0, 1 ), "http://build/3", 10.0, SMALLER_IS_BETTER );
        RunResult oldAwesomeReleaseResult = runResult( "RELEASE", new Date( 337, 0, 1 ), "http://build/3", 1.1, SMALLER_IS_BETTER );

        RunResultSet historicResults = new RunResultSet( oldAwesomeSnapshotResult, oldBadStableResult, oldAwesomeReleaseResult );

        RunResult okNewResult = runResult( "1.1-SNAPSHOT", new Date( 337, 0, 1 ), "http://build/3", 5.0, SMALLER_IS_BETTER );

        // When
        RegressionReport report = detector.detectRegression( historicResults, okNewResult );

        // Then
        assertThat( "no regression should have been detected", report.regressionDetected(), is( false ) );
    }


    private RunResult runResult( String version, Date date, String buildUrl, double value, CaseResult.MetricComparer
            metricComparer )
    {
        RunResult result = new RunResult( version, date, buildUrl );
        result.addResult( new CaseResult( "Perftest 1", new CaseResult.Metric( "Fastness metric", value,
                unit, true, metricComparer ) ) );
        return result;
    }
}
