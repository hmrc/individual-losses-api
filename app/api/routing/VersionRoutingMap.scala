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

package api.routing

import com.google.inject.ImplementedBy
import play.api.routing.Router

import javax.inject.Inject

@ImplementedBy(classOf[VersionRoutingMapImpl])
trait VersionRoutingMap {
  val defaultRouter: Router
  val map: Map[Version, Router]

  final def versionRouter(version: Version): Option[Router] = map.get(version)
}

case class VersionRoutingMapImpl @Inject() (
    defaultRouter: Router,
    v6Router: v6.Routes,
    v7Router: v7.Routes
) extends VersionRoutingMap {

  override val map: Map[Version, Router] = Map(
    Version6 -> v6Router,
    Version7 -> v7Router
  )

}
