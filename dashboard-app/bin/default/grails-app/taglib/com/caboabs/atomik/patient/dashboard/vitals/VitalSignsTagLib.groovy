package com.caboabs.atomik.patient.dashboard.vitals

import groovy.json.JsonBuilder

class VitalSignsTagLib {

    static namespace = "vitals"
    static defaultEncodeAs = [taglib:'raw']
    //static encodeAsForTags = [tagName: [taglib:'html'], otherTagName: [taglib:'none']]


    /**
     * Renders the complete vital signs dashboard
     * @param patient - Patient object with basic info
     * @param queryResult - QueryResult instance with vital signs data
     * @param monitoringPlan - String name of monitoring plan
     * @param startDate - Date when monitoring started
     */
    def dashboard = { attrs ->
        def patient = attrs.patient
        def queryResult = attrs.queryResult
        def monitoringPlan = attrs.monitoringPlan ?: "Standard Monitoring"
        def startDate = attrs.startDate ?: new Date()
        
        // Transform data for dashboard
        def dashboardData = transformQueryResult(queryResult)
        def patientInfo = extractPatientInfo(patient)
        def summaryData = calculateSummaryData(dashboardData, startDate)
        def currentStatus = calculateCurrentStatus(dashboardData)
        def alerts = calculateAlerts(dashboardData)
        
        out << render(template: '/vitals/dashboard', model: [
            patientInfo: patientInfo,
            monitoringPlan: monitoringPlan,
            summaryData: summaryData,
            currentStatus: currentStatus,
            alerts: alerts,
            vitalsData: dashboardData,
            vitalsDataJson: new JsonBuilder(dashboardData).toString()
        ])
    }

    // TAS Calculation
    // we could pass the datapoint directly
    private def calculateTAS(double hr, double sbp, double dbp, double rr, double spo2, double temp)
    {
        // Normal ranges: [min, max]
        def hr_range   = [60, 100]    // heart rate / pulse
        def sbp_range  = [90, 120]    // systolic blood pressure
        def dbp_range  = [60, 80]     // diastolic blood pressure
        def rr_range   = [12, 20]     // respiratory rate
        def spo2_range = [95, 100]    // oxygen saturation
        def temp_range = [36.1, 37.2] // body temperature in Celsius

        // Weights
        double w_hr   = 0.2
        double w_sbp  = 0.2
        double w_dbp  = 0.1
        double w_rr   = 0.2
        double w_spo2 = 0.2
        double w_temp = 0.1

        // Helper to calculate score based on range
        def score = { value, range, weight ->
            if (value >= range[0] && value <= range[1]) return 0.0            // normal

            def deviation
            if (value < range[0]) {                                           // below normal
                deviation = ((range[0] - value) / range[0]) * 100
            } else {                                                          // above normal
                deviation = ((value - range[1]) / range[1]) * 100
            }

            return deviation * weight
        }

        double score_hr   = score(hr, hr_range, w_hr)
        double score_sbp  = score(sbp, sbp_range, w_sbp)
        double score_dbp  = score(dbp, dbp_range, w_dbp)
        double score_rr   = score(rr, rr_range, w_rr)
        double score_spo2 = score(spo2, spo2_range, w_spo2)
        double score_temp = score(temp, temp_range, w_temp)

        // Total TAS
        double tas = score_hr + score_sbp + score_dbp + score_rr + score_spo2 + score_temp

        println "Input values: HR=${hr}, SBP=${sbp}, DBP=${dbp}, RR=${rr}, SpO2=${spo2}, Temp=${temp}"

        println "TAS components: HR=${score_hr}, SBP=${score_sbp}, DBP=${score_dbp}, RR=${score_rr}, SpO2=${score_spo2}, Temp=${score_temp} => TAS=${tas}"

        return tas
    }
    
    /**
     * Transform QueryResult into dashboard-compatible data structure
     */
    private List transformQueryResult(queryResult) {

        def headers = queryResult.headers ?: [] // List<QueryResultHeader>

        def vitalsData = []
        
        queryResult?.result?.eachWithIndex { item, i ->

            //println item // QueryResultItemProjections

            def dataPoint = [
                timestamp: new Date() - i, //item.timestamp, // FIXME: this is missing form the result in Atomik!
                systolic: null,
                diastolic: null,
                pulseAvg: null,
                pulseVar: null,
                breathing: null,
                tas: null,
                spo2: null,
                temp: null
            ]
            
            item.projections?.each { projection ->

                def value = extractValue(projection)
                def vitalType = determineVitalType(headers, projection)

                //println projection // QueryResultProjection

                // println "Projection: ${projection.archetypeId} ${projection.path} -> ${vitalType} = ${value}"
                
                switch(vitalType) {
                    case 'systolic_bp':
                        dataPoint.systolic = value
                    break
                    case 'diastolic_bp':
                        dataPoint.diastolic = value
                    break
                    case 'pulse_rate':
                        dataPoint.pulseAvg = value
                    break
                    case 'pulse_variability':
                        dataPoint.pulseVar = value
                    break
                    case 'breathing_rate':
                        dataPoint.breathing = value
                    break
                    case 'total_anomaly_score':
                        dataPoint.tas = value
                    break
                    case 'oxygen_saturation':
                        dataPoint.spo2 = value
                    break
                    case 'body_temperature':
                        dataPoint.temp = value
                    break
                }
            }

            dataPoint.tas = calculateTAS(
                dataPoint.pulseAvg ?: 0,
                dataPoint.systolic ?: 0,
                dataPoint.diastolic ?: 0,
                dataPoint.breathing ?: 0,
                dataPoint.spo2 ?: 0,
                dataPoint.temp ?: 0
            )
            
            vitalsData << dataPoint


        }
        
        return vitalsData.sort { it.timestamp }
    }
    
    /**
     * Extract numeric value from DvQuantity or DvProportion
     */
    private Double extractValue(projection) {

        def dataValue = projection.value
        Double value = null

        switch (projection.type) {
            case 'DV_QUANTITY':
                value = dataValue.magnitude as Double
            break
            case 'DV_PROPORTION':
                value = (dataValue.numerator / dataValue.denominator * 100) as Double
            break
            default:
                println "Unsupported projection type: ${projection.type}"
        }
        
        return value
    }
    
    private String getNameFromHeaders(headers, projection) {
        def header = headers.find { it.archetypeId == projection.archetypeId && it.path == projection.path }
        return header?.name ?: "Unknown"
    }

    /**
     * Determine vital sign type from projection metadata
     * This would need to be adapted based on your actual data structure
     */
    private String determineVitalType(headers, projection) {
        // This is a placeholder - you'll need to implement based on your data structure
        // You might have terminology codes, names, or other identifiers
        // FIXME: we don't have these in the results...
        def name = getNameFromHeaders(headers, projection).toLowerCase()

        //println "Name: "+ name
        
        if (name.contains('systolic')  || name.contains('sys')) return 'systolic_bp'
        if (name.contains('diastolic') || name.contains('dia')) return 'diastolic_bp'
        
        if (
            projection.archetypeId.contains('pulse_oximetry') &&
            projection.path.equals("/data[at0001]/events[at0002]/data[at0003]/items[at0006]/value")
        ) return 'oxygen_saturation'
        
        // NOTE: pulse alone will match pulse_oximetry
        if (projection.archetypeId.contains('pulse.v2') && !name.contains('var')) return 'pulse_rate'

        // FIXME: pulse variability should be calculated considering the series data,
        // or maybe continuous readings in the same COMPO/OBSERVATION
        if (projection.archetypeId.contains('pulse') && name.contains('var')) return 'pulse_variability'
        if (projection.archetypeId.contains('respiration'))     return 'breathing_rate'
        if (name.contains('anomaly')  || name.contains('tas'))  return 'total_anomaly_score'


        if (name.contains('temp')     || name.contains('body')) return 'body_temperature'
        
        return 'unknown'
    }
    
    /**
     * Extract patient information
     */
    private Map extractPatientInfo(patient) {
        return [
            name: patient?.name ?: "Unknown Patient",
            id: patient?.id ?: "N/A",
            sex: patient?.gender ?: patient?.sex ?: "Unknown",
            age: patient?.age ?: calculateAge(patient?.dateOfBirth) ?: "Unknown"
        ]
    }
    
    /**
     * Calculate summary statistics
     */
    private Map calculateSummaryData(vitalsData, startDate) {
        def totalReadings = vitalsData.size()
        def daysSinceStart = startDate ? ((new Date().time - startDate.time) / (1000 * 60 * 60 * 24)) as Integer : 0
        
        return [
            totalReadings: totalReadings,
            daysSinceStart: daysSinceStart,
            startDate: startDate
        ]
    }
    
    /**
     * Calculate current status from most recent readings
     */
    private Map calculateCurrentStatus(vitalsData) {
        if (!vitalsData) return [:]
        
        def latest = vitalsData.last()
        
        return [
            bloodPressure: [
                systolic: latest.systolic,
                diastolic: latest.diastolic,
                status: getBloodPressureStatus(latest.systolic, latest.diastolic)
            ],
            pulseRate: [
                value: latest.pulseAvg,
                status: getPulseStatus(latest.pulseAvg)
            ],
            breathingRate: [
                value: latest.breathing,
                status: getBreathingStatus(latest.breathing)
            ],
            anomalyScore: [
                total: latest.tas,
                status: getAnomalyStatus(latest.tas)
            ]
        ]
    }
    
    /**
     * Calculate alerts based on vital signs
     */
    private Map calculateAlerts(vitalsData) {
        def alerts = [
            bpHigh: 0,
            critical: 0,
            total: 0
        ]
        
        vitalsData.each { reading ->
            if (reading.systolic > 140 || reading.diastolic > 90) {
                alerts.bpHigh++
                alerts.total++
            }
            if (reading.systolic > 180 || reading.diastolic > 110 || reading.pulseAvg > 120 || reading.pulseAvg < 40) {
                alerts.critical++
                alerts.total++
            }
        }
        
        return alerts
    }
    
    // Status determination methods
    private String getBloodPressureStatus(systolic, diastolic) {
        if (!systolic || !diastolic) return 'unknown'
        if (systolic > 130 || diastolic > 90) return 'warning'
        if (systolic > 160 || diastolic > 110) return 'danger'
        return 'good'
    }
    
    private String getPulseStatus(pulse) {
        if (!pulse) return 'unknown'
        if (pulse < 60 || pulse > 100) return 'warning'
        if (pulse < 40 || pulse > 120) return 'danger'
        return 'good'
    }
    
    private String getBreathingStatus(breathing) {
        if (!breathing) return 'unknown'
        if (breathing < 12 || breathing > 20) return 'warning'
        if (breathing < 8 || breathing > 25) return 'danger'
        return 'good'
    }
    
    private String getAnomalyStatus(score) {
        if (!score) return 'unknown'
        if (score > 1.5) return 'warning'
        if (score > 3.0) return 'danger'
        return 'good'
    }
    
    private Integer calculateAge(dateOfBirth) {
        if (!dateOfBirth) return null
        def now = new Date()
        def age = (now.time - dateOfBirth.time) / (365.25 * 24 * 60 * 60 * 1000)
        return age as Integer
    }

    // Helper function to determine cell highlighting class based on vital sign values
    def getVitalStatus = { attrs, body ->

        def value = attrs.value
        def type = attrs.type

        //println "getVitalStatus: type=${type}, value=${value}"

        if (!value) return ""
        
        def numValue = value as Double
        
        switch(type) {
            case 'systolic':
                if (numValue >= 160) out << "cell-danger"           // Hypertensive crisis
                else if (numValue >= 130) out << "cell-warning"     // High blood pressure
                else if (numValue < 70)   out << "cell-danger"      // Very Low blood pressure
                else if (numValue < 90)   out << "cell-warning"     // Low blood pressure
            break
                
            case 'diastolic':
                if (numValue >= 110) out << "cell-danger"          // Hypertensive crisis
                else if (numValue >= 90) out << "cell-warning"     // High blood pressure
                else if (numValue < 60) out << "cell-warning"      // Low blood pressure
            break
                
            case 'pulse':
                if (numValue >= 120 || numValue <= 40) out << "cell-danger"  // Severe tachycardia/bradycardia
                else if (numValue >= 100 || numValue <= 50) out << "cell-warning" // Mild tachycardia/bradycardia
            break
                
            case 'pulseVar':
                if (numValue >= 15) out << "cell-danger"            // Very high variability
                else if (numValue >= 10) out << "cell-warning"      // High variability
            break
                
            case 'breathing':
                if (numValue >= 25 || numValue <= 8) out << "cell-danger"   // Severe abnormal
                else if (numValue >= 20 || numValue <= 12) out << "cell-warning" // Mild abnormal
            break
                
            case 'tas':
                if (numValue >= 3.0) out << "cell-danger"         // High anomaly score
                else if (numValue >= 1.5) out << "cell-warning"   // Moderate anomaly score
            break
                
            case 'spo2':
                if (numValue < 90) out << "cell-danger"           // Severe hypoxemia
                else if (numValue < 95) out << "cell-warning"     // Mild hypoxemia
            break
                
            case 'temperature':
                if (numValue >= 39 || numValue <= 35)          out << "cell-danger"   // Severe fever/hypothermia
                else if (numValue >= 37.8 || numValue <= 36.1) out << "cell-warning" // Mild fever/low temp
            break
        }
    }
}
