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

package api.controllers

import api.models.errors.NotFoundError
import play.api.mvc._
import utils.Logging

import javax.inject._

@Singleton
class DeprecatedController @Inject() (cc: ControllerComponents) extends AbstractController(cc) with Logging {

  /** For endpoints that were deprecated in the previous version and gone from the current version.
    */
  def version4NotFound(unused: Any*): Action[AnyContent] = Action { implicit request =>
    logger.info(s"Request for deprecated endpoint received: V4 ${request.method} ${request.path}")
    NotFound(NotFoundError.asJson)
  }

}
