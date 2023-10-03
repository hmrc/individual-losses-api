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

package v3.controllers.validators

import api.controllers.validators.Validator
import api.models.domain.{BusinessId, Nino, TaxYear}
import api.models.errors._
import support.UnitSpec
import v3.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}
import v3.models.request.listLossClaims.ListLossClaimsRequestData

class ListLossClaimsValidatorFactorySpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino          = "AA123456A"
  private val invalidNino        = "AA123456"
  private val validTaxYear       = "2021-22"
  private val invalidTaxYear     = "not-a-tax-year"
  private val validLossType      = "self-employment"
  private val invalidLossType    = "not-a-type-of-loss"
  private val validBusinessId    = "XAIS01234567890"
  private val invalidBusinessId  = "not-a-business-id"
  private val validTypeOfClaim   = "carry-sideways"
  private val invalidTypeOfClaim = "not-a-type-of-claim"

  private val parsedNino        = Nino(validNino)
  private val parsedTaxYear     = TaxYear.fromMtd(validTaxYear)
  private val parsedTypeOfLoss  = TypeOfLoss.`self-employment`
  private val parsedBusinessId  = BusinessId(validBusinessId)
  private val parsedTypeOfClaim = TypeOfClaim.`carry-sideways`

  private val validatorFactory = new ListLossClaimsValidatorFactory

  private def validator(nino: String,
                        taxYearClaimedFor: Option[String],
                        typeOfLoss: Option[String],
                        businessId: Option[String],
                        typeOfClaim: Option[String]): Validator[ListLossClaimsRequestData] =
    validatorFactory.validator(nino, taxYearClaimedFor, typeOfLoss, businessId, typeOfClaim)

  "running validation" should {
    "return the parsed request data" when {
      "given a valid request" in {
        val result =
          validator(validNino, Some(validTaxYear), Some(validLossType), Some(validBusinessId), Some(validTypeOfClaim)).validateAndWrapResult()
        result shouldBe Right(
          ListLossClaimsRequestData(parsedNino, Some(parsedTaxYear), Some(parsedTypeOfLoss), Some(parsedBusinessId), Some(parsedTypeOfClaim))
        )
      }

      "given a valid request with no parameters" in {
        val result = validator(validNino, None, None, None, None).validateAndWrapResult()
        result shouldBe Right(
          ListLossClaimsRequestData(parsedNino, None, None, None, None)
        )
      }
    }

    "return NinoFormatError" when {
      "given an invalid nino" in {
        val result =
          validator(invalidNino, Some(validTaxYear), Some(validLossType), Some(validBusinessId), Some(validTypeOfClaim)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return TaxYearFormatError" when {
      "given an invalid tax year" in {
        val result =
          validator(validNino, Some(invalidTaxYear), Some(validLossType), Some(validBusinessId), Some(validTypeOfClaim)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearFormatError)
        )
      }
    }

    "return RuleTaxYearRangeInvalid" when {
      "given a tax year range which isn't a single year" in {
        val result = validator(validNino, Some("2018-20"), Some(validLossType), Some(validBusinessId), Some(validTypeOfClaim)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError)
        )
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "the tax year is too early" in {
        val result = validator(validNino, Some("2018-19"), Some(validLossType), Some(validBusinessId), Some(validTypeOfClaim)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotSupportedError)
        )
      }
    }

    "return TypeOfLossFormatError" when {
      "given an invalid loss type" in {
        val result =
          validator(validNino, Some(validTaxYear), Some(invalidLossType), Some(validBusinessId), Some(validTypeOfClaim)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TypeOfLossFormatError)
        )
      }

      "the loss type is not permitted for claims" in {
        val result = validator(validNino, Some(validTaxYear), Some("self-employment-class4"), Some(validBusinessId), Some(validTypeOfClaim))
          .validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TypeOfLossFormatError)
        )
      }
    }

    "return TypeOfClaimFormatError" when {
      "given an invalid claim type" in {
        val result =
          validator(validNino, Some(validTaxYear), Some(validLossType), Some(validBusinessId), Some(invalidTypeOfClaim)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TypeOfClaimFormatError)
        )
      }

      "given a claim type that isn't carry-sideways" in {
        TypeOfClaim.values
          .filter(_ != TypeOfClaim.`carry-sideways`)
          .foreach { typeOfClaim =>
            val result =
              validator(validNino, Some(validTaxYear), Some(validLossType), Some(validBusinessId), Some(typeOfClaim.toString)).validateAndWrapResult()
            result shouldBe Left(
              ErrorWrapper(correlationId, TypeOfClaimFormatError)
            )
          }
      }
    }

    "return BusinessIdFormatError" when {
      "given an invalid self employment id" in {
        val result =
          validator(validNino, Some(validTaxYear), Some(validLossType), Some(invalidBusinessId), Some(validTypeOfClaim)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BusinessIdFormatError)
        )
      }
    }

    "return multiple errors" when {
      "given a request with multiple errors" in {
        val result =
          validator(invalidNino, Some(invalidTaxYear), Some(validLossType), Some(validBusinessId), Some(validTypeOfClaim)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BadRequestError, Some(List(TaxYearFormatError, NinoFormatError)))
        )
      }
    }
  }

}
