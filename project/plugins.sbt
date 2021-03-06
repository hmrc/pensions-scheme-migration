resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"

resolvers += Resolver.bintrayRepo("hmrc", "releases")

resolvers += Resolver.url(
  name = "HMRC-open-artefacts-ivy",
  baseURL = url("https://open.artefacts.tax.service.gov.uk/ivy2")
)(Resolver.ivyStylePatterns)

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("uk.gov.hmrc"        %   "sbt-auto-build"        % "3.0.0")

addSbtPlugin("uk.gov.hmrc"        %   "sbt-git-versioning"    % "2.2.0")

addSbtPlugin("uk.gov.hmrc"        %   "sbt-distributables"    % "2.1.0")

addSbtPlugin("com.typesafe.play"  %   "sbt-plugin"            % "2.7.5")

addSbtPlugin("org.scalastyle"     %%  "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("org.scoverage"      %   "sbt-scoverage"         % "1.5.1")
