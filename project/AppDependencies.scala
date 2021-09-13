import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-27"  % "5.2.0",
    "uk.gov.hmrc"             %% "simple-reactivemongo"       % "8.0.0-play-27",
    "uk.gov.hmrc"             %% "domain"                     % "5.11.0-play-27",
    "uk.gov.hmrc"             %% "work-item-repo"             % "8.1.0-play-27"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-27"   % "4.1.0"   % Test,
    "org.scalatest"           %% "scalatest"                % "3.0.8"   % Test,
    "com.typesafe.play"       %% "play-test"                % current   % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.36.8"  % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "4.0.3"   % "test, it",
    "com.typesafe.play"       %% "play-cache"               % current   % "test",
    "org.mockito"             %  "mockito-all"              % "1.10.19" % "test",
    "com.github.tomakehurst"  %  "wiremock"                 % "2.26.0"  % "test",
    "org.scalacheck"          %% "scalacheck"               % "1.14.0"  % "test",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"   % "test, it",
    "org.mockito"             %  "mockito-all"              % "1.10.19" % "test",
    "com.github.tomakehurst"  %  "wiremock-jre8"            % "2.26.0"  % "test"

  )
}
