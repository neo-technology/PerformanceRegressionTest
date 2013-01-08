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
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.neo4j.bench.domain.filter.RunResultFilter;
import org.neo4j.bench.domain.filter.VersionFilter;
import org.neo4j.helpers.Pair;

/**
 * A collection of {@link RunResult}s.
 */
public class RunResultSet implements Iterable<RunResult>
{

    @JsonProperty private List<RunResult> results;

    public RunResultSet()
    {
        this.results = new ArrayList<RunResult>(  );
    }

    public RunResultSet(RunResult ... results )
    {
        this( Arrays.asList(results) );
    }

    public RunResultSet(List<RunResult> results )
    {
        this.results = results;
    }

    public void add( RunResult runResult )
    {
        results.add( runResult );
    }

    public RunResultSet filter( RunResultFilter filter )
    {
        List<RunResult> filtered = new ArrayList<RunResult>(  );
        for(RunResult result : results)
        {
            if(filter.accept( result ))
            {
                filtered.add( result );
            }
        }

        return new RunResultSet(filtered);
    }

    public int size()
    {
        return results.size();
    }

    public Pair<CaseResult.Metric, RunResult> getHighestValueOf( String caseName, String metricName)
    {
        return getHighestValueOf( caseName, metricName, VersionFilter.ANY );
    }

    /**
     * Returns the top metric for a filtered set of runresults, or null if none is found.
     * @param caseName
     * @param metricName
     * @param filter
     * @return
     */
    public Pair<CaseResult.Metric, RunResult> getHighestValueOf( String caseName, String metricName, RunResultFilter filter)
    {
        CaseResult.Metric topMetric = null;
        RunResult runForTopMetric = null;

        for(RunResult result : this.filter( filter ))
        {
            CaseResult.Metric current = result.getMetric(caseName, metricName);
            if(current != null && (topMetric == null || current.compareTo( topMetric) < 0))
            {
                topMetric = current;
                runForTopMetric = result;
            }
        }

        return Pair.of( topMetric, runForTopMetric );
    }

    @Override
    public Iterator<RunResult> iterator()
    {
        return results.iterator();
    }
}
