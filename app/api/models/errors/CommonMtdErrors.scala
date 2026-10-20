/*
 * Copyright 2026 HM Revenue & Customs
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

package api.models.errors

import play.api.http.Status.*

// MtdError types that are common across MTD APIs.

// Format Errors
object NinoFormatError extends MtdError("FORMAT_NINO", "The NINO format is invalid", BAD_REQUEST)

object TaxYearFormatError extends MtdError("FORMAT_TAX_YEAR", "The taxYear format is invalid", BAD_REQUEST)

object DateFormatError extends MtdError(code = "FORMAT_DATE", message = "The supplied date format is not valid", BAD_REQUEST)

object BusinessIdFormatError extends MtdError("FORMAT_BUSINESS_ID", "The Business ID format is invalid", BAD_REQUEST)

object ValueFormatError extends MtdError("FORMAT_VALUE", "The value must be between 0 and 99999999999.99", BAD_REQUEST) {

  def forPathAndRange(path: String, min: String, max: String): MtdError =
    ValueFormatError.copy(paths = Some(Seq(path)), message = s"The value must be between $min and $max")

}

object PreferenceOrderFormatError extends MtdError("FORMAT_PREFERENCE_ORDER", "The provided preference order is invalid", BAD_REQUEST)

object ClaimIdFormatError extends MtdError("FORMAT_CLAIM_ID", "The provided claim ID is invalid", BAD_REQUEST)

object LossIdFormatError extends MtdError("FORMAT_LOSS_ID", "The provided loss ID is invalid", BAD_REQUEST)

object TypeOfLossFormatError extends MtdError("FORMAT_TYPE_OF_LOSS", "The provided type of loss is invalid", BAD_REQUEST)

object TypeOfClaimFormatError extends MtdError("FORMAT_TYPE_OF_CLAIM", "The provided type of claim is invalid", BAD_REQUEST)

object TaxYearClaimedForFormatError extends MtdError("FORMAT_TAX_YEAR", "The provided tax year claimed for is invalid", BAD_REQUEST)

//Standard Errors
object NotFoundError extends MtdError("MATCHING_RESOURCE_NOT_FOUND", "Matching resource not found", NOT_FOUND)

object InternalError extends MtdError("INTERNAL_SERVER_ERROR", "An internal server error occurred", INTERNAL_SERVER_ERROR)

object BadRequestError extends MtdError("INVALID_REQUEST", "Invalid request", BAD_REQUEST)

object GatewayTimeoutError extends MtdError("GATEWAY_TIMEOUT", "The request has timed out", GATEWAY_TIMEOUT)

object BVRError extends MtdError("BUSINESS_ERROR", "Business validation error", BAD_REQUEST)

object ServiceUnavailableError extends MtdError("SERVICE_UNAVAILABLE", "Internal server error", INTERNAL_SERVER_ERROR)

object InvalidHttpMethodError extends MtdError("INVALID_HTTP_METHOD", "Invalid HTTP method", METHOD_NOT_ALLOWED)

object InvalidBodyTypeError extends MtdError("INVALID_BODY_TYPE", "Expecting text/json or application/json body", UNSUPPORTED_MEDIA_TYPE)

object InvalidTaxYearParameterError
    extends MtdError(code = "INVALID_TAX_YEAR_PARAMETER", message = "A tax year before 2023-24 was supplied", BAD_REQUEST)

//Authentication/Authorisation errors

/** Authentication OK but not allowed access to the requested resource
  */
object ClientOrAgentNotAuthorisedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client or agent is not authorised", FORBIDDEN) {
  def withStatus401: MtdError = copy(httpStatus = UNAUTHORIZED)
}

object InvalidBearerTokenError extends MtdError("UNAUTHORIZED", "Bearer token is missing or not authorized", UNAUTHORIZED)

// Accept header Errors
object InvalidAcceptHeaderError extends MtdError("ACCEPT_HEADER_INVALID", "The accept header is missing or invalid", NOT_ACCEPTABLE)

object UnsupportedVersionError extends MtdError("NOT_FOUND", "The requested resource could not be found", NOT_FOUND)

// Common rule errors
object RuleTaxYearNotSupportedError
    extends MtdError("RULE_TAX_YEAR_NOT_SUPPORTED", "The tax year specified does not lie within the supported range", BAD_REQUEST)

object RuleTaxYearForVersionNotSupportedError
    extends MtdError(
      "RULE_TAX_YEAR_FOR_VERSION_NOT_SUPPORTED",
      "This version of the API supports tax years up to, and including, 2025-26. For tax years 2026–27 onwards, a new version is in development",
      BAD_REQUEST)

object RuleIncorrectOrEmptyBodyError
    extends MtdError("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", "An empty or non-matching body was submitted", BAD_REQUEST)

object RuleTaxYearRangeInvalidError extends MtdError("RULE_TAX_YEAR_RANGE_INVALID", "A tax year range of one year is required", BAD_REQUEST)

object RuleTaxYearNotEndedError extends MtdError("RULE_TAX_YEAR_NOT_ENDED", "The specified tax year has not yet ended", BAD_REQUEST)

object RuleMissingPreferenceOrderError
    extends MtdError(
      "RULE_MISSING_PREFERENCE_ORDER",
      "Preference order is required when both previousYearGeneralIncome and currentYearGeneralIncome are submitted",
      BAD_REQUEST)

object RulePreferenceOrderNotAllowedError
    extends MtdError(
      "RULE_PREFERENCE_ORDER_NOT_ALLOWED",
      "Preference order must not be provided unless both previousYearGeneralIncome and currentYearGeneralIncome are submitted",
      BAD_REQUEST)

object RuleCarryForwardAndTerminalLossNotAllowedError
    extends MtdError(
      "RULE_CARRY_FORWARD_AND_TERMINAL_LOSS_NOT_ALLOWED",
      "Carry forward loss claims cannot be submitted with terminal loss claims",
      BAD_REQUEST)

object RuleCarryBackClaimError
    extends MtdError("RULE_CARRY_BACK_CLAIM", "Carry back claim type is not valid for property income sources", BAD_REQUEST)

object RuleDuplicateSubmissionError extends MtdError("RULE_DUPLICATE_SUBMISSION", "Submission already exists", BAD_REQUEST)

object RuleDuplicateClaimSubmissionError extends MtdError("RULE_DUPLICATE_SUBMISSION", "This claim matches a previous submission", BAD_REQUEST)

object RuleDeleteAfterFinalDeclarationError
    extends MtdError("RULE_DELETE_AFTER_FINAL_DECLARATION", "This loss cannot be deleted after final declaration", BAD_REQUEST)

object RuleTypeOfClaimInvalid
    extends MtdError("RULE_TYPE_OF_CLAIM_INVALID", "The claim type selected is not available for this type of loss", BAD_REQUEST)

object RuleCSFHLClaimNotSupportedError
    extends MtdError(
      "RULE_CSFHL_CLAIM_NOT_SUPPORTED",
      "carry-sideways-fhl claim for income sources foreign-property-fhl-eea and uk-property-fhl will not be supported from tax year 25/26",
      BAD_REQUEST)

object RuleClaimTypeNotChanged extends MtdError("RULE_NO_CHANGE", "This claim matches a previous submission", BAD_REQUEST)

object RulePeriodNotEnded extends MtdError("RULE_ACCOUNTING_PERIOD_NOT_ENDED", "The accounting period has not yet ended", BAD_REQUEST)

object RuleLossAmountNotChanged extends MtdError("RULE_NO_CHANGE", "The brought forward loss amount has not changed", BAD_REQUEST)

object RuleNoAccountingPeriod extends MtdError("RULE_NO_ACCOUNTING_PERIOD", "For the year of the claim there is no accounting period", BAD_REQUEST)

object RuleInvalidSequenceStart extends MtdError("RULE_INVALID_SEQUENCE_START", "The sequence does not begin with 1", BAD_REQUEST)

object RuleSequenceOrderBroken extends MtdError("RULE_SEQUENCE_ORDER_BROKEN", "The sequence is not continuous", BAD_REQUEST)

object RuleLossClaimsMissing extends MtdError("RULE_LOSS_CLAIMS_MISSING", "One or more loss claims missing from this request", BAD_REQUEST)

object RuleBflNotSupportedForFhlProperties
    extends MtdError(
      "RULE_BFL_NOT_SUPPORTED_FOR_FHL_PROPERTIES",
      "Brought forward losses for income sources uk-property-fhl and foreign-property-fhl-eea are not supported from tax year 25/26",
      BAD_REQUEST)

object RuleOutsideAmendmentWindow extends MtdError("RULE_OUTSIDE_AMENDMENT_WINDOW", "You are outside the amendment window", BAD_REQUEST)

//Stub Errors
object RuleIncorrectGovTestScenarioError
    extends MtdError("RULE_INCORRECT_GOV_TEST_SCENARIO", "The supplied Gov-Test-Scenario is not valid", BAD_REQUEST)
