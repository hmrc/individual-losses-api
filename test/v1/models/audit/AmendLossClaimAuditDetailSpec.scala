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

class AmendLossClaimAuditDetailSpec extends UnitSpec {

  val nino    = "ZG903729C"
  val claimId = "claimId"

  "writes" must {
    "work" when {
      "success response" in {
        val json = Json.parse(s"""{
            |    "userType": "Agent",
            |    "agentReferenceNumber":"012345678",
            |    "nino": "$nino",
            |    "claimId" : "$claimId",
            |    "request": {
            |      "typeOfClaim":"carry-forward"
            |    },
            |    "response":{
            |      "httpStatus": 201,
            |      "body":{
            |        "typeOfLoss": "self-employment",
            |        "selfEmploymentId": "X2IS12356589871",
            |        "typeOfClaim": "carry-forward",
            |        "taxYear": "2019-20",
            |        "lastModified": "2020-07-13T12:13:48.763Z",
            |        "links": [{
            |          "href": "/individuals/losses/$nino/loss-claims/$claimId",
            |          "method": "GET",
            |          "rel": "self"
            |        }
            |        ]
            |     }
            |    },
            |    "X-CorrelationId": "a1e8057e-fbbc-47a8-a8b478d9f015c253"
            |}""".stripMargin)

        Json.toJson(
          AmendLossClaimAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = nino,
            claimId = claimId,
            request = Json.parse("""{
                      |  "typeOfClaim": "carry-forward"
                      |}""".stripMargin),
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            response = AuditResponse(
              201,
              errors = None,
              body = Some(Json.parse(s"""{
                          |        "typeOfLoss": "self-employment",
                          |        "selfEmploymentId": "X2IS12356589871",
                          |        "typeOfClaim": "carry-forward",
                          |        "taxYear": "2019-20",
                          |        "lastModified": "2020-07-13T12:13:48.763Z",
                          |        "links": [{
                          |          "href": "/individuals/losses/$nino/loss-claims/$claimId",
                          |          "method": "GET",
                          |          "rel": "self"
                          |        }
                          |        ]
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
            |    "claimId" : "$claimId",
            |    "request": {
            |      "typeOfClaim":"carry-forward"
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
          AmendLossClaimAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = nino,
            claimId = claimId,
            request = Json.parse("""{
                      |      "typeOfClaim":"carry-forward"
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
