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

package api.controllers.validators.resolvers

import api.models.errors.{InternalError, MtdError}
import cats.data.Validated
import cats.data.Validated.Valid
import cats.implicits._

/** Parses a raw value (e.g. String or JsValue) to a target type, validating in the process.
  *
  * @tparam S
  *   The source data type
  * @tparam T
  *   The target data type
  */
trait Resolver[S, T] {

  def apply(value: S, error: Option[MtdError], path: Option[String]): Validated[Seq[MtdError], T]

  final def apply(value: S): Validated[Seq[MtdError], T] = apply(value, None, None)

  final def apply(value: S, path: Option[String]): Validated[Seq[MtdError], T] = apply(value, None, path)

  final def apply(value: S, path: String): Validated[Seq[MtdError], T] = apply(value, None, Some(path))

  final def apply(value: S, error: MtdError): Validated[Seq[MtdError], T] =
    apply(value, Option(error), path = None)

  final def apply(maybeValue: Option[S], error: MtdError): Validated[Seq[MtdError], Option[T]] =
    apply(maybeValue, Option(error))

  final def apply(maybeValue: Option[S], defaultValue: => T): Validated[Seq[MtdError], T] =
    apply(maybeValue, defaultValue, error = None)

  final def apply(maybeValue: Option[S], defaultValue: => T, error: MtdError): Validated[Seq[MtdError], T] =
    apply(maybeValue, defaultValue, Option(error))

  final def apply(maybeValue: Option[S], defaultValue: => T, error: Option[MtdError]): Validated[Seq[MtdError], T] =
    apply(maybeValue, error)
      .map(maybeResolvedValue => maybeResolvedValue.getOrElse(defaultValue))

  final def apply(maybeValue: Option[S], error: Option[MtdError] = None, path: Option[String] = None): Validated[Seq[MtdError], Option[T]] =
    maybeValue match {
      case Some(value) => apply(value, error, path).map(Option(_))
      case None        => Valid(None)
    }

  final protected def requirePath(path: Option[String]): String = {
    path.getOrElse(throw new IllegalArgumentException(s"${getClass.getSimpleName} requires the path"))
  }

  final protected def requireError(maybeError: Option[MtdError], path: Option[String]): MtdError =
    withError(maybeError, orDefault = InternalError, path)

  final protected def withError(maybeError: Option[MtdError], orDefault: MtdError, extraPath: Option[String]): MtdError =
    maybeError match {
      case Some(error) => error.maybeWithExtraPath(extraPath)
      case None        => orDefault.maybeWithExtraPath(extraPath)
    }

  final protected def withErrors(maybeError: Option[MtdError], additional: Seq[MtdError], extraPath: Option[String]): Seq[MtdError] =
    maybeError match {
      case Some(error) => List(error.maybeWithExtraPath(extraPath))
      case None        => additional
    }

  final protected def combine(results: Validated[Seq[MtdError], _]*): Validated[Seq[MtdError], Unit] =
    results.traverse_(identity)

}
