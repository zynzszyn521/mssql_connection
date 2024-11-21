package com.example.mssql_connection

import android.util.Log
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.Base64


class ResultSetSerializer constructor (private val chunkSize: Int) : JsonSerializer<ResultSet>() {



    class ResultSetSerializerException(cause: Throwable?) :
        JsonProcessingException(cause) {
        companion object {
            private const val serialVersionUID = -914957626413580734L
        }
    }

    override fun handledType(): Class<ResultSet> {
        return ResultSet::class.java
    }
    
     private fun serializer(rs: ResultSet, gen: JsonGenerator, provider: SerializerProvider) {
        try {
            val resultMetaData = rs.metaData
            val numColumns = resultMetaData.columnCount
            val columnNames = arrayOfNulls<String>(numColumns)
            val columnTypes = IntArray(numColumns)
            for (i in columnNames.indices) {
                columnNames[i] = resultMetaData.getColumnLabel(i + 1)
                columnTypes[i] = resultMetaData.getColumnType(i + 1)
            }
            var currentArraySize =0
            gen.writeStartArray()
            while (rs.next()) {
                if (currentArraySize == chunkSize){
                    Log.i("Serializer","currentArraySize - $currentArraySize")
                    break
                }
                var b: Boolean
                var l: Long
                var d: Double

                gen.writeStartObject()
                for (i in columnNames.indices) {
                    gen.writeFieldName(columnNames[i])
                    when (columnTypes[i]) {
                        Types.INTEGER -> {
                            l = rs.getInt(i + 1).toLong()
                            if (rs.wasNull()) {
                                gen.writeNull()
                            } else {
                                gen.writeNumber(l)
                            }
                        }

                        Types.BIGINT -> {
                            l = rs.getLong(i + 1)
                            if (rs.wasNull()) {
                                gen.writeNull()
                            } else {
                                gen.writeNumber(l)
                            }
                        }

                        Types.DECIMAL, Types.NUMERIC -> gen.writeNumber(rs.getBigDecimal(i + 1))
                        Types.FLOAT, Types.REAL, Types.DOUBLE -> {
                            d = rs.getDouble(i + 1)
                            if (rs.wasNull()) {
                                gen.writeNull()
                            } else {
                                gen.writeNumber(d)
                            }
                        }

                        Types.NVARCHAR, Types.VARCHAR, Types.LONGNVARCHAR, Types.LONGVARCHAR -> gen.writeString(
                            rs.getString(i + 1)
                        )

                        Types.BOOLEAN, Types.BIT -> {
                            b = rs.getBoolean(i + 1)
                            if (rs.wasNull()) {
                                gen.writeNull()
                            } else {
                                gen.writeBoolean(b)
                            }
                        }

                        Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> gen.writeBinary(
                            rs.getBytes(
                                i + 1
                            )
                        )

                        Types.TINYINT, Types.SMALLINT -> {
                            l = rs.getShort(i + 1).toLong()
                            if (rs.wasNull()) {
                                gen.writeNull()
                            } else {
                                gen.writeNumber(l)
                            }
                        }

                        Types.DATE -> {
                            provider.defaultSerializeDateValue(rs.getDate(i + 1), gen)
                        }
                        Types.TIMESTAMP -> {
                            val timestamp = rs.getTimestamp(i + 1)
                            if (rs.wasNull()) {
                                gen.writeNull()
                            } else {
                                provider.defaultSerializeDateValue(timestamp, gen)
                            }
                        }

                        Types.BLOB -> {
                            val blob = rs.getBlob(i+1)
                            val byteArray = blob.getBytes(1, blob.length().toInt())
                            val base64String = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
                            Log.i("Allen",base64String);
                            gen.writeString(base64String)
//                            blob.free()
                        }

                        Types.CLOB -> {
                            val clob = rs.getString(i+1)
                            provider.defaultSerializeValue(clob, gen)
//                            clob.free()
                        }

                        Types.ARRAY -> throw RuntimeException("ResultSetSerializer not yet implemented for SQL type ARRAY")
                        Types.STRUCT -> throw RuntimeException("ResultSetSerializer not yet implemented for SQL type STRUCT")
                        Types.DISTINCT -> throw RuntimeException("ResultSetSerializer not yet implemented for SQL type DISTINCT")
                        Types.REF -> throw RuntimeException("ResultSetSerializer not yet implemented for SQL type REF")
                        Types.JAVA_OBJECT -> provider.defaultSerializeValue(
                            rs.getObject(i + 1),
                            gen
                        )

                        else -> provider.defaultSerializeValue(rs.getObject(i + 1), gen)
                    }
                }
                gen.writeEndObject()
                currentArraySize++
            }
            gen.writeEndArray()

        } catch (e: SQLException) {
            throw ResultSetSerializerException(e)
        }
    }

    override fun serialize(
        value: ResultSet?,
        gen: JsonGenerator?,
        serializers: SerializerProvider?
    ) {
        return serializer(value!!,gen!!,serializers!!)
    }
}
