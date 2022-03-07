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

package v2.models.audit

import api.models.audit.{AuditError, AuditResponse}
import play.api.libs.json.Json
import support.UnitSpec
import v2.models.domain.TypeOfClaim

class AmendLossClaimsOrderAuditDetailSpec extends UnitSpec {

  val nino        = "ZG903729C"
  val wrongNino   = "XX751130C"
  val taxYear     = Some("2018-19")
  val typeOfClaim = TypeOfClaim.`carry-forward`
  def uri: String = s"/$nino/brought-forward-losses"

  "writes" must {
    "work" when {
      "success response" in {
        val json = Json.parse(s"""{
                                 |    "userType": "Agent",
                                 |    "agentReferenceNumber":"012345678",
                                 |    "nino": "$nino",
                                 |    "taxYear": "2018-19",
                                 |    "request": {
                                 |     "claimType": "carry-sideways",
                                 |     "listOfLossClaims": [
                                 |      {
                                 |      "id": "123456789ABCDE",
                                 |      "sequence":2
                                 |      },
                                 |      {
                                 |      "id": "123456789ABDE0",
                                 |      "sequence":3
                                 |      },
                                 |      {
                                 |      "id": "123456789ABEF1",
                                 |      "sequence":1
                                 |      }
                                 |     ]
                                 |    },
                                 |    "X-CorrelationId": "a1e8057e-fbbc-47a8-a8b478d9f015c253",
                                 |    "response": {
                                 |      "httpStatus": 200,
                                 |      "body": {
                                 |    "links": [
                                 |      {
                                 |        "href": "/individuals/losses/$nino/loss-claims/order",
                                 |        "method": "PUT",
                                 |        "rel": "amend-loss-claim-order"
                                 |      },
                                 |      {
                                 |        "href": "/individuals/losses/$nino/loss-claims",
                                 |        "method": "GET",
                                 |        "rel": "self"
                                 |      }
                                 |    ]
                                 |    }
                                 |  }
                                 |}""".stripMargin)

        Json.toJson(
          AmendLossClaimsOrderAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = nino,
            taxYear = taxYear,
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            request = Json.parse(s"""{
                | "claimType": "carry-sideways",
                | "listOfLossClaims": [
                |      {
                |      "id": "123456789ABCDE",
                |      "sequence":2
                |      },
                |      {
                |      "id": "123456789ABDE0",
                |      "sequence":3
                |      },
                |      {
                |      "id": "123456789ABEF1",
                |      "sequence":1
                |      }
                |   ]
                | }
                |""".stripMargin),
            response = AuditResponse(
              200,
              Right(Some(Json.parse(s"""{
                                       |    "links": [
                                       |    {
                                       |      "href": "/individuals/losses/$nino/loss-claims/order",
                                       |      "method": "PUT",
                                       |      "rel": "amend-loss-claim-order"
                                       |    },
                                       |    {
                                       |      "href": "/individuals/losses/$nino/loss-claims",
                                       |      "method": "GET",
                                       |      "rel": "self"
                                       |    }
                                       |  ]
                                       |}""".stripMargin)))
            )
          )) shouldBe json
      }

      "response with errors" in {
        val json = Json.parse(s"""{
                                 |  "userType": "Individual",
                                 |  "nino": "$wrongNino",
                                 |  "taxYear": "2018-19",
                                 |  "request": {
                                 |     "claimType": "carry-sideways",
                                 |     "listOfLossClaims": [
                                 |      {
                                 |      "id": "123456789ABCDE",
                                 |      "sequence":2
                                 |      },
                                 |      {
                                 |      "id": "123456789ABDE0",
                                 |      "sequence":3
                                 |      },
                                 |      {
                                 |      "id": "123456789ABEF1",
                                 |      "sequence":1
                                 |      }
                                 |     ]
                                 |    },
                                 |  "X-CorrelationId": "a1e8057e-fbbc-47a8-a8b478d9f015c253",
                                 |  "response": {
                                 |      "httpStatus": 400,
                                 |  "errors": [
                                 |      {
                                 |        "errorCode": "FORMAT_NINO"
                                 |      }]
                                 |    }
                                 |}""".stripMargin)

        Json.toJson(
          AmendLossClaimsOrderAuditDetail(
            userType = "Individual",
            agentReferenceNumber = None,
            nino = wrongNino,
            taxYear = taxYear,
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            request = Json.parse(s"""{
                 | "claimType": "carry-sideways",
                 | "listOfLossClaims": [
                 |      {
                 |      "id": "123456789ABCDE",
                 |      "sequence":2
                 |      },
                 |      {
                 |      "id": "123456789ABDE0",
                 |      "sequence":3
                 |      },
                 |      {
                 |      "id": "123456789ABEF1",
                 |      "sequence":1
                 |      }
                 |   ]
                 | }
                 |""".stripMargin),
            response = AuditResponse(
              400,
              Left(Seq(AuditError("FORMAT_NINO")))
            )
          )) shouldBe json
      }
    }
  }
}
