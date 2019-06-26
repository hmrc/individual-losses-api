/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v1.models.errors._
import v1.models.requestData.AmendBFLossRawData

class AmendBFLossValidatorSpec extends UnitSpec {

  private val validNino                     = "AA123456A"
  private val invalidNino                   = "AA123456"
  private val validLossAmount               = 3.00
  private val minimumLossAmount             = 0.00
  private val maximumLossAmount             = 99999999999.99
  private val invalidLossAmountFormat       = 99999999999.999
  private val invalidLossAmountRuleUnderMin = -0.01
  private val invalidLossAmountRuleOverMax  = 100000000000.00

  private val amendBFLossRawData: (String, BigDecimal) => AmendBFLossRawData = (nino, lossAmount) =>
    AmendBFLossRawData(nino, AnyContentAsJson(Json.obj("lossAmount" -> lossAmount)))

  val validator = new AmendBFLossValidator

  "amend validation" should {
    "return no errors" when {
      "supplied with a valid nino and a valid loss amount" in {
        validator.validate(amendBFLossRawData(validNino, validLossAmount)) shouldBe List()
      }
      "supplied with a valid nino and the minimum loss amount" in {
        validator.validate(amendBFLossRawData(validNino, minimumLossAmount)) shouldBe List()
      }
      "supplied with a valid nino and the maximum loss amount" in {
        validator.validate(amendBFLossRawData(validNino, maximumLossAmount)) shouldBe List()
      }
    }
    "return a FORMAT_NINO error" when {
      "the provided nino is invalid" in {
        validator.validate(amendBFLossRawData(invalidNino, validLossAmount)) shouldBe List(NinoFormatError)
      }
    }
    "return a RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED error" when {
      "a body without a lossAmount field is submitted" in {
        validator.validate(AmendBFLossRawData(nino = validNino, body = AnyContentAsJson(Json.obj()))) shouldBe List(RuleIncorrectOrEmptyBodyError)
      }
    }
    "return a FORMAT_LOSS_AMOUNT error" when {
      "a lossAmount greater than 2 decimal places is submitted" in {
        validator.validate(amendBFLossRawData(validNino, invalidLossAmountFormat)) shouldBe List(AmountFormatError)
      }
    }
    "return a RULE_LOSS_AMOUNT error" when {
      "a lossAmount less than the minimum allowed value is submitted" in {
        validator.validate(amendBFLossRawData(validNino, invalidLossAmountRuleUnderMin)) shouldBe List(RuleInvalidLossAmount)
      }
      "a lossAmount greater than the maximum allowed value is submitted" in {
        validator.validate(amendBFLossRawData(validNino, invalidLossAmountRuleOverMax)) shouldBe List(RuleInvalidLossAmount)
      }
    }
  }

}
