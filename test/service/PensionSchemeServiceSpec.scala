/*
 * Copyright 2023 HM Revenue & Customs
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
import connector.SchemeConnector
import models.enumeration.SchemeType
import models.userAnswersToEtmp.establisher.EstablisherDetails
import models.userAnswersToEtmp.trustee.TrusteeDetails
import models.userAnswersToEtmp.{CustomerAndSchemeDetails, PensionSchemeDeclaration, PensionsScheme, SchemeMigrationDetails}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repositories.{DeclarationLockRepository, ListOfLegacySchemesCacheRepository}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException}
import utils.AuthUtils

import scala.concurrent.Future

class PensionSchemeServiceSpec
  extends AsyncFlatSpec
    with Matchers
    with EitherValues
    with BeforeAndAfterEach {

  import PensionSchemeServiceSpec._

  val schemeSubscription: SchemeMigrationAuditEvent = SchemeMigrationAuditEvent(
    psaId = psaId,
    pstr = pstr,
    status = Status.OK,
    request = Json.obj(),
    response = None
  )

  override protected def beforeEach(): Unit = {
    reset(schemeConnector)
    reset(declarationLockRepository)
    reset(mockListOfLegacySchemesCacheRepository)
    super.beforeEach()
  }

  private val pensionSchemeService: PensionSchemeService = new PensionSchemeService(
    schemeConnector, auditService, new SchemeAuditService, declarationLockRepository,
    mockListOfLegacySchemesCacheRepository
  )

  "registerScheme" must "return the result of false when declaration has already done with same psaId and pstr " in {
    val regDataWithRacDacNode = schemeJsValue.as[JsObject]
    when(declarationLockRepository.insertLockData(any(), any())).
      thenReturn(Future.successful(false))
    pensionSchemeService.registerScheme(psaId, pensionsSchemeJson).map {
      response =>
        verify(schemeConnector, never()).registerScheme(any(), eqTo(regDataWithRacDacNode))(any())
        verify(declarationLockRepository, times(1)).insertLockData(any(), any())
        response mustBe Right(JsBoolean(false))
    }
  }

  "registerScheme" must "return the result of submitting a pensions scheme " in {
    val regDataWithRacDacNode = schemeJsValue.as[JsObject]
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))
    when(declarationLockRepository.insertLockData(any(), any())).
      thenReturn(Future.successful(true))
    pensionSchemeService.registerScheme(psaId, pensionsSchemeJson).map {
      response =>
        val json = response.value
        verify(schemeConnector, times(1)).registerScheme(any(), eqTo(regDataWithRacDacNode))(any())
        verify(declarationLockRepository, times(1)).insertLockData(any(), any())
        json.validate[SchemeRegistrationResponse] mustBe JsSuccess(schemeRegistrationResponse)
    }
  }

  "registerScheme" must "return BadRequestException if json cannot be validated" in {
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))

    val result = pensionSchemeService.registerScheme(psaId, Json.obj())

    ScalaFutures.whenReady(result.failed) { e =>
      e mustBe a[BadRequestException]
      e.getMessage mustBe "Invalid pension scheme"
    }
  }

  "registerScheme" must "return the result of submitting a RAC/DAC pensions scheme" in {
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))

    pensionSchemeService.registerRacDac(psaId, racDACPensionsSchemeJson)(implicitly, implicitly, Some(implicitly)).map {
      response =>
        val json = response.value
        verify(schemeConnector, times(1)).registerScheme(any(), eqTo(racDacRegisterData))(any())
        json.validate[SchemeRegistrationResponse] mustBe JsSuccess(schemeRegistrationResponse)
    }
  }

  "registerScheme" must "return BadRequestException if json cannot be validated for RAC/DAC" in {
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))

    val result = pensionSchemeService.registerRacDac(psaId, Json.obj())(implicitly, implicitly, Some(implicitly))

    ScalaFutures.whenReady(result.failed) { e =>
      e mustBe a[BadRequestException]
      e.getMessage mustBe "Invalid pension scheme"
    }
  }

  "register scheme" must "send a SchemeMigrationAudit event following a successful submission" in {
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))
    when(declarationLockRepository.insertLockData(any(), any())).
      thenReturn(Future.successful(true))
    pensionSchemeService.registerScheme(psaId, pensionsSchemeJson).map {
      response =>
        val json = response.value
        val expected = schemeSubscription.copy(
          status = Status.OK,
          request = expectedJsonForAudit,
          response = Some(json)
        )
        auditService.verifySent(expected) mustBe true
    }
  }

  it must "send a SchemeMigrationAudit event following an unsuccessful submission" in {
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.failed(new BadRequestException("bad request")))
    when(declarationLockRepository.insertLockData(any(), any())).
      thenReturn(Future.successful(true))

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
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))
    pensionSchemeService.registerRacDac(psaId, racDACPensionsSchemeJson, isBulk = true).map {
      response =>
        val json = response.value
        val expected = RacDacMigrationAuditEvent(
          psaId = psaId,
          pstr = pstr,
          status = Status.OK,
          request = racDacRegisterAuditData,
          response = Some(json)
        )
        auditService.verifyExplicitSent(expected) mustBe true
    }
  }

  it must "send a RacDacMigrationAuditEvent event following an unsuccessful submission" in {
    when(schemeConnector.registerScheme(any(), any())(any())).
      thenReturn(Future.failed(new BadRequestException("bad request")))

    pensionSchemeService.registerRacDac(psaId, racDACPensionsSchemeJson, isBulk = true)
      .map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          val expected = RacDacMigrationAuditEvent(
            psaId = psaId,
            pstr = pstr,
            status = Status.BAD_REQUEST,
            request = racDacRegisterAuditData,
            response = None
          )
          auditService.verifyExplicitSent(expected) mustBe true
      }
  }

  "getListOfLegacySchemes" must "return the list of legacy schemes" in {
    val validResponse = Json.parse(
      """{
        |  "totalResults": 2,
        |  "items": [
        |    {
        |      "pstr": "00241768RH",
        |      "declarationDate": "0001-01-01T00:00:00",
        |      "schemeName": "THE AMDAIL PENSION SCHEME",
        |      "schemeOpenDate": "2006-04-05T00:00:00",
        |      "racDac": false
        |    },
        |    {
        |      "pstr": "00615269RH",
        |      "declarationDate": "2012-02-20T00:00:00",
        |      "schemeName": "paul qqq",
        |      "schemeOpenDate": "paul qqq",
        |      "racDac": true,
        |      "policyNo": "24101975"
        |    }
        |  ]
        |}""".stripMargin)

    when(mockListOfLegacySchemesCacheRepository.get(ArgumentMatchers.eq(AuthUtils.psaId))(any())).thenReturn(Future.successful(None))
    when(schemeConnector.listOfLegacySchemes(ArgumentMatchers.eq(AuthUtils.psaId))(any(), any())).thenReturn(Future.successful(Right(validResponse)))
    when(mockListOfLegacySchemesCacheRepository.upsert(ArgumentMatchers.eq(AuthUtils.psaId), ArgumentMatchers.eq(validResponse))(any())).thenReturn(Future.successful(true))
    val result = pensionSchemeService.getListOfLegacySchemes(AuthUtils.psaId)
    result.map { result =>
      result mustBe Right(validResponse)
    }
  }

  it must "return the list of legacy schemes from cache if it's available" in {
    val validResponse = Json.parse(
      """{
        |  "totalResults": 2,
        |  "items": [
        |    {
        |      "pstr": "00241768RH",
        |      "declarationDate": "0001-01-01T00:00:00",
        |      "schemeName": "THE AMDAIL PENSION SCHEME",
        |      "schemeOpenDate": "2006-04-05T00:00:00",
        |      "racDac": false
        |    },
        |    {
        |      "pstr": "00615269RH",
        |      "declarationDate": "2012-02-20T00:00:00",
        |      "schemeName": "paul qqq",
        |      "schemeOpenDate": "paul qqq",
        |      "racDac": true,
        |      "policyNo": "24101975"
        |    }
        |  ]
        |}""".stripMargin)

    when(mockListOfLegacySchemesCacheRepository.get(ArgumentMatchers.eq(AuthUtils.psaId))(any())).thenReturn(Future.successful(Some(validResponse)))
    val result = pensionSchemeService.getListOfLegacySchemes(AuthUtils.psaId)
    result.map { result =>
      result mustBe Right(validResponse)
    }
  }

  "isAssociated" must "return true if psaId is associated with the scheme list for that pstr" in {
    val validResponse = Json.parse(
      """{
        |  "totalResults": 2,
        |  "items": [
        |    {
        |      "pstr": "00241768RH",
        |      "declarationDate": "0001-01-01T00:00:00",
        |      "schemeName": "THE AMDAIL PENSION SCHEME",
        |      "schemeOpenDate": "2006-04-05T00:00:00",
        |      "racDac": false
        |    },
        |    {
        |      "pstr": "00615269RH",
        |      "declarationDate": "2012-02-20T00:00:00",
        |      "schemeName": "paul qqq",
        |      "schemeOpenDate": "paul qqq",
        |      "racDac": true,
        |      "policyNo": "24101975"
        |    }
        |  ]
        |}""".stripMargin)

    when(mockListOfLegacySchemesCacheRepository.get(ArgumentMatchers.eq(AuthUtils.psaId))(any())).thenReturn(Future.successful(Some(validResponse)))
    val result = pensionSchemeService.isAssociated(PsaId(AuthUtils.psaId), "00241768RH")
    result.map { result =>
      result mustBe true
    }
  }

  it must "return false if psaId is not associated with the scheme list for that pstr" in {
    val validResponse = Json.parse(
      """{
        |  "totalResults": 2,
        |  "items": [
        |    {
        |      "pstr": "00241768RH",
        |      "declarationDate": "0001-01-01T00:00:00",
        |      "schemeName": "THE AMDAIL PENSION SCHEME",
        |      "schemeOpenDate": "2006-04-05T00:00:00",
        |      "racDac": false
        |    },
        |    {
        |      "pstr": "00615269RH",
        |      "declarationDate": "2012-02-20T00:00:00",
        |      "schemeName": "paul qqq",
        |      "schemeOpenDate": "paul qqq",
        |      "racDac": true,
        |      "policyNo": "24101975"
        |    }
        |  ]
        |}""".stripMargin)

    when(mockListOfLegacySchemesCacheRepository.get(ArgumentMatchers.eq(AuthUtils.psaId))(any())).thenReturn(Future.successful(Some(validResponse)))
    val result = pensionSchemeService.isAssociated(PsaId(AuthUtils.psaId), "invalidPstr")
    result.map { result =>
      result mustBe false
    }
  }

  it must "throw RuntimeException if error returned from upstream" in {

    reset(mockListOfLegacySchemesCacheRepository)
    reset(schemeConnector)
    when(mockListOfLegacySchemesCacheRepository.get(ArgumentMatchers.eq(AuthUtils.psaId))(any())).thenReturn(Future.successful(None))
    when(schemeConnector.listOfLegacySchemes(ArgumentMatchers.eq(AuthUtils.psaId))(any(), any())).thenReturn(Future.successful(Left(new HttpException("", 500))))
    val result = pensionSchemeService.isAssociated(PsaId(AuthUtils.psaId), "invalidPstr")
    result.failed.map { e =>
      e mustBe a[RuntimeException]
      e.getMessage mustBe "Unable to retrieve list of legacy schemes"
    }
  }

}

object PensionSchemeServiceSpec extends MockitoSugar {

  private val schemeConnector: SchemeConnector = mock[SchemeConnector]
  private val declarationLockRepository: DeclarationLockRepository = mock[DeclarationLockRepository]
  private val auditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()
private val mockListOfLegacySchemesCacheRepository = mock[ListOfLegacySchemesCacheRepository]

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
    "relationshipStartDate" -> "2020-01-01"
  )

  val racDacRegisterData: JsObject = Json.obj(
    "schemeMigrationDetails" -> Json.obj(
      "pstrOrTpssId" -> pstr,
      "registrationStartDate" -> "2012-02-20",
      "psaRelationshipStartDate" -> "2020-01-01",
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

  val racDacRegisterAuditData: JsObject = Json.obj(
    "schemeMigrationDetails" -> Json.obj(
      "pstrOrTpssId" -> pstr,
      "registrationStartDate" -> "2012-02-20",
      "psaRelationshipStartDate" -> "2020-01-01",
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
    "relationshipStartDate" -> "2020-01-01",
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


