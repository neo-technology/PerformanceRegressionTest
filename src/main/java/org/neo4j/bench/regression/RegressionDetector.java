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

import static org.neo4j.bench.domain.CaseResult.*;
import static org.neo4j.bench.regression.RegressionReport.*;

import org.neo4j.bench.domain.CaseResult;
import org.neo4j.bench.domain.RunResult;
import org.neo4j.bench.domain.RunResultSet;
import org.neo4j.bench.domain.filter.RunResultFilter;
import org.neo4j.helpers.Pair;

public class RegressionDetector
{

    private final double threshold;
    private final RunResultFilter runsToCompareToFilter;

    public RegressionDetector( double threshold, RunResultFilter runsToCompareToFilter )
    {
        this.threshold = threshold;
        this.runsToCompareToFilter = runsToCompareToFilter;
    }

    public RegressionReport detectRegression( RunResultSet historicResults, RunResult currentRun )
    {
        RegressionReport report = new RegressionReport(currentRun);

        for(CaseResult caseResult : currentRun.getResults())
        {
            for( Metric currentMetric : caseResult.getMetrics())
            {
                if(currentMetric.shouldTrackRegression())
                {

                    // Check if this metric has ever had a better value

                    Pair<Metric, RunResult> resultPair = historicResults.getHighestValueOf(
                            caseResult.getCaseName(), currentMetric.getName(), runsToCompareToFilter );

                    Metric bestHistoricValue = resultPair.first();
                    RunResult bestHistoricRun = resultPair.other();

                    if(bestHistoricValue == null)
                    {
                        System.out.println("Notice: No appropriate runs found for '" + caseResult.getCaseName() + ":" +
                                currentMetric.getName() + "' to compare regression against. Skipping regression check for this metric.");
                    }
                    else if(currentMetric.hasRegressedFrom(bestHistoricValue, threshold))
                    {
                        // Oh noes! We found a regression :(
                        report.add( new Regression(caseResult.getCaseName(), currentMetric.getName(),
                                                   currentRun, bestHistoricRun, threshold) );
                    }
                }
            }
        }

        return report;
    }
}
