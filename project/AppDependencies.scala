import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  private val playVersion = "7.13.0"
  val compile = Seq(
    ws,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-28"         % playVersion,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-work-item-repo-play-28" % "0.74.0",
    "uk.gov.hmrc"                   %% "domain"                            % "8.1.0-play-28",
    "com.typesafe.play"             %% "play-json-joda"                    % "2.9.4",
    "com.typesafe.play"             %% "play-json"                         % "2.9.4",
    "com.networknt"                 %  "json-schema-validator"             % "1.0.76",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"              % "2.14.2",
    ehcache
  )

  val AkkaVersion = "2.6.14"

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"       % playVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"      % "0.74.0",
    "com.vladsch.flexmark"    %  "flexmark-all"                 % "0.64.0",
    "de.flapdoodle.embed"     %  "de.flapdoodle.embed.mongo"    % "3.5.3",
    "org.scalatestplus"       %% "scalacheck-1-17"              % "3.2.15.0",
    "org.scalatestplus"       %% "mockito-4-6"                  % "3.2.15.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "5.1.0",
    "org.scalatest"           %% "scalatest"                    % "3.2.15",
    "org.pegdown"             %  "pegdown"                      % "1.6.0",
    "com.github.tomakehurst"  %  "wiremock-jre8"                % "2.35.0"
  ).map(_ % "test")
}
