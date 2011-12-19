package org.springdoclet.rest

import groovy.xml.MarkupBuilder
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Simple external documentation parser.  Content is expected to be wrapped in a single &lt;body&gt;
 * root element.  The element may contain an optional <code>title</code> attribute, which will be
 * exposed as the <code>title</code> field.
 * <p>
 * The document may contain standard XML CDATA sections.  These CDATA sections are html escaped in the
 * string exposed as the <code>text</code> field.
 * 
 * @author ck
 * @version $Revision: 1.2 $ $Date: 2004/05/02 22:16:32 $
 */
public class ExternalDocumentation {

  final String name
  final String text
  final String title

  public ExternalDocumentation(File file) {
    this.name = file.name
    String localText = file.getText()
    String localTitle = null
    localText = localText.trim()
    Matcher m = BODY_START_PATTERN.matcher(localText)
    if (m.find()) {
      if (m.group(1)) {
        Matcher m2 = TITLE_PATTERN.matcher(m.group(1))
        if (m2.find()) {
          localTitle = m2.group(1)
        }
      }
      m.replaceAll("")
    }
    localText = BODY_START_PATTERN.matcher(localText).replaceAll("")
    localText = BODY_END_PATTERN.matcher(localText).replaceAll("")
    localText = localText.trim()
    m = CDATA_PATTERN.matcher(localText)
    StringBuffer sb = new StringBuffer(localText.length())
    while (m.find()) {
      m.appendReplacement(sb, escapingBuilder.escapeElementContent(m.group(1)))
    }
    m.appendTail(sb)
    this.text = sb.toString()
    if (localTitle)
      this.title = localTitle
    else
      this.title = file.name.replaceAll(/(.+)\..+$/, '$1')
  }

  private static final Pattern BODY_START_PATTERN = Pattern.compile(/(?i)^<body(.+?)>/)
  private static final Pattern BODY_END_PATTERN = Pattern.compile(/(?i)<\/body>/)
  private static final Pattern TITLE_PATTERN = Pattern.compile(/(?i)title=["'](.+?)["']/)
  private static final Pattern CDATA_PATTERN = Pattern.compile(/<!\[CDATA\[\s*(.+?)\s*\]\]>/, Pattern.DOTALL)
  private static final MarkupBuilder escapingBuilder = new MarkupBuilder(new StringWriter())
}