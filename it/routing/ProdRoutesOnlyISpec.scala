/*
 * Copyright 2020 HM Revenue & Customs
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

package routing

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.models.domain.{Claim, TypeOfClaim}
import v1.stubs.{AuditStub, AuthStub, MtdIdLookupStub}

class ProdRoutesOnlyISpec extends IntegrationBaseSpec {

  override def servicesConfig: Map[String, String] = Map(
    "microservice.services.des.host" -> mockHost,
    "microservice.services.des.port" -> mockPort,
    "microservice.services.mtd-id-lookup.host" -> mockHost,
    "microservice.services.mtd-id-lookup.port" -> mockPort,
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "auditing.consumer.baseUri.port" -> mockPort,
    "feature-switch.amend-loss-claim-order.enabled" -> "false"
  )

  "hitting the amend loss claims order endpoint" should {

    "return 404" when {
      "the feature switch is off" in {

        val nino = "AA123456A"
        val taxYear = "2019-20"

        val claim1 = Claim("1234567890ABEF1", 1)
        val claim2 = Claim("1234567890ABCDE", 2)
        val claim3 = Claim("1234567890ABDE0", 3)
        val claimSeq = Seq(claim2, claim1, claim3)

        def requestJson(
                         claimType: String = TypeOfClaim.`carry-sideways`.toString,
                         listOfLossClaims: Seq[Claim] = claimSeq
                       ): JsValue =
          Json.parse(s"""
            |{
            |   "claimType": "$claimType",
            |   "listOfLossClaims": ${Json.toJson(listOfLossClaims)}
            |}
            """.stripMargin)

        def uri: String = s"/$nino/loss-claims/order"

        def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        def request(): WSRequest = {
          setupStubs()
          buildRequest(uri)
            .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
        }

        val response: WSResponse = await(request().withQueryStringParameters("taxYear" -> taxYear).put(requestJson()))
        response.status shouldBe Status.NOT_FOUND
      }
    }
  }

}
