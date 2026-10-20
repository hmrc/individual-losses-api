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

package api.definition

import api.config.Deprecation.NotDeprecated
import api.config.MockAppConfig
import api.definition.APIStatus.{ALPHA, BETA}
import api.mocks.MockHttpClient
import api.routing.{Version6, Version7, Version9}
import api.utils.UnitSpec
import cats.implicits.catsSyntaxValidatedId

import scala.language.reflectiveCalls

class ApiDefinitionFactorySpec extends UnitSpec with MockAppConfig {

  "definition" when {
    "called" should {
      "return a valid Definition case class" in {
        List(Version6, Version7).foreach { version =>
          MockedAppConfig.apiGatewayContext.anyNumberOfTimes() returns "individuals/losses"
          MockedAppConfig.apiStatus(version) returns "BETA"
          MockedAppConfig.endpointsEnabled(version).returns(true).anyNumberOfTimes()
          MockedAppConfig.controlledAccessEnabled returns false
          MockedAppConfig.deprecationFor(version).returns(NotDeprecated.valid).anyNumberOfTimes()
        }

        val factory = new ApiDefinitionFactory(mockAppConfig)

        factory.definition shouldBe
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
                  access = APIAccessType.PUBLIC,
                  endpointsEnabled = true
                ),
                APIVersion(
                  Version7,
                  status = BETA,
                  access = APIAccessType.PUBLIC,
                  endpointsEnabled = true
                )
              ),
              requiresTrust = None
            )
          )
      }
    }
  }

  "set the access level" when {
    "the controlled access flag is enabled" should {
      "to be CONTROLLED" in {
        List(Version6, Version7).foreach { version =>
          MockedAppConfig.apiGatewayContext.anyNumberOfTimes() returns "individuals/losses"
          MockedAppConfig.apiStatus(version) returns "BETA"
          MockedAppConfig.endpointsEnabled(version).returns(true).anyNumberOfTimes()
          MockedAppConfig.controlledAccessEnabled returns true
          MockedAppConfig.deprecationFor(version).returns(NotDeprecated.valid).anyNumberOfTimes()
        }

        val factory = new ApiDefinitionFactory(mockAppConfig)

        factory.definition.api.versions.head.access shouldBe APIAccessType.CONTROLLED
      }
    }

    "the controlled access flag is disabled" should {
      "return PUBLIC" in {
        List(Version6, Version7).foreach { version =>
          MockedAppConfig.apiGatewayContext.anyNumberOfTimes() returns "individuals/losses"
          MockedAppConfig.apiStatus(version) returns "BETA"
          MockedAppConfig.endpointsEnabled(version).returns(true).anyNumberOfTimes()
          MockedAppConfig.controlledAccessEnabled returns false
          MockedAppConfig.deprecationFor(version).returns(NotDeprecated.valid).anyNumberOfTimes()
        }

        val factory = new ApiDefinitionFactory(mockAppConfig)

        factory.definition.api.versions.head.access shouldBe APIAccessType.PUBLIC
      }
    }
  }

  "buildAPIStatus" when {
    "the 'apiStatus' parameter is present and valid" should {
      "return the expected status" in new Test {
        setupMockConfig(Version9)
        MockedAppConfig.apiStatus(Version9) returns "BETA"

        checkBuildApiStatus(Version9) shouldBe BETA
      }
    }

    "the 'apiStatus' parameter is present but invalid" should {
      "default to alpha" in new Test {
        setupMockConfig(Version9)
        MockedAppConfig.apiStatus(Version9) returns "not-a-status"

        checkBuildApiStatus(Version9) shouldBe ALPHA
      }
    }

    "the 'deprecatedOn' parameter is missing for a deprecated version" should {
      "throw an exception" in new Test {
        MockedAppConfig.apiStatus(Version9) returns "DEPRECATED"
        MockedAppConfig.deprecationFor(Version9).returns("deprecatedOn date is required for a deprecated version".invalid).anyNumberOfTimes()

        val exception: Exception = intercept[Exception] {
          checkBuildApiStatus(Version9)
        }

        exception.getMessage shouldBe "deprecatedOn date is required for a deprecated version"
      }
    }
  }

  trait Test extends UnitSpec with MockHttpClient with MockAppConfig {
    MockedAppConfig.apiGatewayContext returns "individuals/self-assessment/adjustable-summary"

    val apiDefinitionFactory: ApiDefinitionFactory = new ApiDefinitionFactory(mockAppConfig)

    def checkBuildApiStatus(version: api.routing.Version): APIStatus =
      apiDefinitionFactory.buildAPIStatus(version)

    protected def setupMockConfig(version: api.routing.Version): Unit =
      MockedAppConfig.deprecationFor(version).returns(NotDeprecated.valid).anyNumberOfTimes()

  }

}
