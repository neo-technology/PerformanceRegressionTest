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

    var inputDateFormat = d3.time.format("%Y-%m-%d %H:%M");

    function buildTime(d)
    {
        var timeString = "2012-" + d["build"].substring(0, 11);
        return inputDateFormat.parse(timeString);
    }

    function branch(d)
    {
        return d["build"].match(/\[(.*)\]/)[1];
    }

    var scenarios = [
        { key: "avgr", name: "Average Reads" },
        { key: "avgw", name: "Average Writes" },
        { key: "peakr", name: "Peak Reads" },
        { key: "peakw", name: "Peak Writes" },
        { key: "susr", name: "Sustained Reads" },
        { key: "susw", name: "Sustained Writes" }
    ];

    var measurements = data.map(function(build) {
        return scenarios.map(function(scenario) {
            return {
                build: buildTime(build),
                branch: branch(build),
                scenario: scenario,
                measurement: parseFloat(build[scenario.key])
            };
        });
    }).reduce(function(a, b) {
        return a.concat(b);
    });

    var scenarioMeasurements = {};
    measurements.forEach(function(measurement) {
        var list = scenarioMeasurements[measurement.scenario.key];
        if (!list) {
            list = (scenarioMeasurements[measurement.scenario.key] = []);
        }
        list.push(measurement);
    });

    var chartSize = { width: 1024, height: 400},
        margins = { left: 100, right: 100, top: 100, betweenCharts: 150, bottom: 150 },
        boundingBox = {
            width: chartSize.width + margins.left + margins.right,
            height: scenarios.length * (chartSize.height + margins.betweenCharts)
                + margins.top + margins.bottom - margins.betweenCharts
        };

    var svg = d3.select("div.results-container").selectAll("svg.chart" )
        .data([true])
        .enter()
        .append("svg:svg")
        .attr("class", "chart")
        .attr("viewBox", [
            -margins.left,
            -margins.top,
            boundingBox.width,
            boundingBox.height
        ].join(" "))
        .attr("width", boundingBox.width)
        .attr("height", boundingBox.height);

    var x = d3.time.scale()
        .domain([d3.min(data, buildTime), d3.max(data, buildTime)])
        .range([0, chartSize.width]);
    var xAxis = d3.svg.axis().scale(x).orient("left");

    var scenarioSpecificYScales = {};
    scenarios.forEach(function(scenario) {
        var yScale = d3.scale.linear()
            .domain([0, d3.max(scenarioMeasurements[scenario.key].map(measurement) )] )
            .range( [chartSize.height, 0] );
        var yAxis = d3.svg.axis().scale(yScale).orient("left");
        scenarioSpecificYScales[scenario.key] = { y: yScale, yAxis: yAxis };
    });

    var chart = svg.selectAll("g.chart")
        .data(scenarios)
        .enter().append("svg:g")
        .attr("class", "chart")
        .attr("transform", function(d, i) { return "translate(0," + i * (chartSize.height + margins.betweenCharts) + ")" });


    chart.selectAll("text.scenario-name")
        .data(function(scenario) { return [scenario]; })
        .enter().append("svg:text")
        .text(function(scenario) { return scenario.name; })
        .attr("y", -40)
        .attr("class", "scenario-name");

    var branchColour = d3.scale.category10();

    var y = function(d) { return scenarioSpecificYScales[d.scenario.key].y(measurement(d)); };

    var toolTipDateFormat = d3.time.format("%Y-%m-%d %H:%M");

    var mouseOver = function() {
        var circle = d3.select(this);
        circle.style("fill", "black");
        var data = circle.data()[0];
        var tooltipGroup = d3.select(this.parentNode).select("g.tooltip")
            .attr("transform", "translate(" + x(data.build) + "," + y(data) + ")")
            .attr("visibility", "visible");

        tooltipGroup.select("text")
            .text( toolTipDateFormat(data.build) + " [" + data.branch + "] " + measurement(data) );
    };

    var mouseOut = function() {
        var circle = d3.select(this);
        var data = circle.data()[0];
        circle.style("fill", branchColour(data.branch));
        d3.select(this.parentNode).select("g.tooltip")
            .attr("visibility", "hidden");
    };

    chart.selectAll("circle.measurement")
        .data(function(scenario) { return scenarioMeasurements[scenario.key]; })
        .enter()
        .append("svg:circle")
        .attr("class", "measurement")
        .attr("fill", function(d) { return branchColour(d.branch); })
        .attr("r", 3)
        .attr("cy", y)
        .attr("cx", function(d) { return x( d.build ); })
        .on("mouseover", mouseOver)
        .on("mouseout", mouseOut);

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
                var axis = scenarioSpecificYScales[group.data()[0].key].yAxis;
                axis(group);
            });
        });

    chart.append("svg:text")
        .attr("class", "axis-label y")
        .attr("transform", "translate(" + 2 * -margins.left / 3 + " " + chartSize.height / 2 + ") rotate(-90)")
        .text("Operations/s");

    // tooltips should go on top of everything else; simplest way to achieve this is to append them to the DOM last.
    var toolTip = chart.selectAll("g.tooltip")
        .data(function(scenario) { return [scenario]; })
        .enter().append("svg:g")
        .attr("visibility", "hidden")
        .attr("class", "tooltip");

    // TODO: compute the correct size for the outline based on the size of text inside it, using getBBox() or similar
    toolTip.append("svg:path")
        .attr("class", "outline")
        .attr("d", "M 0 -3 L 10 -13 L 150 -13 L 150 -38 L -150 -38 L -150 -13 L -10 -13 Z");

    toolTip.append("svg:text")
        .attr("y", -25.5);
});
