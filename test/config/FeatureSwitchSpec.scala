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

package config

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import routing.{ Version2, Version3 }
import support.UnitSpec

class FeatureSwitchSpec extends UnitSpec {

  private def createFeatureSwitch(config: String) =
    FeatureSwitch(Some(Configuration(ConfigFactory.parseString(config))))

  "version enabled" when {
    val anyVersion = Version2

    "no config" must {
      val featureSwitch = FeatureSwitch(None)

      "return false" in {
        featureSwitch.isVersionEnabled(anyVersion) shouldBe false
      }
    }

    "no config value" must {
      val featureSwitch = createFeatureSwitch("")

      "return false" in {
        featureSwitch.isVersionEnabled(anyVersion) shouldBe false
      }
    }

    "config set" must {
      val featureSwitch = createFeatureSwitch("""
          |version-2.enabled = false
          |version-3.enabled = true
        """.stripMargin)

      "return false for disabled versions" in {
        featureSwitch.isVersionEnabled(Version2) shouldBe false
      }

      "return true for enabled versions" in {
        featureSwitch.isVersionEnabled(Version3) shouldBe true
      }
    }
  }

  "isAmendLossClaimsOrderRouteEnabled" must {
    "return true" when {
      "config set to true" in {
        val featureSwitch = createFeatureSwitch("""
            |amend-loss-claim-order.enabled = true
            |""".stripMargin)

        featureSwitch.isAmendLossClaimsOrderRouteEnabled shouldBe true
      }
    }

    "return false" when {
      "config set to false" in {
        val featureSwitch = createFeatureSwitch("""
            |amend-loss-claim-order.enabled = false
            |""".stripMargin)

        featureSwitch.isAmendLossClaimsOrderRouteEnabled shouldBe false
      }
      "config is missing" in {
        val featureSwitch = FeatureSwitch(None)
        featureSwitch.isAmendLossClaimsOrderRouteEnabled shouldBe false
      }
    }
  }
}
