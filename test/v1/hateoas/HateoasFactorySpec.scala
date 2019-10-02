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

package v1.hateoas

import support.UnitSpec
import v1.models.des._
import v1.models.domain.TypeOfLoss
import v1.models.hateoas.{ HateoasWrapper, Link }
import v1.models.outcomes.DesResponse

class HateoasFactorySpec extends UnitSpec {

  private val nino          = "AA111111A"
  private val lossId        = "123456789"
  private val correlationId = "123456789"

  val hateoasFactory = new HateoasFactory

  val createLink = Link(
    href = s"/individuals/losses/$nino/brought-forward-losses",
    method = "POST",
    rel = "create-brought-forward-loss"
  )

  val getLink = Link(
    href = s"/individuals/losses/$nino/brought-forward-losses/$lossId",
    method = "GET",
    rel = "get-brought-forward-loss"
  )

  val amendLink = Link(
    href = s"/individuals/losses/$nino/brought-forward-losses/$lossId/change-loss-amount",
    method = "POST",
    rel = "amend-brought-forward-loss"
  )

  val deleteLink = Link(
    href = s"/individuals/losses/$nino/brought-forward-losses/$lossId",
    method = "DELETE",
    rel = "delete-brought-forward-loss"
  )

  "linksForCreateBFLoss" should {
    "these tests are now in wrong place" in { fail }
    "the factory tests should now be generic (not losses or endpoint specific)" in { fail }

    "return the correct links" when {
      "supplied a nino and lossId" in {
        CreateBFLossResponse.links(nino, lossId) shouldBe List(getLink, amendLink, deleteLink)
      }
    }
  }
  "linksForAmendBFLoss" should {
    "return the correct links" when {
      "supplied a nino and lossId" in {
        BFLossResponse.links(nino, lossId) shouldBe List(getLink, amendLink, deleteLink)
      }
    }
  }
  "linksForGetBFLoss" should {
    "return the correct links" when {
      "supplied a nino and lossId" in {
        BFLossResponse.links(nino, lossId) shouldBe List(getLink, amendLink, deleteLink)
      }
    }
  }
  "linksForListBFLoss" should {
    "return the correct links" when {
      "supplied a nino" in {
        ListBFLossesResponse.links(nino) shouldBe List(createLink)
      }
    }
  }

  "wrap" should {
    "return the correct response in a HateoasWrapper" when {

      "supplied a ListBFLossResponse for ListBFLoss" in {
        val response = ListBFLossesResponse(Seq(BFLossId(lossId), BFLossId(lossId)))

        val result = hateoasFactory.wrapList(response, ListBFLossHateoasData(nino))
        result shouldBe
          DesResponse(
            correlationId,
            HateoasWrapper(
              ListBFLossesHateoasResponse(
                Seq(HateoasWrapper(BFLossId(lossId), List(getLink, amendLink, deleteLink)),
                    HateoasWrapper(BFLossId(lossId), List(getLink, amendLink, deleteLink)))
              ),
              List(createLink)
            )
          )
      }

      "supplied a CreateBFLossResponse for CreateBFLoss" in {
        val createBFLossResponse = CreateBFLossResponse(lossId)
        hateoasFactory.wrap(createBFLossResponse, CreateBFLossHateoasData(nino, lossId)) shouldBe
          HateoasWrapper(createBFLossResponse, List(getLink, amendLink, deleteLink))
      }

      "supplied a BFLossResponse for AmendBFLoss" in {
        val bfLossResponse = BFLossResponse(
          Some("XKIS00000000988"),
          TypeOfLoss.`self-employment`,
          256.78,
          "2019-20",
          "2018-07-13T12:13:48.763Z"
        )

        hateoasFactory.wrap(bfLossResponse, AmendBFLossHateoasData(nino, lossId)) shouldBe
          HateoasWrapper(bfLossResponse, List(getLink, amendLink, deleteLink))
      }

      "supplied a BFLossResponse for GetBFLoss" in {
        val bfLossResponse = BFLossResponse(
          Some("XKIS00000000988"),
          TypeOfLoss.`self-employment`,
          256.78,
          "2019-20",
          "2018-07-13T12:13:48.763Z"
        )

        hateoasFactory.wrap(bfLossResponse, GetBFLossHateoasData(nino, lossId)) shouldBe
          HateoasWrapper(bfLossResponse, List(getLink, amendLink, deleteLink))
      }

    }
  }

}
