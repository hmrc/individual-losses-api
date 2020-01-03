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
import v1.models.errors.RuleTypeOfClaimInvalid

class CreateLossClaimAuditDetailSpec extends UnitSpec {

  val nino    = "ZG903729C"
  val claimId = "claimId"

  "writes" must {
    "work" when {
      "success response" in {
        val json = Json.parse(s"""{
            |    "userType": "Agent",
            |    "agentReferenceNumber":"012345678",
            |    "nino": "$nino",
            |    "request": {
            |      "typeOfLoss":"self-employment",
            |      "selfEmploymentId":"X2IS12356589871",
            |      "typeOfClaim":"carry-forward",
            |      "taxYear": "2019-20"
            |    },
            |    "response":{
            |      "httpStatus": 201,
            |      "body":{
            |        "id": "$claimId",
            |        "links":[
            |        {
            |            "href":"/individuals/losses/{$nino}/loss-claims/{$claimId}",
            |            "rel":"self",
            |            "method":"GET"
            |        },
            |       {
            |            "href":"/individuals/losses/{$nino}/loss-claims/{$claimId}",
            |            "rel":"delete-loss-claim",
            |            "method":"DELETE"
            |        },
            |       {
            |            "href":"/individuals/losses/{$nino}/loss-claims/{$claimId}/change-type-of-claim",
            |            "rel":"amend-loss-claim",
            |            "method":"POST"
            |        }
            |      ]
            |     }
            |    },
            |    "X-CorrelationId": "a1e8057e-fbbc-47a8-a8b478d9f015c253"
            |}""".stripMargin)

        Json.toJson(
          CreateLossClaimAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = nino,
            request = Json.parse("""{
                      |      "typeOfLoss":"self-employment",
                      |      "selfEmploymentId":"X2IS12356589871",
                      |      "typeOfClaim":"carry-forward",
                      |      "taxYear": "2019-20"
                      |    }""".stripMargin),
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            response = AuditResponse(
              201,
              errors = None,
              body = Some(Json.parse(s"""{
                          |     "id": "$claimId",
                          |     "links":[
                          |      {
                          |         "href":"/individuals/losses/{$nino}/loss-claims/{$claimId}",
                          |          "rel":"self",
                          |          "method":"GET"
                          |      },
                          |      {
                          |         "href":"/individuals/losses/{$nino}/loss-claims/{$claimId}",
                          |          "rel":"delete-loss-claim",
                          |          "method":"DELETE"
                          |      },
                          |      {
                          |          "href":"/individuals/losses/{$nino}/loss-claims/{$claimId}/change-type-of-claim",
                          |           "rel":"amend-loss-claim",
                          |           "method":"POST"
                          |      }
                          |      ]
                          |}""".stripMargin))
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
            |    "request": {
            |      "typeOfLoss":"self-employment",
            |      "selfEmploymentId":"X2IS12356589871",
            |      "typeOfClaim":"carry-forward",
            |      "taxYear": "2019-20"
            |    },
            |    "response": {
            |      "httpStatus": 403,
            |      "errors": [
            |        {
            |          "errorCode":"RULE_TYPE_OF_CLAIM_INVALID"
            |        }
            |      ]
            |    },
            |    "X-CorrelationId":"a1e8057e-fbbc-47a8-a8b478d9f015c253"
            |  }
            |""".stripMargin)

        Json.toJson(
          CreateLossClaimAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = nino,
            request = Json.parse("""{
                      |      "typeOfLoss":"self-employment",
                      |      "selfEmploymentId":"X2IS12356589871",
                      |      "typeOfClaim":"carry-forward",
                      |      "taxYear": "2019-20"
                      |    }""".stripMargin),
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            response = AuditResponse(
              403,
              errors = Some(Seq(AuditError(RuleTypeOfClaimInvalid.code))),
              body = None
            )
          )) shouldBe json
      }
    }
  }
}
