<?xml version="1.0" encoding="UTF-8"?>

<!-- $Id$ -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:preserve-space elements="*"/>

<xsl:template match="/compliance">
<!-- Forrest/Cocoon will place the output from this stylesheet inside of a
     larger html document. The output here is all within one <div> tag. -->
  <div class="content">
  <xsl:apply-templates select="head"/>
  <xsl:apply-templates select="body"/>
  </div>
</xsl:template>

<xsl:template match="head">
      <head>
      <meta http-equiv="Content-Language" content="en-us"/>
      <meta http-equiv="Content-Type" content="text/html; charset=windows-1252"/>
      <link type="text/css" href="skin/page.css" rel="stylesheet"/>
      <style>
      &lt;!--
      .yes         { background-color: #99CCFF }
      .no          { background-color: #FF99CC }
      .partial     { background-color: #C0C0C0 }
      .category    { background-color: #FFCC66;
                     font-size: 1.2em }
      --&gt;
      </style>
      </head>
</xsl:template>

<xsl:template match="body">
  <table class="title" summary="">
    <tr>
    <td valign="middle">
      <h1><xsl:value-of select="/compliance/head/title"/></h1>
    </td>
    <td nowrap="nowrap" width="40" align="center">
      <a class="dida" href="compliance.pdf"><img alt="PDF" src="skin/images/pdfdoc.gif" border="0"/><br/>PDF</a>
    </td>
  </tr>
  </table>
  <xsl:apply-templates select="standard"/>
</xsl:template>

<xsl:template match="standard">
  <h2>
    <a>
      <xsl:attribute name="target">
        <xsl:value-of select="@baseURL"/>
      </xsl:attribute>
      <xsl:attribute name="href">
        <xsl:value-of select="@baseURL"/>
      </xsl:attribute>
      <xsl:value-of select="@name"/>
    </a>
  </h2>
  <xsl:apply-templates select="explanatory"/>
  <xsl:apply-templates select="level-1"/>
</xsl:template>

<xsl:template match="level-1">
  <h3>
    <xsl:value-of select="@name"/>
    <xsl:if test="@citation">
      <xsl:text> (</xsl:text>
      <a>
        <xsl:attribute name="target">
          <xsl:apply-templates select="../@baseURL"/>
        </xsl:attribute>
        <xsl:attribute name="href">
          <xsl:apply-templates select="../@baseURL"/>/<xsl:apply-templates select="@extURL"/>
        </xsl:attribute>
        <xsl:value-of select="@citation"/>
      </a>
      <xsl:text>)</xsl:text>
    </xsl:if>
  </h3>
  <xsl:apply-templates select="explanatory"/>
  <table border="1">
  <tr>
    <th rowspan="2">
      <p><xsl:value-of select="@compliance-item-desc"/></p>
    </th>
    <th align="center" rowspan="2">
      Citation
    </th>
    <th align="center" colspan="3">
      Support
    </th>
    <th rowspan="2">
      Comments
    </th>
  </tr>
  <tr>
    <th align="center">
      <xsl:value-of select="/compliance/body/standard/@compliance-level-1-desc"/>
    </th>
    <th align="center">
      <xsl:value-of select="/compliance/body/standard/@compliance-level-2-desc"/>
    </th>
    <th align="center">
      <xsl:value-of select="/compliance/body/standard/@compliance-level-3-desc"/>
    </th>
  </tr>
  <xsl:apply-templates select="level-2"/>
  </table>
</xsl:template>

<xsl:template match="explanatory">
  <xsl:for-each select="p">
    <p>
      <xsl:apply-templates/>
    </p>
  </xsl:for-each>
</xsl:template>

<xsl:template match="level-2">
  <tr>
    <td colspan="6" class="category">
    <xsl:value-of select="@name"/>
    <xsl:if test="@citation">
      <xsl:text> (</xsl:text>
      <a>
        <xsl:attribute name="target">
          <xsl:apply-templates select="../../@baseURL"/>
        </xsl:attribute>
        <xsl:attribute name="href">
          <xsl:apply-templates select="../../@baseURL"/>/<xsl:apply-templates select="@extURL"/>
        </xsl:attribute>
        <xsl:value-of select="@citation"/>
      </a>
      <xsl:text>)</xsl:text>
    </xsl:if>
    </td>
  </tr>
  <xsl:apply-templates select="level-3"/>
</xsl:template>

<xsl:template match="level-3">
  <tr>
    <td>
      <xsl:value-of select="@name"/>
    </td>
    <td align="center">
      <xsl:choose>
        <xsl:when test="@citation">
          <a>
            <xsl:attribute name="target">
              <xsl:apply-templates select="../../../@baseURL"/>
            </xsl:attribute>
            <xsl:attribute name="href">
              <xsl:apply-templates select="../../../@baseURL"/>/<xsl:apply-templates select="@extURL"/>
            </xsl:attribute>
            <xsl:value-of select="@citation"/>
          </a>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>.</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </td>
    <td align="center">
      <xsl:attribute name="class">
        <xsl:choose>
          <xsl:when test="@comply='yes'">
            <xsl:text>yes</xsl:text>
          </xsl:when>
          <xsl:when test="@comply='no'">
            <xsl:choose>
              <xsl:when test="@compliance-level > 1">
                <xsl:text>yes</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text>no</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:when test="@comply='partial'">
            <xsl:choose>
              <xsl:when test="@compliance-level > 1">
                <xsl:text>yes</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text>partial</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text></xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:choose>
        <xsl:when test="@compliance-level=1">
          <xsl:value-of select="@comply"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>.</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </td>
    <td align="center">
      <xsl:attribute name="class">
        <xsl:choose>
          <xsl:when test="@comply='yes'">
            <xsl:text>yes</xsl:text>
          </xsl:when>
          <xsl:when test="@comply='no'">
            <xsl:choose>
              <xsl:when test="@compliance-level > 2">
                <xsl:text>yes</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text>no</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:when test="@comply='partial'">
            <xsl:choose>
              <xsl:when test="@compliance-level > 2">
                <xsl:text>yes</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text>partial</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text></xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:choose>
        <xsl:when test="@compliance-level=2">
          <xsl:value-of select="@comply"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>.</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </td>
    <td align="center">
      <xsl:attribute name="class">
        <xsl:choose>
          <xsl:when test="@comply='yes'">
            <xsl:text>yes</xsl:text>
          </xsl:when>
          <xsl:when test="@comply='no'">
            <xsl:choose>
              <xsl:when test="@compliance-level > 3">
                <xsl:text>yes</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text>no</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:when test="@comply='partial'">
            <xsl:choose>
              <xsl:when test="@compliance-level > 3">
                <xsl:text>yes</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text>partial</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text></xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:choose>
        <xsl:when test="@compliance-level=3">
          <xsl:value-of select="@comply"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>.</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </td>
    <xsl:choose>
      <xsl:when test="count(comment) > 0">
        <td align="left">
          <ul>
            <xsl:for-each select="comment">
              <li>
                <xsl:value-of select="."/>
             </li>
           </xsl:for-each>
          </ul>
        </td>
      </xsl:when>
      <xsl:otherwise>
        <td align="center">
          <xsl:text>.</xsl:text>
        </td>
      </xsl:otherwise>
    </xsl:choose>
  </tr>
</xsl:template>

</xsl:stylesheet>

<!-- Last Line of $RCSFile -->