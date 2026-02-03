/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.commons

import shared.utils.UnitSpec
import shared.utils.enums.EnumJsonSpecSupport
import v7.lossesAndClaims.commons.PreferenceOrderEnum.{`carry-back`, `carry-sideways`}

class PreferenceOrderEnumSpec extends UnitSpec with EnumJsonSpecSupport {

  testRoundTrip[PreferenceOrderEnum](
    ("carry-sideways", `carry-sideways`),
    ("carry-back", `carry-back`)
  )

}
