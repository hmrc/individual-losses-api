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

import org.scalamock.handlers.CallHandler1
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpec }
import play.api.http.HeaderNames.ACCEPT
import play.api.http.{ HttpConfiguration, HttpErrorHandler, HttpFilters }
import play.api.mvc.{ Handler, RequestHeader }
import play.api.routing.Router
import play.api.test.FakeRequest

class VersionRoutingRequestHandlerSpec extends WordSpec with Matchers with MockFactory {
  test =>

  val defaultRouter = mock[Router]
  val v1Router      = mock[Router]
  val v2Router      = mock[Router]

  val routingMap = new VersionRoutingMap {
    override val defaultRouter = test.defaultRouter
    override val map           = Map("1.0" -> v1Router, "2.0" -> v2Router)
  }

  class Test(implicit acceptHeader: Option[String]) {
    val configuration = HttpConfiguration("context")
    val errorHandler  = mock[HttpErrorHandler]
    val filters       = mock[HttpFilters]
    (filters.filters _).stubs().returns(Seq.empty)

    val requestHandler = new VersionRoutingRequestHandler(routingMap, errorHandler, configuration, filters)

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

  "Routing requests with unsupported version" should {
    implicit val acceptHeader: Some[String] = Some("application/vnd.hmrc.5.0+json")

    handleWith(defaultRouter)
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
