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

package v4.models.response.listBFLosses

import play.api.libs.json.Json
import shared.utils.UnitSpec
import v4.models.domain.bfLoss.{IncomeSourceType, LossType, TypeOfLoss}

class ListBFLossesItemSpec extends UnitSpec {

  val lossId            = "000000123456789"
  val businessId        = "businessId"
  val lastModified      = "2020-07-13T12:13:48.763Z"
  val lossAmount        = 1.75
  val taxYearMtd        = "2019-20"
  val taxYearDownstream = "2020"

  "json writes" must {
    "output as per spec" in {
      Json.toJson(
        ListBFLossesItem(
          lossId = lossId,
          businessId = businessId,
          typeOfLoss = TypeOfLoss.`self-employment`,
          lossAmount = lossAmount,
          taxYearBroughtForwardFrom = taxYearMtd,
          lastModified = lastModified
        )) shouldBe
        Json.parse(
          s"""
            |{
            |   "lossId": "$lossId",
            |   "businessId": "$businessId",
            |   "typeOfLoss": "self-employment",
            |   "lossAmount": $lossAmount,
            |   "taxYearBroughtForwardFrom": "$taxYearMtd",
            |   "lastModified": "$lastModified"
            |}
        """.stripMargin
        )
    }
  }

  "json reads" must {
    "work for employment losses" when {
      checkReadsWith(LossType.INCOME, TypeOfLoss.`self-employment`)
      checkReadsWith(LossType.CLASS4, TypeOfLoss.`self-employment-class4`)

      def checkReadsWith(lossType: LossType, expectedTypeOfLoss: TypeOfLoss): Unit =
        s"income source type is $lossType" in {
          Json
            .parse(s"""
                       |{
                       |   "incomeSourceId": "$businessId",
                       |   "lossType": "$lossType",
                       |   "broughtForwardLossAmount": $lossAmount,
                       |   "taxYear": "$taxYearDownstream",
                       |   "lossId": "$lossId",
                       |   "submissionDate": "$lastModified"
                       |}""".stripMargin)
            .as[ListBFLossesItem] shouldBe
            ListBFLossesItem(
              lossId = lossId,
              businessId = businessId,
              typeOfLoss = expectedTypeOfLoss,
              lossAmount = lossAmount,
              taxYearBroughtForwardFrom = taxYearMtd,
              lastModified = lastModified
            )
        }
    }

    "work for property losses" when {
      checkReadsWith(IncomeSourceType.`02`, TypeOfLoss.`uk-property-non-fhl`)
      checkReadsWith(IncomeSourceType.`03`, TypeOfLoss.`foreign-property-fhl-eea`)
      checkReadsWith(IncomeSourceType.`04`, TypeOfLoss.`uk-property-fhl`)
      checkReadsWith(IncomeSourceType.`15`, TypeOfLoss.`foreign-property`)

      def checkReadsWith(incomeSourceType: IncomeSourceType, expectedTypeOfLoss: TypeOfLoss): Unit =
        s"income source type is $incomeSourceType" in {
          Json
            .parse(s"""
                        |{
                        |   "incomeSourceId": "$businessId",
                        |   "incomeSourceType": "$incomeSourceType",
                        |   "broughtForwardLossAmount": $lossAmount,
                        |   "taxYear": "$taxYearDownstream",
                        |   "lossId": "$lossId",
                        |   "submissionDate": "$lastModified"
                        |}""".stripMargin)
            .as[ListBFLossesItem] shouldBe
            ListBFLossesItem(
              lossId = lossId,
              businessId = businessId,
              typeOfLoss = expectedTypeOfLoss,
              lossAmount = lossAmount,
              taxYearBroughtForwardFrom = taxYearMtd,
              lastModified = lastModified
            )
        }
    }
  }

}
