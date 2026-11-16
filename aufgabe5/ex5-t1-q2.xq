(: ex5-t1-q2.xq
   Purpose: list courses that have a 5-star evaluation and where every
            evaluation is at least 3.
   Features: for, let, where, order by, return; some, every; count, avg.
   Run: java -cp /tmp/basex.jar org.basex.BaseX ex5-t1-q2.xq
   Expected: 3 course elements.
:)
<acclaimedCourses>{
  for $c in doc('ex5-t1.xml')/tuWien/courses/course
  let $evals := $c/evaluations/evaluation
  let $five := $evals[@rating = 5][1]
  where (some $e in $evals satisfies $e/@rating = 5)
    and (every $e in $evals satisfies $e/@rating >= 3)
  order by $c/title ascending
  return <course number="{string($c/@number)}"
                 title="{string($c/title)}"
                 kind="{string($c/kind)}"
                 evaluations="{count($evals)}"
                 avgRating="{round-half-to-even(avg($evals/@rating), 2)}">
           <fiveStarAuthor>{string($five/author)}</fiveStarAuthor>
           <fiveStarComment>{string($five/comment)}</fiveStarComment>
         </course>
}</acclaimedCourses>
