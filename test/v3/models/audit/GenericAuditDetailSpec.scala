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

import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v3.models.errors.TaxYearFormatError

class GenericAuditDetailSpec extends UnitSpec {
  val nino: String = "XX751130C"
  val taxYear: String = "2020-21"
  val userType: String = "Agent"
  val agentReferenceNumber: Option[String] = Some("012345678")
  val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val versionNumber: String = "2.0"

  val auditDetailJsonSuccess: JsValue = Json.parse(
    s"""
       |{
       |   "userType":"$userType",
       |   "versionNumber": "2.0",
       |   "agentReferenceNumber":"${agentReferenceNumber.get}",
       |   "taxYear":"$taxYear",
       |   "nino":"$nino",
       |    "request": {
       |     "someField": true
       |   },
       |   "X-CorrelationId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c253",
       |   "response":{
       |      "httpStatus":200,
       |      "body":{
       |         "links":[
       |            {
       |             "href":"/individuals/nonsense/$nino/$taxYear",
       |             "rel":"do-the-things",
       |             "method":"PUT"
       |           }
       |         ]
       |      }
       |   }
       |}
    """.stripMargin
  )

  val auditDetailModelSuccess: GenericAuditDetail = GenericAuditDetail(
    userType = userType,
    versionNumber = "2.0",
    agentReferenceNumber = agentReferenceNumber,
    params = Map("nino" -> nino, "taxYear" -> taxYear),
    request = Some(Json.parse(
      """
        |{
        |  "someField": true
        |}
        """.stripMargin
    )),
    `X-CorrelationId` = correlationId,
    response = AuditResponse(
      OK,
      Right(Some(Json.parse(
        s"""
           |{
           |   "links":[
           |      {
           |         "href":"/individuals/nonsense/$nino/$taxYear",
           |         "rel":"do-the-things",
           |         "method":"PUT"
           |      }
           |   ]
           |}
        """.stripMargin))
      )
    )
  )

  val invalidTaxYearAuditDetailJson: JsValue = Json.parse(
    s"""
       |{
       |   "userType":"$userType",
       |   "agentReferenceNumber":"${agentReferenceNumber.get}",
       |   "versionNumber": "2.0",
       |   "taxYear":"2020-2021",
       |   "nino":"$nino",
       |   "request": {
       |     "someField": true
       |   },
       |   "X-CorrelationId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c253",
       |   "response":{
       |      "httpStatus":400,
       |      "errors":[
       |         {
       |            "errorCode":"FORMAT_TAX_YEAR"
       |         }
       |      ]
       |   }
       |}
    """.stripMargin
  )

  val invalidTaxYearAuditDetailModel: GenericAuditDetail = GenericAuditDetail(
    userType = userType,
    agentReferenceNumber = agentReferenceNumber,
    versionNumber = "2.0",
    params = Map("nino" -> nino, "taxYear" -> "2020-2021"),
    request = Some(Json.parse(
      """
        |{
        |   "someField": true
        |}
      """.stripMargin
    )),
    `X-CorrelationId` = correlationId,
    response = AuditResponse(BAD_REQUEST, Left(Seq(AuditError(TaxYearFormatError.code))))
  )

  "GenericAuditDetail" when {
    "written to JSON (success)" should {
      "produce the expected JsObject" in {
        Json.toJson(auditDetailModelSuccess) shouldBe auditDetailJsonSuccess
      }
    }

    "written to JSON (error)" should {
      "produce the expected JsObject" in {
        Json.toJson(invalidTaxYearAuditDetailModel) shouldBe invalidTaxYearAuditDetailJson
      }
    }
  }
}
