Scheme
-----------------------
This API retrieves the details of a legacy scheme.

* **URL**

  `/getLegacySchemeDetails`

* **Method**

  `GET`

*  **Request Header**
    
   `psaId`
   
   `pstr`

* **Success Response:**

  * **Code:** 200 <br />

* **Example Success Response**

```json
{
  "establishers": [
    {
      "establisherDetails": {
        "firstName": "User",
        "lastName": "One One"
      },
      "hasNino": true,
      "address": {
        "country": "NO",
        "postcode": "TF3 5TR",
        "addressLine1": "addressline123",
        "addressLine2": "addressline234",
        "addressLine3": "addressline3",
        "addressLine4": "addressline4"
      },
      "phone": "0042334",
      "establisherKind": "individual",
      "dateOfBirth": "1955-03-29",
      "email": "aye@h.com",
      "nino": {
        "value": "AA999999A"
      }
    },
    {
      "establisherDetails": {
        "firstName": "Second",
        "lastName": "User"
      },
      "hasNino": false,
      "noNinoReason": "Test",
      "address": {
        "country": "NO",
        "postcode": "",
        "addressLine1": "addressline1",
        "addressLine2": "addressline2",
        "addressLine3": "",
        "addressLine4": ""
      },
      "phone": "00442335",
      "establisherKind": "individual",
      "dateOfBirth": "1955-03-29",
      "email": "bbb@gmail.com"
    },
    {
      "havePaye": false,
      "haveCompanyNumber": false,
      "address": {
        "postcode": "",
        "addressLine1": "sdfdsf",
        "addressLine2": "sdfdsf",
        "addressLine3": "",
        "addressLine4": ""
      },
      "companyDetails": {
        "companyName": "dsfdsf"
      },
      "phone": "23234",
      "establisherKind": "company",
      "noCompanyNumberReason": "fsdfdsf",
      "haveVat": false,
      "email": "sdf@sdf"
    },
    {
      "havePaye": true,
      "haveCompanyNumber": true,
      "address": {
        "country": "LI",
        "postcode": "",
        "addressLine1": "sdfdsfdsf",
        "addressLine2": "sdfdsfdsf",
        "addressLine3": "",
        "addressLine4": ""
      },
      "companyDetails": {
        "companyName": "sfdsfdsfsdf"
      },
      "companyNumber": {
        "value": "123458"
      },
      "establisherKind": "company",
      "vat": {
        "value": "123456789"
      },
      "paye": {
        "value": "123AB456"
      },
      "haveVat": true,
      "email": "sdfds@dfdsf"
    }
  ],
  "schemeType": {
    "name": "other"
  },
  "occupationalPensionScheme": true,
  "schemeName": "Legacy Scheme Filled",
  "securedBenefits": false,
  "relationshipStartDate": "2017-04-06",
  "currentMembers": "opt2",
  "investmentRegulated": true,
  "schemeOpenDate": "2017-04-06",
  "trustees": [
    {
      "hasNino": true,
      "address": {
        "country": "NO",
        "postcode": "TF3 8TR",
        "addressLine1": "addressline1",
        "addressLine2": "addressline2",
        "addressLine3": "addressline3",
        "addressLine4": "addressline4"
      },
      "phone": "0142334",
      "dateOfBirth": "1955-03-29",
      "trusteeKind": "individual",
      "trusteeDetails": {
        "firstName": "Third",
        "lastName": "User"
      },
      "nino": {
        "value": "AA999999B"
      }
    },
    {
      "hasNino": false,
      "noNinoReason": "Test",
      "address": {
        "country": "GB",
        "postcode": "",
        "addressLine1": "addressline1",
        "addressLine2": "addressline2",
        "addressLine3": "",
        "addressLine4": ""
      },
      "phone": "00442735",
      "dateOfBirth": "1955-03-30",
      "trusteeKind": "individual",
      "trusteeDetails": {
        "firstName": "Fourth",
        "lastName": "User"
      },
      "email": "ddb@gmail.com"
    },
    {
      "havePaye": true,
      "haveCompanyNumber": false,
      "address": {
        "country": "NL",
        "postcode": "",
        "addressLine1": "asdasd",
        "addressLine2": "asdsada",
        "addressLine3": "",
        "addressLine4": ""
      },
      "companyDetails": {
        "companyName": "asdsad"
      },
      "vat": {
        "value": "222456289"
      },
      "paye": {
        "value": "123AB456"
      },
      "noCompanyNumberReason": "dsfdsfsdf",
      "haveVat": true,
      "trusteeKind": "company",
      "email": "asdasd@dfdsf"
    }
  ],
  "schemeEstablishedCountry": "SI",
  "racDac": false
}
```

* **Error Response:**

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":400,"message":"Bad Request with missing parameters idType, idNumber or PSAId"}`
  
  * **Code:** 404 NOT_FOUND <br />

  OR anything else

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />
