/*
 * Copyright 2021 HM Revenue & Customs
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

package v3.connectors

import config.AppConfig
import play.api.Logger
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, HttpReads }
import v3.connectors.DownstreamUri.{ DesUri, IfsUri }

import scala.concurrent.{ ExecutionContext, Future }

trait BaseDownstreamConnector {
  val http: HttpClient
  val appConfig: AppConfig

  val logger: Logger = Logger(this.getClass)

  private def desHeaderCarrier(additionalHeaders: Seq[String])(implicit hc: HeaderCarrier): HeaderCarrier =
    HeaderCarrier(
      extraHeaders = hc.extraHeaders ++
        // Contract headers
        Seq(
          "Authorization" -> s"Bearer ${appConfig.desToken}",
          "Environment"   -> appConfig.desEnv
        ) ++
        // Other headers (i.e Gov-Test-Scenario, Content-Type)
        hc.headers(additionalHeaders ++ appConfig.desEnvironmentHeaders.getOrElse(Seq.empty))
    )

  private def ifsHeaderCarrier(additionalHeaders: Seq[String])(implicit hc: HeaderCarrier): HeaderCarrier =
    HeaderCarrier(
      extraHeaders = hc.extraHeaders ++
        // Contract headers
        Seq(
          "Authorization" -> s"Bearer ${appConfig.ifsToken}",
          "Environment"   -> appConfig.ifsEnv
        ) ++
        // Other headers (i.e Gov-Test-Scenario, Content-Type)
        hc.headers(additionalHeaders ++ appConfig.ifsEnvironmentHeaders.getOrElse(Seq.empty))
    )

  def post[Body: Writes, Resp](body: Body, uri: DownstreamUri[Resp])(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]]): Future[DownstreamOutcome[Resp]] = {

    def doPost(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] = {
      http.POST(getBackendUri(uri), body)
    }

    doPost(getBackendHeaders(uri, hc))
  }

  def get[Resp](uri: DownstreamUri[Resp], queryParams: Seq[(String, String)] = Seq.empty)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]]): Future[DownstreamOutcome[Resp]] = {

    def doGet(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] =
      http.GET(getBackendUri(uri), queryParams = queryParams)

    doGet(getBackendHeaders(uri, hc))
  }

  def delete[Resp](uri: DownstreamUri[Resp])(implicit ec: ExecutionContext,
                                             hc: HeaderCarrier,
                                             httpReads: HttpReads[DownstreamOutcome[Resp]]): Future[DownstreamOutcome[Resp]] = {

    def doDelete(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] = {
      http.DELETE(getBackendUri(uri))
    }

    doDelete(getBackendHeaders(uri, hc))
  }

  def put[Body: Writes, Resp](body: Body, uri: DownstreamUri[Resp])(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]]): Future[DownstreamOutcome[Resp]] = {

    def doPut(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] = {
      http.PUT(getBackendUri(uri), body)
    }

    doPut(getBackendHeaders(uri, hc))
  }

  private def getBackendUri[Resp](uri: DownstreamUri[Resp]): String = uri match {
    case DesUri(value) => s"${appConfig.desBaseUrl}/$value"
    case IfsUri(value) => s"${appConfig.ifsBaseUrl}/$value"
  }

  private def getBackendHeaders[Resp](uri: DownstreamUri[Resp], hc: HeaderCarrier, additionalHeaders: Seq[String] = Seq.empty): HeaderCarrier =
    uri match {
      case DesUri(_) => desHeaderCarrier(additionalHeaders)(hc)
      case IfsUri(_) => ifsHeaderCarrier(additionalHeaders)(hc)
    }
}
