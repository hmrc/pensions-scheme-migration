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
import crypto.{EncryptedValue, SecureGCMCipher}
import models.racDac.EncryptedWorkItemRequest
import org.mongodb.scala.MongoCollection
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.{Configuration, Logging}
import repositories.{DataCacheRepository, ListOfLegacySchemesCacheRepository, RacDacRequestsQueueRepository, SchemeDataCacheRepository}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Success, Try}

//noinspection ScalaStyle
class MigrationService @Inject()(mongoLockRepository: MongoLockRepository,
                                 listOfLegacySchemesCacheRepository: ListOfLegacySchemesCacheRepository,
                                 migrationDataCacheRepository: DataCacheRepository,
                                 racDacWorkItemRepository: RacDacRequestsQueueRepository,
                                 schemeDataCacheRepository: SchemeDataCacheRepository,
                                 cipher: SecureGCMCipher,
                                 configuration: Configuration,
                                 val authConnector: AuthConnector)(implicit ec: ExecutionContext) extends Logging with AuthorisedFunctions {
  private val lock = LockService(mongoLockRepository, "pensions_scheme_migration_mongodb_migration_lock", Duration(10, TimeUnit.MINUTES))
  private val encryptionKey = configuration.get[String]("mongodb.migration.encryptionKey")

  private def encryptCollection(collection: MongoCollection[JsObject], collectionName: String, idAndDataToSave: (String, JsValue) => Future[Boolean]) = {
    collection.find().toFuture().map(seqJsValue => {
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

  private def encryptMigrationDataCollection() = {
    val collection = migrationDataCacheRepository.collection
    collection.find().toFuture().map(seqDataJson => {
      val newEncryptedValues = seqDataJson.flatMap { dataJson =>
        val data = dataJson.data
        val alreadyEncrypted = data.validate[EncryptedValue].fold(_ => false, _ => true)
        if (alreadyEncrypted) {
          None
        } else {
          val encryptedData = Json.toJson(cipher.encrypt(data.toString(), dataJson.pstr, encryptionKey))
          val encryptedJsValue = (data.as[JsObject] - "data") + ("data" -> encryptedData) + ("pstr" -> JsString(dataJson.pstr))
          Some(encryptedJsValue)
        }
      }

      val numberOfNewEncryptedValues = newEncryptedValues.length

      logger.warn(s"[PODS-9953] Number of documents encrypted for Migration Data Cache: $numberOfNewEncryptedValues")

      val successfulInserts = newEncryptedValues.map { jsValue =>
        val data = (jsValue \ "data").as[JsValue]
        val pstr = (jsValue \ "pstr").as[String]
        Await.result(migrationDataCacheRepository.saveMigratedData(pstr, data), 5.seconds)
      }.count(_ == true)

      logger.warn(s"[PODS-9953] Number of documents upserted for Migration Data Cache: $successfulInserts")
      numberOfNewEncryptedValues -> successfulInserts
    })
  }

  private def encryptRacDacWorkItemCollection() = {
    val collection = racDacWorkItemRepository.collection
    collection.find().toFuture().map { seqWorkItem =>
      val newEncryptedValues = seqWorkItem.flatMap { workItem =>
        val item: EncryptedWorkItemRequest = workItem.item
        val request = item.request

        val alreadyEncrypted = request.validate[EncryptedValue].fold(_ => false, _ => true)
        if (alreadyEncrypted) {
          None
        } else {
          val encryptedData = Json.toJson(cipher.encrypt(request.toString(), item.psaId, encryptionKey))
          val encryptedJsValue = (request.as[JsObject] - "request") + ("request" -> encryptedData) + ("psaId" -> JsString(item.psaId))
          Some(encryptedJsValue)
        }
      }

      val numberOfNewEncryptedValues = newEncryptedValues.length

      logger.warn(s"[PODS-9953] Number of documents encrypted for RacDac Work Item: $numberOfNewEncryptedValues")

      val successfulInserts = newEncryptedValues.map { jsValue =>
        val request = (jsValue \ "request").as[JsValue]
        val psaId = (jsValue \ "psaId").as[String]
        Await.result(racDacWorkItemRepository.saveMigratedData(psaId, request), 5.seconds)
      }.count(_ == true)

      logger.warn(s"[PODS-9953] Number of documents upserted for RacDac Work Item: $successfulInserts")
      numberOfNewEncryptedValues -> successfulInserts
    }
  }

  private def encryptCollections() = {
    logger.warn("[PODS-9953] Started encrypting collection")

    Future.sequence(Seq(
      encryptCollection(schemeDataCacheRepository.collection, "Scheme Data Cache", schemeDataCacheRepository.save),
      encryptCollection(listOfLegacySchemesCacheRepository.collection, "List Of Legacy Schemes", listOfLegacySchemesCacheRepository.upsert),
      encryptMigrationDataCollection(),
      encryptRacDacWorkItemCollection()
    ))
  }

  private def decryptCollection(collection: MongoCollection[JsObject], collectionName: String, idAndDataToSave: (String, JsValue) => Future[Boolean]) = {
      collection.find().toFuture().map(seqJsValue => {
        val newDecryptedValues = seqJsValue.flatMap { jsValue =>
          val data = jsValue \ "data"
          val valuesAreEncrypted = data.validate[EncryptedValue].fold(_ => false, _ => true)
          if (valuesAreEncrypted) {
            val decryptedData = Json.parse(cipher.decrypt(data.as[EncryptedValue], (jsValue \ "id").as[String], encryptionKey))
            val decryptedJsValue = (jsValue.as[JsObject] - "data") + ("data" -> decryptedData)
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
              logger.error(s"[PODS-9953] $collectionName upsert failed", exception)
              false
            case Success(_) =>
              true
          }
        }.count(_ == true)

        logger.warn(s"[PODS-9953] Number of documents upserted for $collectionName: $successfulInserts")

        numberOfNewDecryptedValues -> successfulInserts
      })
  }

  private def decryptMigrationDataCollection() = {
    val collection = migrationDataCacheRepository.collection
    collection.find().toFuture().map(seqDataJson => {
      val newDecryptedValues = seqDataJson.flatMap(dataJson => {
        val data = dataJson.data
        val dataIsEncrypted = data.validate[EncryptedValue].fold(_ => false, _ => true)
        if (dataIsEncrypted) {
          val decryptedData = cipher.decrypt(data.as[EncryptedValue], dataJson.pstr, encryptionKey)
          val parsedDecryptedData = Json.parse(decryptedData)
          val decryptedJsValue = (Json.toJson(dataJson).as[JsObject] - "data") + ("data" -> parsedDecryptedData) + ("pstr" -> JsString(dataJson.pstr))
          Some(decryptedJsValue)
        } else {
          None
        }
      })

      val numberOfNewDecryptedValues = newDecryptedValues.length

      logger.warn(s"[PODS-9953] Number of documents decrypted for Migration Data Cache: $numberOfNewDecryptedValues")

      val successfulInserts = newDecryptedValues.map { jsValue =>
        val pstr = (jsValue \ "pstr").as[String]
        val data = (jsValue \ "data").as[JsValue]

        Try(Await.result(migrationDataCacheRepository.saveMigratedData(pstr, data), 5.seconds)) match {
          case Failure(exception) =>
            logger.error(s"[PODS-9953] Migration Data Cache upsert failed", exception)
            false
          case Success(_) =>
            true
        }
      }.count(_ == true)

      logger.warn(s"[PODS-9953] Number of documents upserted for Migration Data Cache: $successfulInserts")

      numberOfNewDecryptedValues -> successfulInserts
    })
  }

  private def decryptRacDacWorkItemCollection() = {
    val collection = racDacWorkItemRepository.collection
    collection.find().toFuture().map(seqWorkItem => {
      val newDecryptedValues = seqWorkItem.flatMap { workItem =>
        val item = workItem.item
        val request = item.request
        val requestIsEncrypted = request.validate[EncryptedValue].fold(_ => false, _ => true)
        if (requestIsEncrypted) {
          val decryptedRequest = Json.parse(cipher.decrypt(request.as[EncryptedValue], item.psaId, encryptionKey))
          val decryptedJsValue = (Json.toJson(item).as[JsObject] - "request") + ("request" -> decryptedRequest) + ("psaId" -> JsString(item.psaId))
          Some(decryptedJsValue)
        } else {
          None
        }
      }

      val numberOfNewDecryptedValues = newDecryptedValues.length

      logger.warn(s"[PODS-9953] Number of documents decrypted for RacDac Work Item: $numberOfNewDecryptedValues")

      val successfulInserts = newDecryptedValues.map { jsValue =>
        val psaId = (jsValue \ "psaId").as[String]
        val request = (jsValue \ "request").as[JsValue]

        Try(Await.result(racDacWorkItemRepository.saveMigratedData(psaId, request), 5.seconds)) match {
          case Failure(exception) =>
            logger.error(s"[PODS-9953] RacDac Work Item upsert failed", exception)
            false
          case Success(_) =>
        }
      }.count(_ == true)

      logger.warn(s"[PODS-9953] Number of documents upserted for RacDac Work Item: $successfulInserts")

      numberOfNewDecryptedValues -> successfulInserts
    })

  }

  private def decryptCollections() = {
    logger.warn("[PODS-9953] Started decrypting collection")

    Future.sequence(Seq(
      decryptCollection(schemeDataCacheRepository.collection, "Scheme Data Cache", schemeDataCacheRepository.save),
      decryptCollection(listOfLegacySchemesCacheRepository.collection, "List Of Legacy Schemes", listOfLegacySchemesCacheRepository.upsert),
      decryptMigrationDataCollection(),
      decryptRacDacWorkItemCollection()
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

