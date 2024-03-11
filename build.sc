import coursier.maven.MavenRepository
import mill._, scalalib._, scalajslib._

trait AppScalaModule extends ScalaModule {
  def scalaVersion = "3.3.1"
}

trait AppScalaJSModule extends AppScalaModule with ScalaJSModule {
  def scalaJSVersion = "1.15.0"
}

object api extends AppScalaModule {
  override def moduleDeps: Seq[JavaModule] = Seq(shared.jvm)

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

  override def resources = T {
    os.makeDir(T.dest / "webapp")
    val jsPath = frontend.fastLinkJS().dest.path
    // Move main.js[.map]into the proper filesystem position
    // in the resource folder for the web server code to pick up
    os.copy(jsPath / "main.js", T.dest / "webapp" / "main.js")
    os.copy(jsPath / "main.js.map", T.dest / "webapp" / "main.js.map")
    super.resources() ++ Seq(PathRef(T.dest))
  }
}

object frontend extends AppScalaJSModule {
  override def moduleDeps: Seq[JavaModule] = Seq(shared.js)

  override def ivyDeps = Agg(
    ivy"com.raquo::laminar::16.0.0",
  )
}

object shared extends Module {
  trait SharedModule extends AppScalaModule with PlatformScalaModule

  object jvm extends SharedModule
  object js extends SharedModule with AppScalaJSModule
}