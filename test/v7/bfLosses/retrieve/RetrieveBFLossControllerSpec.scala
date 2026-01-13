/*
 * Copyright 2025 HM Revenue & Customs
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

package v7.bfLosses.retrieve

import cats.implicits.catsSyntaxValidatedId
import common.errors.LossIdFormatError
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.Result
import shared.config.Deprecation.NotDeprecated
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.domain.Timestamp
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import v7.bfLosses.common.domain.{LossId, TypeOfLoss}
import v7.bfLosses.retrieve
import v7.bfLosses.retrieve.def1.model.request.Def1_RetrieveBFLossRequestData
import v7.bfLosses.retrieve.def1.model.response.Def1_RetrieveBFLossResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveBFLossControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSharedAppConfig
    with retrieve.MockRetrieveBFLossValidatorFactory
    with retrieve.MockRetrieveBFLossService {

  private val lossId      = "AAZZ1234567890a"
  private val requestData = Def1_RetrieveBFLossRequestData(parsedNino, LossId(lossId))

  private val response = Def1_RetrieveBFLossResponse(
    taxYearBroughtForwardFrom = "2017-18",
    typeOfLoss = TypeOfLoss.`uk-property-fhl`,
    businessId = "XKIS00000000988",
    lossAmount = 100.00,
    lastModified = Timestamp("2018-07-13T12:13:48.763Z")
  )

  private val mtdResponseJson = Json.parse(
    """
      |{
      |    "businessId": "XKIS00000000988",
      |    "taxYearBroughtForwardFrom": "2017-18",
      |    "typeOfLoss": "uk-property-fhl",
      |    "lossAmount": 100.00,
      |    "lastModified": "2018-07-13T12:13:48.763Z"
      |}
    """.stripMargin
  )

  "retrieve" should {
    "return OK" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveBFLossService
          .retrieve(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponseJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))
        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveBFLossService
          .retrieve(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, LossIdFormatError, None))))

        runErrorTest(LossIdFormatError)
      }
    }
  }

  private trait Test extends ControllerTest {

    val controller: RetrieveBFLossController = new RetrieveBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockRetrieveBFLossService,
      validatorFactory = mockRetrieveBFLossValidatorFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.retrieve(validNino, lossId)(fakeRequest)

    MockedSharedAppConfig.deprecationFor(Version9).returns(NotDeprecated.valid).anyNumberOfTimes()

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false
  }

}
