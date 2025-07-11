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

package v4.models.request.amendLossClaimsType

import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import shared.config.MockSharedAppConfig
import shared.utils.UnitSpec
import v4.models.domain.lossClaim.TypeOfClaim
import v4.models.domain.lossClaim.TypeOfClaim._
import v4.models.request.amendLossClaimType.AmendLossClaimTypeRequestBody

class AmendLossClaimTypeRequestBodySpec extends UnitSpec with MockSharedAppConfig {

  def ifsDownstreamJson(reliefClaimed: String): JsValue = Json.parse {
    s"""
       |{
       | "updatedReliefClaimedType" : "$reliefClaimed"
       |}
    """.stripMargin
  }

  def hipDownstreamJson(reliefClaimed: String): JsValue = Json.parse {
    s"""
       |{
       | "updatedReliefClaimed" : "$reliefClaimed"
       |}
    """.stripMargin
  }

  def mtdJson(typeOfClaim: String): JsValue = Json.parse {
    s"""
       |{
       |  "typeOfClaim" : "$typeOfClaim"
       |}
     """.stripMargin
  }

  "Calling .write" should {

    val testData = Map(
      "CF"     -> TypeOfClaim.`carry-forward`,
      "CSGI"   -> TypeOfClaim.`carry-sideways`,
      "CFCSGI" -> TypeOfClaim.`carry-forward-to-carry-sideways`,
      "CSFHL"  -> TypeOfClaim.`carry-sideways-fhl`
    )

    "produce the correct Json for IFS downstream submission" when {
      testData.foreach(test =>
        s"supplied with a TypeOfClaim of ${test._2}" in {
          MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration("ifs_hip_migration_1506.enabled" -> false))
          Json.toJson(AmendLossClaimTypeRequestBody(test._2)) shouldBe ifsDownstreamJson(test._1)
        })
    }

    "produce the correct Json for HIP downstream submission" when {
      testData.foreach(test =>
        s"supplied with a TypeOfClaim of ${test._2}" in {
          MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration("ifs_hip_migration_1506.enabled" -> true))
          Json.toJson(AmendLossClaimTypeRequestBody(test._2)) shouldBe hipDownstreamJson(test._1)
        })
    }
  }

  "Calling .read" should {

    val testData = Map(
      "carry-forward"                   -> `carry-forward`,
      "carry-sideways"                  -> `carry-sideways`,
      "carry-forward-to-carry-sideways" -> `carry-forward-to-carry-sideways`,
      "carry-sideways-fhl"              -> `carry-sideways-fhl`
    )

    "produce the correct model from MTD submission json" when {

      testData.foreach(test =>
        s"supplied with a TypeOfClaim of ${test._1}" in {
          mtdJson(test._1).as[AmendLossClaimTypeRequestBody] shouldBe AmendLossClaimTypeRequestBody(test._2)
        })
    }
  }

}
