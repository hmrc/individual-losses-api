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
import v1.hateoas.{HateoasFactory, HateoasLinks}
import v1.models.hateoas.Method.{GET, POST}
import v1.models.hateoas.{HateoasWrapper, Link}

class ListBFLossesResponseSpec extends UnitSpec with HateoasLinks {

  "json writes" must {
    "output as per spec" in {
      Json.toJson(ListBFLossesResponse(Seq(BFLossId("000000123456789"), BFLossId("000000123456790")))) shouldBe
        Json.parse("""
          |{
          |    "losses": [
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
      // Note we ignore all but the lossId currently...
      val desResponseJson =
        Json.parse("""
           |[
           |  {
           |   "incomeSourceId": "000000000000000",
           |   "lossType": "INCOME",
           |   "broughtForwardLossAmount": 99999999999.99,
           |   "taxYear": "2000",
           |   "lossId": "000000000000001",
           |   "submissionDate": "2018-07-13T12:13:48.763Z"
           |  },
           |  {
           |   "incomeSourceId": "000000000000000",
           |   "lossType": "INCOME",
           |   "broughtForwardLossAmount": 0.02,
           |   "taxYear": "2000",
           |   "lossId": "000000000000002",
           |   "submissionDate": "2018-07-13T12:13:48.763Z"
           |  },
           |  {
           |   "incomeSourceId": "000000000000000",
           |   "incomeSourceType": "01",
           |   "broughtForwardLossAmount": 0.02,
           |   "taxYear": "2000",
           |   "lossId": "000000000000008",
           |   "submissionDate": "2018-07-13T12:13:48.763Z"
           |  },
           |  {
           |   "incomeSourceType": "02",
           |   "broughtForwardLossAmount": 0.02,
           |   "taxYear": "2000",
           |   "lossId": "000000000000003",
           |   "submissionDate": "2018-07-13T12:13:48.763Z"
           |  },
           |  {
           |   "incomeSourceType": "04",
           |   "broughtForwardLossAmount": 0.02,
           |   "taxYear": "2000",
           |   "lossId": "000000000000004",
           |   "submissionDate": "2018-07-13T12:13:48.763Z"
           |  }
           |]
           |
        """.stripMargin)

      desResponseJson.as[ListBFLossesResponse[BFLossId]] shouldBe
        ListBFLossesResponse(
          Seq(BFLossId("000000000000001"),
              BFLossId("000000000000002"),
              BFLossId("000000000000008"),
              BFLossId("000000000000003"),
              BFLossId("000000000000004")))
    }
  }

  "HateoasFactory" must {
    class Test extends MockAppConfig {
      val hateoasFactory = new HateoasFactory(mockAppConfig)
      val nino           = "someNino"
      MockedAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes
    }

    "expose the correct links for list" in new Test {
      hateoasFactory.wrapList(ListBFLossesResponse(Seq(BFLossId("lossId"))), ListBFLossHateoasData(nino)) shouldBe
        HateoasWrapper(
          ListBFLossesResponse(
            Seq(HateoasWrapper(BFLossId("lossId"), Seq(Link(s"/individuals/losses/$nino/brought-forward-losses/lossId", GET, "self"))))),
          Seq(
            Link(s"/individuals/losses/$nino/brought-forward-losses", GET, "self"),
            Link(s"/individuals/losses/$nino/brought-forward-losses", POST, "create-brought-forward-loss")
          )
        )
    }
  }

}
