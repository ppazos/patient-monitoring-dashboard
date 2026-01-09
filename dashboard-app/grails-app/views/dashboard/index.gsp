<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Parameter Anomaly Deviation Chart</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background-color: #f9fafb;
            color: #374151;
            line-height: 1.6;
        }
        
        .container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 24px;
        }
        
        .header {
            text-align: center;
            margin-bottom: 32px;
        }
        
        .title {
            font-size: 2rem;
            font-weight: bold;
            color: #1f2937;
            margin-bottom: 16px;
        }
        
        .card {
            background: white;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
            margin-bottom: 24px;
            padding: 24px;
        }
        
        .card-title {
            font-size: 1.25rem;
            font-weight: 600;
            margin-bottom: 16px;
        }
        
        .parameter-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 12px;
        }
        
        .parameter-btn {
            padding: 12px;
            border: 2px solid #d1d5db;
            border-radius: 8px;
            background: white;
            cursor: pointer;
            transition: all 0.2s;
            text-align: center;
        }
        
        .parameter-btn:hover {
            border-color: #93c5fd;
        }
        
        .parameter-btn.active {
            border-color: #3b82f6;
            background-color: #eff6ff;
            color: #1d4ed8;
        }
        
        .parameter-name {
            font-weight: 600;
            font-size: 1.1rem;
        }
        
        .parameter-desc {
            font-size: 0.875rem;
            color: #6b7280;
        }
        
        .info-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 16px;
        }
        
        .info-box {
            padding: 16px;
            border-radius: 8px;
        }
        
        .info-box.normal {
            background-color: #f0fdf4;
            color: #166534;
        }
        
        .info-box.chart {
            background-color: #eff6ff;
            color: #1e40af;
        }
        
        .info-box.step {
            background-color: #fffbeb;
            color: #92400e;
        }
        
        .info-label {
            font-weight: 600;
            font-size: 0.875rem;
        }
        
        .info-value {
            font-size: 1.5rem;
            font-weight: bold;
            margin-top: 4px;
        }
        
        #chartContainer {
            width: 100%;
            height: 400px;
            position: relative;
            border: 1px solid #e5e7eb;
            border-radius: 8px;
            background: white;
        }
        
        .chart-svg {
            width: 100%;
            height: 100%;
        }
        
        .axis {
            stroke: #6b7280;
            stroke-width: 1;
        }
        
        .grid-line {
            stroke: #e5e7eb;
            stroke-width: 1;
            stroke-dasharray: 3,3;
        }
        
        .data-line {
            fill: none;
            stroke: #ef4444;
            stroke-width: 3;
        }
        
        .data-point {
            fill: #ef4444;
            stroke: white;
            stroke-width: 2;
            cursor: pointer;
        }
        
        .data-point:hover {
            r: 6;
            fill: #dc2626;
        }
        
        .reference-line {
            stroke: #10b981;
            stroke-width: 2;
            stroke-dasharray: 5,5;
        }
        
        .axis-label {
            font-size: 12px;
            fill: #374151;
            text-anchor: middle;
        }
        
        .chart-label {
            font-size: 14px;
            fill: #1f2937;
            font-weight: 600;
        }
        
        .tooltip {
            position: absolute;
            background: white;
            border: 1px solid #d1d5db;
            border-radius: 6px;
            padding: 8px 12px;
            font-size: 0.875rem;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            pointer-events: none;
            z-index: 1000;
            display: none;
        }
        
        .formula-section {
            background-color: #f9fafb;
            padding: 16px;
            border-radius: 8px;
            margin-top: 16px;
        }
        
        .formula-item {
            background: white;
            padding: 12px;
            border-radius: 6px;
            margin-bottom: 8px;
        }
        
        .formula-code {
            font-family: 'Courier New', monospace;
            background: #f3f4f6;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 0.875rem;
        }
    </style>
</head>
<body>

    <vitals:dashboard 
        patient="${[name:'Pablo Pazos', sex:'M', age: 43, id: 10666234]}" 
        queryResult="${data}"
        monitoringPlan="Preop"
        startDate="${(new Date() - 20)}" />

    <div class="container">
        <div class="header">
            <h1 class="title">Parameter Anomaly Deviation Analysis</h1>
        </div>
        
        <!-- Parameter Selection -->
        <div class="card">
            <h2 class="card-title">Select Vital Sign Parameter</h2>
            <div class="parameter-grid" id="parameterGrid">
                <!-- Parameters will be populated by JavaScript -->
            </div>
        </div>
        
        <!-- Parameter Info -->
        <div class="card">
            <h2 class="card-title" id="parameterTitle">Systolic BP - Normal Range Analysis</h2>
            <div class="info-grid" id="infoGrid">
                <!-- Info will be populated by JavaScript -->
            </div>
        </div>
        
        <!-- Chart -->
        <div class="card">
            <h2 class="card-title">Anomaly Percentage vs Parameter Value</h2>
            <div id="chartContainer">
                <svg class="chart-svg" id="chartSvg">
                    <!-- Chart will be drawn here -->
                </svg>
            </div>
            <div class="tooltip" id="tooltip"></div>
        </div>
        
        <!-- Formula Explanation -->
        <div class="card">
            <h2 class="card-title">Calculation Formula</h2>
            <div class="formula-section">
                <div class="formula-item">
                    <strong>For values below minimum:</strong>
                    <div class="formula-code">Anomaly% = ((min - value) / min) × 100</div>
                </div>
                <div class="formula-item">
                    <strong>For values above maximum:</strong>
                    <div class="formula-code">Anomaly% = ((value - max) / max) × 100</div>
                </div>
                <div class="formula-item">
                    <strong>For values within range:</strong>
                    <div class="formula-code">Anomaly% = 0</div>
                </div>
                <div style="font-size: 0.875rem; color: #6b7280; margin-top: 8px;">
                    * Maximum anomaly score is capped at 100%
                </div>
            </div>
        </div>
    </div>

    <asset:javascript src="dashboard.js" />
</body>
</html>