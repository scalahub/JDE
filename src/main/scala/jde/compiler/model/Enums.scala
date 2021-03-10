package jde.compiler.model

import jde.compiler._
import kiosk.ergo._
import jde.compiler.Multiple
import kiosk.encoding.ScalaErgoConverters.stringToGroupElement
import kiosk.script.ScriptUtil
import scorex.crypto.hash.Blake2b256

object FilterOp extends MyEnum {
  type Op = Value
  val Eq, Le, Ge, Lt, Gt, Ne = Value
  def matches(actual: scala.Long, required: scala.Long, op: FilterOp.Op) = {
    (actual, required, op) match {
      case (actual, required, Eq) if actual == required => true
      case (actual, required, Le) if actual <= required => true
      case (actual, required, Ge) if actual >= required => true
      case (actual, required, Lt) if actual < required  => true
      case (actual, required, Gt) if actual > required  => true
      case (actual, required, Ne) if actual != required => true
      case _                                            => false
    }
  }
}

object RegNum extends MyEnum {
  type Num = Value
  val R4, R5, R6, R7, R8, R9 = Value
  val valueMap = values.zipWithIndex.toMap
  def getIndex(reg: RegNum.Value): Int = valueMap(reg)
}

object BinaryOperator extends MyEnum { // input and output types are same
  type Operator = Value
  val Add, Sub, Mul, Div, Max, Min = Value

  def operate(operator: Operator, firstSecond: (KioskType[_], KioskType[_])): KioskType[_] = operate(operator, firstSecond._1, firstSecond._2)
  def operate(operator: Operator, first: KioskType[_], second: KioskType[_]): KioskType[_] = {
    (operator, first, second) match {
      case (Add, KioskLong(a), KioskLong(b))                 => KioskLong(a + b)
      case (Sub, KioskLong(a), KioskLong(b))                 => KioskLong(a - b)
      case (Mul, KioskLong(a), KioskLong(b))                 => KioskLong(a * b)
      case (Div, KioskLong(a), KioskLong(b))                 => KioskLong(a / b)
      case (Max, KioskLong(a), KioskLong(b))                 => KioskLong(a max b)
      case (Min, KioskLong(a), KioskLong(b))                 => KioskLong(a min b)
      case (Add, KioskInt(a), KioskInt(b))                   => KioskInt(a + b)
      case (Sub, KioskInt(a), KioskInt(b))                   => KioskInt(a - b)
      case (Mul, KioskInt(a), KioskInt(b))                   => KioskInt(a * b)
      case (Div, KioskInt(a), KioskInt(b))                   => KioskInt(a / b)
      case (Max, KioskInt(a), KioskInt(b))                   => KioskInt(a max b)
      case (Min, KioskInt(a), KioskInt(b))                   => KioskInt(a min b)
      case (Add, KioskGroupElement(g), KioskGroupElement(h)) => KioskGroupElement(g.multiply(h))
      case (Sub, KioskGroupElement(g), KioskGroupElement(h)) => KioskGroupElement(g.multiply(h.negate))
      case (op, someFirst, someSecond)                       => throw new Exception(s"Invalid operation $op for ${someFirst.typeName}, ${someSecond.typeName}")
    }
  }
}

object UnaryOperator extends MyEnum { // input and output types are same
  type Operator = Value
  val Hash, Neg, Abs, Sum, Avg, Min, Max, ProveDlog, ToCollByte, ToLong, ToInt, ToAddress, ToErgoTree, Count, ToGroupElement = Value
  private val aggregates: Set[Operator] = Set(Sum, Avg, Min, Max)
  private val converters: Set[Operator] = Set(ProveDlog, ToCollByte, ToLong, ToInt, ToAddress, ToErgoTree, Count, ToGroupElement)

  def operate(operator: Operator, values: Multiple[KioskType[_]], `type`: DataType.Type): Multiple[KioskType[_]] = {
    operator match {
      case op if aggregates.contains(op) => aggregate(operator, values, `type`)
      case op if converters.contains(op) => convert(operator, values)
      case _                             => values.map(operateSingle(operator, _))
    }
  }

  private def convert(converter: Operator, fromValues: Multiple[KioskType[_]]): Multiple[KioskType[_]] = {
    converter match {
      case Count => Multiple(KioskInt(fromValues.seq.size))
      case _     => fromValues.map(convertSingle(converter, _))
    }
  }

  private def toGroupElement(kioskErgoTree: KioskErgoTree) = {
    val ergoTreeHex = kioskErgoTree.hex
    if (ergoTreeHex.size != 72) throw new Exception("PoveDlog ErgoTree should be exactly 72 chars")
    if (ergoTreeHex.take(6) != "0008cd") throw new Exception("Invalid address prefix for proveDlog")
    KioskGroupElement(stringToGroupElement(ergoTreeHex.drop(6)))
  }

  private def convertSingle(converter: Operator, fromValue: KioskType[_]): KioskType[_] = {
    converter match {
      case ProveDlog      => KioskErgoTree(ScriptUtil.compile(Map("g" -> fromValue.asInstanceOf[KioskGroupElement]), "proveDlog(g)"))
      case ToGroupElement => toGroupElement(fromValue.asInstanceOf[KioskErgoTree])
      case ToCollByte     => KioskCollByte(fromValue.asInstanceOf[KioskErgoTree].serialize)
      case ToLong         => KioskLong(fromValue.asInstanceOf[KioskInt].value.toLong)
      case ToInt          => KioskInt(fromValue.asInstanceOf[KioskLong].value.toInt)
      case ToAddress      => fromValue.ensuring(_.isInstanceOf[KioskErgoTree])
      case ToErgoTree     => fromValue.ensuring(_.isInstanceOf[KioskErgoTree])
    }
  }

  def getFromTo(operator: Operator): FromTo = {
    operator match {
      case ProveDlog      => FromTo(from = DataType.GroupElement, to = DataType.ErgoTree)
      case ToCollByte     => FromTo(from = DataType.ErgoTree, to = DataType.CollByte)
      case ToLong         => FromTo(from = DataType.Int, to = DataType.Long)
      case ToInt          => FromTo(from = DataType.Long, to = DataType.Int)
      case ToAddress      => FromTo(from = DataType.ErgoTree, to = DataType.Address)
      case ToErgoTree     => FromTo(from = DataType.Address, to = DataType.ErgoTree)
      case ToGroupElement => FromTo(from = DataType.Address, to = DataType.GroupElement)
      case Count          => FromTo(from = DataType.Unknown, to = DataType.Int)
      case _              => FromTo(from = DataType.Unknown, to = DataType.Unknown)
    }
  }

  private def operateSingle(operator: Operator, in: KioskType[_]): KioskType[_] = {
    (operator, in) match {
      case (Hash, KioskCollByte(a))    => KioskCollByte(Blake2b256(a))
      case (Neg, KioskGroupElement(g)) => KioskGroupElement(g.negate)
      case (Abs, KioskLong(a))         => KioskLong(a.abs)
      case (Neg, KioskLong(a))         => KioskLong(-a)
      case (Abs, KioskInt(a))          => KioskInt(a.abs)
      case (Neg, KioskInt(a))          => KioskInt(-a)
      case (op, someIn)                => throw new Exception(s"Invalid operation $op for ${someIn.typeName}")
    }
  }

  private def aggregate(aggregate: Operator, values: Multiple[KioskType[_]], `type`: DataType.Type): Multiple[KioskType[_]] = {
    def requiringNonEmpty(f: => Multiple[KioskType[_]]): Multiple[KioskType[_]] = {
      if (values.isEmpty) throw new Exception(s"Empty sequence found when evaluating aggregate $aggregate for type ${`type`}") else f
    }
    (`type`, aggregate) match {
      case (DataType.Int, Sum)          => Multiple(KioskInt(to[KioskInt](values).seq.map(_.value).sum))
      case (DataType.Int, Avg)          => requiringNonEmpty(Multiple(KioskInt(to[KioskInt](values).seq.map(_.value).sum / values.length)))
      case (DataType.Int, Min)          => requiringNonEmpty(Multiple(KioskInt(to[KioskInt](values).seq.map(_.value).min)))
      case (DataType.Int, Max)          => requiringNonEmpty(Multiple(KioskInt(to[KioskInt](values).seq.map(_.value).max)))
      case (DataType.Long, Sum)         => Multiple(KioskLong(to[KioskLong](values).seq.map(_.value).sum))
      case (DataType.Long, Avg)         => requiringNonEmpty(Multiple(KioskLong(to[KioskLong](values).seq.map(_.value).sum / values.length)))
      case (DataType.Long, Min)         => requiringNonEmpty(Multiple(KioskLong(to[KioskLong](values).seq.map(_.value).min)))
      case (DataType.Long, Max)         => requiringNonEmpty(Multiple(KioskLong(to[KioskLong](values).seq.map(_.value).max)))
      case (DataType.GroupElement, Sum) => Multiple(to[KioskGroupElement](values).seq.foldLeft(PointAtInfinity)(_ + _))
      case _                            => throw new Exception(s"Invalid aggregate $aggregate for data type ${`type`}")
    }
  }
}

case class FromTo(from: DataType.Type, to: DataType.Type)

object MatchingOptions extends MyEnum {
  type Options = Value
  val Strict, Multi, Optional = Value
}

object InputType extends MyEnum {
  type Type = Value
  val Aux, Data, Code = Value
}
