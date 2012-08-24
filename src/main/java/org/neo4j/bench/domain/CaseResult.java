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
package org.neo4j.bench.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

public class CaseResult
{


    public static class Metric implements Comparable<Metric>
    {
        private String name;
        private Double value;
        private boolean trackRegression;

        public Metric( String name, double value )
        {
            this(name, value, false);
        }

        public Metric( @JsonProperty("name") String name, @JsonProperty("value") double value, @JsonProperty("trackRegression") boolean trackRegression )
        {
            this.name = name;
            this.value = value;
            this.trackRegression = trackRegression;
        }

        public String getName()
        {
            return name;
        }

        public double getValue()
        {
            return value;
        }

        public boolean shouldTrackRegression()
        {
            return trackRegression;
        }

        @Override
        public int compareTo( Metric other )
        {
            return other.value.compareTo( value );
        }
    }

    private String caseName;
    private List<Metric> metrics;

    public CaseResult( @JsonProperty("caseName") String caseName)
    {
        this.caseName = caseName;
        this.metrics = new ArrayList<Metric>();
    }

    public CaseResult( String caseName, Metric ... metrics )
    {
        this.caseName = caseName;
        this.metrics = Arrays.asList(metrics);
    }

    public String getCaseName()
    {
        return caseName;
    }

    public List<Metric> getMetrics()
    {
        return metrics;
    }

    public boolean containsMetric( String metricName )
    {
        return getMetric(metricName) != null;
    }

    public Metric getMetric( String metricName )
    {
        for(Metric metric : metrics)
        {
            if(metric.getName().equals( metricName ))
            {
                return metric;
            }
        }

        return null;
    }

}
