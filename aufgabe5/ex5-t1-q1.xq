(: ex5-t1-q1.xq
   Purpose: summarise course evaluations, keep courses with at least two
            evaluations, and sort by average rating.
   Features: for, let, where, order by, return; count, avg, max.
   Run: java -cp /tmp/basex.jar org.basex.BaseX ex5-t1-q1.xq
   Expected: 6 courseSummary elements, highest average first.
:)
<courseSummaries>{
  for $c in doc('ex5-t1.xml')/tuWien/courses/course
  let $evals := $c/evaluations/evaluation
  let $avg := avg($evals/@rating)
  let $best := $evals[@rating = max($evals/@rating)][1]
  where count($evals) >= 2
  order by $avg descending, $c/title ascending
  return <courseSummary number="{string($c/@number)}"
                        ects="{string($c/@ects)}"
                        title="{string($c/title)}"
                        kind="{string($c/kind)}"
                        evaluations="{count($evals)}"
                        avgRating="{round-half-to-even($avg, 2)}">
           <topAuthor>{string($best/author)}</topAuthor>
           <topYear>{string($best/@year)}</topYear>
           <topComment>{string($best/comment)}</topComment>
         </courseSummary>
}</courseSummaries>
