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

package v5.bfLosses.create.def1

import api.controllers.validators.Validator
import api.models.domain.{Nino, TodaySupplier}
import api.models.errors._
import config.{AppConfig, MockAppConfig}
import play.api.libs.json.{JsObject, JsValue, Json}
import support.UnitSpec
import v5.bfLosses.common.domain.TypeOfLoss
import v5.bfLosses.create.def1.model.request.{Def1_CreateBFLossRequestBody, Def1_CreateBFLossRequestData}
import v5.bfLosses.create.model.request.CreateBFLossRequestData

import java.time.LocalDate

class Def1_CreateBFLossValidatorSpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino       = "AA123456A"
  private val invalidNino     = "AA123456"
  private val validTaxYear    = "2020-21"
  private val validTypeOfLoss = "self-employment"
  private val validBusinessId = "XAIS01234567890"

  private val parsedNino = Nino(validNino)

  private val emptyBody = JsObject.empty

  private def requestBodyJson(typeOfLoss: String = validTypeOfLoss,
                              businessId: String = validBusinessId,
                              taxYearBroughtForwardFrom: String = validTaxYear,
                              lossAmount: BigDecimal = 1000): JsValue = Json.parse(
    s"""{
       |  "typeOfLoss" : "$typeOfLoss",
       |  "businessId" : "$businessId",
       |  "taxYearBroughtForwardFrom" : "$taxYearBroughtForwardFrom",
       |  "lossAmount" : $lossAmount
       |}""".stripMargin
  )

  private val validRequestBody  = requestBodyJson()
  private val parsedRequestBody = Def1_CreateBFLossRequestBody(TypeOfLoss.`self-employment`, validBusinessId, validTaxYear, 1000)

  class Test extends MockAppConfig {
    implicit val appConfig: AppConfig = mockAppConfig

    implicit val todaySupplier: TodaySupplier = new TodaySupplier {
      override def today(): LocalDate = LocalDate.parse("2022-07-11")
    }

    protected def validator(nino: String, body: JsValue): Validator[CreateBFLossRequestData] = new Def1_CreateBFLossValidator(nino, body)
  }

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in new Test {
        private val result = validator(validNino, validRequestBody).validateAndWrapResult()
        result shouldBe Right(
          Def1_CreateBFLossRequestData(parsedNino, parsedRequestBody)
        )
      }
    }

    "return NinoFormatError" when {
      "given an invalid nino" in new Test {
        private val result = validator(invalidNino, validRequestBody).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return IncorrectOrEmptyBodySubmitted" when {
      "given an empty JSON body" in new Test {
        private val result = validator(validNino, emptyBody).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError)
        )
      }
    }

    "return TaxYearFormatError" when {
      "given an invalid tax year" in new Test {
        private val result = validator(validNino, requestBodyJson(taxYearBroughtForwardFrom = "2016")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearFormatError.withPath("/taxYearBroughtForwardFrom"))
        )
      }

      "given a non-numeric tax year" in new Test {
        private val result = validator(validNino, requestBodyJson(taxYearBroughtForwardFrom = "XXXX-YY")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearFormatError.withPath("/taxYearBroughtForwardFrom"))
        )
      }
    }

    "return RuleTaxYearRangeInvalidError" when {
      "given a tax year with a range greater than a year" in new Test {
        private val result = validator(validNino, requestBodyJson(taxYearBroughtForwardFrom = "2017-19")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError.withPath("/taxYearBroughtForwardFrom"))
        )
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "given an out of range tax year" in new Test {
        private val result = validator(validNino, requestBodyJson(taxYearBroughtForwardFrom = "2015-16")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotSupportedError.withPath("/taxYearBroughtForwardFrom"))
        )
      }
    }

    "return RuleTaxYearNotEndedError" when {
      "given an out of range tax year" in new Test {
        private val result = validator(validNino, requestBodyJson(taxYearBroughtForwardFrom = "2022-23")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotEndedError.withPath("/taxYearBroughtForwardFrom"))
        )
      }
    }

    "return TypeOfLossFormatError" when {
      "given an invalid loss type" in new Test {
        private val result = validator(validNino, requestBodyJson(typeOfLoss = "invalid")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TypeOfLossFormatError.withPath("/typeOfLoss"))
        )
      }
    }

    "return ValueFormatError" when {
      "given an invalid amount (Too high)" in new Test {
        private val result = validator(validNino, requestBodyJson(lossAmount = 100000000000.00)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, ValueFormatError.withPath("/lossAmount"))
        )
      }

      "given an invalid amount (Negative)" in new Test {
        private val result = validator(validNino, requestBodyJson(lossAmount = -100.00)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, ValueFormatError.withPath("/lossAmount"))
        )
      }

      "given an invalid amount (3 decimal places)" in new Test {
        private val result = validator(validNino, requestBodyJson(lossAmount = 100.734)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, ValueFormatError.withPath("/lossAmount"))
        )
      }
    }

    "return BusinessIdFormatError error" when {
      "given an invalid business id" in new Test {
        private val result = validator(validNino, requestBodyJson(businessId = "invalid")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BusinessIdFormatError)
        )
      }
    }

    "return multiple errors" when {
      "given a request with multiple errors" in new Test {
        private val requestBody =
          requestBodyJson(typeOfLoss = "self-employment-class4", businessId = "wrong", taxYearBroughtForwardFrom = "2010-11")

        private val result: Either[ErrorWrapper, CreateBFLossRequestData] =
          validator(validNino, requestBody).validateAndWrapResult()

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