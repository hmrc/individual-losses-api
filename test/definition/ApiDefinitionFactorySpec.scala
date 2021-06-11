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

import com.typesafe.config.ConfigFactory
import config.ConfidenceLevelConfig
import definition.APIStatus.{ALPHA, BETA}
import mocks.MockAppConfig
import support.UnitSpec
import uk.gov.hmrc.auth.core.ConfidenceLevel
import play.api.Configuration

class ApiDefinitionFactorySpec extends UnitSpec with MockAppConfig {

  class Test {
    val factory = new ApiDefinitionFactory(mockAppConfig)
  }

  private val confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200

  "definition" when {
    "there is no appConfig.apiStatus" should {
      "default apiStatus to ALPHA" in new Test {
        MockAppConfig.apiGatewayContext returns "my/context"
        MockAppConfig.featureSwitch returns None anyNumberOfTimes()
        MockAppConfig.apiStatus(status = "1.0") returns ""
        MockAppConfig.apiStatus(status = "2.0") returns ""
        MockAppConfig.endpointsEnabled(version = "1") returns true anyNumberOfTimes()
        MockAppConfig.endpointsEnabled(version = "2") returns true anyNumberOfTimes()
        MockAppConfig.confidenceLevelCheckEnabled returns ConfidenceLevelConfig(definitionEnabled = true, authValidationEnabled = true) anyNumberOfTimes()

        factory.definition shouldBe Definition(
          scopes = Seq(
            Scope(
              key = "read:self-assessment",
              name = "View your Self Assessment information",
              description = "Allow read access to self assessment data",
              confidenceLevel
            ),
            Scope(
              key = "write:self-assessment",
              name = "Change your Self Assessment information",
              description = "Allow write access to self assessment data",
              confidenceLevel
            )
          ),
          api = APIDefinition(
            name = "Individual Losses (MTD)",
            description = "An API for providing individual losses data",
            context = "my/context",
            versions = Seq(
              APIVersion(
                version = "1.0", status = ALPHA, endpointsEnabled = true),
              APIVersion(
                version = "2.0", status = ALPHA, endpointsEnabled = true)
            ),
            requiresTrust = None
          )
        )
      }
    }
  }

  "confidenceLevel" when {
    Seq(
      (true, ConfidenceLevel.L200),
      (false, ConfidenceLevel.L50)
    ).foreach {
      case (definitionEnabled, cl) =>
        s"confidence-level-check.definition.enabled is $definitionEnabled in config" should {
          s"return $cl" in new Test {
            MockAppConfig.confidenceLevelCheckEnabled returns ConfidenceLevelConfig(definitionEnabled = definitionEnabled, authValidationEnabled = true)
            factory.confidenceLevel shouldBe cl
          }
        }
    }
  }

  "buildAPIStatus" when {
    "the 'apiStatus' parameter is present and valid" should {
      "return the correct status" in new Test {
        MockAppConfig.apiStatus(status = "1.0") returns "BETA"
        factory.buildAPIStatus(version = "1.0") shouldBe BETA
      }
    }

    "the 'apiStatus' parameter is present and invalid" should {
      "default to alpha" in new Test {
        MockAppConfig.apiStatus(status = "1.0") returns "ALPHO"
        factory.buildAPIStatus(version = "1.0") shouldBe ALPHA
      }
    }
  }

  "buildWhiteListingAccess" when {
    "the 'featureSwitch' parameter is not present" should {
      "return None" in new Test {
        MockAppConfig.featureSwitch returns None
      }
    }

    "the 'featureSwitch' parameter is present and white listing is enabled" should {
      "return the correct Access object" in new Test {

        private val someString =
          """
            |{
            |   white-list.enabled = true
            |   white-list.applicationIds = ["anId"]
            |}
          """.stripMargin

        MockAppConfig.featureSwitch returns Some(Configuration(ConfigFactory.parseString(someString)))
      }
    }

    "the 'featureSwitch' parameter is present and white listing is not enabled" should {
      "return None" in new Test {
        MockAppConfig.featureSwitch returns Some(Configuration(ConfigFactory.parseString("""white-list.enabled = false""")))
      }
    }
  }
}