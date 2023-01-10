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

package api.models.errors

import play.api.http.Status.BAD_REQUEST

// MtdError types that are common to SA Accounts API.

// Format Errors
object AmountFormatError            extends MtdError("FORMAT_LOSS_AMOUNT", "The provided Loss amount is invalid", BAD_REQUEST)
object ClaimIdFormatError           extends MtdError("FORMAT_CLAIM_ID", "The provided claim ID is invalid", BAD_REQUEST)
object LossIdFormatError            extends MtdError("FORMAT_LOSS_ID", "The provided loss ID is invalid", BAD_REQUEST)
object TypeOfLossFormatError        extends MtdError("FORMAT_TYPE_OF_LOSS", "The provided type of loss is invalid", BAD_REQUEST)
object TypeOfClaimFormatError       extends MtdError("FORMAT_TYPE_OF_CLAIM", "The provided type of claim is invalid", BAD_REQUEST)
object TaxYearClaimedForFormatError extends MtdError("FORMAT_TAX_YEAR", "The provided tax year claimed for is invalid", BAD_REQUEST)

// Rule Errors
object RuleDuplicateSubmissionError      extends MtdError("RULE_DUPLICATE_SUBMISSION", "Submission already exists", BAD_REQUEST)
object RuleDuplicateClaimSubmissionError extends MtdError("RULE_DUPLICATE_SUBMISSION", "This claim matches a previous submission", BAD_REQUEST)

object RuleDeleteAfterFinalDeclarationError
    extends MtdError("RULE_DELETE_AFTER_FINAL_DECLARATION", "This loss cannot be deleted after final declaration", BAD_REQUEST)

object RuleTypeOfClaimInvalid
    extends MtdError("RULE_TYPE_OF_CLAIM_INVALID", "The claim type selected is not available for this type of loss", BAD_REQUEST)

object RuleClaimTypeNotChanged  extends MtdError("RULE_NO_CHANGE", "This claim matches a previous submission", BAD_REQUEST)
object RulePeriodNotEnded       extends MtdError("RULE_ACCOUNTING_PERIOD_NOT_ENDED", "The accounting period has not yet ended", BAD_REQUEST)
object RuleLossAmountNotChanged extends MtdError("RULE_NO_CHANGE", "The brought forward loss amount has not changed", BAD_REQUEST)
object RuleNoAccountingPeriod   extends MtdError("RULE_NO_ACCOUNTING_PERIOD", "For the year of the claim there is no accounting period", BAD_REQUEST)
object RuleInvalidSequenceStart extends MtdError("RULE_INVALID_SEQUENCE_START", "The sequence does not begin with 1", BAD_REQUEST)
object RuleSequenceOrderBroken  extends MtdError("RULE_SEQUENCE_ORDER_BROKEN", "The sequence is not continuous", BAD_REQUEST)
object RuleLossClaimsMissing    extends MtdError("RULE_LOSS_CLAIMS_MISSING", "One or more loss claims missing from this request", BAD_REQUEST)
