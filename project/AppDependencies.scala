import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.14.0",
    "uk.gov.hmrc"             %% "simple-reactivemongo"       % "8.0.0-play-28",
    "uk.gov.hmrc"             %% "domain"                     % "6.2.0-play-28",
    "com.typesafe.play"       %% "play-json-joda"             % "2.6.10",
    "com.typesafe.play"       %% "play-json"                  % "2.6.10",
    "uk.gov.hmrc"             %% "work-item-repo"             % "8.1.0-play-28",
    ehcache
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % "5.14.0"   % Test,
    "org.scalatest"           %% "scalatest"                % "3.2.0"   % Test,
    "com.typesafe.play"       %% "play-test"                % current   % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.36.8"  % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "5.1.0"   % "test, it",
    "org.mockito"             %  "mockito-core"              % "4.0.0" % "test",
    "org.scalacheck"          %% "scalacheck"               % "1.14.0"  % "test",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"   % "test, it",
    "com.github.tomakehurst"  %  "wiremock-jre8"            % "2.26.0"  % "test",
    "uk.gov.hmrc"            %% "reactivemongo-test"         % "5.0.0-play-28" % "test"


  )
}
