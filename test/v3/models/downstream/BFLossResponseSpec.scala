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

package v3.models.downstream

import mocks.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v3.hateoas.HateoasFactory
import v3.models.domain.TypeOfBFLoss
import v3.models.hateoas.Method.{DELETE, GET, POST}
import v3.models.hateoas.{HateoasWrapper, Link}

class BFLossResponseSpec extends UnitSpec {

  val businessId = "000000000000001"
  val lossAmount = 123.45
  val taxYearDownstream = "2020"
  val taxYear = "2019-20"
  val lastModified = "2018-07-13T12:13:48.763Z"

  def responseWith(typeOfLoss: TypeOfBFLoss): BFLossResponse =
    BFLossResponse(
      businessId = businessId,
      typeOfLoss = typeOfLoss,
      lossAmount = lossAmount,
      taxYearBroughtForwardFrom = taxYear,
      lastModified = lastModified
    )

  "Json Reads" when {
    "reading a property brought forward loss" must {
      test(BFIncomeSourceType.`02`, TypeOfBFLoss.`uk-property-non-fhl`)
      test(BFIncomeSourceType.`03`, TypeOfBFLoss.`foreign-property-fhl-eea`)
      test(BFIncomeSourceType.`04`, TypeOfBFLoss.`uk-property-fhl`)
      test(BFIncomeSourceType.`15`, TypeOfBFLoss.`foreign-property`)

      def test(incomeSourceType: BFIncomeSourceType, typeOfLoss: TypeOfBFLoss): Unit =
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

          downstreamJson.as[BFLossResponse] shouldBe responseWith(typeOfLoss)
        }
    }

    "reading a self-employment brought forward loss" must {
      test(LossType.INCOME, TypeOfBFLoss.`self-employment`)
      test(LossType.CLASS4, TypeOfBFLoss.`self-employment-class4`)

      def test(lossType: LossType, typeOfLoss: TypeOfBFLoss): Unit =
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

          downstreamJson.as[BFLossResponse] shouldBe responseWith(typeOfLoss)
        }
    }
  }

  "Json Writes" should {
    "convert a valid model into MTD JSON" in {
      Json.toJson(responseWith(TypeOfBFLoss.`self-employment`)) shouldBe Json.parse(
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
      val bfLossResponse: BFLossResponse = responseWith(typeOfLoss = TypeOfBFLoss.`self-employment`)

      MockAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes
    }

    "expose the correct links for retrieve" in new Test {
      hateoasFactory.wrap(bfLossResponse, GetBFLossHateoasData(nino, lossId)) shouldBe
        HateoasWrapper(
          bfLossResponse,
          Seq(
            Link(s"/individuals/losses/$nino/brought-forward-losses/$lossId", GET, "self"),
            Link(s"/individuals/losses/$nino/brought-forward-losses/$lossId/change-loss-amount", POST, "amend-brought-forward-loss"),
            Link(s"/individuals/losses/$nino/brought-forward-losses/$lossId", DELETE, "delete-brought-forward-loss")
          )
        )
    }

    "expose the correct links for amend" in new Test {
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
