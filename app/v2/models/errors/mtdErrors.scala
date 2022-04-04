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

package v2.models.errors

import api.models.errors.MtdError

object LossIdFormatError      extends MtdError("FORMAT_LOSS_ID", "The provided loss ID is invalid")
object ClaimIdFormatError     extends MtdError("FORMAT_CLAIM_ID", "The provided claim ID is invalid")
object BusinessIdFormatError  extends MtdError("FORMAT_BUSINESS_ID", "The provided Business ID is invalid")
object TypeOfLossFormatError  extends MtdError("FORMAT_TYPE_OF_LOSS", "The provided Type of Loss is invalid")
object TypeOfClaimFormatError extends MtdError("FORMAT_TYPE_OF_CLAIM", "The provided Type of claim is invalid")
object ClaimTypeFormatError   extends MtdError("FORMAT_CLAIM_TYPE", "The provided claim type is invalid")
object SequenceFormatError    extends MtdError("FORMAT_SEQUENCE", "The provided sequence number is invalid")

object RuleInvalidLossAmount
    extends MtdError("RULE_LOSS_AMOUNT", "Amount should be a positive number less than 99999999999.99 with up to 2 decimal places")
object RuleDuplicateSubmissionError        extends MtdError("RULE_DUPLICATE_SUBMISSION", "A brought forward loss already exists for this income source")
object RuleDuplicateClaimSubmissionError   extends MtdError("RULE_DUPLICATE_SUBMISSION", "This claim matches a previous submission")
object RuleDeleteAfterCrystallisationError extends MtdError("RULE_DELETE_AFTER_CRYSTALLISATION", "This loss cannot be deleted after crystallisation")
object RuleTypeOfClaimInvalid              extends MtdError("RULE_TYPE_OF_CLAIM_INVALID", "The claim type selected is not available for this type of loss")

object RuleClaimTypeNotChanged
    extends MtdError("RULE_ALREADY_EXISTS", "The type of claim has already been requested in this tax year for this income source")
object RulePeriodNotEnded       extends MtdError("RULE_PERIOD_NOT_ENDED", "The relevant accounting period has not yet ended")
object RuleLossAmountNotChanged extends MtdError("RULE_NO_CHANGE", "The brought forward loss amount has not changed")
object RuleNoAccountingPeriod   extends MtdError("RULE_NO_ACCOUNTING_PERIOD", "For the year of the claim there is no accounting period")
object RuleInvalidSequenceStart extends MtdError("RULE_INVALID_SEQUENCE_START", "The sequence does not begin with 1")
object RuleSequenceOrderBroken  extends MtdError("RULE_SEQUENCE_ORDER_BROKEN", "The sequence is not continuous")
object RuleLossClaimsMissing    extends MtdError("RULE_LOSS_CLAIMS_MISSING", "One or more loss claims missing from this request")
