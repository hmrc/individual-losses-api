/*
 * Copyright 2022 HM Revenue & Customs
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

package api.validations.anyVersion

import api.models.errors.MtdError
import support.UnitSpec

class RegexValidationSpec extends UnitSpec {

  object TestError extends MtdError("SOME_ERR", "some message")

  object TestValidator extends RegexValidation {
    override protected val regexFormat           = "[a-z]{3}[0-9]{3}"
    override protected val error: TestError.type = TestError
  }

  "RegexValidation" when {
    "string matches regex" must {
      "return no errors" in {
        TestValidator.validate("abc123") shouldBe empty
      }
    }

    "string does not match regex" must {
      "return the required error" in {
        TestValidator.validate(" abc123") shouldBe List(TestError)
        TestValidator.validate("abc123 ") shouldBe List(TestError)
        TestValidator.validate("abc1234") shouldBe List(TestError)
        TestValidator.validate("abcd123") shouldBe List(TestError)
        TestValidator.validate("123abc") shouldBe List(TestError)
        TestValidator.validate("") shouldBe List(TestError)
      }
    }

    "string does not match regex and path specicied" must {
      "return the required error with a path" in {
        TestValidator.validate(" abc123", "/somePath") shouldBe List(TestError.copy(paths = Some(Seq("/somePath"))))
      }
    }
  }
}
