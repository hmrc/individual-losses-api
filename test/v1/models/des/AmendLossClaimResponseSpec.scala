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

package v1.models.des

import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v1.models.domain.{TypeOfClaim, TypeOfLoss}

class AmendLossClaimResponseSpec extends UnitSpec {

  "Json Reads" should {
    def desPropertyJson(incomeSourceType: String): JsValue = {
      Json.parse(s"""
                    |{
                    |  "incomeSourceId": "000000000000001",
                    |  "incomeSourceType": "$incomeSourceType",
                    |  "reliefClaimed": "CSFHL",
                    |  "taxYear": "2020",
                    |  "submissionDate": "20180708"
                    |}
      """.stripMargin)
    }

    def desEmploymentJson: JsValue = {
      Json.parse(s"""
                    |{
                    |  "incomeSourceId": "000000000000001",
                    |  "reliefClaimed": "CF",
                    |  "taxYear": "2020",
                    |  "submissionDate": "20180708"
                    |}
      """.stripMargin)
    }

    def desToModel: TypeOfLoss => AmendLossClaimResponse =
      typeOfLoss =>
        AmendLossClaimResponse(selfEmploymentId = Some("000000000000001"), typeOfLoss = Some(typeOfLoss),
          typeOfClaim = TypeOfClaim.`carry-sideways-fhl`, taxYear = "2019-20", lastModified = "20180708")

    "convert property JSON from DES into a valid model for property type 02" in {
      desPropertyJson("02").as[AmendLossClaimResponse] shouldBe desToModel(TypeOfLoss.`uk-property-non-fhl`)
    }

    "convert se json from DES into a valid model" in {
      desEmploymentJson.as[AmendLossClaimResponse] shouldBe AmendLossClaimResponse(Some("000000000000001"), None, TypeOfClaim.`carry-forward`,
      "2019-20", "20180708")
    }
  }
  "Json Writes" should {
    val model =
      AmendLossClaimResponse(selfEmploymentId = Some("000000000000001"),
        typeOfLoss = Some(TypeOfLoss.`self-employment`),
        typeOfClaim = TypeOfClaim.`carry-forward`,
        taxYear = "2019-20",
        lastModified = "")
    val mtdJson = Json.parse("""{
                               |  "selfEmploymentId": "000000000000001",
                               |  "typeOfLoss": "self-employment",
                               |  "typeOfClaim": "carry-forward",
                               |  "taxYear": "2019-20",
                               |  "lastModified" : ""
                               |}
                             """.stripMargin)
    "convert a valid model into MTD JSON" in {
      Json.toJson(model) shouldBe mtdJson
    }
  }
}
