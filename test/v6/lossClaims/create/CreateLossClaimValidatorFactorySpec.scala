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

package v6.lossClaims.create

import shared.models.utils.JsonErrorValidators
import play.api.libs.json.{JsValue, Json}
import shared.utils.UnitSpec
import v6.lossClaims.create.def1.Def1_CreateLossClaimValidator

class CreateLossClaimValidatorFactorySpec extends UnitSpec with JsonErrorValidators {

  private val validNino        = "AA123456A"
  private val validTaxYear     = "2019-20"
  private val validTypeOfLoss  = "self-employment"
  private val validTypeOfClaim = "carry-forward"
  private val validBusinessId  = "XAIS01234567890"

  def requestBodyJson(typeOfLoss: String = validTypeOfLoss,
                      businessId: String = validBusinessId,
                      typeOfClaim: String = validTypeOfClaim,
                      taxYearClaimedFor: String = validTaxYear): JsValue = Json.parse(
    s"""
       |{
       |  "typeOfLoss" : "$typeOfLoss",
       |  "businessId" : "$businessId",
       |  "typeOfClaim" : "$typeOfClaim",
       |  "taxYearClaimedFor" : "$taxYearClaimedFor"
       |}
     """.stripMargin
  )

  private val validRequestBody = requestBodyJson()

  private val validatorFactory = new CreateLossClaimValidatorFactory

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validatorFactory.validator(validNino, validRequestBody)
        result shouldBe a[Def1_CreateLossClaimValidator]

      }
    }

  }

}
