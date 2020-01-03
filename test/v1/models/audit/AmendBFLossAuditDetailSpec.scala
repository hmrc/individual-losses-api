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

package v1.models.audit

import play.api.libs.json.Json
import support.UnitSpec
import v1.models.errors.NinoFormatError

class AmendBFLossAuditDetailSpec extends UnitSpec {

  val nino   = "ZG903729C"
  val lossId = "lossId"

  "writes" must {
    "work" when {
      "success response" in {
        val json = Json.parse(s"""{
            |    "userType": "Agent",
            |    "agentReferenceNumber":"012345678",
            |    "nino": "$nino",
            |    "lossId" : "$lossId",
            |    "request": {
            |      "lossAmount": 2000.99
            |    },
            |    "response":{
            |      "httpStatus": 201,
            |      "body":{
            |        "taxYear":"2018-19",
            |        "typeOfLoss":"self-employment",
            |        "selfEmploymentId":"XGIS00000001319",
            |        "lossAmount": 99999999999.99,
            |        "lastModified":"2018-07-13T12:13:48.763Z",
            |        "links": [{
            |          "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
            |          "method": "GET",
            |          "rel": "self"
            |        }
            |        ]
            |     }
            |    },
            |    "X-CorrelationId": "a1e8057e-fbbc-47a8-a8b478d9f015c253"
            |}""".stripMargin)

        Json.toJson(
          AmendBFLossAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = nino,
            lossId = lossId,
            request = Json.parse("""{
                      |      "lossAmount": 2000.99
                      |}""".stripMargin),
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            response = AuditResponse(
              201,
              Right(Some(Json.parse(s"""{
                          |        "taxYear":"2018-19",
                          |        "typeOfLoss":"self-employment",
                          |        "selfEmploymentId":"XGIS00000001319",
                          |        "lossAmount": 99999999999.99,
                          |        "lastModified":"2018-07-13T12:13:48.763Z",
                          |        "links": [{
                          |          "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
                          |          "method": "GET",
                          |          "rel": "self"
                          |        }
                          |        ]
                          |}""".stripMargin)))
            )
          )) shouldBe json
      }
    }

    "work" when {
      "error response" in {
        val json = Json.parse(s"""
            |{
            |    "userType": "Agent",
            |    "agentReferenceNumber":"012345678",
            |    "nino": "$nino",
            |    "lossId" : "$lossId",
            |    "request": {
            |      "lossAmount": 2000.99
            |    },
            |    "response": {
            |      "httpStatus": 400,
            |      "errors": [
            |        {
            |          "errorCode":"FORMAT_NINO"
            |        }
            |      ]
            |    },
            |    "X-CorrelationId":"a1e8057e-fbbc-47a8-a8b478d9f015c253"
            |  }
            |""".stripMargin)

        Json.toJson(
          AmendBFLossAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = nino,
            lossId = lossId,
            request = Json.parse("""{
                                   |     "lossAmount": 2000.99
                                   |}""".stripMargin),
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            response = AuditResponse(400, Left(Seq(AuditError(NinoFormatError.code))))
          )) shouldBe json
      }
    }
  }
}
