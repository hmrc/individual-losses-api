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

package definition

import api.config.Deprecation.NotDeprecated
import api.config.MockAppConfig
import api.definition.*
import api.definition.APIStatus.BETA
import api.mocks.MockHttpClient
import api.routing.{Version6, Version7}
import api.utils.UnitSpec
import cats.implicits.catsSyntaxValidatedId

class LossesApiDefinitionFactorySpec extends UnitSpec with MockHttpClient with MockAppConfig {

  "definition" when {
    "called" should {
      "return a valid Definition case class" in {
        List(Version6, Version7).foreach { version =>
          MockedAppConfig.apiGatewayContext.anyNumberOfTimes() returns "individuals/losses"
          MockedAppConfig.apiStatus(version) returns "BETA"
          MockedAppConfig.endpointsEnabled(version).returns(true).anyNumberOfTimes()
          MockedAppConfig.deprecationFor(version).returns(NotDeprecated.valid).anyNumberOfTimes()
        }

        val apiDefinitionFactory = new LossesApiDefinitionFactory(mockAppConfig)

        apiDefinitionFactory.definition shouldBe
          Definition(
            api = APIDefinition(
              name = "Individual Losses (MTD)",
              description = "An API for providing individual losses data",
              context = "individuals/losses",
              categories = List("INCOME_TAX_MTD"),
              versions = List(
                APIVersion(
                  Version6,
                  status = BETA,
                  endpointsEnabled = true
                ),
                APIVersion(
                  Version7,
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
