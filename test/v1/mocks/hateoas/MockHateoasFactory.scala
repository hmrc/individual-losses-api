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
import v1.models.des.ListBFLossesHateoasResponse
import v1.models.hateoas.{HateoasData, HateoasWrapper}
import v1.models.outcomes.DesResponse

trait MockHateoasFactory extends MockFactory {

  val mockHateoasFactory: HateoasFactory = mock[HateoasFactory]

  object MockHateoasFactory {
    def wrap[A](data: HateoasData[A]): CallHandler[DesResponse[HateoasWrapper[A]]] = {
      (mockHateoasFactory.wrap(_: HateoasData[A]))
        .expects(data)
    }

     def wrapList[A,B](data: HateoasData[A]): CallHandler[DesResponse[HateoasWrapper[ListBFLossesHateoasResponse]]] = {
      (mockHateoasFactory.wrapList[A,B](_: HateoasData[A]))
        .expects(data)
    }
  }
}
