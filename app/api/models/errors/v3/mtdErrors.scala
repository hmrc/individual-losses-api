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

package api.models.errors.v3

import api.models.errors.MtdError

object TaxYearClaimedForFormatError extends MtdError("FORMAT_TAX_YEAR", "The provided tax year claimed for is invalid")

object ValueFormatError extends MtdError("FORMAT_VALUE", "The value must be between 0 and 99999999999.99") {

  def forPathAndRange(path: String, min: String, max: String): MtdError =
    ValueFormatError.copy(paths = Some(Seq(path)), message = s"The value must be between $min and $max")
}

object RuleDuplicateSubmissionError      extends MtdError("RULE_DUPLICATE_SUBMISSION", "Submission already exists")
object RuleDuplicateClaimSubmissionError extends MtdError("RULE_DUPLICATE_SUBMISSION", "This claim matches a previous submission")

object RuleDeleteAfterFinalDeclarationError
    extends MtdError("RULE_DELETE_AFTER_FINAL_DECLARATION", "This loss cannot be deleted after final declaration")
object RuleTypeOfClaimInvalid   extends MtdError("RULE_TYPE_OF_CLAIM_INVALID", "The claim type selected is not available for this type of loss")
object RuleClaimTypeNotChanged  extends MtdError("RULE_NO_CHANGE", "This claim matches a previous submission")
object RulePeriodNotEnded       extends MtdError("RULE_ACCOUNTING_PERIOD_NOT_ENDED", "The accounting period has not yet ended")
object RuleLossAmountNotChanged extends MtdError("RULE_NO_CHANGE", "The brought forward loss amount has not changed")
object RuleNoAccountingPeriod   extends MtdError("RULE_NO_ACCOUNTING_PERIOD", "For the year of the claim there is no accounting period")
object RuleInvalidSequenceStart extends MtdError("RULE_INVALID_SEQUENCE_START", "The sequence does not begin with 1")
object RuleSequenceOrderBroken  extends MtdError("RULE_SEQUENCE_ORDER_BROKEN", "The sequence is not continuous")
object RuleLossClaimsMissing    extends MtdError("RULE_LOSS_CLAIMS_MISSING", "One or more loss claims missing from this request")
