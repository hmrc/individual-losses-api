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

import play.api.libs.json.Json
import support.UnitSpec

class ListLossClaimsResponseSpec extends UnitSpec {

  "json writes" must {
    "output as per spec" in {
      Json.toJson(ListLossClaimsResponse(Seq(LossClaimId("000000123456789"), LossClaimId("000000123456790")))) shouldBe
        Json.parse("""
          |{
          |    "claims": [
          |        {
          |            "id": "000000123456789"
          |        },
          |        {
          |            "id": "000000123456790"
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
        Json.parse("""[
           |  {
           |    "incomeSourceId": "000000000000001",
           |    "reliefClaimed": "CF",
           |    "taxYearClaimedFor": "2099",
           |    "claimId": "000000000000011",
           |    "submissionDate": "2019-07-13T12:13:48.763Z"
           |  },
           |  {
           |    "incomeSourceId": "000000000000002",
           |    "reliefClaimed": "CF",
           |    "taxYearClaimedFor": "2020",
           |    "claimId": "000000000000022",
           |    "submissionDate": "2018-07-13T12:13:48.763Z"
           |  },
           |  {
           |     "incomeSourceType": "02",
           |     "reliefClaimed": "CSFHL",
           |     "taxYearClaimedFor": "2020",
           |     "claimId": "000000000000033",
           |     "submissionDate": "2018-07-13T12:13:48.763Z"
           |  }
           |]
           |
        """.stripMargin)

      desResponseJson.as[ListLossClaimsResponse] shouldBe
        ListLossClaimsResponse(
          Seq(LossClaimId("000000000000011"), LossClaimId("000000000000022"), LossClaimId("000000000000033")))
    }
  }

}