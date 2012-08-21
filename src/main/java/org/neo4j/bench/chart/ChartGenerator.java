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

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.SortedSet;

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
import org.neo4j.bench.cases.CaseResult;

public class ChartGenerator
{
    public void generateWritePerformanceChart( SortedSet<CaseResult> results, String fileName )
    {
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
        for ( CaseResult key : results )
        {
            dataSet.addValue(key.getAvgWritePerSec(),
                    key.getName(), "avg writes");
            dataSet.addValue(key.getSustainedWritesPerSec(),
                    key.getName(), "sust writes");
            dataSet.addValue(key.getPeakWritesPerSec(),
                    key.getName(), "peak writes");
        }

        generateChart("Write performance history", fileName, "ops/s", dataSet);
    }

    public void generateReadPerformanceChart( SortedSet<CaseResult> results, String fileName )
    {
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
        for ( CaseResult key : results )
        {
            dataSet.addValue(key.getAvgReadsPerSec(),
                    key.getName(), "avg reads");
            dataSet.addValue(key.getSustainedReadsPerSec(),
                    key.getName(), "sust reads");
            dataSet.addValue(key.getPeakReadsPerSec(),
                    key.getName(), "peak reads");
        }

        generateChart("Read performance history", fileName, "ops/s", dataSet);
    }

    private void generateChart( String chartTitle, String outputFilename, String metricUnit, DefaultCategoryDataset dataset )
    {
        BarRenderer3D barRenderer = new BarRenderer3D();
        barRenderer.setBaseItemLabelsVisible( true );
        barRenderer.setBaseItemLabelGenerator( new StandardCategoryItemLabelGenerator(
                "{2}", new DecimalFormat( "###.#" ) ) );
        barRenderer.setBasePositiveItemLabelPosition( new ItemLabelPosition(
                ItemLabelAnchor.OUTSIDE12, TextAnchor.TOP_CENTER ) );
        barRenderer.setItemMargin( 0.06 );

        CategoryAxis catAxis = new CategoryAxis( "Benchmark case" );

        CategoryPlot basePlot = new CategoryPlot( dataset, catAxis,
                new NumberAxis(
                        metricUnit), barRenderer );
        basePlot.setOrientation( PlotOrientation.VERTICAL );
        basePlot.setDataset( dataset );
        basePlot.getRangeAxis().setLowerBound( 0 );

        JFreeChart chart = new JFreeChart( chartTitle, basePlot );

        Dimension dimensions = new Dimension( 1600, 900 );
        File chartFile = new File( outputFilename );

        System.out.println("Saving chart to " + chartFile.getAbsolutePath());
        try
        {
            ChartUtilities.saveChartAsPNG( chartFile, chart,
                    (int) dimensions.getWidth(), (int) dimensions.getHeight() );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
}
