/*
 * Copyright (C) 2012 Neo Technology
 * All rights reserved
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

    var measurements = data.map(function(build) {
        return ["avgr",	"avgw",	"peakr", "peakw", "susr", "susw"].map(function(operation) {
            return {
                build: buildTime(build),
                operation: operation,
                measurement: parseFloat(build[operation])
            };
        });
    }).reduce(function(a, b) {
        return a.concat(b);
    });

    var chartWidth = 400, chartHeight = 300, margin = 300;

    var chart = d3.select("div.results-container").selectAll("svg.chart" )
        .data([true])
        .enter()
        .append("svg:svg")
        .attr("class", "chart")
        .attr("viewBox", [-margin,-margin,chartWidth + margin * 2,chartHeight + margin * 2].join(" "))
        .attr("width", chartWidth + margin * 2)
        .attr("height", chartHeight + margin * 2);

    var x = d3.scale.ordinal().domain(data.map(buildTime)).rangePoints([0, chartWidth]);
    var y = d3.scale.linear().domain([0, d3.max(measurements.map(measurement) )] ).range( [chartHeight, 0] );

    var xAxis = d3.svg.axis().scale(x ).orient("left");
    var yAxis = d3.svg.axis().scale(y).orient("left");

    var points = chart.selectAll("circle.measurement")
        .data(measurements)
        .enter()
        .append("svg:circle")
        .attr("class", "measurement")
        .attr("r", 2)
        .attr("cy", function(d) { return y( measurement(d)); })
        .attr("cx", function(d) { return x( d.build ); });

    chart.append("svg:g")
        .attr("class", "x axis")
        .attr("transform", "rotate(270,0,0) translate(" + -chartHeight + ", 0)")
        .call(xAxis);

    chart.append("svg:g")
        .attr("class", "y axis")
        .call(yAxis);

    chart.append("svg:text")
        .attr("class", "axis-label y")
        .attr("transform", "translate(" + 2 * -margin / 3 + " " + chartHeight / 2 + ") rotate(-90)")
        .text("Operations/s")
});
