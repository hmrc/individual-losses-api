/*
 * Copyright 2021 HM Revenue & Customs
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

import mocks.MockAppConfig
import play.api.Configuration
import support.UnitSpec

class ApiDefinitionFactorySpec extends UnitSpec with MockAppConfig {
  class Test {
    val factory = new ApiDefinitionFactory(mockAppConfig)
  }

  "definition" when {
    "there is no appConfig.apiStatus" should {
      "default apiStatus to ALPHA" in new Test {
        MockedAppConfig.apiGatewayContext returns "my/context"
        MockedAppConfig.featureSwitch returns None anyNumberOfTimes()
        MockedAppConfig.apiStatus("1.0") returns ""
        MockedAppConfig.apiStatus("2.0") returns ""
        MockedAppConfig.endpointsEnabled returns true anyNumberOfTimes()

        factory.definition shouldBe Definition(
          scopes = Seq(
                     Scope(
                         key = "read:self-assessment",
                         name = "View your Self Assessment information",
                         description = "Allow read access to self assessment data"
                 ),
                 Scope(
                     key = "write:self-assessment",
                     name = "Change your Self Assessment information",
                     description = "Allow write access to self assessment data"
                 )
               ),
               api = APIDefinition(
                   name = "Individual Losses (MTD)",
                   description = "An API for providing individual losses data",
                   context = "my/context",
                   versions = Seq(
                       APIVersion(
                           version = "1.0", status = APIStatus.ALPHA, endpointsEnabled = true),
                       APIVersion(
                           version = "2.0", status = APIStatus.ALPHA, endpointsEnabled = true)
                 ),
                 requiresTrust = None
               )
        )
      }
    }
    "featureSwitch.isWhiteListingEnabled is false" should {
      "have no access" in new Test {
        MockedAppConfig.apiGatewayContext returns "my/context"
        MockedAppConfig.featureSwitch returns Some(Configuration("white-list.enabled" -> false)) anyNumberOfTimes()
        MockedAppConfig.apiStatus("1.0") returns "BETA"
        MockedAppConfig.apiStatus("2.0") returns "BETA"
        MockedAppConfig.endpointsEnabled returns true anyNumberOfTimes()

        factory.definition shouldBe Definition(
          scopes = Seq(
                     Scope(
                         key = "read:self-assessment",
                         name = "View your Self Assessment information",
                         description = "Allow read access to self assessment data"
                 ),
                 Scope(
                     key = "write:self-assessment",
                     name = "Change your Self Assessment information",
                     description = "Allow write access to self assessment data"
                 )
               ),
               api = APIDefinition(
                   name = "Individual Losses (MTD)",
                   description = "An API for providing individual losses data",
                   context = "my/context",
                   versions = Seq(
                       APIVersion(
                           version = "1.0", status = APIStatus.BETA, endpointsEnabled = true),
                       APIVersion(
                           version = "2.0", status = APIStatus.BETA, endpointsEnabled = true)
                 ),
                 requiresTrust = None
               )
        )
      }
    }
    "featureSwitch.isWhiteListingEnabled is true" should {
      "return an access" in new Test {
        MockedAppConfig.apiGatewayContext returns "my/context"
        MockedAppConfig.featureSwitch returns Some(Configuration(
          "white-list.enabled" -> true,
          "white-list.applicationIds" -> Seq("id1", "id2")
        )) anyNumberOfTimes()
        MockedAppConfig.apiStatus("1.0") returns "BETA"
        MockedAppConfig.apiStatus("2.0") returns "BETA"
        MockedAppConfig.endpointsEnabled returns true anyNumberOfTimes()

        factory.definition shouldBe Definition(
          scopes = Seq(
                     Scope(
                         key = "read:self-assessment",
                         name = "View your Self Assessment information",
                         description = "Allow read access to self assessment data"
                 ),
                 Scope(
                     key = "write:self-assessment",
                     name = "Change your Self Assessment information",
                     description = "Allow write access to self assessment data"
                 )
               ),
               api = APIDefinition(
                   name = "Individual Losses (MTD)",
                   description = "An API for providing individual losses data",
                   context = "my/context",
                   versions = Seq(
                       APIVersion(
                           version = "1.0", access = Some(Access("PRIVATE", Seq("id1", "id2"))), status = APIStatus.BETA, endpointsEnabled = true),
                       APIVersion(
                           version = "2.0", access = Some(Access("PRIVATE", Seq("id1", "id2"))), status = APIStatus.BETA, endpointsEnabled = true)
                 ),
                 requiresTrust = None
               )
        )
      }
    }
  }
}
