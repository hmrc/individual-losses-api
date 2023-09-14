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

package api.controllers.validators.resolvers

import api.models.errors.{MtdError, ValueFormatError}
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import support.UnitSpec

class ResolverSpec extends UnitSpec {

  object ResolveValid extends Resolver[String, Int] {

    def apply(value: String, error: Option[MtdError], path: Option[String]): Validated[Seq[MtdError], Int] = {
      Valid(123)
    }

  }

  object ResolveInvalid extends Resolver[String, Int] {

    def apply(value: String, error: Option[MtdError], path: Option[String]): Validated[Seq[MtdError], Int] = {
      Invalid(List(error.getOrElse(ValueFormatError.maybeWithPath(path))))
    }

  }

  "apply()" should {
    "return the parsed type" when {

      "given a valid value" in {
        val result = ResolveValid("123")
        result shouldBe Valid(123)
      }

      "given a valid value in an Option" in {
        val result = ResolveValid(Some("123"))
        result shouldBe Valid(Some(123))
      }

      "given a None with a default value" in {
        val result = ResolveValid(None, 456)
        result shouldBe Valid(456)
      }
    }

    "return an error" when {
      "given an invalid value" in {
        val result = ResolveInvalid("not-a-number")
        result shouldBe Invalid(List(ValueFormatError))
      }

      "given an invalid value in an Option" in {
        val result = ResolveInvalid(Some("not-a-number"))
        result shouldBe Invalid(List(ValueFormatError))
      }

      "given an invalid value and a path" in {
        val result = ResolveInvalid("not-a-number", path = Some("over-here"))
        result shouldBe Invalid(List(ValueFormatError.withPath("over-here")))
      }

    }
  }

}
