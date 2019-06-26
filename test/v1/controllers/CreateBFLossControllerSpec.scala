/*
 * Copyright 2019 HM Revenue & Customs
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

package v1.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.requestParsers.MockCreateBFLossRequestDataParser
import v1.mocks.services.{MockAuditService, MockCreateBFLossService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.domain.BFLoss
import v1.models.outcomes.DesResponse
import v1.models.requestData.{CreateBFLossRawData, CreateBFLossRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateBFLossControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockCreateBFLossService
    with MockCreateBFLossRequestDataParser
    with MockAuditService {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino = "AA123456A"
  val lossId = "AAZZ1234567890a"

  val bfLoss = BFLoss("self-employment", Some("XKIS00000000988"), "2019-20", 256.78)

  val requestBody: JsValue = Json.parse(
    """
      |{
      |    "selfEmploymentId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYear": "2019-20",
      |    "lossAmount": 256.78
      |}
    """.stripMargin)

  trait Test {
    val hc = HeaderCarrier()

    val controller = new CreateBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      createBFLossService = mockCreateBFLossService,
      createBFLossRequestDataParser = mockCreateBFLossRequestDataParser,
      auditService = mockAuditService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  "create" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockCreateBFLossRequestDataParser.parseRequest(
          CreateBFLossRawData(nino, requestBody))
          .returns(Right(retrieveCharitableGivingRequest))

        MockCreateBFLossService
          .create(CreateBFLossRequest(Nino(nino), bfLoss))
          .returns(Future.successful(Right(DesResponse(correlationId, lossId))))

        val result: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
        status(result) shouldBe CREATED

      }
    }
  }
}
