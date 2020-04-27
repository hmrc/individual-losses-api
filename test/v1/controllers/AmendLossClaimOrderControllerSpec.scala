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

package v1.controllers

import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockAmendLossClaimRequestDataParser
import v1.mocks.services.{MockAmendLossClaimService, MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}

class AmendLossClaimControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAmendLossClaimService
    with MockAmendLossClaimRequestDataParser
    with MockHateoasFactory
    with MockAuditService {


  val claimType = "carry-sideways"
  val id = "1234568790ABCDE"
  val sequence = 66

  val claim = Claim(id, sequence)
  amendLossClaimOrderRequest = AmendLossClaimOrderRequest(claimType,claim)


}
