package org.springdoclet.rest.writers

import org.springdoclet.Configuration

class RestStylesheetWriter {

  def writeStylesheet(Configuration config) {
    new File(config.outputDirectory, config.styleSheet).withWriter { it.write(STYLESHEET_CONTENT) }
  }

  private static final STYLESHEET_CONTENT = """
body {
  background: #fff;
  color: #333;
  font: 12px verdana, arial, helvetica, sans-serif;
  margin: 0;
}

a {
  color: #003300;
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

h1 {
  color: #007c00;
  font-weight: normal;
  font-size: 3em;
  text-align: center;
}

h2 {
  color: #83aa59;
  font-weight: normal;
  font-size: 2em;
  padding-top:1.5em;
}

h3 {
  color: #83aa59;
  font-weight: normal;
  font-size: 1.5em;
  border-bottom: 1px solid #ddd;
  padding-top:2em;
}

h4 {
  font-size: 1.2em;
  padding-top: 1.5em;
}

table {
  border: none;
  font-size: 1em;
}

th {
  color: #fff;
  background-color: #007c00;
}

td {
  border: none;
  border-bottom: 1px solid #ddd;
  padding: .5em;
}

.comment {
  /*width: 500px; */
}

td.label {
    text-align: right;
    padding-left:15px;
    padding-right:5px;
}

div.exampleHeader {
    margin-top: 1.5em;
    font-style: italic;
}

div.example {
    white-space: pre-wrap;
    font-family: monospace;
    font-size: 11px;
    height: 200px;
    overflow:auto;
    border: 1px solid #ddd;
    padding: 5px;
}

#leftNav {
    width: 300px;
    overflow: auto;
    float:left;
    padding: 5px;
    border-right: 1px solid #ddd;
    border-bottom: 1px solid #ddd;
}

#mainContent {
    float:left;
    width: 750px;
    margin-left: 20px;
    padding: 5px;
}

#mainWrapper {
    width: 1100px;
    min-height: 100%;
    height: auto !important;
    height: 100%;
    margin: 0 auto -2em;
}

#mainFooter, #mainFooterPush {
    font-size: 11px;
    width:100%;
    height: 2em;
    color: #999;
    line-height: 2em;
    vertical-align:bottom;
}

#mainFooter {
    padding-left: 5px;
}

#mainFooterPush {
  clear:both;
}

"""
}
