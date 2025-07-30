/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.bfLosses.create.def1

import common.errors.TypeOfLossFormatError
import play.api.libs.json.{JsObject, JsValue, Json}
import shared.config.MockSharedAppConfig
import shared.controllers.validators.Validator
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors.*
import shared.utils.UnitSpec
import v6.bfLosses.common.domain.TypeOfLoss
import v6.bfLosses.create.def1.model.request.{Def1_CreateBFLossRequestBody, Def1_CreateBFLossRequestData}
import v6.bfLosses.create.model.request.CreateBFLossRequestData

import java.time.{Clock, Instant, ZoneId}

class Def1_CreateBFLossValidatorSpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino: String           = "AA123456A"
  private val validBusinessId: String     = "XAIS01234567890"
  private val validLossAmount: BigDecimal = 1000
  private val parsedNino: Nino            = Nino(validNino)
  private val parsedTaxYear: TaxYear      = TaxYear.fromMtd("2020-21")
  private val validTaxYear: String        = parsedTaxYear.asMtd

  private def requestBodyJson(typeOfLoss: String = "self-employment",
                              businessId: String = validBusinessId,
                              taxYearBroughtForwardFrom: String = validTaxYear,
                              lossAmount: BigDecimal = validLossAmount): JsValue =
    Json.parse(
      s"""
        |{
        |  "typeOfLoss": "$typeOfLoss",
        |  "businessId": "$businessId",
        |  "taxYearBroughtForwardFrom": "$taxYearBroughtForwardFrom",
        |  "lossAmount": $lossAmount
        |}
      """.stripMargin
    )

  private val validRequestBody: JsValue = requestBodyJson()

  private val parsedRequestBody: Def1_CreateBFLossRequestBody = Def1_CreateBFLossRequestBody(
    TypeOfLoss.`self-employment`,
    validBusinessId,
    validTaxYear,
    validLossAmount
  )

  class Test extends MockSharedAppConfig {

    implicit val clock: Clock = Clock.fixed(Instant.parse("2022-07-11T10:00:00.00Z"), ZoneId.of("UTC"))

    protected def validator(nino: String,
                            taxYear: String,
                            body: JsValue,
                            temporalValidationEnabled: Boolean = true): Validator[CreateBFLossRequestData] =
      new Def1_CreateBFLossValidator(nino, taxYear, body, temporalValidationEnabled)

  }

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, validRequestBody).validateAndWrapResult()

        result shouldBe Right(
          Def1_CreateBFLossRequestData(parsedNino, parsedTaxYear, parsedRequestBody)
        )
      }
    }

    "return NinoFormatError" when {
      "given an invalid nino" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator("BAD_NINO", validTaxYear, validRequestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return RuleIncorrectOrEmptyBodyError" when {
      "given an empty JSON body" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, JsObject.empty).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError)
        )
      }
    }

    "return TaxYearFormatError" when {
      "given an invalid tax year" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, requestBodyJson(taxYearBroughtForwardFrom = "2016")).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearFormatError.withPath("/taxYearBroughtForwardFrom"))
        )
      }

      "given a non-numeric tax year" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, requestBodyJson(taxYearBroughtForwardFrom = "XXXX-YY")).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearFormatError.withPath("/taxYearBroughtForwardFrom"))
        )
      }
    }

    "return RuleTaxYearRangeInvalidError" when {
      "given a tax year with a range greater than a year" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, requestBodyJson(taxYearBroughtForwardFrom = "2017-19")).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError.withPath("/taxYearBroughtForwardFrom")))
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "given an unsupported tax year" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, requestBodyJson(taxYearBroughtForwardFrom = "2015-16")).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError.withPath("/taxYearBroughtForwardFrom")))
      }
    }

    "return RuleTaxYearNotEndedError" when {
      "given a tax year that has not ended and temporal validation is enabled" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, requestBodyJson(taxYearBroughtForwardFrom = "2022-23")).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotEndedError.withPath("/taxYearBroughtForwardFrom")))
      }
    }

    "not return RuleTaxYearNotEndedError and skip its validation" when {
      "given a tax year that has not ended and temporal validation is disabled" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] = validator(
          validNino,
          validTaxYear,
          requestBodyJson(taxYearBroughtForwardFrom = "2022-23"),
          temporalValidationEnabled = false
        ).validateAndWrapResult()

        result shouldBe Right(
          Def1_CreateBFLossRequestData(parsedNino, parsedTaxYear, parsedRequestBody.copy(taxYearBroughtForwardFrom = "2022-23"))
        )
      }
    }

    "return TypeOfLossFormatError" when {
      "given an invalid loss type" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, requestBodyJson(typeOfLoss = "invalid")).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TypeOfLossFormatError.withPath("/typeOfLoss")))
      }
    }

    "return ValueFormatError" when {
      "given an invalid amount (Too high)" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, requestBodyJson(lossAmount = 100000000000.00)).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.withPath("/lossAmount")))
      }

      "given an invalid amount (Negative)" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, requestBodyJson(lossAmount = -100.00)).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.withPath("/lossAmount")))
      }

      "given an invalid amount (3 decimal places)" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, requestBodyJson(lossAmount = 100.734)).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.withPath("/lossAmount")))
      }
    }

    "return BusinessIdFormatError" when {
      "given an invalid business id" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, requestBodyJson(businessId = "invalid")).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, BusinessIdFormatError))
      }
    }

    "return TaxYearFormatError" when {
      "given an invalid tax year parameter" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, "BAD_TAX_YEAR", requestBodyJson()).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }
    }

    "return RuleTaxYearRangeInvalidError" when {
      "given an invalid tax year range in parameter" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, "2017-19", requestBodyJson()).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "given an unsupported tax year parameter" in new Test {
        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, "2015-16", requestBodyJson()).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }
    }

    "return multiple errors" when {
      "given a request with multiple errors" in new Test {
        val requestBody: JsValue =
          requestBodyJson(typeOfLoss = "self-employment-class4", businessId = "wrong", taxYearBroughtForwardFrom = "2010-11")

        val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, validTaxYear, requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(
              List(
                BusinessIdFormatError,
                RuleTaxYearNotSupportedError.withPath("/taxYearBroughtForwardFrom")
              )))
        )
      }
    }
  }

}
