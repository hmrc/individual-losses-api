/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.models.errors

object NinoFormatError extends MtdError("FORMAT_NINO", "The provided NINO is invalid")
object TaxYearFormatError extends MtdError("FORMAT_TAX_YEAR", "The provided tax year is invalid")
object AmountFormatError extends MtdError("FORMAT_LOSS_AMOUNT", "The format of the loss amount is invalid")
object LossIdFormatError extends MtdError("FORMAT_LOSS_ID", "The provided loss ID is invalid")
object ClaimIdFormatError extends MtdError("FORMAT_CLAIM_ID", "The provided claim ID is invalid")
object SelfEmploymentIdFormatError extends MtdError("FORMAT_SELF_EMPLOYMENT_ID", "The supplied self-employment ID format is invalid")
object TypeOfLossFormatError extends MtdError("FORMAT_TYPE_OF_LOSS", "The supplied type of loss format is invalid")
object TypeOfClaimFormatError extends MtdError("FORMAT_TYPE_OF_CLAIM", "The supplied type of claim format is invalid or the type of claim is not recognised")
object ClaimTypeFormatError extends MtdError("FORMAT_CLAIM_TYPE", "The provided claim type is invalid")
object SequenceFormatError extends MtdError("FORMAT_SEQUENCE", "The provided sequence number is invalid")

// Rule Errors
object RuleTaxYearNotSupportedError extends MtdError("RULE_TAX_YEAR_NOT_SUPPORTED",
  "Tax year not supported, because it precedes the earliest allowable tax year")
object RuleIncorrectOrEmptyBodyError extends MtdError("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", "An empty or non-matching body was submitted")
object RuleTaxYearRangeInvalid extends MtdError("RULE_TAX_YEAR_RANGE_INVALID", "Tax year range invalid. A tax year range of one year is required")
object RuleTaxYearNotEndedError extends MtdError("RULE_TAX_YEAR_NOT_ENDED", "The tax year for this brought forward loss has not yet ended")
object RuleSelfEmploymentId extends MtdError("RULE_SELF_EMPLOYMENT_ID", "A self-employment ID can only be supplied for a self-employment business type")
object RuleInvalidLossAmount extends MtdError("RULE_LOSS_AMOUNT", "Amount should be a positive number less than 99999999999.99 with up to 2 decimal places")
object RuleDuplicateSubmissionError extends MtdError("RULE_DUPLICATE_SUBMISSION", "A brought forward loss already exists for this income source")
object RuleDuplicateClaimSubmissionError extends MtdError("RULE_DUPLICATE_SUBMISSION", "This claim matches a previous submission")
object RuleDeleteAfterCrystallisationError extends MtdError("RULE_DELETE_AFTER_CRYSTALLISATION", "This loss cannot be deleted after crystallisation")
object RuleTypeOfClaimInvalid extends MtdError("RULE_TYPE_OF_CLAIM_INVALID", "The claim type selected is not available for this type of loss")
object RuleClaimTypeNotChanged extends MtdError("RULE_ALREADY_EXISTS", "The type of claim has already been requested in this tax year for this income source")
object RulePeriodNotEnded extends MtdError("RULE_PERIOD_NOT_ENDED", "The relevant accounting period has not yet ended")
object RuleLossAmountNotChanged extends MtdError("RULE_NO_CHANGE", "The brought forward loss amount has not changed")
object RuleNoAccountingPeriod extends MtdError("RULE_NO_ACCOUNTING_PERIOD", "For the year of the claim there is no accounting period")
object RuleInvalidSequenceStart extends MtdError( "RULE_INVALID_SEQUENCE_START", "The sequence does not begin with 1")
object RuleSequenceOrderBroken extends MtdError( "RULE_SEQUENCE_ORDER_BROKEN", "The sequence is not continuous")
object RuleLossClaimsMissing extends MtdError("RULE_LOSS_CLAIMS_MISSING",
  "One or more loss claims missing from this request")

//Standard Errors
object NotFoundError extends MtdError("MATCHING_RESOURCE_NOT_FOUND", "Matching resource not found")
object DownstreamError extends MtdError("INTERNAL_SERVER_ERROR", "An internal server error occurred")
object BadRequestError extends MtdError("INVALID_REQUEST", "Invalid request")
object BVRError extends MtdError("BUSINESS_ERROR", "Business validation error")
object ServiceUnavailableError extends MtdError("SERVICE_UNAVAILABLE", "Internal server error")

object InvalidBodyTypeError extends MtdError("INVALID_BODY_TYPE", "Expecting text/json or application/json body")

//Authorisation Errors
object UnauthorisedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised")
object InvalidBearerTokenError extends MtdError("UNAUTHORIZED", "Bearer token is missing or not authorized")

// Accept header Errors
object InvalidAcceptHeaderError extends MtdError("ACCEPT_HEADER_INVALID", "The accept header is missing or invalid")
object UnsupportedVersionError extends MtdError("NOT_FOUND", "The requested resource could not be found")
