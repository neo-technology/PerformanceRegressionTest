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
import org.neo4j.helpers.Pair;

public class RegressionDetector
{

    private double threshold;

    public RegressionDetector(double threshold)
    {
        this.threshold = threshold;
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
                            caseResult.getCaseName(), currentMetric.getName() );

                    Metric bestHistoricValue = resultPair.first();
                    RunResult bestHistoricRun = resultPair.other();

                    if(hasRegressedMoreThanThreshold(currentMetric, bestHistoricValue))
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

    private boolean hasRegressedMoreThanThreshold( Metric currentMetric, Metric bestHistoricValue )
    {
        return currentMetric.compareTo( bestHistoricValue ) > 0 &&
               // Ensure that the difference between the values, disregarding in what "direction" is greater than the threshold allows
               Math.abs( bestHistoricValue.getValue() - currentMetric.getValue() ) > Math.abs( bestHistoricValue.getValue() * threshold );
    }
}
