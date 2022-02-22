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

import support.UnitSpec
import v3.mocks.validators.MockDeleteBFLossValidator
import v3.models.domain.Nino
import v3.models.errors.{BadRequestError, ErrorWrapper, LossIdFormatError, NinoFormatError}
import v3.models.request.deleteBFLoss.{DeleteBFLossRawData, DeleteBFLossRequest}

class DeleteBFLossParserSpec extends UnitSpec{
  val nino: String = "AA123456B"
  val lossId: String = "someLossId"

  val inputData: DeleteBFLossRawData = DeleteBFLossRawData(nino, lossId)

  trait Test extends MockDeleteBFLossValidator {
    lazy val parser = new DeleteBFLossParser(mockValidator)
  }

  "parse" should {

    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockValidator.validate(inputData).returns(Nil)

        parser.parseRequest(inputData) shouldBe
          Right(DeleteBFLossRequest(Nino(nino), lossId))
      }
    }

    "return an ErrorWrapper" when {

      "a single validation error occurs" in new Test {
        MockValidator.validate(inputData)
          .returns(List(NinoFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "multiple validation errors occur" in new Test {
        MockValidator.validate(inputData)
          .returns(List(NinoFormatError, LossIdFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, LossIdFormatError))))
      }
    }
  }
}
