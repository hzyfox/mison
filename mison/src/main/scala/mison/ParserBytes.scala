package mison

import Macros._

object ParserBytes {

  //路径，待实现，以实现访问多层
  def parse(json: String, query: Seq[String], index: Index, stringMask: Index.Bitmap, level1Positions: java.util.List[Int],
            len: Int, fields: Map[String, Int], jsonBytes: Array[Byte], encoding: String = "utf-8"): Map[String, String] = {
    //val jsonBytes = json.getBytes(encoding)
//    val index = Index(jsonBytes, 1)
//    val stringMask = index.bitmaps(0)
//    val level1Positions = index.getColonPositions(0, json.length, 1)
//    val len = level1Positions.size
//    val fields = query.zipWithIndex.toMap

    //start end 都是index 从0开始, start是左引号index+1的位置 end是右"的下标 所以 end-start为字符串长度，取字符串可以过滤掉引号
    @inline def fieldNameBounds(colonPosition: Int, stringMask: Index.Bitmap): (Int, Int) = {
      var (start, end) = (-1, -1)
      forloop((colonPosition + 63) / 64 - 1, _ >= 0, _ - 1) { i =>
        var (s, e) = (-1, -1)
        var mask = ~stringMask(i)
        loop {
          val bits = E(mask)
          val offset = (i * 64) + trailingZeros(bits)
          if (offset < colonPosition) {
            if (offset == e + 1) {
              e = offset
            }
            else {
              s = offset
              e = offset
            }
            mask = R(mask)
            if (mask == 0) break
          }
          else {
            break
          }
        }
        if (e == start - 1) {
          start = s
        }
        else {
          start = s
          end = e
        }
        if (start % 64 != 0) {
          break
        }
      }
      (start, end)
    }

    @inline def fieldName(colonPosition: Int, jsonBytes: Array[Byte], stringMask: Index.Bitmap): String = {
      val (start, end) = fieldNameBounds(colonPosition, stringMask)
      new String(jsonBytes, start, end - start, encoding)
    }

    //from是上一个引号index+1， to是下一个引号index，用于调用filedNameBound往前取出下一个key以便求上一个value的end
    @inline def extractValue(from: Int, to: Int, jsonBytes: Array[Byte], stringMask: Index.Bitmap): String = {
      var (start, end) = (-1, -1)
      val to0 = if (to == -1) json.length - 1 else fieldNameBounds(to, stringMask)._1 - 2
      forloop(from, _ < to0, _ + 1) { i =>
        val c = json(i)
        if (c != ' ') {
          start = i
          break
        }
      }
      //fix bug: if to0 = start + 1, and  jsonBytes(to0)==',', end will be -1, then end -start <0, OutOfBoundsError!
      end = to0
      forloop(to0, _ > start, _ - 1) { i =>
        val c = jsonBytes(i).toChar
        if (c != ' ' && c != ',' && c != '}') {
          end = i + 1
          break
        }
      }
      //start 是左引号的位置, end是value最后一位index+1的位置（value包括引号）
      new String(jsonBytes, start, end - start, encoding)
    }

    @inline def findResult(len: Int, fields: Map[String, Int], level1Positions: java.util.List[Int], jsonBytes: Array[Byte], stringMask: Index.Bitmap): Map[String, String] = {
      val result = collection.mutable.HashMap.empty[String, String]
      forloop(0, _ < len, _ + 1) { i =>
        val position = level1Positions.get(i)
        val field = fieldName(position, jsonBytes, stringMask)
        if (fields.contains(field)) {
          val value = extractValue(position + 1, if ((i + 1) < level1Positions.size) level1Positions.get(i + 1) else -1, jsonBytes, stringMask)
          result += (field -> value)
        }
      }

      result.toMap
    }

    findResult(len, fields, level1Positions, jsonBytes, stringMask)
  }

}
