/*
 * Copyright 2021 HM Revenue & Customs
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

package v3.models.downstream

import mocks.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v3.hateoas.HateoasFactory
import v3.models.domain.TypeOfLoss
import v3.models.hateoas.Method.{DELETE, GET, POST}
import v3.models.hateoas.{HateoasWrapper, Link}

class BFLossResponseSpec extends UnitSpec {

  def downstreamPropertyJson(incomeSourceType: String): JsValue = {
    Json.parse(
      s"""
        |{
        |  "incomeSourceId": "000000000000001",
        |  "incomeSourceType": "$incomeSourceType",
        |  "broughtForwardLossAmount": 99999999999.99,
        |  "taxYear": "2020",
        |  "submissionDate": "2018-07-13T12:13:48.763Z"
        |}
      """.stripMargin
    )
  }

  def downstreamEmploymentJson(lossType: String): JsValue = {
    Json.parse(
      s"""
        |{
        |  "incomeSourceId": "000000000000001",
        |  "lossType": "$lossType",
        |  "broughtForwardLossAmount": 99999999999.99,
        |  "taxYear": "2020",
        |  "submissionDate": "2018-07-13T12:13:48.763Z"
        |}
      """.stripMargin
    )
  }

  def downstreamToModel: TypeOfLoss => BFLossResponse =
    typeOfLoss =>
      BFLossResponse(businessId = Some("000000000000001"),
                     typeOfLoss = typeOfLoss,
                     lossAmount = 99999999999.99,
                     taxYear = "2019-20",
                     lastModified = "2018-07-13T12:13:48.763Z")

  val bfLossResponse: BFLossResponse = BFLossResponse(
    businessId = Some("000000000000001"),
    typeOfLoss = TypeOfLoss.`self-employment`,
    lossAmount = 99999999999.99,
    taxYear = "2019-20",
    lastModified = "2018-07-13T12:13:48.763Z"
  )

  "Json Reads" should {
    "convert property JSON from downstream into a valid model for property type 02" in {
      downstreamPropertyJson("02").as[BFLossResponse] shouldBe downstreamToModel(TypeOfLoss.`uk-property-non-fhl`)
    }

    "convert property JSON from downstream into a valid model for property type 04" in {
      downstreamPropertyJson("04").as[BFLossResponse] shouldBe downstreamToModel(TypeOfLoss.`uk-property-fhl`)
    }

    "convert employment JSON from downstream into a valid model for property type INCOME" in {
      downstreamEmploymentJson("INCOME").as[BFLossResponse] shouldBe downstreamToModel(TypeOfLoss.`self-employment`)
    }

    "convert employment JSON from downstream into a valid model for property type CLASS4" in {
      downstreamEmploymentJson("CLASS4").as[BFLossResponse] shouldBe downstreamToModel(TypeOfLoss.`self-employment-class4`)
    }
    "convert employment JSON from downstream into a valid model for property type 03" in {
      downstreamPropertyJson("03").as[BFLossResponse] shouldBe downstreamToModel(TypeOfLoss.`foreign-property-fhl-eea`)
    }
    "convert employment JSON from downstream into a valid model for property type 15" in {
      downstreamPropertyJson("15").as[BFLossResponse] shouldBe downstreamToModel(TypeOfLoss.`foreign-property`)
    }
  }
  "Json Writes" should {
    val mtdJson = Json.parse(
      """
        |{
        |  "businessId": "000000000000001",
        |  "typeOfLoss": "self-employment",
        |  "lossAmount": 99999999999.99,
        |  "taxYear": "2019-20",
        |  "lastModified": "2018-07-13T12:13:48.763Z"
        |}
      """.stripMargin
    )
    "convert a valid model into MTD JSON" in {
      Json.toJson(bfLossResponse) shouldBe mtdJson
    }
  }

  "HateoasFactory" must {
    class Test extends MockAppConfig {
      val hateoasFactory = new HateoasFactory(mockAppConfig)
      val nino           = "someNino"
      val lossId         = "lossId"
      MockAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes
    }

    "expose the correct links for retrieve" in new Test {
      hateoasFactory.wrap(bfLossResponse, GetBFLossHateoasData(nino, lossId)) shouldBe
        HateoasWrapper(
          bfLossResponse,
          Seq(
            Link(s"/individuals/losses/$nino/brought-forward-losses/lossId", GET, "self"),
            Link(s"/individuals/losses/$nino/brought-forward-losses/lossId", DELETE, "delete-brought-forward-loss"),
            Link(s"/individuals/losses/$nino/brought-forward-losses/lossId/change-loss-amount", POST, "amend-brought-forward-loss")
          )
        )
    }

    "expose the correct links for amend" in new Test {
      hateoasFactory.wrap(bfLossResponse, AmendBFLossHateoasData(nino, lossId)) shouldBe
        HateoasWrapper(
          bfLossResponse,
          Seq(
            Link(s"/individuals/losses/$nino/brought-forward-losses/lossId", GET, "self")
          )
        )
    }
  }
}