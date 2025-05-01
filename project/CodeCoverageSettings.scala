import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    "target.*"
  )

  val excludedFiles: Seq[String] = Seq(
    ".*handlers.*",
    ".*repositories.*",
    ".*BuildInfo.*",
    ".*javascript.*",
    ".*GuiceInjector",
    ".*AppConfig",
    ".*Module",
    ".*ControllerConfiguration",
    ".*TestController",
    ".*LanguageSwitchController",
    ".*TestBulkRacDacController.*",
    ".*MigrationService.*",
    "RacDacRequestsQueueRepository.*",
    ".*StartupModule.*"
  )

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageExcludedFiles := excludedFiles.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}