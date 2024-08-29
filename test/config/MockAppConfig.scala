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

package config

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import routing.Version

trait MockAppConfig extends MockFactory {

  implicit val mockAppConfig: AppConfig = mock[AppConfig]

  object MockedAppConfig {
    // MTD ID Lookup Config
    def mtdIdBaseUrl: CallHandler[String] = (() => mockAppConfig.mtdIdBaseUrl: String).expects()

    // DES Config
    def desBaseUrl: CallHandler[String]                         = (() => mockAppConfig.desBaseUrl: String).expects()
    def desToken: CallHandler[String]                           = (() => mockAppConfig.desToken: String).expects()
    def desEnvironment: CallHandler[String]                     = (() => mockAppConfig.desEnv: String).expects()
    def desEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.desEnvironmentHeaders: Option[Seq[String]]).expects()

    // IFS Config
    def ifsBaseUrl: CallHandler[String]                         = (() => mockAppConfig.ifsBaseUrl: String).expects()
    def ifsToken: CallHandler[String]                           = (() => mockAppConfig.ifsToken: String).expects()
    def ifsEnvironment: CallHandler[String]                     = (() => mockAppConfig.ifsEnv: String).expects()
    def ifsEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.ifsEnvironmentHeaders: Option[Seq[String]]).expects()

    // TYS IFS Config
    def tysIfsBaseUrl: CallHandler[String]                         = (() => mockAppConfig.tysIfsBaseUrl: String).expects()
    def tysIfsToken: CallHandler[String]                           = (() => mockAppConfig.tysIfsToken: String).expects()
    def tysIfsEnvironment: CallHandler[String]                     = (() => mockAppConfig.tysIfsEnv: String).expects()
    def tysIfsEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.tysIfsEnvironmentHeaders: Option[Seq[String]]).expects()

    // HIP Config
    def hipBaseUrl: CallHandler[String] = (() => mockAppConfig.hipBaseUrl: String).expects()

    def hipClientId: CallHandler[String]     = (() => mockAppConfig.hipClientId: String).expects()
    def hipClientSecret: CallHandler[String] = (() => mockAppConfig.hipClientSecret: String).expects()
    def hipEnvironment: CallHandler[String] = (() => mockAppConfig.hipEnv: String).expects()
    def hipEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.hipEnvironmentHeaders: Option[Seq[String]]).expects()

    // API Config
    def featureSwitches: CallHandler[Configuration]              = (() => mockAppConfig.featureSwitches: Configuration).expects()
    def apiGatewayContext: CallHandler[String]                   = (() => mockAppConfig.apiGatewayContext: String).expects()
    def apiDocumentationUrl: CallHandler[String]                 = (() => mockAppConfig.apiDocumentationUrl: String).expects()
    def apiStatus(version: Version): CallHandler[String]         = (mockAppConfig.apiStatus: Version => String).expects(version)
    def isApiDeprecated(version: Version): CallHandler[Boolean]  = (mockAppConfig.isApiDeprecated: Version => Boolean).expects(version)
    def endpointsEnabled(version: String): CallHandler[Boolean]  = (mockAppConfig.endpointsEnabled(_: String)).expects(version)
    def endpointsEnabled(version: Version): CallHandler[Boolean] = (mockAppConfig.endpointsEnabled: Version => Boolean).expects(version)

    def apiVersionReleasedInProduction(version: String): CallHandler[Boolean] =
      (mockAppConfig.apiVersionReleasedInProduction: String => Boolean).expects(version)

    def endpointReleasedInProduction(version: String, key: String): CallHandler[Boolean] =
      (mockAppConfig.endpointReleasedInProduction: (String, String) => Boolean).expects(version, key)

    def confidenceLevelCheckEnabled: CallHandler[ConfidenceLevelConfig] =
      (() => mockAppConfig.confidenceLevelConfig: ConfidenceLevelConfig).expects()

  }

}
