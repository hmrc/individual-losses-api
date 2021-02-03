/*
 * Copyright 2021 HM Revenue & Customs
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

package v2.controllers.requestParsers.validators

import support.UnitSpec
import v2.models.errors._
import v2.models.requestData.ListBFLossesRawData

class ListBFLossesValidatorSpec extends UnitSpec {

  private val validNino     = "AA123456A"
  private val validTaxYear  = "2021-22"
  private val validLossType = "self-employment"
  private val validBusinessId = "XAIS01234567890"

  val validator = new ListBFLossesValidator

  "running validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
                                               taxYear = Some(validTaxYear),
                                               typeOfLoss = Some(validLossType),
                                               businessId = Some(validBusinessId))) shouldBe Nil
      }
      "a business id is not supplied for a loss type of self-employment" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("self-employment"),
          businessId = None)) shouldBe Nil
      }

      "a business id is supplied and no loss type is supplied" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = None,
          businessId = Some(validBusinessId))) shouldBe Nil
      }
    }

    "return NinoFormatError" when {
      "the nino is supplied and invalid" in {
        validator.validate(ListBFLossesRawData(nino = "badNino",
                                               taxYear = Some(validTaxYear),
                                               typeOfLoss = Some(validLossType),
                                               businessId = Some(validBusinessId))) shouldBe List(NinoFormatError)
      }
    }

    "return TaxYearFormatError" when {
      "the tax year is the incorrect format" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some("XXXX"),
          typeOfLoss = Some(validLossType),
          businessId = Some(validBusinessId))) shouldBe List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearRangeExceededError" when {
      "the tax year range is not a single year" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some("2018-20"),
          typeOfLoss = Some(validLossType),
          businessId = Some(validBusinessId))) shouldBe List(RuleTaxYearRangeInvalid)
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "the tax year is too early" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some("2017-18"),
          typeOfLoss = Some(validLossType),
          businessId = Some(validBusinessId))) shouldBe List(RuleTaxYearNotSupportedError)
      }
    }

    "return TypeOfLossFormatError" when {
      "the loss type is invalid" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("badLossType"),
          businessId = Some(validBusinessId))) shouldBe List(TypeOfLossFormatError)
      }

      // Because DES does not distinguish self-employment types for its query...
      "the loss type is self-employment-class4" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("self-employment-class4"),
          businessId = Some(validBusinessId))) shouldBe List(TypeOfLossFormatError)
      }
    }

    "return BusinessIdFormatError" when {
      "the self employment id is invalid" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some(validLossType),
          businessId = Some("badBusinessId"))) shouldBe List(BusinessIdFormatError)
      }
    }

    "not return an error" when {
      "a self employment id is not supplied for a loss type of non-fhl property" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("uk-property-non-fhl"),
          businessId = None)) shouldBe Nil
      }
      "a business id is not supplied for a loss type of fhl property" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("uk-property-fhl"),
          businessId = None)) shouldBe Nil
      }
      "a business id is supplied for a loss type of self-employment" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("self-employment"),
          businessId = Some(validBusinessId))) shouldBe Nil
      }
      "a business id is supplied for a loss type of foreign-property" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("foreign-property"),
          businessId = Some(validBusinessId))) shouldBe Nil
      }
      "a business id is supplied for a loss type of foreign-property-fhl-eea" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("foreign-property-fhl-eea"),
          businessId = Some(validBusinessId))) shouldBe Nil
      }
    }

    "return multiple errors" when {
      "there are multiple errors" in {
        validator.validate(ListBFLossesRawData(nino = "badNino",
          taxYear = Some("badTaxYear"),
          typeOfLoss = Some(validLossType),
          businessId = Some(validBusinessId))) shouldBe List(NinoFormatError, TaxYearFormatError) }
    }
  }

}
