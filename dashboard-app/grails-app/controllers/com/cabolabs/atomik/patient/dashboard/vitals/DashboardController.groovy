package com.cabolabs.atomik.patient.dashboard.vitals

import com.cabolabs.openehr.rest.client.QueryResult

class DashboardController {

    def dataService

    def index()
    {
        QueryResult result = dataService.getVitalsData()
        [data: result]
    }

    
}
