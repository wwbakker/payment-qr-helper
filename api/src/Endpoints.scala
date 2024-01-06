import sttp.capabilities.zio.ZioStreams
import sttp.model.MediaType
import sttp.tapir.ztapir.*
import sttp.tapir.{CodecFormat, Endpoint, Schema}
import zio.*
object Endpoints {

  case object Svg extends CodecFormat {
    override val mediaType: MediaType = MediaType("image", "svg+xml")
  }

  private val svgStreamBody =
    streamBody(ZioStreams)(Schema.binary, Svg, None)

  val index: Endpoint[Unit, Unit, Unit, stream.Stream[Throwable, Byte], Any with ZioStreams] =
    endpoint
      .get
      .out(streamTextBody(ZioStreams)(CodecFormat.TextHtml(), None))

  val generateLoonbelastingQr: Endpoint[Unit, String, Unit, stream.Stream[Throwable, Byte], ZioStreams] =
    endpoint
      .get
      .in("loonbelasting-qr.svg")
      .in(query[String]("betalingskenmerk"))
      .out(svgStreamBody)
      .name("loonbelasting-qr.svg")

}
