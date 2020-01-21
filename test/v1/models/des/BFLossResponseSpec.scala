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
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v1.hateoas.HateoasFactory
import v1.models.domain.TypeOfLoss
import v1.models.hateoas.Method.{DELETE, GET, POST}
import v1.models.hateoas.{HateoasWrapper, Link}

class BFLossResponseSpec extends UnitSpec {

  def desPropertyJson(incomeSourceType: String): JsValue = {
    Json.parse(s"""
           |{
           |  "incomeSourceId": "000000000000001",
           |  "incomeSourceType": "$incomeSourceType",
           |  "broughtForwardLossAmount": 99999999999.99,
           |  "taxYear": "2020",
           |  "submissionDate": "2018-07-13T12:13:48.763Z"
           |}
      """.stripMargin)
  }

  def desEmploymentJson(lossType: String): JsValue = {
    Json.parse(s"""
           |{
           |  "incomeSourceId": "000000000000001",
           |  "lossType": "$lossType",
           |  "broughtForwardLossAmount": 99999999999.99,
           |  "taxYear": "2020",
           |  "submissionDate": "2018-07-13T12:13:48.763Z"
           |}
      """.stripMargin)
  }

  def desToModel: TypeOfLoss => BFLossResponse =
    typeOfLoss =>
      BFLossResponse(selfEmploymentId = Some("000000000000001"),
                     typeOfLoss = typeOfLoss,
                     lossAmount = 99999999999.99,
                     taxYear = "2019-20",
                     lastModified = "2018-07-13T12:13:48.763Z")

  val bfLossResponse =
    BFLossResponse(
      selfEmploymentId = Some("000000000000001"),
      typeOfLoss = TypeOfLoss.`self-employment`,
      lossAmount = 99999999999.99,
      taxYear = "2019-20",
      lastModified = "2018-07-13T12:13:48.763Z"
    )

  "Json Reads" should {
    "convert property JSON from DES into a valid model for property type 02" in {
      desPropertyJson("02").as[BFLossResponse] shouldBe desToModel(TypeOfLoss.`uk-property-non-fhl`)
    }

    "convert property JSON from DES into a valid model for property type 04" in {
      desPropertyJson("04").as[BFLossResponse] shouldBe desToModel(TypeOfLoss.`uk-property-fhl`)
    }

    "convert employment JSON from DES into a valid model for property type INCOME" in {
      desEmploymentJson("INCOME").as[BFLossResponse] shouldBe desToModel(TypeOfLoss.`self-employment`)
    }

    "convert employment JSON from DES into a valid model for property type CLASS4" in {
      desEmploymentJson("CLASS4").as[BFLossResponse] shouldBe desToModel(TypeOfLoss.`self-employment-class4`)
    }
  }
  "Json Writes" should {
    val mtdJson = Json.parse("""{
        |  "selfEmploymentId": "000000000000001",
        |  "typeOfLoss": "self-employment",
        |  "lossAmount": 99999999999.99,
        |  "taxYear": "2019-20",
        |  "lastModified": "2018-07-13T12:13:48.763Z"
        |}
      """.stripMargin)
    "convert a valid model into MTD JSON" in {
      Json.toJson(bfLossResponse) shouldBe mtdJson
    }
  }

  "HateoasFactory" must {
    class Test extends MockAppConfig {
      val hateoasFactory = new HateoasFactory(mockAppConfig)
      val nino           = "someNino"
      val lossId         = "lossId"
      MockedAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes
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
