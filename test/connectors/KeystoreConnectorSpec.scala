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

package connectors

import config.VatSessionCache
import helpers.VatRegSpec
import mocks.VatMocks
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future
import uk.gov.hmrc.http.HttpResponse

class KeystoreConnectorSpec extends VatRegSpec with VatMocks {

  val mockVatSessionCache = mock[VatSessionCache]

  val connector = new KeystoreConnector(mockVatSessionCache)

  case class TestModel(test: String)

  object TestModel {
    implicit val formats = Json.format[TestModel]
  }

  val testModel = TestModel("test")

  "Saving into KeyStore" should {
    "save the model" in {
      val returnCacheMap = CacheMap("", Map("" -> Json.toJson(testModel)))

      when(mockVatSessionCache.cache[TestModel](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(returnCacheMap))

      connector.cache("", testModel) returns returnCacheMap
    }
  }

  "Fetching and getting from KeyStore" should {
    "return a list" in {
      val list = List(testModel)

      when(mockVatSessionCache.fetchAndGetEntry[List[TestModel]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(list)))

      connector.fetchAndGet[List[TestModel]]("") returns Some(list)
    }
  }

  "Fetching from KeyStore" should {
    "return a CacheMap" in {
      val returnCacheMap = CacheMap("", Map("" -> Json.toJson(testModel)))

      when(mockVatSessionCache.fetch()(any(), any())).thenReturn(Future.successful(Some(returnCacheMap)))

      await(connector.fetch()) shouldBe Some(returnCacheMap)
    }
  }

  "Removing from KeyStore" should {
    "return a HTTP Response" in {
      when(mockVatSessionCache.remove()(any(), any())).thenReturn(Future.successful(HttpResponse(OK)))

      await(connector.remove()).status shouldBe HttpResponse(OK).status
    }
  }
}
