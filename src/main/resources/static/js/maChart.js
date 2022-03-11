function maChart(divId) {

    maybeDisposeRoot(divId);

    security = selectedSecurity;
    strategy = selectedStrategy;
    var dailyPricesUrl = "dailyPrices?security="+security+"&strategy="+strategy;
	var indicatorValueUrl = "indicatorValues?security="+security+"&strategy="+strategy;

    var root = am5.Root.new(divId);

    root.setThemes([
      am5themes_Animated.new(root)
    ]);
    var chart = root.container.children.push(
      am5xy.XYChart.new(root, {
        focusable: true,
        panX: true,
        panY: true,
        wheelX: "panX",
        wheelY: "zoomX"
      })
    );
    var xAxis = chart.xAxes.push(
      am5xy.DateAxis.new(root, {
        maxDeviation:0.5,
        groupData: true,
        baseInterval: { timeUnit: "day", count: 1 },
        renderer: am5xy.AxisRendererX.new(root, {pan:"zoom"}),
        tooltip: am5.Tooltip.new(root, {
          themeTags: ["axis"],
          animationDuration: 300
        })
      })
    );
    var yAxis = chart.yAxes.push(
      am5xy.ValueAxis.new(root, {
        maxDeviation:1,
        //logarithmic: true,
        treatZeroAs: 0.000001,
        renderer: am5xy.AxisRendererY.new(root, {
            pan:"zoom"
        })
      })
    );

    var yyAxis = chart.yAxes.push(
      am5xy.ValueAxis.new(root, {
        maxDeviation:1,
        renderer: am5xy.AxisRendererY.new(root, {
            opposite: true,
            pan:"zoom"
        })
      })
    );

    var stochasticOscillKAxisRenderer = am5xy.AxisRendererY.new(root, {
      inside: true
    });
    stochasticOscillKAxisRenderer.labels.template.setAll({
      centerY: am5.percent(100),
      maxPosition: 0.98
    });
    var stochasticOscillKAxis = chart.yAxes.push(am5xy.ValueAxis.new(root, {
      renderer: stochasticOscillKAxisRenderer,
      height: am5.percent(30),
      layer: 5,
      numberFormat: "#a"
    }));
    stochasticOscillKAxis.axisHeader.set("paddingTop", 10);
    stochasticOscillKAxis.axisHeader.children.push(am5.Label.new(root, {
      text: "stochasticOscillK",
      fontWeight: "bold",
      paddingTop: 5,
      paddingBottom: 5
    }));
    var macdAxisRenderer = am5xy.AxisRendererY.new(root, {
      inside: true
    });
    macdAxisRenderer.labels.template.setAll({
      centerY: am5.percent(100),
      maxPosition: 0.98
    });
    var macdAxis = chart.yAxes.push(am5xy.ValueAxis.new(root, {
      renderer: macdAxisRenderer,
      height: am5.percent(30),
      layer: 5,
      numberFormat: "#a"
    }));
    macdAxis.axisHeader.set("paddingTop", 10);
    macdAxis.axisHeader.children.push(am5.Label.new(root, {
      text: "macd",
      fontWeight: "bold",
      paddingTop: 5,
      paddingBottom: 5
    }));

    var volumeAxisRenderer = am5xy.AxisRendererY.new(root, {
      inside: true
    });
    volumeAxisRenderer.labels.template.setAll({
      centerY: am5.percent(100),
      maxPosition: 0.98
    });
    var volumeAxis = chart.yAxes.push(am5xy.ValueAxis.new(root, {
      renderer: volumeAxisRenderer,
      height: am5.percent(30),
      layer: 5,
      numberFormat: "#a"
    }));
    volumeAxis.axisHeader.set("paddingTop", 10);
    volumeAxis.axisHeader.children.push(am5.Label.new(root, {
      text: "Volume",
      fontWeight: "bold",
      paddingTop: 5,
      paddingBottom: 5
    }));

    var color = root.interfaceColors.get("background");
    var series = chart.series.push(
      am5xy.CandlestickSeries.new(root, {
        fill: color,
        calculateAggregates: true,
        stroke: color,
        name: security,
        xAxis: xAxis,
        yAxis: yAxis,
        valueYField: "value",
        openValueYField: "open",
        lowValueYField: "low",
        highValueYField: "high",
        valueXField: "date",
        lowValueYGrouped: "low",
        highValueYGrouped: "high",
        openValueYGrouped: "open",
        valueYGrouped: "close",
        legendRangeValueText: "{valueYClose}",
        tooltip: am5.Tooltip.new(root, {
          pointerOrientation: "horizontal",
          labelText: "open: {openValueY}\nlow: {lowValueY}\nhigh: {highValueY}\nclose: {valueY}"
        })
      })
    );
    // make professional
    series.columns.template.get("themeTags").push("pro");
    var longEmaSeries = chart.series.push(am5xy.LineSeries.new(root, {
        name: "longEmaSeries",
        xAxis: xAxis,
        yAxis: yAxis,
        valueYField: "value",
        valueXField: "date",
        fill: am5.color(0x095256),
        stroke: am5.color(0x095256),
        legendValueText: "{valueY}",
        tooltip: am5.Tooltip.new(root, {
          pointerOrientation: "horizontal",
          labelText: "{valueY}"
        })
    }));
    var shortEmaSeries = chart.series.push(am5xy.LineSeries.new(root, {
        name: "shortEmaSeries",
        xAxis: xAxis,
        yAxis: yAxis,
        valueYField: "value",
        valueXField: "date",
        legendValueText: "{valueY}",
        tooltip: am5.Tooltip.new(root, {
          pointerOrientation: "horizontal",
          labelText: "{valueY}"
        })
    }));
    var stochasticOscillKSeries = chart.series.push(am5xy.LineSeries.new(root, {
        name: "stochasticOscillK",
        xAxis: xAxis,
        yAxis: stochasticOscillKAxis,
        valueYField: "value",
        valueXField: "date",
        legendValueText: "{valueY}",
        tooltip: am5.Tooltip.new(root, {
          pointerOrientation: "horizontal",
          labelText: "{valueY}"
        })
    }));
    var macdSeries = chart.series.push(am5xy.LineSeries.new(root, {
        name: "macd",
        xAxis: xAxis,
        yAxis: macdAxis,
        valueYField: "value",
        valueXField: "date",
        legendValueText: "{valueY}",
        tooltip: am5.Tooltip.new(root, {
          pointerOrientation: "horizontal",
          labelText: "{valueY}"
        })
    }));

    var emaMacdSeries = chart.series.push(am5xy.LineSeries.new(root, {
        name: "emaMacd",
        xAxis: xAxis,
        yAxis: macdAxis,
        valueYField: "value",
        valueXField: "date",
        fill: am5.color(0x095256),
        stroke: am5.color(0x095256),
        legendValueText: "{valueY}",
        tooltip: am5.Tooltip.new(root, {
          pointerOrientation: "horizontal",
          labelText: "{valueY}"
        })
    }));

    var firstColor = chart.get("colors") .getIndex(0);
    var volumeSeries = chart.series.push(am5xy.ColumnSeries.new(root, {
      name: "Volym",
      clustered:false,
      fill: firstColor,
      stroke: firstColor,
      valueYField: "volume",
      valueXField: "date",
      valueYGrouped: "sum",
      xAxis: xAxis,
      yAxis: volumeAxis,
      legendValueText: "{valueY}",
      tooltip: am5.Tooltip.new(root, {
        labelText: "{valueY}"
      })
    }));


    stochasticOscillKSeries.strokes.template.setAll({ strokeWidth: 1 });
    var cursor = chart.set(
      "cursor",
      am5xy.XYCursor.new(root, {
        xAxis: xAxis
      })
    );
    cursor.lineY.set("visible", false);
    chart.leftAxesContainer.set("layout", root.verticalLayout);
    var scrollbar = am5xy.XYChartScrollbar.new(root, {
      orientation: "horizontal",
      height: 50
    });
    chart.set("scrollbarX", scrollbar);
    var sbxAxis = scrollbar.chart.xAxes.push(
      am5xy.DateAxis.new(root, {
        groupData: true,
        groupIntervals: [{ timeUnit: "week", count: 1 }],
        baseInterval: { timeUnit: "day", count: 1 },
        renderer: am5xy.AxisRendererX.new(root, {
          opposite: false,
          strokeOpacity: 0
        })
      })
    );
    var sbyAxis = scrollbar.chart.yAxes.push(
      am5xy.ValueAxis.new(root, {
        renderer: am5xy.AxisRendererY.new(root, {})
      })
    );
    var sbseries = scrollbar.chart.series.push(
      am5xy.LineSeries.new(root, {
        xAxis: sbxAxis,
        yAxis: sbyAxis,
        valueYField: "value",
        valueXField: "date"
      })
    );

    am5.net.load(dailyPricesUrl).then(function(result) {
      const dailyPrices = am5.JSONParser.parse(result.response);
      sbseries.data.setAll(dailyPrices);
      series.data.setAll(dailyPrices);
      volumeSeries.data.setAll(dailyPrices);
    }).catch(function(result) {
      console.log("Error loading " + result);
    });

    var longEmaData = [];
    var shortEmaData = [];
    var stochasticOscillKData = [];
    var macdData = [];
    var emaMacdData = [];

    am5.net.load(indicatorValueUrl).then(function(result) {
      const indicatorValues = am5.JSONParser.parse(result.response);
      for (let i in indicatorValues) {
         if (indicatorValues[i].name === 'longEma') {
             longEmaData.push({
               date: indicatorValues[i].date,
               name: indicatorValues[i].name,
               value: indicatorValues[i].value
             });
         } else if(indicatorValues[i].name === 'shortEma') {
             shortEmaData.push({
               date: indicatorValues[i].date,
               name: indicatorValues[i].name,
               value: indicatorValues[i].value
             });
         } else if(indicatorValues[i].name === 'stochasticOscillK') {
              stochasticOscillKData.push({
                date: indicatorValues[i].date,
                name: indicatorValues[i].name,
                value: indicatorValues[i].value
              });
         } else if(indicatorValues[i].name === 'macd') {
             macdData.push({
               date: indicatorValues[i].date,
               name: indicatorValues[i].name,
               value: indicatorValues[i].value
             });
         } else if(indicatorValues[i].name === 'emaMacd') {
              emaMacdData.push({
                date: indicatorValues[i].date,
                name: indicatorValues[i].name,
                value: indicatorValues[i].value
              });
         }
      }
      longEmaSeries.data.setAll(longEmaData);
      shortEmaSeries.data.setAll(shortEmaData);
      stochasticOscillKSeries.data.setAll(stochasticOscillKData);
      macdSeries.data.setAll(macdData);
      emaMacdSeries.data.setAll(emaMacdData);
    }).catch(function(result) {
      console.log("Error loading " + result);
    });

    var legend = yAxis.axisHeader.children.push(am5.Legend.new(root, {}));
    legend.data.setAll(chart.series.values);
    legend.markers.template.setAll({
      width: 10
    });
    legend.markerRectangles.template.setAll({
      cornerRadiusTR: 0,
      cornerRadiusBR: 0,
      cornerRadiusTL: 0,
      cornerRadiusBL: 0
    });

    series.appear(1000);
    longEmaSeries.appear(1000);
    shortEmaSeries.appear(1000);
    stochasticOscillKSeries.appear(1000);
    macdSeries.appear(100);
    emaMacdSeries.appear(100);
    chart.appear(1000, 100);

}