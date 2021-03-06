/*
 * Copyright 2017 HM Revenue & Customs
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

package support

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPathPattern
import common.enums.{CacheKeys, VatRegStatus}
import play.api.libs.json.{Format, JsObject, Json}
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.crypto.CompositeSymmetricCrypto.aes
import uk.gov.hmrc.crypto.json.{JsonDecryptor, JsonEncryptor}

trait StubUtils {
  me: StartAndStopWireMock =>

  final class RequestHolder(var request: FakeRequest[AnyContentAsFormUrlEncoded])

  implicit lazy val jsonCrypto = ApplicationCrypto.JsonCrypto
  implicit lazy val encryptionFormat = new JsonEncryptor[JsObject]()
  implicit lazy val decryptionFormat = new JsonDecryptor[JsObject]()

  class PreconditionBuilder {

    implicit val builder: PreconditionBuilder = this

    def address(id: String, line1: String, line2: String, country: String, postcode: String) =
      AddressStub(id, line1, line2, country, postcode)

    def postRequest(data: Map[String, String])(implicit requestHolder: RequestHolder): PreconditionBuilder = {
      val requestWithBody = FakeRequest("POST", "/").withFormUrlEncodedBody(data.toArray: _*)
      requestHolder.request = requestWithBody
      this
    }

    def user = UserStub()

    def journey(id: String) = JourneyStub(id)

    def vatRegistrationFootprint = VatRegistrationFootprintStub()

    def vatScheme = VatSchemeStub()

    def corporationTaxRegistration = CorporationTaxRegistrationStub()

    def currentProfile = CurrentProfile()

    def company = IncorporationStub()

    def s4lContainer: ViewModelStub = new ViewModelStub()
    def s4lContainerInScenario: ViewModelScenarioStub = new ViewModelScenarioStub()

    def audit = AuditStub()

    def keystore = new KeystoreStubWrapper()
    def keystoreInScenario = new KeystoreStubScenarioWrapper()

    def businessReg = BusinessRegStub()

  }

  def given(): PreconditionBuilder = {
    new PreconditionBuilder()
  }

  class KeystoreStubWrapper()(implicit builder:PreconditionBuilder) extends KeystoreStub {
    def hasKeyStoreValue(key: String, data: String):PreconditionBuilder ={
      stubFor(stubKeystoreGet(key,data))
      builder
    }

    def putKeyStoreValue(key:String, data: String):PreconditionBuilder ={
      stubFor(stubKeystorePut(key,data))
      builder
    }

    def deleteKeystore(): PreconditionBuilder = {
      stubFor(stubKeystoreRemove())
      builder
    }

    def keystoreGetNotFound(): PreconditionBuilder = {
      stubFor(stubKeystoreGetNotFound())
      builder
    }
  }

  class KeystoreStubScenarioWrapper(scenario: String = "Keystore Scenario")(implicit builder: PreconditionBuilder) extends KeystoreStubWrapper {
    def hasKeyStoreValue(key: String, data: String, currentState: Option[String] = None, nextState: Option[String] = None): PreconditionBuilder = {
      val mpScenarioGET = stubKeystoreGet(key,data).inScenario(scenario)
      val mpGET = currentState.fold(mpScenarioGET)(mpScenarioGET.whenScenarioStateIs)
      stubFor(nextState.fold(mpGET)(mpGET.willSetStateTo))
      builder
    }

    def putKeyStoreValue(key: String, data: String, currentState: Option[String] = None, nextState: Option[String] = None): PreconditionBuilder = {
      val mpScenarioPUT = stubKeystorePut(key,data).inScenario(scenario)
      val mpPUT = currentState.fold(mpScenarioPUT)(mpScenarioPUT.whenScenarioStateIs)
      stubFor(nextState.fold(mpPUT)(mpPUT.willSetStateTo))
      builder
    }
  }

  case class BusinessRegStub()(implicit builder:PreconditionBuilder)  {
    def getBusinessprofileSuccessfully = {
      stubFor(
        get(urlPathEqualTo("/business-registration/business-tax-registration"))
          .willReturn((ok(
            """
              |{
              | "registrationID" : "1",
              | "language" : "EN"
              |}
              |"""".stripMargin)
            )
          )
      )
      builder
    }

    def failsToGetBusinessProfile = {
      stubFor(
        get(urlPathEqualTo("/business-registration/business-tax-registration"))
          .willReturn(notFound())
      )
      builder
    }
  }

  trait KeystoreStub {
    def stubKeystorePut(key: String, data: String): MappingBuilder =
      put(urlPathMatching(s"/keystore/vat-registration-eligibility-frontend/session-[a-z0-9-]+/data/$key"))
        .willReturn(ok(
          s"""
             |{ "atomicId": { "$$oid": "598ac0b64e0000d800170620" },
             |    "data": { "$key": $data },
             |    "id": "session-ac4ed3e7-dbc3-4150-9574-40771c4285c1",
             |    "modifiedDetails": {
             |      "createdAt": { "$$date": 1502265526026 },
             |      "lastUpdated": { "$$date": 1502265526026 }}}
          """.stripMargin
        ))

    def stubKeystoreGet(key: String, data: String): MappingBuilder =
      get(urlPathMatching("/keystore/vat-registration-eligibility-frontend/session-[a-z0-9-]+"))
        .willReturn(ok(
          s"""
             |{ "atomicId": { "$$oid": "598ac0b64e0000d800170620" },
             |    "data": { "$key": $data },
             |    "id": "session-ac4ed3e7-dbc3-4150-9574-40771c4285c1",
             |    "modifiedDetails": {
             |      "createdAt": { "$$date": 1502265526026 },
             |      "lastUpdated": { "$$date": 1502265526026 }}}
          """.stripMargin
        ))

    def stubKeystoreRemove(): MappingBuilder = {
      delete(urlMatching("/keystore/vat-registration-eligibility-frontend/session-[a-z0-9-]+"))
        .willReturn(ok())
    }

    def stubKeystoreGetNotFound(): MappingBuilder = {
      get(urlPathMatching("/keystore/vat-registration-eligibility-frontend/session-[a-z0-9-]+"))
        .willReturn(notFound())
    }

  }

  trait S4LStub {
    import uk.gov.hmrc.crypto._

    //TODO get the json.encryption.key config value from application.conf
    val crypto: CompositeSymmetricCrypto = aes("fqpLDZ4sumDsekHkeEBlCA==", Seq.empty)

    def decrypt(encData: String): String = crypto.decrypt(Crypted(encData)).value
    def encrypt(str: String): String = crypto.encrypt(PlainText(str)).value

    def stubS4LPut(key: String, data: String): MappingBuilder =
        put(urlPathMatching(s"/save4later/vat-registration-eligibility-frontend/1/data/$key"))
          .willReturn(ok(
            s"""
               |{ "atomicId": { "$$oid": "598ac0b64e0000d800170620" },
               |    "data": { "$key": "${encrypt(data)}" },
               |    "id": "1",
               |    "modifiedDetails": {
               |      "createdAt": { "$$date": 1502265526026 },
               |      "lastUpdated": { "$$date": 1502265526026 }}}
            """.stripMargin
          ))

    def stubS4LGet[T](key: String, t: T)(implicit fmt: Format[T]): MappingBuilder = {
      get(urlPathMatching("/save4later/vat-registration-eligibility-frontend/1"))
        .willReturn(ok(
          s"""
             |{
             |  "atomicId": { "$$oid": "598830cf5e00005e00b3401e" },
             |  "data": {
             |    "$key": "${encrypt(fmt.writes(t).toString())}"
             |  },
             |  "id": "1",
             |  "modifiedDetails": {
             |    "createdAt": { "$$date": 1502097615710 },
             |    "lastUpdated": { "$$date": 1502189409725 }
             |  }
             |}
          """.stripMargin
        ))
    }

    def stubS4LGetNothing(): MappingBuilder = {
      get(urlPathMatching("/save4later/vat-registration-eligibility-frontend/1"))
        .willReturn(ok(
          s"""
             |{
             |  "atomicId": { "$$oid": "598830cf5e00005e00b3401e" },
             |  "data": {},
             |  "id": "1",
             |  "modifiedDetails": {
             |    "createdAt": { "$$date": 1502097615710 },
             |    "lastUpdated": { "$$date": 1502189409725 }
             |  }
             |}
          """.stripMargin
        ))
    }

    def stubS4LClear(): MappingBuilder =
      delete(urlPathMatching("/save4later/vat-registration-eligibility-frontend/1")).willReturn(ok(""))
  }


  class ViewModelStub()(implicit builder: PreconditionBuilder) extends S4LStub with KeystoreStub {

    def contains[T](key: String, t: T)(implicit fmt: Format[T]): PreconditionBuilder = {
      stubFor(stubS4LGet[T](key, t))
      builder
    }


    def isUpdatedWith[T](key: String, t: T)(implicit fmt: Format[T]): PreconditionBuilder = {
      stubFor(stubS4LPut(key, fmt.writes(t).toString()))
      builder
    }

    def isEmpty: PreconditionBuilder = {
      stubFor(stubS4LGetNothing())
      builder
    }

    def cleared: PreconditionBuilder = {
      stubFor(stubS4LClear())
      builder
    }
  }

  class ViewModelScenarioStub(scenario: String = "S4L Scenario")
                             (implicit builder: PreconditionBuilder) extends ViewModelStub {

    def contains[T](key: String, t: T, currentState: Option[String] = None, nextState: Option[String] = None)
                   (implicit fmt: Format[T]): PreconditionBuilder = {
      val mappingBuilderScenarioGET = stubS4LGet[T](key, t).inScenario(scenario)
      val mappingBuilderGET = currentState.fold(mappingBuilderScenarioGET)(mappingBuilderScenarioGET.whenScenarioStateIs)

      stubFor(nextState.fold(mappingBuilderGET)(mappingBuilderGET.willSetStateTo))
      builder
    }

    def isUpdatedWith[T](key: String, t: T, currentState: Option[String] = None, nextState: Option[String] = None)
                        (implicit fmt: Format[T]): PreconditionBuilder = {
      val mappingBuilderScenarioPUT = stubS4LPut(key, fmt.writes(t).toString()).inScenario(scenario)
      val mappingBuilderPUT = currentState.fold(mappingBuilderScenarioPUT)(mappingBuilderScenarioPUT.whenScenarioStateIs)

      stubFor(nextState.fold(mappingBuilderPUT)(mappingBuilderPUT.willSetStateTo))
      builder
    }

    def isEmpty(currentState: Option[String] = None, nextState: Option[String] = None): PreconditionBuilder = {
      val mappingBuilderScenarioGET = stubS4LGetNothing().inScenario(scenario)
      val mappingBuilderGET = currentState.fold(mappingBuilderScenarioGET)(mappingBuilderScenarioGET.whenScenarioStateIs)

      stubFor(nextState.fold(mappingBuilderGET)(mappingBuilderGET.willSetStateTo))
      builder
    }

    def cleared(currentState: Option[String] = None, nextState: Option[String] = None): PreconditionBuilder = {
      val mappingBuilderScenarioDELETE = stubS4LClear().inScenario(scenario)
      val mappingBuilderDELETE = currentState.fold(mappingBuilderScenarioDELETE)(mappingBuilderScenarioDELETE.whenScenarioStateIs)

      stubFor(nextState.fold(mappingBuilderDELETE)(mappingBuilderDELETE.willSetStateTo))
      builder
    }
  }


  case class IncorporationStub()(implicit builder: PreconditionBuilder) extends KeystoreStub {

    def isIncorporated: PreconditionBuilder = {

      stubFor(
        get(urlPathEqualTo("/vatreg/incorporation-information/000-434-1"))
          .willReturn(ok(
            s"""
               |{
               |  "statusEvent": {
               |    "crn": "90000001",
               |    "incorporationDate": "2016-08-05",
               |    "status": "accepted"
               |  },
               |  "subscription": {
               |    "callbackUrl": "http://localhost:9896/callbackUrl",
               |    "regime": "vat",
               |    "subscriber": "scrs",
               |    "transactionId": "000-434-1"
               |  }
               |}
             """.stripMargin
          ))
      )
      builder
    }

    def incorporationStatusNotKnown(): PreconditionBuilder = {
      stubFor(
        get(urlPathEqualTo(s"/vatreg/incorporation-information/000-434-1"))
          .willReturn(notFound().withBody(
            s"""
               |{
               |  "errorCode": 404,
               |  "errorMessage": "Incorporation Status not known. A subscription has been setup"
               |}
             """.stripMargin
          )))
      builder
    }
  }


  case class CorporationTaxRegistrationStub
  ()
  (implicit builder: PreconditionBuilder) {

    def existsWithStatus(status: String): PreconditionBuilder = {
      stubFor(
        get(urlPathEqualTo(s"/incorporation-frontend-stubs/1/corporation-tax-registration"))
          .willReturn(ok(
            s"""{ "confirmationReferences": { "transaction-id": "000-434-1" }, "status": "$status" }"""
          )))
      builder
    }

  }

  case class CurrentProfile()(implicit builder: PreconditionBuilder) extends KeystoreStubScenarioWrapper {
    val incorpInfo = s"""
                        |{
                        |  "statusEvent": {
                        |    "crn": "90000001",
                        |    "incorporationDate": "2016-08-05",
                        |    "status": "accepted"
                        |  },
                        |  "subscription": {
                        |    "callbackUrl": "http://localhost:9896/callbackUrl",
                        |    "regime": "vat",
                        |    "subscriber": "scrs",
                        |    "transactionId": "000-434-1"
                        |  }
                        |}
             """.stripMargin

    val businessProfile = s"""
                             |{
                             | "registrationID" : "1",
                             | "language" : "EN"
                             |}
                             |"""".stripMargin

    def setup(status: VatRegStatus.Value = VatRegStatus.draft, currentState: Option[String] = None, nextState: Option[String] = None): PreconditionBuilder = {
      stubFor(get(urlPathEqualTo(s"/incorporation-information/000-434-1/company-profile"))
        .willReturn(ok(s"""{ "company_name": "testCompanyName" }""")
        )
      )

      stubFor(get(urlPathEqualTo(s"/vatreg/1/status"))
        .willReturn(ok(s"""{ "status": "${status.toString}" }""")
        )
      )

      stubFor(get(urlPathEqualTo("/vatreg/incorporation-information/000-434-1")).willReturn(ok(incorpInfo)))
      stubFor(get(urlPathEqualTo("/business-registration/business-tax-registration")).willReturn(ok(businessProfile)))
      CorporationTaxRegistrationStub().existsWithStatus("held")

      val currentProfile = """
                             |{
                             | "companyName" : "testCompanyName",
                             | "registrationID" : "1",
                             | "transactionID" : "000-434-1",
                             | "vatRegistrationStatus" : "${VatRegStatus.draft}"
                             |}
                           """.stripMargin

      (currentState, nextState) match {
        case (None, None) => putKeyStoreValue(CacheKeys.CurrentProfile, currentProfile)
        case _ => putKeyStoreValue(CacheKeys.CurrentProfile, currentProfile, currentState, nextState)
      }

      builder
    }

    def withProfileAndIncorpDate(currentState: Option[String] = None, nextState: Option[String] = None) = withProfileInclIncorp(true, currentState, nextState)
    def withProfile(currentState: Option[String] = None, nextState: Option[String] = None) = withProfileInclIncorp(false, currentState, nextState)

    private val withProfileInclIncorp = (withIncorporationDate: Boolean, currentState: Option[String], nextState: Option[String]) => {
      val incorporationDate = Json.parse("""{"incorporationDate": "2016-08-05"}""").as[JsObject]
      val js = Json.parse(s"""
        |{
        | "companyName" : "testCompanyName",
        | "registrationID" : "1",
        | "transactionID" : "000-434-1",
        | "vatRegistrationStatus" : "${VatRegStatus.draft}"
        |}
        """.stripMargin).as[JsObject]

      val currentProfile = if(withIncorporationDate) js.deepMerge(incorporationDate) else js

      (currentState, nextState) match {
        case (None, None) => hasKeyStoreValue(CacheKeys.CurrentProfile, currentProfile.toString)
        case _ => hasKeyStoreValue(CacheKeys.CurrentProfile, currentProfile.toString, currentState, nextState)
      }

      builder
    }
  }

  case class VatSchemeStub()(implicit builder: PreconditionBuilder) extends KeystoreStub {

    def hasValidEligibilityData:PreconditionBuilder = {
      stubFor(
        get(urlPathEqualTo("/vatreg/1/get-scheme"))
          .willReturn(ok(
            s"""{ "registrationId" : "1", "status" : "draft",
               | "vatEligibility" : {
               |        "haveNino" : true,
               |        "doingBusinessAbroad" : false,
               |        "doAnyApplyToYou" : false,
               |        "applyingForAnyOf" : false,
               |        "applyingForVatExemption" : false,
               |        "companyWillDoAnyOf" : false,
               |        "vatEligibilityChoice" : {
               |          "necessity" : "voluntary"
               |        }
               |    }}""".stripMargin
          )))
      builder
    }

    def hasServiceEligibilityDataApartFromLastQuestion:PreconditionBuilder = {
      stubFor(
        get(urlPathEqualTo("/vatreg/1/service-eligibility"))
          .willReturn(ok(
            s"""{ "registrationId" : "1",
               | "vatEligibility" : {
               |        "haveNino" : true,
               |        "doingBusinessAbroad" : false,
               |        "doAnyApplyToYou" : false,
               |        "applyingForAnyOf" : false,
               |        "applyingForVatExemption" : false
               |    }}""".stripMargin
          )))
      builder
    }

    def isBlank: PreconditionBuilder = {
      stubFor(
        get(urlPathEqualTo("/vatreg/1/get-scheme"))
          .willReturn(ok(
            s"""{ "registrationId" : "1" , "status" : "draft" }"""
          )))
      builder
    }

    def isUpdatedWith[T](t: T)(implicit tFmt: Format[T]) = {
      stubFor(
        patch(urlPathMatching(s"/vatreg/1/.*"))
          .willReturn(aResponse().withStatus(202).withBody(tFmt.writes(t).toString())))
      builder
    }

    def deleted: PreconditionBuilder = {
      stubFor(delete(urlPathEqualTo("/vatreg/1/delete-scheme")).willReturn(ok("")))
      builder
    }
  }


  case class VatRegistrationFootprintStub
  ()
  (implicit builder: PreconditionBuilder) extends KeystoreStub {

    def exists: PreconditionBuilder = {
      stubFor(
        post(urlPathEqualTo("/vatreg/new"))
          .willReturn(ok(
            s"""{ "registrationId" : "1" }"""
          )))

      builder
    }

    def fails: PreconditionBuilder = {
      stubFor(
        post(urlPathEqualTo("/vatreg/new"))
          .willReturn(serverError()))

      builder
    }
  }

  case class UserStub
  ()
  (implicit builder: PreconditionBuilder) extends SessionBuilder {

    def isAuthorised(implicit requestHolder: RequestHolder): PreconditionBuilder = {
      requestHolder.request = requestWithSession(requestHolder.request, "anyUserId")
      stubFor(
        get(urlPathEqualTo("/auth/authority"))
          .willReturn(ok(
            s"""
               |{
               |  "uri":"anyUserId",
               |  "loggedInAt": "2014-06-09T14:57:09.522Z",
               |  "previouslyLoggedInAt": "2014-06-09T14:48:24.841Z",
               |  "credentials": {"gatewayId":"xxx2"},
               |  "accounts": {},
               |  "levelOfAssurance": "2",
               |  "confidenceLevel" : 50,
               |  "credentialStrength": "strong",
               |  "legacyOid": "1234567890",
               |  "userDetailsLink": "http://localhost:11111/auth/userDetails",
               |  "ids": "/auth/ids"
               |}""".stripMargin
          )))
      builder
    }

    def isNotAuthorised  = {
      stubFor(
        get(urlPathEqualTo("/auth/authority"))
          .willReturn(forbidden()))
      builder
     }
  }



  case class JourneyStub
  (journeyId: String)
  (implicit builder: PreconditionBuilder) {

    val journeyInitUrl: UrlPathPattern = urlPathMatching(s".*/api/init/$journeyId")

    def initialisedSuccessfully(): PreconditionBuilder = {
      stubFor(post(journeyInitUrl).willReturn(aResponse.withStatus(202).withHeader("Location", "continueUrl")))
      builder
    }

    def notInitialisedAsExpected(): PreconditionBuilder = {
      stubFor(post(journeyInitUrl).willReturn(aResponse().withStatus(202))) // a 202 _without_ Location header
      builder
    }

    def failedToInitialise(): PreconditionBuilder = {
      stubFor(post(journeyInitUrl).willReturn(serverError()))
      builder
    }

  }

  case class AddressStub
  (id: String, line1: String, line2: String, country: String, postcode: String)
  (implicit builder: PreconditionBuilder) {

    val confirmedAddressPath = s""".*/api/confirmed[?]id=$id"""

    def isFound: PreconditionBuilder = {
      stubFor(
        get(urlMatching(confirmedAddressPath))
          .willReturn(ok(
            s"""
               |{
               |  "auditRef": "$id",
               |  "id": "GB990091234520",
               |  "address": {
               |    "country": {
               |      "code": "GB",
               |      "name": "$country"
               |    },
               |    "lines": [
               |      "$line1",
               |      "$line2"
               |    ],
               |    "postcode": "$postcode"
               |  }
               |}
         """.stripMargin
          )))
      builder
    }

    def isNotFound: PreconditionBuilder = {
      stubFor(
        get(urlMatching(confirmedAddressPath))
          .willReturn(notFound()))
      builder
    }
  }
  case class AuditStub()
  (implicit builder: PreconditionBuilder) {
    def writesAudit(status:Int =200) = {
      stubFor(post(urlMatching("/write/audit"))
        .willReturn(
          aResponse().
            withStatus(status).
            withBody("""{"x":2}""")
        )
      )
      builder
    }
    def failsToWriteAudit() = {
      writesAudit(404)
    }
  }
}
