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

package api.endpoints.lossClaim.amendType.v3.response

import api.endpoints.lossClaim.domain.v3.{TypeOfClaim, TypeOfLoss}
import api.models.hateoas.Link
import api.models.hateoas.Method.{DELETE, GET, POST}
import config.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec

class AmendLossClaimTypeResponseSpec extends UnitSpec with MockAppConfig {

  val nino: String    = "AA123456A"
  val claimId: String = "claimId"

  val lossClaimResponse: AmendLossClaimTypeResponse = AmendLossClaimTypeResponse(
    businessId = "000000000000001",
    typeOfLoss = TypeOfLoss.`self-employment`,
    typeOfClaim = TypeOfClaim.`carry-forward`,
    taxYearClaimedFor = "2019-20",
    lastModified = "",
    sequence = Some(1)
  )

  "Json Reads" should {
    def downstreamPropertyJson(incomeSourceType: String): JsValue = {
      Json.parse(
        s"""
           |{
           |  "incomeSourceId": "000000000000001",
           |  "incomeSourceType": "$incomeSourceType",
           |  "reliefClaimed": "CSFHL",
           |  "taxYearClaimedFor": "2020",
           |  "claimId": "notUsed",
           |  "submissionDate": "20180708",
           |  "sequence": 1
           |}
        """.stripMargin
      )
    }

    def downstreamEmploymentJson: JsValue = {
      Json.parse(
        """
           |{
           |  "incomeSourceId": "000000000000001",
           |  "reliefClaimed": "CF",
           |  "taxYearClaimedFor": "2020",
           |  "claimId": "notUsed",
           |  "submissionDate": "20180708",
           |  "sequence": 1
           |}
         """.stripMargin
      )
    }

    def downstreamToModel: TypeOfLoss => AmendLossClaimTypeResponse =
      typeOfLoss =>
        AmendLossClaimTypeResponse(
          businessId = "000000000000001",
          typeOfLoss = typeOfLoss,
          typeOfClaim = TypeOfClaim.`carry-sideways-fhl`,
          taxYearClaimedFor = "2019-20",
          lastModified = "20180708",
          sequence = Some(1)
      )

    "convert property JSON from downstream into a valid model for property type 02" in {
      downstreamPropertyJson("02").as[AmendLossClaimTypeResponse] shouldBe downstreamToModel(TypeOfLoss.`uk-property-non-fhl`)
    }

    "convert se json from downstream into a valid model" in {
      downstreamEmploymentJson.as[AmendLossClaimTypeResponse] shouldBe AmendLossClaimTypeResponse(
        "2019-20",
        TypeOfLoss.`self-employment`,
        TypeOfClaim.`carry-forward`,
        "000000000000001",
        Some(1),
        "20180708"
      )
    }
  }
  "Json Writes" should {
    val mtdJson = Json.parse(
      """
        |{
        |  "businessId": "000000000000001",
        |  "typeOfLoss": "self-employment",
        |  "typeOfClaim": "carry-forward",
        |  "taxYearClaimedFor": "2019-20",
        |  "lastModified" : "",
        |  "sequence": 1
        |}
      """.stripMargin
    )
    "convert a valid model into MTD JSON" in {
      Json.toJson(lossClaimResponse) shouldBe mtdJson
    }
  }

  "Links Factory" should {

    "expose the correct links" in {
      MockAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes()
      AmendLossClaimTypeResponse.AmendLinksFactory.links(mockAppConfig, AmendLossClaimTypeHateoasData(nino, claimId)) shouldBe
        Seq(
          Link(s"/individuals/losses/$nino/loss-claims/claimId", GET, "self"),
          Link(s"/individuals/losses/$nino/loss-claims/claimId", DELETE, "delete-loss-claim"),
          Link(s"/individuals/losses/$nino/loss-claims/claimId/change-type-of-claim", POST, "amend-loss-claim")
        )
    }
  }
}
