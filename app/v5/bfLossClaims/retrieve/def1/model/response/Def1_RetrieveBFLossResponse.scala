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

package v5.bfLossClaims.retrieve.def1.model.response

import api.hateoas.{HateoasData, HateoasLinks, HateoasLinksFactory, Link}
import api.models.domain.{TaxYear, Timestamp}
import config.AppConfig
import play.api.libs.functional.syntax._
import play.api.libs.json._
import v5.bfLossClaims.retrieve.model.response.RetrieveBFLossResponse
import v5.bfLossClaims.retrieve.model.{IncomeSourceType, LossType, TypeOfLoss}

case class Def1_RetrieveBFLossResponse(businessId: String,
                                  typeOfLoss: TypeOfLoss,
                                  lossAmount: BigDecimal,
                                  taxYearBroughtForwardFrom: String,
                                  lastModified: Timestamp)

object Def1_RetrieveBFLossResponse extends HateoasLinks {
  implicit val writes: OWrites[Def1_RetrieveBFLossResponse] = Json.writes[Def1_RetrieveBFLossResponse]

  implicit val reads: Reads[Def1_RetrieveBFLossResponse] = (
    (__ \ "incomeSourceId").read[String] and
      ((__ \ "lossType").read[LossType].map(_.toTypeOfLoss)
        orElse (__ \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss)) and
      (__ \ "broughtForwardLossAmount").read[BigDecimal] and
      (__ \ "taxYear").read[String].map(TaxYear(_)).map(_.asMtd) and
      (__ \ "submissionDate").read[Timestamp]
  )(Def1_RetrieveBFLossResponse.apply _)

  implicit val downstreamToMtdReads: Reads[RetrieveBFLossResponse] =
    ((__ \ "incomeSourceId").read[String] and
        ((__ \ "lossType").read[LossType].map(_.toTypeOfLoss)
          orElse (__ \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss)) and
        (__ \ "broughtForwardLossAmount").read[BigDecimal] and
        (__ \ "taxYear").read[String].map(TaxYear(_)).map(_.asMtd) and
        (__ \ "submissionDate").read[Timestamp]
      )(Def1_RetrieveBFLossResponse.apply _)

  implicit object GetLinksFactory extends HateoasLinksFactory[RetrieveBFLossResponse, GetBFLossHateoasData] {

    override def links(appConfig: AppConfig, data: GetBFLossHateoasData): Seq[Link] = {
      import data._
      Seq(getBFLoss(appConfig, nino, lossId), amendBfLoss(appConfig, nino, lossId), deleteBfLoss(appConfig, nino, lossId))
    }

  }

}

case class GetBFLossHateoasData(nino: String, lossId: String) extends HateoasData
