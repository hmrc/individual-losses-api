/*
 * Copyright 2022 HM Revenue & Customs
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

package v3.models.response.listBFLosses

import api.endpoints.common.bfLoss.v3.domain.TypeOfLoss
import api.hateoas.{HateoasFactory, HateoasLinks}
import api.models.hateoas.Method.{GET, POST}
import api.models.hateoas.{HateoasWrapper, Link}
import config.MockAppConfig
import play.api.libs.json.Json
import support.UnitSpec

class ListBFLossesResponseSpec extends UnitSpec with HateoasLinks {

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

      Json.toJson(ListBFLossesResponse(Seq(item1, item2))) shouldBe
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

      downstreamResponseJson.as[ListBFLossesResponse[ListBFLossesItem]] shouldBe
        ListBFLossesResponse(Seq(item1, item2))
    }
  }

  "HateoasFactory" must {
    class Test extends MockAppConfig {
      val hateoasFactory = new HateoasFactory(mockAppConfig)
      val nino           = "someNino"
      MockAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes
    }

    "expose the correct links for list" in new Test {
      hateoasFactory.wrapList(ListBFLossesResponse(Seq(item1, item2)), ListBFLossHateoasData(nino)) shouldBe
        HateoasWrapper(
          ListBFLossesResponse(
            Seq(
              HateoasWrapper(item1, Seq(Link(s"/individuals/losses/$nino/brought-forward-losses/lossId1", GET, "self"))),
              HateoasWrapper(item2, Seq(Link(s"/individuals/losses/$nino/brought-forward-losses/lossId2", GET, "self")))
            )
          ),
          Seq(
            Link(s"/individuals/losses/$nino/brought-forward-losses", GET, "self"),
            Link(s"/individuals/losses/$nino/brought-forward-losses", POST, "create-brought-forward-loss")
          )
        )
    }
  }
}
