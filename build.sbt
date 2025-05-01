import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys

val appName = "pensions-scheme-migration"

val silencerVersion = "1.7.0"

lazy val microservice = Project(appName, file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(
    name                             := appName,
    majorVersion                     := 0,
    scalaVersion                     := "3.6.2",
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test
  )
  .settings(
    Test / fork := true,
    Test / javaOptions += "-Dconfig.file=" + Option(System.getProperty("conf/test.application.conf")).getOrElse("conf/test.application.conf")

  )
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo
  ))
  .settings(
    RoutesKeys.routesImport ++= Seq("models.enumeration.JourneyType"),
    PlayKeys.devSettings += "play.server.http.port" -> "8214",
    scalacOptions := Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-encoding", "utf8",
      "-Wconf:src=routes/.*:s"
    ),
  )
  .settings(CodeCoverageSettings.settings)
  .settings(
    RoutesKeys.routesImport ++= Seq(
      "models.MigrationType"
    )
  )
