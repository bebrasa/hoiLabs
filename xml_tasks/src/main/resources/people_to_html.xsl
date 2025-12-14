<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="html" encoding="UTF-8" indent="yes"/>

  <!-- Index people by @id for fast lookup; also use id() as a helper when available -->
  <xsl:key name="kPersonById" match="people/person" use="@id"/>

  <!-- Optional selection parameters -->
  <xsl:param name="personId"/>
  <xsl:param name="nth" select="1"/>

  <!-- Helper: render a display name for a person node -->
  <xsl:template match="people/person" mode="display-name">
    <xsl:choose>
      <xsl:when test="normalize-space(name/full)">
        <xsl:value-of select="normalize-space(name/full)"/>
      </xsl:when>
      <xsl:when test="normalize-space(name/first) or normalize-space(name/last)">
        <xsl:value-of select="normalize-space(concat(normalize-space(name/first), ' ', normalize-space(name/last)))"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@id"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Block with details for one person node -->
  <xsl:template name="person-block">
    <xsl:param name="p"/>
    <xsl:param name="title"/>

    <xsl:variable name="parentsById" select="key('kPersonById', $p/parents/parentRef/@ref) | id($p/parents/parentRef/@ref)"/>
    <xsl:variable name="fathers" select="$parentsById[gender='male']"/>
    <xsl:variable name="mothers" select="$parentsById[gender='female']"/>

    <xsl:variable name="sib" select="$p/siblings"/>
    <!-- Resolve by @ref only via key(); id() may not work in non schema-aware processors -->
    <xsl:variable name="brothers" select="key('kPersonById', $sib/brotherRef/@ref) | key('kPersonById', $sib/siblingRef/@ref)[gender='male']"/>
    <xsl:variable name="sisters"  select="key('kPersonById', $sib/sisterRef/@ref)  | key('kPersonById', $sib/siblingRef/@ref)[gender='female']"/>

    <xsl:variable name="sons" select="key('kPersonById', $p/children/sonRef/@ref) | key('kPersonById', $p/children/childRef/@ref)[gender='male']"/>
    <xsl:variable name="daughters" select="key('kPersonById', $p/children/daughterRef/@ref) | key('kPersonById', $p/children/childRef/@ref)[gender='female']"/>

    <xsl:variable name="grandParentsById" select="key('kPersonById', $parentsById/parents/parentRef/@ref)"/>
    <xsl:variable name="grandFathers" select="$grandParentsById[gender='male']"/>
    <xsl:variable name="grandMothers" select="$grandParentsById[gender='female']"/>

    <xsl:variable name="parentSibs" select="$parentsById/siblings"/>
    <xsl:variable name="uncles" select="(key('kPersonById', $parentSibs/brotherRef/@ref) | id($parentSibs/brotherRef/@ref)) | (key('kPersonById', $parentSibs/siblingRef/@ref) | id($parentSibs/siblingRef/@ref))[gender='male']"/>
    <xsl:variable name="aunts"  select="(key('kPersonById', $parentSibs/sisterRef/@ref)  | id($parentSibs/sisterRef/@ref))  | (key('kPersonById', $parentSibs/siblingRef/@ref) | id($parentSibs/siblingRef/@ref))[gender='female']"/>

    <section>
      <h2><xsl:value-of select="$title"/></h2>
      <p>
        <strong>Name:</strong>
        <xsl:apply-templates select="$p" mode="display-name"/>
        <span> (</span><xsl:value-of select="$p/gender"/><span>)</span>
      </p>

      <p>
        <strong>Father(s):</strong>
        <xsl:choose>
          <xsl:when test="count($fathers) &gt; 0">
            <xsl:for-each select="$fathers">
              <xsl:if test="position() &gt; 1">, </xsl:if>
              <xsl:apply-templates select="." mode="display-name"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>—</xsl:otherwise>
        </xsl:choose>
      </p>

      <p>
        <strong>Mother(s):</strong>
        <xsl:choose>
          <xsl:when test="count($mothers) &gt; 0">
            <xsl:for-each select="$mothers">
              <xsl:if test="position() &gt; 1">, </xsl:if>
              <xsl:apply-templates select="." mode="display-name"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>—</xsl:otherwise>
        </xsl:choose>
      </p>

      <p>
        <strong>Brothers:</strong>
        <xsl:choose>
          <xsl:when test="count($brothers) &gt; 0 or count($sib/brotherName) &gt; 0">
            <xsl:for-each select="$brothers">
              <xsl:if test="position() &gt; 1">, </xsl:if>
              <xsl:apply-templates select="." mode="display-name"/>
            </xsl:for-each>
            <xsl:if test="count($brothers) &gt; 0 and count($sib/brotherName) &gt; 0">, </xsl:if>
            <xsl:for-each select="$sib/brotherName[normalize-space()]">
              <xsl:if test="position() &gt; 1 and not(count($brothers) = 0 and position() = 1)">, </xsl:if>
              <xsl:value-of select="normalize-space(.)"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>—</xsl:otherwise>
        </xsl:choose>
      </p>

      <p>
        <strong>Sisters:</strong>
        <xsl:choose>
          <xsl:when test="count($sisters) &gt; 0 or count($sib/sisterName) &gt; 0">
            <xsl:for-each select="$sisters">
              <xsl:if test="position() &gt; 1">, </xsl:if>
              <xsl:apply-templates select="." mode="display-name"/>
            </xsl:for-each>
            <xsl:if test="count($sisters) &gt; 0 and count($sib/sisterName) &gt; 0">, </xsl:if>
            <xsl:for-each select="$sib/sisterName[normalize-space()]">
              <xsl:if test="position() &gt; 1 and not(count($sisters) = 0 and position() = 1)">, </xsl:if>
              <xsl:value-of select="normalize-space(.)"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>—</xsl:otherwise>
        </xsl:choose>
      </p>

      <p>
        <strong>Sons:</strong>
        <xsl:choose>
          <xsl:when test="count($sons) &gt; 0 or count($p/children/sonName) &gt; 0">
            <xsl:for-each select="$sons">
              <xsl:if test="position() &gt; 1">, </xsl:if>
              <xsl:apply-templates select="." mode="display-name"/>
            </xsl:for-each>
            <xsl:if test="count($sons) &gt; 0 and count($p/children/sonName) &gt; 0">, </xsl:if>
            <xsl:for-each select="$p/children/sonName[normalize-space()]">
              <xsl:if test="position() &gt; 1 and not(count($sons) = 0 and position() = 1)">, </xsl:if>
              <xsl:value-of select="normalize-space(.)"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>—</xsl:otherwise>
        </xsl:choose>
      </p>

      <p>
        <strong>Daughters:</strong>
        <xsl:choose>
          <xsl:when test="count($daughters) &gt; 0 or count($p/children/daughterName) &gt; 0">
            <xsl:for-each select="$daughters">
              <xsl:if test="position() &gt; 1">, </xsl:if>
              <xsl:apply-templates select="." mode="display-name"/>
            </xsl:for-each>
            <xsl:if test="count($daughters) &gt; 0 and count($p/children/daughterName) &gt; 0">, </xsl:if>
            <xsl:for-each select="$p/children/daughterName[normalize-space()]">
              <xsl:if test="position() &gt; 1 and not(count($daughters) = 0 and position() = 1)">, </xsl:if>
              <xsl:value-of select="normalize-space(.)"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>—</xsl:otherwise>
        </xsl:choose>
      </p>

      <p>
        <strong>Grandfathers:</strong>
        <xsl:choose>
          <xsl:when test="count($grandFathers) &gt; 0">
            <xsl:for-each select="$grandFathers">
              <xsl:if test="position() &gt; 1">, </xsl:if>
              <xsl:apply-templates select="." mode="display-name"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>—</xsl:otherwise>
        </xsl:choose>
      </p>

      <p>
        <strong>Grandmothers:</strong>
        <xsl:choose>
          <xsl:when test="count($grandMothers) &gt; 0">
            <xsl:for-each select="$grandMothers">
              <xsl:if test="position() &gt; 1">, </xsl:if>
              <xsl:apply-templates select="." mode="display-name"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>—</xsl:otherwise>
        </xsl:choose>
      </p>

      <p>
        <strong>Uncles:</strong>
        <xsl:choose>
          <xsl:when test="count($uncles) &gt; 0 or count($parentSibs/brotherName) &gt; 0">
            <xsl:for-each select="$uncles">
              <xsl:if test="position() &gt; 1">, </xsl:if>
              <xsl:apply-templates select="." mode="display-name"/>
            </xsl:for-each>
            <xsl:if test="count($uncles) &gt; 0 and count($parentSibs/brotherName) &gt; 0">, </xsl:if>
            <xsl:for-each select="$parentSibs/brotherName[normalize-space()]">
              <xsl:if test="position() &gt; 1 and not(count($uncles) = 0 and position() = 1)">, </xsl:if>
              <xsl:value-of select="normalize-space(.)"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>—</xsl:otherwise>
        </xsl:choose>
      </p>

      <p>
        <strong>Aunts:</strong>
        <xsl:choose>
          <xsl:when test="count($aunts) &gt; 0 or count($parentSibs/sisterName) &gt; 0">
            <xsl:for-each select="$aunts">
              <xsl:if test="position() &gt; 1">, </xsl:if>
              <xsl:apply-templates select="." mode="display-name"/>
            </xsl:for-each>
            <xsl:if test="count($aunts) &gt; 0 and count($parentSibs/sisterName) &gt; 0">, </xsl:if>
            <xsl:for-each select="$parentSibs/sisterName[normalize-space()]">
              <xsl:if test="position() &gt; 1 and not(count($aunts) = 0 and position() = 1)">, </xsl:if>
              <xsl:value-of select="normalize-space(.)"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>—</xsl:otherwise>
        </xsl:choose>
      </p>

    </section>
  </xsl:template>

  <!-- Root template: pick the first person having parents, at least one grandparent, and siblings -->
  <xsl:template match="/">
    <html>
      <head>
        <meta charset="UTF-8"/>
        <title>Family Report</title>
        <style type="text/css">
          body{font-family: -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif; margin: 2rem;}
          h1{margin-top:0}
          section{border:1px solid #ddd; padding:1rem; margin:1rem 0; border-radius:8px;}
          strong{display:inline-block; width:10rem}
        </style>
      </head>
      <body>
        <h1>Family Report</h1>
        <!-- Allow forcing a specific person via @id; otherwise pick the N-th matching one (default 1st). -->
        <xsl:variable name="candById" select="key('kPersonById', $personId)[normalize-space($personId)]"/>
        <xsl:variable name="candidate" select="
          $candById
          |
          (/people/person[
            (
              count(key('kPersonById', parents/parentRef/@ref)[gender='male']) &gt; 0
              and count(key('kPersonById', parents/parentRef/@ref)[gender='female']) &gt; 0
            )
            and (
              count(key('kPersonById', siblings/brotherRef/@ref))
              + count(key('kPersonById', siblings/siblingRef/@ref)[gender='male'])
              &gt; 0
            )
            and (
              count(key('kPersonById', siblings/sisterRef/@ref))
              + count(key('kPersonById', siblings/siblingRef/@ref)[gender='female'])
              &gt; 0
            )
            and (
              count(key('kPersonById', key('kPersonById', parents/parentRef/@ref)/parents/parentRef/@ref))
              &gt; 0
            )
          ][$nth])[not($candById)]
        "/>
        <xsl:choose>
          <xsl:when test="$candidate">
            <!-- Subject -->
            <xsl:call-template name="person-block">
              <xsl:with-param name="p" select="$candidate"/>
              <xsl:with-param name="title" select="'Subject'"/>
            </xsl:call-template>

            <!-- Parents (father, mother) -->
            <xsl:variable name="candParents" select="key('kPersonById', $candidate/parents/parentRef/@ref) | id($candidate/parents/parentRef/@ref)"/>
            <xsl:for-each select="$candParents[gender='male']">
              <xsl:call-template name="person-block">
                <xsl:with-param name="p" select="."/>
                <xsl:with-param name="title" select="'Father'"/>
              </xsl:call-template>
            </xsl:for-each>
            <xsl:for-each select="$candParents[gender='female']">
              <xsl:call-template name="person-block">
                <xsl:with-param name="p" select="."/>
                <xsl:with-param name="title" select="'Mother'"/>
              </xsl:call-template>
            </xsl:for-each>

            <!-- Brothers -->
            <xsl:variable name="csib" select="$candidate/siblings"/>
            <xsl:for-each select="(key('kPersonById', $csib/brotherRef/@ref) | id($csib/brotherRef/@ref)) | (key('kPersonById', $csib/siblingRef/@ref) | id($csib/siblingRef/@ref))[gender='male']">
              <xsl:call-template name="person-block">
                <xsl:with-param name="p" select="."/>
                <xsl:with-param name="title" select="'Brother'"/>
              </xsl:call-template>
            </xsl:for-each>

            <!-- Sisters -->
            <xsl:for-each select="(key('kPersonById', $csib/sisterRef/@ref) | id($csib/sisterRef/@ref)) | (key('kPersonById', $csib/siblingRef/@ref) | id($csib/siblingRef/@ref))[gender='female']">
              <xsl:call-template name="person-block">
                <xsl:with-param name="p" select="."/>
                <xsl:with-param name="title" select="'Sister'"/>
              </xsl:call-template>
            </xsl:for-each>

            <!-- Grandfathers / Grandmothers -->
            <xsl:variable name="candGrandParents" select="key('kPersonById', $candParents/parents/parentRef/@ref)"/>
            <xsl:for-each select="$candGrandParents[gender='male']">
              <xsl:call-template name="person-block">
                <xsl:with-param name="p" select="."/>
                <xsl:with-param name="title" select="'Grandfather'"/>
              </xsl:call-template>
            </xsl:for-each>
            <xsl:for-each select="$candGrandParents[gender='female']">
              <xsl:call-template name="person-block">
                <xsl:with-param name="p" select="."/>
                <xsl:with-param name="title" select="'Grandmother'"/>
              </xsl:call-template>
            </xsl:for-each>

          </xsl:when>
          <xsl:otherwise>
            <p>No person with parents, grandparent and siblings found.</p>
          </xsl:otherwise>
        </xsl:choose>
      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>
