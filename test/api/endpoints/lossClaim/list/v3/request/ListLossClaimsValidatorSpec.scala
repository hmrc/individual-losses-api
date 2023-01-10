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

package api.endpoints.lossClaim.list.v3.request

import api.endpoints.lossClaim.domain.v3.TypeOfClaim
import api.models.errors._
import support.UnitSpec

class ListLossClaimsValidatorSpec extends UnitSpec {

  private val validNino        = "AA123456A"
  private val validTaxYear     = "2021-22"
  private val validLossType    = "self-employment"
  private val validBusinessId  = "XAIS01234567890"
  private val validTypeOfClaim = "carry-sideways"

  val validator = new ListLossClaimsValidator

  def rawData(nino: String = validNino,
              taxYearClaimedFor: Option[String] = Some(validTaxYear),
              typeOfLoss: Option[String] = Some(validLossType),
              businessId: Option[String] = Some(validBusinessId),
              typeOfClaim: Option[String] = Some(validTypeOfClaim)): ListLossClaimsRawData =
    ListLossClaimsRawData(nino = nino,
                          taxYearClaimedFor = taxYearClaimedFor,
                          typeOfLoss = typeOfLoss,
                          businessId = businessId,
                          typeOfClaim = typeOfClaim)

  "running validation" should {
    "return no errors" when {
      "a valid request with all parameters is supplied" in {
        validator.validate(rawData()) shouldBe Nil
      }

      "a valid request with no parameters is supplied" in {
        validator.validate(rawData(taxYearClaimedFor = None, typeOfLoss = None, businessId = None, typeOfClaim = None)) shouldBe Nil
      }
    }

    "return NinoFormatError" when {
      "the nino is supplied and invalid" in {
        validator.validate(rawData(nino = "badNino")) shouldBe List(NinoFormatError)
      }
    }

    "return TaxYearFormatError" when {
      "the tax year is the incorrect format" in {
        validator.validate(rawData(taxYearClaimedFor = Some("XXXX"))) shouldBe List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearRangeInvalid" when {
      "the tax year range is not a single year" in {
        validator.validate(rawData(taxYearClaimedFor = Some("2018-20"))) shouldBe List(RuleTaxYearRangeInvalidError)
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "the tax year is too early" in {
        validator.validate(rawData(taxYearClaimedFor = Some("2018-19"))) shouldBe List(RuleTaxYearNotSupportedError)
      }
    }

    "return TypeOfLossFormatError" when {
      "the loss type is invalid" in {
        validator.validate(rawData(typeOfLoss = Some("badLossType"))) shouldBe List(TypeOfLossFormatError)
      }

      "the loss type is not permitted for claims" in {
        validator.validate(rawData(typeOfLoss = Some("self-employment-class4"))) shouldBe List(TypeOfLossFormatError)
      }
    }

    "return TypeOfClaimFormatError" when {
      "is not valid claim type" in {
        validator.validate(rawData(typeOfClaim = Some("invalid"))) shouldBe List(TypeOfClaimFormatError)
      }

      "is something other than carry-sideways" in {
        TypeOfClaim.values
          .filter(_ != TypeOfClaim.`carry-sideways`)
          .foreach(typeOfClaim => validator.validate(rawData(typeOfClaim = Some(typeOfClaim.toString))) shouldBe List(TypeOfClaimFormatError))
      }
    }

    "return BusinessIdFormatError" when {
      "the self employment id is invalid" in {
        validator.validate(rawData(businessId = Some("badBusinessId"))) shouldBe List(BusinessIdFormatError)
      }
    }

    "return multiple errors" when {
      "there are multiple errors" in {
        validator.validate(rawData(nino = "badNino", taxYearClaimedFor = Some("badTaxYear"))) shouldBe List(NinoFormatError, TaxYearFormatError)
      }
    }
  }
}
