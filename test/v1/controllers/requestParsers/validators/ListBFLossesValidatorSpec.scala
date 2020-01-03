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

import support.UnitSpec
import v1.models.errors._
import v1.models.requestData.ListBFLossesRawData

class ListBFLossesValidatorSpec extends UnitSpec {

  private val validNino     = "AA123456A"
  private val validTaxYear  = "2021-22"
  private val validLossType = "self-employment"
  private val validSEId     = "XAIS01234567890"

  val validator = new ListBFLossesValidator

  "running validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
                                               taxYear = Some(validTaxYear),
                                               typeOfLoss = Some(validLossType),
                                               selfEmploymentId = Some(validSEId))) shouldBe Nil
      }
      "a self employment id is not supplied for a loss type of self-employment" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("self-employment"),
          selfEmploymentId = None)) shouldBe Nil
      }

      "a self employment id is supplied and no loss type is supplied" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = None,
          selfEmploymentId = Some(validSEId))) shouldBe Nil
      }
    }

    "return NinoFormatError" when {
      "the nino is supplied and invalid" in {
        validator.validate(ListBFLossesRawData(nino = "badNino",
                                               taxYear = Some(validTaxYear),
                                               typeOfLoss = Some(validLossType),
                                               selfEmploymentId = Some(validSEId))) shouldBe List(NinoFormatError)
      }
    }

    "return TaxYearFormatError" when {
      "the tax year is the incorrect format" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some("XXXX"),
          typeOfLoss = Some(validLossType),
          selfEmploymentId = Some(validSEId))) shouldBe List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearRangeExceededError" when {
      "the tax year range is not a single year" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some("2018-20"),
          typeOfLoss = Some(validLossType),
          selfEmploymentId = Some(validSEId))) shouldBe List(RuleTaxYearRangeInvalid)
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "the tax year is too early" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some("2017-18"),
          typeOfLoss = Some(validLossType),
          selfEmploymentId = Some(validSEId))) shouldBe List(RuleTaxYearNotSupportedError)
      }
    }

    "return TypeOfLossFormatError" when {
      "the loss type is invalid" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("badLossType"),
          selfEmploymentId = Some(validSEId))) shouldBe List(TypeOfLossFormatError)
      }

      // Because DES does not distinguish self-employment types for its query...
      "the loss type is self-employment-class4" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("self-employment-class4"),
          selfEmploymentId = Some(validSEId))) shouldBe List(TypeOfLossFormatError)
      }
    }

    "return SelfEmploymentIdFormatError" when {
      "the self employment id is invalid" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some(validLossType),
          selfEmploymentId = Some("badSEId"))) shouldBe List(SelfEmploymentIdFormatError)
      }
    }

    "return RuleSelfEmploymentId" when {
      "a self employment id is supplied for a loss type of non-fhl property" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("uk-property-non-fhl"),
          selfEmploymentId = Some(validSEId))) shouldBe List(RuleSelfEmploymentId)
      }
      "a self employment id is supplied for a loss type of fhl property" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("uk-property-fhl"),
          selfEmploymentId = Some(validSEId))) shouldBe List(RuleSelfEmploymentId)
      }
    }

    "not return a RuleSelfEmploymentId" when {
      "a self employment id is not supplied for a loss type of non-fhl property" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("uk-property-non-fhl"),
          selfEmploymentId = None)) shouldBe Nil
      }
      "a self employment id is not supplied for a loss type of fhl property" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("uk-property-fhl"),
          selfEmploymentId = None)) shouldBe Nil
      }
      "a self employment id is supplied for a loss type of self-employment" in {
        validator.validate(ListBFLossesRawData(nino = validNino,
          taxYear = Some(validTaxYear),
          typeOfLoss = Some("self-employment"),
          selfEmploymentId = Some(validSEId))) shouldBe Nil
      }
    }

    "return multiple errors" when {
      "there are multiple errors" in {
        validator.validate(ListBFLossesRawData(nino = "badNino",
          taxYear = Some("badTaxYear"),
          typeOfLoss = Some(validLossType),
          selfEmploymentId = Some(validSEId))) shouldBe List(NinoFormatError, TaxYearFormatError) }
    }
  }

}
