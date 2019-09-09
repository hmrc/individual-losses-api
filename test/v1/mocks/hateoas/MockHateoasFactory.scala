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

package v1.mocks.hateoas

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import v1.hateoas.HateoasFactory
import v1.models.des.CreateBFLossResponse
import v1.models.hateoas.HateoasWrapper
import v1.models.outcomes.DesResponse

trait MockHateoasFactory extends MockFactory {

  val mockHateoasFactory: HateoasFactory = mock[HateoasFactory]

  object MockHateoasFactory {
    def wrap(nino: String, lossId: String, payload: DesResponse[CreateBFLossResponse]): CallHandler[DesResponse[HateoasWrapper[CreateBFLossResponse]]] = {
      (mockHateoasFactory.wrap(_: String, _: String, _: DesResponse[CreateBFLossResponse]))
        .expects(nino, lossId, payload)
    }
  }
}
