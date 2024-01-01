import zio._
import zio.http._

object ApiMain extends ZIOAppDefault:

  private val errorResponse = handler(Response.error(Status.InternalServerError))

  val app: HttpApp[Any] =
    Routes(
      Method.GET / "" -> Handler.fromResource("index.html").tapErrorZIO(e => ZIO.logErrorCause( "index.html could not be loaded", Cause.fail(e))).orElse(errorResponse),
      Method.GET / "qr.svg" -> Handler.fromZIO(
        QrLogic.loonBelasting("5000.00", "6864 7875 9130 1300")
          .map(file =>
            Response(
              status = Status.Ok,
              headers = MediaType.forFileExtension("svg").map(mt => Headers(Header.ContentType(mt))).getOrElse(Headers.empty),
              body = Body.fromFile(file)
            )
          ).catchAll(error =>
            ZIO.succeed(Response.error(Status.InternalServerError, error.friendlyText))
          )
      )
    ).toHttpApp

  override val run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    Server.serve(app).provide(Server.default)