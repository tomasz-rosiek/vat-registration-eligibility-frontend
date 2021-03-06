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

package config

import java.nio.charset.Charset
import java.util.Base64

import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val timeoutInSeconds: String
  val vatRegFrontendFeedbackUrl: String
  val contactFrontendPartialBaseUrl: String
}

object FrontendAppConfig extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val contactFormServiceIdentifier = "SCRS"

  override lazy val contactFrontendPartialBaseUrl = loadConfig("microservice.services.contact-frontend.url")

  override lazy val analyticsToken = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost = loadConfig(s"google-analytics.host")
  override lazy val reportAProblemPartialUrl = s"$contactFrontendPartialBaseUrl/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactFrontendPartialBaseUrl/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  lazy val vatRegFrontendWelcomeUrl: String = loadConfig("microservice.services.vat-registration-frontend.www.url")
  override lazy val vatRegFrontendFeedbackUrl: String = s"${loadConfig("microservice.services.vat-registration-frontend.www.url")}/feedback"

  override val timeoutInSeconds = loadConfig("timeoutInSeconds")

  private def whitelistConfig(key: String): Seq[String] = Some(new String(Base64.getDecoder
    .decode(loadConfig(key)), "UTF-8"))
    .map(_.split(",")).getOrElse(Array.empty).toSeq

  private def loadStringConfigBase64(key : String) : String = {
    new String(Base64.getDecoder.decode(configuration.getString(key).getOrElse("")), Charset.forName("UTF-8"))
  }

  lazy val whitelist = whitelistConfig("whitelist")
  lazy val whitelistExcluded = whitelistConfig("whitelist-excluded")

  lazy val uriWhiteList     = configuration.getStringSeq("csrfexceptions.whitelist").getOrElse(Seq.empty).toSet
  lazy val csrfBypassValue  = loadStringConfigBase64("Csrf-Bypass-value")
}
