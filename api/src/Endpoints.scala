import sttp.capabilities.zio.ZioStreams
import sttp.model.{MediaType, StatusCode}
import sttp.tapir.ztapir.*
import sttp.tapir.{CodecFormat, Endpoint, Schema}
import zio.*
object Endpoints {

  case object Svg extends CodecFormat {
    override val mediaType: MediaType = MediaType("image", "svg+xml")
  }

  private val svgStreamBody =
    streamBody(ZioStreams)(Schema.binary, Svg, None)

  val index: Endpoint[Unit, Unit, Unit, stream.Stream[Throwable, Byte], ZioStreams] =
    endpoint
      .get
      .out(streamTextBody(ZioStreams)(CodecFormat.TextHtml(), None))

  val mainJs: Endpoint[Unit, Unit, Unit, stream.Stream[Throwable, Byte], ZioStreams] =
    endpoint
      .get
      .in("main.js")
      .out(streamTextBody(ZioStreams)(CodecFormat.TextJavascript(), None))

  val mainJsMap: Endpoint[Unit, Unit, Unit, stream.Stream[Throwable, Byte], ZioStreams] =
    endpoint
      .get
      .in("main.js.map")
      .out(streamTextBody(ZioStreams)(CodecFormat.Json(), None))

  val generateLoonbelastingQr: Endpoint[Unit, (String, String), String, stream.Stream[Throwable, Byte], ZioStreams] =
    endpoint
      .get
      .in("loonbelasting-qr.svg")
      .in(query[String]("betalingskenmerk"))
      .in(query[String]("bedrag"))
      .out(svgStreamBody)
      .errorOut(stringBody)
      .name("loonbelasting-qr.svg")

}
