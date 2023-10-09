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

package v4.controllers.validators

import api.controllers.validators.Validator
import api.models.domain.{BusinessId, Nino, TaxYear}
import api.models.errors._
import support.UnitSpec
import v4.models.domain.bfLoss.IncomeSourceType
import v4.models.request.listLossClaims.ListBFLossesRequestData

class ListBFLossesValidatorFactorySpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino         = "AA123456A"
  private val invalidNino       = "AA123456"
  private val validTaxYear      = "2021-22"
  private val invalidTaxYear    = "not-a-tax-year"
  private val validLossType     = "self-employment"
  private val invalidLossType   = "not-a-type-of-loss"
  private val validBusinessId   = "XAIS01234567890"
  private val invalidBusinessId = "not-a-business-id"

  private val parsedNino             = Nino(validNino)
  private val parsedTaxYear          = TaxYear.fromMtd(validTaxYear)
  private val parsedIncomeSourceType = IncomeSourceType.`01`
  private val parsedBusinessId       = BusinessId(validBusinessId)

  private val validatorFactory = new ListBFLossesValidatorFactory

  private def validator(nino: String,
                        taxYearBroughtForwardFrom: String,
                        typeOfLoss: Option[String],
                        businessId: Option[String]): Validator[ListBFLossesRequestData] =
    validatorFactory.validator(nino, taxYearBroughtForwardFrom, typeOfLoss, businessId)

  "running validation" should {
    "return the parsed request data" when {
      "given a valid request" in {
        val result = validator(validNino, validTaxYear, Some(validLossType), Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Right(
          ListBFLossesRequestData(parsedNino, parsedTaxYear, Some(parsedIncomeSourceType), Some(parsedBusinessId))
        )
      }

      "given a valid request with no loss type" in {
        val result = validator(validNino, validTaxYear, None, Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Right(
          ListBFLossesRequestData(parsedNino, parsedTaxYear, None, Some(parsedBusinessId))
        )
      }

      "given a valid request with no business ID" in {
        val result = validator(validNino, validTaxYear, Some(validLossType), None).validateAndWrapResult()
        result shouldBe Right(
          ListBFLossesRequestData(parsedNino, parsedTaxYear, Some(parsedIncomeSourceType), None)
        )
      }

      "given a valid request with no query params" in {
        val result = validator(validNino, validTaxYear, None, None).validateAndWrapResult()
        result shouldBe Right(
          ListBFLossesRequestData(parsedNino, parsedTaxYear, None, None)
        )
      }

      "given a valid request with self-employment as type of loss" in {
        val result = validator(validNino, validTaxYear, Some("self-employment"), Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Right(
          ListBFLossesRequestData(parsedNino, parsedTaxYear, Some(IncomeSourceType.`01`), Some(parsedBusinessId))
        )
      }

      "given a valid request with uk-property-non-fhl as type of loss" in {
        val result = validator(validNino, validTaxYear, Some("uk-property-non-fhl"), Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Right(
          ListBFLossesRequestData(parsedNino, parsedTaxYear, Some(IncomeSourceType.`02`), Some(parsedBusinessId))
        )
      }

      "given a valid request with foreign-property-fhl-eea as type of loss" in {
        val result = validator(validNino, validTaxYear, Some("foreign-property-fhl-eea"), Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Right(
          ListBFLossesRequestData(parsedNino, parsedTaxYear, Some(IncomeSourceType.`03`), Some(parsedBusinessId))
        )
      }

      "given a valid request with uk-property-fhl as type of loss" in {
        val result = validator(validNino, validTaxYear, Some("uk-property-fhl"), Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Right(
          ListBFLossesRequestData(parsedNino, parsedTaxYear, Some(IncomeSourceType.`04`), Some(parsedBusinessId))
        )
      }

      "given a valid request with foreign-property as type of loss" in {
        val result = validator(validNino, validTaxYear, Some("foreign-property"), Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Right(
          ListBFLossesRequestData(parsedNino, parsedTaxYear, Some(IncomeSourceType.`15`), Some(parsedBusinessId))
        )
      }
    }

    "return NinoFormatError" when {
      "given an invalid nino" in {
        val result = validator(invalidNino, validTaxYear, Some(validLossType), Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return TaxYearFormatError" when {
      "given an invalid tax year" in {
        val result = validator(validNino, invalidTaxYear, Some(validLossType), Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearFormatError)
        )
      }
    }

    "return RuleTaxYearRangeInvalidError" when {
      "given a tax year range which isn't a single year" in {
        val result = validator(validNino, "2018-20", Some(validLossType), Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError)
        )
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "the tax year is too early" in {
        val result = validator(validNino, "2017-18", Some(validLossType), Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotSupportedError)
        )
      }
    }

    "return TypeOfLossFormatError" when {
      "given an invalid loss type" in {
        val result = validator(validNino, validTaxYear, Some(invalidLossType), Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TypeOfLossFormatError)
        )
      }

      "the loss type is self-employment-class4" in {
        withClue("Because IFS does not distinguish self-employment types for its query...") {
          val result = validator(validNino, validTaxYear, Some("self-employment-class4"), Some(validBusinessId)).validateAndWrapResult()
          result shouldBe Left(
            ErrorWrapper(correlationId, TypeOfLossFormatError)
          )
        }
      }
    }

    "return BusinessIdFormatError" when {
      "given an invalid self employment id" in {
        val result = validator(validNino, validTaxYear, Some(validLossType), Some(invalidBusinessId)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BusinessIdFormatError)
        )
      }
    }

    "return multiple errors" when {
      "given a request with multiple errors" in {
        val result = validator(invalidNino, invalidTaxYear, Some(validLossType), Some(validBusinessId)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BadRequestError, Some(List(NinoFormatError, TaxYearFormatError)))
        )
      }
    }
  }

}
