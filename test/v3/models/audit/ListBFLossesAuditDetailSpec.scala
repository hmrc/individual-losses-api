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

package v3.models.audit

import play.api.libs.json.Json
import support.UnitSpec

class ListBFLossesAuditDetailSpec extends UnitSpec {


  val nino = "ZG903729C"
  val wrongNino = "XX751130C"
  def uri: String = s"/$nino/brought-forward-losses"

  "writes" must {
    "work" when {
      "success response" in {
        val json = Json.parse(s"""{
                                 |    "userType": "Agent",
                                 |    "agentReferenceNumber":"012345678",
                                 |    "nino": "$nino",
                                 |    "X-CorrelationId": "a1e8057e-fbbc-47a8-a8b478d9f015c253",
                                 |    "response": {
                                 |      "httpStatus": 200,
                                 |      "body": {
                                 |        "losses": [
                                 |          {
                                 |            "id": "000000000000001",
                                 |            "links" : [
                                 |            {
                                 |               "href" : "/individuals/losses$uri/000000000000001",
                                 |               "rel": "self",
                                 |               "method": "GET"
                                 |            }]
                                 |        },
                                 |        {
                                 |            "id": "000000000000002",
                                 |             "links" : [
                                 |             {
                                 |               "href" : "/individuals/losses$uri/000000000000002",
                                 |               "rel": "self",
                                 |               "method": "GET"
                                 |             }
                                 |           ]
                                 |        }
                                 |    ],
                                 |    "links": [
                                 |      {
                                 |        "href": "/individuals/losses$uri",
                                 |        "rel": "self",
                                 |        "method": "GET"
                                 |      },
                                 |      {
                                 |        "href": "/individuals/losses$uri",
                                 |        "rel": "create-brought-forward-loss",
                                 |        "method": "POST"
                                 |      }
                                 |    ]
                                 |    }
                                 |  }
                                 |}""".stripMargin)

        Json.toJson(
          ListBFLossesAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = nino,
            taxYear = None,
            typeOfLoss = None,
            businessId = None,
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            response = AuditResponse(
              200,
              Right(Some(Json.parse(s"""{
                                       |  "losses": [
                                       |          {
                                       |            "id": "000000000000001",
                                       |            "links" : [
                                       |            {
                                       |               "href" : "/individuals/losses$uri/000000000000001",
                                       |               "rel": "self",
                                       |               "method": "GET"
                                       |            }]
                                       |        },
                                       |        {
                                       |            "id": "000000000000002",
                                       |             "links" : [
                                       |             {
                                       |               "href" : "/individuals/losses$uri/000000000000002",
                                       |               "rel": "self",
                                       |               "method": "GET"
                                       |             }
                                       |           ]
                                       |        }
                                       |    ],
                                       |    "links": [
                                       |      {
                                       |        "href": "/individuals/losses$uri",
                                       |        "rel": "self",
                                       |        "method": "GET"
                                       |      },
                                       |      {
                                       |        "href": "/individuals/losses$uri",
                                       |        "rel": "create-brought-forward-loss",
                                       |        "method": "POST"
                                       |      }
                                       |    ]
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
          ListBFLossesAuditDetail(
            userType = "Individual",
            agentReferenceNumber = None,
            nino = wrongNino,
            taxYear = None,
            typeOfLoss = None,
            businessId = None,
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
