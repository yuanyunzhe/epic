package scalanlp.parser
package projections

import scalanlp.trees._;

import java.io._;

/**
 * Takes another SpanScorer.Factory, and thresholds its outputs so that any thing > threshold is 0.0, and
 * anything else is Double.NegativeInfinity
 *
 * @author dlwh
 */
class StepFunctionSpanScorerFactory[L,W](innerFactory: SpanScorer.Factory[W], threshold: Double= -7) extends SpanScorer.Factory[W] {
  def mkSpanScorer(s: scala.Seq[W], oldScorer: SpanScorer = SpanScorer.identity):SpanScorer = {
    val inner = innerFactory.mkSpanScorer(s,oldScorer);
    new StepFunctionSpanScorer(inner, threshold);
  }
}

@serializable
@SerialVersionUID(1)
final class StepFunctionSpanScorer(inner: SpanScorer, threshold: Double = -7) extends SpanScorer {
  @inline def I(score: Double) = if(score > threshold) 0.0 else Double.NegativeInfinity;

  def scoreLexical(begin: Int, end: Int, tag: Int) = I(inner.scoreLexical(begin,end,tag))

  def scoreUnaryRule(begin: Int, end: Int, parent: Int, child: Int) = I(inner.scoreUnaryRule(begin,end,parent,child));

  def scoreBinaryRule(begin: Int, split: Int, end: Int, parent: Int, leftChild: Int, rightChild: Int) = {
    I(inner.scoreBinaryRule(begin, split, end, parent, leftChild, rightChild))
  }
}

object StepFunctionifySpanScorers {
  import ProjectTreebankToLabeledSpans._;

  def main(args: Array[String]) {
    val outDir = new File(args(0));
    val inSpanDir = new File(args(1));

    val trainSpans = loadSpansFile(new File(inSpanDir,TRAIN_SPANS_NAME))
    val testSpans = loadSpansFile(new File(inSpanDir,TEST_SPANS_NAME))
    val devSpans = loadSpansFile(new File(inSpanDir,DEV_SPANS_NAME))

    outDir.mkdirs();

    writeIterable(trainSpans.map(new StepFunctionSpanScorer(_,-7.0)),new File(outDir,TRAIN_SPANS_NAME))
    writeIterable(testSpans.map(new StepFunctionSpanScorer(_,-7.0)),new File(outDir,TEST_SPANS_NAME))
    writeIterable(devSpans.map(new StepFunctionSpanScorer(_,-7.0)),new File(outDir,DEV_SPANS_NAME))
  }

}
