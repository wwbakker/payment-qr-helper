import zio.*
import zio.stream.ZStream
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.*
import zio.http.{Response, Routes, Server}
import Endpoints.*

object ApiMain extends ZIOAppDefault:

  private val app: Routes[Any, Response] =
    ZioHttpInterpreter().toHttp(
      List(
        generateLoonbelastingQr.zServerLogic((betalingskenmerk, bedrag) =>
          QrLogic.loonBelasting(bedrag, betalingskenmerk).mapBoth(e => e.friendlyText, ZStream.fromFile(_))
        ),
        parseEmailForAmountAndRef.zServerLogic(email =>
          QrLogic.parseFromText(email).mapBoth(e => e.friendlyText, identity)
        ),
        mainJs.zServerLogic(_ => ZIO.succeed(ZStream.fromResource("webapp/main.js"))),
        mainJsMap.zServerLogic(_ => ZIO.succeed(ZStream.fromResource("webapp/main.js.map"))),
        index.zServerLogic(_ => ZIO.succeed(ZStream.fromResource("index.html")))
      )
    )


  override val run: ZIO[ZIOAppArgs & Scope, Any, Any] =
      ZIO.attemptBlockingIO(println(s"Application available at: http://localhost:8080")) *>
        BrowserTrigger.openBrowser("http://localhost:8080").delay(1.seconds).forkDaemon *>
          Server.serve(app).provide(Server.default)