<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" encoding="UTF-8" indent="yes" />

  <!-- Template 1: Root transformation into Inverted Course-Centric Catalog -->
  <xsl:template match="/tuWien">
    <courseCatalog semester="{@semester}" generated="2026-11-09">
      <xsl:apply-templates select="courses/course" />
    </courseCatalog>
  </xsl:template>

  <!-- Template 2: Course Entry with Cross-Referencing into Curricula -->
  <xsl:template match="course">
    <xsl:variable name="currentNo" select="@number" />
    <xsl:variable name="matchingModules" select="//module[courseRef/@number = $currentNo]" />

    <courseEntry number="{@number}">
      <details title="{title}" kind="{kind}" ects="{ects}" weeklyHours="{weeklyHours}" deliveryMode="{deliveryMode}" />

      <curriculumMapping totalUsages="{count($matchingModules)}">
        <xsl:apply-templates select="$matchingModules" mode="usage-mapping" />
      </curriculumMapping>

      <xsl:apply-templates select="." mode="metrics" />
    </courseEntry>
  </xsl:template>

  <!-- Template 3: Inverted Mapping (Module -> Usage -> Subject -> Curriculum)
       Produces deep nesting: courseEntry (2) -> curriculumMapping (3) -> usage (4) ->
       examinationSubject (5) -> moduleRequirement (6) -> moduleTitle (7). -->
  <xsl:template match="module" mode="usage-mapping">
    <usage studyCode="{ancestor::curriculum/@studyCode}" degree="{ancestor::curriculum/@degree}">
      <curriculumTitle><xsl:value-of select="ancestor::curriculum/title" /></curriculumTitle>
      <examinationSubject id="{ancestor::examinationSubject/@id}">
        <subjectTitle><xsl:value-of select="ancestor::examinationSubject/title" /></subjectTitle>
        <moduleRequirement category="{@category}" allocatedEcts="{ects}">
          <moduleTitle><xsl:value-of select="title" /></moduleTitle>
        </moduleRequirement>
      </examinationSubject>
    </usage>
  </xsl:template>

  <!-- Template 4: Structural Aggregations / Metrics per Course without loops -->
  <xsl:template match="course" mode="metrics">
    <xsl:variable name="currentNo" select="@number" />
    <statistics>
      <allocation compulsoryCount="{count(//module[courseRef/@number = $currentNo and @category = 'compulsory'])}"
                  electiveCount="{count(//module[courseRef/@number = $currentNo and @category != 'compulsory'])}" />
    </statistics>
  </xsl:template>

</xsl:stylesheet>
