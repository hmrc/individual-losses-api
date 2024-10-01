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

package v5.lossClaims.common.resolvers

import cats.data.Validated.{Invalid, Valid}
import common.errors.TypeOfLossFormatError
import shared.utils.UnitSpec
import v5.lossClaims.validators.models.TypeOfLoss

class ResolveLossClaimTypeOfLossSpec extends UnitSpec {

  "The resolver" should {

    "return the resolved value" when {
      import TypeOfLoss._

      checkValid(`uk-property-non-fhl`)
      checkValid(`foreign-property`)
      checkValid(`self-employment`)

      def checkValid(typeOfLoss: TypeOfLoss): Unit =
        s"given '$typeOfLoss'" in {
          val result = ResolveLossClaimTypeOfLoss(typeOfLoss.toString)
          result shouldBe Valid(typeOfLoss)
        }
    }

    "return a TypeOfLossFormatError" when {
      "provided with an unknown type of loss" in {
        val result = ResolveLossClaimTypeOfLoss("not-a-type-of-loss")
        result shouldBe Invalid(List(TypeOfLossFormatError))
      }
    }
  }

}
