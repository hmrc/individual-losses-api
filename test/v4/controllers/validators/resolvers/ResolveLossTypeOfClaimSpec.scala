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

package v4.controllers.validators.resolvers

import cats.data.Validated.{Invalid, Valid}
import common.errors.TypeOfClaimFormatError
import shared.utils.UnitSpec
import v4.models.domain.lossClaim.TypeOfClaim

class ResolveLossTypeOfClaimSpec extends UnitSpec {

  "The resolver" should {

    "return the resolved value" when {
      import TypeOfClaim.*

      checkValid(`carry-forward`)
      checkValid(`carry-sideways`)
      checkValid(`carry-sideways-fhl`)
      checkValid(`carry-forward-to-carry-sideways`)

      def checkValid(typeOfClaim: TypeOfClaim): Unit =
        s"given '$typeOfClaim'" in {
          val result = ResolveLossTypeOfClaim(typeOfClaim.toString)
          result shouldBe Valid(typeOfClaim)
        }
    }

    "return a TypeOfClaimFormatError" when {
      "provided with an unknown type of claim" in {
        val result = ResolveLossTypeOfClaim("not-a-type-of-claim")
        result shouldBe Invalid(List(TypeOfClaimFormatError))
      }
    }
  }

}
