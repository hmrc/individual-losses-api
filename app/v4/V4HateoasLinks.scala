/*
 * Copyright 2024 HM Revenue & Customs
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

package v4

import shared.config.AppConfig
import shared.hateoas.Link
import shared.hateoas.Method.{DELETE, GET, POST, PUT}
import v4.RelType._

trait V4HateoasLinks {

  // Domain URIs
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

  private def lossClaimOrderUri(appConfig: AppConfig, nino: String): String =
    lossClaimsBaseUri(appConfig, nino) + s"/order"

  private def lossClaimOrderTaxYearClaimedForUri(appConfig: AppConfig, nino: String, taxYearClaimedFor: String): String =
    lossClaimOrderUri(appConfig, nino) + s"/$taxYearClaimedFor"

  private def lossClaimChangeRequest(appConfig: AppConfig, nino: String, lossId: String): String =
    lossClaimUri(appConfig, nino, lossId) + "/change-type-of-claim"

  // API resource links
  def createBfLoss(appConfig: AppConfig, nino: String): Link =
    Link(href = bfLossBaseUri(appConfig, nino), method = POST, rel = CREATE_BF_LOSS)

  def getBFLoss(appConfig: AppConfig, nino: String, lossId: String): Link =
    Link(href = bfLossUri(appConfig, nino, lossId), method = GET, rel = SELF)

  def amendBfLoss(appConfig: AppConfig, nino: String, lossId: String): Link =
    Link(href = bfLossChangeRequest(appConfig, nino, lossId), method = POST, rel = AMEND_BF_LOSS)

  def deleteBfLoss(appConfig: AppConfig, nino: String, lossId: String): Link =
    Link(href = bfLossUri(appConfig, nino, lossId), method = DELETE, rel = DELETE_BF_LOSS)

  def listBfLoss(appConfig: AppConfig, nino: String): Link = Link(href = bfLossBaseUri(appConfig, nino), method = GET, rel = SELF)

  def createLossClaim(appConfig: AppConfig, nino: String): Link =
    Link(href = lossClaimsBaseUri(appConfig, nino), method = POST, rel = CREATE_LOSS_CLAIM)

  def getLossClaim(appConfig: AppConfig, nino: String, claimId: String): Link =
    Link(href = lossClaimUri(appConfig, nino, claimId), method = GET, rel = SELF)

  def amendLossClaimType(appConfig: AppConfig, nino: String, claimId: String): Link =
    Link(href = lossClaimChangeRequest(appConfig, nino, claimId), method = POST, rel = AMEND_LOSS_CLAIM)

  def deleteLossClaim(appConfig: AppConfig, nino: String, claimId: String): Link =
    Link(href = lossClaimUri(appConfig, nino, claimId), method = DELETE, rel = DELETE_LOSS_CLAIM)

  def amendLossClaimOrder(appConfig: AppConfig, nino: String, taxYearClaimedFor: String, rel: String = AMEND_LOSS_CLAIM_ORDER): Link =
    Link(href = lossClaimOrderTaxYearClaimedForUri(appConfig, nino, taxYearClaimedFor), method = PUT, rel)

  def listLossClaim(appConfig: AppConfig, nino: String, rel: String = SELF): Link =
    Link(href = lossClaimsBaseUri(appConfig, nino), method = GET, rel)

}

object RelType {
  val DELETE_BF_LOSS = "delete-brought-forward-loss"
  val AMEND_BF_LOSS  = "amend-brought-forward-loss"
  val CREATE_BF_LOSS = "create-brought-forward-loss"

  val AMEND_LOSS_CLAIM       = "amend-loss-claim"
  val DELETE_LOSS_CLAIM      = "delete-loss-claim"
  val CREATE_LOSS_CLAIM      = "create-loss-claim"
  val LIST_LOSS_CLAIMS       = "list-loss-claims"
  val AMEND_LOSS_CLAIM_ORDER = "amend-loss-claim-order"

  val SELF = "self"
}
