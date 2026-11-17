# Bericht zu Übung 5: XQuery

## 1. Ausgangslage und Szenario

Die Übung 5 besteht aus zwei Teilen. In Aufgabe 1 sollte ein nicht-triviales XML-Dokument aus dem eigenen Szenario mit XQuery abgefragt werden. In Aufgabe 2 sollten zwei XML-Dokumente miteinander verknüpft und mit XQuery neu strukturiert werden. Mein Szenario ist „TU Wien Courses and Curricula“, das bereits in den Aufgaben 3 und 4 verwendet wurde. Ich habe die Daten für Aufgabe 1 um Bewertungen erweitert und die Beispiele bewusst klein gehalten. Die Lösung liegt im Verzeichnis `aufgabe5/`.

## 2. Aufgabe 1: Abfragen eines Dokuments

### 2.1 Datenmodell

`ex5-t1.xml` beschreibt einen Ausschnitt des TU-Wien-Lehrangebots. Die Wurzel `tuWien` enthält `courses` und `curricula`. Jeder `course` hat die Attribute `number` und `ects` sowie die Kindelemente `title`, `kind` und `evaluations`. Eine `evaluation` hat `rating`, `year`, `author` und `comment`. Die `curricula` enthalten `curriculum`-Elemente mit `studyCode`, `degree`, `validFrom`, `examinationSubject` und `module`. Ein `module` verweist über `courseRef/@number` auf einen Kurs.

Das Dokument hat 94 Elemente und Attribute. Die tiefste Stelle ist `tuWien/courses/course/evaluations/evaluation/author`; `author` hat fünf Vorfahren, das Dokument erreicht also Tiefe 6 und erfüllt die geforderte Tiefe 5. Eine Zählung der Eingabeknoten, deren Wert in mindestens einer Ausgabe vorkommt, ergab 50 Knoten. Das sind mehr als die geforderten 20.

### 2.2 Abfrage 1: Bewertungsübersicht

`ex5-t1-q1.xq` berechnet pro Kurs die Anzahl der Bewertungen und den Durchschnitt der Ratings. Kurse mit weniger als zwei Bewertungen werden mit `where` ausgefiltert. Das Ergebnis wird mit `order by $avg descending, $c/title ascending` sortiert. Für jede Zeile wird ein neues `courseSummary`-Element mit Attributen für Kursnummer, ECTS, Titel, Art, Anzahl und Durchschnitt erzeugt. Zusätzlich werden die beste Bewertung mit Autor, Jahr und Kommentar als Kindelemente ausgegeben.

Die Abfrage verwendet alle fünf FLWOR-Klauseln: `for`, `let`, `where`, `order by` und `return`. Als Aggregationen kommen `count`, `avg` und `max` zum Einsatz. Das Ergebnis sind drei `courseSummary`-Elemente, angeführt von „Verteilte Systeme“ mit Durchschnitt 5.

### 2.3 Abfrage 2: Quantifizierte Ausdrücke

`ex5-t1-q2.xq` sucht Kurse, die mindestens eine 5-Sterne-Bewertung haben und bei denen jede Bewertung mindestens 3 Sterne hat. Dafür werden zwei quantifizierte Ausdrücke kombiniert:

```xquery
where (some $e in $evals satisfies $e/@rating = 5)
  and (every $e in $evals satisfies $e/@rating >= 3)
```

`some` ist wahr, wenn mindestens ein Element die Bedingung erfüllt. `every` ist wahr, wenn alle Elemente die Bedingung erfüllen. Bei einer leeren Sequenz ist `some` falsch und `every` wahr; dieser Fall der leeren Allquantifizierung ist ein wichtiger Unterschied zu einer naiven Schleife. Das Ergebnis sind zwei Kurse, sortiert nach Titel.

### 2.4 Abfrage 3: Aggregation pro Curriculum

`ex5-t1-q3.xq` gruppiert nicht über `group by`, sondern iteriert über die `curriculum`-Knoten. Für jedes Curriculum werden die Module und Prüfungsfächer mit `let` gebunden, die ECTS-Summe mit `sum` berechnet und die Anzahl mit `count` ermittelt. Die Ausgabe wird nach `moduleEcts` absteigend sortiert. Das Ergebnis ist „Informatik“ mit 2 Fächern, 2 Modulen und 12 ECTS vor „Wirtschaftsinformatik“ mit 1 Fach, 1 Modul und 6 ECTS.

### 2.5 Feature-Matrix

| Feature | q1 | q2 | q3 |
|---|---|---|---|
| `for` | ja | ja | ja |
| `let` | ja | ja | ja |
| `where` | ja | ja | nein |
| `order by` | ja | ja | ja |
| `return` | ja | ja | ja |
| Aggregation | count/avg/max | count/avg | count/sum |
| Quantifizierung | nein | some/every | nein |

## 3. Aufgabe 2: Zwei Dokumente und Konstruktoren

### 3.1 Eingabedokumente

`ex5-t2-i1.xml` ist ein Kurskatalog. Jeder Kurs enthält Titel, Art, Wochenstunden, ECTS, Abhaltungsmodus, Vortragende mit ID, Name und E-Mail sowie Prüfungsleistungen mit Typ und Gewicht. Das Dokument hat 42 Elemente und Attribute und erreicht Tiefe 5.

`ex5-t2-i2.xml` beschreibt die Curricula. Jedes `curriculum` enthält `studyCode`, `degree`, `validFrom`, Titel, Semesterdauer, ECTS und `examinationSubject`-Elemente. Darunter liegen `module` mit `category`, `title`, `ects` und `courseRef/@number`. Das Dokument hat 33 Elemente und Attribute und erreicht Tiefe 5. Die Verweise `courseRef/@number` verbinden beide Dokumente.

### 3.2 Join-Abfrage

`ex5-t2-q1-multi.xq` verbindet die Module aus `ex5-t2-i2.xml` mit den Kursen aus `ex5-t2-i1.xml`:

```xquery
let $course := doc('ex5-t2-i1.xml')/courseCatalogue/course
               [@number = string($m/courseRef/@number)]
where exists($course)
```

Das ist ein Gleichheits-Join über `courseRef/@number` und `course/@number`. Die verschachtelten `for`-Schleifen erzeugen zunächst alle Kombinationen aus Curriculum, Prüfungsfach und Modul. Der `let`-Ausdruck sucht den passenden Kurs, und `where exists($course)` entfernt Module ohne Kurs. Die Ausgabe enthält pro Modul ein `module`-Element mit Daten aus beiden Dokumenten, darunter Studienkennzahl, Curriculum, Fach, Kategorie, Modultitel und ECTS sowie Kursnummer, Kurstitel, Art, Wochenstunden, Abhaltungsmodus, Sprache und Vortragende. Das Ergebnis sind 2 Module und 2 Vortragende.

### 3.3 Dynamische Konstruktoren

`ex5-t2-q2-create.xq` erzeugt einen Kursindex, dessen Element- und Attributnamen von den Eingabedaten abhängen. Für jeden Kurs wird ein berechnetes Element gebaut:

```xquery
element {concat('course_', translate(string($c/@number), '.-', '__'))} {
  attribute {concat('moduleCount_', count($modules))} { count($modules) },
  ...
}
```

Der Elementname entsteht aus der Kursnummer. Punkte und Bindestriche werden durch Unterstriche ersetzt, damit ein gültiger NCName entsteht. Der Attributname `moduleCount_<n>` enthält die Anzahl der Module, die auf den Kurs verweisen; auch der Attributwert ist dynamisch. Für jedes Curriculum, das den Kurs verwendet, wird ein weiteres berechnetes Element `curriculum_<studyCode>` mit dynamischem `degree`-Attribut erzeugt.

Die Ausgabe enthält 2 `course_*`-Elemente, 2 `curriculum_*`-Elemente und 2 `moduleCount_*`-Attribute. Sie hat 49 Elemente und Attribute und erreicht Tiefe 5. Die dynamischen Namen sind `course_192_134`, `course_192_041`, `curriculum_UE_033_521`, `curriculum_UE_033_534` und `moduleCount_1`.

## 4. Theoretische Grundlagen

### 4.1 XQuery-Datenmodell

XQuery arbeitet mit geordneten Sequenzen von Items. Ein Item ist entweder ein Knoten oder ein atomarer Wert. Pfadausdrücke wie `doc('ex5-t1.xml')/tuWien/courses/course` liefern eine Sequenz von Knoten. Funktionen wie `count`, `sum` oder `avg` atomisieren ihre Argumente. Aus `$modules/ects` werden die Textwerte der `ects`-Elemente, die anschließend in Zahlen umgewandelt werden. Ohne Atomisierung könnte man nicht sinnvoll summieren.

### 4.2 FLWOR

FLWOR steht für `for`, `let`, `where`, `order by`, `return`. `for` erzeugt für jedes Element der Eingabesequenz ein Tupel. `let` bindet eine Variable an eine ganze Sequenz, ohne die Anzahl der Tupel zu verändern. `where` filtert Tupel anhand des effektiven booleschen Werts. `order by` sortiert die Tupel, wobei mehrere Sortierschlüssel und `ascending` beziehungsweise `descending` möglich sind. `return` konstruiert aus jedem verbleibenden Tupel ein Ergebnisitem.

Der Unterschied zwischen `for` und `let` ist zentral. `for $x in (1,2,3)` erzeugt drei Tupel, `let $x := (1,2,3)` erzeugt eines, in dem `$x` an die gesamte Sequenz gebunden ist. In q1 wird `let $evals := ...` verwendet, weil die Bewertungen als Ganzes für `count`, `avg` und `max` gebraucht werden.

### 4.3 Vergleiche und effektiver boolescher Wert

XQuery unterscheidet Wertvergleiche (`eq`, `ne`, `lt` und weitere) und allgemeine Vergleiche (`=`, `!=`, `<` und weitere). Wertvergleiche erwarten genau einen atomaren Wert pro Seite. Allgemeine Vergleiche vergleichen Sequenzen elementweise und sind wahr, wenn mindestens ein Paar die Bedingung erfüllt. In `$e/@rating = 5` wird das Attribut atomisiert und als Zahl verglichen. `where` verwendet den effektiven booleschen Wert. Eine nicht-leere Sequenz ist wahr, eine leere falsch, außer bei bestimmten Funktionen und Knoten.

### 4.4 Quantifizierte Ausdrücke

`some $x in S satisfies P` ist wahr, wenn mindestens ein Element aus `S` die Bedingung `P` erfüllt. `every $x in S satisfies P` ist wahr, wenn alle Elemente die Bedingung erfüllen. Bei leerem `S` ist `some` falsch und `every` wahr. Quantifizierte Ausdrücke sind näher an der Logik formuliert als verschachtelte Schleifen und brechen die Auswertung intern ab, sobald das Ergebnis feststeht.

### 4.5 Konstruktoren

XQuery kennt direkte Konstruktoren wie `<courseSummary ...>...</courseSummary>` und berechnete Konstruktoren wie `element {Name} {Inhalt}` und `attribute {Name} {Wert}`. Bei berechneten Konstruktoren müssen Name und Wert zur Laufzeit gültige Werte ergeben. Ein Elementname muss ein gültiger NCName sein, darf also nicht mit einer Ziffer beginnen und keine Leerzeichen oder Punkte enthalten. Deshalb ersetzt `translate` in q2 Punkte und Leerzeichen durch Unterstriche. Berechnete Konstruktoren sind der Grund, warum XQuery für die Umformung von XML geeignet ist: Das Ergebnis ist selbst wieder XML.

### 4.6 XQuery, XPath, SQL und XSLT

XPath ist eine Pfadsprache und liefert Knotensequenzen. XQuery erweitert XPath um FLWOR, Konstruktoren, Funktionen und Module. SQL arbeitet mit Tabellen und relationalen Tupeln, während XQuery hierarchische, geordnete Bäume verarbeitet. XSLT ist templatebasiert und eignet sich besonders für regelbasierte Umformungen eines Dokuments. XQuery ist stärker ausdrucksorientiert und eignet sich für Abfragen, Joins und die gezielte Konstruktion neuer Dokumente. Für diese Aufgaben war XQuery passend, weil sowohl Aggregationen über Hierarchien als auch Joins zwischen zwei Dokumenten gefragt waren.

## 5. Umsetzung und Verifikation

Als XQuery-Prozessor habe ich BaseX 13.0 verwendet. Die JAR-Datei liegt nur unter `/tmp/basex.jar` und ist nicht Teil der Abgabe. Ich habe die Abfragen aus dem Verzeichnis `aufgabe5` gestartet, damit relative `doc()`-Pfade funktionieren. Die XML-Dateien wurden mit `xmllint` geprüft.

| Prüfung | Ergebnis |
|---|---|
| Wohlgeformtheit aller XML-Dateien | bestanden |
| `ex5-t1.xml`: Knoten / Elemente mit vier Vorfahren | 94 / 12 |
| q1 / q2 / q3 | 3 / 2 / 2 Ergebnisse |
| `ex5-t2-i1.xml`: Knoten | 42 |
| `ex5-t2-i2.xml`: Knoten | 33 |
| Join-Ausgabe: Module / Vortragende | 2 / 2 |
| Konstruktor-Ausgabe: course_ / curriculum_ / moduleCount_ | 2 / 2 / 2 |
| Konstruktor-Ausgabe: Knoten / Elemente mit drei Vorfahren | 49 / 8 |
| Repräsentierte Eingabeknoten (T1 / T2-i1 / T2-i2) | 50 / 28 / 22 |

## 6. Dateien

Die Abgabe besteht aus den geforderten 13 Dateien in `aufgabe5/`: `ex5-t1.xml`, den drei Abfragen und drei Ausgaben für Aufgabe 1, `ex5-t2-i1.xml`, `ex5-t2-i2.xml`, den zwei Abfragen und zwei Ausgaben für Aufgabe 2. Der Ordner enthält keine temporären Dateien und keine zusätzlichen Zwischenergebnisse. Dieser Bericht liegt bewusst außerhalb von `aufgabe5/`, damit das Abgabeverzeichnis minimal bleibt. Die individuelle Reflexion, `metadata.xml` und der Diskussionsbericht wurden nicht bearbeitet.

## 7. Fazit

Die größte Herausforderung war nicht die Syntax, sondern die saubere Trennung von Knoten- und Wertesemantik. Man muss wissen, wann eine Sequenz atomisiert wird, wann ein allgemeiner Vergleich ausreicht und wann ein quantifizierter Ausdruck die Absicht klarer ausdrückt. Besonders lehrreich waren die berechneten Konstruktoren, weil dabei klar wird, dass Element- und Attributnamen in XQuery echte Laufzeitwerte sein können. Der Join über `courseRef/@number` zeigt außerdem, dass XQuery Dokumente ähnlich wie Tabellen verknüpfen kann, ohne dass eine gemeinsame Datenbank nötig ist.
