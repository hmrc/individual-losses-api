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

package unit.uk.gov.hmrc.hello.controllers

import org.scalatest.Matchers
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.hello.controllers.HeaderValidator
import uk.gov.hmrc.play.test.UnitSpec

class HeaderValidatorSpec extends UnitSpec with Matchers {

  val validator = new HeaderValidator(stubControllerComponents())

  "acceptHeaderValidationRules" should {

    "return false when the header value is missing" in {
      validator.acceptHeaderValidationRules(None) shouldBe false
    }

    "return true when the version and the content type in header value is well formatted" in {
      validator.acceptHeaderValidationRules(Some("application/vnd.hmrc.1.0+json")) shouldBe true
      validator.acceptHeaderValidationRules(Some("application/vnd.hmrc.2.0+json")) shouldBe true
    }

    "return true when the version and the content type in header value is well formatted xml" in {
      validator.acceptHeaderValidationRules(Some("application/vnd.hmrc.1.0+xml")) shouldBe true
      validator.acceptHeaderValidationRules(Some("application/vnd.hmrc.2.0+xml")) shouldBe true
    }

    "return false when the content type in header value is missing" in {
      validator.acceptHeaderValidationRules(Some("application/vnd.hmrc.1.0")) shouldBe false
    }

    "return false when the content type in header value is not well formatted" in {
      validator.acceptHeaderValidationRules(Some("application/vnd.hmrc.v1+json")) shouldBe false
    }

    "return false when the content type in header value is not valid" in {
      validator.acceptHeaderValidationRules(Some("application/vnd.hmrc.notvalid+XML")) shouldBe false
    }

    "return false when the version in header value is not valid" in {
      validator.acceptHeaderValidationRules(Some("application/vnd.hmrc.notvalid+json")) shouldBe false
    }

  }
}

