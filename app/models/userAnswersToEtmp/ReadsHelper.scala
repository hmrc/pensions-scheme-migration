/*
 * Copyright 2022 HM Revenue & Customs
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

package models.userAnswersToEtmp

import play.api.libs.json._

import scala.annotation.tailrec

object ReadsHelper {

  def previousAddressDetails(
    addressYears: Boolean,
    previousAddress: Option[Address],
    tradingTime: Option[Boolean] = None): Option[PreviousAddressDetails] = {

    val tradingTimeAnswer = tradingTime.getOrElse(true)


    if (!addressYears && tradingTimeAnswer) {
      Some(
        PreviousAddressDetails(isPreviousAddressLast12Month = true, previousAddress)
      )
    }
    else {
      None
    }
  }

  //noinspection ConvertExpressionToSAM
  def readsFiltered[T](isA: JsValue => JsLookupResult, readsA: Reads[T], detailsType: String): Reads[Seq[T]] = new Reads[Seq[T]] {
    override def reads(json: JsValue): JsResult[Seq[T]] = {
      json match {
        case JsArray(establishers) =>
          readFilteredSeq(JsSuccess(Nil), filterDeleted(establishers, detailsType), isA, readsA)
        case _ => JsSuccess(Nil)
      }
    }
  }

  //noinspection ConvertExpressionToSAM
  def readsFilteredBoolean[T](isA: JsValue => Boolean, readsA: Reads[T], detailsType: String): Reads[Seq[T]] = new Reads[Seq[T]] {
    override def reads(json: JsValue): JsResult[Seq[T]] = {
      json match {
        case JsArray(establishers) =>
          readFilteredSeqBoolean(JsSuccess(Nil), filterDeleted(establishers, detailsType), isA, readsA)
        case _ => JsSuccess(Nil)
      }
    }
  }

  private def filterDeleted(jsValueSeq: Seq[JsValue], detailsType: String): Seq[JsValue] = {
    jsValueSeq.filterNot { json =>
      (json \ detailsType \ "isDeleted").validate[Boolean] match {
        case JsSuccess(e, _) => e
        case _ => false
      }
    }
  }

  @tailrec
  private def readFilteredSeq[T](result: JsResult[Seq[T]], js: Seq[JsValue], isA: JsValue => JsLookupResult, reads: Reads[T]): JsResult[Seq[T]] = {
    js match {
      case Seq(h, t@_*) =>
        isA(h) match {
          case JsDefined(_) =>
            reads.reads(h) match {
              case JsSuccess(individual, _) => readFilteredSeq(JsSuccess(result.get :+ individual), t, isA, reads)
              case error@JsError(_) => error
            }
          case _ => readFilteredSeq(result, t, isA, reads)
        }
      case Nil => result
    }
  }

  @tailrec
  private def readFilteredSeqBoolean[T](result: JsResult[Seq[T]], js: Seq[JsValue], isA: JsValue => Boolean, reads: Reads[T]): JsResult[Seq[T]] = {
    js match {
      case Seq(h, t@_*) =>
        if (isA(h)) {
          reads.reads(h) match {
            case JsSuccess(individual, _) =>
              readFilteredSeqBoolean(JsSuccess(result.get :+ individual), t, isA, reads)
            case error@JsError(_) =>
              error
          }
        } else {
          readFilteredSeqBoolean(result, t, isA, reads)
        }
      case Nil => result
    }
  }

}
