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

package api.endpoints.lossClaim.amendType.v3

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.endpoints.lossClaim.amendType.v3.request.{AmendLossClaimTypeRawData, AmendLossClaimTypeRequest, AmendLossClaimTypeRequestBody, MockAmendLossClaimTypeRequestDataParser}
import api.endpoints.lossClaim.amendType.v3.response.{AmendLossClaimTypeHateoasData, AmendLossClaimTypeResponse}
import api.endpoints.lossClaim.domain.v3.{TypeOfClaim, TypeOfLoss}
import api.hateoas.MockHateoasFactory
import api.models.ResponseWrapper
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.Nino
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{HateoasWrapper, Link}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendLossClaimTypeControllerSpec
  extends ControllerBaseSpec
    with ControllerTestRunner
    with MockAmendLossClaimTypeService
    with MockAmendLossClaimTypeRequestDataParser
    with MockHateoasFactory {

  private val claimId            = "AAZZ1234567890a"
  private val amendLossClaimType = AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

  private val response =
    AmendLossClaimTypeResponse(
      "2019-20",
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      "XKIS00000000988",
      Some(1),
      "2018-07-13T12:13:48.763Z"
    )

  private val request = AmendLossClaimTypeRequest(Nino(nino), claimId, amendLossClaimType)

  private val testHateoasLink = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  private val mtdResponseJson = Json.parse(
    """
      |{
      |    "businessId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYearClaimedFor": "2019-20",
      |    "typeOfClaim": "carry-forward",
      |    "sequence": 1,
      |    "lastModified": "2018-07-13T12:13:48.763Z",
      |    "links" : [
      |      {
      |        "href": "/foo/bar",
      |        "method": "GET",
      |        "rel": "test-relationship"
      |      }
      |    ]
      |}
   """.stripMargin
  )

  private val requestBody = Json.parse(
    """
      |{
      |  "typeOfClaim": "carry-forward"
      |}
   """.stripMargin
  )

  "amend" should {
    "return OK" when {
      "the request is valid" in new Test {
        MockAmendLossClaimTypeRequestDataParser
          .parseRequest(AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Right(request))

        MockAmendLossClaimTypeService
          .amend(AmendLossClaimTypeRequest(Nino(nino), claimId, amendLossClaimType))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrap(response, AmendLossClaimTypeHateoasData(nino, claimId))
            .returns(HateoasWrapper(response, Seq(testHateoasLink)))

        runOkTestWithAudit(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponseJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        MockAmendLossClaimTypeRequestDataParser
          .parseRequest(AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))

        runErrorTestWithAudit(NinoFormatError)
      }

      "the service returns an error" in new Test {
        MockAmendLossClaimTypeRequestDataParser
          .parseRequest(AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Right(request))

        MockAmendLossClaimTypeService
          .amend(AmendLossClaimTypeRequest(Nino(nino), claimId, amendLossClaimType))
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTypeOfClaimInvalidForbidden, None))))

        runErrorTestWithAudit(RuleTypeOfClaimInvalidForbidden)
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking {

    private val controller = new AmendLossClaimTypeController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      amendLossClaimTypeService = mockAmendLossClaimTypeService,
      amendLossClaimTypeParser = mockAmendLossClaimTypeRequestDataParser,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "AmendLossClaim",
        transactionName = "amend-loss-claim",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = "3.0",
          params = Map("nino" -> nino, "claimId" -> claimId),
          request = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          response = auditResponse
        )
      )

  }
}
