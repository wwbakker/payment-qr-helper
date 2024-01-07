import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

val appContainer: dom.Element = dom.document.querySelector("#appContainer")
val appElement: Div = div(
  img(src := "/loonbelasting-qr.svg?betalingskenmerk=6864 7875 9130 1300", alt := "qr code")
)

def main(args: Array[String]): Unit = {
  render(appContainer, appElement)
}