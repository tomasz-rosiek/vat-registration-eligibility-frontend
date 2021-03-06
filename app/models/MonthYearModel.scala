/*
 * Copyright 2017 HM Revenue & Customs
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

package models


import java.time.LocalDate
import java.time.format.{DateTimeFormatter, ResolverStyle}
import java.time.temporal.TemporalAdjusters

import scala.util.Try


case class DayMonthYearModel(day:String,month: String, year: String){

  def toLocalDate: Option[LocalDate] = Try {
    LocalDate.parse(s"$day-$month-$year", DayMonthYearModel.formatter)
  }.toOption

}

object DayMonthYearModel {

  //uuuu for year as we're using STRICT ResolverStyle
  val formatter = DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT)

  def fromLocalDate(localDate: LocalDate): DayMonthYearModel =
    DayMonthYearModel(localDate.getDayOfMonth.toString,localDate.getMonthValue.toString, localDate.getYear.toString)

  val FORMAT_DD_MMMM_Y = DateTimeFormatter.ofPattern("dd MMMM y").withResolverStyle(ResolverStyle.STRICT)


}

case class MonthYearModel(month: String, year: String) {

  def toLocalDate: Option[LocalDate] = Try {
    LocalDate.parse(s"1-$month-$year", MonthYearModel.formatter).`with`(TemporalAdjusters.lastDayOfMonth())
  }.toOption

}

object MonthYearModel {

  //uuuu for year as we're using STRICT ResolverStyle
  val formatter = DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT)

  def fromLocalDate(localDate: LocalDate): MonthYearModel =
    MonthYearModel(localDate.getMonthValue.toString, localDate.getYear.toString)

  val FORMAT_DD_MMMM_Y = DateTimeFormatter.ofPattern("dd MMMM y").withResolverStyle(ResolverStyle.STRICT)

}
