import java.util.Random
import java.time.Instant
import java.time.format.DateTimeFormatter

class VitalSignsGenerator {
    
    static void main(String[] args) {
        def generator = new VitalSignsGenerator()
        generator.generateVitalSignsData()
    }
    
    void generateVitalSignsData() {
        // 1. Generate table with 100 rows of vital signs data
        def vitalSignsData = generateVitalSignsTable()
        
        // 2. Load JSON example in memory (pseudo-code - replace with your parser)
        def templateJson = loadJsonFromFile("sample_vitals_compo.json")
        
        // 3-5. Process each row and generate files
        vitalSignsData.eachWithIndex { row, index ->
            // Create a deep copy of the template
            def workingJson = deepCopyJson(templateJson)
            
            // Set the vital signs measurements
            setVitalSignsInJson(workingJson, row, index)
            
            // Serialize back to JSON string (pseudo-code - replace with your serializer)
            def jsonString = serializeToJsonString(workingJson)
            
            // Store the new file
            saveJsonToFile(jsonString, "vital_signs_${String.format('%03d', index + 1)}.json")
        }
        
        println "Generated 100 vital signs JSON files successfully!"
    }
    
    List<Map> generateVitalSignsTable() {
        Random random = new Random()
        List<Map> data = []
        
        for (int i = 0; i < 100; i++) {
            Map row = [:]
            
            // Blood Pressure (some out of range)
            if (random.nextDouble() < 0.8) {
                // Normal range: 90-140 systolic, 60-90 diastolic
                row.systolic = 90 + random.nextInt(51)      // 90-140
                row.diastolic = 60 + random.nextInt(31)     // 60-90
            } else {
                // Out of range values
                if (random.nextBoolean()) {
                    // High BP
                    row.systolic = 180 + random.nextInt(100)   // 180-279
                    row.diastolic = 100 + random.nextInt(50)   // 100-149
                } else {
                    // Low BP
                    row.systolic = 50 + random.nextInt(40)     // 50-89
                    row.diastolic = 30 + random.nextInt(30)    // 30-59
                }
            }
            
            // Body Temperature (some out of range)
            if (random.nextDouble() < 0.85) {
                // Normal range: 36.1-37.2°C
                row.temperature = 36.1 + random.nextDouble() * 1.1
            } else {
                // Out of range
                if (random.nextBoolean()) {
                    // Fever
                    row.temperature = 38.0 + random.nextDouble() * 3.0  // 38-41°C
                } else {
                    // Hypothermia
                    row.temperature = 33.0 + random.nextDouble() * 3.0  // 33-36°C
                }
            }
            
            // Heart Rate (some out of range)
            if (random.nextDouble() < 0.82) {
                // Normal range: 60-100 bpm
                row.heartRate = 60 + random.nextInt(41)     // 60-100
            } else {
                // Out of range
                if (random.nextBoolean()) {
                    // Tachycardia
                    row.heartRate = 120 + random.nextInt(80)   // 120-199
                } else {
                    // Bradycardia
                    row.heartRate = 40 + random.nextInt(20)    // 40-59
                }
            }
            
            // Oxygen Saturation (some out of range)
            if (random.nextDouble() < 0.88) {
                // Normal range: 95-100%
                row.oxygenSaturation = 95.0 + random.nextDouble() * 5.0
            } else {
                // Low oxygen saturation
                row.oxygenSaturation = 80.0 + random.nextDouble() * 15.0  // 80-94%
            }
            
            // Respiratory Rate (some out of range)
            if (random.nextDouble() < 0.83) {
                // Normal range: 12-20 breaths/min
                row.respiratoryRate = 12 + random.nextInt(9)    // 12-20
            } else {
                // Out of range
                if (random.nextBoolean()) {
                    // Tachypnea
                    row.respiratoryRate = 25 + random.nextInt(15)  // 25-39
                } else {
                    // Bradypnea
                    row.respiratoryRate = 6 + random.nextInt(6)    // 6-11
                }
            }
            
            // Additional pulse oximetry values
            row.spoc = 10.0 + random.nextDouble() * 5.0      // SpOC: 10-15 ml/dl
            row.spco = 1.0 + random.nextDouble() * 3.0       // SpCO: 1-4%
            row.spmet = 0.5 + random.nextDouble() * 2.0      // SpMet: 0.5-2.5%
            
            data.add(row)
        }
        
        return data
    }
    
    void setVitalSignsInJson(Map json, Map vitalSigns, int index) {
        String timestamp = Instant.now().toString()
        String patientId = "patient_${String.format('%03d', index + 1)}"
        
        // Update composer and context
        json.composer.name = "Dr. Smith"
        json.context.start_time.value = timestamp
        
        // Process each observation in the content array
        json.content.each { observation ->
            switch(observation.name.value) {
                case "Blood pressure":
                    updateBloodPressure(observation, vitalSigns, timestamp)
                    break
                case "Body temperature":
                    updateBodyTemperature(observation, vitalSigns, timestamp)
                    break
                case "Pulse/Heart beat":
                    updatePulseHeartBeat(observation, vitalSigns, timestamp)
                    break
                case "Pulse oximetry":
                    updatePulseOximetry(observation, vitalSigns, timestamp)
                    break
                case "Respiration":
                    updateRespiration(observation, vitalSigns, timestamp)
                    break
            }
        }
    }
    
    void updateBloodPressure(Map observation, Map vitalSigns, String timestamp) {
        observation.data.origin.value = timestamp
        observation.data.events[0].time.value = timestamp
        
        def dataItems = observation.data.events[0].data.items
        dataItems.find { it.name.value == "Systolic" }.value.magnitude = vitalSigns.systolic
        dataItems.find { it.name.value == "Diastolic" }.value.magnitude = vitalSigns.diastolic
        
        // Update clinical interpretation based on values
        def interpretation = getClinicalInterpretationBP(vitalSigns.systolic, vitalSigns.diastolic)
        dataItems.find { it.name.value == "Clinical interpretation" }.value.value = interpretation
        
        // Clear the random comment text
        dataItems.find { it.name.value == "Comment" }.value.value = "Generated test data"
    }
    
    void updateBodyTemperature(Map observation, Map vitalSigns, String timestamp) {
        observation.data.origin.value = timestamp
        observation.data.events[0].time.value = timestamp
        
        def dataItems = observation.data.events[0].data.items
        dataItems.find { it.name.value == "Temperature" }.value.magnitude = vitalSigns.temperature
        dataItems.find { it.name.value == "Temperature" }.value.units = "°C"
        
        // Clear the random comment text
        dataItems.find { it.name.value == "Comment" }.value.value = "Generated test data"
    }
    
    void updatePulseHeartBeat(Map observation, Map vitalSigns, String timestamp) {
        observation.data.origin.value = timestamp
        observation.data.events[0].time.value = timestamp
        
        def dataItems = observation.data.events[0].data.items
        dataItems.find { it.name.value == "Rate" }.value.magnitude = vitalSigns.heartRate
        
        // Update clinical interpretation based on heart rate
        def interpretation = getClinicalInterpretationHR(vitalSigns.heartRate)
        dataItems.find { it.name.value == "Clinical interpretation" }.value.value = interpretation
        
        // Clear random text fields
        dataItems.find { it.name.value == "Character" }.value.value = "Regular, strong"
        dataItems.find { it.name.value == "Clinical description" }.value.value = "Generated test data"
        dataItems.find { it.name.value == "Comment" }.value.value = "Generated test data"
    }
    
    void updatePulseOximetry(Map observation, Map vitalSigns, String timestamp) {
        observation.data.origin.value = timestamp
        observation.data.events[0].time.value = timestamp
        
        def dataItems = observation.data.events[0].data.items
        dataItems.find { it.name.value == "SpOâ‚‚" }.value.numerator = vitalSigns.oxygenSaturation
        dataItems.find { it.name.value == "SpOC" }.value.magnitude = vitalSigns.spoc
        dataItems.find { it.name.value == "SpCO" }.value.numerator = vitalSigns.spco
        dataItems.find { it.name.value == "SpMet" }.value.numerator = vitalSigns.spmet
        
        // Clear random sensor site text
        observation.protocol.items.find { it.name.value == "Sensor site" }.value.value = "Index finger"
    }
    
    void updateRespiration(Map observation, Map vitalSigns, String timestamp) {
        observation.data.origin.value = timestamp
        observation.data.events[0].time.value = timestamp
        
        def dataItems = observation.data.events[0].data.items
        dataItems.find { it.name.value == "Rate" }.value.magnitude = vitalSigns.respiratoryRate
        
        // Update clinical interpretation based on respiratory rate
        def interpretation = getClinicalInterpretationRR(vitalSigns.respiratoryRate)
        dataItems.find { it.name.value == "Clinical interpretation" }.value.value = interpretation
        
        // Clear random text fields
        dataItems.find { it.name.value == "Clinical description" }.value.value = "Generated test data"
        dataItems.find { it.name.value == "Comment" }.value.value = "Generated test data"
    }
    
    String getClinicalInterpretationBP(double systolic, double diastolic) {
        if (systolic > 180 || diastolic > 110) return "Hypertensive crisis"
        if (systolic > 140 || diastolic > 90) return "High blood pressure"
        if (systolic < 90 || diastolic < 60) return "Low blood pressure"
        return "Normal blood pressure"
    }
    
    String getClinicalInterpretationHR(int heartRate) {
        if (heartRate > 100) return "Tachycardia"
        if (heartRate < 60) return "Bradycardia"
        return "Normal heart rate"
    }
    
    String getClinicalInterpretationRR(int respiratoryRate) {
        if (respiratoryRate > 20) return "Tachypnea"
        if (respiratoryRate < 12) return "Bradypnea"
        return "Normal respiratory rate"
    }
    
    // Pseudo-code methods - replace with your actual implementations
    Map loadJsonFromFile(String filename) {
        // Replace with your JSON parser
        println "Loading JSON from: ${filename}"
        // return yourJsonParser.parseFromFile(filename)
        return [:] // Placeholder
    }
    
    Map deepCopyJson(Map original) {
        // Replace with your deep copy implementation
        // return yourJsonUtil.deepCopy(original)
        return [:] // Placeholder
    }
    
    String serializeToJsonString(Map jsonObject) {
        // Replace with your JSON serializer
        // return yourJsonSerializer.toJsonString(jsonObject)
        return "{}" // Placeholder
    }
    
    void saveJsonToFile(String jsonString, String filename) {
        // Replace with your file saving implementation
        println "Saving JSON to: ${filename}"
        // yourFileUtil.writeToFile(jsonString, filename)
    }
}