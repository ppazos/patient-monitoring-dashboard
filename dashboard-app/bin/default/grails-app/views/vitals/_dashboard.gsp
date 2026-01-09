<%-- grails-app/views/vitals/_dashboard.gsp --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="groovy.json.JsonBuilder" %>

<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Patient Vital Signs Dashboard</title>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.9.1/chart.min.js"></script>
    <style>
      * {
        margin: 0;
        padding: 0;
        box-sizing: border-box;
      }

      body {
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        background-color: #f8f9fa;
        color: #333;
        line-height: 1.6;
      }

      .container {
        max-width: 1400px;
        margin: 0 auto;
        padding: 20px;
      }

      .patient-header {
        background: white;
        border-radius: 8px;
        padding: 24px;
        margin-bottom: 24px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      }

      .patient-info {
        display: flex;
        gap: 32px;
        align-items: center;
      }

      .patient-name {
        font-size: 24px;
        font-weight: 600;
        color: #2c3e50;
      }

      .patient-details {
        display: flex;
        gap: 24px;
        font-size: 14px;
        color: #666;
      }

      .card-row {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 20px;
        margin-bottom: 24px;
      }

      .card {
        background: white;
        border-radius: 8px;
        padding: 20px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      }

      .card-title {
        font-size: 14px;
        color: #666;
        margin-bottom: 8px;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .card-value {
        font-size: 20px;
        font-weight: 600;
        color: #2c3e50;
      }

      .badge {
        display: inline-block;
        padding: 4px 8px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 500;
        margin-right: 8px;
      }

      .badge-warning {
        background-color: #fff3cd;
        color: #856404;
      }

      .badge-danger {
        background-color: #f8d7da;
        color: #721c24;
      }

      .badge-success {
        background-color: #d1edff;
        color: #0c5460;
      }

      .readings-section {
        background: white;
        border-radius: 8px;
        padding: 24px;
        margin-bottom: 24px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      }

      .section-title {
        font-size: 18px;
        font-weight: 600;
        margin-bottom: 16px;
        color: #2c3e50;
      }

      .readings-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 13px;
      }

      .readings-table th,
      .readings-table td {
        text-align: left;
        padding: 12px 8px;
        border-bottom: 1px solid #eee;
      }

      .readings-table th {
        background-color: #f8f9fa;
        font-weight: 600;
        color: #666;
      }

      .readings-table tbody tr:hover {
        background-color: #f8f9fa;
      }

      .chart-container {
        background: white;
        border-radius: 8px;
        padding: 24px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      }

      .chart-controls {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
        margin-bottom: 20px;
      }

      .chart-toggle {
        display: flex;
        align-items: center;
        gap: 6px;
      }

      .chart-toggle input[type="checkbox"] {
        margin: 0;
      }

      .chart-toggle label {
        font-size: 13px;
        cursor: pointer;
      }

      .anomaly-score {
        display: flex;
        gap: 16px;
        align-items: center;
      }

      .anomaly-detail {
        font-size: 12px;
        color: #666;
      }

      .status-good { color: #28a745; }
      .status-warning { color: #ffc107; }
      .status-danger { color: #dc3545; }
      .status-unknown { color: #6c757d; }

      /* Table cell highlighting for abnormal values */
      .cell-warning {
        background-color: #fff3cd !important;
        color: #856404;
        font-weight: 500;
      }

      .cell-danger {
        background-color: #f8d7da !important;
        color: #721c24;
        font-weight: 600;
      }

      .readings-table td.cell-warning,
      .readings-table td.cell-danger {
        border-left: 3px solid transparent;
      }

      .readings-table td.cell-warning {
        border-left-color: #ffc107;
      }

      .readings-table td.cell-danger {
        border-left-color: #dc3545;
      }
    </style>
  </head>
  <body>
    <div class="container">
      <!-- Patient Header -->
      <div class="patient-header">
        <div class="patient-info">
          <div>
            <div class="patient-name">${patientInfo.name}</div>
            <div class="patient-details">
              <span>ID: ${patientInfo.id}</span>
              <span>${patientInfo.sex}</span>
              <span>Age: ${patientInfo.age}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Summary Cards -->
      <div class="card-row">
        <div class="card">
          <div class="card-title">Monitoring Plan</div>
          <div class="card-value">${monitoringPlan}</div>
        </div>
        <div class="card">
          <div class="card-title">Start Date</div>
          <div class="card-value">
            <g:formatDate date="${summaryData.startDate}" format="MMM dd, yyyy"/>
          </div>
        </div>
        <div class="card">
          <div class="card-title">Total Readings</div>
          <div class="card-value">${summaryData.totalReadings}</div>
        </div>
        <div class="card">
          <div class="card-title">Alerts</div>
          <div class="card-value">
            <g:if test="${alerts.bpHigh > 0}">
              <span class="badge badge-warning">${alerts.bpHigh} BP High</span>
            </g:if>
            <g:if test="${alerts.critical > 0}">
              <span class="badge badge-danger">${alerts.critical} Critical</span>
            </g:if>
            <g:if test="${alerts.total == 0}">
              <span class="badge badge-success">No Alerts</span>
            </g:if>
          </div>
        </div>
      </div>

      <!-- Current Status Cards -->
      <div class="card-row">
        <div class="card">
          <div class="card-title">Blood Pressure</div>
          <div class="card-value status-${currentStatus.bloodPressure.status}">
            <g:if test="${currentStatus.bloodPressure.systolic && currentStatus.bloodPressure.diastolic}">
              ${Math.round(currentStatus.bloodPressure.systolic)}/${Math.round(currentStatus.bloodPressure.diastolic)} mmHg
            </g:if>
            <g:else>
              No Data
            </g:else>
          </div>
        </div>
        <div class="card">
          <div class="card-title">Average Pulse Rate</div>
          <div class="card-value status-${currentStatus.pulseRate.status}">
            <g:if test="${currentStatus.pulseRate.value}">
              ${Math.round(currentStatus.pulseRate.value)} bpm
            </g:if>
            <g:else>
              No Data
            </g:else>
          </div>
        </div>
        <div class="card">
          <div class="card-title">Breathing Rate</div>
          <div class="card-value status-${currentStatus.breathingRate.status}">
            <g:if test="${currentStatus.breathingRate.value}">
              ${Math.round(currentStatus.breathingRate.value)} rpm
            </g:if>
            <g:else>
              No Data
            </g:else>
          </div>
        </div>
        <div class="card">
          <div class="card-title">Total Anomaly Score</div>
          <div class="card-value status-${currentStatus.anomalyScore.status}">
            <g:if test="${currentStatus.anomalyScore.total}">
              <div class="anomaly-score">
                <span>${String.format("%.1f", currentStatus.anomalyScore.total)}</span>
              </div>
            </g:if>
            <g:else>
              No Data
            </g:else>
          </div>
        </div>
      </div>

      <!-- Readings Table -->
      <div class="readings-section">
        <div class="section-title">Recent Readings</div>
        <table class="readings-table">
          <thead>
            <tr>
              <th>Timestamp</th>
              <th>Systolic BP</th>
              <th>Diastolic BP</th>
              <th>Pulse Avg</th>
              <th>Pulse Var</th>
              <th>Breathing</th>
              <th>TAS</th>
              <th>SpO2</th>
              <th>Temp</th>
            </tr>
          </thead>
          <tbody>
            <g:if test="${vitalsData}">
              <g:each in="${vitalsData.reverse()}" var="reading">
                <tr>
                  <td>
                    <g:formatDate date="${reading.timestamp}" format="MM/dd HH:mm"/>
                  </td>
                  <td class="${vitals.getVitalStatus(value: reading.systolic, type: 'systolic')}">
                    <g:if test="${reading.systolic}">
                      ${Math.round(reading.systolic)}
                    </g:if>
                    <g:else>---</g:else>
                  </td>
                  <td class="${vitals.getVitalStatus(value: reading.diastolic, type: 'diastolic')}">
                    <g:if test="${reading.diastolic}">
                      ${Math.round(reading.diastolic)}
                    </g:if>
                    <g:else>---</g:else>
                  </td>
                  <td class="${vitals.getVitalStatus(value: reading.pulseAvg, type: 'pulse')}">
                    <g:if test="${reading.pulseAvg}">
                      ${Math.round(reading.pulseAvg)}
                    </g:if>
                    <g:else>---</g:else>
                  </td>
                  <td class="${vitals.getVitalStatus(value: reading.pulseVar, type: 'pulseVar')}">
                    <g:if test="${reading.pulseVar}">
                      ${String.format("%.1f", reading.pulseVar)}
                    </g:if>
                    <g:else>---</g:else>
                  </td>
                  <td class="${vitals.getVitalStatus(value: reading.breathing, type: 'breathing')}">
                    <g:if test="${reading.breathing}">
                      ${Math.round(reading.breathing)}
                    </g:if>
                    <g:else>---</g:else>
                  </td>
                  <td class="${vitals.getVitalStatus(value: reading.tas, type: 'tas')}">
                    <g:if test="${reading.tas}">
                      ${String.format("%.1f", reading.tas)}
                    </g:if>
                    <g:else>---</g:else>
                  </td>
                  <td class="${vitals.getVitalStatus(value: reading.spo2, type: 'spo2')}">
                    <g:if test="${reading.spo2}">
                      ${String.format("%.1f", reading.spo2)}%
                    </g:if>
                    <g:else>---</g:else>
                  </td>
                  <td class="${vitals.getVitalStatus(value: reading.temp, type: 'temperature')}">
                    <g:if test="${reading.temp}">
                      ${String.format("%.1f", reading.temp)}°C
                    </g:if>
                    <g:else>---</g:else>
                  </td>
                </tr>
              </g:each>
            </g:if>
            <g:else>
              <tr>
                <td colspan="9" style="text-align: center; color: #666; padding: 20px;">
                  No readings available
                </td>
              </tr>
            </g:else>
          </tbody>
        </table>
      </div>

      <!-- Chart -->
      <div class="chart-container">
        <div class="section-title">Vital Signs Trends</div>
        <div class="chart-controls">
          <div class="chart-toggle">
            <input type="checkbox" id="systolic" checked>
            <label for="systolic">Systolic BP</label>
          </div>
          <div class="chart-toggle">
            <input type="checkbox" id="diastolic" checked>
            <label for="diastolic">Diastolic BP</label>
          </div>
          <div class="chart-toggle">
            <input type="checkbox" id="pulse" checked>
            <label for="pulse">Pulse Rate</label>
          </div>
          <div class="chart-toggle">
            <input type="checkbox" id="breathing">
            <label for="breathing">Breathing Rate</label>
          </div>
          <div class="chart-toggle">
            <input type="checkbox" id="tas">
            <label for="tas">TAS</label>
          </div>
          <div class="chart-toggle">
            <input type="checkbox" id="spo2">
            <label for="spo2">SpO2</label>
          </div>
          <div class="chart-toggle">
            <input type="checkbox" id="temp">
            <label for="temp">Temperature</label>
          </div>
        </div>
        <canvas id="vitalsChart" width="400" height="200"></canvas>
      </div>
    </div>

    <script>
      // Inject the real data from Grails
      const vitalsData = ${raw(vitalsDataJson)};
      
      // Prepare chart data
      const chartLabels = vitalsData.map(d => {
        const date = new Date(d.timestamp);
        return date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
      });

      // Chart setup
      const ctx = document.getElementById('vitalsChart').getContext('2d');
      
      const chartConfig = {
        type: 'line',
        data: {
          labels: chartLabels,
          datasets: [
            {
              label: 'Systolic BP',
              data: vitalsData.map(d => d.systolic),
              borderColor: '#e74c3c',
              backgroundColor: 'rgba(231, 76, 60, 0.1)',
              tension: 0.4,
              yAxisID: 'y'
            },
            {
              label: 'Diastolic BP',
              data: vitalsData.map(d => d.diastolic),
              borderColor: '#c0392b',
              backgroundColor: 'rgba(192, 57, 43, 0.1)',
              tension: 0.4,
              yAxisID: 'y'
            },
            {
              label: 'Pulse Rate',
              data: vitalsData.map(d => d.pulseAvg),
              borderColor: '#3498db',
              backgroundColor: 'rgba(52, 152, 219, 0.1)',
              tension: 0.4,
              yAxisID: 'y'
            },
            {
              label: 'Breathing Rate',
              data: vitalsData.map(d => d.breathing),
              borderColor: '#2ecc71',
              backgroundColor: 'rgba(46, 204, 113, 0.1)',
              tension: 0.4,
              hidden: true,
              yAxisID: 'y'
            },
            {
              label: 'TAS',
              data: vitalsData.map(d => d.tas),
              borderColor: '#f39c12',
              backgroundColor: 'rgba(243, 156, 18, 0.1)',
              tension: 0.4,
              hidden: true,
              yAxisID: 'y1'
            },
            {
              label: 'SpO2',
              data: vitalsData.map(d => d.spo2),
              borderColor: '#9b59b6',
              backgroundColor: 'rgba(155, 89, 182, 0.1)',
              tension: 0.4,
              hidden: true,
              yAxisID: 'y'
            },
            {
              label: 'Temperature',
              data: vitalsData.map(d => d.temp),
              borderColor: '#e67e22',
              backgroundColor: 'rgba(230, 126, 34, 0.1)',
              tension: 0.4,
              hidden: true,
              yAxisID: 'y'
            }
          ]
        },
        options: {
          responsive: true,
          interaction: {
            mode: 'index',
            intersect: false,
          },
          scales: {
            x: {
              display: true,
              title: {
                display: true,
                text: 'Time'
              }
            },
            y: {
              type: 'linear',
              display: true,
              position: 'left',
              title: {
                display: true,
                text: 'Primary Vitals'
              }
            },
            y1: {
              type: 'linear',
              display: true,
              position: 'right',
              title: {
                display: true,
                text: 'TAS'
              },
              grid: {
                drawOnChartArea: false,
              },
            }
          },
          plugins: {
            legend: {
              display: false
            }
          }
        }
      };

      const chart = new Chart(ctx, chartConfig);

      // Chart toggle functionality
      const toggles = document.querySelectorAll('.chart-toggle input');
      const datasetMap = {
        'systolic': 0,
        'diastolic': 1,
        'pulse': 2,
        'breathing': 3,
        'tas': 4,
        'spo2': 5,
        'temp': 6
      };

      toggles.forEach(toggle => {
        toggle.addEventListener('change', function() {
          const datasetIndex = datasetMap[this.id];
          const dataset = chart.data.datasets[datasetIndex];
          dataset.hidden = !this.checked;
          chart.update();
        });
      });

      // Handle empty data gracefully
      if (!vitalsData || vitalsData.length === 0) {
        document.getElementById('vitalsChart').style.display = 'none';
        const chartContainer = document.querySelector('.chart-container');
        chartContainer.innerHTML += '<div style="text-align: center; color: #666; padding: 40px;">No data available for chart</div>';
      }
    </script>
  </body>
</html>