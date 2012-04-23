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
package org.neo4j.bench.chart;

import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer3D;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.TextAnchor;
import org.neo4j.bench.cases.mixedload.Stats;

public class GenerateOpsPerSecChart
{
    private static final int TESTS_TO_DRAW = 7;
    public static final String OPS_PER_SECOND_FILE_ARG = "ops-per-sec-file";
    public static final String CHART_FILE_ARG = "chart-file";

    private final String inputFilename;
    private final String outputFilename;
    private boolean performanceHasDegraded;
    // If performance has degraded, store the stats that trumped current performance here.
    private Stats trumpingStats; 
    private final SortedSet<Stats> data;
    private final double threshold;
    private boolean hasProcessed = false;

    public GenerateOpsPerSecChart( String inputFilename, String outputFilename,
            double threshold, boolean onlyCompareToGAReleases )
    {
        this.inputFilename = inputFilename;
        this.outputFilename = outputFilename;
        this.threshold = threshold;
        data = loadOpsPerSecond( this.inputFilename, onlyCompareToGAReleases );
    }

    public void process() throws Exception
    {
        
        trumpingStats = detectDegradation( threshold );
        performanceHasDegraded = trumpingStats != null;
        hasProcessed = true;
    }
    
    public boolean performanceHasDegraded() {
        if(hasProcessed) 
            return performanceHasDegraded;
        throw new RuntimeException("Unable to determine performance degradation before having run #process()");
    }
    
    public Stats getTrumpingStats() {
        return trumpingStats;
    }
    
    public Stats getLatestStats() {
        return data.last();
    }
    
    public void generateChart() throws Exception
    {
        if(!hasProcessed) 
            throw new RuntimeException("Unable to generate chart before having run #process()");
        
        DefaultCategoryDataset dataset = generateDataset();

        BarRenderer3D barRenderer = new BarRenderer3D();
        barRenderer.setBaseItemLabelsVisible( true );
        barRenderer.setBaseItemLabelGenerator( new StandardCategoryItemLabelGenerator(
                "{2}", new DecimalFormat( "###.#" ) ) );
        barRenderer.setBasePositiveItemLabelPosition( new ItemLabelPosition(
                ItemLabelAnchor.OUTSIDE12, TextAnchor.TOP_CENTER ) );
        barRenderer.setItemMargin( 0.06 );

        CategoryAxis catAxis = new CategoryAxis( "Bench Case" );

        CategoryPlot basePlot = new CategoryPlot( dataset, catAxis,
                new NumberAxis(
                "Operations Per Sec" ), barRenderer );
        basePlot.setOrientation( PlotOrientation.VERTICAL );
        basePlot.setDataset( dataset );
        basePlot.getRangeAxis().setLowerBound( 0 );

        JFreeChart chart = new JFreeChart( "Performance Chart", basePlot );

        Dimension dimensions = new Dimension( 1600, 900 );
        File chartFile = new File( outputFilename );
        if ( performanceHasDegraded )
        {
            chart.setBackgroundPaint( Color.RED );
        }
        System.out.println("Saving chart to " + chartFile.getAbsolutePath());
        ChartUtilities.saveChartAsPNG( chartFile, chart,
                (int) dimensions.getWidth(), (int) dimensions.getHeight() );
    }


    private Stats detectDegradation( double threshold )
    {
        Stats latestRun = getLatestStats();
        for ( Stats previous : data.headSet( latestRun ) )
        {
            double previousReads = previous.getAvgReadsPerSec();
            double previousWrites = previous.getAvgWritePerSec();
            if ( previousReads > latestRun.getAvgReadsPerSec()
                                 * ( 1 + threshold )
                    || previousWrites > latestRun.getAvgWritePerSec()
                                     * ( 1 + threshold ) )
            {
                return previous;
            }
        }
        return null;
    }

    
    private DefaultCategoryDataset generateDataset()
    {
        SortedSet<Stats> dataToDraw = new TreeSet<Stats>();
        if ( data.size() > TESTS_TO_DRAW )
        {
            Iterator<Stats> it = data.iterator();
            int i = 0;
            while ( data.size() - i++ > TESTS_TO_DRAW )
            {
                Stats toSkip = it.next();
                if(toSkip == trumpingStats) {
                    // Always include any stats that are currently
                    // trumping our latest stats.
                    dataToDraw.add(toSkip);
                }
            }
            dataToDraw.addAll(data.tailSet( it.next() ));
        }
        else
        {
            dataToDraw.addAll(data);
        }
        
        
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();

        for ( Stats key : dataToDraw )
        {
            dataSet.addValue(key.getAvgReadsPerSec(), key.getName(), "avg reads");
            dataSet.addValue(key.getAvgWritePerSec(),
                    key.getName(), "avg writes");
            dataSet.addValue(key.getPeakReadsPerSec(),
                    key.getName(), "peak reads");
            dataSet.addValue(key.getPeakWritesPerSec(),
                    key.getName(), "peak writes");
            dataSet.addValue(key.getSustainedReadsPerSec(),
                    key.getName(), "sust reads");
            dataSet.addValue(key.getSustainedWritesPerSec(),
                    key.getName(), "sust writes");
        }
        return dataSet;
    }

    /**
     * Opens the operations per second file, reads in the contents and creates a
     * SortedSet of the therein stored Stats.
     */
    public static SortedSet<Stats> loadOpsPerSecond( String fileName, boolean onlyCompareToGAReleases )
    {
        File dataFile = new File( fileName );
        if ( !dataFile.exists() )
        {
            return null;
        }
        BufferedReader reader = null;
        SortedSet<Stats> result = new TreeSet<Stats>();
        Stats currentStat = null;
        try
        {
            reader = new BufferedReader( new FileReader( dataFile ) );
            String line; // The current line
            while ( ( line = reader.readLine() ) != null )
            {
                currentStat = Stats.parse( line );
                if ( currentStat != null && (!onlyCompareToGAReleases || currentStat.isGARelease()))
                {
                    result.add( currentStat );
                }
            }
            
            // Add the latest result, even if it was not a GA
            if(currentStat != null && !currentStat.isGARelease()) {
                result.add(currentStat);
            }
        }
        catch ( IOException e )
        {
            // This should not happen as we check above
            e.printStackTrace();
            return null;
        }
        finally
        {
            if ( reader != null )
            {
                try
                {
                    reader.close();
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
