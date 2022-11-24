/*
 * Copyright 2022 HM Revenue & Customs
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

package api.endpoints.lossClaim.connector.v3

import api.connectors.ConnectorSpec
import api.mocks.MockHttpClient
import config.MockAppConfig

class LossClaimConnectorSpec extends ConnectorSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890ag"

  class IfsLocalTest extends MockHttpClient with MockAppConfig {

    val connector: LossClaimConnector = new LossClaimConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.ifsBaseUrl returns baseUrl
    MockAppConfig.ifsToken returns "ifs-token"
    MockAppConfig.ifsEnvironment returns "ifs-environment"
    MockAppConfig.ifsEnvironmentHeaders returns Some(allowedIfsHeaders)
  }

  class DesLocalTest extends MockHttpClient with MockAppConfig {

    val connector: LossClaimConnector = new LossClaimConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.desBaseUrl returns baseUrl
    MockAppConfig.desToken returns "des-token"
    MockAppConfig.desEnvironment returns "des-environment"
    MockAppConfig.desEnvironmentHeaders returns Some(allowedDesHeaders)
  }

  class TysIfsLocalTest extends MockHttpClient with MockAppConfig {

    val connector: LossClaimConnector = new LossClaimConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.tysIfsBaseUrl returns baseUrl
    MockAppConfig.tysIfsToken returns "TYS-IFS-token"
    MockAppConfig.tysIfsEnvironment returns "TYS-IFS-environment"
    MockAppConfig.tysIfsEnvironmentHeaders returns Some(allowedTysIfsHeaders)

  }
}
