/*
 * This app will generate some vital signs data and load an EHR with it.
 * Then another app with a GUI will query the server and render the data in the patient dashboard.
 * We need to have an extra commit on Insomnia to show that if we commit new data, we get it in the dashboard.
 */
package com.cabolabs.atomik.patient.dashboard.vitals

import com.cabolabs.openehr.rest.client.OpenEhrRestClient
import com.cabolabs.openehr.rest.client.ContentTypeEnum
import com.cabolabs.openehr.rest.client.auth.CustomAuth
import com.cabolabs.openehr.rest.client.PreferEnum

import groovy.json.*
import java.time.Instant
import java.util.UUID

import com.cabolabs.openehr.formats.OpenEhrJsonParserQuick
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartySelf
import com.cabolabs.openehr.rm_1_0_2.support.identification.*
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped


class App {

   static def client
   static Properties properties
   static int ehrCount = 100 // number of EHRs to create

   static void main(String[] args)
   {
      // Load sample compo for vital signs monitoring
      def templateJson = loadJsonSampleFromFile("sample_vitals_compo.json")

      // Connect to Atomik
      createConnection()


      // Load vital signs monitoring template on server
      def optVitalSigns = loadContentsFromResources('vital_signs_monitoring.opt')
      client.uploadTemplate(optVitalSigns)

     
      // Load EHR status template for creating the EHR server
       def optEhrStatus = loadContentsFromResources('ehr_status_generic_v1.opt')
      client.uploadTemplate(optEhrStatus)


      for (int i = 0; i < ehrCount; i++)
      {
         println "\n\n=== EHR ${i+1} of ${ehrCount} ==="

         // Create EHR (output the ehr_id)
         def patientId = UUID.randomUUID().toString()
         def ehrStatus = createEhrStatus(patientId)
         client.createEhr(ehrStatus)

         def ehrId
         if (!client.lastResponseHeaders['ETag'])
         {
            println client.lastError
         }
         else
         {
            ehrId = client.lastResponseHeaders['ETag']
            println "EHR Created: "+ ehrId
         }

         // Generate compo combinations with test data and save
         generateJsonData(templateJson)

         // Push all generated compos to EHR
         def generatedFilesFolder = new File('generated_compos')

         def parser = new OpenEhrJsonParserQuick()
         parser.setSchemaFlavorAPI()

         def compoString, compo

         generatedFilesFolder.eachFile { compoFile ->

            compoString = compoFile.text
            compo = parser.parseJson(compoString)

            client.createComposition(ehrId, compo)

            if (!client.lastResponseHeaders['ETag'])
            {
               println "Last error composition: "+ client.lastError
            }
         }
      }

      // (Opt.) Execute query to get values and show
      // TODO
   }


   static String getProperty(String propertyName)
   {
      return System.getenv(propertyName.toUpperCase()) ?: properties[propertyName]
   }


   static void createConnection()
   {
      properties = new Properties()
      // getClass().getClassLoader().getResourceAsStream('application.properties')?.withInputStream {
      //     properties.load(it)
      // }
      App.class.getResource('/application.properties').withInputStream {
         properties.load(it)
      }

      def aapi = getProperty("api_auth_url")
      def user = getProperty("api_username")
      def pass = getProperty("api_password")

      if (!aapi)
      {
         throw new Exception("api_auth_url is not set and it's required when api_auth='custom'")
      }

      if (!user)
      {
         throw new Exception("api_username is not set and it's required when api_auth='custom'")
      }

      if (!pass)
      {
         throw new Exception("api_password is not set and it's required when api_auth='custom'")
      }

      def auth = new CustomAuth(aapi, user, pass)

      client = new OpenEhrRestClient(
         getProperty("base_url"),
         auth,
         ContentTypeEnum.JSON,
         PreferEnum.MINIMAL
      )
      client.setCommitterHeader('name="John Doe", external_ref.id="BC8132EA-8F4A-11E7-BB31-BE2E44B06B34", external_ref.namespace="demographic", external_ref.type="PERSON"')

      client.setDescriptionHeader('value="A new description"')
   }

   static Map loadJsonSampleFromFile(String filename)
   {
      String inputContents = loadContentsFromResources(filename)

      def slurper = new JsonSlurper()
      def map = slurper.parseText(inputContents)
      return map
   }

   static String loadContentsFromResources(String filename)
   {
      String inputContents

      App.class.getResource('/'+ filename).withInputStream {
         inputContents = it.text
      }

      return inputContents
   }

   static List<Map> generateVitalSignsTable()
   {
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

         row.temperature = row.temperature.round(1)

         
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

         row.oxygenSaturation = row.oxygenSaturation.round(1)

         
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
         
         row.spoc  = row.spoc.round(1)
         row.spco  = row.spco.round(1)
         row.spmet = row.spmet.round(1)

         data.add(row)
      }
      
      return data
   }

   static void generateJsonData(Map inputJson)
   {
      def vitalSignsData = generateVitalSignsTable()

      vitalSignsData.eachWithIndex { row, index ->

         // Create a deep copy of the template
         def workingJson = inputJson //deepCopyJson(inputJson)
         
         // Set the vital signs measurements
         setVitalSignsInJson(workingJson, row, index)
         
         // Serialize back to JSON string (pseudo-code - replace with your serializer)
         def jsonString = serializeToJsonString(workingJson)
         
         // Store the new file
         saveJsonToFile(jsonString, "vital_signs_${String.format('%03d', index + 1)}.json")
      }
   }


   static void setVitalSignsInJson(Map json, Map vitalSigns, int index)
   {
      String timestamp = Instant.now().toString()
      String patientId = "patient_${String.format('%03d', index + 1)}"
      
      // Update composer and context
      json.composer.name = "Dr. Smith"
      json.context.start_time.value = timestamp
      
      // Process each observation in the content array
      json.content.each { observation ->

         switch(observation.name.value)
         {
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
    
   static void updateBloodPressure(Map observation, Map vitalSigns, String timestamp) {
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
   
   static void updateBodyTemperature(Map observation, Map vitalSigns, String timestamp) {
      observation.data.origin.value = timestamp
      observation.data.events[0].time.value = timestamp
      
      def dataItems = observation.data.events[0].data.items
      dataItems.find { it.name.value == "Temperature" }.value.magnitude = vitalSigns.temperature
      dataItems.find { it.name.value == "Temperature" }.value.units = "°C"
      
      // Clear the random comment text
      dataItems.find { it.name.value == "Comment" }.value.value = "Generated test data"
   }
   
   static void updatePulseHeartBeat(Map observation, Map vitalSigns, String timestamp) {
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
   
   static void updatePulseOximetry(Map observation, Map vitalSigns, String timestamp) {
      observation.data.origin.value = timestamp
      observation.data.events[0].time.value = timestamp
      
      def dataItems = observation.data.events[0].data.items
      dataItems.find { it.name.value == "SpO₂" }.value.numerator = vitalSigns.oxygenSaturation
      dataItems.find { it.name.value == "SpOC" }.value.magnitude = vitalSigns.spoc
      dataItems.find { it.name.value == "SpCO" }.value.numerator = vitalSigns.spco
      dataItems.find { it.name.value == "SpMet" }.value.numerator = vitalSigns.spmet
      
      // Clear random sensor site text
      observation.protocol.items.find { it.name.value == "Sensor site" }.value.value = "Index finger"
   }
   
   static void updateRespiration(Map observation, Map vitalSigns, String timestamp) {
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
   
   static String getClinicalInterpretationBP(double systolic, double diastolic) {
      if (systolic > 180 || diastolic > 110) return "Hypertensive crisis"
      if (systolic > 140 || diastolic > 90) return "High blood pressure"
      if (systolic < 90 || diastolic < 60) return "Low blood pressure"
      return "Normal blood pressure"
   }
   
   static String getClinicalInterpretationHR(int heartRate) {
      if (heartRate > 100) return "Tachycardia"
      if (heartRate < 60) return "Bradycardia"
      return "Normal heart rate"
   }
   
   static String getClinicalInterpretationRR(int respiratoryRate) {
      if (respiratoryRate > 20) return "Tachypnea"
      if (respiratoryRate < 12) return "Bradypnea"
      return "Normal respiratory rate"
   }

   static String serializeToJsonString(Map json)
   {
      // Serialize map to json string
      def generator = new JsonGenerator.Options()
         .excludeNulls()
         .disableUnicodeEscaping() // avoid unicode escape
         .build()

      // Save file
      //def file = new File('generated_compos/'+ UUID.randomUUID().toString() +'.json')
      //file << generator.toJson(json)

      return generator.toJson(json)
   }

   static void saveJsonToFile(contents, filename)
   {
      def file = new File('generated_compos/'+ filename)
      file.write contents
   }

   static EhrStatus createEhrStatus(String patient_id)
    {
      def hier_patient_id = new HierObjectId(patient_id)
      def ehr_status = new EhrStatus(
         name:              new DvText("Generic Status"),
         archetype_node_id: "openEHR-EHR-EHR_STATUS.generic.v1",
         archetype_details: new Archetyped(
               archetype_id:  new ArchetypeId("openEHR-EHR-EHR_STATUS.generic.v1"),
               template_id:   new TemplateId("ehr_status_generic_en_v1"),
               rm_version:    "1.0.2"
         ),
         is_modifiable: true,
         is_queryable:  true,
         subject:       new PartySelf(hier_patient_id),

      )

      // ehr_status.setTerritory(new CodePhrase("ISO_3166-1", "CL"))
      // ehr_status.setLanguage(new CodePhrase("ISO_639-1", "es"))

      return ehr_status
   }
}
