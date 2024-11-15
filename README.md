# Pensions Scheme  Migration

## Overview

This is the repository for Pension Scheme Migration Frontend. This service allows a user to migrate pension schemes from TPSS including RAC/DACs. All schemes will need to be migrated from TPSS to the MPS infrastructure by end of 2026. RACs (Retired Annuity Contracts) and DACs (Deferred Annuity Contracts) are two older types of pension scheme. A user declares as an administrator of a RAC/DAC. The administrator is responsible for the migration of schemes.

This service has a corresponding back-end microservice to support the migration of legacy schemes and legacy scheme details from TPSS, and registration of legacy schemes to ETMP.

**Associated Frontend Link:** https://github.com/hmrc/pensions-scheme-migration-frontend

**Stubs:** https://github.com/hmrc/pensions-scheme-stubs



## Requirements
This service is written in Scala and Play, so needs at least a [JRE] to run.

**Node version:** 20.18.0

**Java version:** 19

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


## Service-Specific Documentation [To Do]
Include relevant links or details to any additional, service-specific documents (e.g., stubs, testing protocols) when available. 


## API Endpoints [To Do]

| *Task*                                                      | *Supported Methods* | *Description*                                                                                                        |
|-------------------------------------------------------------|---------------------|----------------------------------------------------------------------------------------------------------------------|
| ```/register-scheme                                     ``` | POST                | Register legacy scheme to ETMP [More...](docs/register-scheme.md)                                                    |
| ```/list-of-schemes                                     ``` | GET                 | Retrieves list of legacy scheme details from TPSS [More...](docs/list-of-schemes.md)                                 |
| ```/getLegacySchemeDetails                              ``` | GET                 | Returns Legacy Scheme Details [More...](docs/scheme.md)                                                              |
| ```/lock                                                ``` | GET                 | Returns the migration lock from the Mongo lock cache on basis of pstr,credId and psaId                               |
| ```/lock                                                ``` | POST                | Saves a value as migration lock(pstr,credId and psaId) to a key in the Mongo lock cache                              |
| ```/lock                                                ``` | DELETE              | Removes the migration lock(pstr,credId and psaId) of a key from the Mongo lock cache                                 |
| ```/migration-data                                      ``` | GET                 | Returns the value of a key from the Mongo data cache                                                                 |
| ```/migration-data                                      ``` | POST                | Saves a value to a key in the Mongo data cache                                                                       |
| ```/migration-data                                      ``` | DELETE              | Removes the value of a key from the Mongo data cache                                                                 |
| ```/scheme-data                                         ``` | GET                 | Returns the value of a key from the Mongo scheme data cache                                                          |
| ```/scheme-data                                         ``` | POST                | Saves a value to a key in the Mongo scheme data cache                                                                |
| ```/scheme-data                                         ``` | DELETE              | Removes the value of a key from the Mongo scheme data cache                                                          |
| ```/lock-by-user                                        ``` | GET                 | Returns the migration lock(pstr,credId and psaId) from the Mongo lock cache on basis of credId                       |
| ```/lock-by-user                                        ``` | DELETE              | Remove the migration lock(pstr,credId and psaId) from the Mongo lock cache on basis of credId                        |
| ```/lock-on-scheme                                      ``` | GET                 | Returns the migration lock(pstr,credId and psaId) from the Mongo lock cache on basis of pstr                         |
| ```/lock-on-scheme                                      ``` | DELETE              | Remove the migration lock(pstr,credId and psaId) from the Mongo lock cache on basis of pstr                          |
| ```/bulk-migration                                      ``` | POST                | Save a values of rac/dac legacy schemes in racDac work item mongo                                                    |
| ```/bulk-migration/isRequestInProgress                  ``` | GET                 | Returns true or false for psaId.This will check to see if bulk migration request has been made or not.               |
| ```/bulk-migration/isAllFailed                          ``` | GET                 | Returns true or false for psaId.This will check to see if bulk migration request all record is failed or not.        |
| ```/bulk-migration/deleteAll                            ``` | DELETE              | Remove value for psa from the  racDac work item mongo                                                                |
| ```/email-response/:journeyType/:id                     ``` | POST                | Sends an audit event indicating the response returned by the email service in response to a request to send an email |

## License
This code is open source software Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.


Back-end microservice to support the migration of legacy schemes and legacy scheme details from TPSS, and registration of legacy schemes to ETMP.
