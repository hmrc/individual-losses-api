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

package v4.models.response.retrieveLossClaim

import shared.hateoas.Link
import shared.models.domain.Timestamp
import shared.hateoas.Method.{DELETE, GET, POST}
import shared.config.MockSharedAppConfig
import play.api.libs.json.{JsValue, Json}
import shared.utils.UnitSpec
import v4.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}

class RetrieveLossClaimResponseSpec extends UnitSpec with MockSharedAppConfig {

  val nino: String    = "AA123456A"
  val claimId: String = "claimId"
  val taxYearInt: Int = 2020

  val lossClaimResponse: RetrieveLossClaimResponse = RetrieveLossClaimResponse(
    businessId = "000000000000001",
    typeOfLoss = TypeOfLoss.`self-employment`,
    typeOfClaim = TypeOfClaim.`carry-forward`,
    taxYearClaimedFor = "2019-20",
    lastModified = Timestamp("2021-11-05T11:56:28Z"),
    sequence = Some(1)
  )

  "Json Reads" should {
    def ifsDownstreamPropertyJson(incomeSourceType: String): JsValue = {
      Json.parse(
        s"""
           |{
           |  "incomeSourceId": "000000000000001",
           |  "incomeSourceType": "$incomeSourceType",
           |  "reliefClaimed": "CSFHL",
           |  "taxYearClaimedFor": "2020",
           |  "claimId": "notUsed",
           |  "submissionDate": "2021-11-05T11:56:28Z",
           |  "sequence": 1
           |}
        """.stripMargin
      )
    }

    def hipDownstreamPropertyJson(incomeSourceType: String): JsValue = {
      Json.parse(
        s"""
           |{
           |  "incomeSourceId": "000000000000001",
           |  "incomeSourceType": "$incomeSourceType",
           |  "reliefClaimed": "CSFHL",
           |  "taxYearClaimedFor": 2020,
           |  "claimId": "notUsed",
           |  "submissionDate": "2021-11-05T11:56:28Z",
           |  "sequence": 1
           |}
        """.stripMargin
      )
    }

    def ifsDownstreamEmploymentJson: JsValue = {
      Json.parse(
        """
           |{
           |  "incomeSourceId": "000000000000001",
           |  "reliefClaimed": "CF",
           |  "taxYearClaimedFor": "2020",
           |  "claimId": "notUsed",
           |  "submissionDate": "2021-11-05T11:56:28Z",
           |  "sequence": 1
           |}
         """.stripMargin
      )
    }

    def hipDownstreamEmploymentJson: JsValue = {
      Json.parse(
        s"""
          |{
          |  "incomeSourceId": "000000000000001",
          |  "reliefClaimed": "CF",
          |  "taxYearClaimedFor": 2020,
          |  "claimId": "notUsed",
          |  "submissionDate": "2021-11-05T11:56:28Z",
          |  "sequence": 1
          |}
         """.stripMargin
      )
    }

    def downstreamToModel: TypeOfLoss => RetrieveLossClaimResponse =
      typeOfLoss =>
        RetrieveLossClaimResponse(
          businessId = "000000000000001",
          typeOfLoss = typeOfLoss,
          typeOfClaim = TypeOfClaim.`carry-sideways-fhl`,
          taxYearClaimedFor = "2019-20",
          lastModified = Timestamp("2021-11-05T11:56:28Z"),
          sequence = Some(1)
        )

    "convert property JSON from downstream into a valid model for property type 02" when {
      "taxYearClaimedFor is a String" in {
        ifsDownstreamPropertyJson("02").as[RetrieveLossClaimResponse] shouldBe downstreamToModel(TypeOfLoss.`uk-property-non-fhl`)
      }

      "taxYearClaimedFor is an Int" in {
        hipDownstreamPropertyJson("02").as[RetrieveLossClaimResponse] shouldBe downstreamToModel(TypeOfLoss.`uk-property-non-fhl`)
      }
    }

    "convert se json from downstream into a valid model" when {
      "taxYearClaimedFor is a String" in {
        ifsDownstreamEmploymentJson.as[RetrieveLossClaimResponse] shouldBe RetrieveLossClaimResponse(
          "2019-20",
          TypeOfLoss.`self-employment`,
          TypeOfClaim.`carry-forward`,
          "000000000000001",
          Some(1),
          Timestamp("2021-11-05T11:56:28Z")
        )
      }

      "taxYearClaimedFor is an Int" in {
        hipDownstreamEmploymentJson.as[RetrieveLossClaimResponse] shouldBe RetrieveLossClaimResponse(
          "2019-20",
          TypeOfLoss.`self-employment`,
          TypeOfClaim.`carry-forward`,
          "000000000000001",
          Some(1),
          Timestamp("2021-11-05T11:56:28Z")
        )
      }
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
        |  "lastModified" : "2021-11-05T11:56:28.000Z",
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
      MockedSharedAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes()
      RetrieveLossClaimResponse.GetLinksFactory.links(mockSharedAppConfig, GetLossClaimHateoasData(nino, claimId)) shouldBe
        Seq(
          Link(s"/individuals/losses/$nino/loss-claims/claimId", GET, "self"),
          Link(s"/individuals/losses/$nino/loss-claims/claimId", DELETE, "delete-loss-claim"),
          Link(s"/individuals/losses/$nino/loss-claims/claimId/change-type-of-claim", POST, "amend-loss-claim")
        )
    }
  }

}
