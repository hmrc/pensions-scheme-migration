List-of-legacy-schemes
-----------------------
This API retrieves the list of legacy schemes.
* **URL**

  `/list-of-schemes`

* **Method**

  `GET`

*  **Request Header**
    
   `psaId`

* **Success Response:**

  * **Code:** 200 

* **Example Success Response**

```json
{
  "totalResults": 4,
  "items": [
    {
      "pstr": "00141768JH",
      "declarationDate": "0001-01-01",
      "racDac": false,
      "schemeName": "Test non-rac/dac scheme for a suspended user",
      "schemeOpenDate": "2006-01-05"
    },
    {
      "pstr": "00515289RH",
      "declarationDate": "2012-02-20",
      "racDac": true,
      "schemeName": "Test rac/dac scheme for a suspended user",
      "schemeOpenDate": "2020-02-01",
      "policyNo": "24101475"
    },
    {
      "pstr": "00241768RH",
      "declarationDate": "0001-01-01",
      "racDac": false,
      "schemeName": "THE AMDAIL PENSION SCHEME - SHARED",
      "schemeOpenDate": "2006-04-05"
    },
    {
      "pstr": "00615269RH",
      "declarationDate": "2012-02-20",
      "racDac": true,
      "schemeName": "vbjTBcNjdtefkhNfOotL",
      "schemeOpenDate": "2020-01-01",
      "policyNo": "24101975"
    }
  ]
}
```

* **Error Response:**

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":400,"message":"The reason will be dependent on the back end service response"}`
  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":400,"message":"Submission has not passed validation. Invalid parameter psaId."}`
  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":400,"message":"Submission has not passed validation. Invalid Header parameter CorrelationId."}`
  OR anything else

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />
