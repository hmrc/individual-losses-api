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

package v5.bfLoss.list.def1.model.response

import api.hateoas.HateoasLinks
import play.api.libs.json.Json
import support.UnitSpec
import v5.bfLosses.list.def1.model.response.{ListBFLossesItem, Def1_ListBFLossesResponse}
import v5.bfLosses.list.model._

class Def1_ListBFLossesResponseSpec extends UnitSpec with HateoasLinks {

  val item1: ListBFLossesItem = ListBFLossesItem(
    lossId = "lossId1",
    businessId = "businessId1",
    typeOfLoss = TypeOfLoss.`self-employment`,
    lossAmount = 1.50,
    taxYearBroughtForwardFrom = "2019-20",
    lastModified = "lastModified1"
  )

  val item2: ListBFLossesItem = ListBFLossesItem(
    lossId = "lossId2",
    businessId = "businessId2",
    typeOfLoss = TypeOfLoss.`self-employment`,
    lossAmount = 2.75,
    taxYearBroughtForwardFrom = "2019-20",
    lastModified = "lastModified2"
  )

  "json writes" must {
    "output as per spec" in {

      Json.toJson(Def1_ListBFLossesResponse(Seq(item1, item2))) shouldBe
        Json.parse(
          """
          |{
          |    "losses": [
          |        {
          |            "lossId": "lossId1",
          |            "businessId": "businessId1",
          |            "typeOfLoss": "self-employment",
          |            "lossAmount": 1.50,
          |            "taxYearBroughtForwardFrom": "2019-20",
          |            "lastModified": "lastModified1"
          |        },
          |        {
          |            "lossId": "lossId2",
          |            "businessId": "businessId2",
          |            "typeOfLoss": "self-employment",
          |            "lossAmount": 2.75,
          |            "taxYearBroughtForwardFrom": "2019-20",
          |            "lastModified": "lastModified2"
          |        }
          |    ]
          |}
        """.stripMargin
        )
    }
  }

  "json reads" must {
    "work for downstream response" in {
      val downstreamResponseJson =
        Json.parse(
          """
           |[
           |  {
           |   "incomeSourceId": "businessId1",
           |   "lossType": "INCOME",
           |   "broughtForwardLossAmount": 1.50,
           |   "taxYear": "2020",
           |   "lossId": "lossId1",
           |   "submissionDate": "lastModified1"
           |  },
           |  {
           |   "incomeSourceId": "businessId2",
           |   "lossType": "INCOME",
           |   "broughtForwardLossAmount": 2.75,
           |   "taxYear": "2020",
           |   "lossId": "lossId2",
           |   "submissionDate": "lastModified2"
           |  }
           |]
         """.stripMargin
        )

      downstreamResponseJson.as[Def1_ListBFLossesResponse] shouldBe
        Def1_ListBFLossesResponse(Seq(item1, item2))
    }
  }

}
