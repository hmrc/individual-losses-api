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

package v1.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import support.WireMockMethods

object DesStub extends WireMockMethods {

  private val responseBody = Json.parse(
    """
      | {
      | "lossId": "AAZZ1234567890a"
      | }
    """.stripMargin)

  private def url(nino: String): String =
    s"/income-tax/brought-forward-losses/$nino"

  def serviceSuccess(nino: String): StubMapping = {
    when(method = POST, uri = url(nino))
      .thenReturn(status = OK, responseBody)
  }

  def serviceError(nino: String, errorStatus: Int, errorBody: String): StubMapping = {
    when(method = POST, uri = url(nino))
      .thenReturn(status = errorStatus, errorBody)
  }
}
