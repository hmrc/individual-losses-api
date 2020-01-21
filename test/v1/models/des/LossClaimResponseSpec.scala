/*
 * Copyright 2020 HM Revenue & Customs
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

import mocks.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v1.models.domain.{TypeOfClaim, TypeOfLoss}
import v1.models.hateoas.Link
import v1.models.hateoas.Method.{DELETE, GET, POST}

class LossClaimResponseSpec extends UnitSpec with MockAppConfig {

  val nino: String = "AA123456A"
  val claimId: String = "claimId"

  val lossClaimResponse =
    LossClaimResponse(
      selfEmploymentId = Some("000000000000001"),
      typeOfLoss = TypeOfLoss.`self-employment`,
      typeOfClaim = TypeOfClaim.`carry-forward`,
      taxYear = "2019-20",
      lastModified = ""
    )

  "Json Reads" should {
    def desPropertyJson(incomeSourceType: String): JsValue = {
      Json.parse(
        s"""
           |{
           |  "incomeSourceId": "000000000000001",
           |  "incomeSourceType": "$incomeSourceType",
           |  "reliefClaimed": "CSFHL",
           |  "taxYearClaimedFor": "2020",
           |  "claimId": "notUsed",
           |  "submissionDate": "20180708"
           |}
      """.stripMargin)
    }

    def desEmploymentJson: JsValue = {
      Json.parse(
        s"""
           |{
           |  "incomeSourceId": "000000000000001",
           |  "reliefClaimed": "CF",
           |  "taxYearClaimedFor": "2020",
           |  "claimId": "notUsed",
           |  "submissionDate": "20180708"
           |}
      """.stripMargin)
    }

    def desToModel: TypeOfLoss => LossClaimResponse =
      typeOfLoss =>
        LossClaimResponse(
          selfEmploymentId = Some("000000000000001"),
          typeOfLoss = typeOfLoss,
          typeOfClaim = TypeOfClaim.`carry-sideways-fhl`,
          taxYear = "2019-20",
          lastModified = "20180708"
        )

    "convert property JSON from DES into a valid model for property type 02" in {
      desPropertyJson("02").as[LossClaimResponse] shouldBe desToModel(TypeOfLoss.`uk-property-non-fhl`)
    }

    "convert se json from DES into a valid model" in {
      desEmploymentJson.as[LossClaimResponse] shouldBe LossClaimResponse(Some("000000000000001"),
        TypeOfLoss.`self-employment`,
        TypeOfClaim.`carry-forward`,
        "2019-20",
        "20180708")
    }
  }
  "Json Writes" should {
    val mtdJson = Json.parse(
      """{
        |  "selfEmploymentId": "000000000000001",
        |  "typeOfLoss": "self-employment",
        |  "typeOfClaim": "carry-forward",
        |  "taxYear": "2019-20",
        |  "lastModified" : ""
        |}
      """.stripMargin)
    "convert a valid model into MTD JSON" in {
      Json.toJson(lossClaimResponse) shouldBe mtdJson
    }
  }

  "Links Factory" should {

    "expose the correct links for retrieve" in {
      MockedAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes
      LossClaimResponse.GetLinksFactory.links(mockAppConfig, GetLossClaimHateoasData(nino, claimId)) shouldBe
          Seq(
            Link(s"/individuals/losses/$nino/loss-claims/claimId", GET, "self"),
            Link(s"/individuals/losses/$nino/loss-claims/claimId", DELETE, "delete-loss-claim"),
            Link(s"/individuals/losses/$nino/loss-claims/claimId/change-type-of-claim", POST, "amend-loss-claim")
          )
    }

    "expose the correct links for amend" in {
      MockedAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes
      LossClaimResponse.AmendLinksFactory.links(mockAppConfig, AmendLossClaimHateoasData(nino, claimId)) shouldBe
        Seq(
          Link("/individuals/losses/AA123456A/loss-claims/claimId", GET, "self")
        )
    }
  }
}
