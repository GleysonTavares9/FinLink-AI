package com.example

import com.example.data.remote.nfce.NfceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NfceParserTest {

    private val parser = NfceParser()

    @Test
    fun testExtractMetadataFromStandard44DigitKey() {
        // UF: 35 (SP), Ano: 24, Mes: 05, CNPJ: 12345678000199 (14 digitos), Mod: 65, Serie: 001, NFe: 000001234, ...
        val key = "35240512345678000199650010000012341000012345"
        val metadata = parser.extractQrCodeMetadata(key)

        assertEquals("35240512345678000199650010000012341000012345", metadata.accessKey)
        assertEquals("12345678000199", metadata.cnpj)
        assertEquals("SP", metadata.ufCode)
        assertEquals("2024-05-01", metadata.emissionDate)
    }

    @Test
    fun testExtractMetadataFromQrCodeUrl() {
        val url = "https://www.nfce.fazenda.sp.gov.br/qrcode?p=35240312345678000199650010000012341000012345|2|1|1|189.50|37.90|4857...|1|ABCDEF"
        val metadata = parser.extractQrCodeMetadata(url)

        assertEquals("35240312345678000199650010000012341000012345", metadata.accessKey)
        assertEquals("12345678000199", metadata.cnpj)
        assertEquals("SP", metadata.ufCode)
        assertEquals(189.50, metadata.totalAmount, 0.001)
    }

    @Test
    fun testParseSefazHtml() {
        val sampleHtml = """
            <html>
                <body>
                    <div class="txtTopo">SUPERMERCADO MODELO LTDA</div>
                    <div class="totalNfe">
                        Valor Total R$ <span class="totalNfeValor">245,80</span>
                    </div>
                    <table>
                        <tr id="Item + 1">
                            <td class="txtTit">ARROZ BRANCO 5KG</td>
                            <td class="Rqtd">1</td>
                            <td class="RvalUnit">28,90</td>
                            <td class="valor">28,90</td>
                        </tr>
                        <tr id="Item + 2">
                            <td class="txtTit">FEIJAO CARIOCA 1KG</td>
                            <td class="Rqtd">2</td>
                            <td class="RvalUnit">7,50</td>
                            <td class="valor">15,00</td>
                        </tr>
                    </table>
                </body>
            </html>
        """.trimIndent()

        val url = "https://www.sefaz.rs.gov.br/NFCE/NFCE-COM.aspx?p=43240398765432000188650010000056781000056789"
        val result = parser.parse(sampleHtml, url)

        assertNotNull(result)
        assertEquals("SUPERMERCADO MODELO LTDA", result.storeName)
        assertEquals(245.80, result.totalAmount, 0.01)
        assertEquals("Mercado", result.category)
        assertTrue(result.items.size >= 2)
        assertTrue(result.items[0].contains("ARROZ BRANCO"))
    }
}
