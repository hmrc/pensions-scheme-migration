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

package service

import audit._
import base.SpecBase
import connector.SchemeConnector
import models.enumeration.SchemeType
import models.userAnswersToEtmp.establisher.EstablisherDetails
import models.userAnswersToEtmp.trustee.TrusteeDetails
import models.userAnswersToEtmp.{CustomerAndSchemeDetails, PensionSchemeDeclaration, PensionsScheme, SchemeMigrationDetails}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.MockitoSugar._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.http.Status
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}

import scala.concurrent.Future

class PensionSchemeServiceSpec
  extends AsyncFlatSpec
    with Matchers
    with EitherValues {

  import PensionSchemeServiceSpec._
  val schemeSubscription: SchemeMigrationAuditEvent = SchemeMigrationAuditEvent(
    psaId = psaId,
    pstr = pstr,
    status = Status.OK,
    request = Json.obj(),
    response = None
  )

  "registerScheme" must "return the result of submitting a pensions scheme " in {
    reset(schemeConnector)
    val regDataWithRacDacNode = schemeJsValue.as[JsObject]
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))
    pensionSchemeService.registerScheme(psaId, pensionsSchemeJson).map {
      response =>
        val json = response.right.value
        verify(schemeConnector, times(1)).registerScheme(any(), eqTo(regDataWithRacDacNode))(any())
        json.validate[SchemeRegistrationResponse] mustBe JsSuccess(schemeRegistrationResponse)
    }
  }

  "registerScheme" must "return the result of submitting a RAC/DAC pensions scheme" in {
    reset(schemeConnector)
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))

    pensionSchemeService.registerRacDac(psaId, racDACPensionsSchemeJson)(implicitly,implicitly,Some(implicitly)).map {
      response =>
        val json = response.right.value
        verify(schemeConnector, times(1)).registerScheme(any(), eqTo(racDacRegisterData))(any())
        json.validate[SchemeRegistrationResponse] mustBe JsSuccess(schemeRegistrationResponse)
    }
  }

  "register scheme" must "send a SchemeMigrationAudit event following a successful submission" in {
    reset(schemeConnector)
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))
    pensionSchemeService.registerScheme(psaId, pensionsSchemeJson).map {
      response =>
        val json = response.right.value
        val expected = schemeSubscription.copy(
          status = Status.OK,
          request = expectedJsonForAudit,
          response = Some(json)
        )
        auditService.verifySent(expected) mustBe true
    }
  }

  it must "send a SchemeMigrationAudit event following an unsuccessful submission" in {
    reset(schemeConnector)
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.failed(new BadRequestException("bad request")))

    pensionSchemeService.registerScheme(psaId, pensionsSchemeJson)
      .map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          val expected = schemeSubscription.copy(
            status = Status.BAD_REQUEST,
            request = expectedJsonForAudit,
            response = None
          )
          auditService.verifySent(expected) mustBe true
      }
  }

  "register RAC DAC scheme" must "send a RacDacMigrationAuditEvent event following a successful submission" in {
    reset(schemeConnector)
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))
    pensionSchemeService.registerRacDac(psaId, racDACPensionsSchemeJson,true).map {
      response =>
        val json = response.right.value
        val expected = RacDacMigrationAuditEvent(
          psaId = psaId,
          pstr=pstr,
          status = Status.OK,
          request = racDacRegisterAuditData,
          response = Some(json)
        )
        auditService.verifyExplicitSent(expected) mustBe true
    }
  }

  it must "send a RacDacMigrationAuditEvent event following an unsuccessful submission" in {
    reset(schemeConnector)
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.failed(new BadRequestException("bad request")))

    pensionSchemeService.registerRacDac(psaId, racDACPensionsSchemeJson,true)
      .map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          val expected = RacDacMigrationAuditEvent(
            psaId = psaId,
            pstr=pstr,
            status = Status.BAD_REQUEST,
            request = racDacRegisterAuditData,
            response = None
          )
          auditService.verifyExplicitSent(expected) mustBe true
      }
  }

}

object PensionSchemeServiceSpec extends SpecBase {

  private val schemeConnector: SchemeConnector = mock[SchemeConnector]
  private val auditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()

  private val pensionSchemeService: PensionSchemeService = new PensionSchemeService(
    schemeConnector,auditService, new SchemeAuditService
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("", "")

  val psaId: String = "test-psa-id"

  val pstr: String = "test-pstr"

  private val racDACPensionsSchemeJson: JsValue = Json.obj(
      "schemeName" -> "test-scheme-name",
      "policyNo" -> "121212",
      "pstr" -> pstr,
      "schemeOpenDate" -> "2012-02-20",
      "relationshipStartDate" ->"2020-01-01"
  )

  val racDacRegisterData = Json.obj(
    "schemeMigrationDetails" -> Json.obj(
      "pstrOrTpssId" -> pstr,
      "registrationStartDate" -> "2012-02-20",
      "psaRelationshipStartDate" -> "2020-01-01" ,
    ),
    "racdacScheme" -> true,
    "racdacSchemeDetails" -> Json.obj(
      "racdacName" -> "test-scheme-name",
      "contractOrPolicyNumber" -> "121212",
      "registrationStartDate" -> "2012-02-20",
    ),
    "racdacSchemeDeclaration" -> Json.obj(
      "box12" -> true,
      "box13" -> true,
      "box14" -> true
    )
  )

  val racDacRegisterAuditData = Json.obj(
    "schemeMigrationDetails" -> Json.obj(
      "pstrOrTpssId" -> pstr,
      "registrationStartDate" -> "2012-02-20",
      "psaRelationshipStartDate" -> "2020-01-01" ,
    ),
    "racdacScheme" -> true,
    "racdacSchemeDetails" -> Json.obj(
      "racdacName" -> "test-scheme-name",
      "contractOrPolicyNumber" -> "121212",
      "registrationStartDate" -> "2012-02-20",
    )
  )

  val pensionsScheme: PensionsScheme = PensionsScheme(
    SchemeMigrationDetails(pstrOrTpssId = pstr,
      registrationStartDate = "2012-02-20",
      psaRelationshipStartDate = "2020-01-01"
    ),
    CustomerAndSchemeDetails(
      schemeName = "test-pensions-scheme",
      isSchemeMasterTrust = false,
      schemeStructure = Some(SchemeType.single.value),
      currentSchemeMembers = "test-current-scheme-members",
      futureSchemeMembers = "test-future-scheme-members",
      isRegulatedSchemeInvestment = false,
      isOccupationalPensionScheme = false,
      areBenefitsSecuredContractInsuranceCompany = false,
      doesSchemeProvideBenefits = "test-does-scheme-provide-benefits",
      tcmpBenefitType = Some("01"),
      schemeEstablishedCountry = "test-scheme-established-country",
      haveInvalidBank = false
    ),
    PensionSchemeDeclaration(
      box6 = true,
      box7 = true,
      box8 = true
    ),
    EstablisherDetails(
      Nil,
      Nil,
      Nil
    ),
    TrusteeDetails(
      Nil,
      Nil,
      Nil
    )
  )

  val pensionsSchemeJson: JsValue = Json.obj(
    "schemeName" -> "test-scheme-name",
    "isSchemeMasterTrust" -> false,
    "schemeType" -> Json.obj(
      "name" -> SchemeType.single.name
    ),
    "membership" -> "opt1",
    "pstr" -> pstr,
    "schemeOpenDate" -> "2012-02-20",
    "relationshipStartDate" ->"2020-01-01",
    "currentMembers" -> "opt1",
    "futureMembers" -> "opt1",
    "investmentRegulated" -> false,
    "occupationalPensionScheme" -> false,
    "securedBenefits" -> false,
    "benefits" -> "moneyPurchaseOnly",
    "moneyPurchaseBenefits" -> "cashBalanceAndOtherMoneyPurchaseBenefits",
    "schemeEstablishedCountry" -> "test-scheme-established-country",
    "insuranceCompanyName" -> "Test insurance company name",
    "insurancePolicyNumber" -> "Test insurance policy number",
    "workingKnowledge" -> true,
    "establishers" -> Json.arr(
      Json.obj(
        "establisherDetails" -> Json.obj(
          "firstName" -> "test-first-name",
          "lastName" -> "test-last-name"
        ),
        "dateOfBirth" -> "1969-07-20",
          "email" -> "test-email-address",
          "phone" -> "test-phone-number",
        "address" -> Json.obj(
          "addressLine1" -> "test-address-line-1",
          "country" -> "test-country"
        ),
        "addressYears" -> true,
        "establisherKind" -> "individual"
      )
    )
  )

  private val schemeJsValue: JsValue =
    Json.parse(
      """
        |{
        |  "schemeMigrationDetails": {
        |    "pstrOrTpssId": "test-pstr",
        |    "registrationStartDate": "2012-02-20",
        |    "psaRelationshipStartDate": "2020-01-01"
        |  },
        |    "customerAndSchemeDetails": {
        |    "schemeName": "test-scheme-name",
        |    "isSchemeMasterTrust": false,
        |    "schemeStructure": "A single trust under which all of the assets are held for the benefit of all members of the scheme",
        |    "currentSchemeMembers": "0",
        |    "futureSchemeMembers": "0",
        |    "isRegulatedSchemeInvestment": false,
        |    "isOccupationalPensionScheme": false,
        |    "areBenefitsSecuredContractInsuranceCompany": false,
        |    "doesSchemeProvideBenefits": "Money Purchase benefits only (defined contribution)",
        |    "tcmpBenefitType": "05",
        |    "schemeEstablishedCountry": "test-scheme-established-country",
        |    "haveInvalidBank": false,
        |    "insuranceCompanyName": "Test insurance company name",
        |    "policyNumber": "Test insurance policy number"
        |  },
        |    "pensionSchemeDeclaration": {
        |    "box6": true,
        |    "box7": true,
        |    "box8": true,
        |    "box10": true
        |  },
        |  "establisherDetails": {
        |    "individual": [
        |      {
        |        "personalDetails": {
        |          "firstName": "test-first-name",
        |          "lastName": "test-last-name",
        |          "dateOfBirth": "1969-07-20"
        |        },
        |        "correspondenceAddressDetails": {
        |          "addressDetails": {
        |            "line1": "test-address-line-1",
        |            "countryCode": "test-country",
        |            "addressType": "NON-UK"
        |          }
        |        },
        |        "correspondenceContactDetails": {
        |          "contactDetails": {
        |            "telephone": "test-phone-number",
        |            "email": "test-email-address"
        |          }
        |        }
        |      }
        |    ],
        |    "companyOrOrganization": [],
        |    "partnership": []
        |  },
        |  "trusteeDetails": {
        |    "individualTrusteeDetail": [],
        |    "companyTrusteeDetail": [],
        |    "partnershipTrusteeDetail": []
        |  }
        |}
        |""".stripMargin)


  val expectedJsonForAudit: JsValue = Json.parse(
    """{
     "schemeMigrationDetails": {
      "pstrOrTpssId": "test-pstr",
      "registrationStartDate": "2012-02-20",
      "psaRelationshipStartDate": "2020-01-01"
    },
   "customerAndSchemeDetails":{
      "schemeName":"test-scheme-name",
      "isSchemeMasterTrust":false,
      "schemeStructure":"A single trust under which all of the assets are held for the benefit of all members of the scheme",
      "currentSchemeMembers":"0",
      "futureSchemeMembers":"0",
      "isRegulatedSchemeInvestment":false,
      "isOccupationalPensionScheme":false,
      "areBenefitsSecuredContractInsuranceCompany":false,
      "doesSchemeProvideBenefits":"Money Purchase benefits only (defined contribution)",
      "tcmpBenefitType":"05",
      "schemeEstablishedCountry":"test-scheme-established-country",
      "haveInvalidBank":false,
      "insuranceCompanyName":"Test insurance company name",
      "policyNumber":"Test insurance policy number"
   },
   "pensionSchemeDeclaration":{},
   "establisherDetails":{
      "individual":[
         {
            "personalDetails":{
               "firstName":"test-first-name",
               "lastName":"test-last-name",
               "dateOfBirth":"1969-07-20"
            },
            "correspondenceAddressDetails":{
               "addressDetails":{
                  "line1":"test-address-line-1",
                  "countryCode":"test-country",
                  "addressType":"NON-UK"
               }
            },
            "correspondenceContactDetails":{
               "contactDetails":{
                  "telephone":"test-phone-number",
                  "email":"test-email-address"
               }
            }
         }
      ],
      "companyOrOrganization":[

      ],
      "partnership":[

      ]
   },
   "trusteeDetails":{
      "individualTrusteeDetail":[

      ],
      "companyTrusteeDetail":[

      ],
      "partnershipTrusteeDetail":[

      ]
   }
  }""")

  val schemeRegistrationResponse: SchemeRegistrationResponse = SchemeRegistrationResponse(
    "test-processing-date",
    "test-scheme-reference-number")
  val schemeRegistrationResponseJson: JsValue =
    Json.toJson(schemeRegistrationResponse)
}

case class SchemeRegistrationResponse(processingDate: String, schemeReferenceNumber: String)

object SchemeRegistrationResponse {
  implicit val formatsSchemeRegistrationResponse: Format[SchemeRegistrationResponse] = Json.format[SchemeRegistrationResponse]
}


