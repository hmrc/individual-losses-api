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

package utils.enums

import shapeless._

// Based on code in https://github.com/milessabin/shapeless/blob/master/examples/src/main/scala/shapeless/examples/enum.scala
object Values {

  trait MkValues[T] {
    def values: List[T]
  }

  object MkValues {
    implicit def values[T, Repr <: Coproduct](implicit gen: Generic.Aux[T, Repr], v: Aux[T, Repr]): MkValues[T] =
      new MkValues[T] {
        def values: List[T] = v.values
      }

    trait Aux[T, Repr] {
      def values: List[T]
    }

    object Aux {
      implicit def cnilAux[A]: Aux[A, CNil] =
        new Aux[A, CNil] {
          def values: Nil.type = Nil
        }

      implicit def cconsAux[T, L <: T, R <: Coproduct](implicit l: Witness.Aux[L], r: Aux[T, R]): Aux[T, L :+: R] =
        new Aux[T, L :+: R] {
          def values: List[T] = l.value :: r.values
        }
    }
  }
}
