(: ex5-t2-q2-create.xq
   Purpose: build an index whose element names and attribute names are computed
            from course numbers and study codes.
   Features: computed element constructors, computed attribute constructors,
             dynamic element name, dynamic attribute name and value; join.
   Run: java -cp /tmp/basex.jar org.basex.BaseX ex5-t2-q2-create.xq
   Expected: 6 course_ elements, 7 curriculum_ elements, 6 moduleCount_ attributes.
:)
<courseIndex>{
  for $c in doc('ex5-t2-i1.xml')/courseCatalogue/course
  let $modules := doc('ex5-t2-i2.xml')//module[courseRef/@number = string($c/@number)]
  let $curricula := doc('ex5-t2-i2.xml')//curriculum[.//module[courseRef/@number = string($c/@number)]]
  order by $c/@number ascending
  return element {concat('course_', translate(string($c/@number), '.-', '__'))} {
    attribute {concat('moduleCount_', count($modules))} { count($modules) },
    attribute curricula { count($curricula) },
    attribute language { string($c/@language) },
    attribute ects { string($c/ects) },
    <title>{string($c/title)}</title>,
    <kind>{string($c/kind)}</kind>,
    <deliveryMode>{string($c/deliveryMode)}</deliveryMode>,
    <lecturers>{
      for $l in $c/lecturers/lecturer
      return <lecturer id="{string($l/@id)}"
                       email="{string($l/@email)}">{string($l/name)}</lecturer>
    }</lecturers>,
    <assessments>{
      for $a in $c/assessments/assessment
      return <assessment type="{string($a/@type)}"
                         weight="{string($a/@weight)}"/>
    }</assessments>,
    <usedIn>{
      for $cur in $curricula
      return element {concat('curriculum_', translate(string($cur/@studyCode), ' ', '_'))} {
        attribute degree { string($cur/@degree) },
        attribute modules { count($cur//module[courseRef/@number = string($c/@number)]) },
        <title>{string($cur/title)}</title>
      }
    }</usedIn>
  }
}</courseIndex>
