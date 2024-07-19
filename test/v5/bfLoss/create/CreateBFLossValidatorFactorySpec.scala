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

package v5.bfLoss.create

import api.models.utils.JsonErrorValidators
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v5.bfLossClaims.create.CreateBFLossValidatorFactory
import v5.bfLossClaims.create.def1.Def1_CreateBFLossValidator

class CreateBFLossValidatorFactorySpec extends UnitSpec with JsonErrorValidators {

  private val validNino        = "AA123456A"
  private val validLossAmount  = 1000

  def requestBodyJson(lossAmount: BigDecimal = validLossAmount): JsValue = Json.parse(
    s"""
       |{
       |  "lossAmount": "$lossAmount"
       |}
     """.stripMargin
  )

  private val validRequestBody = requestBodyJson()

  private val validatorFactory = new CreateBFLossValidatorFactory

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validatorFactory.validator(validNino, validRequestBody)
        result shouldBe a[Def1_CreateBFLossValidator]

      }
    }

  }

}
