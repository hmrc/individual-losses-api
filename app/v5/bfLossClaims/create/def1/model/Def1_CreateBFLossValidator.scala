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

package v5.bfLossClaims.create.def1.model

import api.controllers.validators.Validator
import api.controllers.validators.resolvers._
import api.models.domain.TodaySupplier
import api.models.errors.{MtdError, TypeOfLossFormatError}
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.catsSyntaxTuple2Semigroupal
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import v5.bfLossClaims.create.model.TypeOfLoss
import v5.bfLossClaims.create.def1.model.request.{Def1_CreateBFLossRequestBody, Def1_CreateBFLossRequestData}
import v5.bfLossClaims.create.model.request.CreateBFLossRequestData

import javax.inject.Inject

class Def1_CreateBFLossValidator @Inject()
(nino: String, body: JsValue)(implicit todaySupplier: TodaySupplier = new TodaySupplier) extends Validator[CreateBFLossRequestData] {
  val minimumTaxYearBFLoss = 2019
  val minimumTaxYearLossClaim = 2020
  private val resolveJson = new ResolveJsonObject[Def1_CreateBFLossRequestBody]()
  private val resolveParsedNumber = ResolveParsedNumber()
  private val resolveTaxYear = DetailedResolveTaxYear(allowIncompleteTaxYear = false, maybeMinimumTaxYear = Some(minimumTaxYearBFLoss))
    def validate: Validated[Seq[MtdError], CreateBFLossRequestData] =
      ResolveBFTypeOfLossFromJson(body, None, errorPath = Some("/typeOfLoss"))
        .andThen(_ =>
          (
            ResolveNino(nino),
            resolveJson(body)
          ).mapN(Def1_CreateBFLossRequestData)
            .andThen(validateParsedData))

  private def validateParsedData(parsed: Def1_CreateBFLossRequestData): Validated[Seq[MtdError], CreateBFLossRequestData] =
    combine(
      resolveTaxYear(parsed.broughtForwardLoss.taxYearBroughtForwardFrom, None, Some("/taxYearBroughtForwardFrom")),
      ResolveBusinessId(parsed.broughtForwardLoss.businessId),
      resolveParsedNumber(parsed.broughtForwardLoss.lossAmount, path = "/lossAmount")
    ).map(_ => parsed)

  object ResolveBFTypeOfLossFromJson extends Resolver[JsValue, Option[TypeOfLoss]] {
    override def apply(body: JsValue, maybeError: Option[MtdError], errorPath: Option[String]): Validated[Seq[MtdError], Option[TypeOfLoss]] = {
      def useError = maybeError.getOrElse(TypeOfLossFormatError).maybeWithExtraPath(errorPath)

      val jsPath = body \ "typeOfLoss"

      if (jsPath.isEmpty) {
        Valid(None)
      }
      else {
        jsPath.validate[String] match {
          case JsError(_) => Invalid(List(useError))
          case JsSuccess(value, _) =>
            TypeOfLoss.parser
              .lift(value)
              .map(parsed => Valid(Some(parsed)))
              .getOrElse(Invalid(List(useError)))
        }
      }
    }
  }
}

