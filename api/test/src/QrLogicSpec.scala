import java.nio.file.Files

import zio.*
import zio.test.*

import shared.ParseFromTextResponse

object QrLogicSpec extends ZIOSpecDefault:

  private val validRef = "1234.5678.9012.3456"

  def spec: Spec[TestEnvironment, Any] = suite("QrLogic")(
    suite("parseAmount")(
      test("accepts whole euros") {
        for result <- QrLogic.parseAmount("42").either
        yield assertTrue(result == Right(QrLogic.ParsedAmount("EUR42")))
      },
      test("formats cents with a comma separator") {
        for result <- QrLogic.parseAmount("42,50").either
        yield assertTrue(result == Right(QrLogic.ParsedAmount("EUR42.50")))
      },
      test("formats cents with a dot separator") {
        for result <- QrLogic.parseAmount("42.50").either
        yield assertTrue(result == Right(QrLogic.ParsedAmount("EUR42.50")))
      },
      test("keeps a single cent digit as-is") {
        for result <- QrLogic.parseAmount("42,5").either
        yield assertTrue(result == Right(QrLogic.ParsedAmount("EUR42.5")))
      },
      test("rejects non-numeric input") {
        for result <- QrLogic.parseAmount("veertig").either
        yield assertTrue(result == Left(QrLogic.ParseError("Bedrag is niet geldig")))
      },
      test("rejects an empty amount") {
        for result <- QrLogic.parseAmount("").either
        yield assertTrue(result == Left(QrLogic.ParseError("Bedrag is niet geldig")))
      },
      test("rejects an amount with more than five digits") {
        for result <- QrLogic.parseAmount("123456").either
        yield assertTrue(result == Left(QrLogic.ParseError("Bedrag is niet geldig")))
      },
      test("rejects an amount with more than two decimals") {
        for result <- QrLogic.parseAmount("42,505").either
        yield assertTrue(result == Left(QrLogic.ParseError("Bedrag is niet geldig")))
      }
    ),
    suite("parseRef")(
      test("accepts a reference of sixteen digits separated by dots") {
        for result <- QrLogic.parseRef(validRef).either
        yield assertTrue(result == Right(QrLogic.ParsedRef("1234567890123456")))
      },
      test("accepts sixteen plain digits") {
        for result <- QrLogic.parseRef("1234567890123456").either
        yield assertTrue(result == Right(QrLogic.ParsedRef("1234567890123456")))
      },
      test("ignores non-digit separators") {
        for result <- QrLogic.parseRef("1234-5678-9012-3456").either
        yield assertTrue(result == Right(QrLogic.ParsedRef("1234567890123456")))
      },
      test("rejects a reference with too few digits") {
        for result <- QrLogic.parseRef("1234.5678.9012.345").either
        yield assertTrue(result == Left(QrLogic.ParseError("Geen geldig betalingskenmerk")))
      },
      test("rejects a reference with too many digits") {
        for result <- QrLogic.parseRef("12345678901234567").either
        yield assertTrue(result == Left(QrLogic.ParseError("Geen geldig betalingskenmerk")))
      }
    ),
    suite("parseFromText")(
      test("extracts amount and reference from an e-mail text") {
        val text =
          s"""Beste heer/mevrouw,
             |
             |Betaal € 42 aan de Belastingdienst.
             |Betalingskenmerk: $validRef
             |""".stripMargin
        for result <- QrLogic.parseFromText(text).either
        yield assertTrue(result == Right(ParseFromTextResponse("42", validRef)))
      },
      test("fails when the text contains no amount") {
        for result <- QrLogic.parseFromText(s"Betalingskenmerk: $validRef").either
        yield assertTrue(result == Left(QrLogic.ParseError("Geen bedrag gevonden")))
      },
      test("fails when the text contains no reference") {
        for result <- QrLogic.parseFromText("Betaal € 42 aan de Belastingdienst.").either
        yield assertTrue(result == Left(QrLogic.ParseError("Geen betalingskenmerk gevonden")))
      }
    ),
    suite("loonBelasting")(
      test("generates an SVG QR code for a valid payment") {
        for
          qrFile <- QrLogic.loonBelasting("42,50", validRef).orDieWith(e => new RuntimeException(e.friendlyText))
          existed = qrFile.exists()
          svg <- ZIO.attempt(Files.readString(qrFile.toPath))
          _ <- ZIO.attempt(qrFile.delete()).ignore
        yield assertTrue(existed, svg.contains("<svg"))
      },
      test("rejects an invalid amount") {
        for result <- QrLogic.loonBelasting("abc", validRef).either
        yield assertTrue(result == Left(QrLogic.ParseError("Bedrag is niet geldig")))
      },
      test("rejects an invalid reference") {
        for result <- QrLogic.loonBelasting("42,50", "123").either
        yield assertTrue(result == Left(QrLogic.ParseError("Geen geldig betalingskenmerk")))
      }
    )
  )
