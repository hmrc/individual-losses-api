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

import api.models.domain.{TaxYear, TodaySupplier}
import api.models.errors._
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits._

trait TaxYearResolving extends Resolver[String, TaxYear] {

  private val taxYearFormat = "20[1-9][0-9]-[1-9][0-9]".r

  protected val rangeInvalidError: MtdError = RuleTaxYearRangeInvalidError

  protected def resolve(value: String, maybeFormatError: Option[MtdError], path: Option[String]): Validated[Seq[MtdError], TaxYear] =
    if (taxYearFormat.matches(value)) {
      val startTaxYearStart: Int = 2
      val startTaxYearEnd: Int   = 4

      val endTaxYearStart: Int = 5
      val endTaxYearEnd: Int   = 7

      val start = value.substring(startTaxYearStart, startTaxYearEnd).toInt
      val end   = value.substring(endTaxYearStart, endTaxYearEnd).toInt

      if (end - start == 1)
        Valid(TaxYear.fromMtd(value))
      else
        Invalid(List(withError(None, rangeInvalidError, path)))

    } else {
      Invalid(List(withError(maybeFormatError, TaxYearFormatError, path)))
    }

}

object ResolveTaxYear extends TaxYearResolving {

  def apply(value: String, maybeError: Option[MtdError], errorPath: Option[String]): Validated[Seq[MtdError], TaxYear] =
    resolve(value, maybeError, errorPath)

}

object ResolveTysTaxYear extends TaxYearResolving {

  def apply(value: String, maybeError: Option[MtdError], errorPath: Option[String]): Validated[Seq[MtdError], TaxYear] =
    resolve(value, maybeError, errorPath)
      .andThen { taxYear =>
        if (taxYear.year < TaxYear.tysTaxYear)
          Invalid(List(InvalidTaxYearParameterError) ++ maybeError)
        else
          Valid(taxYear)
      }

}

case class DetailedResolveTaxYear(
    allowIncompleteTaxYear: Boolean = true,
    incompleteTaxYearError: MtdError = RuleTaxYearNotEndedError,
    maybeMinimumTaxYear: Option[Int] = None,
    minimumTaxYearError: MtdError = RuleTaxYearNotSupportedError
)(implicit todaySupplier: TodaySupplier = new TodaySupplier)
    extends TaxYearResolving {

  def apply(value: String, maybeFormatError: Option[MtdError], errorPath: Option[String]): Validated[Seq[MtdError], TaxYear] = {

    def validateMinimumTaxYear(parsed: TaxYear): Validated[Seq[MtdError], Unit] =
      maybeMinimumTaxYear
        .traverse_ { minimumTaxYear =>
          if (parsed.year < minimumTaxYear)
            Invalid(List(minimumTaxYearError.maybeWithExtraPath(errorPath)))
          else
            Valid(())
        }

    def validateIncompleteTaxYear(parsed: TaxYear): Validated[Seq[MtdError], Unit] =
      if (allowIncompleteTaxYear)
        Valid(())
      else {
        val currentTaxYear = TaxYear.currentTaxYear()
        if (parsed.year >= currentTaxYear.year)
          Invalid(List(incompleteTaxYearError.maybeWithExtraPath(errorPath)))
        else
          Valid(())
      }

    resolve(value, maybeFormatError, errorPath)
      .andThen { parsed =>
        combine(
          validateMinimumTaxYear(parsed),
          validateIncompleteTaxYear(parsed)
        ).map(_ => parsed)

      }
  }

}
