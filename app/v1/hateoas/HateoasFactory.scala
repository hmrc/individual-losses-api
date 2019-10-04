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

package v1.hateoas

import cats.Functor
import javax.inject.Inject
import v1.models.hateoas._

import scala.language.higherKinds

class HateoasFactory @Inject()() {

  def wrap[A, D <: HateoasData](payload: A, data: D)(implicit lf: HateoasLinksFactory[A, D]): HateoasWrapper[A] = {
    val links = lf.links(data)

    HateoasWrapper(payload, links)
  }

  def wrapList[A[_]: Functor, I, D](payload: A[I], data: D)(implicit lf: HateoasListLinksFactory[A, I, D]): HateoasWrapper[A[HateoasWrapper[I]]] = {
    val hateoasList = Functor[A].map(payload)(i => HateoasWrapper(i, lf.itemLinks(data, i)))

    HateoasWrapper(hateoasList, lf.links(data))
  }
}

trait HateoasLinksFactory[A, D] {
  def links(data: D): Seq[Link]
}

trait HateoasListLinksFactory[A[_], I, D] {
  def itemLinks(data: D, item: I): Seq[Link]
  def links(data: D): Seq[Link]
}
