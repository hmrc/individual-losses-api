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
import v1.models.requestData.DesTaxYear

case class BFLoss(typeOfLoss: String,
                  selfEmploymentId: Option[String],
                  taxYear: String,
                  lossAmount: BigDecimal)

object BFLoss {
  implicit val reads: Reads[BFLoss] = Json.reads[BFLoss]

  implicit val writes: Writes[BFLoss] = new Writes[BFLoss] {
    override def writes(loss: BFLoss): JsValue = {

      if (isProperty(loss.typeOfLoss))  {
        Json.obj(
          "incomeSourceType" -> convertToDesCode(loss.typeOfLoss),
          "taxYear" -> DesTaxYear.fromMtd(loss.taxYear).toString,
          "broughtForwardLossAmount" -> loss.lossAmount
        )
      }
      else {
        Json.obj(
          "incomeSourceId" -> loss.selfEmploymentId,
          "lossType" -> convertToDesCode(loss.typeOfLoss),
          "taxYear" -> DesTaxYear.fromMtd(loss.taxYear).toString,
          "broughtForwardLossAmount" -> loss.lossAmount
        )
      }
    }
  }

  def isProperty(typeOfLoss: String): Boolean = typeOfLoss.contains("property")

  def convertToDesCode(typeOfLoss: String): String = typeOfLoss match {
    case "self-employment" => "INCOME"
    case "self-employment-class4" => "CLASS4"
    case "uk-property-fhl" => "04"
    case "uk-property-non-fhl" => "02"
  }

  def convertLossTypeToMtdCode(lossType: String): String = lossType match {
    case "INCOME" => "self-employment"
    case "CLASS4" => "self-employment-class4"
  }

  def convertIncomeSourceTypeToMtdCode(lossType: String): String = lossType match {
    case "04" => "uk-property-fhl"
    case "02" => "uk-property-non-fhl"
  }
}
