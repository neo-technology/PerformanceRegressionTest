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

import java.util.Date;

import org.junit.Test;
import org.neo4j.bench.domain.RunResult;

public class TestRegressionReport
{

    @Test
    public void shouldPrintNiceReportIfNoRegressionsOccurred()
    {
        // Given
        RegressionReport report = new RegressionReport(new RunResult( "1.0", new Date( 1012, 0, 1 ), "http://build.com" ));

        // When
        String output = report.toString();

        // Then
        assertThat(output, is(
                "REGRESSION REPORT\n" +
                "-----------------\n" +
                "Tested version 1.0 on Fri Jan 01 00:00:00 CET 2912.\n" +
                "http://build.com\n" +
                "All metrics are within allowed thresholds.\n"));
    }

}
