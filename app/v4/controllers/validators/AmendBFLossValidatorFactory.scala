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

package v4.controllers.validators

import api.controllers.validators.Validator
import api.controllers.validators.resolvers.{ResolveNino, ResolveNonEmptyJsonObject, ResolveParsedNumber, ResolveStringPattern}
import api.models.errors.{LossIdFormatError, MtdError, RuleIncorrectOrEmptyBodyError, ValueFormatError}
import cats.data.Validated
import cats.data.Validated.Invalid
import cats.implicits._
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import v4.models.domain.bfLoss.LossId
import v4.models.request.amendBFLosses.{AmendBFLossRequestBody, AmendBFLossRequestData}

import javax.inject.Singleton

@Singleton
class AmendBFLossValidatorFactory {

  private val resolveLossId = new ResolveStringPattern("^[A-Za-z0-9]{15}$".r, LossIdFormatError)
  private val resolveJson   = new ResolveNonEmptyJsonObject[AmendBFLossRequestBody]()

  def validator(nino: String, lossId: String, body: JsValue): Validator[AmendBFLossRequestData] =
    new Validator[AmendBFLossRequestData] {

      def validate: Validated[Seq[MtdError], AmendBFLossRequestData] =
        validateJsonFields
          .andThen(_ =>
            (
              ResolveNino(nino),
              resolveLossId(lossId).map(LossId),
              resolveJson(body)
            )
              .mapN(AmendBFLossRequestData))

      private def validateJsonFields: Validated[Seq[MtdError], Unit] = {
        val jsPath = (body \ "lossAmount")

        if (jsPath.isDefined) {
          jsPath.validate[BigDecimal] match {
            case JsSuccess(value, _) => ResolveParsedNumber()(value, path = "/lossAmount").map(_ => ())
            case JsError(_)          => Invalid(List(ValueFormatError.withPath("/lossAmount")))
          }
        } else {
          Invalid(List(RuleIncorrectOrEmptyBodyError))
        }
      }

    }

}
