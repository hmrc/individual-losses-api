/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package v5.bfLosses.retrieve.model.response

import shared.utils.UnitSpec

class RetrieveBFLossResponseSpec extends UnitSpec {

  "RetrieveBFLossResponse" when {
    "deserialised from downstream JSON" when {
      "the downstream JSON has a Def1 tax year" must {
        "return a Def1 instance" in {
          fail()
        }
      }

      "the downstream JSON has a Def2 tax year" must {
        "return a Def2 instance" in {
          fail()
        }
      }
    }

    "serialized to MTD JSON" when {
      "a Def1 instance is passed" must {
        "work" in {
          fail()
        }
      }

      "a Def2 instance is passed" must {
        "work" in {
          fail()
        }
      }

    }
  }

}
