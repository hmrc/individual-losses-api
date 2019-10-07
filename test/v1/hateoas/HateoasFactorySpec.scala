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
import support.UnitSpec
import v1.models.hateoas.{ HateoasData, HateoasWrapper, Link }

class HateoasFactorySpec extends UnitSpec {

  private val nino   = "AA111111A"
  private val lossId = "123456789"

  val hateoasFactory = new HateoasFactory

  case class Response(foo: String)

  case class Data1(id: String) extends HateoasData
  case class Data2(id: String) extends HateoasData

  val response = Response("X")

  "wrap" should {

    implicit object LinksFactory1 extends HateoasLinksFactory[Response, Data1] {
      override def links(data: Data1) = Seq(Link(s"path/${data.id}", "GET", "rel1"))
    }

    implicit object LinksFactory2 extends HateoasLinksFactory[Response, Data2] {
      override def links(data: Data2) = Seq(Link(s"path/${data.id}", "GET", "rel2"))
    }

    "use the response specific links" in {
      hateoasFactory.wrap(response, Data1("id")) shouldBe HateoasWrapper(response, Seq(Link("path/id", "GET", "rel1")))
    }

    "use the endpoint HateoasData specific links" in {
      hateoasFactory.wrap(response, Data2("id")) shouldBe HateoasWrapper(response, Seq(Link("path/id", "GET", "rel2")))
    }
  }

  "wrapList" should {
    "work" in {
      case class ListResponse[A](items: Seq[A])

      implicit object ListResponseFunctor extends Functor[ListResponse] {
        override def map[A, B](fa: ListResponse[A])(f: A => B) = ListResponse(fa.items.map(f))
      }

      implicit object LinksFactory extends HateoasListLinksFactory[ListResponse, Response, Data1] {
        override def itemLinks(data: Data1, item: Response) = Seq(Link(s"path/${data.id}/${item.foo}", "GET", "item"))

        override def links(data: Data1) = Seq(Link(s"path/${data.id}", "GET", "rel"))
      }

      hateoasFactory.wrapList(ListResponse(Seq(response)), Data1("id")) shouldBe
        HateoasWrapper(ListResponse(Seq(HateoasWrapper(response, Seq(Link("path/id/X", "GET", "item"))))),
          Seq(Link("path/id", "GET", "rel"))
        )
    }
  }
}
