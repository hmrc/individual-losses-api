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

package v1.hateoas

import cats.Functor
import javax.inject.Inject
import v1.models.hateoas.RelType._
import v1.models.hateoas._

import scala.language.higherKinds

class HateoasFactory @Inject()() extends Hateoas {

  def wrap[A, D <: HateoasData](payload: A, data: D)(implicit lf: HateoasLinksFactory[A, D]): HateoasWrapper[A] = {
    val links = lf.links(data)

    HateoasWrapper(payload, links)
  }

  def wrapList[A[_]: Functor, I, D](payload: A[I], data: D)(implicit lf: HateoasListLinksFactory[A, I, D]): HateoasWrapper[A[HateoasWrapper[I]]] = {
   val hateoasList =  Functor[A].map(payload)(i => HateoasWrapper(i, lf.itemLinks(data, i)))

    HateoasWrapper(hateoasList, lf.links(data))
  }
}

trait HateoasLinksFactory[A, D] {
  def links(data: D): Seq[Link]
}

trait HateoasListLinksFactory[A[_], I, D] {
  def itemLinks(data: D, item: I): Seq[Link]
  def links(data: D): Seq[Link]
}

trait Hateoas {

  //Domain URIs
  private val collectionUri: String => String                 = nino => s"/individuals/losses/$nino/brought-forward-losses"
  private val bfLossUri: (String, String) => String           = (nino, lossId) => collectionUri(nino) + s"/$lossId"
  private val bfLossChangeRequest: (String, String) => String = (nino, lossId) => bfLossUri(nino, lossId) + "/change-loss-amount"

  //API resource links
  def createBfLoss(nino: String): Link                 = Link(href = collectionUri(nino), method = "POST", rel = CREATE_BF_LOSS)
  def getBFLoss(nino: String, lossId: String): Link    = Link(href = bfLossUri(nino, lossId), method = "GET", rel = GET_BF_LOSS)
  def amendBfLoss(nino: String, lossId: String): Link  = Link(href = bfLossChangeRequest(nino, lossId), method = "POST", rel = AMEND_BF_LOSS)
  def deleteBfLoss(nino: String, lossId: String): Link = Link(href = bfLossUri(nino, lossId), method = "DELETE", rel = DELETE_BF_LOSS)

  //Links for responses
  def selfLink(nino: String, lossId: String): Seq[Link] = List(getBFLoss(nino, lossId).copy(rel = "self"))
}
