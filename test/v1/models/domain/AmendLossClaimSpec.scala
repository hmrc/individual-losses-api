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

package v1.models.domain

import play.api.libs.json.{JsValue, Json}
import support.UnitSpec

class AmendLossClaimSpec extends UnitSpec {

  def json(reliefClaimed: String): JsValue = Json.parse {
    s"""
      |{
      | "reliefClaimed" : "$reliefClaimed"
      |}
    """.stripMargin
  }

  val testData = Map(
    "CF" -> TypeOfClaim.`carry-forward`,
    "CSGI" -> TypeOfClaim.`carry-sideways`,
    "CFCSGI" -> TypeOfClaim.`carry-forward-to-carry-sideways-general-income`,
    "CSFHL" -> TypeOfClaim.`carry-sideways-fhl`)

  "Calling .write" should {

    "produce the correct Json for des submission" when {

      testData.foreach( test =>
        s"supplied with a TypeOfClaim of ${test._2}" in {
          Json.toJson(AmendLossClaim(test._2)) shouldBe json(test._1)
        }
      )
    }
  }
}
