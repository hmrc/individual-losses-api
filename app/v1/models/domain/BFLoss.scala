/*
 * Copyright 2019 HM Revenue & Customs
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

package v1.models.domain

import play.api.libs.json._
import v1.models.des.DesBFLoss
import v1.models.requestData.DesTaxYear

case class BFLoss(typeOfLoss: String,
                  selfEmploymentId: Option[String],
                  taxYear: String,
                  lossAmount: BigDecimal) {

  def toDes(broughtForwardLoss: BFLoss) : DesBFLoss = {
    DesBFLoss(lossType = typeOfLoss match {
      case "self-employment" => "INCOME"
      case "self-employment-class4" => "CLASS4"
      case "uk-fhl-property" => "04"
      case "uk-other-property" => "02"
    },
      incomeSourceId =  selfEmploymentId,
      taxYearBroughtForwardFrom = DesTaxYear.fromMtd(taxYear).toString,
      broughtForwardLossAmount = lossAmount)
  }
}

object BFLoss {
  implicit val reads: Reads[BFLoss] = Json.reads[BFLoss]

  implicit val writes: Writes[BFLoss] = Json.writes[BFLoss]
}


