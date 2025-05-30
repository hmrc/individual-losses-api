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

package v4.models.request.createLossClaim

import shared.models.domain.TaxYear
import play.api.libs.json._
import shared.config.{ConfigFeatureSwitches, SharedAppConfig}
import v4.models.domain.lossClaim.TypeOfLoss._
import v4.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}

case class CreateLossClaimRequestBody(taxYearClaimedFor: String, typeOfLoss: TypeOfLoss, typeOfClaim: TypeOfClaim, businessId: String)

object CreateLossClaimRequestBody {
  implicit val reads: Reads[CreateLossClaimRequestBody] = Json.reads[CreateLossClaimRequestBody]

  implicit def writes(implicit appConfig: SharedAppConfig): OWrites[CreateLossClaimRequestBody] = (requestBody: CreateLossClaimRequestBody) => {
    val baseJson: JsObject = Json.obj(
      "reliefClaimed"  -> requestBody.typeOfClaim.toReliefClaimed,
      "incomeSourceId" -> requestBody.businessId
    )

    val typeOfLossJson: JsObject = requestBody.typeOfLoss match {
      case `uk-property-non-fhl` | `foreign-property` =>
        Json.obj("incomeSourceType" -> requestBody.typeOfLoss.toIncomeSourceType)
      case `self-employment` => Json.obj()
    }

    val taxYearClaimedForJson: JsObject = if (ConfigFeatureSwitches().isEnabled("ifs_hip_migration_1505")) {
      Json.obj("taxYearClaimedFor" -> TaxYear.fromMtd(requestBody.taxYearClaimedFor).year)
    } else {
      Json.obj("taxYear" -> TaxYear.fromMtd(requestBody.taxYearClaimedFor).asDownstream)
    }

    baseJson ++ typeOfLossJson ++ taxYearClaimedForJson
  }

}
