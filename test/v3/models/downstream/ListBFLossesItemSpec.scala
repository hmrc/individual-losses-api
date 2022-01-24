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

import play.api.libs.json.Json
import support.UnitSpec
import v3.models.domain.TypeOfBFLoss

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
          typeOfLoss = TypeOfBFLoss.`self-employment`,
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
      checkReadsWith(LossType.INCOME, TypeOfBFLoss.`self-employment`)
      checkReadsWith(LossType.CLASS4, TypeOfBFLoss.`self-employment-class4`)

      def checkReadsWith(lossType: LossType, expectedTypeOfLoss: TypeOfBFLoss): Unit =
        s"income source type is $lossType" in {
          Json.parse(s"""
                       |{
                       |   "incomeSourceId": "$businessId",
                       |   "lossType": "$lossType",
                       |   "broughtForwardLossAmount": $lossAmount,
                       |   "taxYear": "$taxYearDownstream",
                       |   "lossId": "$lossId",
                       |   "submissionDate": "$lastModified"
                       |}""".stripMargin).as[ListBFLossesItem] shouldBe
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
      checkReadsWith(BFIncomeSourceType.`02`, TypeOfBFLoss.`uk-property-non-fhl`)
      checkReadsWith(BFIncomeSourceType.`03`, TypeOfBFLoss.`foreign-property-fhl-eea`)
      checkReadsWith(BFIncomeSourceType.`04`, TypeOfBFLoss.`uk-property-fhl`)
      checkReadsWith(BFIncomeSourceType.`15`, TypeOfBFLoss.`foreign-property`)

      def checkReadsWith(incomeSourceType: BFIncomeSourceType, expectedTypeOfLoss: TypeOfBFLoss): Unit =
        s"income source type is $incomeSourceType" in {
          Json.parse(s"""
                        |{
                        |   "incomeSourceId": "$businessId",
                        |   "incomeSourceType": "$incomeSourceType",
                        |   "broughtForwardLossAmount": $lossAmount,
                        |   "taxYear": "$taxYearDownstream",
                        |   "lossId": "$lossId",
                        |   "submissionDate": "$lastModified"
                        |}""".stripMargin).as[ListBFLossesItem] shouldBe
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
