Register-scheme
-----------------------
Register a scheme.

* **URL**

  `/register-scheme`
* **Method**

  `POST`

*  **Request Header**
    
   `psaId`

* **Example Payload**

```json

{
  "establishers":[
    {
      "establisherDetails":{
        "firstName":"Coffee",
        "lastName":"McCoffee"
      },
      "hasNino":true,
      "address":{
        "country":"NO",
        "postcode":"TF3 5TR",
        "addressLine1":"addressline123",
        "addressLine2":"addressline234",
        "addressLine3":"addressline3",
        "addressLine4":"addressline4"
      },
      "phone":"0042334",
      "establisherKind":"individual",
      "dateOfBirth":"1955-03-29",
      "email":"aye@h.com",
      "nino":{
        "value":"AA999999A"
      },
      "hasUtr":false,
      "noUtrReason":"No UTR",
      "addressYears":true
    }
  ],
  "schemeType":{
    "name":"corp"
  },
  "occupationalPensionScheme":true,
  "schemeName":"Starbucks Coffee",
  "securedBenefits":false,
  "relationshipStartDate":"2014-01-06",
  "currentMembers":"opt3",
  "investmentRegulated":true,
  "schemeOpenDate":"2014-02-06",
  "schemeEstablishedCountry":"SI",
  "racDac":false,
  "expireAt":1639612800000,
  "workingKnowledge":true,
  "futureMembers":"opt2",
  "benefits":"definedBenefitsOnly",
  "anyTrustees":false,
  "pstr":"21000001AB"
}

```

* **Success Response:**

  * **Code:** 200 <br />

* **Example Success Response**

```json
{
   "processingDate": "2021-11-18",
   "schemeReferenceNumber":"S0123456789"
}
```

* **Error Response:**

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":INVALID_PSAID,"message":"Submission has not passed validation. Invalid parameter psaId."}`

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":INVALID_PAYLOAD,"message":"Submission has not passed validation. Invalid payload."}`
    
  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":INVALID_CORRELATIONID,"message":"Submission has not passed validation. Invalid Header parameter CorrelationId."}`
    
  OR anything else

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />
