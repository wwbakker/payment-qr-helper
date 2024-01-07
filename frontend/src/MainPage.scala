import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.{HTMLAnchorElement, HTMLElement}

val appContainer: dom.Element = dom.document.querySelector("#appContainer")

enum Pages:
  case Loonbelasting, Salaris, Reiskostenvergoeding
  
val selectedPage = Var(Pages.Loonbelasting)

def navigationLink(page: Pages): ReactiveHtmlElement[HTMLAnchorElement] =
  a(
    className := "nav-link",
    className <-- selectedPage.signal.map(p => if p == page then "active" else "inactive"),
    onClick.map(_ => page) --> selectedPage,
    href("#"),
    page.toString
  )

val navigationBar: ReactiveHtmlElement[HTMLElement] =
  navTag(
    className("nav", "nav-pills", "nav-justified"),
    navigationLink(Pages.Loonbelasting),
    navigationLink(Pages.Salaris),
    navigationLink(Pages.Reiskostenvergoeding),
  )


val appElement: Div = div(
  className := "m-1",
  navigationBar,
  child <-- selectedPage.signal.map{
    case Pages.Loonbelasting => LoonbelastingPage.element
    case _ => div()
  }.map(_.amend(className := "m-1"))
)

def main(args: Array[String]): Unit = {
  render(appContainer, appElement)
}