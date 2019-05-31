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

package uk.gov.hmrc.hello.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

@Singleton
class HeaderValidator @Inject() (cc: ControllerComponents) extends Results with HmrcMimeTypes with ErrorConversion {

  val validateVersion: String => Boolean = v => v == "1.0" || v == "2.0"

  val validateContentType: String => Boolean = ct => ct == "json" || ct == "xml"

  val matchHeader: String => Option[Match] = new Regex( """^application/vnd[.]{1}hmrc[.]{1}(.*?)[+]{1}(.*)$""", "version", "contenttype") findFirstMatchIn _

  val acceptHeaderValidationRules: Option[String] => Boolean =
    _ flatMap (a => matchHeader(a) map (res => validateContentType(res.group("contenttype")) && validateVersion(res.group("version")))) getOrElse false

  def validateAction(rules: Option[String] => Boolean) = {
    new ActionBuilder[Request, AnyContent] with ActionFilter[Request] {

      override val parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser
      override protected val executionContext: ExecutionContext = cc.executionContext

      def filter[T](input: Request[T]) = Future.successful {

        implicit val r = input

        if (!rules(input.headers.get("Accept"))) {
          Some(ErrorAcceptHeaderInvalid)
        } else {
          None
        }
      }

    }
  }

  val validateAcceptHeader: ActionBuilder[Request, AnyContent] = validateAction(acceptHeaderValidationRules)

}
