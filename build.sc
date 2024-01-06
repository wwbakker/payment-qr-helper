import coursier.maven.MavenRepository
import mill._
import scalalib._

val projectScalaVersion = "3.3.1"

object api extends ScalaModule {
  override def scalaVersion = projectScalaVersion
  def zioCoreVersion = "2.0.19"
  def zioHttpVersion = "3.0.0-RC4"
  def zioJsonVersion = "0.6.2"
  override def ivyDeps = Agg(
    ivy"dev.zio::zio:$zioCoreVersion",
    ivy"dev.zio::zio-streams:$zioCoreVersion",
    ivy"dev.zio::zio-http:$zioHttpVersion",
    ivy"dev.zio::zio-json:$zioJsonVersion",
    ivy"com.softwaremill.sttp.tapir::tapir-zio-http-server:1.9.6",
    ivy"com.github.kenglxn.QRGen:javase:3.0.1",
  )

  val jitPackRepository = MavenRepository("https://jitpack.io")

  override def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq(jitPackRepository)
  }
}
