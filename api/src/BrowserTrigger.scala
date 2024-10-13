import zio.{IO, ZIO}

import java.awt.Desktop
import java.net.URI

object BrowserTrigger {

  def openBrowser(url: String): IO[Exception, Unit] = ZIO.attemptBlockingIO(
    if (Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.BROWSE)) {
      Desktop.getDesktop.browse(new URI(url))
    } else {
      println("Desktop or browse action is not supported!")
    }
  )

}
