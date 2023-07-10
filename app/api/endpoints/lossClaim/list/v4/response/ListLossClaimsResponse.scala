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

package api.endpoints.lossClaim.list.v4.response

import api.hateoas.{HateoasLinks, HateoasListLinksFactory}
import api.models.hateoas.{HateoasData, Link}
import cats.Functor
import config.AppConfig
import play.api.libs.json._

case class ListLossClaimsResponse[I](claims: Seq[I])

object ListLossClaimsResponse extends HateoasLinks {

  implicit def writes[I: Writes]: OWrites[ListLossClaimsResponse[I]] =
    Json.writes[ListLossClaimsResponse[I]]

  implicit def reads[I: Reads]: Reads[ListLossClaimsResponse[I]] =
    implicitly[Reads[Seq[I]]].map(ListLossClaimsResponse(_))

  implicit object LinksFactory extends HateoasListLinksFactory[ListLossClaimsResponse, ListLossClaimsItem, ListLossClaimsHateoasData] {

    override def links(appConfig: AppConfig, data: ListLossClaimsHateoasData): Seq[Link] = {
      import data._
      val baseLinks = Seq(
        listLossClaim(appConfig, nino),
        createLossClaim(appConfig, nino),
        amendLossClaimOrder(appConfig, nino, taxYearClaimedFor)
      )
      baseLinks
    }

    override def itemLinks(appConfig: AppConfig, data: ListLossClaimsHateoasData, item: ListLossClaimsItem): Seq[Link] =
      Seq(getLossClaim(appConfig, data.nino, item.claimId))

  }

  implicit object ResponseFunctor extends Functor[ListLossClaimsResponse] {

    override def map[A, B](fa: ListLossClaimsResponse[A])(f: A => B): ListLossClaimsResponse[B] =
      ListLossClaimsResponse(fa.claims.map(f))

  }

}

case class ListLossClaimsHateoasData(nino: String, taxYearClaimedFor: String) extends HateoasData
