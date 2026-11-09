<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" encoding="UTF-8" indent="yes" />

  <!-- Template 1: Root Template / HTML Document Skeleton -->
  <xsl:template match="/tuWien">
    <xsl:variable name="totalCourses" select="count(courses/course)" />
    <xsl:variable name="totalCurricula" select="count(curricula/curriculum)" />

    <html lang="en">
      <head>
        <meta charset="UTF-8" />
        <title>TU Wien - Course and Curricula Catalog (<xsl:value-of select="@semester" />)</title>
        <style>
          body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; line-height: 1.5; color: #222; margin: 2rem auto; max-width: 1000px; padding: 0 1rem; }
          h1 { color: #006699; border-bottom: 2px solid #006699; padding-bottom: 0.3rem; }
          h2 { color: #333; margin-top: 2rem; border-bottom: 1px solid #ccc; padding-bottom: 0.2rem; }
          h3 { color: #555; margin-bottom: 0.4rem; }
          nav.quick-nav { background: #f2f5f8; border-left: 4px solid #006699; padding: 0.75rem 1rem; margin-bottom: 1.5rem; }
          nav.quick-nav ul { margin: 0.3rem 0 0 1.2rem; padding: 0; }
          table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
          th, td { border: 1px solid #ddd; padding: 0.5rem 0.75rem; text-align: left; }
          th { background: #f8f9fa; }
          .badge { display: inline-block; padding: 0.15rem 0.45rem; border-radius: 4px; font-size: 0.8rem; font-weight: bold; text-transform: uppercase; }
          .badge-compulsory { background: #e3f2fd; color: #0d47a1; }
          .badge-elective { background: #fff3e0; color: #e65100; }
          .badge-delivery { background: #e8f5e9; color: #1b5e20; }
          .curriculum-card { background: #fafafa; border: 1px solid #e0e0e0; border-radius: 4px; padding: 1rem 1.5rem; margin-top: 1.25rem; }
          .subject-block { margin-left: 1rem; border-left: 2px solid #ddd; padding-left: 1rem; margin-top: 0.8rem; }
          .module-item { margin: 0.4rem 0; }
        </style>
      </head>
      <body>
        <header>
          <h1><xsl:value-of select="@university" /> Catalog (<xsl:value-of select="@semester" />)</h1>
          <p>
            Summary: <strong><xsl:value-of select="$totalCourses" /></strong> courses listed across
            <strong><xsl:value-of select="$totalCurricula" /></strong> curricula.
          </p>

          <!-- Intentional, justifiable use of xsl:for-each:
               Compact, non-reusable navigation list without polluting global template scope. -->
          <nav class="quick-nav">
            <strong>Quick Navigation (Curricula):</strong>
            <ul>
              <xsl:for-each select="curricula/curriculum">
                <li>
                  <a href="#curr-{translate(@studyCode, ' ', '_')}">
                    <xsl:value-of select="title" /> (<xsl:value-of select="@studyCode" /> - <xsl:value-of select="@degree" />)
                  </a>
                </li>
              </xsl:for-each>
            </ul>
          </nav>
        </header>

        <main>
          <xsl:apply-templates select="courses" />
          <xsl:apply-templates select="curricula" />
        </main>
      </body>
    </html>
  </xsl:template>

  <!-- Template 2: Courses Catalog Section -->
  <xsl:template match="courses">
    <section id="courses-section">
      <h2>Offered Courses</h2>
      <table>
        <thead>
          <tr>
            <th>Course No.</th>
            <th>Title</th>
            <th>Type</th>
            <th>Hours</th>
            <th>ECTS</th>
            <th>Delivery</th>
          </tr>
        </thead>
        <tbody>
          <xsl:apply-templates select="course">
            <xsl:sort select="@number" data-type="text" order="ascending" />
          </xsl:apply-templates>
        </tbody>
      </table>
    </section>
  </xsl:template>

  <!-- Template 3: Single Course Table Row -->
  <xsl:template match="course">
    <tr>
      <td><code><xsl:value-of select="@number" /></code></td>
      <td><strong><xsl:value-of select="title" /></strong></td>
      <td><xsl:value-of select="kind" /></td>
      <td><xsl:value-of select="weeklyHours" /></td>
      <td><xsl:value-of select="ects" /></td>
      <td>
        <span class="badge badge-delivery">
          <xsl:value-of select="deliveryMode" />
        </span>
      </td>
    </tr>
  </xsl:template>

  <!-- Template 4: Curricula Section -->
  <xsl:template match="curricula">
    <section id="curricula-section">
      <h2>Degree Curricula</h2>
      <xsl:apply-templates select="curriculum" />
    </section>
  </xsl:template>

  <!-- Template 5: Individual Curriculum Card -->
  <xsl:template match="curriculum">
    <article class="curriculum-card" id="curr-{translate(@studyCode, ' ', '_')}">
      <h3>
        <xsl:value-of select="title" />
        <small>(<xsl:value-of select="@degree" />, <xsl:value-of select="@studyCode" />)</small>
      </h3>
      <p>
        Valid from: <xsl:value-of select="@validFrom" /> |
        Duration: <xsl:value-of select="durationSemesters" /> Semesters |
        Total: <xsl:value-of select="totalEcts" /> ECTS
      </p>
      <div>
        <xsl:apply-templates select="examinationSubject" />
      </div>
    </article>
  </xsl:template>

  <!-- Template 6: Examination Subject and Modules -->
  <xsl:template match="examinationSubject">
    <div class="subject-block">
      <h4>Subject: <xsl:value-of select="title" /></h4>
      <ul>
        <xsl:apply-templates select="module" />
      </ul>
    </div>
  </xsl:template>

  <!-- Template 7: Module Item with Course Reference Resolution -->
  <xsl:template match="module">
    <xsl:variable name="refNumber" select="courseRef/@number" />
    <xsl:variable name="referencedCourse" select="/tuWien/courses/course[@number = $refNumber]" />

    <li class="module-item">
      <span>
        <xsl:choose>
          <xsl:when test="@category = 'compulsory'">
            <span class="badge badge-compulsory">Compulsory</span>
          </xsl:when>
          <xsl:otherwise>
            <span class="badge badge-elective"><xsl:value-of select="@category" /></span>
          </xsl:otherwise>
        </xsl:choose>
      </span>
      &#160;<strong><xsl:value-of select="title" /></strong>
      (<xsl:value-of select="ects" /> ECTS)
      &#8212;
      <small>
        Ref: course <code><xsl:value-of select="$refNumber" /></code>
        <xsl:if test="$referencedCourse">
          (<xsl:value-of select="$referencedCourse/title" />, <xsl:value-of select="$referencedCourse/kind" />)
        </xsl:if>
      </small>
    </li>
  </xsl:template>

</xsl:stylesheet>
