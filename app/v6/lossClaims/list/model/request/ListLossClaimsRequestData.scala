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

package v6.lossClaims.list.model.request

import shared.models.domain.{BusinessId, Nino, TaxYear}
import v6.lossClaims.common.models.*
import v6.lossClaims.list.ListLossClaimsSchema

trait ListLossClaimsRequestData {
  def nino: Nino
  def taxYearClaimedFor: TaxYear
  def typeOfLoss: Option[TypeOfLoss]
  def businessId: Option[BusinessId]
  def typeOfClaim: Option[TypeOfClaim]
  val schema: ListLossClaimsSchema
}
