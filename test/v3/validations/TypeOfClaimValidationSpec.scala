/*
 * Copyright 2022 HM Revenue & Customs
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

package v3.validations

import api.models.domain.lossClaim.v3.{TypeOfClaim, TypeOfLoss}
import api.validations.lossClaim.v3.TypeOfClaimValidation
import support.UnitSpec
import api.models.domain.lossClaim.v3.TypeOfLoss
import v3.models.errors.{RuleTypeOfClaimInvalid, TypeOfClaimFormatError}

class TypeOfClaimValidationSpec extends UnitSpec {

  "validate" should {

    "return no errors" when {
      checkValid("carry-forward")
      checkValid("carry-sideways")
      checkValid("carry-sideways-fhl")
      checkValid("carry-forward-to-carry-sideways")

      def checkValid(typeOfClaim: String): Unit =
        s"provided with a string of '$typeOfClaim'" in {
          TypeOfClaimValidation.validate(typeOfClaim) shouldBe Nil
        }
    }

    "return a TypeOfLossFormatError" when {
      "provided with an unknown type of claim" in {
        TypeOfClaimValidation.validate("invalid") shouldBe List(TypeOfClaimFormatError)
      }
    }
  }

  "validateTypeOfClaimPermitted" when {
    "a typeOfLoss is self employment" must {
      permitOnly(TypeOfLoss.`self-employment`, Seq(TypeOfClaim.`carry-forward`, TypeOfClaim.`carry-sideways`))
    }

    "a typeOfLoss is uk-property-non-fhl" must {
      permitOnly(TypeOfLoss.`uk-property-non-fhl`,
                 Seq(TypeOfClaim.`carry-sideways`, TypeOfClaim.`carry-sideways-fhl`, TypeOfClaim.`carry-forward-to-carry-sideways`))
    }

    "a typeOfLoss is foreign-property" must {
      permitOnly(TypeOfLoss.`foreign-property`,
                 Seq(TypeOfClaim.`carry-sideways`, TypeOfClaim.`carry-sideways-fhl`, TypeOfClaim.`carry-forward-to-carry-sideways`))
    }

    def permitOnly(typeOfLoss: TypeOfLoss, permittedTypesOfClaim: Seq[TypeOfClaim]): Unit = {
      permittedTypesOfClaim.foreach(typeOfClaim =>
        s"permit $typeOfLoss with $typeOfClaim" in {
          TypeOfClaimValidation.validateTypeOfClaimPermitted(typeOfClaim, typeOfLoss) shouldBe Nil
      })

      TypeOfClaim.values
        .filterNot(permittedTypesOfClaim.contains)
        .foreach(typeOfClaim =>
          s"not permit $typeOfLoss with $typeOfClaim" in {
            TypeOfClaimValidation.validateTypeOfClaimPermitted(typeOfClaim, typeOfLoss) shouldBe List(RuleTypeOfClaimInvalid)
        })
    }
  }
}
