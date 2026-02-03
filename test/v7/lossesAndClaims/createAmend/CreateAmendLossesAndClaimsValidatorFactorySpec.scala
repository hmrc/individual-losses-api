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

import play.api.libs.json.{JsValue, Json}
import shared.models.domain.{BusinessId, Nino, TaxYear}
import shared.models.errors.*
import shared.models.utils.JsonErrorValidators
import shared.utils.UnitSpec
import v7.lossesAndClaims.commons.PreferenceOrderEnum.`carry-sideways`
import v7.lossesAndClaims.commons.{Losses, PreferenceOrder}
import v7.lossesAndClaims.createAmend.request.*

class CreateAmendLossesAndClaimsValidatorFactorySpec extends UnitSpec with JsonErrorValidators {

  private val validNino       = "AA123456A"
  private val validBusinessId = "X0IS12345678901"
  private val validTaxYear    = "2026-27"

  private implicit val correlationId: String = "1234"
  private val validatorFactory               = new CreateAmendLossesAndClaimsValidationFactory

  private val parsedNino       = Nino(validNino)
  private val parsedBusinessId = BusinessId(validBusinessId)
  private val parsedTaxYear    = TaxYear.fromMtd(validTaxYear)

  val defaultRequestJson: JsValue = Json.parse(s"""
                                                  |{
                                                  |  "claims": {
                                                  |    "carryBack": {
                                                  |      "previousYearGeneralIncome": 5000.99,
                                                  |      "earlyYearLosses": 5000.99
                                                  |    },
                                                  |    "carrySideways": {
                                                  |      "currentYearGeneralIncome": 5000.99
                                                  |    },
                                                  |    "preferenceOrder": {
                                                  |      "applyFirst": "carry-sideways"
                                                  |    },
                                                  |    "carryForward": {
                                                  |      "currentYearLosses": 5000.99,
                                                  |      "previousYearsLosses": 5000.99
                                                  |    }
                                                  |  },
                                                  |  "losses": {
                                                  |    "broughtForwardLosses": 5000.99
                                                  |  }
                                                  |}
            """.stripMargin)

  val emptyBody: JsValue = Json.parse(s"""
       |{}
       |""".stripMargin)

  val invalidFieldValueDoubleRequestJson: JsValue = Json.parse(s"""
       |{
       |  "claims": {
       |    "carryBack": {
       |      "previousYearGeneralIncome": "something",
       |      "earlyYearLosses": 5000.99
       |    },
       |    "carrySideways": {
       |      "currentYearGeneralIncome": "something"
       |    },
       |    "preferenceOrder": {
       |      "applyFirst": "carry-sideways"
       |    },
       |    "carryForward": {
       |      "currentYearLosses": 5000.99,
       |      "previousYearsLosses": 5000.99
       |    }
       |  },
       |  "losses": {
       |    "broughtForwardLosses": 5000.99
       |  }
       |}
              """.stripMargin)

  val invalidPreferenceOrderRequestJson: JsValue = Json.parse(s"""
       |{
       |  "claims": {
       |    "preferenceOrder": {
       |      "applyFirst": "carry-sideways"
       |    },
       |    "carryForward": {
       |      "currentYearLosses": 5000.99,
       |      "previousYearsLosses": 5000.99
       |    }
       |  },
       |  "losses": {
       |    "broughtForwardLosses": 5000.99
       |  }
       |}
                """.stripMargin)

  val invalidPreferenceOrderValueRequestJson: JsValue = Json.parse(s"""
       |{
       |  "claims": {
       |    "carryBack": {
       |      "previousYearGeneralIncome": 5000.99,
       |      "earlyYearLosses": 5000.99
       |    },
       |    "carrySideways": {
       |      "currentYearGeneralIncome": 5000.99
       |    },
       |    "preferenceOrder": {
       |      "applyFirst": "somthing"
       |    },
       |    "carryForward": {
       |      "currentYearLosses": 5000.99,
       |      "previousYearsLosses": 5000.99
       |    }
       |  },
       |  "losses": {
       |    "broughtForwardLosses": 5000.99
       |  }
       |}
       """.stripMargin)

  val invalidRuleCarryForwardAndTerminalLossNotAllowed: JsValue = Json.parse(s"""
       |{
       |  "claims": {
       |    "carryBack": {
       |      "previousYearGeneralIncome": 5000.99,
       |      "earlyYearLosses": 5000.99,
       |      "terminalLosses": 5000.99
       |    },
       |    "carrySideways": {
       |      "currentYearGeneralIncome": 5000.99
       |    },
       |    "preferenceOrder": {
       |      "applyFirst": "carry-sideways"
       |    },
       |    "carryForward": {
       |      "currentYearLosses": 5000.99,
       |      "previousYearsLosses": 5000.99
       |    }
       |  },
       |  "losses": {
       |    "broughtForwardLosses": 5000.99
       |  }
       |}
       """.stripMargin)

  val invalidPreferenceOrderNotSetRequestJson: JsValue = Json.parse(s"""
       |{
       |  "claims": {
       |    "carryBack": {
       |      "previousYearGeneralIncome": 5000.99,
       |      "earlyYearLosses": 5000.99
       |    },
       |    "carrySideways": {
       |      "currentYearGeneralIncome": 5000.99
       |    },
       |    "carryForward": {
       |      "currentYearLosses": 5000.99,
       |      "previousYearsLosses": 5000.99
       |    }
       |  },
       |  "losses": {
       |    "broughtForwardLosses": 5000.99
       |  }
       |}
                """.stripMargin)

  val createAmendLossesAndClaimsRequestBody: CreateAmendLossesAndClaimsRequestBody = CreateAmendLossesAndClaimsRequestBody(
    Option(
      Claims(
        Option(
          CarryBack(
            Option(5000.99),
            Option(5000.99),
            None
          )),
        Option(
          CarrySideways(
            Option(5000.99)
          )),
        Option(
          PreferenceOrder(
            Option(`carry-sideways`)
          )),
        Option(
          CarryForward(
            Option(5000.99),
            Option(5000.99)
          ))
      )),
    Option(
      Losses(
        Option(5000.99)
      ))
  )

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validatorFactory.validator(validNino, validBusinessId, validTaxYear, defaultRequestJson, false).validateAndWrapResult()
        result shouldBe Right(
          CreateAmendLossesAndClaimsRequestData(parsedNino, parsedBusinessId, parsedTaxYear, createAmendLossesAndClaimsRequestBody))
      }
    }

    "return NinoFormatError" when {
      "given a tax year passed an unsupported tax year" in {
        val result = validatorFactory.validator("validNino", validBusinessId, "2026-27", defaultRequestJson, false).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return BusinessIdFormatError" when {
      "given a tax year passed an unsupported tax year" in {
        val result = validatorFactory.validator(validNino, "invalidBusinessId", "2026-27", defaultRequestJson, false).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BusinessIdFormatError)
        )
      }
    }

    "return RuleIncorrectOrEmptyBodyError" when {
      "given a tax year passed an unsupported tax year" in {
        val result = validatorFactory.validator(validNino, validBusinessId, "2025-26", emptyBody, false).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            RuleIncorrectOrEmptyBodyError.withPaths(List(
              "/claims/carryBack/previousYearGeneralIncome",
              "/claims/carryBack/earlyYearLosses",
              "/claims/carryBack/terminalLosses",
              "/claims/carrySideways/currentYearGeneralIncome",
              "/claims/carryForward/currentYearLosses",
              "/claims/carryForward/previousYearsLosses",
              "/losses/broughtForwardLosses",
              "/claims/preferenceOrder"
            ))
          ))
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "given a tax year passed an unsupported tax year" in {
        val result = validatorFactory.validator(validNino, validBusinessId, "2025-26", defaultRequestJson, false).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotSupportedError)
        )
      }
    }

    "return RuleTaxYearRangeInvalid" when {
      "given a tax year range which isn't a single year" in {
        val result = validatorFactory.validator(validNino, validBusinessId, "2025-27", defaultRequestJson, false).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError)
        )
      }
    }

    "return RuleTaxYearNotEndedError" when {
      "given a tax year range which isn't a single year" in {
        val result = validatorFactory
          .validator(validNino, validBusinessId, "2026-27", defaultRequestJson, temporalValidationEnabled = true)
          .validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotEndedError)
        )
      }
    }

    "return ValueFormatError" when {
      "given a json body not valid" in {
        val result =
          validatorFactory.validator(validNino, validBusinessId, "2026-27", invalidFieldValueDoubleRequestJson, false).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            ValueFormatError.withPaths(List("/claims/carryBack/previousYearGeneralIncome", "/claims/carrySideways/currentYearGeneralIncome")))
        )
      }
    }

    "return RulePreferenceOrderNotAllowed" when {
      "given a json body not valid" in {
        val result =
          validatorFactory.validator(validNino, validBusinessId, "2026-27", invalidPreferenceOrderRequestJson, false).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RulePreferenceOrderNotAllowed.withPaths(List("/claims/preferenceOrder"))))
      }
    }

    "return RuleMissingPreferenceOrder" when {
      "given a json body not valid" in {
        val result =
          validatorFactory.validator(validNino, validBusinessId, "2026-27", invalidPreferenceOrderNotSetRequestJson, false).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleMissingPreferenceOrder.withPaths(List("/claims/preferenceOrder"))))
      }
    }

    "return FormatPreferenceOrder" when {
      "given a json body not valid" in {
        val result =
          validatorFactory.validator(validNino, validBusinessId, "2026-27", invalidPreferenceOrderValueRequestJson, false).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, FormatPreferenceOrder.withPaths(List("/claims/preferenceOrder"))))
      }
    }

    "return RuleCarryForwardAndTerminalLossNotAllowed" when {
      "given a json body not valid" in {
        val result =
          validatorFactory
            .validator(validNino, validBusinessId, "2026-27", invalidRuleCarryForwardAndTerminalLossNotAllowed, false)
            .validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            RuleCarryForwardAndTerminalLossNotAllowed.withPaths(List("/claims/carryBack/terminalLosses", "/claims/carryForward"))))
      }
    }
  }

}
