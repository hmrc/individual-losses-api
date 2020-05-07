/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.controllers.requestParsers.validators

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v1.models.domain.Claim
import v1.models.errors._
import v1.models.requestData.AmendLossClaimsOrderRawData

class AmendLossClaimsOrderValidatorSpec extends UnitSpec {

  private val validNino                     = "AA123456A"
  private val invalidNino                   = "AA123456"
  private val validTaxYear                  = "2019-20"
  private val invalidTaxYearGap             = "2018-20"
  private val invalidTaxYearFormat          = "19-20"
  private val validId                       = "1234568790ABCDE"

  private def lossClaim(claimType: String, listOfLossClaims: List[Claim]) =
    AnyContentAsJson(Json.obj("claimType" -> claimType, "listOfLossClaims" -> listOfLossClaims))

  val validator = new AmendLossClaimsOrderValidator()

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), lossClaim("carry-sideways", List(Claim(validId, 1))))) shouldBe Nil
      }
      "no Tax year is supplied" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, None, lossClaim("carry-sideways", List(Claim("1234568790ABCDE", 1))))) shouldBe Nil
      }
    }

    "return one error" when {
      "nino validation fails" in {
        validator.validate(AmendLossClaimsOrderRawData(invalidNino, Some(validTaxYear), lossClaim("carry-sideways", List(Claim(validId, 1))))) shouldBe List(NinoFormatError)
      }

      "tax year gap is higher than 1" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(invalidTaxYearGap), lossClaim("carry-sideways", List(Claim(validId, 1))))) shouldBe List(TaxYearFormatError)
      }

      "tax year format is invalid" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(invalidTaxYearFormat), lossClaim("carry-sideways", List(Claim(validId, 1))))) shouldBe List(TaxYearFormatError)
      }

      "a non-carry-sideways claimType is provided" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), lossClaim("carry-forward", List(Claim(validId, 1))))) shouldBe List(ClaimTypeFormatError)
      }

      "Id format is invalid" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), lossClaim("carry-sideways", List(Claim("1234", 1))))) shouldBe List(ClaimIdFormatError)
      }

      "a mandatory field isn't provided" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), AnyContentAsJson(Json.obj()))) shouldBe List(RuleIncorrectOrEmptyBodyError)
      }

      "sequence order is invalid" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), lossClaim("carry-sideways", List(Claim(validId, 1), Claim(validId, 3), Claim(validId, 5))))) shouldBe List(RuleSequenceOrderBroken)
      }
      "sequence start is invalid" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), lossClaim("carry-sideways", List(Claim(validId, 3), Claim(validId, 2), Claim(validId, 4))))) shouldBe List(RuleInvalidSequenceStart)
      }
    }
    "return multiple errors" when {
      "an invalid nino and tax year is provided" in {
        validator.validate(AmendLossClaimsOrderRawData("Walrus", Some("13900"), lossClaim("carry-sideways", List(Claim(validId, 1))))) shouldBe List(NinoFormatError, TaxYearFormatError)
      }
      "invalid body fields are provided" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), lossClaim("carry-diagonal", List(Claim("Walrus", 2),Claim("Walrus", 100))))) shouldBe List(RuleInvalidSequenceStart, RuleSequenceOrderBroken, ClaimTypeFormatError, ClaimIdFormatError, SequenceFormatError)
      }
    }
  }
}
