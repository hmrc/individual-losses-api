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

import javax.inject.Inject
import v1.models.des.{BFLossId, ListBFLossesHateoasResponse}
import v1.models.hateoas.RelType._
import v1.models.hateoas._
import v1.models.outcomes.DesResponse

class HateoasFactory @Inject()() extends Hateoas {

  def wrap[A](data: HateoasData[A]): DesResponse[HateoasWrapper[A]] = {
    data match {
      case data: CreateBFLossHateoasData =>
        data.payload.copy(responseData = HateoasWrapper(data.payload.responseData, linksForCreateBFLoss(data.nino, data.lossId)))
      case data: AmendBFLossHateoasData =>
        data.payload.copy(responseData = HateoasWrapper(data.payload.responseData, linksForAmendBFLoss(data.nino, data.lossId)))
      case data: GetBFLossHateoasData =>
        data.payload.copy(responseData = HateoasWrapper(data.payload.responseData, selfLink(data.nino, data.lossId)))
    }
  }

  //TODO: Make return type generic.  Resolve casting -> B issues within returned HateoasWrapper
  def wrapList[A, B](data: HateoasData[A]): DesResponse[HateoasWrapper[ListBFLossesHateoasResponse]] = {
    data match {
      case data : ListBFLossHateoasData =>
        val list: Seq[BFLossId] = data.payload.responseData.losses
        val hateoasList: Seq[HateoasWrapper[BFLossId]] = list.map { loss => HateoasWrapper(loss, selfLink(data.nino, loss.id))}
        val response: ListBFLossesHateoasResponse = ListBFLossesHateoasResponse(hateoasList)

        data.payload.copy(responseData = HateoasWrapper(response, linksForListBFLoss(data.nino)))
    }
  }
}

trait Hateoas {

  //Domain URIs
  private val collectionUri: String => String = nino => s"/individuals/losses/$nino/brought-forward-losses"
  private val bfLossUri: (String, String) => String = (nino,lossId) => collectionUri(nino) + s"/$lossId"
  private val bfLossChangeRequest: (String, String) => String = (nino, lossId) => bfLossUri(nino, lossId) + "/change-loss-amount"

  //API resource links
  private def createBfLoss(nino: String): Link = Link(href = collectionUri(nino), method = "POST", rel = CREATE_BF_LOSS)
  private def getBFLoss(nino: String, lossId: String): Link = Link(href = bfLossUri(nino, lossId), method = "GET", rel = GET_BF_LOSS)
  private def amendBfLoss(nino: String, lossId: String): Link = Link(href = bfLossChangeRequest(nino, lossId), method = "POST", rel = AMEND_BF_LOSS)
  private def deleteBfLoss(nino: String, lossId: String): Link = Link(href = bfLossUri(nino, lossId), method = "DELETE", rel = DELETE_BF_LOSS)

  //Links for responses
  def selfLink(nino: String, lossId: String): Seq[Link] = List(getBFLoss(nino, lossId).copy(rel = "self"))
  def linksForListBFLoss(nino: String): Seq[Link] = List(createBfLoss(nino))
  def linksForGetBFLoss(nino: String, lossId: String): List[Link] =
    List(getBFLoss(nino, lossId), amendBfLoss(nino, lossId), deleteBfLoss(nino, lossId))
  def linksForCreateBFLoss(nino: String, lossId: String): List[Link] =
    List(getBFLoss(nino, lossId), amendBfLoss(nino, lossId), deleteBfLoss(nino, lossId))
  def linksForAmendBFLoss(nino: String, lossId: String): List[Link] =
    List(getBFLoss(nino, lossId), amendBfLoss(nino, lossId), deleteBfLoss(nino, lossId))

}