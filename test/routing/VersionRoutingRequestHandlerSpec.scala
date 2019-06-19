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

package routing

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory
import org.scalamock.handlers.CallHandler1
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Inside, Matchers }
import play.api.Configuration
import play.api.http.HeaderNames.ACCEPT
import play.api.http.{ HttpConfiguration, HttpErrorHandler, HttpFilters }
import play.api.libs.json.Json
import play.api.mvc._
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.UnitSpec
import v1.models.errors.{ InvalidAcceptHeaderError, UnsupportedVersionError }

class VersionRoutingRequestHandlerSpec extends UnitSpec with Matchers with MockFactory with Inside {
  test =>

  implicit private val actorSystem: ActorSystem = ActorSystem("test")
  implicit private val mat: Materializer        = ActorMaterializer()

  private def configWithVersionsEnabled(enabledVersions: String*) =
    Configuration(
      ConfigFactory.parseString(
        enabledVersions.map(v => s"version-$v.enabled = true").mkString("\n")
      ))

  val defaultRouter = mock[Router]
  val v1Router      = mock[Router]
  val v2Router      = mock[Router]

  val routingMap = new VersionRoutingMap {
    override val defaultRouter = test.defaultRouter
    override val map           = Map("1.0" -> v1Router, "2.0" -> v2Router)
  }

  class Test(implicit acceptHeader: Option[String], config: Configuration = configWithVersionsEnabled("1", "2")) {
    val httpConfiguration = HttpConfiguration("context")
    val errorHandler      = mock[HttpErrorHandler]
    val filters           = mock[HttpFilters]
    (filters.filters _).stubs().returns(Seq.empty)

    val requestHandler = new VersionRoutingRequestHandler(routingMap, errorHandler, httpConfiguration, Some(config), filters, Action)

    def stubHandling(router: Router, path: String)(handler: Option[Handler]): CallHandler1[RequestHeader, Option[Handler]] =
      (router.handlerFor _)
        .expects(where { r: RequestHeader =>
          r.path == path
        })
        .returns(handler)

    def buildRequest(path: String): RequestHeader =
      acceptHeader
        .foldLeft(FakeRequest("GET", path)) { (req, accept) =>
          req.withHeaders((ACCEPT, accept))
        }
  }

  "Routing requests with no version" should {
    implicit val acceptHeader: None.type = None

    handleWith(defaultRouter)

    "return 406 not acceptable if bad header" in new Test {
      fail("what do we mean bad header? - only after default router cannot handle?")

      val request = buildRequest("path")
      inside(requestHandler.routeRequest(request)) {
        case Some(a: EssentialAction) =>
          val result = a.apply(request)

          status(result) shouldBe NOT_ACCEPTABLE
          contentAsJson(result) shouldBe Json.toJson(InvalidAcceptHeaderError)
      }
    }
  }

  "Routing requests with v1" should {
    implicit val acceptHeader: Some[String] = Some("application/vnd.hmrc.1.0+json")

    handleWith(v1Router)
    fallBackToDefaultRouterIfNoVersionedRouteFound(v1Router)
  }

  "Routing requests with v2" should {
    implicit val acceptHeader: Some[String] = Some("application/vnd.hmrc.2.0+json")

    handleWith(v2Router)
    fallBackToDefaultRouterIfNoVersionedRouteFound(v2Router)
  }

  "Routing requests with unsupported version (not yet coded up)" should {
    implicit val acceptHeader: Some[String] = Some("application/vnd.hmrc.5.0+json")

    "return 404" in new Test {
      val request = buildRequest("path")
      inside(requestHandler.routeRequest(request)) {
        case Some(a: EssentialAction) =>
          val result = a.apply(request)

          status(result) shouldBe NOT_FOUND
          contentAsJson(result) shouldBe Json.toJson(UnsupportedVersionError)
      }
    }
  }

  "Routing results for version unsupported (disabled by config)" when {
    implicit val acceptHeader: Some[String] = Some("application/vnd.hmrc.2.0+json")
    implicit val config: Configuration      = configWithVersionsEnabled("1")

    "the version has a route for the resource" must {
      "return 404 Not Found" in new Test {
        val handler: Handler = mock[Handler]
        stubHandling(v2Router, "path")(Some(handler))

        val request = buildRequest("path")
        inside(requestHandler.routeRequest(request)) {
          case Some(a: EssentialAction) =>
            val result = a.apply(request)

            status(result) shouldBe NOT_FOUND
            contentAsJson(result) shouldBe Json.toJson(UnsupportedVersionError)
        }
      }
    }

    "the version does not have a route for the resource" must {
      "use the default router" in new Test {
        val handler: Handler = mock[Handler]
        inSequence {
          stubHandling(v2Router, "path")(None)
          stubHandling(defaultRouter, "path")(Some(handler))
        }

        requestHandler.routeRequest(buildRequest("path")) shouldBe Some(handler)
      }
    }
  }

  private def handleWith(router: Router)(implicit acceptHeader: Option[String]): Unit = {
    "if the request ends with a trailing slash" when {
      "handler found" should {
        "use it" in new Test {
          val handler: Handler = mock[Handler]
          stubHandling(router, "path/")(Some(handler))

          requestHandler.routeRequest(buildRequest("path/")) shouldBe Some(handler)
        }
      }

      "handler not found" should {
        "try without the trailing slash" in new Test {
          val handler: Handler = mock[Handler]

          inSequence {
            stubHandling(router, "path/")(None)
            stubHandling(router, "path")(Some(handler))
          }

          requestHandler.routeRequest(buildRequest("path/")) shouldBe Some(handler)
        }
      }
    }

    "if the request does not end with a trailing slash" when {
      "handler found" should {
        "use it" in new Test {
          val handler: Handler = mock[Handler]
          stubHandling(router, "path")(Some(handler))

          requestHandler.routeRequest(buildRequest("path")) shouldBe Some(handler)
        }
      }
    }
  }

  private def fallBackToDefaultRouterIfNoVersionedRouteFound(versionedRouter: Router)(implicit acceptHeader: Option[String]): Unit = {
    "fall back to the default router if no versioned route found" in new Test {
      val handler: Handler = mock[Handler]
      inSequence {
        stubHandling(versionedRouter, "path")(None)
        stubHandling(defaultRouter, "path")(Some(handler))
      }

      requestHandler.routeRequest(buildRequest("path")) shouldBe Some(handler)
    }
  }

}
