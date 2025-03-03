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

package v6.lossClaims.amendType

import play.api.libs.json.{JsValue, Json}
import shared.utils.UnitSpec
import v6.lossClaims.amendType.def1.Def1_AmendLossClaimTypeValidator

class AmendLossClaimTypeValidatorFactorySpec extends UnitSpec {

  private val validNino    = "AA123456A"
  private val validClaimId = "AAZZ1234567890a"
  private val taxYear      = "2019-20"

  private def requestBodyJson(claimType: String = "carry-forward"): JsValue = Json.obj("typeOfClaim" -> claimType)
  private val validRequestBody: JsValue                                     = requestBodyJson()

  private val validatorFactory = new AmendLossClaimTypeValidatorFactory

  "Amend Loss Claim Validator" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validatorFactory.validator(validNino, validClaimId, validRequestBody, taxYear)
        result shouldBe a[Def1_AmendLossClaimTypeValidator]
      }
    }

  }

}
