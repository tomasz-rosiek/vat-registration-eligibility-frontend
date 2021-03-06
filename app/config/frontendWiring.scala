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

import javax.inject.Singleton

import play.api.mvc.Call
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.{HttpDelete, HttpGet, HttpPatch, HttpPost, HttpPut}
import uk.gov.hmrc.http.cache.client.{SessionCache, ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.http.hooks.{HttpHook, HttpHooks}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPatch, WSPost, WSPut}
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.frontend.filters.MicroserviceFilterSupport

object FrontendAuditConnector extends AuditConnector with AppName {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

trait Hooks extends HttpHooks with HttpAuditing {
  override val hooks: Seq[HttpHook] = Seq(AuditingHook)
  override lazy val auditConnector: AuditConnector = FrontendAuditConnector
}

trait WSHttp extends
  HttpGet with WSGet with
  HttpPut with WSPut with
  HttpPatch with WSPatch with
  HttpPost with WSPost with
  HttpDelete with WSDelete with
  Hooks with AppName
object WSHttp extends WSHttp {
  override val hooks = NoneRequired
}

object FrontendAuthConnector extends AuthConnector with ServicesConfig with WSHttp {
  val serviceUrl = baseUrl("auth")
  lazy val http = WSHttp
}

object VatShortLivedHttpCaching extends ShortLivedHttpCaching with AppName with ServicesConfig {
  override lazy val http = WSHttp
  override lazy val defaultSource = appName
  override lazy val baseUri = baseUrl("cachable.short-lived-cache")
  override lazy val domain = getConfString("cachable.short-lived-cache.domain",
    throw new Exception(s"Could not find config 'cachable.short-lived-cache.domain'"))
}

@Singleton
class VatShortLivedCache extends ShortLivedCache {
  override implicit lazy val crypto = ApplicationCrypto.JsonCrypto
  override lazy val shortLiveCache = VatShortLivedHttpCaching
}

@Singleton
class VatSessionCache extends SessionCache with AppName with ServicesConfig {
  override lazy val http = WSHttp
  override lazy val defaultSource = appName
  override lazy val baseUri = baseUrl("cachable.session-cache")
  override lazy val domain = getConfString("cachable.session-cache.domain",
    throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))
}

object WhitelistFilter extends AkamaiWhitelistFilter
  with RunMode with MicroserviceFilterSupport {

  override def whitelist: Seq[String] = FrontendAppConfig.whitelist

  override def excludedPaths: Seq[Call] = {
    FrontendAppConfig.whitelistExcluded.map { path =>
      Call("GET", path)
    }
  }

  override def destination: Call = Call("GET", "https://www.tax.service.gov.uk/outage-register-for-vat")
}