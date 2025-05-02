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
    ".*BenefitsProvisionType.*",
    ".*BenefitsType.*",
    ".*SchemeMembers.*",
    ".*SchemeType.*",
    ".*MinPSA.*",
    ".*EmailEvents.*",
    ".*WorkItemRequest.*",
    ".*PensionsScheme.*",
    ".*PensionSchemeDeclaration.*",
    ".*Address.*",
    ".*SendEmailRequest.*",
    ".*DeclarationLockJson.*",
    ".*MigrationLock.*",
    ".*DataJson.*",
    ".*LockJson.*",
    ".*Enumerable.*",
    ".*MigrationType.*",
    ".*ContactDetails.*",
    ".*CorrespondenceContactDetails.*",
    ".*PersonalDetails.*",
    ".*Individual.*",
    ".*CustomerAndSchemeDetails.*",
    ".*RACDACDeclaration.*",
    ".*ReadsHelper.*",
    ".*SchemeMigrationDetails.*",
    ".*RACDACSchemeDetails.*",
    ".*CompanyEstablisher.*",
    ".*EstablisherDetails.*",
    ".*Partnership.*",
    ".*CompanyTrustee.*",
  )

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageExcludedFiles := excludedFiles.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}