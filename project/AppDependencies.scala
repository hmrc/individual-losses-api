import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "0.32.0"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.4.0-play-25" % "test,it",
    "org.scalaj" %% "scalaj-http" % "2.4.0" % "test,it",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test,it",
    "org.pegdown" % "pegdown" % "1.6.0" % "test,it",
    "com.typesafe.play" %% "play-test" % PlayVersion.current % "test,it",
    "com.github.tomakehurst" % "wiremock" % "1.58" % "test,it",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test,it",
    "org.mockito" % "mockito-core" % "1.10.19" % "test,it",
    "info.cukes" %% "cucumber-scala" % "1.2.5" % "test,it",
    "info.cukes" % "cucumber-junit" % "1.2.5" % "test,it"
  )

}
