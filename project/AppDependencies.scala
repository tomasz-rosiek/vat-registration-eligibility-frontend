import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object AppDependencies {
  def apply() = MainDependencies() ++ UnitTestDependencies() ++ IntegrationTestDependencies()
}

object MainDependencies {
  private val frontendBootstrapVersion        = "8.8.0"
  private val playPartialsVersion             = "6.1.0"
  private val httpCachingVersion              = "7.0.0"
  private val playWhitelistVersion            = "2.0.0"
  private val playConditionalMappingVersion   = "0.2.0"
  private val scalaCatsVersion                = "0.9.0"
  private val timeVersion                     = "3.1.0"

  def apply() = Seq(
    "uk.gov.hmrc"   %% "frontend-bootstrap"             % frontendBootstrapVersion,
    "uk.gov.hmrc"   %% "play-partials"                  % playPartialsVersion,
    "uk.gov.hmrc"   %% "http-caching-client"            % httpCachingVersion,
    "uk.gov.hmrc"   %% "play-whitelist-filter"          % playWhitelistVersion,
    "uk.gov.hmrc"   %% "play-conditional-form-mapping"  % playConditionalMappingVersion,
    "uk.gov.hmrc"   %% "time"                           % timeVersion,
    "org.typelevel" %% "cats"                           % scalaCatsVersion
  )
}

trait TestDependencies {
  val scalaTestPlusVersion     = "2.0.1"
  val hmrcTestVersion          = "2.3.0"
  val scalaTestVersion         = "3.0.4"
  val pegdownVersion           = "1.6.0"
  val mockitoCoreVersion       = "2.11.0"
  val jsoupVersion             = "1.10.3"
  val wireMockVersion          = "2.9.0"

  val scope: Configuration
  val test: Seq[ModuleID]
}

object UnitTestDependencies extends TestDependencies {
  override val scope = Test
  override val test = Seq(
    "uk.gov.hmrc"             %% "hmrctest"           % hmrcTestVersion           % scope,
    "org.scalatest"           %% "scalatest"          % scalaTestVersion          % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play" % scalaTestPlusVersion      % scope,
    "org.pegdown"             %  "pegdown"            % pegdownVersion            % scope,
    "org.jsoup"               %  "jsoup"              % jsoupVersion              % scope,
    "com.typesafe.play"       %% "play-test"          % PlayVersion.current       % scope,
    "org.mockito"             %  "mockito-core"       % mockitoCoreVersion        % scope
  )

  def apply() = test
}

object IntegrationTestDependencies extends TestDependencies {
  override val scope = IntegrationTest
  override val test = Seq(
    "uk.gov.hmrc"             %% "hmrctest"           % hmrcTestVersion           % scope,
    "org.scalatest"           %% "scalatest"          % scalaTestVersion          % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play" % scalaTestPlusVersion      % scope,
    "org.pegdown"             %  "pegdown"            % pegdownVersion            % scope,
    "org.jsoup"               %  "jsoup"              % jsoupVersion              % scope,
    "com.typesafe.play"       %% "play-test"          % PlayVersion.current       % scope,
    "com.github.tomakehurst"  %  "wiremock"           % wireMockVersion           % scope
  )

  def apply() = test
}
