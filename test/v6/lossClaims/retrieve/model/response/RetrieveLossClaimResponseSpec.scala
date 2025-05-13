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

package v6.lossClaims.retrieve.model.response

import play.api.libs.json.{JsValue, Json}
import shared.config.MockSharedAppConfig
import shared.models.domain.Timestamp
import shared.utils.UnitSpec
import v6.lossClaims.common.models.{TypeOfClaim, TypeOfLoss}
import v6.lossClaims.retrieve.def1.model.response.Def1_RetrieveLossClaimResponse

class RetrieveLossClaimResponseSpec extends UnitSpec with MockSharedAppConfig {

  val nino: String    = "AA123456A"
  val claimId: String = "claimId"

  private val lossClaimResponse = Def1_RetrieveLossClaimResponse(
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
           |  "taxYearClaimedFor": "2020",
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

    def downstreamToModel: TypeOfLoss => Def1_RetrieveLossClaimResponse =
      typeOfLoss =>
        Def1_RetrieveLossClaimResponse(
          businessId = "000000000000001",
          typeOfLoss = typeOfLoss,
          typeOfClaim = TypeOfClaim.`carry-sideways-fhl`,
          taxYearClaimedFor = "2019-20",
          lastModified = Timestamp("2021-11-05T11:56:28Z"),
          sequence = Some(1)
        )

    "convert property JSON from downstream into a valid model for property type 02" when {
      "taxYearClaimedFor is a String" in {
        val result = ifsDownstreamPropertyJson("02").as[Def1_RetrieveLossClaimResponse]
        result shouldBe downstreamToModel(TypeOfLoss.`uk-property`)
      }

      "taxYearClaimedFor is an Int" in {
        val result = hipDownstreamPropertyJson("02").as[Def1_RetrieveLossClaimResponse]
        result shouldBe downstreamToModel(TypeOfLoss.`uk-property`)
      }
    }

    "convert se json from downstream into a valid model" when {
      "taxYearClaimedFor is a String" in {
        ifsDownstreamEmploymentJson.as[Def1_RetrieveLossClaimResponse] shouldBe Def1_RetrieveLossClaimResponse(
          "2019-20",
          TypeOfLoss.`self-employment`,
          TypeOfClaim.`carry-forward`,
          "000000000000001",
          Some(1),
          Timestamp("2021-11-05T11:56:28Z")
        )
      }

      "taxYearClaimedFor is an Int" in {
        hipDownstreamEmploymentJson.as[Def1_RetrieveLossClaimResponse] shouldBe Def1_RetrieveLossClaimResponse(
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

}
