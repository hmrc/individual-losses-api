/*
 * Copyright 2019 HM Revenue & Customs
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

object NinoFormatError extends MTDError("FORMAT_NINO", "The provided NINO is invalid")
object TaxYearFormatError extends MTDError("FORMAT_TAX_YEAR", "The provided tax year is invalid")
object AmountFormatError extends MTDError("FORMAT_LOSS_AMOUNT", "The format of the loss amount is invalid")
object LossIdFormatError extends MTDError("FORMAT_LOSS_ID", "The format of the supplied loss ID is not valid")

// Rule Errors
object RuleTaxYearNotSupportedError extends MTDError("RULE_TAX_YEAR_NOT_SUPPORTED", "Tax year not supported, because it precedes the earliest allowable tax year")

object RuleIncorrectOrEmptyBodyError extends MTDError("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", "An empty or non-matching body was submitted")
object RuleTaxYearRangeExceededError extends MTDError("RULE_TAX_YEAR_RANGE_EXCEEDED", "Tax year range exceeded. A tax year range of one year is required.")
object RuleTypeOfLossUnsupported extends MTDError("FORMAT_TYPE_OF_LOSS", "The supplied type of loss format is invalid")
object RuleInvalidSelfEmploymentId extends MTDError("FORMAT_SELF_EMPLOYMENT_ID", "The supplied self employment ID format is invalid")
object RulePropertySelfEmploymentId extends MTDError("RULE_SELF_EMPLOYMENT_ID", "An ID was supplied for a non-self employment business type")
object RuleInvalidLossAmount extends MTDError("RULE_LOSS_AMOUNT", "Amount should be a positive number less than 99999999999.99 with up to 2 decimal places")
object RuleDuplicateSubmissionError extends MTDError("RULE_DUPLICATE_SUBMISSION", "A brought forward loss already exists for this income source.")

//Standard Errors
object NotFoundError extends MTDError("MATCHING_RESOURCE_NOT_FOUND", "Matching resource not found")
object DownstreamError extends MTDError("INTERNAL_SERVER_ERROR", "An internal server error occurred")
object BadRequestError extends MTDError("INVALID_REQUEST", "Invalid request")
object BVRError extends MTDError("BUSINESS_ERROR", "Business validation error")
object ServiceUnavailableError extends MTDError("SERVICE_UNAVAILABLE", "Internal server error")

//Authorisation Errors
object UnauthorisedError extends MTDError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised.")

// Accept header Errors
object InvalidAcceptHeaderError extends MTDError("ACCEPT_HEADER_INVALID", "The accept header is missing or invalid")
object UnsupportedVersionError extends MTDError("NOT_FOUND", "The requested resource could not be found.")
