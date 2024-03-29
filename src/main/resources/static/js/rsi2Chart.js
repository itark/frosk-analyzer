function rsi2Chart(divId) {

    maybeDisposeRoot(divId);

    security = selectedSecurity;
    strategy = selectedStrategy;
    var dailyPricesUrl = "dailyPrices?security="+security+"&strategy="+strategy;
	var indicatorValueUrl = "indicatorValues?security="+security+"&strategy="+strategy;
	var tradesUrl = "trades?security="+security+"&strategy="+strategy;

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

    var rsiAxisRenderer = am5xy.AxisRendererY.new(root, {
      inside: true
    });
    rsiAxisRenderer.labels.template.setAll({
      centerY: am5.percent(100),
      maxPosition: 0.98
    });
    var rsiAxis = chart.yAxes.push(am5xy.ValueAxis.new(root, {
      renderer: rsiAxisRenderer,
      height: am5.percent(30),
      layer: 5,
      numberFormat: "#a"
    }));
    rsiAxis.axisHeader.set("paddingTop", 10);
    rsiAxis.axisHeader.children.push(am5.Label.new(root, {
      text: "rsi",
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
    var longSmaSeries = chart.series.push(am5xy.LineSeries.new(root, {
        name: "longSmaSeries",
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
    var shortSmaSeries = chart.series.push(am5xy.LineSeries.new(root, {
        name: "shortSmaSeries",
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
    var rsiSeries = chart.series.push(am5xy.LineSeries.new(root, {
        name: "rsi",
        xAxis: xAxis,
        yAxis: rsiAxis,
        valueYField: "value",
        valueXField: "date",
        legendValueText: "{valueY}",
        tooltip: am5.Tooltip.new(root, {
          pointerOrientation: "horizontal",
          labelText: "{valueY}"
        })
    }));

    series.bullets.push(function(root, series, dataItem) {
     var _centerY;
     if (dataItem.dataContext.trade) {
      if (dataItem.dataContext.trade === 'Buy') {
        _centerY = 0;
      } else {
        _centerY = 100;
      }
      return am5.Bullet.new(root, {
        sprite: am5.Label.new(root, {
          text: "{trade}",
          centerX: am5.percent(50),
          centerY: am5.percent(_centerY),
          populateText: true,
          fill: am5.color(0x000000),
        })
      });
     }
    });

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
    rsiSeries.strokes.template.setAll({ strokeWidth: 1 });
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

    var tradeData = [];
    am5.net.load(dailyPricesUrl).then(function(result) {
      const dailyPrices = am5.JSONParser.parse(result.response);
      sbseries.data.setAll(dailyPrices);
      series.data.setAll(dailyPrices);
      volumeSeries.data.setAll(dailyPrices);
    }).catch(function(result) {
      console.log("Error loading " + result);
    });

    var longSmaData = [];
    var shortSmaData = [];
    var rsiData = [];
    am5.net.load(indicatorValueUrl).then(function(result) {
      const indicatorValues = am5.JSONParser.parse(result.response);
      for (let i in indicatorValues) {
         console.log(indicatorValues[i]);
         if (indicatorValues[i].name === 'longSma') {
             longSmaData.push({
               date: indicatorValues[i].date,
               name: indicatorValues[i].name,
               value: indicatorValues[i].value
             });
         } else if(indicatorValues[i].name === 'shortSma') {
             shortSmaData.push({
               date: indicatorValues[i].date,
               name: indicatorValues[i].name,
               value: indicatorValues[i].value
             });
         } else if(indicatorValues[i].name === 'rsi') {
             rsiData.push({
               date: indicatorValues[i].date,
               name: indicatorValues[i].name,
               value: indicatorValues[i].value
             });
         }
      }
      longSmaSeries.data.setAll(longSmaData);
      shortSmaSeries.data.setAll(shortSmaData);
      rsiSeries.data.setAll(rsiData);
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
    longSmaSeries.appear(1000);
    shortSmaSeries.appear(1000);

    chart.appear(1000, 100);

    //$("#charttype").text('rsi2');

}