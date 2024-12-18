import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  private val bootstrapVersion = "9.5.0"
  private val hmrcMongoVersion = "2.2.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"         % bootstrapVersion,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-work-item-repo-play-30" % hmrcMongoVersion,
    "uk.gov.hmrc"                   %% "domain-play-30"                    % "10.0.0",
    "com.typesafe.play"             %% "play-json"                         % "2.10.5",
    "com.networknt"                 %  "json-schema-validator"             % "1.5.1",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"              % "2.17.2",
    ehcache
  )

  val AkkaVersion = "2.6.14"

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"       % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"      % hmrcMongoVersion,
    "com.vladsch.flexmark"    %  "flexmark-all"                 % "0.64.8",
    "org.scalatestplus"       %% "scalacheck-1-17"              % "3.2.18.0",
    "org.scalatestplus"       %% "mockito-4-6"                  % "3.2.15.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "7.0.1",
    "org.scalatest"           %% "scalatest"                    % "3.2.19",
    "org.pegdown"             %  "pegdown"                      % "1.6.0"
  ).map(_ % "test")
}
