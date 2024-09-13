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

package definition

import shared.config.AppConfig
import shared.routing.{Version4, Version5}
import shared.definition._

import javax.inject.{Inject, Singleton}

@Singleton
class LossesApiDefinitionFactory @Inject() (protected val appConfig: AppConfig) extends ApiDefinitionFactory {

  lazy val definition: Definition =
    Definition(
      api = APIDefinition(
        name = "Individual Losses (MTD)",
        description = "An API for providing individual losses data",
        context = appConfig.apiGatewayContext,
        categories = List("INCOME_TAX_MTD"),
        versions = List(
          APIVersion(
            version = Version4,
            status = buildAPIStatus(Version4),
            endpointsEnabled = appConfig.endpointsEnabled(version = Version4)
          ),
          APIVersion(
            version = Version5,
            status = buildAPIStatus(Version5),
            endpointsEnabled = appConfig.endpointsEnabled(version = Version5)
          )
        ),
        requiresTrust = None
      )
    )

}
