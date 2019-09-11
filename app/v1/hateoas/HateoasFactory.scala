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
import Endpoints.{AmendBFLoss, CreateBFLoss, Endpoint, GetBFLoss}
import v1.models.hateoas.RelType._
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.outcomes.DesResponse

class HateoasFactory @Inject()() extends Hateoas {

  def wrap[A](nino: String, lossId: String, payload: DesResponse[A], endpoint: Endpoint): DesResponse[HateoasWrapper[A]] = {
    endpoint match {
      case CreateBFLoss => payload.copy(responseData = HateoasWrapper(payload.responseData, linksForCreateBFLoss(nino, lossId)))
      case AmendBFLoss => payload.copy(responseData = HateoasWrapper(payload.responseData, linksForAmendBFLoss(nino, lossId)))
      case GetBFLoss => payload.copy(responseData = HateoasWrapper(payload.responseData, linksForGetBFLoss(nino, lossId)))
    }
  }
}

trait Hateoas {

  private val collectionUri: String => String = nino => s"/individual/losses/$nino/brought-forward-losses"
  private val bfLossUri: (String, String) => String = (nino,lossId) => collectionUri(nino) + s"/$lossId"
  private val bfLossChangeRequest: (String, String) => String = (nino, lossId) => bfLossUri(nino, lossId) + "/change-loss-amount"

  private def createBfLoss(nino: String): Link = Link(href = collectionUri(nino), method = "POST", rel = CREATE_BF_LOSS)
  private def getBFLoss(nino: String, lossId: String): Link = Link(href = bfLossUri(nino, lossId), method = "GET", rel = GET_BF_LOSS)
  private def amendBfLoss(nino: String, lossId: String): Link = Link(href = bfLossChangeRequest(nino, lossId), method = "POST", rel = AMEND_BF_LOSS)
  private def deleteBfLoss(nino: String, lossId: String): Link = Link(href = bfLossUri(nino, lossId), method = "DELETE", rel = DELETE_BF_LOSS)

  def linksForGetBFLoss(nino: String, lossId: String): List[Link] = List(getBFLoss(nino, lossId), amendBfLoss(nino, lossId), deleteBfLoss(nino, lossId))
  def linksForCreateBFLoss(nino: String, lossId: String): List[Link] = List(getBFLoss(nino, lossId), amendBfLoss(nino, lossId), deleteBfLoss(nino, lossId))
  def linksForAmendBFLoss(nino: String, lossId: String): List[Link] = List(getBFLoss(nino, lossId), amendBfLoss(nino, lossId), deleteBfLoss(nino, lossId))

}
