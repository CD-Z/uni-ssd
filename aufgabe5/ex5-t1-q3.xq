(: ex5-t1-q3.xq
   Purpose: aggregate modules and ECTS per curriculum.
   Features: for, let, order by, return; count, sum.
   Run: java -cp /tmp/basex.jar org.basex.BaseX ex5-t1-q3.xq
   Expected: 2 curriculum elements, Informatik first.
:)
<curriculumStats>{
  for $c in doc('ex5-t1.xml')/tuWien/curricula/curriculum
  let $modules := $c//module
  let $subjects := $c/examinationSubject
  let $moduleEcts := sum($modules/ects)
  order by $moduleEcts descending, $c/title ascending
  return <curriculum studyCode="{string($c/@studyCode)}"
                     degree="{string($c/@degree)}"
                     validFrom="{string($c/@validFrom)}"
                     title="{string($c/title)}"
                     durationSemesters="{string($c/durationSemesters)}"
                     declaredEcts="{string($c/totalEcts)}"
                     subjects="{count($subjects)}"
                     modules="{count($modules)}"
                     moduleEcts="{$moduleEcts}"/>
}</curriculumStats>
