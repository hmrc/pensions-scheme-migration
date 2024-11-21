# Pensions Scheme Migration

- [Overview](#overview)
- [Requirements](#requirements)
- [Running the Service](#running-the-service)
- [Enrolments](#enrolments)
- [Compile & Test](#compile--test)
- [Navigation and Dependent Services](#navigation-and-dependent-services)
- [Service Documentation](#service-documentation)
- [Endpoints](#endpoints)
- [License](#license)

## Overview

This is the repository for Pension Scheme Migration Frontend. This service allows a user to migrate pension schemes from TPSS including RAC/DACs. All schemes will need to be migrated from TPSS to the MPS infrastructure by end of 2026. RACs (Retired Annuity Contracts) and DACs (Deferred Annuity Contracts) are two older types of pension scheme. A user declares as an administrator of a RAC/DAC. The administrator is responsible for the migration of schemes.

This service has a corresponding back-end microservice to support the migration of legacy schemes and legacy scheme details from TPSS, and registration of legacy schemes to ETMP.

**Associated Frontend Link:** https://github.com/hmrc/pensions-scheme-migration-frontend

**Stubs:** https://github.com/hmrc/pensions-scheme-stubs



## Requirements
This service is written in Scala and Play, so needs at least a [JRE] to run.

**Node version:** 16.20.2

**Java version:** 11

**Scala version:** 2.13.14


## Running the Service
**Service Manager Profile:** PODS_ALL

**Port:** 8214

**Links:** http://localhost:8213/add-pension-scheme/list-pension-schemes 

http://localhost:8213/add-pension-scheme/rac-dac/add-all 

In order to run the service, ensure Service Manager is installed (see [MDTP guidance](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-service-manager.html) if needed) and launch the relevant configuration by typing into the terminal:
`sm2 --start PODS_ALL`

To run the service locally, enter `sm2 --stop PENSIONS_SCHEME_MIGRATION`.

In your terminal, navigate to the relevant directory and enter `sbt run`.

Access the Authority Wizard and login with the relevant enrolment details [here](http://localhost:9949/auth-login-stub/gg-sign-in)


## Enrolments
There are several different options for enrolling through the auth login stub. In order to enrol as a dummy user to access the platform for local development and testing purposes, the following details must be entered on the auth login page.


For access to the **Pension Administrator dashboard** for local development, enter the following information: 

**Redirect url -** http://localhost:8204/manage-pension-schemes/overview 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key -** HMRC-PODS-ORG 

**Identifier Name -** PsaID 

**Identifier Value -** A2100005

---

In order to access the **Pension Practitioner dashboard** for local development, enter the following information: 

**Redirect URL -** http://localhost:8204/manage-pension-schemes/dashboard 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key -** HMRC-PODSPP-ORG 

**Identifier Name -** PspID 

**Identifier Value -** 21000005

---



**Dual enrolment** as both a Pension Administrator and Practitioner is also possible and can be accessed by entering:

**Redirect url -** http://localhost:8204/manage-pension-schemes/overview 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key 1 -** HMRC-PODSPP-ORG Identifier 

**Name 1 -** PspID Identifier 

**Value 1 -** 21000005

**Enrolment Key 2 -** HMRC-PODS-ORG 

**Identifier Name 2 -** PsaID 

**Identifier Value 2 -** A2100005

---

To access the **Scheme Registration journey**, enter the following information:

**Redirect URL -** http://localhost:8204/manage-pension-schemes/you-need-to-register 

**GNAP Token -** NO 

**Affinity Group -** Organisation

---


## Compile & Test
**To compile:** Run `sbt compile`

**To test:** Use `sbt test`

**To view test results with coverage:** Run `sbt clean coverage test coverageReport`

For further information on the PODS Test Approach and wider testing including acceptance, accessibility, performance, security and E2E testing, visit the PODS Confluence page [here](https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=PODSP&title=PODS+Test+Approach).

For Journey Tests, visit the [Journey Test Repository](| Journey tests(https://github.com/hmrc/pods-journey-tests).

View the prototype [here](https://pods-event-reporting-prototype.herokuapp.com/).


## Navigation and Dependent Services
The Pension Migration Frontend integrates with the Manage Pension Schemes (MPS) service and uses various stubs available on [GitHub](https://github.com/hmrc/pensions-scheme-stubs). From the Authority Wizard page you will be redirected to the dashboard. Navigate to the migration tile and select 'Add pension schemes registered on the Pension Schemes Online service' to add schemes or 'Add RAC/DACs registered on the Pension Schemes Online service' to add RAC/DACs.


There are numerous APIs implemented throughout the MPS architecture, and the relevant endpoints are illustrated below. For an overview of all PODS APIs, refer to the [PODS API Documentation](https://confluence.tools.tax.service.gov.uk/display/PODSP/PODS+API+Latest+Version).


## Service Documentation
[To Do]
Include relevant links or details to any additional, service-specific documents (e.g., stubs, testing protocols) when available. 

## Note on terminology
The terms scheme reference number and submission reference number (SRN) are interchangeable within the PODS codebase; some downstream APIs use scheme reference number, some use submission reference number, probably because of oversight on part of the technical teams who developed these APIs. This detail means the same thing, the reference number that was returned from ETMP when the scheme details were submitted.

## Endpoints
[To Do]

**Standard Path**
```POST   /register-scheme```

**Description**
Register legacy scheme to ETMP [More...](docs/register-scheme.md)

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

**Standard Path**


**Description**


| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```GET   /list-of-schemes```

**Description**  
Retrieves a list of legacy scheme details from TPSS [More...](docs/list-of-schemes.md)

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```GET   /getLegacySchemeDetails```

**Description**  
Returns Legacy Scheme Details [More...](docs/scheme.md)

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```GET   /lock```

**Description**  
Returns the migration lock from the Mongo lock cache on the basis of pstr, credId, and psaId

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```POST   /lock```

**Description**  
Saves a value as a migration lock (pstr, credId, and psaId) to a key in the Mongo lock cache

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```DELETE   /lock```

**Description**  
Removes the migration lock (pstr, credId, and psaId) of a key from the Mongo lock cache

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```GET   /migration-data```

**Description**  
Returns the value of a key from the Mongo data cache

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```POST   /migration-data```

**Description**  
Saves a value to a key in the Mongo data cache

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```DELETE   /migration-data```

**Description**  
Removes the value of a key from the Mongo data cache

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```GET   /scheme-data```

**Description**  
Returns the value of a key from the Mongo scheme data cache

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```POST   /scheme-data```

**Description**  
Saves a value to a key in the Mongo scheme data cache

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```DELETE   /scheme-data```

**Description**  
Removes the value of a key from the Mongo scheme data cache

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```GET   /lock-by-user```

**Description**  
Returns the migration lock (pstr, credId, and psaId) from the Mongo lock cache on the basis of credId

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```DELETE   /lock-by-user```

**Description**  
Removes the migration lock (pstr, credId, and psaId) from the Mongo lock cache on the basis of credId

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```GET   /lock-on-scheme```

**Description**  
Returns the migration lock (pstr, credId, and psaId) from the Mongo lock cache on the basis of pstr

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```DELETE   /lock-on-scheme```

**Description**  
Removes the migration lock (pstr, credId, and psaId) from the Mongo lock cache on the basis of pstr

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```POST   /bulk-migration```

**Description**  
Saves values of rac/dac legacy schemes in racDac work item mongo

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```GET   /bulk-migration/isRequestInProgress```

**Description**  
Returns true or false for psaId. This checks if a bulk migration request has been made.

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```GET   /bulk-migration/isAllFailed```

**Description**  
Returns true or false for psaId. This checks if all records in a bulk migration request failed.

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```DELETE   /bulk-migration/deleteAll```

**Description**  
Removes value for psaId from the racDac work item mongo

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**  
```POST   /email-response/:journeyType/:id```

**Description**  
Sends an audit event indicating the response returned by the email service in response to a request to send an email

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

## License
This code is open source software Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.


Back-end microservice to support the migration of legacy schemes and legacy scheme details from TPSS, and registration of legacy schemes to ETMP.

[↥ Back to Top](#pensions-scheme-migration)
