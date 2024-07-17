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

package v5.bfLoss

import api.models.domain.TaxYear
import play.api.libs.json.{JsValue, Json}
import v5.bfLossClaims.list.def1.model.response.{Def1_ListBFLossesItem, Def1_ListBFLossesResponse}
import v5.bfLossClaims.list.model._

object ListBFLossesFixtures {

  def listBFLossesModel(taxYear: String): Def1_ListBFLossesItem = Def1_ListBFLossesItem(
    lossId = "lossId",
    businessId = "businessId",
    typeOfLoss = TypeOfLoss.`uk-property-fhl`,
    lossAmount = 2.75,
    taxYearBroughtForwardFrom = taxYear,
    lastModified = "2021-11-05T11:56:271Z"
  )

  def singleBFLossesResponseModel(taxYear: String): Def1_ListBFLossesResponse = Def1_ListBFLossesResponse(
    List(listBFLossesModel(taxYear))
  )

  val multipleBFLossesResponseModel: Def1_ListBFLossesResponse = Def1_ListBFLossesResponse(
    List(
      listBFLossesModel("2019-20"),
      listBFLossesModel("2020-21"),
      listBFLossesModel("2021-22"),
      listBFLossesModel("2022-23")
    )
  )

  def mtdLossJsonWithSelfEmployment(nino: String, taxYear: String): JsValue = Json.parse(
    s"""
       |{
       |  "lossId": "AAZZ1234567890A",
       |  "businessId": "XAIS12345678911",
       |  "typeOfLoss": "self-employment",
       |  "lossAmount": 345.67,
       |  "taxYearBroughtForwardFrom": "$taxYear",
       |  "lastModified": "2020-07-13T12:13:48.763Z",
       |  "links": [
       |    {
       |      "href": "/individuals/losses/$nino/brought-forward-losses/AAZZ1234567890A",
       |      "rel": "self",
       |      "method": "GET"
       |    }
       |  ]
       |}
      """.stripMargin
  )

  def mtdLossJsonWithUkPropertyFhl(nino: String, taxYear: String): JsValue = Json.parse(
    s"""
       |{
       |  "lossId": "AAZZ1234567890B",
       |  "businessId": "XAIS12345678912",
       |  "typeOfLoss": "uk-property-fhl",
       |  "lossAmount": 385.67,
       |  "taxYearBroughtForwardFrom": "$taxYear",
       |  "lastModified": "2020-08-13T12:13:48.763Z",
       |  "links": [
       |    {
       |      "href": "/individuals/losses/$nino/brought-forward-losses/AAZZ1234567890B",
       |      "rel": "self",
       |      "method": "GET"
       |    }
       |  ]
       |}
        """.stripMargin
  )

  def downstreamResponseJsonWithLossType(taxYear: String = "2019-20"): JsValue = Json.parse(
    s"""
      |[
      |  {
      |    "incomeSourceId": "XAIS12345678911",
      |    "lossType": "INCOME",
      |    "broughtForwardLossAmount": 345.67,
      |    "taxYear": "${TaxYear.fromMtd(taxYear).asDownstream}",
      |    "lossId": "AAZZ1234567890A",
      |    "submissionDate": "2020-07-13T12:13:48.763Z"
      |  }
      |]
   """.stripMargin
  )

  def downstreamResponseJsonWithIncomeSourceType(taxYear: String = "2019-20"): JsValue = Json.parse(
    s"""
       |[
       |  {
       |    "incomeSourceId": "XAIS12345678912",
       |    "incomeSourceType": "04",
       |    "broughtForwardLossAmount": 385.67,
       |    "taxYear": "${TaxYear.fromMtd(taxYear).asDownstream}",
       |    "lossId": "AAZZ1234567890B",
       |    "submissionDate": "2020-08-13T12:13:48.763Z"
       |  }
       |]
   """.stripMargin
  )

  def baseHateoasLinks(nino: String): JsValue = Json.parse(
    s"""
      |[
      |  {
      |    "href": "/individuals/losses/$nino/brought-forward-losses",
      |    "rel": "self",
      |    "method": "GET"
      |  },
      |  {
      |    "href": "/individuals/losses/$nino/brought-forward-losses",
      |    "rel": "create-brought-forward-loss",
      |    "method": "POST"
      |  }
      |]
   """.stripMargin
  )

}
