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

package v4.models.response.listBFLosses

import api.hateoas.{HateoasData, HateoasLinks, HateoasListLinksFactory, Link}
import cats.Functor
import config.AppConfig
import play.api.libs.json._

case class ListBFLossesResponse[I](losses: Seq[I])

object ListBFLossesResponse extends HateoasLinks {

  implicit def writes[I: Writes]: OWrites[ListBFLossesResponse[I]] =
    Json.writes[ListBFLossesResponse[I]]

  implicit def reads[I: Reads]: Reads[ListBFLossesResponse[I]] =
    implicitly[Reads[Seq[I]]].map(ListBFLossesResponse(_))

  implicit object LinksFactory extends HateoasListLinksFactory[ListBFLossesResponse, ListBFLossesItem, ListBFLossHateoasData] {

    override def links(appConfig: AppConfig, data: ListBFLossHateoasData): Seq[Link] =
      Seq(listBfLoss(appConfig, data.nino), createBfLoss(appConfig, data.nino))

    override def itemLinks(appConfig: AppConfig, data: ListBFLossHateoasData, item: ListBFLossesItem): Seq[Link] =
      Seq(getBFLoss(appConfig, data.nino, item.lossId))

  }

  implicit object ResponseFunctor extends Functor[ListBFLossesResponse] {

    override def map[A, B](fa: ListBFLossesResponse[A])(f: A => B): ListBFLossesResponse[B] =
      ListBFLossesResponse(fa.losses.map(f))

  }

}

case class ListBFLossHateoasData(nino: String) extends HateoasData
