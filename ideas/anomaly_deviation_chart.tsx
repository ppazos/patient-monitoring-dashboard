import React, { useState } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceLine } from 'recharts';

const DeviationChart = () => {
  const [selectedParameter, setSelectedParameter] = useState('SYS');
  
  // Parameter definitions with extended ranges for visualization
  const parameters = {
    SYS: { 
      name: 'Systolic BP (mmHg)', 
      min: 90, 
      max: 120, 
      chartMin: 60, 
      chartMax: 180,
      step: 5
    },
    DIA: { 
      name: 'Diastolic BP (mmHg)', 
      min: 60, 
      max: 80, 
      chartMin: 40, 
      chartMax: 120,
      step: 2
    },
    PRA: { 
      name: 'Pulse Rate (bpm)', 
      min: 60, 
      max: 100, 
      chartMin: 30, 
      chartMax: 150,
      step: 5
    },
    ABR: { 
      name: 'Breathing Rate (breaths/min)', 
      min: 12, 
      max: 20, 
      chartMin: 5, 
      chartMax: 35,
      step: 1
    },
    PRV: { 
      name: 'Pulse Rate Variability (%)', 
      min: 20, 
      max: 50, 
      chartMin: 0, 
      chartMax: 80,
      step: 2
    }
  };

  // Calculate anomaly score for a single value
  const calculateParameterAnomaly = (value, range) => {
    const { min, max } = range;
    
    if (value >= min && value <= max) {
      return 0;
    }
    
    let deviation;
    if (value < min) {
      deviation = ((min - value) / min) * 100;
    } else {
      deviation = ((value - max) / max) * 100;
    }
    
    return Math.min(deviation, 100);
  };

  // Generate chart data for selected parameter
  const generateChartData = (paramKey) => {
    const param = parameters[paramKey];
    const data = [];
    
    for (let value = param.chartMin; value <= param.chartMax; value += param.step) {
      const anomalyScore = calculateParameterAnomaly(value, param);
      const status = value >= param.min && value <= param.max ? 'Normal' : 'Anomaly';
      
      data.push({
        value: value,
        anomalyPercentage: Math.round(anomalyScore * 10) / 10,
        status: status,
        inRange: status === 'Normal'
      });
    }
    
    return data;
  };

  const chartData = generateChartData(selectedParameter);
  const currentParam = parameters[selectedParameter];

  // Custom tooltip
  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div className="bg-white p-3 border border-gray-300 rounded-lg shadow-lg">
          <p className="font-semibold">{`Value: ${label}`}</p>
          <p className={`${data.inRange ? 'text-green-600' : 'text-red-600'}`}>
            {`Anomaly: ${data.anomalyPercentage}%`}
          </p>
          <p className={`text-sm ${data.inRange ? 'text-green-600' : 'text-red-600'}`}>
            {data.status}
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="w-full p-6 bg-gray-50 min-h-screen">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-800 mb-6 text-center">
          Parameter Anomaly Deviation Analysis
        </h1>
        
        {/* Parameter Selection */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">Select Vital Sign Parameter</h2>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
            {Object.entries(parameters).map(([key, param]) => (
              <button
                key={key}
                onClick={() => setSelectedParameter(key)}
                className={`p-3 rounded-lg border-2 transition-colors ${
                  selectedParameter === key
                    ? 'border-blue-500 bg-blue-50 text-blue-700'
                    : 'border-gray-300 bg-white hover:border-blue-300'
                }`}
              >
                <div className="font-semibold">{key}</div>
                <div className="text-sm text-gray-600">{param.name}</div>
              </button>
            ))}
          </div>
        </div>

        {/* Parameter Info */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">
            {currentParam.name} - Normal Range Analysis
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-green-50 p-4 rounded-lg">
              <div className="text-green-800 font-semibold">Normal Range</div>
              <div className="text-2xl font-bold text-green-700">
                {currentParam.min} - {currentParam.max}
              </div>
            </div>
            <div className="bg-blue-50 p-4 rounded-lg">
              <div className="text-blue-800 font-semibold">Chart Range</div>
              <div className="text-2xl font-bold text-blue-700">
                {currentParam.chartMin} - {currentParam.chartMax}
              </div>
            </div>
            <div className="bg-amber-50 p-4 rounded-lg">
              <div className="text-amber-800 font-semibold">Step Size</div>
              <div className="text-2xl font-bold text-amber-700">
                {currentParam.step}
              </div>
            </div>
          </div>
        </div>

        {/* Chart */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-semibold mb-4">Anomaly Percentage vs Parameter Value</h2>
          <div className="h-96">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 20 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis 
                  dataKey="value" 
                  label={{ value: `${currentParam.name}`, position: 'insideBottom', offset: -10 }}
                />
                <YAxis 
                  label={{ value: 'Anomaly Percentage (%)', angle: -90, position: 'insideLeft' }}
                  domain={[0, 100]}
                />
                <Tooltip content={<CustomTooltip />} />
                <Legend />
                
                {/* Reference lines for normal range */}
                <ReferenceLine 
                  x={currentParam.min} 
                  stroke="#10B981" 
                  strokeDasharray="5 5" 
                  label={{ value: "Min Normal", position: "topLeft" }}
                />
                <ReferenceLine 
                  x={currentParam.max} 
                  stroke="#10B981" 
                  strokeDasharray="5 5" 
                  label={{ value: "Max Normal", position: "topRight" }}
                />
                
                <Line
                  type="monotone"
                  dataKey="anomalyPercentage"
                  stroke="#EF4444"
                  strokeWidth={3}
                  dot={{ fill: '#EF4444', strokeWidth: 2, r: 4 }}
                  name="Anomaly %"
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Formula Explanation */}
        <div className="bg-white rounded-lg shadow-md p-6 mt-6">
          <h2 className="text-xl font-semibold mb-4">Calculation Formula</h2>
          <div className="space-y-3">
            <div className="bg-gray-50 p-4 rounded-lg">
              <strong>For values below minimum:</strong>
              <p className="font-mono">Anomaly% = ((min - value) / min) × 100</p>
            </div>
            <div className="bg-gray-50 p-4 rounded-lg">
              <strong>For values above maximum:</strong>
              <p className="font-mono">Anomaly% = ((value - max) / max) × 100</p>
            </div>
            <div className="bg-gray-50 p-4 rounded-lg">
              <strong>For values within range:</strong>
              <p className="font-mono">Anomaly% = 0</p>
            </div>
            <div className="text-sm text-gray-600">
              * Maximum anomaly score is capped at 100%
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DeviationChart;