import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Transforms a bicycle-rental XML document into HTML using SAX.
 *
 * The handler is deliberately generic: every element becomes a nested block
 * showing the element name, all attributes and the element text. Therefore no
 * information from the input document is dropped, whatever the schema is.
 *
 * Usage: java BicycleRentalHtml bicycle-rental.xml bicycle-rental.html
 */
public final class BicycleRentalHtml {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java BicycleRentalHtml <input.xml> <output.html>");
            System.exit(2);
        }

        File input = new File(args[0]);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        // The input is assumed to be valid w.r.t. bicycle-rental.dtd.
        // Validating would require resolving the DTD; we only need the content.
        factory.setValidating(false);

        SAXParser parser = factory.newSAXParser();
        try (Writer out = new OutputStreamWriter(
                new FileOutputStream(args[1]), StandardCharsets.UTF_8)) {
            HtmlHandler handler = new HtmlHandler(out, input.getName());
            parser.parse(input, handler);
        }

        System.out.println("Wrote " + args[1]);
    }

    private static final class HtmlHandler extends DefaultHandler {

        private final Writer out;
        private final String sourceName;
        private final Deque<String> stack = new ArrayDeque<>();
        private final StringBuilder text = new StringBuilder();

        HtmlHandler(Writer out, String sourceName) {
            this.out = out;
            this.sourceName = sourceName;
        }

        @Override
        public void startDocument() throws org.xml.sax.SAXException {
            write("""
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                    <meta charset="utf-8">
                    <title>Bicycle rental</title>
                    <style>
                      body { font-family: system-ui, sans-serif; margin: 2rem; color: #222; }
                      h1 { font-size: 1.4rem; }
                      .el { border-left: 2px solid #c8d0da; margin: 0.15rem 0;
                            padding-left: 0.6rem; }
                      .tag { font-family: monospace; font-weight: bold; color: #0b5fa5; }
                      .attr { font-family: monospace; color: #7a4a00; }
                      .val { font-family: monospace; color: #333; }
                      .text { color: #222; }
                      .src { color: #666; font-size: 0.85rem; }
                    </style>
                    </head>
                    <body>
                    <h1>Bicycle rental</h1>
                    """);
            write("<p class=\"src\">Source: " + escape(sourceName) + "</p>\n");
        }

        @Override
        public void endDocument() {
            write("</body>\n</html>\n");
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs)
                throws org.xml.sax.SAXException {
            flushText();
            int depth = stack.size();
            StringBuilder line = new StringBuilder();
            line.append("<div class=\"el\" style=\"margin-left:")
                .append(String.format(Locale.ROOT, "%.1f", depth * 1.2))
                .append("rem\">");
            line.append("<span class=\"tag\">&lt;").append(escape(qName)).append("&gt;</span>");
            for (int i = 0; i < attrs.getLength(); i++) {
                line.append(" <span class=\"attr\">")
                    .append(escape(attrs.getQName(i)))
                    .append("</span>=<span class=\"val\">&quot;")
                    .append(escape(attrs.getValue(i)))
                    .append("&quot;</span>");
            }
            write(line.toString());
            stack.push(qName);
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            text.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String value = text.toString().trim();
            text.setLength(0);
            if (!value.isEmpty()) {
                write(" <span class=\"text\">" + escape(value) + "</span>");
            }
            write("</div>\n");
            stack.pop();
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) {
            // formatting whitespace between elements: not information
        }

        private void flushText() {
            String value = text.toString().trim();
            text.setLength(0);
            if (!value.isEmpty()) {
                write("<span class=\"text\">" + escape(value) + "</span>\n");
            }
        }

        private void write(String s) {
            try {
                out.write(s);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }

        private static String escape(String s) {
            StringBuilder b = new StringBuilder(s.length() + 16);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '&' -> b.append("&amp;");
                    case '<' -> b.append("&lt;");
                    case '>' -> b.append("&gt;");
                    case '"' -> b.append("&quot;");
                    default -> b.append(c);
                }
            }
            return b.toString();
        }
    }
}
