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

package api.endpoints.bfLoss.list.v4.request

import api.models.errors._
import support.UnitSpec

class ListBFLossesValidatorSpec extends UnitSpec {

  private val nino       = "AA123456A"
  private val taxYear    = "2021-22"
  private val lossType   = "self-employment"
  private val businessId = "XAIS01234567890"

  val validator = new ListBFLossesValidator

  "running validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = taxYear,
            typeOfLoss = Some(lossType),
            businessId = Some(businessId)
          )
        )

        result shouldBe Nil
      }

      "no loss type is supplied" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = taxYear,
            typeOfLoss = None,
            businessId = Some(businessId)
          )
        )

        result shouldBe Nil
      }

      "no business ID is supplied" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = taxYear,
            typeOfLoss = Some(lossType),
            businessId = None
          )
        )

        result shouldBe Nil
      }

      "a request with no query params is provided" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = taxYear,
            typeOfLoss = None,
            businessId = None
          )
        )

        result shouldBe Nil
      }

      "a request with self-employment as type of loss is provided" in {
        val result = validator.validate(
          ListBFLossesRawData(nino = nino, taxYearBroughtForwardFrom = taxYear, typeOfLoss = Some("self-employment"), businessId = Some(businessId)))

        result shouldBe Nil
      }

      "a request with uk-property-non-fhl as type of loss is provided" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = taxYear,
            typeOfLoss = Some("uk-property-non-fhl"),
            businessId = Some(businessId)
          )
        )
        result shouldBe Nil
      }

      "a request with foreign-property-fhl-eea as type of loss is provided" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = taxYear,
            typeOfLoss = Some("foreign-property-fhl-eea"),
            businessId = Some(businessId)))

        result shouldBe Nil
      }

      "a request with uk-property-fhl as type of loss is provided" in {
        val result = validator.validate(
          ListBFLossesRawData(nino = nino, taxYearBroughtForwardFrom = taxYear, typeOfLoss = Some("uk-property-fhl"), businessId = Some(businessId))
        )

        result shouldBe Nil
      }

      "a request with foreign-property as type of loss is provided" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = taxYear,
            typeOfLoss = Some("foreign-property"),
            businessId = Some(businessId)
          )
        )
        result shouldBe Nil
      }
    }

    "return NinoFormatError" when {
      "the nino is supplied and invalid" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = "badNino",
            taxYearBroughtForwardFrom = taxYear,
            typeOfLoss = Some(lossType),
            businessId = Some(businessId)
          )
        )

        result shouldBe List(NinoFormatError)
      }
    }

    "return TaxYearFormatError" when {
      "the tax year is the incorrect format" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = "XXXX",
            typeOfLoss = Some(lossType),
            businessId = Some(businessId)
          )
        )

        result shouldBe List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearRangeExceededError" when {
      "the tax year range is not a single year" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = "2018-20",
            typeOfLoss = Some(lossType),
            businessId = Some(businessId)
          )
        )

        result shouldBe List(RuleTaxYearRangeInvalidError)
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "the tax year is too early" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = "2017-18",
            typeOfLoss = Some(lossType),
            businessId = Some(businessId)
          )
        )

        result shouldBe List(RuleTaxYearNotSupportedError)
      }
    }

    "return TypeOfLossFormatError" when {
      "the loss type is invalid" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = taxYear,
            typeOfLoss = Some("badLossType"),
            businessId = Some(businessId)
          )
        )

        result shouldBe List(TypeOfLossFormatError)
      }

      // Because IFS does not distinguish self-employment types for its query...
      "the loss type is self-employment-class4" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = taxYear,
            typeOfLoss = Some("self-employment-class4"),
            businessId = Some(businessId)))

        result shouldBe List(TypeOfLossFormatError)
      }
    }

    "return BusinessIdFormatError" when {
      "the self employment id is invalid" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = taxYear,
            typeOfLoss = Some(lossType),
            businessId = Some("badBusinessId")
          )
        )

        result shouldBe List(BusinessIdFormatError)
      }
    }

    "return multiple errors" when {
      "there are multiple errors" in {
        val result = validator.validate(
          ListBFLossesRawData(
            nino = "badNino",
            taxYearBroughtForwardFrom = "badTaxYear",
            typeOfLoss = Some(lossType),
            businessId = Some(businessId)
          )
        )

        result shouldBe List(NinoFormatError, TaxYearFormatError)
      }
    }
  }

}
