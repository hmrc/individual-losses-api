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

package api.models.errors

object NinoFormatError       extends MtdError("FORMAT_NINO", "The provided NINO is invalid")
object TaxYearFormatError    extends MtdError("FORMAT_TAX_YEAR", "The provided tax year is invalid")
object AmountFormatError     extends MtdError("FORMAT_LOSS_AMOUNT", "The provided Loss amount is invalid")
object BusinessIdFormatError extends MtdError("FORMAT_BUSINESS_ID", "The provided Business ID is invalid")
object ClaimIdFormatError           extends MtdError("FORMAT_CLAIM_ID", "The provided claim ID is invalid")
object LossIdFormatError            extends MtdError("FORMAT_LOSS_ID", "The provided loss ID is invalid")
object TypeOfLossFormatError extends MtdError("FORMAT_TYPE_OF_LOSS", "The provided type of loss is invalid")
object TypeOfClaimFormatError       extends MtdError("FORMAT_TYPE_OF_CLAIM", "The provided type of claim is invalid")

//Standard Errors
object NotFoundError           extends MtdError("MATCHING_RESOURCE_NOT_FOUND", "Matching resource not found")
object StandardDownstreamError extends MtdError("INTERNAL_SERVER_ERROR", "An internal server error occurred")
object BadRequestError         extends MtdError("INVALID_REQUEST", "Invalid request")
object BVRError                extends MtdError("BUSINESS_ERROR", "Business validation error")
object ServiceUnavailableError extends MtdError("SERVICE_UNAVAILABLE", "Internal server error")
object InvalidBodyTypeError    extends MtdError("INVALID_BODY_TYPE", "Expecting text/json or application/json body")

//Authorisation Errors
object UnauthorisedError       extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client or agent is not authorised")
object InvalidBearerTokenError extends MtdError("UNAUTHORIZED", "Bearer token is missing or not authorized")

// Accept header Errors
object InvalidAcceptHeaderError extends MtdError("ACCEPT_HEADER_INVALID", "The accept header is missing or invalid")
object UnsupportedVersionError  extends MtdError("NOT_FOUND", "The requested resource could not be found")

// Common rule errors
object RuleTaxYearNotSupportedError
    extends MtdError("RULE_TAX_YEAR_NOT_SUPPORTED", "Tax year not supported, because it precedes the earliest allowable tax year")
object RuleIncorrectOrEmptyBodyError extends MtdError("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", "An empty or non-matching body was submitted")
object RuleTaxYearRangeInvalid       extends MtdError("RULE_TAX_YEAR_RANGE_INVALID", "Tax year range invalid. A tax year range of one year is required")
object RuleTaxYearNotEndedError      extends MtdError("RULE_TAX_YEAR_NOT_ENDED", "The tax year for this brought forward loss has not yet ended")

object RuleBusinessId
    extends MtdError("RULE_BUSINESS_ID", "A Business ID must be supplied for a self-employment or a foreign property business type.")
