import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.{HTMLAnchorElement, HTMLDivElement, HTMLElement}

object LoonbelastingPage {
  private val betalingskenmerk = Var("")
  
  private val betalingskenmerkInputId = "betalingskenmerkInput"
  
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
      ),
      div(
        img(
          className("mx-auto", "d-block", "border"),
          width := "500px",
          height := "500px",
          src <-- betalingskenmerk.signal.map(bk => s"/loonbelasting-qr.svg?betalingskenmerk=$bk"),
          alt := "qr code"
        )
      )
    )
}
