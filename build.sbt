import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.itSettings

val appName = "pensions-scheme-migration"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.6.4"
ThisBuild / scalacOptions := Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-encoding", "utf8",
//  "-Xfatal-warnings",                        // Treat all warnings as errors
  "-Wconf:src=target/.*:s",                  // silence warnings from compiled files
  "-Wconf:src=routes/.*:silent",             // Suppress warnings from routes files
  "-Wconf:msg=Flag.*repeatedly:silent",      // Suppress warnings for repeated flags
  "-Wconf:msg=.*-Wunused.*:silent"           // Suppress unused warnings
)

val silencerVersion = "1.7.0"

lazy val microservice = Project(appName, file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(
    name                             := appName,
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
    RoutesKeys.routesImport ++= Seq(
      "models.enumeration.JourneyType",
      "utils.Binders.*"
    ),
    PlayKeys.devSettings += "play.server.http.port" -> "8214",
  )
  .settings(CodeCoverageSettings.settings)
  .settings(
    RoutesKeys.routesImport ++= Seq(
      "models.MigrationType"
    )
  )

val it: Project = project.in(file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
