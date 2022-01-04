/*
 * Copyright 2022 HM Revenue & Customs
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

package v3.controllers.requestParsers.validators.validations

import play.api.Logger
import play.api.libs.json._
import utils.{EmptinessChecker, EmptyPathsResult}
import v3.models.errors.{MtdError, RuleIncorrectOrEmptyBodyError}

object JsonFormatValidation {

  private val logger: Logger = Logger(this.getClass)

  def validate[A: OFormat](data: JsValue): List[MtdError] =
    validateOrRead(data) match {
      case Left(errors) => errors
      case Right(_)     => Nil
    }

  def validateAndCheckNonEmpty[A: OFormat: EmptinessChecker](data: JsValue): List[MtdError] =
    validateOrRead[A](data) match {
      case Left(schemaErrors) => schemaErrors
      case Right(body) =>
        EmptinessChecker.findEmptyPaths(body) match {
          case EmptyPathsResult.CompletelyEmpty   => List(RuleIncorrectOrEmptyBodyError)
          case EmptyPathsResult.EmptyPaths(paths) => List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(paths)))
          case EmptyPathsResult.NoEmptyPaths      => Nil
        }
      case _ => Nil
    }

  def validateOrRead[A: OFormat](data: JsValue): Either[List[MtdError], A] = {
    if (data == JsObject.empty) {
      Left(List(RuleIncorrectOrEmptyBodyError))
    } else {
      data.validate[A] match {
        case JsSuccess(a, _)                                          => Right(a)
        case JsError(errors: Seq[(JsPath, Seq[JsonValidationError])]) => Left(handleErrors(errors))
      }
    }
  }

  private def handleErrors(errors: Seq[(JsPath, Seq[JsonValidationError])]): List[MtdError] = {
    val failures = errors.map {
      case (path: JsPath, Seq(JsonValidationError(Seq("error.path.missing"))))                              => MissingMandatoryField(path)
      case (path: JsPath, Seq(JsonValidationError(Seq(error: String)))) if error.contains("error.expected") => WrongFieldType(path)
      case (path: JsPath, _)                                                                                => OtherFailure(path)
    }

    val logString = failures
      .groupBy(_.getClass)
      .values
      .map(failure => s"${failure.head.failureReason}: " + s"${failure.map(_.fromJsPath)}")
      .toString()
      .dropRight(1)
      .drop(5)

    logger.warn(s"[JsonFormatValidation][validate] - Request body failed validation with errors - $logString")
    List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(failures.map(_.fromJsPath))))
  }

  private class JsonFormatValidationFailure(path: JsPath, failure: String) {
    val failureReason: String = failure

    def fromJsPath: String =
      path
        .toString()
        .replace("(", "/")
        .replace(")", "")
  }

  private case class MissingMandatoryField(path: JsPath) extends JsonFormatValidationFailure(path, "Missing mandatory field")
  private case class WrongFieldType(path: JsPath)        extends JsonFormatValidationFailure(path, "Wrong field type")
  private case class OtherFailure(path: JsPath)          extends JsonFormatValidationFailure(path, "Other failure")
}
