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
import v1.models.errors.RuleInvalidLossAmount

class CreateBFLossAuditDetailSpec extends UnitSpec {

  val nino    = "ZG903729C"
  val lossId = "lossId"

  "writes" must {
    "work" when {
      "success response" in {
        val json = Json.parse(s"""{
            |    "userType": "Agent",
            |    "agentReferenceNumber":"012345678",
            |    "nino": "$nino",
            |    "request": {
            |      "selfEmploymentId":"X2IS12356589871",
            |      "typeOfLoss":"self-employment-class4",
            |      "taxYear": "2019-20",
            |      "lossAmount": 100
            |    },
            |    "response":{
            |      "httpStatus": 201,
            |      "body":{
            |        "id": "$lossId",
            |        "links":[
            |        {
            |            "href":"/individuals/losses/{$nino}/brought-forward-losses/{$lossId}",
            |            "rel":"self",
            |            "method":"GET"
            |        },
            |       {
            |            "href":"/individuals/losses/{$nino}/brought-forward-losses/{$lossId}",
            |            "rel":"delete-brought-forward-loss",
            |            "method":"DELETE"
            |        },
            |       {
            |            "href":"/individuals/losses/{$nino}/brought-forward-losses/{$lossId}/change-loss-amount",
            |            "rel":"amend-brought-forward-loss",
            |            "method":"POST"
            |        }
            |      ]
            |     }
            |    },
            |    "X-CorrelationId": "a1e8057e-fbbc-47a8-a8b478d9f015c253"
            |}""".stripMargin)

        Json.toJson(
          CreateBFLossAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = nino,
            request = Json.parse("""{
                      |      "selfEmploymentId":"X2IS12356589871",
                      |      "typeOfLoss":"self-employment-class4",
                      |      "taxYear": "2019-20",
                      |      "lossAmount": 100
                      |    }""".stripMargin),
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            response = AuditResponse(
              201,
              errors = None,
              body = Some(Json.parse(s"""{
                          |     "id": "$lossId",
                          |     "links":[
                          |        {
                          |            "href":"/individuals/losses/{$nino}/brought-forward-losses/{$lossId}",
                          |            "rel":"self",
                          |            "method":"GET"
                          |        },
                          |       {
                          |            "href":"/individuals/losses/{$nino}/brought-forward-losses/{$lossId}",
                          |            "rel":"delete-brought-forward-loss",
                          |            "method":"DELETE"
                          |        },
                          |       {
                          |            "href":"/individuals/losses/{$nino}/brought-forward-losses/{$lossId}/change-loss-amount",
                          |            "rel":"amend-brought-forward-loss",
                          |            "method":"POST"
                          |        }
                          |      ]
                          |}""".stripMargin))
            )
          )) shouldBe json
      }
    }

    "work" when {
      "error response" in {
        val json = Json.parse(s"""{
            |    "userType": "Agent",
            |    "agentReferenceNumber":"012345678",
            |    "nino": "$nino",
            |    "request": {
            |      "selfEmploymentId":"X2IS12356589871",
            |      "typeOfLoss":"self-employment-class4",
            |      "taxYear": "2019-20",
            |      "lossAmount": -1
            |    },
            |    "response": {
            |      "httpStatus": 400,
            |      "errors": [
            |        {
            |          "errorCode":"RULE_LOSS_AMOUNT"
            |        }
            |      ]
            |    },
            |    "X-CorrelationId":"a1e8057e-fbbc-47a8-a8b478d9f015c253"
            |  }
            |""".stripMargin)

        Json.toJson(
          CreateBFLossAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = nino,
            request = Json.parse("""{
                      |      "selfEmploymentId":"X2IS12356589871",
                      |      "typeOfLoss":"self-employment-class4",
                      |      "taxYear": "2019-20",
                      |      "lossAmount": -1
                      |    }""".stripMargin),
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            response = AuditResponse(
              400,
              errors = Some(Seq(AuditError(RuleInvalidLossAmount.code))),
              body = None
            )
          )) shouldBe json
      }
    }
  }
}
