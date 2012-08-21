/*
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
d3.tsv("data.tsv", function(data) {

    function measurement( d )
    {
        return d.measurement;
    }

    function buildTime(d)
    {
        return d["build"];
    }

    var scenarios = ["avgr", "avgw", "peakr", "peakw", "susr", "susw"];

    var measurements = data.map(function(build) {
        return scenarios.map(function(scenario) {
            return {
                build: buildTime(build),
                scenario: scenario,
                measurement: parseFloat(build[scenario])
            };
        });
    }).reduce(function(a, b) {
        return a.concat(b);
    });

    var scenarioMeasurements = {};
    measurements.forEach(function(measurement) {
        var list = scenarioMeasurements[measurement.scenario];
        if (!list) {
            list = (scenarioMeasurements[measurement.scenario] = []);
        }
        list.push(measurement);
    });

    var chartSize = { width: 1024, height: 400},
        margins = { left: 100, right: 100, top: 100, betweenCharts: 300, bottom: 300 },
        boundingBox = {
            width: chartSize.width + margins.left + margins.right,
            height: scenarios.length * (chartSize.height + margins.betweenCharts)
                + margins.top + margins.bottom - margins.betweenCharts
        };

    var svg = d3.select("div.results-container").selectAll("svg.chart" )
        .data([true])
        .enter()
        .append("svg:svg")
        .attr("class", "svg")
        .attr("viewBox", [
            -margins.left,
            -margins.top,
            boundingBox.width,
            boundingBox.height
        ].join(" "))
        .attr("width", boundingBox.width)
        .attr("height", boundingBox.height);

    var x = d3.scale.ordinal().domain(data.map(buildTime)).rangePoints([0, chartSize.width]);
    var xAxis = d3.svg.axis().scale(x).orient("left");

    var scenarioSpecificYScales = {};
    scenarios.forEach(function(scenario) {
        var yScale = d3.scale.linear()
            .domain([0, d3.max(scenarioMeasurements[scenario].map(measurement) )] )
            .range( [chartSize.height, 0] );
        var yAxis = d3.svg.axis().scale(yScale).orient("left");
        scenarioSpecificYScales[scenario] = { y: yScale, yAxis: yAxis };
    });

    var chart = svg.selectAll("g.chart")
        .data(scenarios)
        .enter().append("svg:g")
        .attr("class", "chart")
        .attr("transform", function(d, i) { return "translate(0," + i * (chartSize.height + margins.betweenCharts) + ")" });


    chart.selectAll("circle.measurement")
        .data(function(scenario) { return scenarioMeasurements[scenario]; })
        .enter()
        .append("svg:circle")
        .attr("class", "measurement")
        .attr("r", 2)
        .attr("cy", function(d) { return scenarioSpecificYScales[d.scenario].y(measurement(d)); })
        .attr("cx", function(d) { return x( d.build ); });

    chart.append("svg:g")
        .attr("class", "x axis")
        .attr("transform", "rotate(270,0,0) translate(" + -chartSize.height + ", 0)")
        .call(xAxis);

    chart.selectAll("g.y.axis")
        .data(function(d) { return [d]; })
        .enter().append("svg:g")
        .attr("class", "y axis")
        .call(function(selection) {
            selection.forEach(function(d) {
                var group = d3.select(d[0]);
                var axis = scenarioSpecificYScales[group.data()].yAxis;
                axis(group);
            });
        });

    chart.append("svg:text")
        .attr("class", "axis-label y")
        .attr("transform", "translate(" + 2 * -margins.left / 3 + " " + chartSize.height / 2 + ") rotate(-90)")
        .text("Operations/s");
});
