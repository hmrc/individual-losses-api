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

package v5.lossClaims.fixtures

import play.api.libs.json.{JsValue, Json}
import shared.models.domain.TaxYear
import v5.lossClaims.common.models.{TypeOfClaim, TypeOfLoss}
import v5.lossClaims.list.def1.response.{Def1_ListLossClaimsResponse, ListLossClaimsItem}
import v5.lossClaims.list.model.response.ListLossClaimsResponse

object ListLossClaimsFixtures {

  def listLossClaimsModel(taxYear: String): ListLossClaimsItem = ListLossClaimsItem(
    businessId = "testId",
    typeOfClaim = TypeOfClaim.`carry-sideways`,
    typeOfLoss = TypeOfLoss.`self-employment`,
    taxYearClaimedFor = taxYear,
    claimId = "claimId",
    sequence = Some(1),
    lastModified = "2020-07-13T12:13:48.763Z"
  )

  def nonFhlClaimMtdJson(taxYear: String): JsValue = Json.parse(
    s"""
       |{
       |  "businessId": "XAIS12345678910",
       |  "typeOfLoss": "uk-property-non-fhl",
       |  "typeOfClaim": "carry-sideways",
       |  "taxYearClaimedFor": "${TaxYear.fromMtd(taxYear).asMtd}",
       |  "claimId": "AAZZ1234567890A",
       |  "sequence": 1,
       |  "lastModified": "2020-07-13T12:13:763Z"
       |}
     """.stripMargin
  )

  def selfEmploymentClaimMtdJson(taxYear: String): JsValue = Json.parse(
    s"""
       |{
       |  "businessId": "XAIS12345678911",
       |  "typeOfLoss": "self-employment",
       |  "typeOfClaim": "carry-sideways",
       |  "taxYearClaimedFor": "${TaxYear.fromMtd(taxYear).asMtd}",
       |  "claimId": "AAZZ1234567890B",
       |  "sequence": 1,
       |  "lastModified": "2020-07-13T12:13:763Z"
       |}
       |""".stripMargin
  )

  def singleClaimResponseModel(taxYear: String): ListLossClaimsResponse = Def1_ListLossClaimsResponse(
    List(listLossClaimsModel(taxYear))
  )

  val multipleClaimsResponseModel: ListLossClaimsResponse = Def1_ListLossClaimsResponse(
    List(
      listLossClaimsModel("2019-20"),
      listLossClaimsModel("2020-21"),
      listLossClaimsModel("2021-22"),
      listLossClaimsModel("2022-23")
    )
  )

  def nonFhlDownstreamResponseJson(taxYear: String): JsValue = Json.parse(
    s"""
       |[
       |  {
       |    "incomeSourceId": "XAIS12345678910",
       |    "incomeSourceType": "02",
       |    "reliefClaimed": "CSGI",
       |    "taxYearClaimedFor": "${TaxYear.fromMtd(taxYear).asDownstream}",
       |    "claimId": "AAZZ1234567890A",
       |    "sequence": 1,
       |    "submissionDate": "2020-07-13T12:13:763Z"
       |  }
       |]
   """.stripMargin
  )

  def selfEmploymentDownstreamResponseJson(taxYear: String): JsValue = Json.parse(
    s"""
       |[
       |  {
       |    "incomeSourceId": "XAIS12345678911",
       |    "reliefClaimed": "CSGI",
       |    "taxYearClaimedFor": "${TaxYear.fromMtd(taxYear).asDownstream}",
       |    "claimId": "AAZZ1234567890B",
       |    "sequence": 1,
       |    "submissionDate": "2020-07-13T12:13:763Z"
       |  }
       |]
   """.stripMargin
  )

}
