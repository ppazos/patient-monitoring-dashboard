// Vital Signs Total Anomaly Score (TAS) Calculator

// Normal ranges for vital signs (adult values)
const NORMAL_RANGES = {
  SYS: { min: 90, max: 120, weight: 0.25 },    // Systolic BP (mmHg)
  DIA: { min: 60, max: 80, weight: 0.25 },     // Diastolic BP (mmHg)
  PRA: { min: 60, max: 100, weight: 0.20 },    // Pulse Rate Average (bpm)
  ABR: { min: 12, max: 20, weight: 0.15 },     // Average Breathing Rate (breaths/min)
  PRV: { min: 20, max: 50, weight: 0.15 }      // Pulse Rate Variability (%)
};

/**
 * Calculate anomaly score for a single vital sign
 * @param {number} value - The measured value
 * @param {object} range - Normal range with min, max, and weight
 * @returns {number} - Anomaly score (0-100)
 */
function calculateParameterAnomaly(value, range) {
  const { min, max } = range;
  
  // If within normal range, return 0
  if (value >= min && value <= max) {
    return 0;
  }
  
  // Calculate deviation percentage
  let deviation;
  if (value < min) {
    deviation = ((min - value) / min) * 100;
  } else {
    deviation = ((value - max) / max) * 100;
  }
  
  // Cap at 100% anomaly
  return Math.min(deviation, 100);
}

/**
 * Calculate Total Anomaly Score (TAS)
 * @param {object} vitals - Object containing vital sign measurements
 * @returns {object} - Contains TAS, DAS, ASS, and individual scores
 */
function calculateTAS(vitals) {
  const individualScores = {};
  let weightedSum = 0;
  let totalWeight = 0;
  let maxAnomaly = 0;
  let anomalyCount = 0;
  
  // Calculate individual anomaly scores
  for (const [param, value] of Object.entries(vitals)) {
    if (NORMAL_RANGES[param]) {
      const range = NORMAL_RANGES[param];
      const anomalyScore = calculateParameterAnomaly(value, range);
      
      individualScores[param] = {
        value: value,
        anomalyScore: Math.round(anomalyScore * 100) / 100,
        normal: anomalyScore === 0
      };
      
      // Weighted sum for TAS
      weightedSum += anomalyScore * range.weight;
      totalWeight += range.weight;
      
      // Track maximum anomaly for ASS
      maxAnomaly = Math.max(maxAnomaly, anomalyScore);
      
      // Count anomalies for DAS
      if (anomalyScore > 0) {
        anomalyCount++;
      }
    }
  }
  
  // Calculate scores
  const TAS = Math.round((weightedSum / totalWeight) * 100) / 100;
  const DAS = Math.round((anomalyCount / Object.keys(NORMAL_RANGES).length) * 100 * 100) / 100; // Deviation Anomaly Score
  const ASS = Math.round(maxAnomaly * 100) / 100; // Acute System Score (highest individual anomaly)
  
  return {
    TAS: TAS,
    DAS: DAS,
    ASS: ASS,
    individualScores: individualScores,
    summary: {
      totalParameters: Object.keys(NORMAL_RANGES).length,
      parametersWithAnomalies: anomalyCount,
      overallStatus: TAS < 10 ? 'Normal' : TAS < 25 ? 'Mild Concern' : TAS < 50 ? 'Moderate Concern' : 'High Concern'
    }
  };
}

// Test data examples
const testData = [
  {
    name: "Normal Adult",
    vitals: { SYS: 110, DIA: 70, PRA: 75, ABR: 16, PRV: 30 }
  },
  {
    name: "Hypertensive",
    vitals: { SYS: 150, DIA: 95, PRA: 80, ABR: 18, PRV: 25 }
  },
  {
    name: "Tachycardic",
    vitals: { SYS: 115, DIA: 75, PRA: 120, ABR: 22, PRV: 15 }
  },
  {
    name: "Multiple Anomalies",
    vitals: { SYS: 160, DIA: 100, PRA: 110, ABR: 25, PRV: 60 }
  }
];

// Run calculations and display results
console.log("=== VITAL SIGNS ANOMALY SCORE CALCULATOR ===\n");

testData.forEach((test, index) => {
  console.log(`${index + 1}. ${test.name}`);
  console.log("Vitals:", test.vitals);
  
  const results = calculateTAS(test.vitals);
  
  console.log(`TAS (Total Anomaly Score): ${results.TAS}%`);
  console.log(`DAS (Deviation Anomaly Score): ${results.DAS}%`);
  console.log(`ASS (Acute System Score): ${results.ASS}%`);
  console.log(`Overall Status: ${results.summary.overallStatus}`);
  
  console.log("Individual Parameter Scores:");
  for (const [param, data] of Object.entries(results.individualScores)) {
    const status = data.normal ? "✓ Normal" : "⚠ Anomaly";
    console.log(`  ${param}: ${data.value} (${data.anomalyScore}% anomaly) ${status}`);
  }
  
  console.log("-".repeat(50));
});

// Example usage function
function monitorVitals(SYS, DIA, PRA, ABR, PRV) {
  const vitals = { SYS, DIA, PRA, ABR, PRV };
  return calculateTAS(vitals);
}

// Example: Monitor a patient with high blood pressure
console.log("\n=== LIVE MONITORING EXAMPLE ===");
const patientResult = monitorVitals(145, 92, 85, 19, 28);
console.log("Patient Monitoring Result:", patientResult);