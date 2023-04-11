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

import org.apache.commons.lang3.BooleanUtils
import play.api.Configuration
import play.api.mvc.Request
import routing.Version

case class FeatureSwitches(featureSwitchConfig: Configuration) {

  def isVersionEnabled(version: Version): Boolean =
    (for {
      enabled <- featureSwitchConfig.getOptional[Boolean](s"version-${version.configName}.enabled")
    } yield enabled).getOrElse(false)

  def isAmendLossClaimsOrderRouteEnabled: Boolean =
    featureSwitchConfig.getOptional[Boolean]("amend-loss-claim-order.enabled").getOrElse(false)

  val isTaxYearSpecificApiEnabled: Boolean = isEnabled("tys-api.enabled")

  private def isEnabled(key: String): Boolean = featureSwitchConfig.getOptional[Boolean](key).getOrElse(true)

  def isTemporalValidationEnabled(implicit request: Request[_]): Boolean = {
    if (isEnabled("allowTemporalValidationSuspension.enabled")) {
      request.headers.get("suspend-temporal-validations").forall(!BooleanUtils.toBoolean(_))
    } else {
      true
    }
  }

}

object FeatureSwitches {
  def apply()(implicit appConfig: AppConfig): FeatureSwitches = FeatureSwitches(appConfig.featureSwitches)
}
