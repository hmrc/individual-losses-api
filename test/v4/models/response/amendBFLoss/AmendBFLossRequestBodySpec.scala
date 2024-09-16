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

package v4.models.response.amendBFLoss

import play.api.libs.json.{JsValue, Json}
import shared.utils.UnitSpec
import v4.models.request.amendBFLosses.AmendBFLossRequestBody

class AmendBFLossRequestBodySpec extends UnitSpec {

  val mtdJson: JsValue = Json.parse("""
      |{
      |  "lossAmount": 1000.99
      |}
    """.stripMargin)

  val model: AmendBFLossRequestBody = AmendBFLossRequestBody(1000.99)

  val downstreamJson: JsValue = Json.parse(
    """
      |{
      |  "updatedBroughtForwardLossAmount": 1000.99
      |}
    """.stripMargin
  )

  "Json Reads" should {
    "convert valid MTD JSON into a model" in {
      mtdJson.as[AmendBFLossRequestBody] shouldBe model
    }
  }

  "Json Writes" should {
    "convert a model into valid downstream JSON" in {
      Json.toJson(model) shouldBe downstreamJson
    }
  }

}
