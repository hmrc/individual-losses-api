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

import config.AppConfig
import v1.models.hateoas.Link
import v1.models.hateoas.Method._
import v1.models.hateoas.RelType._

trait HateoasLinks {

  //Domain URIs
  private def bfLossBaseUri(appConfig: AppConfig, nino: String): String =
    s"/${appConfig.apiGatewayContext}/$nino/brought-forward-losses"

  private def bfLossUri(appConfig: AppConfig, nino: String, lossId: String): String =
    bfLossBaseUri(appConfig, nino) + s"/$lossId"

  private def bfLossChangeRequest(appConfig: AppConfig, nino: String, lossId: String): String =
    bfLossUri(appConfig, nino, lossId) + "/change-loss-amount"

  private def lossClaimsBaseUri(appConfig: AppConfig, nino: String): String =
    s"/${appConfig.apiGatewayContext}/$nino/loss-claims"

  private def lossClaimUri(appConfig: AppConfig, nino: String, claimId: String): String =
    lossClaimsBaseUri(appConfig, nino) + s"/$claimId"

  private def lossClaimChangeRequest(appConfig: AppConfig, nino: String, lossId: String): String =
    lossClaimUri(appConfig, nino, lossId) + "/change-type-of-claim"

  //API resource links
  def createBfLoss(appConfig: AppConfig, nino: String): Link =
    Link(href = bfLossBaseUri(appConfig, nino), method = POST, rel = CREATE_BF_LOSS)

  def getBFLoss(appConfig: AppConfig, nino: String, lossId: String): Link =
    Link(href = bfLossUri(appConfig, nino, lossId), method = GET, rel = SELF)

  def amendBfLoss(appConfig: AppConfig, nino: String, lossId: String): Link =
    Link(href = bfLossChangeRequest(appConfig, nino, lossId), method = POST, rel = AMEND_BF_LOSS)

  def deleteBfLoss(appConfig: AppConfig, nino: String, lossId: String): Link =
    Link(href = bfLossUri(appConfig, nino, lossId), method = DELETE, rel = DELETE_BF_LOSS)

  def listBfLoss(appConfig: AppConfig, nino: String): Link = Link(href =bfLossBaseUri(appConfig, nino), method = GET, rel = SELF)

  def getLossClaim(appConfig: AppConfig, nino: String, claimId: String): Link =
    Link(href = lossClaimUri(appConfig, nino, claimId), method = GET, rel = SELF)

  def amendLossClaim(appConfig: AppConfig, nino: String, claimId: String): Link =
    Link(href = lossClaimChangeRequest(appConfig, nino, claimId), method = POST, rel = AMEND_LOSS_CLAIM)

  def deleteLossClaim(appConfig: AppConfig, nino: String, claimId: String): Link =
    Link(href = lossClaimUri(appConfig, nino, claimId), method = DELETE, rel = DELETE_LOSS_CLAIM)
}
