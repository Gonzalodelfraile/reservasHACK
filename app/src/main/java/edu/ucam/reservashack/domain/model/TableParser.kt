package edu.ucam.reservashack.domain.model

/**
 * Utilidades para parsear nombres de mesas con formato "Fila X;Mesa Y"
 */
object TableParser {
    /**
     * Extrae el número de fila del nombre de una mesa.
     * @param tableName Nombre con formato "Fila X;Mesa Y"
     * @return Número de fila o null si no se puede parsear
     */
    fun extractRowNumber(tableName: String): Int? {
        return tableName.split(";")
            .firstOrNull()
            ?.filter { it.isDigit() }
            ?.toIntOrNull()
    }

    /**
     * Extrae el número de mesa del nombre de una mesa.
     * @param tableName Nombre con formato "Fila X;Mesa Y"
     * @return Número de mesa o null si no se puede parsear
     */
    fun extractTableNumber(tableName: String): Int? {
        return tableName.split(";")
            .lastOrNull()
            ?.filter { it.isDigit() }
            ?.toIntOrNull()
    }

    /**
     * Datos parseados de una mesa
     */
    data class ParsedTable(val rowNumber: Int, val tableNumber: Int)

    /**
     * Parsea completamente un nombre de mesa
     * @return ParsedTable o null si no se puede parsear
     */
    fun parse(tableName: String): ParsedTable? {
        val row = extractRowNumber(tableName) ?: return null
        val table = extractTableNumber(tableName) ?: return null
        return ParsedTable(row, table)
    }
}
