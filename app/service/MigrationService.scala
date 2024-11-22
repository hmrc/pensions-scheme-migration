/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package service

import com.google.inject.Inject
import controllers.cache.CredIdNotFoundFromAuth
import crypto.{DataEncryptor, EncryptedValue, SecureGCMCipher}
import models.cache.{DataJson, MigrationLock}
import models.racDac.{RacDacHeaders, WorkItemRequest}
import org.mongodb.scala.MongoCollection
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsString, JsValue, Json}
import play.api.{Configuration, Logging}
import repositories.{DataCacheRepository, ListOfLegacySchemesCacheRepository, LockCacheRepository, RacDacRequestsQueueEventsLogRepository, RacDacRequestsQueueRepository, SchemeDataCacheRepository}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Success, Try}

//noinspection ScalaStyle
class MigrationService @Inject()(mongoLockRepository: MongoLockRepository,
                                 listOfLegacySchemesCacheRepository: ListOfLegacySchemesCacheRepository,
                                 migrationDataCacheRepository: DataCacheRepository,
                                 racDacRequestsQueueRepository: RacDacRequestsQueueRepository,
                                 schemeDataCacheRepository: SchemeDataCacheRepository,
                                 cipher: SecureGCMCipher,
                                 configuration: Configuration,
                                 dataEncryptor: DataEncryptor,
                                 val authConnector: AuthConnector)(implicit ec: ExecutionContext) extends Logging with AuthorisedFunctions {
  private val lock = LockService(mongoLockRepository, "pensions_scheme_migration_mongodb_migration_lock", Duration(10, TimeUnit.MINUTES))
  // TODO - look into encryption keys and add to config - https://confluence.tools.tax.service.gov.uk/display/PBD/Self-Service+of+Secrets
  private val encryptionKey  = configuration.get[String]("mongodb.migration.encryptionKey")

  // list of legacy schemes id is the psaId
  private def encryptCollection(collection: MongoCollection[JsObject], collectionName: String, idAndDataToSave: (String, JsValue) => Future[Boolean]) ={
    collection.find().toFuture().map(seqJsValue => {
//      println(s"Existing scheme data collections: ${seqJsValue}")

      val newEncryptedValues = seqJsValue.flatMap { jsValue =>
        val data = jsValue \ "data"
        val alreadyEncrypted = data.validate[EncryptedValue].fold(_ => false, _ => true)
        if (alreadyEncrypted) {
          None
        } else {
          val encryptedData = Json.toJson(cipher.encrypt(data.as[JsValue].toString(), (jsValue \ "id").as[String], encryptionKey))
          val encryptedJsValue = (jsValue.as[JsObject] - "data") + ("data" -> encryptedData)
          Some(encryptedJsValue)
        }
      }

      val numberOfNewEncryptedValues = newEncryptedValues.length

      logger.warn(s"[PODS-9953] Number of documents encrypted for $collectionName: $numberOfNewEncryptedValues")

      val successfulInserts = newEncryptedValues.map { jsValue =>
        val id = (jsValue \ "id").as[String]
        val data = (jsValue \ "data").as[JsValue]
        Await.result(idAndDataToSave(id, data), 5.seconds)
      }.count(_ == true)

      logger.warn(s"[PODS-9953] Number of documents upserted for $collectionName: $successfulInserts")
      numberOfNewEncryptedValues -> successfulInserts
    })
  }

//  private def encryptListOfLegacySchemesCollection() ={
//    val collection = listOfLegacySchemesCacheRepository.collection
//    collection.find().toFuture().map(seqJsValue => {
//      println(s"Existing list of legacy collections: ${seqJsValue}")
//      val newEncryptedValues = seqJsValue.flatMap { jsValue =>
//        val totalResults = jsValue \ "data" \ "totalResults"
//        val items = jsValue \ "data" \ "items"
//        val totalResultsAlreadyEncrypted = totalResults.validate[EncryptedValue].fold(_ => false, _ => true)
//        val itemsAlreadyEncrypted = items.validate[EncryptedValue].fold(_ => false, _ => true)
//
//        if (totalResultsAlreadyEncrypted && itemsAlreadyEncrypted) {
//          None
//        } else {
//          // TODO - If issues, may be problem with using "id" associated text
//          val encryptedTotalResults = Json.toJson(cipher.encrypt(totalResults.as[JsNumber].toString(), (jsValue \ "id").as[String], encryptionKey))
//          val encryptedItems = Json.toJson(cipher.encrypt(items.as[List[JsString]].toString(), (jsValue \ "id").as[String], encryptionKey))
//          val dataObj = Json.obj(
//            "totalResults" -> encryptedTotalResults,
//            "items" -> encryptedItems
//          )
//          val encryptedJsValue = (jsValue.as[JsObject] - "data") + ("data" -> dataObj)
//          Some(encryptedJsValue)
//        }
//      }
//
//      val numberOfNewEncryptedValues = newEncryptedValues.length
//
//      logger.warn(s"[PODS-9953] Number of documents encrypted for List Of Legacy Schemes Cache: $numberOfNewEncryptedValues")
//
//      val successfulInserts = newEncryptedValues.map { jsValue =>
//        val id = (jsValue \ "id").as[String]
//        val data = (jsValue \ "data").as[JsValue]
//        Await.result(listOfLegacySchemesCacheRepository.upsert(id, data), 5.seconds)
//      }.count(_ == true)
//
//      logger.warn(s"[PODS-9953] Number of documents upserted for List Of Legacy Schemes Cache: $successfulInserts")
//      numberOfNewEncryptedValues -> successfulInserts
//    })
//  }

//  private def encryptMigrationDataCollection() = {
//    val collection = migrationDataCacheRepository.collection
//    collection.find().toFuture().map(seqDataJson => {
//
//      val newEncryptedValues = seqDataJson.flatMap { dataJson =>
//        val data = dataJson.data
//        val dataValue = data \ "data"
//        val alreadyEncrypted = data.validate[EncryptedValue].fold(_ => false, _ => true)
//        if (alreadyEncrypted) {
//          None
//        } else {
//          val encryptedData = Json.toJson(cipher.encrypt(dataValue.as[JsValue].toString(), (data \ "id").as[String], encryptionKey))
//          val encryptedJsValue = (data.as[JsObject] - "data") + ("data" -> encryptedData) + ("pstr" -> JsString(dataJson.pstr))
//          Some(encryptedJsValue)
//        }
//      }
//
//      val numberOfNewEncryptedValues = newEncryptedValues.length
//
//      logger.warn(s"[PODS-9953] Number of documents encrypted for Migration Data Cache: $numberOfNewEncryptedValues")
//
//      val successfulInserts = newEncryptedValues.map { jsValue =>
//        // TODO - is this just the mongo document ID?
//        val id = (jsValue \ "id").as[String]
//        val data = (jsValue \ "data").as[JsValue]
//        val pstr = (jsValue \ "pstr").as[String]
//        authorised().retrieve(Retrievals.externalId) {
//          // TODO - work out how to retrieve real psaId
//          case Some(credId) => Future.successful(MigrationLock(pstr, credId, "psaId"))
//          case _ => Future.failed(CredIdNotFoundFromAuth())
//        }.map { migrationLock =>
//          Await.result(migrationDataCacheRepository.renewLockAndSave(migrationLock, data), 5.seconds)
//        }
//
//      }.map(futureInsert => futureInsert.filter(insert => insert)).length
//
//      logger.warn(s"[PODS-9953] Number of documents upserted for Migration Data Cache: $successfulInserts")
//      numberOfNewEncryptedValues -> successfulInserts
//    })
//  }

//  private def encryptRacDacRequestQueueCollection() = {
//    val collection = racDacRequestsQueueRepository.collection
//    collection.find().toFuture().map(seqWorkItem => {
//      val newEncryptedValues = seqWorkItem.flatMap { workItem =>
//        val workItemRequest = workItem.item.request
//        val requestData = workItemRequest \ "request"
//        val alreadyEncrypted = requestData.validate[EncryptedValue].fold(_ => false, _ => true)
//        if (alreadyEncrypted) {
//          None
//        } else {
//          val encryptedData = Json.toJson(cipher.encrypt(requestData.as[JsValue].toString(), (workItemRequest \ "id").as[String], encryptionKey))
//          val psaId = workItem.item.psaId
//          val headers = Json.obj(
//            "requestId" -> workItem.item.headers.requestId,
//            "sessionId" -> workItem.item.headers.sessionId
//          )
//          val encryptedJsValue = (workItemRequest.as[JsObject] - "data") +
//            ("psaId" -> JsString(psaId)) +
//            ("headers" -> headers) +
//            ("request" -> encryptedData)
//          Some(encryptedJsValue)
//        }
//      }
//
//      val numberOfNewEncryptedValues = newEncryptedValues.length
//
//      logger.warn(s"[PODS-9953] Number of documents encrypted for RacDac Request Queue: $numberOfNewEncryptedValues")
//
//      val successfulInserts = newEncryptedValues.map { jsValue =>
//        val psaId = jsValue \ "psaId"
//        val requestId = jsValue \ "requestId"
//        val sessionId = jsValue \ "sessionId"
//        val request = jsValue \ "request"
//        val requestIdValue = if(requestId.as[String].isEmpty) {
//          None
//        } else {
//          Some(requestId.as[String])
//        }
//        val sessionIdValue = if (sessionId.as[String].isEmpty) {
//          None
//        } else {
//          Some(sessionId.as[String])
//        }
//        val racDacHeaders = RacDacHeaders(requestIdValue, sessionIdValue)
//        WorkItemRequest(psaId.as[String], request.as[JsValue], racDacHeaders)
////        val decryptedWorkItems = seqWorkItem.map(encryptedWorkItem =>
////          encryptedWorkItem.item.decrypt(dataEncryptor)
////        )
//        Await.result(racDacRequestsQueueRepository.pushAll(decryptedWorkItems), 5.seconds)
//      }.count(_ == true)
//    })
//  }

  private def encryptCollections() = {
    logger.warn("[PODS-9953] Started encrypting collection")

    Future.sequence(Seq(
//      encryptListOfLegacySchemesCollection(),
      encryptCollection(schemeDataCacheRepository.collection, "Scheme Data Cache", schemeDataCacheRepository.save),
      encryptCollection(listOfLegacySchemesCacheRepository.collection, "List Of Legacy Schemes", listOfLegacySchemesCacheRepository.upsert)
//      encryptMigrationDataCollection()
    ))
  }

  private def decryptCollection(collection: MongoCollection[JsObject], collectionName: String, idAndDataToSave: (String, JsValue) => Future[Boolean]) = {
      collection.find().toFuture().map(seqJsValue => {
        val newDecryptedValues = seqJsValue.flatMap { jsValue =>
          val data = jsValue \ "data"
          val valuesAreEncrypted = data.validate[EncryptedValue].fold(_ => false, _ => true)
          if(valuesAreEncrypted) {
            val decryptedData = Json.parse(cipher.decrypt(data.as[EncryptedValue], (jsValue \ "id").as[String], encryptionKey))
            val decryptedJsValue = (jsValue.as[JsObject] - "data") + ("data" -> decryptedData)
//            println(s"Decrypted ${collectionName} value is: ${decryptedJsValue}")
            Some(decryptedJsValue)
          } else {
            None
          }
        }

        val numberOfNewDecryptedValues = newDecryptedValues.length

        logger.warn(s"[PODS-9953] Number of documents decrypted for $collectionName: $numberOfNewDecryptedValues")

        val successfulInserts = newDecryptedValues.map { jsValue =>
          val id = (jsValue \ "id").as[String]
          val data = (jsValue \ "data").as[JsValue]

          Try(Await.result(idAndDataToSave(id, data), 5.seconds)) match {
            case Failure(exception) =>
              logger.error(s"[PODS-9953] upsert failed", exception)
              false
            case Success(_) =>
              true
          }
        }.count(_ == true)

        logger.warn(s"[PODS-9953] Number of documents upserted for $collectionName: $successfulInserts")

        numberOfNewDecryptedValues -> successfulInserts
      })
  }

//  private def decryptListOfLegacySchemesCollection() = {
//    val collection = listOfLegacySchemesCacheRepository.collection
//    collection.find().toFuture().map(seqJsValue =>
//    )
//  }

  private def decryptCollections() = {
    logger.warn("[PODS-9953] Started decrypting collection")

    Future.sequence(Seq(
      decryptCollection(schemeDataCacheRepository.collection, "Scheme Data Cache", schemeDataCacheRepository.save),
      decryptCollection(listOfLegacySchemesCacheRepository.collection, "List Of Legacy Schemes", listOfLegacySchemesCacheRepository.upsert)
    ))
  }

  lock withLock {
    for {
      res <- if(configuration.get[Boolean]("mongodb.migration.encrypt")) encryptCollections() else decryptCollections()
    } yield res
  } map {
    case Some(seq) =>
      logger.warn(s"[PODS-9953] collection modified successfully. Total documents: ${seq.map(_._1).sum}. Documents updated: ${seq.map(_._2).sum}")
    case None => logger.warn(s"[PODS-9953] locked by other instance")
  } recover {
    case e => logger.error("Locking finished with error", e)
  }

}

