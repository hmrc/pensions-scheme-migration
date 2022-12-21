import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-28"         % "7.11.0",
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-work-item-repo-play-28" % "0.73.0",
    "uk.gov.hmrc"                   %% "domain"                            % "8.1.0-play-28",
    "com.typesafe.play"             %% "play-json-joda"                    % "2.9.3",
    "com.typesafe.play"             %% "play-json"                         % "2.9.3",
    "com.networknt"                 %  "json-schema-validator"             % "1.0.73",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"              % "2.14.1",
    ehcache
  )

  val AkkaVersion = "2.6.14"

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"       % "7.11.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"      % "0.73.0",
    "com.vladsch.flexmark"    %  "flexmark-all"                 % "0.62.2",
    "de.flapdoodle.embed"     %  "de.flapdoodle.embed.mongo"    % "3.5.3",
    "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.14.0",
    "org.scalatestplus"       %% "mockito-4-6"                % "3.2.14.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0",
    "org.scalatest"           %% "scalatest"                  % "3.2.14",
    "org.pegdown"             %  "pegdown"                      % "1.6.0",
    "com.github.tomakehurst"  %  "wiremock-jre8"                % "2.35.0"
  ).map(_ % "test")
}
