/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.Configuration

case class FeatureSwitch(value: Option[Configuration]) {
  val DEFAULT_VALUE = true


  def isWhiteListingEnabled: Boolean = {
    value match {
      case Some(config) => config.getOptional[Boolean]("white-list.enabled").getOrElse(false)
      case None => false
    }
  }

  def whiteListedApplicationIds: Seq[String] = {
    value match {
      case Some(config) =>
        config
          .getOptional[Seq[String]]("white-list.applicationIds")
          .getOrElse(throw new RuntimeException(s"feature-switch.white-list.applicationIds is not configured"))
      case None => Seq()
    }
  }
}

sealed case class FeatureConfig(config: Configuration) {

  def isSummaryEnabled(source: String, summary: String): Boolean = {
    val summaryEnabled = config.getOptional[Boolean](s"$source.$summary.enabled") match {
      case Some(flag) => flag
      case None => true
    }
    isSourceEnabled(source) && summaryEnabled
  }

  def isSourceEnabled(source: String): Boolean = {
    config.getOptional[Boolean](s"$source.enabled").getOrElse(true)
  }
}
