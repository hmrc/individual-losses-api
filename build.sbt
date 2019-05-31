import _root_.play.sbt.PlayScala
import sbt.Keys.{testOptions, _}
import sbt.Tests.{Group, SubProcess}
import sbt.{Tests, _}
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

lazy val appName = "api-example-microservice"

lazy val plugins: Seq[Plugins] = Seq(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
lazy val playSettings: Seq[Setting[_]] = Seq.empty
lazy val microservice = (project in file("."))
  .enablePlugins(plugins: _*)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    name := appName,
    targetJvm := "jvm-1.8",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test(),
    parallelExecution in Test := false,
    fork in Test := false,
    retrieveManaged := true,
    scalaVersion := "2.11.11",
    majorVersion := 0
  )
  .settings(
    testOptions in Test := Seq(Tests.Filter(unitFilter)),
    addTestReportOption(Test, "test-reports"),
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-eT")
  )
  .settings(
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources"
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := Seq((baseDirectory in IntegrationTest).value / "test/it"),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    testOptions in IntegrationTest += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    parallelExecution in IntegrationTest := false)
  .configs(ComponentTest)
  .settings(inConfig(ComponentTest)(Defaults.testSettings): _*)
  .settings(
    testOptions in ComponentTest := Seq(Tests.Filter(componentTestFilter)),
    unmanagedSourceDirectories in ComponentTest := Seq((baseDirectory in ComponentTest).value / "test/component"),
    unmanagedResourceDirectories in ComponentTest := Seq((baseDirectory in ComponentTest).value / "test/component")
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    resolvers += Resolver.bintrayRepo("hmrc", "releases"),
    resolvers += Resolver.jcenterRepo)
  .settings(ivyScala := ivyScala.value map {
    _.copy(overrideScalaVersion = true)
  })

lazy val ComponentTest = config("component") extend IntegrationTest

def componentTestFilter(name: String): Boolean = name startsWith "component"

def unitFilter(name: String): Boolean = name startsWith "unit"

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests map {
    test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }

// Coverage configuration
coverageMinimum := 95
coverageFailOnMinimum := true
coverageExcludedPackages := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;testOnlyDoNotUseInAppConf.*;app.*;uk.gov.hmrc.BuildInfo"
