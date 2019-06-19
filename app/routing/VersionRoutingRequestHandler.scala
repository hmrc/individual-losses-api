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

import config.FeatureSwitch
import definition.Versions
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.http.{DefaultHttpRequestHandler, HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.libs.json.Json
import play.api.mvc.{DefaultActionBuilder, Handler, RequestHeader, Results}
import play.api.routing.Router
import v1.models.errors.UnsupportedVersionError

@Singleton
class VersionRoutingRequestHandler @Inject()(versionRoutingMap: VersionRoutingMap,
                                             errorHandler: HttpErrorHandler,
                                             httpConfiguration: HttpConfiguration,
                                             config: Option[Configuration],
                                             filters: HttpFilters,
                                             action: DefaultActionBuilder)
    extends DefaultHttpRequestHandler(versionRoutingMap.defaultRouter, errorHandler, httpConfiguration, filters) {

  private val featureSwitch = FeatureSwitch(config)

  private val unsupportedVersionAction = action(Results.NotFound(Json.toJson(UnsupportedVersionError)))

  override def routeRequest(request: RequestHeader): Option[Handler] = {

    Versions.getFromRequest(request) match {
      case Some(version) =>
        versionRoutingMap.versionRouter(version) match {
          case Some(versionRouter) =>
            routeWith(versionRouter)(request) match {
              case Some(handler) if featureSwitch.isVersionEnabled(version) => Some(handler)
              case Some(_)                                                  => Some(unsupportedVersionAction)
              case None                                                     => routeWith(versionRoutingMap.defaultRouter)(request)
            }

          case None => Some(unsupportedVersionAction)
        }

      case None => routeWith(versionRoutingMap.defaultRouter)(request)
    }
  }

  private def routeWith(router: Router)(request: RequestHeader) =
    router
      .handlerFor(request)
      .orElse {
        if (request.path.endsWith("/")) {
          val pathWithoutSlash        = request.path.dropRight(1)
          val requestWithModifiedPath = request.withTarget(request.target.withPath(pathWithoutSlash))
          router.handlerFor(requestWithModifiedPath)
        } else {
          None
        }
      }

}
