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

import org.slf4j.{Logger, LoggerFactory}
import play.api.http.Status
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, Upstream4xxResponse, Upstream5xxResponse}

import scala.language.implicitConversions

package object connectors {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  implicit def classToString(`class`: Class[_]): String = `class`.toString

  private[connectors] def logResponse(e: Throwable, f: String): Throwable = {
    def log(s: String): Unit = logger.warn(s"[$f] received $s")

    e match {
      case e: NotFoundException   => log("NOT FOUND")
      case e: BadRequestException => log("BAD REQUEST")
      case e: Upstream4xxResponse => e.upstreamResponseCode match {
        case Status.FORBIDDEN => log("FORBIDDEN")
        case _                => log(s"Upstream 4xx: ${e.upstreamResponseCode} ${e.message}")
      }
      case e: Upstream5xxResponse => log(s"Upstream 5xx: ${e.upstreamResponseCode}")
      case e: Exception           => log(s"ERROR: ${e.getMessage}")
    }
    e
  }
}

