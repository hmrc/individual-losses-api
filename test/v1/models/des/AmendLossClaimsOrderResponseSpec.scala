/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.models.des

import mocks.MockAppConfig
import play.api.libs.json.Json
import support.UnitSpec
import v1.models.hateoas.Link
import v1.models.hateoas.Method.{GET, PUT}

class AmendLossClaimsOrderResponseSpec extends UnitSpec with MockAppConfig {

  val nino = "AA123456A"

  "json writes" must {
    "output as per spec" in {
      Json.toJson(AmendLossClaimsOrderResponse()) shouldBe
        Json.parse("""{}""".stripMargin)
    }
  }

  "Links Factory" should {
    "expose the correct links" when {
      "called" in {
        MockedAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes
        AmendLossClaimsOrderResponse.AmendOrderLinksFactory.links(mockAppConfig, AmendLossClaimsOrderHateoasData(nino)) shouldBe
          Seq(
            Link(s"/individuals/losses/$nino/loss-claims/order", PUT, "self"),
              Link(s"/individuals/losses/$nino/loss-claims", GET, "list-loss-claims")
          )
      }
    }
  }

}
