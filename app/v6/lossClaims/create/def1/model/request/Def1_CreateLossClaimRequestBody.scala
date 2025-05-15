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

package v6.lossClaims.create.def1.model.request

import play.api.libs.json._
import shared.config.{ConfigFeatureSwitches, SharedAppConfig}
import shared.models.domain.TaxYear
import v6.lossClaims.common.models.TypeOfLoss.{`foreign-property`, `self-employment`, `uk-property`}
import v6.lossClaims.common.models.{TypeOfClaim, TypeOfLoss}
import v6.lossClaims.create.model.request.CreateLossClaimRequestBody

case class Def1_CreateLossClaimRequestBody(taxYearClaimedFor: String, typeOfLoss: TypeOfLoss, typeOfClaim: TypeOfClaim, businessId: String)
    extends CreateLossClaimRequestBody

object Def1_CreateLossClaimRequestBody {
  implicit val reads: Reads[Def1_CreateLossClaimRequestBody] = Json.reads[Def1_CreateLossClaimRequestBody]

  implicit def writes(implicit appConfig: SharedAppConfig): OWrites[Def1_CreateLossClaimRequestBody] =
    (requestBody: Def1_CreateLossClaimRequestBody) => {
      val baseJson = Json.obj(
        "reliefClaimed"  -> requestBody.typeOfClaim.toReliefClaimed,
        "incomeSourceId" -> requestBody.businessId
      )

      val typeOfLossJson = requestBody.typeOfLoss match {
        case `uk-property` | `foreign-property` => Json.obj("incomeSourceType" -> requestBody.typeOfLoss.toIncomeSourceType)
        case `self-employment`                  => Json.obj()
      }

      val taxYearClaimedForJson = if (ConfigFeatureSwitches().isEnabled("ifs_hip_migration_1505")) {
        Json.obj("taxYearClaimedFor" -> TaxYear.fromMtd(requestBody.taxYearClaimedFor).year)
      } else {
        Json.obj("taxYear" -> TaxYear.fromMtd(requestBody.taxYearClaimedFor).asDownstream)
      }

      baseJson ++ typeOfLossJson ++ taxYearClaimedForJson
    }

}
