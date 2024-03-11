import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.{Blob, HTMLAnchorElement, HTMLDivElement, HTMLElement, URL, Window, WindowLocalStorage, window}

object LoonbelastingPage {
  private def loonBelastingBedragFromLocalStorage: String = {
    val bedrag = window.localStorage.getItem("loonbelastingBedrag")
    if bedrag == null then "0.00" else bedrag
  }
  private val betalingskenmerk = Var("")
  private val betalingskenmerkInputId = "betalingskenmerkInput"
  private val loonbelastingBedrag = Var(loonBelastingBedragFromLocalStorage)
  private val loonbelastingBedragInputId = "loonbelastingBedragInput"
  private def qrResponseDecoder(response: dom.Response): EventStream[Either[String, Blob]] = {
    if response.status == 200 then
      EventStream.fromJsPromise(response.blob()).map(Right(_))
    else
      EventStream.fromJsPromise(response.text()).map(Left(_))
  }

  private val qrResponseStream: EventStream[Either[String, Blob]] =
    for
      betalingsKenmerk <- betalingskenmerk.signal
      bedrag <- loonbelastingBedrag.signal
      response <- FetchStream.withDecoder(qrResponseDecoder).get(s"/loonbelasting-qr.svg?bedrag=$bedrag&betalingskenmerk=$betalingsKenmerk")
    yield response


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


  val element: ReactiveHtmlElement[HTMLDivElement] =
    div(
      div(
        className := "mb-3",
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
          inContext(thisNode => onKeyUp.map(_ => thisNode.ref.value) --> betalingskenmerk)
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
          value := loonBelastingBedragFromLocalStorage,
          inContext(thisNode => onKeyUp.map{ _ =>
            val bedrag = thisNode.ref.value
            window.localStorage.setItem("loonbelastingBedrag", bedrag)
            bedrag
          } --> loonbelastingBedrag),
        ),
        span(child <-- loonbelastingBedrag.signal.map(bedrag => s"Bedrag: €$bedrag"))
      ),
      div(
        child <-- imageOrError
      )
    )
}
