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

import cats.Functor
import config.{AppConfig, FeatureSwitch}
import play.api.libs.json._
import v1.hateoas.{HateoasLinks, HateoasListLinksFactory}
import v1.models.hateoas.{HateoasData, Link}
import play.api.libs.functional.syntax._
import v1.models.domain.TypeOfClaim
import v1.models.hateoas.RelType.AMEND_LOSS_CLAIM_ORDER


case class LossClaimId(id: String, sequence: Option[Int], typeOfClaim: TypeOfClaim)

object LossClaimId {
  implicit val writes: OWrites[LossClaimId] = Json.writes[LossClaimId]

  implicit val reads: Reads[LossClaimId] = (
    (JsPath \ "claimId").read[String] and
      (JsPath \ "sequence").readNullable[Int] and
      (JsPath \ "reliefClaimed").read[ReliefClaimed].map(_.toTypeOfClaim)
    )(LossClaimId.apply _)
}

case class ListLossClaimsResponse[I](claims: Seq[I])

object ListLossClaimsResponse extends HateoasLinks {
  implicit def writes[I: Writes]: OWrites[ListLossClaimsResponse[I]] =
    Json.writes[ListLossClaimsResponse[I]]

  implicit def reads[I: Reads]: Reads[ListLossClaimsResponse[I]] =
    implicitly[Reads[Seq[I]]].map(ListLossClaimsResponse(_))

  implicit object LinksFactory extends HateoasListLinksFactory[ListLossClaimsResponse, LossClaimId, ListLossClaimsHateoasData] {
    override def links(appConfig: AppConfig, data: ListLossClaimsHateoasData): Seq[Link] = {
      val featureSwitch = FeatureSwitch(appConfig.featureSwitch)
      val baseLinks = Seq(
        listLossClaim(appConfig, data.nino),
        createLossClaim(appConfig, data.nino)
      )

      val extraLinks = if(featureSwitch.isAmendLossClaimsOrderRouteEnabled) Seq(amendLossClaimOrder(appConfig, data.nino, rel = AMEND_LOSS_CLAIM_ORDER)) else Seq()

      baseLinks ++ extraLinks
    }

    override def itemLinks(appConfig: AppConfig, data: ListLossClaimsHateoasData, item: LossClaimId): Seq[Link] =
      Seq(getLossClaim(appConfig, data.nino, item.id))
  }

  implicit object ResponseFunctor extends Functor[ListLossClaimsResponse] {
    override def map[A, B](fa: ListLossClaimsResponse[A])(f: A => B): ListLossClaimsResponse[B] =
      ListLossClaimsResponse(fa.claims.map(f))
  }
}

case class ListLossClaimsHateoasData(nino: String) extends HateoasData
