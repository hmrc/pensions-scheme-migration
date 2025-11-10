/*
 * Copyright 2025 HM Revenue & Customs
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

import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {
  private val bootstrapVersion = "10.4.0"
  private val hmrcMongoVersion = "2.10.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"         % bootstrapVersion,
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30"        % bootstrapVersion, //needed for ApplicationCrypto
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-work-item-repo-play-30" % hmrcMongoVersion,
    "uk.gov.hmrc"                   %% "domain-play-30"                    % "13.0.0",
    "com.typesafe.play"             %% "play-json"                         % "2.10.8",
    "com.networknt"                 %  "json-schema-validator"             % "1.5.6",
    "com.github.java-json-tools"    % "json-schema-validator"               % "2.2.14",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"              % "2.19.0"
  )


  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"       % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"      % hmrcMongoVersion,
    "org.scalatestplus"       %% "scalacheck-1-17"              % "3.2.18.0",
    "org.scalatestplus"       %% "mockito-4-6"                  % "3.2.15.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "7.0.2",
    "org.scalatest"           %% "scalatest"                    % "3.2.19"
  ).map(_ % "test")
}
