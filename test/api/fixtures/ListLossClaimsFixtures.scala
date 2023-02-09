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

package api.fixtures

import api.endpoints.lossClaim.domain.v3.{ TypeOfClaim, TypeOfLoss }
import api.endpoints.lossClaim.list.v3.response.{ ListLossClaimsItem, ListLossClaimsResponse }

object ListLossClaimsFixtures {

  def listLossClaimsModel(taxYear: String): ListLossClaimsItem = ListLossClaimsItem(
    businessId = "testId",
    typeOfClaim = TypeOfClaim.`carry-sideways`,
    typeOfLoss = TypeOfLoss.`self-employment`,
    taxYearClaimedFor = taxYear,
    claimId = "claimId",
    sequence = Some(1),
    lastModified = "2020-07-13T12:13:48.763Z"
  )

  def singleListLossClaimsResponseModel(taxYear: String): ListLossClaimsResponse[ListLossClaimsItem] = ListLossClaimsResponse(
    List(listLossClaimsModel(taxYear))
  )

  val multipleListLossClaimsResponseModel: ListLossClaimsResponse[ListLossClaimsItem] = ListLossClaimsResponse(
    List(
      listLossClaimsModel("2019-20"),
      listLossClaimsModel("2020-21"),
      listLossClaimsModel("2021-22"),
      listLossClaimsModel("2022-23")
    )
  )

}
