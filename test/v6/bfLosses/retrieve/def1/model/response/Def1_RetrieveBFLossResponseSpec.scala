/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.bfLosses.retrieve.def1.model.response

import play.api.libs.json.{JsValue, Json}
import shared.models.domain.Timestamp
import shared.utils.UnitSpec
import v6.bfLosses.common.domain.{IncomeSourceType, LossType, TypeOfLoss}
import v6.bfLosses.retrieve.model.response.RetrieveBFLossResponse

class Def1_RetrieveBFLossResponseSpec extends UnitSpec {

  private val businessId           = "000000000000001"
  private val lossAmount           = 123.45
  private val ifsTaxYearDownstream = "2020"
  private val hipTaxYearDownstream = 2020
  private val taxYear              = "2019-20"
  private val lastModified         = Timestamp("2018-07-13T12:13:48.763Z")

  def responseWith(typeOfLoss: TypeOfLoss): RetrieveBFLossResponse =
    Def1_RetrieveBFLossResponse(
      businessId = businessId,
      typeOfLoss = typeOfLoss,
      lossAmount = lossAmount,
      taxYearBroughtForwardFrom = taxYear,
      lastModified = lastModified
    )

  "Json Reads" when {
    "reading a property brought forward loss" must {
      test(IncomeSourceType.`02`, TypeOfLoss.`uk-property`)
      test(IncomeSourceType.`03`, TypeOfLoss.`foreign-property-fhl-eea`)
      test(IncomeSourceType.`04`, TypeOfLoss.`uk-property-fhl`)
      test(IncomeSourceType.`15`, TypeOfLoss.`foreign-property`)

      def test(incomeSourceType: IncomeSourceType, typeOfLoss: TypeOfLoss): Unit =
        s"convert the downstream incomeSourceType of $incomeSourceType typeOfLoss $typeOfLoss" when {
          "feature switch is disabled (IFS enabled)" in {
            val downstreamJson =
              Json.parse(s"""
                   |{
                   |  "incomeSourceId": "$businessId",
                   |  "incomeSourceType": "$incomeSourceType",
                   |  "broughtForwardLossAmount": $lossAmount,
                   |  "taxYear": "$ifsTaxYearDownstream",
                   |  "submissionDate": "$lastModified"
                   |}
             """.stripMargin)

            downstreamJson.as[Def1_RetrieveBFLossResponse] shouldBe responseWith(typeOfLoss)
          }
          "feature switch is enabled (HIP enabled)" in {
            val downstreamJson =
              Json.parse(s"""
                   |{
                   |  "incomeSourceId": "$businessId",
                   |  "incomeSourceType": "$incomeSourceType",
                   |  "broughtForwardLossAmount": $lossAmount,
                   |  "taxYearBroughtForwardFrom": $hipTaxYearDownstream,
                   |  "submissionDate": "$lastModified"
                   |}
             """.stripMargin)

            downstreamJson.as[Def1_RetrieveBFLossResponse] shouldBe responseWith(typeOfLoss)
          }
        }

      "reading a self-employment brought forward loss" must {
        test(LossType.INCOME, TypeOfLoss.`self-employment`)
        test(LossType.CLASS4, TypeOfLoss.`self-employment-class4`)

        def test(lossType: LossType, typeOfLoss: TypeOfLoss): Unit =
          s"convert the downstream lossType of $lossType typeOfLoss $typeOfLoss" when {
            "feature switch is disabled (IFS enabled)" in {
              val downstreamJson: JsValue =
                Json.parse(s"""
                     |{
                     |  "incomeSourceId": "$businessId",
                     |  "lossType": "$lossType",
                     |  "broughtForwardLossAmount": $lossAmount,
                     |  "taxYear": "$ifsTaxYearDownstream",
                     |  "submissionDate": "$lastModified"
                     |}
               """.stripMargin)

              downstreamJson.as[Def1_RetrieveBFLossResponse] shouldBe responseWith(typeOfLoss)
            }

            "feature switch is enabled (HIP enabled)" in {
              val downstreamJson: JsValue =
                Json.parse(s"""
                     |{
                     |  "incomeSourceId": "$businessId",
                     |  "lossType": "$lossType",
                     |  "broughtForwardLossAmount": $lossAmount,
                     |  "taxYearBroughtForwardFrom": $hipTaxYearDownstream,
                     |  "submissionDate": "$lastModified"
                     |}
               """.stripMargin)

              downstreamJson.as[Def1_RetrieveBFLossResponse] shouldBe responseWith(typeOfLoss)
            }
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

}
