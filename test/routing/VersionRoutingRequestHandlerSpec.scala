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

package routing

import akka.actor.ActorSystem
import api.models.errors.{InvalidAcceptHeaderError, UnsupportedVersionError}
import com.typesafe.config.{Config, ConfigFactory}
import mocks.MockAppConfig
import org.scalatest.Inside
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.HeaderNames.ACCEPT
import play.api.http.{HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.UnitSpec

class VersionRoutingRequestHandlerSpec extends UnitSpec with Inside with MockAppConfig with GuiceOneAppPerSuite {
  test =>

  implicit private val actorSystem: ActorSystem = ActorSystem("test")
  val action: DefaultActionBuilder              = app.injector.instanceOf[DefaultActionBuilder]

  import play.api.mvc.Handler
  import play.api.routing.sird._

  object DefaultHandler extends Handler
  object V2Handler      extends Handler
  object V3Handler      extends Handler

  private val defaultRouter = Router.from {
    case GET(p"") => DefaultHandler
  }
  private val v2Router = Router.from {
    case GET(p"/resource") => V2Handler
  }
  private val v3Router = Router.from {
    case GET(p"/resource") => V3Handler
  }

  private val routingMap = new VersionRoutingMap {
    override val defaultRouter: Router     = test.defaultRouter
    override val map: Map[Version, Router] = Map(Version2 -> v2Router, Version3 -> v3Router)
  }

  private val confWithAllEnabled: Config = ConfigFactory.parseString("""
      |version-2.enabled = true
      |version-3.enabled = true
    """.stripMargin)

  private val confWithV3Disabled: Config = ConfigFactory.parseString("""
      |version-2.enabled = true
      |version-3.enabled = false
    """.stripMargin)

  class Test(implicit acceptHeader: Option[String], conf: Config) {
    val httpConfiguration: HttpConfiguration = HttpConfiguration("context")
    private val errorHandler                 = mock[HttpErrorHandler]
    private val filters                      = mock[HttpFilters]
    (filters.filters _).stubs().returns(Seq.empty)

    MockAppConfig.featureSwitch.returns(Some(Configuration(conf)))

    val requestHandler: VersionRoutingRequestHandler =
      new VersionRoutingRequestHandler(routingMap, errorHandler, httpConfiguration, mockAppConfig, filters, action)

    def buildRequest(path: String): RequestHeader =
      acceptHeader
        .foldLeft(FakeRequest("GET", path)) { (req, accept) =>
          req.withHeaders((ACCEPT, accept))
        }
  }

  "Routing requests with no version" should {
    implicit val acceptHeader: None.type = None

    handleWithDefaultRoutes()
  }

  "Routing requests with any enabled version" should {
    implicit val acceptHeader: Option[String] = Some("application/vnd.hmrc.2.0+json")

    handleWithDefaultRoutes()
  }

  "Routing requests with v2" should {
    implicit val acceptHeader: Option[String] = Some("application/vnd.hmrc.2.0+json")
    handleWithVersionRoutes("/resource", V2Handler)
  }

  "Routing a request with v3" when {
//    implicit val acceptHeader: Option[String] = Some("application/vnd.hmrc.3.0+json")

    "the v3 endpoint exists" should {
      "use the v3 handler" in {
        implicit val acceptHeader: Option[String] = Some("application/vnd.hmrc.3.0+json")
        handleWithVersionRoutes("/resource", V3Handler)
      }
    }
  }

  private def handleWithDefaultRoutes()(implicit acceptHeader: Option[String]): Unit = {
    implicit val useConf: Config = confWithAllEnabled

    "if the request ends with a trailing slash" when {
      "handler found" should {
        "use it" in new Test {
          requestHandler.routeRequest(buildRequest("/")) shouldBe Some(DefaultHandler)
        }
      }

      "handler not found" should {
        "try without the trailing slash" in new Test {
          requestHandler.routeRequest(buildRequest("")) shouldBe Some(DefaultHandler)
        }
      }
    }
  }

  private def handleWithVersionRoutes(path: String, handler: Handler, conf: Config = confWithAllEnabled)(
      implicit acceptHeader: Option[String]): Unit = {

    implicit val useConf: Config = conf

    withClue("request ends with a trailing slash...") {
      new Test {
        requestHandler.routeRequest(buildRequest(s"$path/")) shouldBe Some(handler)
      }
    }
    withClue("request doesn't end with a trailing slash...") {
      new Test {
        requestHandler.routeRequest(buildRequest(s"$path")) shouldBe Some(handler)
      }
    }

//    "if the request ends with a trailing slash" when {
//      "handler found" should {
//        "use it" in new Test {
//          requestHandler.routeRequest(buildRequest(s"$path/")) shouldBe Some(handler)
//        }
//      }
//
//      "handler not found" should {
//        "try without the trailing slash" in new Test {
//          requestHandler.routeRequest(buildRequest(s"$path")) shouldBe Some(handler)
//        }
//      }
//    }
  }

  "Routing requests to non-default router with no version" should {
    implicit val acceptHeader: None.type = None
    implicit val useConf: Config         = confWithAllEnabled

    "return 406" in new Test {
      val request: RequestHeader = buildRequest("/resource")
      inside(requestHandler.routeRequest(request)) {
        case Some(a: EssentialAction) =>
          val result = a.apply(request)

          status(result) shouldBe NOT_ACCEPTABLE
          contentAsJson(result) shouldBe Json.toJson(InvalidAcceptHeaderError)
      }
    }
  }

  "Routing requests with an undefined version" should {
    implicit val acceptHeader: Option[String] = Some("application/vnd.hmrc.5.0+json")
    implicit val useConf: Config              = confWithAllEnabled

    "return 404" in new Test {
      val request: RequestHeader = buildRequest("/resource")

      inside(requestHandler.routeRequest(request)) {
        case Some(a: EssentialAction) =>
          val result = a.apply(request)

          status(result) shouldBe NOT_FOUND
          contentAsJson(result) shouldBe Json.toJson(UnsupportedVersionError)
      }
    }
  }

  "Routing requests for a defined but disabled version" when {
    implicit val acceptHeader: Option[String] = Some("application/vnd.hmrc.3.0+json")
    implicit val useConf: Config              = confWithV3Disabled

    "the version has a route for the resource" must {
      "return 404 Not Found" in new Test {
        val request: RequestHeader = buildRequest("/resource")

        inside(requestHandler.routeRequest(request)) {
          case Some(a: EssentialAction) =>
            val result = a.apply(request)

            status(result) shouldBe NOT_FOUND
            contentAsJson(result) shouldBe Json.toJson(UnsupportedVersionError)
        }
      }
    }
  }
}
