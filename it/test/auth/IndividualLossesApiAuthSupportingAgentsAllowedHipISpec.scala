/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.http.Status
import play.api.libs.json.{JsValue, Json, OWrites, Writes}
import play.api.libs.ws.{WSRequest, WSResponse}
import shared.auth.AuthSupportingAgentsAllowedISpec
import shared.services.DownstreamStub
import v4.models.domain.lossClaim.TypeOfClaim
import v5.lossClaims.amendOrder.def1.model.request.Claim

class IndividualLossesApiAuthSupportingAgentsAllowedHipISpec extends AuthSupportingAgentsAllowedISpec {

  val callingApiVersion = "6.0"

  val supportingAgentsAllowedEndpoint = "amend-loss-claims-order"

  val mtdUrl = s"/$nino/loss-claims/order/2023-24"

  def sendMtdRequest(request: WSRequest): WSResponse = await(request.put(requestJson()))

  val downstreamUri = s"/itsd/income-sources/claims-for-relief/$nino/preferences"

  override val downstreamQueryParam: Map[String, String] = Map("taxYear" -> "23-24")

  override val downstreamHttpMethod: DownstreamStub.HTTPMethod = DownstreamStub.PUT

  val maybeDownstreamResponseJson: Option[JsValue] = None

  override protected val downstreamSuccessStatus: Int = Status.NO_CONTENT

  private def requestJson(): JsValue = {
    val claims                        = Seq(Claim("1234567890ABEF1", 1), Claim("1234567890ABCDE", 2), Claim("1234567890ABDE0", 3))
    def writes: OWrites[Claim]        = Json.writes[Claim]
    def writesSeq: Writes[Seq[Claim]] = Writes.seq[Claim](writes)
    Json.parse(
      s"""
        |{
        |   "typeOfClaim": "${TypeOfClaim.`carry-sideways`.toString}",
        |   "listOfLossClaims": ${Json.toJson(claims)(writesSeq)}
        |}
      """.stripMargin
    )
  }

}
