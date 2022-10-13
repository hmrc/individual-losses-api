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

package api.models.domain

import config.FeatureSwitches
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
  * Represents a tax year for downstream systems
  *
  * @param value the tax year string (where 2018 represents 2017-18)
  */
final case class TaxYear private(value: String) {

  override def toString: String = value

  val year: Int     = value.toInt
  val asMtd: String = (value.toInt - 1) + "-" + value.drop(2)

  /** The tax year in the pre-TYS downstream format, e.g. "2023-24".
    */
  val asDownstream: String = value

  /** The tax year in the Tax Year Specific (TYS) downstream format, e.g. "23-24".
    */
  val asTysDownstream: String = {
    val year2 = value.toInt - 2000
    val year1 = year2 - 1
    s"$year1-$year2"
  }

  /** Use this for downstream API endpoints that are known to be TYS.
    */
  def useTaxYearSpecificApi(implicit featureSwitches: FeatureSwitches): Boolean =
    featureSwitches.isTaxYearSpecificApiEnabled && year >= 2024

}

object TaxYear {

  /**
    * @param taxYear tax year in MTD format (e.g. 2017-18)
    */
  def fromMtd(taxYear: String): TaxYear =
    TaxYear(taxYear.take(2) + taxYear.drop(5))

  def fromDownstream(taxYear: String): TaxYear =
    TaxYear(taxYear)

  def fromDownstreamInt(taxYear: Int): TaxYear =
    TaxYear(taxYear.toString)

  def mostRecentTaxYear(date: LocalDate = LocalDate.now()): TaxYear = {
    val limit = LocalDate.parse(s"${date.getYear}-04-05", DateTimeFormatter.ISO_DATE)
    if (date.isBefore(limit)) {
      TaxYear(s"${date.getYear - 1}")
    } else {
      TaxYear(s"${date.getYear}")
    }
  }
}
