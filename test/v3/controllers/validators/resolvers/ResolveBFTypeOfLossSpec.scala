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

package v3.controllers.validators.resolvers

import api.models.errors.TypeOfLossFormatError
import cats.data.Validated.{Invalid, Valid}
import support.UnitSpec
import v3.models.domain.bfLoss.TypeOfLoss

class ResolveBFTypeOfLossSpec extends UnitSpec {

  "The resolver" should {

    "return the resolved value" when {
      import TypeOfLoss._

      checkValid(`self-employment`)
      checkValid(`self-employment-class4`)
      checkValid(`uk-property-fhl`)
      checkValid(`uk-property-non-fhl`)
      checkValid(`foreign-property`)
      checkValid(`foreign-property-fhl-eea`)

      def checkValid(typeOfLoss: TypeOfLoss): Unit =
        s"given '$typeOfLoss'" in {
          val result = ResolveBFTypeOfLoss(typeOfLoss.toString)
          result shouldBe Valid(typeOfLoss)
        }
    }

    "return a TypeOfLossFormatError" when {
      "provided with an unknown type of loss" in {
        val result = ResolveBFTypeOfLoss("not-a-type-of-loss")
        result shouldBe Invalid(List(TypeOfLossFormatError))
      }
    }
  }

}
