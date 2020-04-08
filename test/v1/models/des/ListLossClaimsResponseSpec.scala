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
import play.api.libs.json.Json
import support.UnitSpec
import v1.models.hateoas.Link
import v1.models.hateoas.Method.{GET, POST}

class ListLossClaimsResponseSpec extends UnitSpec with MockAppConfig {

  val nino = "AA123456A"

  "json writes" must {
    "output as per spec" in {
      Json.toJson(ListLossClaimsResponse(Seq(LossClaimId("000000123456789", Some(1)), LossClaimId("000000123456790", Some(2))))) shouldBe
        Json.parse(
          """
            |{
            |    "claims": [
            |        {
            |            "id": "000000123456789",
            |            "sequence": 1
            |        },
            |        {
            |            "id": "000000123456790",
            |            "sequence": 2
            |        }
            |    ]
            |}
          """.stripMargin)
    }
  }
  "json reads" must {
    "work for des response" in {
      // Note we ignore all but the claimId currently...
      val desResponseJson =
        Json.parse(
          """[
            |  {
            |    "incomeSourceId": "000000000000001",
            |    "reliefClaimed": "CF",
            |    "taxYearClaimedFor": "2099",
            |    "claimId": "000000000000011",
            |    "submissionDate": "2019-07-13T12:13:48.763Z",
            |    "sequence": 1
            |  },
            |  {
            |    "incomeSourceId": "000000000000002",
            |    "reliefClaimed": "CF",
            |    "taxYearClaimedFor": "2020",
            |    "claimId": "000000000000022",
            |    "submissionDate": "2018-07-13T12:13:48.763Z",
            |    "sequence": 2
            |  },
            |  {
            |     "incomeSourceType": "02",
            |     "reliefClaimed": "CSFHL",
            |     "taxYearClaimedFor": "2020",
            |     "claimId": "000000000000033",
            |     "submissionDate": "2018-07-13T12:13:48.763Z",
            |     "sequence": 3
            |  }
            |]
            |
        """.stripMargin)

      desResponseJson.as[ListLossClaimsResponse[LossClaimId]] shouldBe
        ListLossClaimsResponse(
          Seq(LossClaimId("000000000000011", Some(1)), LossClaimId("000000000000022", Some(2)), LossClaimId("000000000000033", Some(3))))
    }
  }

  "Links Factory" should {

    "expose the correct top level links for list" in {
      MockedAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes
      ListLossClaimsResponse.LinksFactory.links(mockAppConfig, ListLossClaimsHateoasData(nino)) shouldBe
        Seq(
          Link(s"/individuals/losses/$nino/loss-claims", GET, "self"),
          Link(s"/individuals/losses/$nino/loss-claims", POST, "create-loss-claim")
        )
    }

    "expose the correct item level links for list" in {
      MockedAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes
      ListLossClaimsResponse.LinksFactory.itemLinks(mockAppConfig, ListLossClaimsHateoasData(nino), LossClaimId("claimId", Some(1))) shouldBe
        Seq(
          Link(s"/individuals/losses/$nino/loss-claims/claimId", GET, "self")
        )
    }
  }

}
