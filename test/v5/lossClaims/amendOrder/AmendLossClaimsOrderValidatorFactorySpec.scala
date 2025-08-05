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

package v5.lossClaims.amendOrder

import play.api.libs.json.{JsArray, JsValue, Json}
import shared.models.utils.JsonErrorValidators
import shared.utils.UnitSpec
import v5.lossClaims.amendOrder.def1.Def1_AmendLossClaimsOrderValidator

class AmendLossClaimsOrderValidatorFactorySpec extends UnitSpec with JsonErrorValidators {

  private val validNino    = "AA123456A"
  private val validTaxYear = "2019-20"

  private def item(seq: Int, claimId: String = "AAZZ1234567890a") = Json.parse(s"""
       |{
       |  "claimId":"$claimId",
       |  "sequence": $seq
       |}
    """.stripMargin)

  private def mtdRequestWith(typeOfClaim: String = "carry-sideways", items: Seq[JsValue]) =
    Json.parse(s"""
      |{
      |
      |  "typeOfClaim":"$typeOfClaim",
      |  "listOfLossClaims": ${JsArray(items)}
      |}
    """.stripMargin)

  private val mtdRequest = mtdRequestWith(items = List(item(1)))

  private val validatorFactory = new AmendLossClaimsOrderValidatorFactory

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validatorFactory.validator(validNino, validTaxYear, mtdRequest)
        result shouldBe a[Def1_AmendLossClaimsOrderValidator]
      }
    }

  }

}
