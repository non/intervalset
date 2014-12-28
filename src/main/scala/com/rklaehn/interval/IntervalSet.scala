package com.rklaehn.interval

import language.implicitConversions
import spire.algebra.{AdditiveMonoid, Order}
import spire.math.interval._
import spire.math._

import scala.annotation.tailrec
import scala.collection.AbstractTraversable

sealed abstract class IntervalSet[T] extends AbstractIntervalSet[T, IntervalSet[T]]

object IntervalSet {

  trait Element[@specialized(Float, Int, Long, Double) T] {

    implicit def ops:Order[T] with AdditiveMonoid[T]

    def toLong(value:T) : Long

    def fromLong(key:Long) : T
  }

  implicit object ByteElement extends Element[Byte] {

    def ops = spire.std.byte.ByteAlgebra

    def toLong(value:Byte) = value

    def fromLong(key:Long): Byte = key.toByte
  }

  implicit object ShortElement extends Element[Short] {

    def ops = spire.std.short.ShortAlgebra

    def toLong(value:Short) = value

    def fromLong(key:Long): Short = key.toShort
  }

  implicit object IntElement extends Element[Int] {

    def ops = spire.std.int.IntAlgebra

    def toLong(value:Int) = value

    def fromLong(key:Long) : Int = key.toInt
  }

  implicit object LongElement extends Element[Long] {

    def ops = spire.std.long.LongAlgebra

    def toLong(value:Long) = value

    def fromLong(key:Long) : Long = key
  }

  implicit object FloatElement extends Element[Float] {

    def ops = spire.std.float.FloatAlgebra

    def toLong(value:Float): Long = {
      if(value.isNaN)
        throw new IllegalArgumentException("NaN")
      // sign and magnitude signed integer
      val signAndMagnitude = java.lang.Float.floatToIntBits(value)
      // two's complement signed integer: if the sign bit is set, negate everything except the sign bit
      val twosComplement = if(signAndMagnitude>=0) signAndMagnitude else (-signAndMagnitude | (1L<<63))
      twosComplement
    }

    def fromLong(twosComplement:Long): Float = {
      // sign and magnitude signed integer: if the sign bit is set, negate everything except the sign bit
      val signAndMagnitude = if(twosComplement>=0) twosComplement else (-twosComplement | (1L<<63))
      // double from sign and magnitude signed integer
      java.lang.Float.intBitsToFloat(signAndMagnitude.toInt)
    }
  }

  implicit object CharElement extends Element[Char] {

    val ops: Order[Char] with AdditiveMonoid[Char] = new spire.std.CharAlgebra with AdditiveMonoid[Char] {

      def zero: Char = 0.toChar

      def plus(x: Char, y: Char): Char = (x.toInt + y.toInt).toChar
    }

    def toLong(value:Char) = value.toLong

    def fromLong(key:Long): Char = key.toChar
  }

  implicit object DoubleElement extends Element[Double] {

    def ops = spire.std.double.DoubleAlgebra

    def toLong(value:Double): Long = {
      if(value.isNaN)
        throw new IllegalArgumentException("NaN")
      // sign and magnitude signed integer
      val signAndMagnitude = java.lang.Double.doubleToLongBits(value)
      // two's complement signed integer: if the sign bit is set, negate everything except the sign bit
      val twosComplement = if(signAndMagnitude>=0) signAndMagnitude else (-signAndMagnitude | (1L<<63))
      twosComplement
    }

    def fromLong(twosComplement:Long): Double = {
      // sign and magnitude signed integer: if the sign bit is set, negate everything except the sign bit
      val signAndMagnitude = if(twosComplement>=0) twosComplement else (-twosComplement | (1L<<63))
      // double from sign and magnitude signed integer
      java.lang.Double.longBitsToDouble(signAndMagnitude)
    }
  }

  implicit object UByteElement extends Element[UByte] {

    def ops = spire.math.UByte.UByteAlgebra

    def toLong(value:UByte) = value.toLong

    def fromLong(key:Long) : UByte = UByte(key.toByte)
  }

  implicit object UShortElement extends Element[UShort] {

    def ops = spire.math.UShort.UShortAlgebra

    def toLong(value:UShort) = value.toLong

    def fromLong(key:Long) : UShort = UShort(key.toShort)
  }

  implicit object UIntElement extends Element[UInt] {

    def ops = spire.math.UInt.UIntAlgebra

    def toLong(value:UInt) = value.toLong

    def fromLong(key:Long) : UInt = UInt(key.toInt)
  }

  implicit object ULongElement extends Element[ULong] {

    def ops = spire.math.ULong.ULongAlgebra

    def toLong(value:ULong) = value.toLong + Long.MinValue

    def fromLong(key:Long) : ULong = ULong(key - Long.MinValue)
  }

  import IntervalTrie._

  private implicit def tIsLong[T](value:T)(implicit tl:Element[T]) = tl.toLong(value)

  private[interval] def fromKind[T:Element](value:T, kind:Int) = {
    val bound = kind match {
      case 0 => Below(value)
      case 1 => Above(value)
      case 2 => Both(value)
    }
    IntervalSet[T](false, bound)
  }

  def constant[T:Element](value:Boolean) = IntervalSet[T](value, null)

  def zero[T:Element] = constant[T](false)

  def point[T:Element](value:T) = IntervalSet[T](false, IntervalTrie.Leaf(toPrefix(value), true, false))

  def atOrAbove[T:Element](value:T) = IntervalSet[T](false, IntervalTrie.Leaf(toPrefix(value), true, true))

  def above[T:Element](value:T) = IntervalSet[T](false, IntervalTrie.Leaf(toPrefix(value), false, true))

  def one[T:Element] = constant[T](true)

  def hole[T:Element](value:T) = IntervalSet[T](true, IntervalTrie.Leaf(toPrefix(value), true, false))

  def below[T:Element](value:T) = IntervalSet[T](true, IntervalTrie.Leaf(toPrefix(value), true, true))

  def atOrBelow[T:Element](value:T) = IntervalSet[T](true, IntervalTrie.Leaf(toPrefix(value), false, true))

  def apply[T:Element](interval:Interval[T]) : IntervalSet[T] = interval.fold {
    case (Closed(a),    Closed(b)) if a == b => point(a)
    case (Unbound(),    Open(x))      => below(x)
    case (Unbound(),    Closed(x))    => atOrBelow(x)
    case (Open(x),      Unbound())    => above(x)
    case (Closed(x),    Unbound())    => atOrAbove(x)
    case (Closed(a),    Closed(b))    => fromTo(Below(a), Above(b))
    case (Closed(a),    Open(b))      => fromTo(Below(a), Below(b))
    case (Open(a),      Closed(b))    => fromTo(Above(a), Above(b))
    case (Open(a),      Open(b))      => fromTo(Above(a), Below(b))
    case (Unbound(),    Unbound())    => one[T]
    case (EmptyBound(), EmptyBound()) => zero[T]
  }

  private object Below {

    def apply[T: Element](value:T) = Leaf(toPrefix(value), true, true)

    def unapply(l:Leaf) = if(l.at && l.sign) Some(l.key) else None
  }

  private object Above {

    def apply[T: Element](value:T) = Leaf(toPrefix(value), false, true)

    def unapply(l:Leaf) = if(!l.at && l.sign) Some(l.key) else None
  }

  private object Both {

    def apply[T: Element](value:T) = Leaf(toPrefix(value), true, false)

    def unapply(l:Leaf) = if(l.at && !l.sign) Some(l.key) else None
  }

  private def fromTo[T:Element](a:Leaf, b:Leaf) : IntervalSet[T] = {
    IntervalSet[T](false, concat(a, b))
  }

  def apply(text:String) : IntervalSet[Long] = {
    val la = spire.std.long.LongAlgebra
    def rationalToLong(r:Rational) : Long = {
      if(r>Long.MaxValue || r<Long.MinValue)
        throw new NumberFormatException("Integer number too large")
      else
        r.toLong
    }
    def intervalToIntervalSet(i:Interval[Long]) : IntervalSet[Long] = apply(i)
    val intervals = text.split(';').map(Interval.apply).map(_.mapBounds(rationalToLong)(la))
    val simpleSets = intervals.map(intervalToIntervalSet)
    (zero[Long] /: simpleSets)(_ | _)
  }

  private final def foreachInterval[T:Element, U](a0:Boolean, a:IntervalTrie)(f:Interval[T] => U): Unit = {
    val x = implicitly[Element[T]]
    import x._
    def op(b0:Bound[T], a0:Boolean, a:IntervalTrie): Bound[T] = a match {
      case Below(a) =>
        if(a0)
          f(Interval.fromBounds(b0, Open(fromLong(a))))
        Closed(fromLong(a))
      case Above(a) =>
        if(a0)
          f(Interval.fromBounds(b0, Closed(fromLong(a))))
        Open(fromLong(a))
      case Both(a) =>
        if(a0)
          f(Interval.fromBounds(b0, Open(fromLong(a))))
        else
          f(Interval.point(fromLong(a)))
        Open(fromLong(a))
      case a:Branch =>
        val am = a0 ^ a.left.sign
        val bm = op(b0, a0, a.left)
        val b1 = op(bm, am, a.right)
        b1
      case _ =>
        Unbound()
    }
    val last = op(Unbound(), a0, a)
    if(a0 ^ ((a ne null) && a.sign))
      f(Interval.fromBounds(last, Unbound()))
  }

  private def apply[T:Element](below:Boolean, tree:IntervalTrie): IntervalSet[T] =
    TreeBasedIntervalSet(below, tree, implicitly[Element[T]])

  private final case class TreeBasedIntervalSet[T](belowAll:Boolean, tree:IntervalTrie, implicit val ise:Element[T]) extends IntervalSet[T] { lhs =>

    import IntervalTrie._

    def aboveAll: Boolean = if(tree eq null) belowAll else belowAll ^ tree.sign

    def isEmpty = !belowAll && (tree eq null)

    def isContiguous = if(belowAll) {
      tree match {
        case a:Leaf => a.sign
        case null => true
        case _ => false
      }
    } else {
      tree match {
        case _:Leaf => true
        case Branch(_,_,a:Leaf, b:Leaf) => a.sign & b.sign
        case null => true
        case _ => false
      }
    }

    def hull: Interval[T] = {
      implicit val ops = ise.ops
      @tailrec
      def lowerBound(a:IntervalTrie) : Bound[T] = a match {
        case a:Branch => lowerBound(a.left)
        case Above(x) => Open(ise.fromLong(x))
        case Below(x) => Closed(ise.fromLong(x))
        case Both(x) => Closed(ise.fromLong(x))
      }
      @tailrec
      def upperBound(a:IntervalTrie) : Bound[T] = a match {
        case a:Branch => upperBound(a.right)
        case Both(x) => Closed(ise.fromLong(x))
        case Above(x) => Closed(ise.fromLong(x))
        case Below(x) => Open(ise.fromLong(x))
      }
      if(isEmpty) {
        Interval.empty[T]
      } else {
        val lower = if(belowAll) Unbound[T] else lowerBound(tree)
        val upper = if(aboveAll) Unbound[T] else upperBound(tree)
        Interval.fromBounds(lower, upper)
      }
    }

    def below(value:T) : Boolean = SampleBelow(belowAll, tree, toPrefix(ise.toLong(value)))

    def at(value:T) : Boolean = SampleAt(belowAll, tree, toPrefix(ise.toLong(value)))

    def above(value:T) : Boolean = SampleAbove(belowAll, tree, toPrefix(ise.toLong(value)))

    def apply(value:T) : Boolean = at(value)

    def & (rhs:IntervalSet[T]) = rhs match {
      case rhs:TreeBasedIntervalSet[T] =>
        IntervalSet[T](lhs.belowAll & rhs.belowAll, AndCalculator(lhs.belowAll, lhs.tree, rhs.belowAll, rhs.tree))
    }

    def | (rhs:IntervalSet[T]) = rhs match {
      case rhs: TreeBasedIntervalSet[T] =>
        IntervalSet[T](lhs.belowAll | rhs.belowAll, OrCalculator(lhs.belowAll, lhs.tree, rhs.belowAll, rhs.tree))
    }

    def ^ (rhs:IntervalSet[T]) = rhs match {
      case rhs: TreeBasedIntervalSet[T] => IntervalSet[T](lhs.belowAll ^ rhs.belowAll, XorCalculator(lhs.belowAll, lhs.tree, rhs.belowAll, rhs.tree))
    }

    def unary_~ = IntervalSet[T](!belowAll, tree)

    def isSupersetOf(rhs:IntervalSet[T]) = rhs match {
      case rhs:TreeBasedIntervalSet[T] =>
        SupersetOfCalculator(lhs.belowAll, lhs.tree, rhs.belowAll, rhs.tree)
    }

    def intersects(rhs:IntervalSet[T]) = rhs match {
      case rhs:TreeBasedIntervalSet[T] =>
        !DisjointCalculator(lhs.belowAll, lhs.tree, rhs.belowAll, rhs.tree)
    }

    def isProperSupersetOf(rhs:IntervalSet[T]) = isSupersetOf(rhs) && (rhs != lhs)

    def intervals = new AbstractTraversable[Interval[T]] {
      override def foreach[U](f: Interval[T] => U): Unit = foreachInterval(belowAll, tree)(f)
    }

    def edges = new AbstractTraversable[T] {
      override def foreach[U](f: T => U): Unit = IntervalTrie.foreachEdge(tree)(key => f(ise.fromLong(key)))
    }

    override def toString = {
      import ise.ops
      if (isEmpty)
        Interval.empty[T].toString
      else
        intervals.map(_.toString).mkString(";")
    }
  }

}
