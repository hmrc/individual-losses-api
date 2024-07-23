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

package v5.bfLoss.amend.def1.response

import api.models.domain.Timestamp
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v5.bfLosses.amend.def1.model.response.Def1_AmendBFLossResponse
import v5.bfLosses.amend.model._

class Def1_AmendBFLossResponseSpec extends UnitSpec {

  private val businessId        = "000000000000001"
  private val lossAmount        = 123.45
  private val taxYearDownstream = "2020"
  private val taxYear           = "2019-20"
  private val lastModified      = Timestamp("2018-07-13T12:13:48.763Z")

  def responseWith(typeOfLoss: TypeOfLoss): Def1_AmendBFLossResponse =
    Def1_AmendBFLossResponse(
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

          downstreamJson.as[Def1_AmendBFLossResponse] shouldBe responseWith(typeOfLoss)
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

          downstreamJson.as[Def1_AmendBFLossResponse] shouldBe responseWith(typeOfLoss)
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

}
