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

package v1.models.des

import play.api.libs.json._
import v1.models.domain.BroughtForwardLoss
import v1.models.requestData.DesTaxYear

case class DesBroughtForwardLoss(lossType: String,
                                 incomeSourceId: Option[String],
                                 taxYearBroughtForwardFrom: String,
                                 broughtForwardLossAmount: BigDecimal){

  def toDes(desBroughtForwardLoss: DesBroughtForwardLoss) : BroughtForwardLoss = {
    BroughtForwardLoss(typeOfLoss = lossType match {
      case "INCOME" => "self-employment"
      case "CLASS4" => "self-employment-class4"
      case "04" => "uk-fhl-property"
      case "02" => "uk-other-property"
    },
      selfEmploymentId =  incomeSourceId,
      taxYear = DesTaxYear.fromDes(taxYearBroughtForwardFrom).toString,
      lossAmount = broughtForwardLossAmount)
  }
}

object DesBroughtForwardLoss {
  implicit val reads: Reads[DesBroughtForwardLoss] = Json.reads[DesBroughtForwardLoss]

  implicit val writes: Writes[DesBroughtForwardLoss] = Json.writes[DesBroughtForwardLoss]
}


