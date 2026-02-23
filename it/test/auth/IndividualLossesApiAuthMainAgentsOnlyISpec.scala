/*
 * Copyright 2026 HM Revenue & Customs
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

package auth

import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import shared.auth.AuthMainAgentsOnlyISpec
import v7.lossesAndClaims.createAmend.fixtures.CreateAmendLossesAndClaimsFixtures.requestBodyJson

class IndividualLossesApiAuthMainAgentsOnlyISpec extends AuthMainAgentsOnlyISpec {

  val callingApiVersion = "7.0"

  val supportingAgentsNotAllowedEndpoint = "create-or-amend-losses-and-claims"

  val mtdUrl = s"/$nino/businesses/XAIS12345678910/loss-claims/2026-27"

  def sendMtdRequest(request: WSRequest): WSResponse = await(request.put(requestBodyJson))

  val downstreamUri = s"/itsd/reliefs/loss-claims/$nino/XAIS12345678910"

  override val downstreamQueryParam: Map[String, String] = Map("taxYear" -> "26-27")

}
