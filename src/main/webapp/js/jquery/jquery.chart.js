/*
 * A small utility to use the Google Chart API to display a chart with data from
 * a JSON request to /chart/
 */
(function($) {
	$.chart = function(options) {
		var stringToFunction = function(str) {
			var arr = str.split(".");

			var fn = (window || this);
			for (var i = 0, len = arr.length; i < len; i++) {
				fn = fn[arr[i]];
			}

			if (typeof fn !== "function") {
				throw new Error("function not found");
			}

			return  fn;
		};
		
		$.getJSON("/chart/" + options.chartId, function(data) {
			var table = new google.visualization.DataTable();
			
			// dates can't be sent via json
			var dateCols = [];
			
			// add cols
			for (var i = 0; i < data.columns.length; i++) {
				var column = data.columns[i];
				
				if (column.type === "date") {
					dateCols.push(i);
				}
				
				table.addColumn(column);
			}
			
			// fix date rows
			data.rows.forEach(function(row) {
				dateCols.forEach(function(col) {
					var millisDate = row[col];
					row[col] = new Date(millisDate);
				});
			});
			
			// add rows
			table.addRows(data.rows);
			
			// determine the chart class - plugins first
			var className;
			if (data.chartType === "Timeline") {
				className = "links.Timeline";
			} else {
				className = "google.visualization." + data.chartType;
			}
			
			// instantiate chart class
			var chartClass = stringToFunction(className);
			var chart = new chartClass(
					document.getElementById(options.containerId));
			
			if (data.options) {
				chart.draw(table, data.options);
			} else {
				chart.draw(table, data);
			}
			
			// timeline range hack
			if (data.chartType === "Timeline") {
				//chart.setVisibleChartRangeNow();
				var now = new Date().getTime();
				var week = 7 * 24 * 60 * 60 * 1000;
				
				var lastWeek = new Date(now - week);
				var nextWeek = new Date(now + week);
				
				chart.setVisibleChartRange(lastWeek, nextWeek);
			}
		});
	};
})(jQuery);
