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

package api.endpoints.bfLoss.amend.v3.response

import api.endpoints.bfLoss.amend.anyVersion.response.AmendBFLossHateoasData
import api.endpoints.bfLoss.domain.v3.{IncomeSourceType, LossType, TypeOfLoss}
import api.hateoas.HateoasFactory
import api.models.hateoas.Method.{DELETE, GET, POST}
import api.models.hateoas.{HateoasWrapper, Link}
import config.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec

class AmendBFLossResponseSpec extends UnitSpec {

  val businessId        = "000000000000001"
  val lossAmount        = 123.45
  val taxYearDownstream = "2020"
  val taxYear           = "2019-20"
  val lastModified      = "2018-07-13T12:13:48.763Z"

  def responseWith(typeOfLoss: TypeOfLoss): AmendBFLossResponse =
    AmendBFLossResponse(
      businessId = businessId,
      typeOfLoss = typeOfLoss,
      lossAmount = lossAmount,
      taxYearBroughtForwardFrom = taxYear,
      lastModified = lastModified
    )

  "Json Reads" when {
    "reading a property brought forward loss" must {
      test(IncomeSourceType.`02`, TypeOfLoss.`uk-property-non-fhl`)
      test(IncomeSourceType.`03`, TypeOfLoss.`foreign-property-fhl-eea`)
      test(IncomeSourceType.`04`, TypeOfLoss.`uk-property-fhl`)
      test(IncomeSourceType.`15`, TypeOfLoss.`foreign-property`)

      def test(incomeSourceType: IncomeSourceType, typeOfLoss: TypeOfLoss): Unit =
        s"convert the downstream incomeSourceType of $incomeSourceType typeOfLoss $typeOfLoss" in {
          val downstreamJson =
            Json.parse(s"""
                 |{
                 |  "incomeSourceId": "$businessId",
                 |  "incomeSourceType": "$incomeSourceType",
                 |  "broughtForwardLossAmount": $lossAmount,
                 |  "taxYear": "$taxYearDownstream",
                 |  "submissionDate": "$lastModified"
                 |}
             """.stripMargin)

          downstreamJson.as[AmendBFLossResponse] shouldBe responseWith(typeOfLoss)
        }
    }

    "reading a self-employment brought forward loss" must {
      test(LossType.INCOME, TypeOfLoss.`self-employment`)
      test(LossType.CLASS4, TypeOfLoss.`self-employment-class4`)

      def test(lossType: LossType, typeOfLoss: TypeOfLoss): Unit =
        s"convert the downstream lossType of $lossType typeOfLoss $typeOfLoss" in {
          val downstreamJson: JsValue =
            Json.parse(s"""
                 |{
                 |  "incomeSourceId": "$businessId",
                 |  "lossType": "$lossType",
                 |  "broughtForwardLossAmount": $lossAmount,
                 |  "taxYear": "$taxYearDownstream",
                 |  "submissionDate": "$lastModified"
                 |}
               """.stripMargin)

          downstreamJson.as[AmendBFLossResponse] shouldBe responseWith(typeOfLoss)
        }
    }
  }

  "Json Writes" should {
    "convert a valid model into MTD JSON" in {
      Json.toJson(responseWith(TypeOfLoss.`self-employment`)) shouldBe Json.parse(
        s"""
          |{
          |  "businessId": "$businessId",
          |  "typeOfLoss": "self-employment",
          |  "lossAmount": $lossAmount,
          |  "taxYearBroughtForwardFrom": "$taxYear",
          |  "lastModified": "$lastModified"
          |}
      """.stripMargin
      )
    }
  }

  "HateoasFactory" must {
    class Test extends MockAppConfig {
      val hateoasFactory = new HateoasFactory(mockAppConfig)
      val nino           = "someNino"
      val lossId         = "someLossId"

      // WLOG
      val bfLossResponse: AmendBFLossResponse = responseWith(typeOfLoss = TypeOfLoss.`self-employment`)

      MockAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes()
    }

    "expose the correct links" in new Test {
      hateoasFactory.wrap(bfLossResponse, AmendBFLossHateoasData(nino, lossId)) shouldBe
        HateoasWrapper(
          bfLossResponse,
          Seq(
            Link(s"/individuals/losses/$nino/brought-forward-losses/$lossId", GET, "self"),
            Link(s"/individuals/losses/$nino/brought-forward-losses/$lossId/change-loss-amount", POST, "amend-brought-forward-loss"),
            Link(s"/individuals/losses/$nino/brought-forward-losses/$lossId", DELETE, "delete-brought-forward-loss")
          )
        )
    }
  }
}
