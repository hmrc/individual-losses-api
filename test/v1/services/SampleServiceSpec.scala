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

package v1.services

import uk.gov.hmrc.domain.Nino
import v1.connectors.DesUri
import v1.mocks.connectors.MockDesConnector
import v1.models.des.DesSampleResponse
import v1.models.domain.{SampleRequestBody, EmptyJsonBody}
import v1.models.outcomes.ResponseWrapper
import v1.models.requestData.{DesTaxYear, SampleRequestData}

import scala.concurrent.Future

class SampleServiceSpec extends ServiceSpec {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val taxYear = DesTaxYear("2018")
  val nino    = Nino("AA123456A")
  val calcId  = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"

  trait Test extends MockDesConnector {
    lazy val service = new SampleService(connector)
  }

  "doService" must {
    val request = SampleRequestData(nino, taxYear, SampleRequestBody("someData"))

    "post an empty body and retun the result" in new Test {

      val outcome = Right(ResponseWrapper(correlationId, DesSampleResponse(calcId)))

      MockedDesConnector
        .post(EmptyJsonBody,
              DesUri[DesSampleResponse](s"income-tax/nino/${nino.nino}/taxYear/${taxYear.value}/someService"))
        .returns(Future.successful(outcome))

      await(service.doService(request)) shouldBe outcome
    }
  }
}
