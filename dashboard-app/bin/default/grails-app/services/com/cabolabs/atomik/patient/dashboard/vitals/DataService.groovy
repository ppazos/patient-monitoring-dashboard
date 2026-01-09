package com.cabolabs.atomik.patient.dashboard.vitals

import grails.gorm.transactions.Transactional

import com.cabolabs.openehr.rest.client.OpenEhrRestClient
import com.cabolabs.openehr.rest.client.ContentTypeEnum
import com.cabolabs.openehr.rest.client.auth.CustomAuth
import com.cabolabs.openehr.rest.client.PreferEnum
import com.cabolabs.openehr.rest.client.QueryResult


@Transactional
class DataService {

   def grailsApplication

   def getVitalsData()
   {
      def client = createConnection()
      QueryResult result = client.executeQuery('bb56d898-0e42-48b5-9321-f53f4fc03000',
         [
            ehr_id: '01cb3295-6a0e-42ab-b744-3248ca6c6213',
            ord:    'desc' // newer first
         ]
      )
      return result
   }

   private def createConnection()
   {
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

      def client = new OpenEhrRestClient(
         getProperty("base_url"),
         auth,
         ContentTypeEnum.JSON,
         PreferEnum.MINIMAL
      )
      client.setCommitterHeader('name="John Doe", external_ref.id="BC8132EA-8F4A-11E7-BB31-BE2E44B06B34", external_ref.namespace="demographic", external_ref.type="PERSON"')

      client.setDescriptionHeader('value="A new description"')

      return client
   }

   private String getProperty(String propertyName)
   {
      return grailsApplication.config.getProperty('atomik.'+ propertyName, String)
   }
}
