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

class DeleteBFLossAuditDetailSpec extends UnitSpec {

  val nino = "ZG903729C"
  val lossId = "lossId"

  "writes" must {
    "work" when {
      "success response" in {
        val json = Json.parse(s"""{
            |    "userType": "Agent",
            |    "agentReferenceNumber":"012345678",
            |    "nino": "$nino",
            |    "lossId" : "$lossId",
            |    "response":{
            |      "httpStatus": 204
            |    },
            |    "X-CorrelationId": "a1e8057e-fbbc-47a8-a8b478d9f015c253"
            |}""".stripMargin)

        Json.toJson(
          DeleteBFLossAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = nino,
            lossId = lossId,
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            response = AuditResponse(
              204,
              errors = None,
              body = None
            )
          )) shouldBe json
      }
    }

    "work" when {
      "error response" in {
        val json = Json.parse(s"""|{
             |    "userType": "Agent",
             |    "agentReferenceNumber":"012345678",
             |    "nino": "notANino",
             |    "lossId" : "$lossId",
             |    "response": {
             |      "httpStatus": 400,
             |      "errors": [
             |        {
             |          "errorCode":"FORMAT_NINO"
             |        }
             |      ]
             |    },
             |    "X-CorrelationId":"a1e8057e-fbbc-47a8-a8b478d9f015c253"
             |}""".stripMargin)

        Json.toJson(
          DeleteBFLossAuditDetail(
            userType = "Agent",
            agentReferenceNumber = Some("012345678"),
            nino = "notANino",
            lossId = lossId,
            `X-CorrelationId` = "a1e8057e-fbbc-47a8-a8b478d9f015c253",
            response = AuditResponse(
              400,
              errors = Some(Seq(AuditError(NinoFormatError.code))),
              body = None
            )
          )) shouldBe json
      }
    }
  }
}
