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

package v5.bfLossClaims.list.def1.model.response

import api.hateoas.{HateoasData, HateoasLinks, HateoasListLinksFactory, Link}
import cats.Functor
import config.AppConfig
import play.api.libs.json._
import v5.bfLossClaims.list.model.response.{ListBFLossesItem, ListBFLossesResponse}

case class Def1_ListBFLossesResponse[I](losses: Seq[I]) extends ListBFLossesResponse[I]

object Def1_ListBFLossesResponse extends HateoasLinks {

  implicit def writes[I: Writes]: OWrites[Def1_ListBFLossesResponse[I]] =
    Json.writes[Def1_ListBFLossesResponse[I]]

  implicit def reads[I: Reads]: Reads[Def1_ListBFLossesResponse[I]] =
    implicitly[Reads[Seq[I]]].map(Def1_ListBFLossesResponse(_))

  implicit object LinksFactory extends HateoasListLinksFactory[Def1_ListBFLossesResponse, ListBFLossesItem, ListBFLossHateoasData] {

    override def links(appConfig: AppConfig, data: ListBFLossHateoasData): Seq[Link] =
      Seq(listBfLoss(appConfig, data.nino), createBfLoss(appConfig, data.nino))

    override def itemLinks(appConfig: AppConfig, data: ListBFLossHateoasData, item: ListBFLossesItem): Seq[Link] =
      Seq(getBFLoss(appConfig, data.nino, item.lossId))

  }

  implicit object ResponseFunctor extends Functor[Def1_ListBFLossesResponse] {

    override def map[A, B](fa: Def1_ListBFLossesResponse[A])(f: A => B): Def1_ListBFLossesResponse[B] =
      Def1_ListBFLossesResponse(fa.losses.map(f))

  }
}

case class ListBFLossHateoasData(nino: String) extends HateoasData
