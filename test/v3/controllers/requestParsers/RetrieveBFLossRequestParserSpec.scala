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

package v3.controllers.requestParsers

import api.models.domain.Nino
import api.models.errors._
import support.UnitSpec
import v3.controllers.requestParsers.validators.MockRetrieveBFLossValidator
import v3.models.request.retrieveBFLoss.{RetrieveBFLossRawData, RetrieveBFLossRequest}

class RetrieveBFLossRequestParserSpec extends UnitSpec {

  private val nino   = "AA123456A"
  private val lossId = "AAZZ1234567890a"
  private val data   = RetrieveBFLossRawData(nino, lossId)

  implicit val correlationId: String = "X-123"

  trait Test extends MockRetrieveBFLossValidator {
    lazy val parser = new RetrieveBFLossRequestParser(mockValidator)
  }

  "parse" should {
    "return a RetrieveBFLossRequest" when {
      "the validator returns no errors" in new Test {
        MockValidator.validate(data).returns(List())
        parser.parseRequest(data) shouldBe Right(RetrieveBFLossRequest(Nino(nino), lossId))
      }
    }
    "return a single error" when {
      "the validator returns a single error" in new Test {
        MockValidator.validate(data).returns(List(NinoFormatError))
        parser.parseRequest(data) shouldBe Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }
    }
    "return multiple errors" when {
      "the validator returns multiple errors" in new Test {
        MockValidator.validate(data).returns(List(NinoFormatError, LossIdFormatError))
        parser.parseRequest(data) shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, LossIdFormatError))))
      }
    }
  }

}
