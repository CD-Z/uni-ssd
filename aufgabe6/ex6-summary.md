# Exercise 6: SAX

Scenario: TU Wien courses and curricula (student ID ending in 6).

## Task 1: bicycle rental XML to HTML

Files:
- `ex6-1-src/BicycleRentalHtml.java`
- `bicycle-rental.dtd`, `bicycle-rental.xml` (reused from Exercise 3)
- `bicycle-rental.html` (generated output)

The program parses the input with SAX and writes HTML. The handler does not know
the DTD. Every element becomes a nested `div` with its tag name, all attributes
(name/value pairs), and its trimmed text. Empty elements such as
`station-pickup` and `rented-bicycle` still show their attributes. Because the
mapping is generic, no element, attribute, value, or non-whitespace text is lost,
and the program works for any valid bicycle-rental document. Indentation is done
with inline `margin-left` and a small inline CSS block.

Build and run:

```sh
javac -d /tmp/ex6c1 ex6-1-src/BicycleRentalHtml.java
java -cp /tmp/ex6c1 BicycleRentalHtml bicycle-rental.xml bicycle-rental.html
```

The DTD was not changed. Validation was checked with
`xmllint --noout --valid bicycle-rental.xml`.

## Task 2: course catalogue statistics

Files:
- `ex6-2-src/CourseStats.java`
- `ex6.xml` (input: 8 courses, 3 curricula)
- `ex6-out.xml` (generated output)

`ex6.xml` combines a course catalogue (`2026W`) with three curricula
(Informatik, Wirtschaftsinformatik, Data Science). Modules link to courses via
`<courseRef number="..."/>`, so the document contains a real many-to-many
relation between courses and curricula.

The SAX handler keeps an element-path stack. Text is assigned to model fields
based on the parent element, which matters because `title` and `ects` occur in
several contexts. `<courseRef>` links are collected per curriculum, then resolved
against the courses after parsing. The output contains:

- totals: courses, curricula, weekly hours, ECTS, averages per course
- courses grouped by kind, language, and delivery mode
- per lecturer: number of courses and total ECTS
- per assessment type: count and average weight
- per curriculum: module count, summed module ECTS, its share of `totalEcts`,
  course references, distinct courses
- per course: attributes, lecturer count, sum of assessment weights, and the
  curricula that use it (`<used-in>`)

Build and run:

```sh
javac -d /tmp/ex6c2 ex6-2-src/CourseStats.java
java -cp /tmp/ex6c2 CourseStats ex6.xml ex6-out.xml
```

Both input and output are well-formed (`xmllint --noout`).

## Observations on SAX

- Streaming keeps memory flat, but there is no tree: relationships such as
  course-to-curriculum links must be collected manually and rebuilt at the end.
- Context only exists through the handler's own stack; a `title` element means
  nothing without its parent path.
- Character data can arrive in several `characters()` calls, so text needs a
  buffer and should only be used at `endElement()`.
- Unlike DOM or XPath, there is no way to look ahead or query: aggregates need
  mutable accumulators (maps of counts and sums).

## Not included

`metadata.xml` with the individual reflection and the scanned discussion report
belong to the post-discussion submission and were not produced. Pack the files
above into `ex6.zip` once those two items exist.
