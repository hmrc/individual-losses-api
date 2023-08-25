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
import api.models.domain.Nino
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{HateoasWrapper, Link}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import v3.controllers.requestParsers.MockCreateBFLossRequestParser
import v3.models.domain.bfLoss.TypeOfLoss
import v3.models.request.createBFLosses.{CreateBFLossRawData, CreateBFLossRequest, CreateBFLossRequestBody}
import v3.models.response.createBFLosses.{CreateBFLossHateoasData, CreateBFLossResponse}
import v3.services.MockCreateBFLossService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateBFLossControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockCreateBFLossService
    with MockCreateBFLossRequestParser
    with MockHateoasFactory {

  private val lossId = "AAZZ1234567890a"

  private val bfLoss               = CreateBFLossRequestBody(TypeOfLoss.`self-employment`, "XKIS00000000988", "2019-20", 256.78)
  private val createBFLossResponse = CreateBFLossResponse("AAZZ1234567890a")
  private val testHateoasLink      = Link(href = "/foo/bar", method = GET, rel = "test-relationship")
  private val bfLossRequest        = CreateBFLossRequest(Nino(nino), bfLoss)

  private val requestBody: JsValue = Json.parse(
    """
      |{
      |    "businessId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYearBroughtForwardFrom": "2019-20",
      |    "lossAmount": 256.78
      |}
    """.stripMargin
  )

  private val mtdResponseJson: JsValue = Json.parse(
    """
      |{
      |  "lossId": "AAZZ1234567890a",
      |  "links" : [
      |     {
      |       "href": "/foo/bar",
      |       "method": "GET",
      |       "rel": "test-relationship"
      |     }
      |  ]
      |}
    """.stripMargin
  )

  "create" should {
    "return Created" when {
      "the request is valid" in new Test {
        MockCreateBFLossRequestDataParser
          .parseRequest(CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Right(bfLossRequest))

        MockCreateBFLossService
          .create(CreateBFLossRequest(Nino(nino), bfLoss))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, createBFLossResponse))))

        MockHateoasFactory
          .wrap(createBFLossResponse, CreateBFLossHateoasData(nino, lossId))
          .returns(HateoasWrapper(createBFLossResponse, Seq(testHateoasLink)))

        runOkTestWithAudit(
          expectedStatus = CREATED,
          maybeAuditRequestBody = Some(requestBody),
          maybeExpectedResponseBody = Some(mtdResponseJson),
          maybeAuditResponseBody = Some(mtdResponseJson)
        )
      }
    }
  }

  "return the error as per spec" when {
    "the parser validation fails" in new Test {
      MockCreateBFLossRequestDataParser
        .parseRequest(CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
        .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))

      runErrorTestWithAudit(NinoFormatError, maybeAuditRequestBody = Some(requestBody))
    }

    "the service returns an error" in new Test {
      MockCreateBFLossRequestDataParser
        .parseRequest(CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
        .returns(Right(bfLossRequest))

      MockCreateBFLossService
        .create(CreateBFLossRequest(Nino(nino), bfLoss))
        .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleDuplicateSubmissionError, None))))

      runErrorTestWithAudit(RuleDuplicateSubmissionError, maybeAuditRequestBody = Some(requestBody))
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking {

    private val controller = new CreateBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockCreateBFLossService,
      parser = mockCreateBFLossParser,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.create(nino)(fakePostRequest(requestBody))

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateBroughtForwardLoss",
        transactionName = "create-brought-forward-loss",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = "3.0",
          params = Map("nino" -> nino),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

  }

}
