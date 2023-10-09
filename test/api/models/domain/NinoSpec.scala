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

package api.models.domain

import support.UnitSpec

class NinoSpec extends UnitSpec {

  "The validation of a nino" should {
    "pass with valid number without spaces" in { validateNino("AB123456C") shouldBe true }
    "pass with valid number with spaces" in { validateNino("AB 12 34 56 C") shouldBe true }
    "fail with valid number with leading space" in { validateNino(" AB123456C") shouldBe false }
    "fail with valid number with trailing space" in { validateNino("AB123456C ") shouldBe false }
    "fail with empty string" in { validateNino("") shouldBe false }
    "fail with only space" in { validateNino("    ") shouldBe false }
    "fail with total garbage" in {
      validateNino("XXX") shouldBe false
      validateNino("werionownadefwe") shouldBe false
      validateNino("@£%!)(*&^") shouldBe false
      validateNino("123456") shouldBe false
    }
    "fail with only one starting letter" in {
      validateNino("A123456C") shouldBe false
      validateNino("A1234567C") shouldBe false
    }
    "fail with three starting letters" in {
      validateNino("ABC12345C") shouldBe false
      validateNino("ABC123456C") shouldBe false
    }
    "fail with lowercase letters" in {
      validateNino("ab123456c") shouldBe false
    }
    "fail with less than 6 middle digits" in { validateNino("AB12345C") shouldBe false }
    "fail with more than 6 middle digits" in { validateNino("AB1234567C") shouldBe false }

    "fail if we start with invalid characters" in {
      val invalidStartLetterCombinations = List('D', 'F', 'I', 'Q', 'U', 'V').combinations(2).map(_.mkString("")).toList
      val invalidPrefixes                = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")
      for (v <- invalidStartLetterCombinations ::: invalidPrefixes) {
        validateNino(v + "123456C") shouldBe false
      }
    }

    "fail if the second letter O" in {
      validateNino("AO123456C") shouldBe false
    }

    "fail if the suffix is E" in {
      validateNino("AB123456E") shouldBe false
    }
  }

  "Formatting a Nino" should {
    "produce a formatted nino" in {
      Nino("CS100700A").formatted shouldBe "CS 10 07 00 A"
    }
  }

  "Removing a suffix" should {
    "produce a nino without a suffix" in {
      Nino("AA111111A").withoutSuffix shouldBe "AA111111"
    }
  }

  def validateNino(nino: String): Boolean = Nino.isValid(nino)
}
