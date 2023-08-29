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

package v3.models.response.listLossClaims

import api.models.hateoas.Link
import api.models.hateoas.Method.{GET, POST}
import config.MockAppConfig
import play.api.libs.json.Json
import support.UnitSpec
import v3.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}

class ListLossClaimsResponseSpec extends UnitSpec with MockAppConfig {

  val nino: String = "AA123456A"
  val taxYear      = "2018-19"

  "json writes" must {
    "output as per spec" in {
      Json.toJson(
        ListLossClaimsResponse(Seq(
          ListLossClaimsItem(
            "XAIS12345678910",
            TypeOfClaim.`carry-sideways`,
            TypeOfLoss.`self-employment`,
            "2020-21",
            "AAZZ1234567890A",
            Some(1),
            "2020-07-13T12:13:48.763Z"),
          ListLossClaimsItem(
            "XAIS12345678912",
            TypeOfClaim.`carry-sideways`,
            TypeOfLoss.`self-employment`,
            "2020-21",
            "AAZZ1234567890B",
            Some(2),
            "2020-07-13T12:13:48.763Z")
        ))) shouldBe
        Json.parse(
          """
            |{
            |    "claims": [
            |        {
            |            "businessId": "XAIS12345678910",
            |            "typeOfClaim": "carry-sideways",
            |            "typeOfLoss": "self-employment",
            |            "taxYearClaimedFor": "2020-21",
            |            "claimId": "AAZZ1234567890A",
            |            "sequence": 1,
            |            "lastModified": "2020-07-13T12:13:48.763Z"
            |        },
            |        {
            |            "businessId": "XAIS12345678912",
            |            "typeOfClaim": "carry-sideways",
            |            "typeOfLoss": "self-employment",
            |            "taxYearClaimedFor": "2020-21",
            |            "claimId": "AAZZ1234567890B",
            |            "sequence": 2,
            |            "lastModified": "2020-07-13T12:13:48.763Z"
            |        }
            |    ]
            |}
          """.stripMargin
        )
    }
  }

  "json reads" must {
    "work for downstream response" in {
      // Note we ignore all but the claimId currently...
      val downstreamResponseJson =
        Json.parse(
          """
            |[
            |  {
            |    "incomeSourceId": "000000000000001",
            |    "reliefClaimed": "CSGI",
            |    "taxYearClaimedFor": "2020",
            |    "claimId": "000000000000011",
            |    "submissionDate": "2020-07-13T12:13:48.763Z",
            |    "sequence": 1
            |  },
            |  {
            |    "incomeSourceId": "000000000000002",
            |    "incomeSourceType": "02",
            |    "reliefClaimed": "CSGI",
            |    "taxYearClaimedFor": "2020",
            |    "claimId": "000000000000022",
            |    "submissionDate": "2020-07-13T12:13:48.763Z",
            |    "sequence": 2
            |  },
            |  {
            |     "incomeSourceId": "000000000000003",
            |     "incomeSourceType": "15",
            |     "reliefClaimed": "CF",
            |     "taxYearClaimedFor": "2020",
            |     "claimId": "000000000000033",
            |     "submissionDate": "2020-07-13T12:13:48.763Z",
            |     "sequence": 3
            |  }
            |]
          """.stripMargin
        )

      downstreamResponseJson.as[ListLossClaimsResponse[ListLossClaimsItem]] shouldBe
        ListLossClaimsResponse(
          Seq(
            ListLossClaimsItem(
              "000000000000001",
              TypeOfClaim.`carry-sideways`,
              TypeOfLoss.`self-employment`,
              "2019-20",
              "000000000000011",
              Some(1),
              "2020-07-13T12:13:48.763Z"),
            ListLossClaimsItem(
              "000000000000002",
              TypeOfClaim.`carry-sideways`,
              TypeOfLoss.`uk-property-non-fhl`,
              "2019-20",
              "000000000000022",
              Some(2),
              "2020-07-13T12:13:48.763Z"),
            ListLossClaimsItem(
              "000000000000003",
              TypeOfClaim.`carry-forward`,
              TypeOfLoss.`foreign-property`,
              "2019-20",
              "000000000000033",
              Some(3),
              "2020-07-13T12:13:48.763Z")
          ))
    }
  }

  "Links Factory" should {

    "expose the correct top level links for list" in {
      MockAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes()
      ListLossClaimsResponse.LinksFactory.links(mockAppConfig, ListLossClaimsHateoasData(nino)) shouldBe
        Seq(
          Link(s"/individuals/losses/$nino/loss-claims", GET, "self"),
          Link(s"/individuals/losses/$nino/loss-claims", POST, "create-loss-claim")
        )
    }

    "expose the correct item level links for list" in {
      MockAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes()
      ListLossClaimsResponse.LinksFactory.itemLinks(
        mockAppConfig,
        ListLossClaimsHateoasData(nino),
        ListLossClaimsItem(
          "businessId",
          TypeOfClaim.`carry-sideways`,
          TypeOfLoss.`self-employment`,
          "2020",
          "claimId",
          Some(1),
          "2020-07-13T12:13:48.763Z")
      ) shouldBe
        Seq(
          Link(s"/individuals/losses/$nino/loss-claims/claimId", GET, "self")
        )
    }
  }

}
