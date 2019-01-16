package mison

import java.util.concurrent._

import com.alibaba.fastjson.JSON
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

case class UrlCreated(html_url: String = "", created_at: String = "")

object UrlCreated {
  implicit val codec: JsonValueCodec[UrlCreated] = make(CodecMakerConfig())
  val fieldNames: Seq[String] = Seq("html_url", "created_at","body")
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = Array(
  "-server",
  "-Xms2g",
  "-Xmx2g",
  "-XX:NewSize=1g",
  "-XX:MaxNewSize=1g",
  "-XX:InitialCodeCacheSize=512m",
  "-XX:ReservedCodeCacheSize=512m",
  "-XX:+UseParallelGC",
  "-XX:-UseBiasedLocking",
  "-XX:+AlwaysPreTouch"
))
class Benchmarks {
  private[this] val jacksonMapper: ObjectMapper = new ObjectMapper

  @Param(Array("empty", "short", "medium", "long", "huge", "giant"))
  var entry: String = _
  var json: String = _
  var jsonBytes: Array[Byte] = _
  var index: Index = _
  var stringMask: Index.Bitmap = _
  var level1Positions: java.util.List[Int] = _
  var len: Int = _
  var fields: Map[String, Int] = _

  @Setup
  def prepare(): Unit = {
    Index
    json = Main.getJson(entry)
    jsonBytes = json.getBytes("UTF-8")
    index = Index(jsonBytes, 1)
    stringMask = index.bitmaps(0)
    level1Positions = index.getColonPositions(0, json.length, 1)
    len = level1Positions.size
    fields = UrlCreated.fieldNames.zipWithIndex.toMap
  }


  @Benchmark
  def misonJsonBytes(bh: Blackhole): Unit = {
    val bytes = json.getBytes("UTF-8")
    bh.consume(bytes)
  }

  @Benchmark
  def misonBuildStructure(bh: Blackhole): Unit = {
    val index0 = Index(jsonBytes, 1)
    val levelPositions0 = index0.getColonPositions(0, json.length, 1)
    val fields = UrlCreated.fieldNames.zipWithIndex.toMap
    bh.consume(levelPositions0)
    bh.consume(fields)
  }

  @Benchmark
  def misonFindResult(bh: Blackhole): Unit = {
    val value = ParserBytes.parse(json, UrlCreated.fieldNames, index, stringMask, level1Positions, len, fields, jsonBytes)
    bh.consume(value.get("html_url"))
    bh.consume(value.get("created_at"))
    bh.consume(value.get("body"))
  }

  @Benchmark
  def mison(bh: Blackhole): Unit = {
    val value = Parser.parse(json, UrlCreated.fieldNames)
    bh.consume(value.get("html_url"))
    bh.consume(value.get("created_at"))
    bh.consume(value.get("body"))
  }

  //  @Benchmark
  //  def misonAvoidGettingBytes(bh: Blackhole): Unit = {
  //    val value = ParserBytes.parse(json, UrlCreated.fieldNames, jsonBytes)
  //    bh.consume(value.get("html_url"))
  //    bh.consume(value.get("created_at"))
  //  }

  @Benchmark
  def fastJson(bh: Blackhole): Unit = {
    val value = JSON.parseObject(json)
    bh.consume(value.get("html_url"))
    bh.consume(value.get("created_at"))
    bh.consume(value.get("body"))
  }

  @Benchmark
  def jackson(bh: Blackhole): Unit = {
    val value = jacksonMapper.readTree(json)
    bh.consume(value.path("html_url").asText)
    bh.consume(value.path("created_at").asText)
    bh.consume(value.get("body"))
  }

  //  @Benchmark
  //  def jsoniterScala(bh: Blackhole): Unit = {
  //    val value = readFromArray[UrlCreated](jsonBytes)
  //    bh.consume(value.html_url)
  //    bh.consume(value.created_at)
  //  }
}
