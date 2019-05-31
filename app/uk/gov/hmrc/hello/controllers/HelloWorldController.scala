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
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.hello.services.{Hello, HelloWorldService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class HelloWorldController @Inject()(headerValidator: HeaderValidator, service: HelloWorldService, val cc: ControllerComponents)
  extends BackendController(cc) with HmrcMimeTypes with ErrorConversion with XmlHeaderHandling {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  // Due to the need to demonstrate validation, some of these Accept headers are never present by the time we reach here.
  // However it does demonstrate how to handle multiple versions and types.
  private val AcceptsHmrcXml1 = Accepting(VndHmrcXml_1_0)
  private val AcceptsHmrcXml2 = Accepting(VndHmrcXml_2_0)
  private val AcceptsHmrcJson1 = Accepting(VndHmrcJson_1_0)
  private val AcceptsHmrcJson2 = Accepting(VndHmrcJson_2_0)

  private def renderHello[T](h: Hello)(implicit request: Request[T]) = {
    render {
      case AcceptsHmrcJson1() | AcceptsHmrcJson2() => Ok(Json.toJson(h))
      case AcceptsHmrcXml1() => Ok(<Hello><message>{h.message}</message></Hello>).as(MimeTypes.XML)
      case AcceptsHmrcXml2() => Ok(<Hello2><message>{h.message}</message></Hello2>).as(MimeTypes.XML)
      case _ => ErrorAcceptHeaderInvalid
    }
  }

  private def callAndRenderHello(f: Request[_] => Future[Hello]): Request[AnyContent] => Future[Result] = { implicit request =>
    f(request).map(renderHello(_))
  }

  final def world: Action[AnyContent] = headerValidator.validateAcceptHeader.async { callAndRenderHello(_ => service.fetchWorld) }

  final def application: Action[AnyContent] = headerValidator.validateAcceptHeader async { callAndRenderHello(_ => service.fetchApplication) }

  final def user: Action[AnyContent] = headerValidator.validateAcceptHeader async { callAndRenderHello(_ => service.fetchUser) }
}
