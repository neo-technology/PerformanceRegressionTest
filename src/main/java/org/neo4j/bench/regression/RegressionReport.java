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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.bench.domain.RunResult;
import org.neo4j.graphdb.NotFoundException;

public class RegressionReport
{

    private RunResult testResult;

    public static class Regression
    {
        private String caseName;
        private String metricName;
        private RunResult regressedRun;
        private RunResult trumpingRun;
        private double threshold;

        public Regression(String caseName, String metricName, RunResult regressedRun, RunResult trumpingRun, double threshold)
        {
            this.caseName = caseName;
            this.metricName = metricName;
            this.regressedRun = regressedRun;
            this.trumpingRun = trumpingRun;
            this.threshold = threshold;
        }

        public String getCaseName()
        {
            return caseName;
        }

        public String getMetricName()
        {
            return metricName;
        }

        public String toString()
        {
            return toStringWithPrefix( "" );
        }

        public String toStringWithPrefix( String prefix )
        {
            StringWriter sw = new StringWriter(  );
            PrintWriter out = new PrintWriter( sw );

            double trumpingValue = trumpingRun.getMetric( caseName, metricName ).getValue();
            double regressedValue = regressedRun.getMetric( caseName, metricName ).getValue();

            out.printf( "%sMetric: '%s' has regressed since version %s (%s)\n", prefix, metricName, trumpingRun.getTestedVersion(), trumpingRun.getBuildUrl());
            out.printf( "%s  Was: %.4f\n", prefix, trumpingValue);
            out.printf( "%s  Is now: %.4f\n", prefix, regressedValue);
            out.printf( "%s  (Needs to be at least %.4f)\n",  prefix, trumpingValue - threshold * trumpingValue);

            return sw.toString();
        }
    }

    List<Regression> regressions = new ArrayList<Regression>(  );

    public RegressionReport(RunResult testResult)
    {
        this.testResult = testResult;
    }

    public void add( Regression regression )
    {
        regressions.add(regression);
    }

    public boolean regressionDetected()
    {
        return regressions.size() > 0;
    }

    @Override
    public String toString()
    {
        StringBuilder out = new StringBuilder( );
        out.append( "REGRESSION REPORT\n" +
                    "-----------------\n" +
                    "Tested version "+testResult.getTestedVersion()+" on "+testResult.getTimestamp()+".\n" +
                    ""+testResult.getBuildUrl()+"\n");
        if( regressionDetected())
        {

            out.append( regressions.size() + " metric(s) have regressed.\n\n" );

            for(String caseName : getCaseNames())
            {

                out.append( "Case: '"+ caseName +"'\n" );

                for(String metricName : getMetricNames(caseName))
                {
                    out.append( getRegression(caseName, metricName).toStringWithPrefix( "  " ) );
                    out.append( "\n" );
                }
            }
        } else
        {
            out.append( "All metrics are within allowed thresholds.\n" );
        }

        return out.toString();
    }

    private Regression getRegression( String caseName, String metricName )
    {
        for(Regression regression : regressions)
        {
            if(regression.getCaseName().equals( caseName ) && regression.getMetricName().equals( metricName ))
            {
               return regression;
            }
        }

        throw new NotFoundException( "No regression listed for case '" + caseName + "' and metric '" + metricName + "'." );
    }

    private Collection<String> getMetricNames( String caseName )
    {
        Set<String> names = new HashSet<String>();
        for(Regression regression : regressions)
        {
            if(regression.getCaseName().equals( caseName ))
            {
                names.add( regression.getMetricName() );
            }
        }

        return names;
    }

    private Collection<String> getCaseNames()
    {
        Set<String> names = new HashSet<String>();
        for(Regression regression : regressions)
        {
            names.add( regression.getCaseName() );
        }
        return names;
    }
}
