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
    scalaVersion                     := "2.13.12",
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
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;.*Module.*;.*TestBulkRacDacController*.*;.*MigrationService",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
  .settings(
    RoutesKeys.routesImport ++= Seq(
      "models.MigrationType"
    )
  )
