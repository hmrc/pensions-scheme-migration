
# Pensions Scheme Migration

Back-end microservice to support the get legacy list of schemes and get legacy scheme details from TPSS,And Register scheme to ETMP.

API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/register-scheme                                     ```  | POST   | Register legacy scheme to ETMP [More...](docs/register-scheme.md) |
| ```/list-of-schemes                                     ```  | GET    | Retrieves list of legacy scheme details from TPSS [More...](docs/list-of-schemes.md) |
| ```/getLegacySchemeDetails                              ```  | GET    | Returns Legacy Scheme Details [More...](docs/scheme.md) |
| ```/lock                                                ```  | GET    | Returns the migration lock from the Mongo lock cache on basis of pstr,credId and psaId
| ```/lock                                                ```  | POST   | Saves a value as migration lock(pstr,credId and psaId) to a key in the Mongo lock cache
| ```/lock                                                ```  | DELETE | Removes the migration lock(pstr,credId and psaId) of a key from the Mongo lock cache
| ```/migration-data                                      ```  | GET    | Returns the value of a key from the Mongo data cache
| ```/migration-data                                      ```  | POST   | Saves a value to a key in the Mongo data cache
| ```/migration-data                                      ```  | DELETE | Removes the value of a key from the Mongo data cache
| ```/scheme-data                                         ```  | GET    | Returns the value of a key from the Mongo scheme data cache
| ```/scheme-data                                         ```  | POST   | Saves a value to a key in the Mongo scheme data cache
| ```/scheme-data                                         ```  | DELETE | Removes the value of a key from the Mongo scheme data cache
| ```/lock-by-user                                        ```  | GET    | Returns the migration lock(pstr,credId and psaId) from the Mongo lock cache on basis of credId
| ```/lock-by-user                                        ```  | DELETE | Remove the migration lock(pstr,credId and psaId) from the Mongo lock cache on basis of credId
| ```/lock-on-scheme                                      ```  | GET    | Returns the migration lock(pstr,credId and psaId) from the Mongo lock cache on basis of pstr
| ```/lock-on-scheme                                      ```  | DELETE | Remove the migration lock(pstr,credId and psaId) from the Mongo lock cache on basis of pstr
| ```/bulk-migration                                      ```  | POST   | Save a values of rac/dac legacy schemes in racDac work item mongo
| ```/bulk-migration/isRequestInProgress                  ```  | GET    | Returns true or false for psaId.This will check to see if bulk migration request has been made or not.
| ```/bulk-migration/isAllFailed                          ```  | GET    | Returns true or false for psaId.This will check to see if bulk migration request all record is failed or not.
| ```/bulk-migration/deleteAll                            ```  | DELETE | Remove value for psa from the  racDac work item mongo
| ```/email-response/:journeyType/:id                     ```  | POST   | Sends an audit event indicating the response returned by the email service in response to a request to send an email
