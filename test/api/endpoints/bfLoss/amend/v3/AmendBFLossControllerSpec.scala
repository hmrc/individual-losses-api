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

package api.endpoints.bfLoss.amend.v3

import api.controllers.{ ControllerBaseSpec, ControllerTestRunner }
import api.endpoints.bfLoss.amend.anyVersion.request.{ AmendBFLossRawData, AmendBFLossRequestBody }
import api.endpoints.bfLoss.amend.anyVersion.response.AmendBFLossHateoasData
import api.endpoints.bfLoss.amend.v3.request.{ AmendBFLossRequest, MockAmendBFLossParser }
import api.endpoints.bfLoss.amend.v3.response.AmendBFLossResponse
import api.endpoints.bfLoss.domain.v3.TypeOfLoss
import api.hateoas.MockHateoasFactory
import api.models.ResponseWrapper
import api.models.audit.{ AuditEvent, AuditResponse, GenericAuditDetail }
import api.models.domain.Nino
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{ HateoasWrapper, Link }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, Result }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendBFLossControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockAmendBFLossService
    with MockAmendBFLossParser
    with MockHateoasFactory {

  private val lossId      = "AAZZ1234567890a"
  private val lossAmount  = BigDecimal(2345.67)
  private val amendBFLoss = AmendBFLossRequestBody(lossAmount)

  private val amendBFLossResponse = AmendBFLossResponse(
    businessId = "XBIS12345678910",
    typeOfLoss = TypeOfLoss.`self-employment`,
    lossAmount = lossAmount,
    taxYearBroughtForwardFrom = "2021-22",
    lastModified = "2022-07-13T12:13:48.763Z"
  )

  private val testHateoasLink = Link(href = "/individuals/losses/TC663795B/brought-forward-losses/AAZZ1234567890a", method = GET, rel = "self")
  private val bfLossRequest   = AmendBFLossRequest(Nino(nino), lossId, amendBFLoss)

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
      "the request is valid" in new RunControllerTest {

        protected def setupMocks(): Unit = {
          MockAmendBFLossRequestDataParser
            .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
            .returns(Right(bfLossRequest))

          MockAmendBFLossService
            .amend(AmendBFLossRequest(Nino(nino), lossId, amendBFLoss))
            .returns(Future.successful(Right(ResponseWrapper(correlationId, amendBFLossResponse))))

          MockHateoasFactory
            .wrap(amendBFLossResponse, AmendBFLossHateoasData(nino, lossId))
            .returns(HateoasWrapper(amendBFLossResponse, Seq(testHateoasLink)))
        }

        runOkTestWithAudit(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponseJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new RunControllerTest {

        protected def setupMocks(): Unit = {
          MockAmendBFLossRequestDataParser
            .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
            .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))
        }

        runErrorTestWithAudit(NinoFormatError)
      }

      "the service returns an error" in new RunControllerTest {

        protected def setupMocks(): Unit = {
          MockAmendBFLossRequestDataParser
            .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
            .returns(Right(bfLossRequest))

          MockAmendBFLossService
            .amend(AmendBFLossRequest(Nino(nino), lossId, amendBFLoss))
            .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleLossAmountNotChanged))))
        }

        runErrorTestWithAudit(RuleLossAmountNotChanged)
      }
    }
  }

  private trait RunControllerTest extends RunTest with AuditEventChecking {

    private val controller = new AmendBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      amendBFLossService = mockAmendBFLossService,
      amendBFLossParser = mockAmendBFLossRequestDataParser,
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
          request = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          response = auditResponse
        )
      )
  }
}
