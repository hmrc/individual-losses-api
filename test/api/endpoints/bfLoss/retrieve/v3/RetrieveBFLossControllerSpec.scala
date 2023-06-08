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

package api.endpoints.bfLoss.retrieve.v3

import api.controllers.{ ControllerBaseSpec, ControllerTestRunner }
import api.endpoints.bfLoss.domain.anyVersion.TypeOfLoss
import api.endpoints.bfLoss.retrieve.v3.request.{ MockRetrieveBFLossParser, RetrieveBFLossRawData, RetrieveBFLossRequest }
import api.endpoints.bfLoss.retrieve.v3.response.{ GetBFLossHateoasData, RetrieveBFLossResponse }
import api.hateoas.MockHateoasFactory
import api.models.ResponseWrapper
import api.models.domain.{ Nino, Timestamp }
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{ HateoasWrapper, Link }
import play.api.libs.json.Json
import play.api.mvc.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveBFLossControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockRetrieveBFLossService
    with MockRetrieveBFLossParser
    with MockHateoasFactory {

  private val lossId  = "AAZZ1234567890a"
  private val rawData = RetrieveBFLossRawData(nino, lossId)
  private val request = RetrieveBFLossRequest(Nino(nino), lossId)

  private val response = RetrieveBFLossResponse(
    taxYearBroughtForwardFrom = "2017-18",
    typeOfLoss = TypeOfLoss.`uk-property-fhl`,
    businessId = "XKIS00000000988",
    lossAmount = 100.00,
    lastModified = Timestamp("2018-07-13T12:13:48.763Z")
  )

  private val testHateoasLink = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  private val mtdResponseJson = Json.parse(
    """
      |{
      |    "businessId": "XKIS00000000988",
      |    "taxYearBroughtForwardFrom": "2017-18",
      |    "typeOfLoss": "uk-property-fhl",
      |    "lossAmount": 100.00,
      |    "lastModified": "2018-07-13T12:13:48.763Z",
      |    "links": [
      |      {
      |       "href": "/foo/bar",
      |       "method": "GET",
      |       "rel": "test-relationship"
      |      }
      |    ]
      |}
    """.stripMargin
  )

  "retrieve" should {
    "return OK" when {
      "the request is valid" in new Test {
        MockRetrieveBFLossRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockRetrieveBFLossService
          .retrieve(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrap(response, GetBFLossHateoasData(nino, lossId))
          .returns(HateoasWrapper(response, Seq(testHateoasLink)))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponseJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        MockRetrieveBFLossRequestDataParser
          .parseRequest(rawData)
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))

        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        MockRetrieveBFLossRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockRetrieveBFLossService
          .retrieve(request)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, LossIdFormatError, None))))

        runErrorTest(LossIdFormatError)
      }
    }
  }

  private trait Test extends ControllerTest {

    private val controller = new RetrieveBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockRetrieveBFLossService,
      parser = mockRetrieveBFLossParser,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.retrieve(nino, lossId)(fakeRequest)
  }

}
