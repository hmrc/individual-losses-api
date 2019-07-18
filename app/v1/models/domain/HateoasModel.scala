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

package v1.models.domain

import play.api.libs.json.{JsValue, Json, Writes}

trait HateoasModel[T] {
  val booleanList: T => Seq[(Boolean, LinkGenerator)] // Seq[(does this link exist, what this link is)]
  val hateoasWrites: T => JsValue

  val getLinks: T => Option[JsValue] = o => {
    if (booleanList(o).exists(_._1)) {
      Some(
        Links(
          booleanList(o)
            .map {
              case (bool, lg) =>
                if (bool) Some(lg) else None
            }
            .filter(_.isDefined)))
    } else {
      None
    }
  }
}


case class LinkGenerator(rel: String, href: String, method: String, title: Option[String] = None)

object Links {
  def apply(o: Seq[Option[LinkGenerator]]): JsValue = Json.toJson(o)

  implicit val writes: Writes[LinkGenerator] = Json.writes[LinkGenerator]
}
