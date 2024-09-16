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

package v4.models.response.amendLossClaimsOrder

import play.api.libs.json.{Json, OWrites}
import shared.config.AppConfig
import shared.hateoas.{HateoasData, HateoasLinksFactory, Link}
import v4.V4HateoasLinks
import v4.RelType.LIST_LOSS_CLAIMS

case class AmendLossClaimsOrderResponse()

object AmendLossClaimsOrderResponse extends V4HateoasLinks {
  implicit val writes: OWrites[AmendLossClaimsOrderResponse] = OWrites[AmendLossClaimsOrderResponse](_ => Json.obj())

  implicit object AmendOrderLinksFactory extends HateoasLinksFactory[AmendLossClaimsOrderResponse, AmendLossClaimsOrderHateoasData] {

    override def links(appConfig: AppConfig, data: AmendLossClaimsOrderHateoasData): Seq[Link] = {
      import data._
      Seq(
        amendLossClaimOrder(appConfig, nino, taxYearClaimedFor),
        listLossClaim(appConfig, nino, LIST_LOSS_CLAIMS)
      )
    }

  }

}

case class AmendLossClaimsOrderHateoasData(nino: String, taxYearClaimedFor: String) extends HateoasData
