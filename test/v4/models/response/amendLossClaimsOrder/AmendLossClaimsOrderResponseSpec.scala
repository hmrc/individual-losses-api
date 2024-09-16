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

package v4.models.response.amendLossClaimsOrder

import shared.hateoas.Link
import shared.hateoas.Method.{GET, PUT}
import shared.config.MockAppConfig
import play.api.libs.json.Json
import shared.utils.UnitSpec
import v4.models.response.amendLossClaimsOrder.{AmendLossClaimsOrderHateoasData, AmendLossClaimsOrderResponse}

class AmendLossClaimsOrderResponseSpec extends UnitSpec with MockAppConfig {

  val nino: String = "AA123456A"
  val taxYear      = "2018-19"

  "json writes" must {
    "output as per spec" in {
      Json.toJson(AmendLossClaimsOrderResponse()) shouldBe
        Json.parse("""{}""".stripMargin)
    }
  }

  "Links Factory" should {
    "expose the correct links" when {
      "called" in {
        MockedAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes()
        AmendLossClaimsOrderResponse.AmendOrderLinksFactory.links(
          mockAppConfig,
          AmendLossClaimsOrderHateoasData(nino, taxYearClaimedFor = taxYear)) shouldBe
          Seq(
            Link(s"/individuals/losses/$nino/loss-claims/order/$taxYear", PUT, "amend-loss-claim-order"),
            Link(s"/individuals/losses/$nino/loss-claims", GET, "list-loss-claims")
          )
      }
    }
  }

}
