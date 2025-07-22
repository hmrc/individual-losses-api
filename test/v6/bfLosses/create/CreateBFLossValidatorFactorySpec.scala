/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.bfLosses.create

import play.api.libs.json.{JsValue, Json}
import shared.controllers.validators.Validator
import shared.utils.UnitSpec
import v6.bfLosses.create.def1.Def1_CreateBFLossValidator
import v6.bfLosses.create.model.request.CreateBFLossRequestData

class CreateBFLossValidatorFactorySpec extends UnitSpec {

  private val requestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "typeOfLoss": "self-employment",
      |  "businessId": "XAIS01234567890",
      |  "taxYearBroughtForwardFrom": "2024-25",
      |  "lossAmount": 1000
      |}
    """.stripMargin
  )

  private val validatorFactory: CreateBFLossValidatorFactory = new CreateBFLossValidatorFactory

  "validator()" when {
    "given any tax year" should {
      "return the Validator for schema definition 1" in {
        val result: Validator[CreateBFLossRequestData] = validatorFactory.validator(
          nino = "AA123456A",
          taxYear = "2025-26",
          body = requestBodyJson,
          temporalValidationEnabled = true
        )

        result shouldBe a[Def1_CreateBFLossValidator]
      }
    }

  }

}
