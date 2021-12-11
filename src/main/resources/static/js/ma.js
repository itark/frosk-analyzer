function multipleValueAxes() {
// Create root element
// https://www.amcharts.com/docs/v5/getting-started/#Root_element

  security = selectedSecurity;
  strategy = selectedStrategy;
  var dailyPricesUrl = "dailyPrices?security="+security;
  var indicatorValueUrl = "indicatorValues?security="+security+"&strategy="+strategy;
  var data = [];
  //var firstDate = new Date();
  //firstDate.setDate(firstDate.getDate() - 100);
  //firstDate.setHours(0, 0, 0, 0);
/*
am5.net.load(dailyPricesUrl).then(function(result) {
  const dailyPrices = am5.JSONParser.parse(result.response);
  console.log('dailyPrices',dailyPrices);
  for (let i in dailyPrices) {
     data.push({
       date: dailyPrices[i].date,
       value: dailyPrices[i].value
     });
  }
  console.log('data',data);
}).catch(function(result) {
  console.log("Error loading dailyPrices " + result);
});
*/

root.container.children.clear();
//root = am5.Root.new("chart-div-row2");

// Set themes
// https://www.amcharts.com/docs/v5/concepts/themes/
root.setThemes([
  am5themes_Animated.new(root)
]);

// Create chart
// https://www.amcharts.com/docs/v5/charts/xy-chart/
var chart = root.container.children.push(
  am5xy.XYChart.new(root, {
    focusable: true,
    panX: true,
    panY: true,
    wheelX: "panX",
    wheelY: "zoomX"
  })
);

var easing = am5.ease.linear;
chart.get("colors").set("step", 3);

// Create axes
// https://www.amcharts.com/docs/v5/charts/xy-chart/axes/
/*
var xAxis = chart.xAxes.push(
  am5xy.DateAxis.new(root, {
    maxDeviation: 0.1,
    groupData: false,
    baseInterval: {
      timeUnit: "day",
      count: 1
    },
    renderer: am5xy.AxisRendererX.new(root, {}),
    tooltip: am5.Tooltip.new(root, {})
  })
);
*/
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


function createAxisAndSeries(startValue, opposite, name) {
  var yRenderer = am5xy.AxisRendererY.new(root, {
    opposite: opposite,
    pan:"zoom"
  });
  var yAxis = chart.yAxes.push(
    am5xy.ValueAxis.new(root, {
      maxDeviation: 1,
      renderer: yRenderer
    })
  );

  if (chart.yAxes.indexOf(yAxis) > 0) {
    yAxis.set("syncWithAxis", chart.yAxes.getIndex(0));
  }
  // Add series
  // https://www.amcharts.com/docs/v5/charts/xy-chart/series/
  var series = chart.series.push(
    am5xy.LineSeries.new(root, {
      name: name,
      xAxis: xAxis,
      yAxis: yAxis,
      valueYField: "value",
      valueXField: "date",
      legendValueText: "{valueY}",
      tooltip: am5.Tooltip.new(root, {
        pointerOrientation: "horizontal",
        labelText: "{valueY}"
      })
    })
  );

  //series.fills.template.setAll({ fillOpacity: 0.2, visible: true });
  series.strokes.template.setAll({ strokeWidth: 1 });

  yRenderer.grid.template.set("strokeOpacity", 0.05);
  yRenderer.labels.template.set("fill", series.get("fill"));
  yRenderer.setAll({
    stroke: series.get("fill"),
    strokeOpacity: 1,
    opacity: 1
  });

  var data = generateChartData(startValue, name);
  console.log('data2',data);
  series.data.setAll(data);

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


}

// Add cursor
// https://www.amcharts.com/docs/v5/charts/xy-chart/cursor/
var cursor = chart.set("cursor", am5xy.XYCursor.new(root, {
  xAxis: xAxis,
  behavior: "none"
}));
cursor.lineY.set("visible", false);

// add scrollbar
chart.set("scrollbarX", am5.Scrollbar.new(root, {
  orientation: "horizontal"
}));

createAxisAndSeries(100, false, "ett");
createAxisAndSeries(1000, true, "två");
createAxisAndSeries(8000, true, "tre");

createAxisAndSeries(0, false, "dailyPrices");

// Make stuff animate on load
// https://www.amcharts.com/docs/v5/concepts/animations/
chart.appear(1000, 100);

// Generates random data, quite different range
function generateChartData(value, name) {
  security = selectedSecurity;
  strategy = selectedStrategy;
  var dailyPricesUrl = "dailyPrices?security="+security;
  var indicatorValueUrl = "indicatorValues?security="+security+"&strategy="+strategy;
  var data = [];
  //var firstDate = new Date();
  //firstDate.setDate(firstDate.getDate() - 100);
  //firstDate.setHours(0, 0, 0, 0);

  if (name === "dailyPrices") {
    am5.net.load(dailyPricesUrl).then(function(result) {
      const dailyPrices = am5.JSONParser.parse(result.response);
      console.log('dailyPrices',dailyPrices);
      for (let i in dailyPrices) {
         data.push({
           date: dailyPrices[i].date,
           value: dailyPrices[i].value
         });
      }
      console.log('data',data);
      return data;
    }).catch(function(result) {
      console.log("Error loading dailyPrices " + result);
    });
  } else {
    var firstDate = new Date();
    firstDate.setDate(firstDate.getDate() - 100);
    firstDate.setHours(0, 0, 0, 0);
      for (var i = 0; i < 100; i++) {
        var newDate = new Date(firstDate);
        newDate.setDate(newDate.getDate() + i);

        value += Math.round(
          ((Math.random() < 0.5 ? 1 : -1) * Math.random() * value) / 20
        );

        data.push({
          date: newDate.getTime(),
          value: value
        });
      }
      console.log('data',data);
      return data;
  }


}

}

