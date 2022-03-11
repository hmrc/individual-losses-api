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

package v3.controllers.requestParsers

import api.models.domain.Nino
import api.models.errors._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v3.mocks.validators.MockAmendBFLossValidator
import v3.models.errors.RuleIncorrectOrEmptyBodyError
import v3.models.request.amendBFLoss.{AmendBFLossRawData, AmendBFLossRequest, AmendBFLossRequestBody}

class AmendBFLossParserSpec extends UnitSpec {

  private val nino       = "AA123456A"
  private val lossId     = "AAZZ1234567890a"
  private val lossAmount = 3.00
  private val data       = AmendBFLossRawData(nino, lossId, AnyContentAsJson(Json.obj("lossAmount" -> lossAmount)))

  trait Test extends MockAmendBFLossValidator {
    lazy val parser = new AmendBFLossParser(mockValidator)
  }

  "parse" should {
    "return an AmendBFLossRequest" when {
      "the validator returns no errors" in new Test {
        MockValidator.validate(data).returns(List())
        parser.parseRequest(data) shouldBe Right(AmendBFLossRequest(Nino(nino), lossId, AmendBFLossRequestBody(lossAmount)))
      }
    }
    "return a single error" when {
      "the validator returns a single error" in new Test {
        MockValidator.validate(data).returns(List(NinoFormatError))
        parser.parseRequest(data) shouldBe Left(ErrorWrapper(None, NinoFormatError, None))
      }
    }
    "return multiple errors" when {
      "the validator returns multiple errors" in new Test {
        MockValidator.validate(data).returns(List(NinoFormatError, RuleIncorrectOrEmptyBodyError))
        parser.parseRequest(data) shouldBe Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, RuleIncorrectOrEmptyBodyError))))
      }
    }
  }
}
