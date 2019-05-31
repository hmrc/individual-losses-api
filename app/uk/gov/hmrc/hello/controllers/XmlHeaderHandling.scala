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

import java.io.StringWriter

import play.api.http.Writeable
import play.api.http._
import play.api.mvc._

import scala.xml.{Node, XML}

trait XmlHeaderHandling {

  implicit def writeable(implicit codec: Codec): Writeable[Node] = {
    Writeable(node => {
      codec.encode(printXml(node).toString)
    })
  }

  implicit def contentType(implicit codec: Codec): ContentTypeOf[Node] = {
    ContentTypeOf(Some(ContentTypes.XML(codec)))
  }

  def printXml(node: Node): String = {
    val writer: _root_.java.io.Writer = new StringWriter()
    XML.write(writer, node, enc = XML.encoding, xmlDecl = true, doctype = null)
    writer.toString
  }
}

object XmlHeaderHandling extends XmlHeaderHandling