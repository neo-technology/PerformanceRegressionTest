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
package org.neo4j.bench;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.neo4j.bench.domain.CaseResult;
import org.neo4j.bench.domain.RunResult;

public class TestBackwardsCompatResultsDeserialization
{

    @Test
    public void shouldDeserializeOldCypherTestToSmallerIsBetter() throws Exception
    {
        // Given
        ObjectMapper jsonMapper = new ObjectMapper();

        // When
        RunResult result = jsonMapper.readValue( "{\n" +
                "  \"timestamp\" : 1349810534349,\n" +
                "  \"results\" : [ {\n" +
                "    \"caseName\" : \"CineastsQueriesBenchmark\",\n" +
                "    \"metrics\" : [ {\n" +
                "      \"name\" : \"Average for: Single path with many start points\",\n" +
                "      \"value\" : 10.0,\n" +
                "      \"unit\" : {\n" +
                "        \"key\" : \"ms\"\n" +
                "      }\n" +
                "    } ]\n" +
                "  }],\n" +
                "  \"testedVersion\" : \"1.9-SNAPSHOT\",\n" +
                "  \"buildUrl\" : \"${build-url}\"\n" +
                "}", RunResult.class );

        // Then
        CaseResult.Metric metric = result.getMetric( "CineastsQueriesBenchmark", "Average for: Single path with many start points" );

        assertThat( metric, not(nullValue()));
        assertThat( metric.calculateAllowedRegression( 0.2 ), is( 12.0 ) );
    }

    @Test
    public void shouldDeserializeOldMixedLoadResultsAsBiggerIsBetter() throws Exception
    {
        // Given
        ObjectMapper jsonMapper = new ObjectMapper();

        // When
        RunResult result = jsonMapper.readValue( "{\n" +
                "  \"timestamp\" : 1349810534349,\n" +
                "  \"results\" : [ {\n" +
                "    \"caseName\" : \"MixedLoadBenchCase\",\n" +
                "    \"metrics\" : [ {\n" +
                "      \"name\" : \"Average reads\",\n" +
                "      \"value\" : 1000.0,\n" +
                "      \"unit\" : {\n" +
                "        \"key\" : \"Core API read / ms\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"name\" : \"Average writes\",\n" +
                "      \"value\" : 1.0,\n" +
                "      \"unit\" : {\n" +
                "        \"key\" : \"Core API write tx / ms\"\n" +
                "      }\n" +
                "    } ]\n" +
                "  } ],\n" +
                "  \"testedVersion\" : \"1.9-SNAPSHOT\",\n" +
                "  \"buildUrl\" : \"${build-url}\"\n" +
                "}", RunResult.class );

        // Then
        CaseResult.Metric readMetric = result.getMetric( "MixedLoadBenchCase", "Average reads" );

        assertThat(readMetric, not(nullValue()));
        assertThat(readMetric.calculateAllowedRegression( 0.2 ), is( 800.0 ) );

        CaseResult.Metric writeMetric = result.getMetric( "MixedLoadBenchCase", "Average writes" );

        assertThat(writeMetric, not(nullValue()));
        assertThat(writeMetric.calculateAllowedRegression( 0.2 ), is( 0.8 ) );
    }

}
