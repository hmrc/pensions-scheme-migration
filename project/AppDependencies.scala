import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  private val bootstrapVersion = "9.11.0"
  private val hmrcMongoVersion = "2.6.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"         % bootstrapVersion,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-work-item-repo-play-30" % hmrcMongoVersion,
    "uk.gov.hmrc"                   %% "domain-play-30"                    % "12.0.0",
    "com.typesafe.play"             %% "play-json"                         % "2.10.6",
    "com.networknt"                 %  "json-schema-validator"             % "1.5.6",
    "com.github.java-json-tools"    % "json-schema-validator"               % "2.2.14",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"              % "2.19.0"
  )


  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"       % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"      % hmrcMongoVersion,
    "org.scalatestplus"       %% "scalacheck-1-17"              % "3.2.18.0",
    "org.scalatestplus"       %% "mockito-4-6"                  % "3.2.15.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "7.0.1",
    "org.scalatest"           %% "scalatest"                    % "3.2.19"
  ).map(_ % "test")
}
