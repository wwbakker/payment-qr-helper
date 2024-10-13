import com.raquo.airstream.web.FetchOptions
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.{Blob, HTMLAnchorElement, HTMLDivElement, HTMLElement, URL, Window, WindowLocalStorage, window}
import shared.ParseFromTextResponse

import scala.util.Try

object LoonbelastingPage {
  private val betalingskenmerk = Var("")
  private val betalingskenmerkInputId = "betalingskenmerkInput"
  private val loonbelastingBedrag = Var("")
  private val loonbelastingBedragInputId = "loonbelastingBedragInput"
  private val prefillTextInput = Var("")
  private val prefillTextInputId = "prefillResponseInput"
  private val prefillResponseAfterCall = Var[Option[Either[String, ParseFromTextResponse]]](None)

  private def qrResponseDecoder(response: dom.Response): EventStream[Either[String, Blob]] = {
    if response.status == 200 then
      EventStream.fromJsPromise(response.blob()).map(Right(_))
    else
      EventStream.fromJsPromise(response.text()).map(Left(_))
  }

  private def decodeParseFromTextResponse(response: Any): Either[String, ParseFromTextResponse] =
    Try{
      val dynamicObject = response.asInstanceOf[scala.scalajs.js.Dynamic]

      // Step 3: Extract fields and create an instance of the case class
      ParseFromTextResponse(
        amount = dynamicObject.amount.asInstanceOf[String],
        ref = dynamicObject.ref.asInstanceOf[String]
      )
    }.toEither.left.map(_.getMessage)


  private def prefillResponseDecoder(response: dom.Response): EventStream[Either[String, ParseFromTextResponse]] = {
    if response.status == 200 then
      EventStream.fromJsPromise(response.json()).map(decodeParseFromTextResponse)
    else
      EventStream.fromJsPromise(response.text()).map(Left(_))
  }

  private val qrResponseStream: EventStream[Either[String, Blob]] =
    betalingskenmerk.signal.flatMapSwitch(betalingsKenmerk =>
      loonbelastingBedrag.signal.flatMapSwitch(loonbelastingBedrag =>
        FetchStream.withDecoder(qrResponseDecoder).get(s"/loonbelasting-qr.svg?bedrag=$loonbelastingBedrag&betalingskenmerk=$betalingsKenmerk")
      )
    )

  private def prefillCall(emailText: String): EventStream[Option[Either[String, ParseFromTextResponse]]] =
    FetchStream.withDecoder(prefillResponseDecoder).put(s"/parse-email", _.body(emailText)).map(Some(_))

  private val imageOrError = qrResponseStream.map{
    case Right(blob) =>
      img(
        className("mx-auto", "d-block", "border"),
        width := "500px",
        height := "500px",
        src := URL.createObjectURL(blob),
        alt := "qr code"
      )

    case Left(error) => span(
      color := "red",
      "Error: ", error
    )
  }

  private def errorMessage(signal: Signal[Either[String, ?]]) =
    div(
      span(
        color := "red",
        child <-- signal.map{
          case Left(error) => error
          case _ => ""
        }
      )
    )

  val element: ReactiveHtmlElement[HTMLDivElement] =
    div(
      div(
        className := "mb-3",
        label(
          forId := prefillTextInputId,
          className := "form-label",
          "Simpele loonstrook e-mail"
        ),
        textArea(
          className := "form-control",
          idAttr := prefillTextInputId,
          inContext(thisNode => onChange.map(_ => thisNode.ref.value).flatMapStream(prefillCall) --> prefillResponseAfterCall)
        ),
        errorMessage(
          prefillResponseAfterCall.signal.map{
            case Some(Left(error)) => Left(error)
            case _ => Right(())
          }
        ),
        label(
          forId := betalingskenmerkInputId,
          className := "form-label",
          "Betalingskenmerk"
        ),
        input(
          `type` := "text",
          className := "form-control",
          idAttr := betalingskenmerkInputId,
          placeholder := "1111 2222 3333 4444",
          inContext(thisNode => onChange.map(_ => thisNode.ref.value) --> betalingskenmerk),
          inContext(thisNode =>
            value <-- prefillResponseAfterCall.signal.map{
              case Some(Right(ParseFromTextResponse(_, ref))) => ref
              case _ => thisNode.ref.value
            }),
          prefillResponseAfterCall.signal.map{
            case Some(Right(ParseFromTextResponse(_, ref))) => ref
            case _ => ""
          } --> betalingskenmerk
        ),
        label(
          forId := loonbelastingBedragInputId,
          className := "form-label",
          "Bedrag"
        ),
        input(
          `type` := "text",
          className := "form-control",
          idAttr := loonbelastingBedragInputId,
          inContext(thisNode =>
            value <-- prefillResponseAfterCall.signal.map{
              case Some(Right(ParseFromTextResponse(amount, _))) => amount
              case _ => thisNode.ref.value
            }),
          inContext(thisNode => onChange.map{ _ => thisNode.ref.value } --> loonbelastingBedrag),
          prefillResponseAfterCall.signal.map{
            case Some(Right(ParseFromTextResponse(amount, _))) => amount
            case _ => ""
          } --> loonbelastingBedrag
        ),
        span(child <-- loonbelastingBedrag.signal.map(bedrag => s"Bedrag: €$bedrag"))
      ),
      div(
        child <-- imageOrError
      )
    )
}
