/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.createAmend

import org.scalactic.Prettifier.default
import play.api.libs.json.*
import shared.models.domain.{BusinessId, Nino, TaxYear}
import shared.models.errors.*
import shared.models.utils.JsonErrorValidators
import shared.utils.UnitSpec
import v7.lossesAndClaims.createAmend.fixtures.CreateAmendLossesAndClaimsFixtures.{requestBodyJson, requestBodyModel}
import v7.lossesAndClaims.createAmend.request.*

import scala.math.Ordering.Implicits.infixOrderingOps

class CreateAmendLossesAndClaimsValidatorFactorySpec extends UnitSpec with JsonErrorValidators {

  private val validNino: String       = "AA123456A"
  private val validBusinessId: String = "X0IS12345678901"
  private val validTaxYear: String    = "2026-27"

  private implicit val correlationId: String = "1234"

  private val validatorFactory: CreateAmendLossesAndClaimsValidationFactory = new CreateAmendLossesAndClaimsValidationFactory

  private val parsedNino: Nino             = Nino(validNino)
  private val parsedBusinessId: BusinessId = BusinessId(validBusinessId)
  private val parsedTaxYear: TaxYear       = TaxYear.fromMtd(validTaxYear)

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request with temporal validation disabled" in {
        val result = validatorFactory.validator(validNino, validBusinessId, validTaxYear, requestBodyJson, false).validateAndWrapResult()

        result shouldBe Right(
          CreateAmendLossesAndClaimsRequestData(parsedNino, parsedBusinessId, parsedTaxYear, requestBodyModel)
        )
      }
    }

    "return NinoFormatError" when {
      "given a badly formatted nino" in {
        val result = validatorFactory.validator("validNino", validBusinessId, validTaxYear, requestBodyJson, false).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }
    }

    "return TaxYearFormatError" when {
      "given a badly formatted tax year" in {
        val result = validatorFactory.validator(validNino, validBusinessId, "2026", requestBodyJson, false).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }
    }

    "return BusinessIdFormatError" when {
      "given a badly formatted business ID" in {
        val result = validatorFactory.validator(validNino, "invalidBusinessId", validTaxYear, requestBodyJson, false).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, BusinessIdFormatError))
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "given an unsupported tax year" in {
        val result = validatorFactory.validator(validNino, validBusinessId, "2025-26", requestBodyJson, false).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }
    }

    "return RuleTaxYearRangeInvalidError" when {
      "given a tax year with an invalid range" in {
        val result = validatorFactory.validator(validNino, validBusinessId, "2025-27", requestBodyJson, false).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }
    }

    "return RuleTaxYearNotEndedError" when {
      "given a tax year that has not ended with temporal validation enabled" in {
        val notEndedTaxYear = parsedTaxYear.max(TaxYear.currentTaxYear)

        val result = validatorFactory.validator(validNino, validBusinessId, notEndedTaxYear.asMtd, requestBodyJson, true).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotEndedError))
      }
    }

    "return PreferenceOrderFormatError" when {
      "given a json body with a badly formatted preference order" in {
        val invalidJson = requestBodyJson.update("/claims/preferenceOrder/applyFirst", JsString("carry-bag"))

        val result = validatorFactory.validator(validNino, validBusinessId, validTaxYear, invalidJson, false).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, PreferenceOrderFormatError.withPath("/claims/preferenceOrder/applyFirst")))
      }
    }

    "return ValueFormatError" when {
      Seq(
        "/claims/carryBack/previousYearGeneralIncome",
        "/claims/carryBack/earlyYearLosses",
        "/claims/carryBack/terminalLosses",
        "/claims/carrySideways/currentYearGeneralIncome",
        "/claims/carryForward/currentYearLosses",
        "/claims/carryForward/previousYearsLosses",
        "/losses/broughtForwardLosses"
      ).foreach { path =>
        s"given a json body with a badly formatted value for field $path" in {
          val invalidJsonBase: JsValue = requestBodyJson.update(path, JsNumber(-5000.99))

          val invalidJson: JsValue = if (path.endsWith("terminalLosses")) {
            invalidJsonBase.removeProperty("/claims/carryForward")
          } else {
            invalidJsonBase
          }

          val result = validatorFactory.validator(validNino, validBusinessId, validTaxYear, invalidJson, false).validateAndWrapResult()

          result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.withPath(path)))
        }
      }
    }

    "return RuleIncorrectOrEmptyBodyError" when {
      "given an empty json body" in {
        val result = validatorFactory.validator(validNino, validBusinessId, validTaxYear, JsObject.empty, false).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      requestBodyJson.collectPaths().foreach { path =>
        s"given a json body with an incorrect type for field $path" in {
          val invalidJson = requestBodyJson.update(path, JsBoolean(true))

          val result = validatorFactory.validator(validNino, validBusinessId, validTaxYear, invalidJson, false).validateAndWrapResult()

          result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath(path)))
        }
      }
    }

    "return RulePreferenceOrderNotAllowedError" when {
      "given a json body with preference order specified but no previousYearGeneralIncome and currentYearGeneralIncome supplied" in {
        val invalidJson = requestBodyJson
          .removeProperty("/claims/carryBack/previousYearGeneralIncome")
          .removeProperty("/claims/carrySideways")

        val result = validatorFactory.validator(validNino, validBusinessId, validTaxYear, invalidJson, false).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RulePreferenceOrderNotAllowedError.withPath("/claims/preferenceOrder/applyFirst")))
      }
    }

    "return RuleMissingPreferenceOrderError" when {
      "given a json body with previousYearGeneralIncome and currentYearGeneralIncome supplied but no preference order specified" in {
        val invalidJson = requestBodyJson.removeProperty("/claims/preferenceOrder")

        val result = validatorFactory.validator(validNino, validBusinessId, validTaxYear, invalidJson, false).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleMissingPreferenceOrderError.withPath("/claims")))
      }
    }

    "return RuleCarryForwardAndTerminalLossNotAllowedError" when {
      "given a json body with carry forward and terminal loss claims supplied" in {
        val invalidJson = requestBodyJson.update("/claims/carryBack/terminalLosses", JsNumber(5000.99))

        val result = validatorFactory.validator(validNino, validBusinessId, validTaxYear, invalidJson, false).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleCarryForwardAndTerminalLossNotAllowedError.withPath("/claims")))
      }
    }
  }

}
