/*
 * Copyright 2022 HM Revenue & Customs
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

package repositories

import com.google.inject.Inject
import play.api.Configuration
import play.modules.reactivemongo.ReactiveMongoComponent

import scala.concurrent.ExecutionContext

class ListOfLegacySchemesCacheRepository @Inject()(
                                        mongoComponent: ReactiveMongoComponent,
                                        configuration: Configuration
                                      )(implicit val ec: ExecutionContext)
  extends ManageCacheRepository(
    configuration.get[String](path = "mongodb.migration-cache.list-of-legacy-schemes.name"),
    Some(configuration.get[Int](path = "mongodb.migration-cache.list-of-legacy-schemes.timeToLiveInSeconds")),
    mongoComponent
  )

