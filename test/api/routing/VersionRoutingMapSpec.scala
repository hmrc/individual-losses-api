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

package api.routing

import api.utils.UnitSpec
import play.api.routing.Router
import play.api.routing.sird._
import play.api.mvc.Handler

class VersionRoutingMapSpec extends UnitSpec {

  object DefaultHandler extends Handler
  object V6Handler      extends Handler
  object V7Handler      extends Handler

  private val defaultRoute = Router.from { case GET(p"") =>
    DefaultHandler
  }

  private val v6Router = Router.from { case GET(p"/v6") =>
    V6Handler
  }

  private val v7Router = Router.from { case GET(p"/v7") =>
    V7Handler
  }

  private val routingMap = new VersionRoutingMap {
    override val defaultRouter: Router = defaultRoute

    override val map: Map[Version, Router] = Map(
      Version6 -> v6Router,
      Version7 -> v7Router
    )

  }

  "VersionRoutingMap" should {
    "return the router for a supported version" in {
      routingMap.versionRouter(Version6) shouldBe Some(v6Router)
      routingMap.versionRouter(Version7) shouldBe Some(v7Router)
    }

    "return None for an unsupported version" in {
      routingMap.versionRouter(Version9) shouldBe None
    }
  }

}
