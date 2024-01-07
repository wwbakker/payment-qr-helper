import net.glxn.qrgen.javase.QRCode
import net.glxn.qrgen.core.scheme.Girocode
import zio.ZIO

import java.io.File

object QrLogic {

  val belastingdienstBic = "INGBNL2A"
  val belastingdienstIban = "NL86INGB0002445588"
  val belastingdienstApeldoornNaam = "Belastingdienst Apeldoorn"

  sealed trait ServiceError:
    def code: Int
    def friendlyText: String
  case class ParseError(friendlyText: String) extends ServiceError:
    def code = 400
  case class InternalError(friendlyText: String) extends ServiceError:
    def code = 500
  
  
  case class ParsedAmount(value: String)
  case class ParsedRef(value: String)

  private def parseAmount(s: String): ZIO[Any, ParseError, ParsedAmount] =
    val regex = "^([0-9]{1,5})([.,][0-9]{0,2})?$".r
    regex.findFirstMatchIn(s) match
      case None => ZIO.fail(ParseError("Bedrag is niet geldig"))
      case Some(m) =>
        m.subgroups match
          case euros :: cents :: Nil if cents == null =>
            ZIO.succeed(ParsedAmount(s"EUR$euros"))
          case euros :: seperatorAndCents :: Nil =>
            ZIO.succeed(ParsedAmount(s"EUR$euros.${seperatorAndCents.tail}"))
          case _ =>
            ZIO.fail(ParseError("Inlezen bedrag ging fout"))


  private def parseRef(s: String): ZIO[Any, ParseError, ParsedRef] =
    val digitsOnly = s.filter(_.isDigit)
    if digitsOnly.length == 4 * 4 then
      ZIO.succeed(ParsedRef(digitsOnly))
    else
      ZIO.fail(ParseError("Geen geldig betalingskenmerk"))



  private def loonbelastingQr(parsedAmount: ParsedAmount, parsedRef: ParsedRef): ZIO[Any, InternalError, java.io.File] =
    ZIO.attemptBlockingIO {
      // https://www.europeanpaymentscouncil.eu/sites/default/files/kb/file/2022-09/EPC069-12%20v3.0%20Quick%20Response%20Code%20-%20Guidelines%20to%20Enable%20the%20Data%20Capture%20for%20the%20Initiation%20of%20an%20SCT_0.pdf
      val qr = new Girocode
      qr.setEncoding(Girocode.Encoding.UTF_8)
      qr.setBic(belastingdienstBic)
      qr.setName(belastingdienstApeldoornNaam)
      qr.setIban(belastingdienstIban)
      qr.setAmount(parsedAmount.value)
      qr.setReference(parsedRef.value)
      QRCode.from(qr).withSize(500, 500).svg()
    }.logError("Failed to create QR").catchAll(_ => ZIO.fail(InternalError("Aanmaken QR code mislukt door interne fout.")))
    
  def loonBelasting(unparsedAmount: String, unparsedRef: String): ZIO[Any, ServiceError, File] =
    for 
      parsedAmount <- parseAmount(unparsedAmount)
      parsedRef <- parseRef(unparsedRef)
      qrCode <- loonbelastingQr(parsedAmount, parsedRef)
    yield qrCode
}
