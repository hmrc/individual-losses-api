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

package api.endpoints.lossClaim.create.v3.request

import api.endpoints.lossClaim.domain.v3.TypeOfLoss._
import api.endpoints.lossClaim.domain.v3.{TypeOfClaim, TypeOfLoss}
import api.models.domain.TaxYear
import play.api.libs.json._

case class CreateLossClaimRequestBody(taxYearClaimedFor: String, typeOfLoss: TypeOfLoss, typeOfClaim: TypeOfClaim, businessId: String)

object CreateLossClaimRequestBody {
  implicit val reads: Reads[CreateLossClaimRequestBody] = Json.reads[CreateLossClaimRequestBody]

  implicit val writes: OWrites[CreateLossClaimRequestBody] = (requestBody: CreateLossClaimRequestBody) => {
    requestBody.typeOfLoss match {
      case `uk-property-non-fhl` | `foreign-property` =>
        Json.obj(
          "taxYear"          -> TaxYear.fromMtd(requestBody.taxYearClaimedFor).value,
          "incomeSourceType" -> requestBody.typeOfLoss.toIncomeSourceType,
          "reliefClaimed"    -> requestBody.typeOfClaim.toReliefClaimed,
          "incomeSourceId"   -> requestBody.businessId
        )
      case `self-employment` =>
        Json.obj(
          "taxYear"        -> TaxYear.fromMtd(requestBody.taxYearClaimedFor).value,
          "reliefClaimed"  -> requestBody.typeOfClaim.toReliefClaimed,
          "incomeSourceId" -> requestBody.businessId
        )
    }
  }
}
