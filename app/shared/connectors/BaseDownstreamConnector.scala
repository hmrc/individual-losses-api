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

package shared.connectors

import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Writes
import shared.config.SharedAppConfig
import shared.utils.{Logging, UrlUtils}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}

import scala.concurrent.{ExecutionContext, Future}

trait BaseDownstreamConnector extends Logging {
  val http: HttpClient
  val appConfig: SharedAppConfig

  // This is to provide an implicit AppConfig in existing connector implementations (which
  // typically declare the abstract `appConfig` field non-implicitly) without having to change them.
  implicit protected lazy val _appConfig: SharedAppConfig = appConfig

  private val jsonContentTypeHeader = Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

  def post[Body: Writes, Resp](body: Body, uri: DownstreamUri[Resp], maybeIntent: Option[String] = None)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]],
      correlationId: String): Future[DownstreamOutcome[Resp]] = {

    val strategy = uri.strategy

    def doPost(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] = {
      http.POST(getBackendUri(uri.path, strategy), body)
    }

    for {
      headers <- getBackendHeaders(strategy, jsonContentTypeHeader ++ intentHeader(maybeIntent))
      result  <- doPost(headers)
    } yield result
  }

  def get[Resp](uri: DownstreamUri[Resp], queryParams: Seq[(String, String)] = Nil, maybeIntent: Option[String] = None)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]],
      correlationId: String): Future[DownstreamOutcome[Resp]] = {

    val strategy = uri.strategy

    def doGet(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] =
      http.GET(getBackendUri(uri.path, strategy), queryParams)

    for {
      headers <- getBackendHeaders(strategy, intentHeader(maybeIntent))
      result  <- doGet(headers)
    } yield result
  }

  def delete[Resp](uri: DownstreamUri[Resp], queryParams: Seq[(String, String)] = Nil, maybeIntent: Option[String] = None)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]],
      correlationId: String): Future[DownstreamOutcome[Resp]] = {

    val strategy = uri.strategy

    def doDelete(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] = {
      val fullUrl = UrlUtils.appendQueryParams(getBackendUri(uri.path, strategy), queryParams)
      http.DELETE(fullUrl)
    }

    for {
      headers <- getBackendHeaders(strategy, intentHeader(maybeIntent))
      result  <- doDelete(headers)
    } yield result
  }

  def put[Body: Writes, Resp](body: Body, uri: DownstreamUri[Resp], maybeIntent: Option[String] = None)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]],
      correlationId: String): Future[DownstreamOutcome[Resp]] = {

    val strategy = uri.strategy

    def doPut(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] = {
      http.PUT(getBackendUri(uri.path, strategy), body)
    }

    for {
      headers <- getBackendHeaders(strategy, jsonContentTypeHeader ++ intentHeader(maybeIntent))
      result  <- doPut(headers)
    } yield result
  }

  private def intentHeader(maybeIntent: Option[String]) =
    maybeIntent.map(intent => Seq("intent" -> intent)).getOrElse(Nil)

  private def getBackendUri(path: String, strategy: DownstreamStrategy): String =
    s"${strategy.baseUrl}/$path"

  private def getBackendHeaders(
      strategy: DownstreamStrategy,
      additionalHeaders: Seq[(String, String)]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier, correlationId: String): Future[HeaderCarrier] = {

    for {
      contractHeaders <- strategy.contractHeaders(correlationId)
    } yield {
      val apiHeaders = hc.extraHeaders ++ contractHeaders ++ additionalHeaders

      val passThroughHeaders = hc
        .headers(strategy.environmentHeaders)
        .filterNot(hdr => apiHeaders.exists(_._1.equalsIgnoreCase(hdr._1)))

      HeaderCarrier(extraHeaders = apiHeaders ++ passThroughHeaders)
    }

  }

}
