import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.20.0",
//    "uk.gov.hmrc"             %% "simple-reactivemongo"       % "8.0.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"         % "0.68.0",
    "uk.gov.hmrc"             %% "domain"                     % "7.0.0-play-28",
    "com.typesafe.play"       %% "play-json-joda"             % "2.6.10",
    "com.typesafe.play"       %% "play-json"                  % "2.6.10",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-work-item-repo-play-27" % "0.51.0",
    "uk.gov.hmrc"             %% "play-scheduling-play-27"           % "7.10.0",
    "com.networknt"           %  "json-schema-validator"      % "1.0.49",
    ehcache
  )

  val AkkaVersion = "2.6.14"

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"       % "5.20.0"   % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "0.68.0"            % Test,
    "com.github.simplyscala"  %% "scalatest-embedmongo"       % "0.2.4"             % Test,
    "com.typesafe.play"       %% "play-test"                    % current   % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"                 % "0.36.8"  % "test, it",
    "org.scalatestplus"       %% "scalatestplus-scalacheck"     % "3.1.0.0-RC2"  % "test",
    "org.mockito"             %  "mockito-core"                 % "4.0.0" % "test",
    "org.mockito"             %% "mockito-scala"                % "1.16.42"  % "test",
    "org.mockito"             %% "mockito-scala-scalatest"      % "1.16.42"  % "test",
    "org.scalacheck"          %% "scalacheck"                   % "1.14.0"  % "test",
    "org.pegdown"             %  "pegdown"                      % "1.6.0"   % "test, it",
    "com.github.tomakehurst"  %  "wiremock-jre8"                % "2.26.0"  % "test",
//    "uk.gov.hmrc"             %% "reactivemongo-test"             % "5.0.0-play-28" % "test",
    "com.typesafe.akka"       %% "akka-testkit"                 % AkkaVersion % Test,
    "com.typesafe.akka"       %% "akka-actor-typed"             % AkkaVersion % Test,
    "com.typesafe.akka"       %% "akka-actor"                   % AkkaVersion % Test,
    "com.typesafe.akka"       %% "akka-protobuf-v3"             % AkkaVersion % Test,
    "com.typesafe.akka"       %% "akka-serialization-jackson"   % AkkaVersion % Test,
    "com.typesafe.akka"       %% "akka-slf4j" % AkkaVersion     % Test,
    "com.typesafe.akka"       %% "akka-stream" % AkkaVersion    % Test
  )
}
