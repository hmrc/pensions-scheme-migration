import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"         % "5.20.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"                % "0.69.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-work-item-repo-play-28" % "0.69.0",
    "uk.gov.hmrc"             %% "domain"                            % "7.0.0-play-28",
    "com.typesafe.play"       %% "play-json-joda"                    % "2.6.10",
    "com.typesafe.play"       %% "play-json"                         % "2.6.10",
    "com.networknt"           %  "json-schema-validator"             % "1.0.49",
    ehcache
  )

  val AkkaVersion = "2.6.14"

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"       % "5.20.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"      % "0.69.0",
    "com.github.simplyscala"  %% "scalatest-embedmongo"         % "0.2.4",
    "com.typesafe.play"       %% "play-test"                    % current,
    "com.vladsch.flexmark"    %  "flexmark-all"                 % "0.36.8",
    "org.scalatestplus"       %% "scalatestplus-scalacheck"     % "3.1.0.0-RC2",
    "org.mockito"             %  "mockito-core"                 % "4.0.0",
    "org.mockito"             %% "mockito-scala"                % "1.16.42",
    "org.mockito"             %% "mockito-scala-scalatest"      % "1.16.42",
    "org.scalacheck"          %% "scalacheck"                   % "1.14.0",
    "org.pegdown"             %  "pegdown"                      % "1.6.0",
    "com.github.tomakehurst"  %  "wiremock-jre8"                % "2.26.0",
    "com.typesafe.akka"       %% "akka-testkit"                 % AkkaVersion,
    "com.typesafe.akka"       %% "akka-actor-typed"             % AkkaVersion,
    "com.typesafe.akka"       %% "akka-actor"                   % AkkaVersion,
    "com.typesafe.akka"       %% "akka-protobuf-v3"             % AkkaVersion,
    "com.typesafe.akka"       %% "akka-serialization-jackson"   % AkkaVersion,
    "com.typesafe.akka"       %% "akka-slf4j" % AkkaVersion,
    "com.typesafe.akka"       %% "akka-stream" % AkkaVersion
  ).map(_ % "test")
}
