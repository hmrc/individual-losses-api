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

import v1.models.hateoas.Link
import v1.models.hateoas.Method._
import v1.models.hateoas.RelType._

trait HateoasLinks {

  //Domain URIs
  private val collectionUri: String => String                 = nino => s"/individuals/losses/$nino/brought-forward-losses"
  private val bfLossUri: (String, String) => String           = (nino, lossId) => collectionUri(nino) + s"/$lossId"
  private val bfLossChangeRequest: (String, String) => String = (nino, lossId) => bfLossUri(nino, lossId) + "/change-loss-amount"

  //API resource links
  def createBfLoss(nino: String): Link                 = Link(href = collectionUri(nino), method = POST, rel = CREATE_BF_LOSS)
  def getBFLoss(nino: String, lossId: String): Link    = Link(href = bfLossUri(nino, lossId), method = GET, rel = SELF)
  def amendBfLoss(nino: String, lossId: String): Link  = Link(href = bfLossChangeRequest(nino, lossId), method = POST, rel = AMEND_BF_LOSS)
  def deleteBfLoss(nino: String, lossId: String): Link = Link(href = bfLossUri(nino, lossId), method = DELETE, rel = DELETE_BF_LOSS)
  def listBfLoss(nino: String): Link = Link(href = collectionUri(nino), method = GET, rel = SELF)

}
