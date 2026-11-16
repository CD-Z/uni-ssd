(: ex5-t2-q1-multi.xq
   Purpose: join every curriculum module to its catalogue course through
            courseRef/@number and enrich the module with course data.
   Features: nested for, let, where, order by, return; join across two documents.
   Run: java -cp /tmp/basex.jar org.basex.BaseX ex5-t2-q1-multi.xq
   Expected: 7 module elements, ordered by study code, subject, category.
:)
<curriculumModules>{
  for $c in doc('ex5-t2-i2.xml')/curricula/curriculum
  let $curriculumTitle := string($c/title)
  for $s in $c/examinationSubject
  for $m in $s/module
  let $course := doc('ex5-t2-i1.xml')/courseCatalogue/course[@number = string($m/courseRef/@number)]
  where exists($course)
  order by $c/@studyCode, $s/@id, $m/@category
  return <module studyCode="{string($c/@studyCode)}"
                  degree="{string($c/@degree)}"
                  validFrom="{string($c/@validFrom)}"
                  curriculum="{$curriculumTitle}"
                  curriculumEcts="{string($c/totalEcts)}"
                  subject="{string($s/title)}"
                  category="{string($m/@category)}"
                  moduleTitle="{string($m/title)}"
                  moduleEcts="{string($m/ects)}"
                  courseNumber="{string($course/@number)}"
                  courseTitle="{string($course/title)}"
                  kind="{string($course/kind)}"
                  weeklyHours="{string($course/weeklyHours)}"
                  courseEcts="{string($course/ects)}"
                  deliveryMode="{string($course/deliveryMode)}"
                  language="{string($course/@language)}">
           <lecturers>{
             for $l in $course/lecturers/lecturer
             return <lecturer id="{string($l/@id)}"
                              name="{string($l/name)}"
                              email="{string($l/email)}"/>
           }</lecturers>
         </module>
}</curriculumModules>
