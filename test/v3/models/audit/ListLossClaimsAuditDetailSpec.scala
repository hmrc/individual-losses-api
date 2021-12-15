/*
 * Copyright 2021 HM Revenue & Customs
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

package v3.models.audit

import play.api.libs.json.Json
import support.UnitSpec

class ListLossClaimsAuditDetailSpec extends UnitSpec {


  val nino = "ZG903729C"
  val wrongNino = "XX751130C"
  def uri: String = s"/$nino/loss-claims"

  "writes" must {
    "work" when {
      "success response" in {
        val json = Json.parse(s"""{
                                 |  "userType": "Agent",
                                 |  "agentReferenceNumber": "012345678",
                                 |  "nino": "$nino",
                                 |  "X-CorrelationId": "a1e8057e-fbbc-47a8-a8b478d9f015c253",
                                 |  "response": {
                                 |    "httpStatus": 200,
                                 |    "body": {
                                 |      "claims": [
                                 |        {
                                 |          "id": "000000000000011",
                                 |          "sequence": 1,
                                 |          "typeOfClaim": "carry-forward",
                                 |          "links": [
                                 |            {
                                 |              "href": "/individuals/losses$uri/000000000000011",
                                 |              "rel": "self",
                                 |              "method": "GET"
                                 |            }
                                 |          ]
                                 |        },
                                 |        {
                                 |          "id": "000000000000022",
                                 |          "sequence": 2,
                                 |          "typeOfClaim": "carry-forward",
                                 |          "links": [
                                 |            {
                                 |              "href": "/individuals/losses$uri/000000000000022",
                                 |              "rel": "self",
                                 |              "method": "GET"
                                 |            }
                                 |          ]
                                 |        }
                                 |      ],
                                 |      "links": [
                                 |        {
                                 |          "href": "/individuals/losses$uri",
                                 |          "rel": "self",
                                 |          "method": "GET"
                                 |        },
                                 |        {
                                 |          "href": "/individuals/losses$uri",
                                 |          "rel": "create-loss-claim",
                                 |          "method": "POST"
                                 |        },
                                 |        {
                                 |          "href": "/individuals/losses$uri/order",
                                 |          "rel": "amend-loss-claim-order",
                                 |          "method": "PUT"
                                 |        }
                                 |      ]
                                 |    }
                                 |  }
                                 |}""".stripMargin)

        Json.toJson(
          ListLossClaimsAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = nino,
            taxYear = None,
            typeOfLoss = None,
            businessId = None,
            claimType = None,
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            response = AuditResponse(
              200,
              Right(Some(Json.parse(s"""{
                                       |  "claims": [
                                       |    {
                                       |      "id": "000000000000011",
                                       |      "sequence": 1,
                                       |      "typeOfClaim": "carry-forward",
                                       |      "links": [
                                       |        {
                                       |          "href": "/individuals/losses$uri/000000000000011",
                                       |          "rel": "self",
                                       |          "method": "GET"
                                       |        }
                                       |      ]
                                       |    },
                                       |    {
                                       |      "id": "000000000000022",
                                       |      "sequence": 2,
                                       |      "typeOfClaim": "carry-forward",
                                       |      "links": [
                                       |        {
                                       |          "href": "/individuals/losses$uri/000000000000022",
                                       |          "rel": "self",
                                       |          "method": "GET"
                                       |        }
                                       |      ]
                                       |    }
                                       |  ],
                                       |  "links": [
                                       |    {
                                       |      "href": "/individuals/losses$uri",
                                       |      "rel": "self",
                                       |      "method": "GET"
                                       |    },
                                       |    {
                                       |      "href": "/individuals/losses$uri",
                                       |      "rel": "create-loss-claim",
                                       |      "method": "POST"
                                       |    },
                                       |    {
                                       |      "href": "/individuals/losses$uri/order",
                                       |      "rel": "amend-loss-claim-order",
                                       |      "method": "PUT"
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
          ListLossClaimsAuditDetail(
            userType = "Individual",
            agentReferenceNumber = None,
            nino = wrongNino,
            taxYear = None,
            typeOfLoss = None,
            businessId = None,
            claimType = None,
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            response = AuditResponse(
              400,
              Left(Seq(AuditError("FORMAT_NINO")))
            )
          )) shouldBe json
      }
    }
  }
}
