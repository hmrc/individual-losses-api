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

package hateoas

import support.UnitSpec
import v1.hateoas.Endpoints.{AmendBFLoss, CreateBFLoss, GetBFLoss}
import v1.hateoas.HateoasFactory
import v1.models.des.{BFLossResponse, CreateBFLossResponse}
import v1.models.domain.TypeOfLoss
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.outcomes.DesResponse

class HateoasFactorySpec extends UnitSpec {

  private val nino = "AA111111A"
  private val lossId = "123456789"
  private val correlationId = "123456789"

  val hateoasFactory = new HateoasFactory

  val createLink = Link(
    href = s"/individual/losses/$nino/brought-forward-losses",
    method = "POST",
    rel = "create-brought-forward-loss"
  )
  val getLink = Link(
    href = s"/individual/losses/$nino/brought-forward-losses/$lossId",
    method = "GET",
    rel = "get-brought-forward-loss"
  )
  val amendLink = Link(
    href = s"/individual/losses/$nino/brought-forward-losses/$lossId/change-loss-amount",
    method = "POST",
    rel = "amend-brought-forward-loss"
  )
  val deleteLink = Link(
    href = s"/individual/losses/$nino/brought-forward-losses/$lossId",
    method = "DELETE",
    rel = "delete-brought-forward-loss"
  )

  "linksForCreateBFLoss" should {
    "return the correct links" when {
      "supplied a nino and lossId" in {
        hateoasFactory.linksForCreateBFLoss(nino, lossId) shouldBe List(getLink, amendLink, deleteLink)
      }
    }
  }
  "linksForAmendBFLoss" should {
    "return the correct links" when {
      "supplied a nino and lossId" in {
        hateoasFactory.linksForAmendBFLoss(nino, lossId) shouldBe List(getLink, amendLink, deleteLink)
      }
    }
  }
  "linksForGetBFLoss" should {
    "return the correct links" when {
      "supplied a nino and lossId" in {
        hateoasFactory.linksForGetBFLoss(nino, lossId) shouldBe List(getLink, amendLink, deleteLink)
      }
    }
  }

  "wrap" should {
    "return the correct response in a HateoasWrapper" when {

      "supplied a CreateBFLossResponse for CreateBFLoss" in {
        val createBFLossResponse = DesResponse(correlationId, CreateBFLossResponse(lossId))
        hateoasFactory.wrap[CreateBFLossResponse](nino, lossId, createBFLossResponse, CreateBFLoss) shouldBe
          DesResponse(correlationId, HateoasWrapper(createBFLossResponse.responseData, List(getLink, amendLink, deleteLink)))
      }

      "supplied a BFLossResponse for AmendBFLoss" in {
        val bfLossResponse = DesResponse(correlationId, BFLossResponse(
          Some("XKIS00000000988"),
          TypeOfLoss.`self-employment`,
          256.78,
          "2019-20",
          "2018-07-13T12:13:48.763Z"
        ))

        hateoasFactory.wrap[BFLossResponse](nino, lossId, bfLossResponse, AmendBFLoss) shouldBe
          DesResponse(correlationId, HateoasWrapper(bfLossResponse.responseData, List(getLink, amendLink, deleteLink)))
      }

      "supplied a BFLossResponse for GetBFLoss" in {
        val bfLossResponse = DesResponse(correlationId, BFLossResponse(
          Some("XKIS00000000988"),
          TypeOfLoss.`self-employment`,
          256.78,
          "2019-20",
          "2018-07-13T12:13:48.763Z"
        ))

        hateoasFactory.wrap[BFLossResponse](nino, lossId, bfLossResponse, GetBFLoss) shouldBe
          DesResponse(correlationId, HateoasWrapper(bfLossResponse.responseData, List(getLink, amendLink, deleteLink)))
      }

    }
  }

}