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
package v1.hateaos

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/*

Some options.

Basically all require injecting a factory in to a controller or whatever uses the service.

Options differ based on the assumptions that can be made about how the links are determined:
Option A - the mtd response payload has enough info to determine the links
Option B - the raw des payload is required to determine the links
Option C - external services are requires to determine the links

To me it seems that as they are all just injected (i.e. they all conform to the
same basic pattern, we can use either of them as required without unnecessarily breaking too much
of the downstream machinery.

 */

// For the case when the DES response is the payload
object HateaosOptionA {

  type Error
  type Request
  type Payload
  type Service = Request => Future[Either[Error, Payload]]

  trait HateoasFactory {
    def links(payload: Payload): Seq[Link]
  }

  class OrchestratorOrController @Inject()(hateoasFactory: HateoasFactory, service: Service) {

    def handleRequest(request: Request): Future[Either[Error, Wrapper[Payload]]] = {
      // ...Parsing and validation...

      val rawResult = service(request)

      wrap(rawResult)

      // ...convert errors....
    }

    private def wrap(rawResult: Future[Either[Error, Payload]]): Future[Either[Error, Wrapper[Payload]]] =
      EitherT(rawResult).map(payload => Wrapper(payload, hateoasFactory.links(payload))).value
  }

}

//#############################################################################

/*
For the case when the payload is a cut-down version of the des response but the full
(or additional fields from the) des response must
be used to determine the links (e.g. in case where getting list of ids where the things cannot be deleted
if they are in a certain state)

***THIS MEANS*** we cannot always convert the DES response to MTD response immediately (e.g. through Json reads)
but may need to pass it (at least with the descriminating fields) all the way out of the Des service layer.
(We could conceivably base the payload case classes on DES and then do writes jiggery-pokery to only emit
the relevant converted fields to the client but this would be very confusing..!
 */
object HateaosOptionB {

  type Error
  type Request
  type MtdPayload
  type DesPayload

  type Service = Request => Future[Either[Error, DesPayload]]
  val desToMtd: DesPayload => MtdPayload = ???

  trait HateoasFactory {
    def links(payload: DesPayload): Seq[Link]
  }

  class OrchestratorOrController @Inject()(hateoasFactory: HateoasFactory, service: Service) {

    def handleRequest(request: Request): Future[Either[Error, Wrapper[MtdPayload]]] = {
      // ...Parsing and validation...

      val rawDesResult = service(request)

      wrap(rawDesResult)

      // ...convert errors....
    }

    private def wrap(rawResult: Future[Either[Error, DesPayload]]): Future[Either[Error, Wrapper[MtdPayload]]] =
      EitherT(rawResult).map(desPayload => Wrapper(desToMtd(desPayload), hateoasFactory.links(desPayload))).value
  }

}

//#############################################################################

/*
  The more general case where we need to appeal to
  other micro-services or back to DES to determine the links. Since these can fail and are async
  we get result type that handles this...
 */
object HateaosOptionC {

  type Error
  type Request
  type MtdPayload
  type DesPayload

  type Service = Request => Future[Either[Error, DesPayload]]
  val desToMtd: DesPayload => MtdPayload = ???

  trait HateoasFactory {
    def links(payload: DesPayload): Future[Either[Error, Seq[Link]]]
  }

  class OrchestratorOrController @Inject()(hateoasFactory: HateoasFactory, service: Service) {

    def handleRequest(request: Request): Future[Either[Error, Wrapper[MtdPayload]]] = {
      // ...Parsing and validation...

      val rawDesResult = service(request)

      wrap(rawDesResult)

      // ...convert errors....
    }

    private def wrap(rawResult: Future[Either[Error, DesPayload]]): Future[Either[Error, Wrapper[MtdPayload]]] = {
      val wrapped = for {
        desPayload <- EitherT(rawResult)
        links      <- EitherT(hateoasFactory.links(desPayload))
      } yield Wrapper(desToMtd(desPayload), links)

      wrapped.value
    }

    // Would need something like this for list of wrappers (e.g. for list query requests)
    private def wrapSeq(rawResult: Future[Either[Error, List[DesPayload]]]): Future[Either[Error, List[Wrapper[MtdPayload]]]] = ???
  }
}
