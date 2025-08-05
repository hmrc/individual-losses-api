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

package definition

import cats.implicits.catsSyntaxValidatedId
import shared.config.Deprecation.NotDeprecated
import shared.config.MockSharedAppConfig
import shared.definition.*
import shared.definition.APIStatus.BETA
import shared.mocks.MockHttpClient
import shared.routing.{Version4, Version5, Version6}
import shared.utils.UnitSpec

class LossesApiDefinitionFactorySpec extends UnitSpec with MockHttpClient with MockSharedAppConfig {

  "definition" when {
    "called" should {
      "return a valid Definition case class" in {
        List(Version4, Version5, Version6).foreach { version =>
          MockedSharedAppConfig.apiGatewayContext.anyNumberOfTimes() returns "individuals/losses"
          MockedSharedAppConfig.apiStatus(version) returns "BETA"
          MockedSharedAppConfig.endpointsEnabled(version).returns(true).anyNumberOfTimes()
          MockedSharedAppConfig.deprecationFor(version).returns(NotDeprecated.valid).anyNumberOfTimes()
        }

        val apiDefinitionFactory = new LossesApiDefinitionFactory(mockSharedAppConfig)

        apiDefinitionFactory.definition shouldBe
          Definition(
            api = APIDefinition(
              name = "Individual Losses (MTD)",
              description = "An API for providing individual losses data",
              context = "individuals/losses",
              categories = List("INCOME_TAX_MTD"),
              versions = List(
                APIVersion(
                  Version4,
                  status = BETA,
                  endpointsEnabled = true
                ),
                APIVersion(
                  Version5,
                  status = BETA,
                  endpointsEnabled = true
                ),
                APIVersion(
                  Version6,
                  status = BETA,
                  endpointsEnabled = true
                )
              ),
              requiresTrust = None
            )
          )
      }
    }
  }

}
