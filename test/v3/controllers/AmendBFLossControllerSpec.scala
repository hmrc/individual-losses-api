/*
 * Copyright 2023 HM Revenue & Customs
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

package v3.controllers

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.hateoas.MockHateoasFactory
import api.models.ResponseWrapper
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.{Nino, Timestamp}
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{HateoasWrapper, Link}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import v3.controllers.requestParsers.MockAmendBFLossRequestParser
import v3.models.request
import v3.models.domain.bfLoss.TypeOfLoss
import v3.models.request.amendBFLosses.{AmendBFLossRawData, AmendBFLossRequestBody}
import v3.models.response.amendBFLosses.{AmendBFLossHateoasData, AmendBFLossResponse}
import v3.services.MockAmendBFLossService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendBFLossControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockAmendBFLossService
    with MockAmendBFLossRequestParser
    with MockHateoasFactory {

  private val lossId      = "AAZZ1234567890a"
  private val lossAmount  = BigDecimal(2345.67)
  private val amendBFLoss = AmendBFLossRequestBody(lossAmount)

  private val amendBFLossResponse = AmendBFLossResponse(
    businessId = "XBIS12345678910",
    typeOfLoss = TypeOfLoss.`self-employment`,
    lossAmount = lossAmount,
    taxYearBroughtForwardFrom = "2021-22",
    lastModified = Timestamp("2022-07-13T12:13:48.763Z")
  )

  private val testHateoasLink = Link(href = "/individuals/losses/TC663795B/brought-forward-losses/AAZZ1234567890a", method = GET, rel = "self")
  private val bfLossRequest   = request.amendBFLosses.AmendBFLossRequest(Nino(nino), lossId, amendBFLoss)

  private val mtdResponseJson = Json.parse(
    s"""
      |{
      |    "businessId": "XBIS12345678910",
      |    "typeOfLoss": "self-employment",
      |    "lossAmount": $lossAmount,
      |    "taxYearBroughtForwardFrom": "2021-22",
      |    "lastModified": "2022-07-13T12:13:48.763Z",
      |    "links" : [
      |     {
      |       "href": "/individuals/losses/TC663795B/brought-forward-losses/AAZZ1234567890a",
      |        "rel": "self",
      |        "method": "GET"
      |     }
      |  ]
      |}
    """.stripMargin
  )

  private val requestBody: JsValue = Json.parse(
    s"""
      |{
      |  "lossAmount": $lossAmount
      |}
    """.stripMargin
  )

  "amend" should {
    "return OK" when {
      "the request is valid" in new Test {
        MockAmendBFLossRequestDataParser
          .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
          .returns(Right(bfLossRequest))

        MockAmendBFLossService
          .amend(request.amendBFLosses.AmendBFLossRequest(Nino(nino), lossId, amendBFLoss))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, amendBFLossResponse))))

        MockHateoasFactory
          .wrap(amendBFLossResponse, AmendBFLossHateoasData(nino, lossId))
          .returns(HateoasWrapper(amendBFLossResponse, Seq(testHateoasLink)))

        runOkTestWithAudit(
          expectedStatus = OK,
          maybeExpectedResponseBody = Some(mtdResponseJson),
          maybeAuditRequestBody = Some(requestBody),
          maybeAuditResponseBody = Some(mtdResponseJson)
        )
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        MockAmendBFLossRequestDataParser
          .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))

        runErrorTestWithAudit(NinoFormatError, maybeAuditRequestBody = Some(requestBody))
      }

      "the service returns an error" in new Test {
        MockAmendBFLossRequestDataParser
          .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
          .returns(Right(bfLossRequest))

        MockAmendBFLossService
          .amend(request.amendBFLosses.AmendBFLossRequest(Nino(nino), lossId, amendBFLoss))
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleLossAmountNotChanged))))

        runErrorTestWithAudit(RuleLossAmountNotChanged, maybeAuditRequestBody = Some(requestBody))
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking {

    private val controller = new AmendBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockAmendBFLossService,
      parser = mockAmendBFLossRequestDataParser,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.amend(nino, lossId)(fakePostRequest(requestBody))

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "AmendBroughtForwardLoss",
        transactionName = "amend-brought-forward-loss",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = "3.0",
          params = Map("nino" -> nino, "lossId" -> lossId),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

  }

}
