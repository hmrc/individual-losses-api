package api.controllers.validators.resolvers

import api.models.domain.TaxYear
import api.models.errors.{MtdError, RuleTaxYearNotEndedError, RuleTaxYearNotSupportedError}
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import support.UnitSpec

class DetailedResolveTaxYearSpec extends UnitSpec {

  "validateMinimumTaxYear" when {
    "a minimum tax year isn't specified" should {
      val resolveTaxYear = DetailedResolveTaxYear()

      "accept a tax year < a reasonable minimum" in {
        val result: Validated[Seq[MtdError], TaxYear] = resolveTaxYear("2010-11")
        result shouldBe Valid(TaxYear.fromMtd("2010-11"))
      }

    }

    "a minimum tax year is specified" should {
      val resolveTaxYear = DetailedResolveTaxYear(maybeMinimumTaxYear = Some(2019))

      "accept a tax year >= the minimum" in {
        val result: Validated[Seq[MtdError], TaxYear] = resolveTaxYear("2018-19")
        result shouldBe Valid(TaxYear.fromMtd("2018-19"))
      }

      "reject a tax year < the minimum" in {
        val result: Validated[Seq[MtdError], TaxYear] = resolveTaxYear("2017-18")
        result shouldBe Invalid(List(RuleTaxYearNotSupportedError))
      }
    }
  }

  "validateIncompleteTaxYear" should {

    "accept an incomplete tax year if allowed" in {
      val resolveTaxYear = DetailedResolveTaxYear()
      val result         = resolveTaxYear("2090-91")
      result shouldBe Valid(TaxYear.fromMtd("2090-91"))
    }

    "reject an incomplete tax year if not allowed" in {
      val resolveTaxYear = DetailedResolveTaxYear(allowIncompleteTaxYear = false)
      val result         = resolveTaxYear("2090-91")
      result shouldBe Invalid(List(RuleTaxYearNotEndedError))

    }
  }

}
