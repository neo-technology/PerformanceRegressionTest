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
package org.neo4j.bench.domain;

import static org.neo4j.bench.domain.Units.CORE_API_READ;
import static org.neo4j.bench.domain.Units.CORE_API_WRITE_TRANSACTION;
import static org.neo4j.bench.domain.Units.MILLISECOND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

public class CaseResult
{

    /**
     * Determines how to compare two metrics and track regression -
     * is a smaller value of a given metric better, or is a bigger
     * value better?
     */
    public static enum MetricComparer
    {
        BIGGER_IS_BETTER
                {
                    @Override
                    public boolean valueHasRegressed( Double newValue, Double oldValue, double threshold )
                    {
                        return newValue < calculateAllowedRegression( oldValue, threshold );
                    }

                    @Override
                    public int compare( Double firstValue, Double secondValue )
                    {
                        return secondValue.compareTo( firstValue );
                    }

                    @Override
                    public double calculateAllowedRegression( Double value, double threshold )
                    {
                        return value - value * threshold;
                    }
                },
        SMALLER_IS_BETTER
                {
                    @Override
                    public boolean valueHasRegressed( Double newValue, Double oldValue, double threshold )
                    {
                        return newValue > calculateAllowedRegression( oldValue, threshold );
                    }

                    @Override
                    public double calculateAllowedRegression( Double value, double threshold )
                    {
                        return value + value * threshold;
                    }

                    @Override
                    public int compare( Double firstValue, Double secondValue )
                    {
                        return firstValue.compareTo( secondValue );
                    }
                };

        public abstract int compare( Double firstValue, Double secondValue );

        public abstract double calculateAllowedRegression( Double value, double threshold );

        public abstract boolean valueHasRegressed( Double newValue, Double oldValue, double threshold );
    }

    public static class Metric implements Comparable<Metric>
    {
        @JsonProperty private final String name;
        @JsonProperty private final Double value;
        @JsonProperty private final boolean trackRegression;
        @JsonProperty private final Unit unit;
        @JsonProperty private final MetricComparer comparer;

        public Metric( String name, double value, Unit unit, MetricComparer comparer )
        {
            this(name, value, unit, false, comparer );
        }

        public Metric( @JsonProperty("name") String name,
                       @JsonProperty("value") double value,
                       @JsonProperty("unit") Unit unit ,
                       @JsonProperty("trackRegression") boolean trackRegression,
                       @JsonProperty("comparer") MetricComparer comparer )
        {
            this.name = name;
            this.value = value;
            this.trackRegression = trackRegression;
            this.unit     = unit     != null ? unit     : backwardsCompatUnit();
            this.comparer = comparer != null ? comparer : backwardsCompatComparer();
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
            return comparer.compare( value, other.value );
        }

        public double calculateAllowedRegression( double threshold )
        {
            return comparer.calculateAllowedRegression(value, threshold);
        }

        public boolean hasRegressedFrom( Metric other, double threshold )
        {
            if(other == null)
            {
                throw new IllegalArgumentException( "You need to provide a metric to compare regression to, got null.");
            }

            if(comparer == null)
            {
                throw new IllegalStateException( "No comparer set for metric '"+getName()+"'" );
            }

            return comparer.valueHasRegressed(value, other.value, threshold);
        }

        /**
         * Get a unit for this case result based on the name, used to cover
         * for saved stats that didn't contain units.
         * @return
         */
        private Unit backwardsCompatUnit()
        {
            if(name.contains( "reads" ))
            {
                return CORE_API_READ.per( MILLISECOND );
            } else
            {
                return CORE_API_WRITE_TRANSACTION.per( MILLISECOND );
            }
        }

        private MetricComparer backwardsCompatComparer()
        {
            if(name.contains( "Average for: Single path with many start points" ))
            {
                return MetricComparer.SMALLER_IS_BETTER;
            } else
            {
                return MetricComparer.BIGGER_IS_BETTER;
            }
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
