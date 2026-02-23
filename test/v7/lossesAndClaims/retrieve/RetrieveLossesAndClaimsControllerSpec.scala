/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.retrieve

import play.api.Configuration
import play.api.mvc.Result
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.domain.{BusinessId, TaxYear}
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import v7.lossesAndClaims.retrieve.fixtures.RetrieveLossesAndClaimsFixtures.{mtdResponseBodyJson, responseBodyModel}
import v7.lossesAndClaims.retrieve.model.request.RetrieveLossesAndClaimsRequestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveLossesAndClaimsControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockRetrieveLossesAndClaimsService
    with MockRetrieveLossesAndClaimsValidatorFactory {

  private val businessId: String = "X0IS12345678901"
  private val taxYear: String    = "2026-27"

  private val requestData: RetrieveLossesAndClaimsRequestData = RetrieveLossesAndClaimsRequestData(
    nino = parsedNino,
    businessId = BusinessId(businessId),
    taxYear = TaxYear.fromMtd(taxYear)
  )

  "retrieve" should {
    "return OK" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveLossesAndClaimsService
          .retrieve(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseBodyModel))))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponseBodyJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(BusinessIdFormatError))
        runErrorTest(BusinessIdFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveLossesAndClaimsService
          .retrieve(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, NotFoundError, None))))

        runErrorTest(NotFoundError)
      }
    }
  }

  private trait Test extends ControllerTest {

    val controller: RetrieveLossesAndClaimsController = new RetrieveLossesAndClaimsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockRetrieveLossesAndClaimsService,
      validatorFactory = mockRetrieveLossesAndClaimsValidatorFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.retrieve(validNino, businessId, taxYear)(fakeRequest)

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns true

  }

}
