/*
 * Copyright 2020 HM Revenue & Customs
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

import config.AppConfig
import play.api.libs.json.{Json, OWrites}
import v1.hateoas.{HateoasLinks, HateoasLinksFactory}
import v1.models.hateoas.RelType.LIST_LOSS_CLAIMS
import v1.models.hateoas.{HateoasData, Link}

case class AmendLossClaimsOrderResponse()

object AmendLossClaimsOrderResponse extends HateoasLinks {
  implicit val writes: OWrites[AmendLossClaimsOrderResponse] = OWrites[AmendLossClaimsOrderResponse](_ => Json.obj())

  implicit object AmendOrderLinksFactory extends HateoasLinksFactory[AmendLossClaimsOrderResponse, AmendLossClaimsOrderHateoasData] {
    override def links(appConfig: AppConfig, data: AmendLossClaimsOrderHateoasData): Seq[Link] = {
      import data._
      Seq(
        amendLossClaimOrder(appConfig, nino),
        listLossClaim(appConfig, nino, rel = LIST_LOSS_CLAIMS)
      )
    }
  }
}

case class AmendLossClaimsOrderHateoasData(nino: String) extends HateoasData
