import net.sf.saxon.s9api.*;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

public class Application {

    /**
     * Class is not instantiated, so give it a private constructor
     */
    private Application() {
    }

    public static void main(String[] argv) {
        try {
            Processor proc = new Processor(false);
            String htmlStart = "<!DOCTYPE html>\n" +
                                "<html>\n" +
                                "<head>\n" +
                                "<style>\n" +
                                "table, th, td {border: 1px solid black;}\n" +
                                "</style>\n" +
                                "</head>\n" +
                                "<body>\n" +
                                "<table>\n" +
                                "<tr>" +
                                "<th>Interface Name: </th>\n" +
                                "<th>Interface description: </th>\n" +
                                "<th>VRF: </th>\n" +
                                "<th>MTU: </th>\n" +
                                "<th>Interface IP address: </th>\n" +
                                "<th>Interface IP mask: </th>\n" +
                                "</tr>\n";
            String htmlEnd = "</table>\n" +
                            "</body>\n" +
                             "</html>";
            XQueryCompiler comp = proc.newXQueryCompiler();
            XQueryExecutable exp = comp.compile(
                    "declare default element namespace \"urn:cisco:xml-pi\";" +
                            "declare variable $dc external;\n" +
                            "for $prod in doc($dc)/Device-Configuration/interface\n" +
                            //"where matches($prod/Param,\"Loopback*\")" +
                            "return <tr><td>{$prod/Param/text()}</td>" +
                            "<td>{$prod/ConfigIf-Configuration/description/UpTo200CharactersInterface/text()}</td>" +
                            "<td>{$prod/ConfigIf-Configuration/vrf/forwarding/VRFName/text()}</td>" +
                            "<td>{$prod/ConfigIf-Configuration/ip/mtu/MTUBytes/text()}</td>" +
                            "<td>{$prod/ConfigIf-Configuration/ip/address/IPAddress/text()}</td>" +
                            "<td>{$prod/ConfigIf-Configuration/ip/address/IPSubnetMask/text()}</td>" +
                            "</tr>");
            XQueryEvaluator qe = exp.load();
            //XdmNode source = proc.newDocumentBuilder().build(new StreamSource(new File("D:\\2_Work\\Companies\\Digicel\\Config\\Trinidad_and_Tobago\\TT-ASR920-T043-Ana-Street.txt")));
            //qe.setContextItem(source);
            qe.setExternalVariable(new QName("dc"), new XdmAtomicValue("file:///D:/2_Work/Companies/Digicel/Config/Trinidad_and_Tobago/TT-ASR920-T043-Ana-Street.txt"));
            XdmValue result = qe.evaluate();
            PrintWriter out = new PrintWriter("D:\\2_Work\\Companies\\Digicel\\Config\\Trinidad_and_Tobago\\Output.html");
            String stringResult=htmlStart + result + htmlEnd;
            out.write(stringResult);
            out.close();
            //System.out.println(source);
            //Serializer out = proc.newSerializer(fos);
            //proc.writeXdmValue(result,out);
            //System.out.println(result);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private interface Test {
        public String name();

        public boolean needsSaxonEE();

        public void run() throws SaxonApiException;
    }


     private static class QueryA implements Application.Test {
        public String name() {
            return "QueryA";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {

        }
    }

    /**
     * Show a query compiled using a reusable XQExpression object, taking a parameter
     * (external variable), and returning a sequence of integers
     */

    private static class QueryB implements Application.Test {
        public String name() {
            return "QueryB";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            XQueryCompiler comp = proc.newXQueryCompiler();
            XQueryExecutable exp = comp.compile(
                    "declare variable $n external; for $i in 1 to $n return $i*$i");
            XQueryEvaluator qe = exp.load();
            qe.setExternalVariable(new QName("n"), new XdmAtomicValue(10));

            XdmValue result = qe.evaluate();
            int total = 0;
            for (XdmItem item : result) {
                total += ((XdmAtomicValue) item).getLongValue();
            }
            System.out.println("Total: " + total);
        }
    }

    /**
     * Show a query taking input from the context item, reusing the compiled Expression object
     * to run more than one query in succession.
     */

    private static class QueryC implements Application.Test {
        public String name() {
            return "QueryC";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            XQueryCompiler comp = proc.newXQueryCompiler();
            XQueryExecutable exp = comp.compile("contains(., 'e')");

            XQueryEvaluator qe = exp.load();
            qe.setContextItem(new XdmAtomicValue("apple"));
            XdmValue result = qe.evaluate();
            System.out.println("apple: " + ((XdmAtomicValue) result).getBooleanValue());

            qe = exp.load();
            qe.setContextItem(new XdmAtomicValue("banana"));
            result = qe.evaluate();
            System.out.println("banana: " + ((XdmAtomicValue) result).getBooleanValue());

            qe = exp.load();
            qe.setContextItem(new XdmAtomicValue("cherry"));
            result = qe.evaluate();
            System.out.println("cherry: " + ((XdmAtomicValue) result).getBooleanValue());

        }
    }

    /**
     * Show a query producing a DOM document as its output, and passing the DOM as input to a second query.
     * Also demonstrates declaring a namespace for use in the (second) query
     */

    private static class QueryD implements Application.Test {
        public String name() {
            return "QueryD";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            XQueryCompiler comp = proc.newXQueryCompiler();
            comp.declareNamespace("xsd", "http://www.w3.org/2001/XMLSchema");
            XQueryExecutable exp = comp.compile("<temp>{for $i in 1 to 20 return <e>{$i}</e>}</temp>");

            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            dfactory.setNamespaceAware(true);
            Document dom;

            try {
                dom = dfactory.newDocumentBuilder().newDocument();
            } catch (ParserConfigurationException e) {
                throw new SaxonApiException(e);
            }

            exp.load().run(new DOMDestination(dom));

            XdmNode temp = proc.newDocumentBuilder().wrap(dom);

            exp = comp.compile("<out>{//e[xsd:integer(.) gt 10]}</out>");
            XQueryEvaluator qe = exp.load();
            qe.setContextItem(temp);

            Serializer out = proc.newSerializer(System.out);
            qe.run(out);

        }
    }

    /**
     * Show a query taking a SAX event stream as its input and producing a SAX event stream as
     * its output.
     */

    private static class QueryE implements Application.Test {
        public String name() {
            return "QueryE";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            XQueryCompiler comp = proc.newXQueryCompiler();
            comp.declareNamespace("xsd", "http://www.w3.org/2001/XMLSchema");
            XQueryExecutable exp = comp.compile("<copy>{//ITEM[1]}</copy>");
            XQueryEvaluator qe = exp.load();

            File inputFile = new File("data/books.xml");
            FileInputStream fis;
            try {
                fis = new FileInputStream(inputFile);
            } catch (FileNotFoundException e) {
                throw new SaxonApiException(
                        "Input file not found. The current directory should be the Saxon samples directory");
            }
            SAXSource source = new SAXSource(new InputSource(fis));
            source.setSystemId(inputFile.toURI().toString());

            qe.setSource(source);

            ContentHandler ch = new XMLFilterImpl() {

                public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                    System.out.println("Start element {" + uri + "}" + localName);
                }

                public void endElement(String uri, String localName, String qName) throws SAXException {
                    System.out.println("End element {" + uri + "}" + localName);
                }
            };

            qe.run(new SAXDestination(ch));

        }
    }

    /**
     * Show a query that calls an external function, and that passes an external object as a parameter
     * to use as the invocation target of this external function.
     */

    private static class QueryF implements Application.Test {
        public String name() {
            return "QueryF";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(true);
            XQueryCompiler comp = proc.newXQueryCompiler();
            comp.declareNamespace("jt", "http://saxon.sf.net/java-type");
            comp.declareNamespace("Locale", "java:java.util.Locale");
            XQueryExecutable exp = comp.compile(
                    "declare variable $locale as jt:java.util.Locale external;\n" +
                            "Locale:getDisplayLanguage($locale)");

            XQueryEvaluator qe = exp.load();
            XdmItem locale = new ItemTypeFactory(proc).getExternalObject(Locale.ITALIAN);
            qe.setExternalVariable(new QName("locale"), locale);
            XdmAtomicValue result = (XdmAtomicValue) qe.evaluate();
            System.out.println("Locale: " + result.getStringValue());

        }
    }

    /**
     * Show a query that uses two modules, one of which is compiled as a library module so
     * that it can be used repeatedly by further queries in the same XQueryCompiler.
     */

    private static class QueryG implements Application.Test {
        public String name() {
            return "QueryG";
        }

        public boolean needsSaxonEE() {
            return true;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(true);
            XQueryCompiler comp = proc.newXQueryCompiler();
            comp.compileLibrary(
                    "module namespace lib = 'http://example.com/library';" +
                            "declare function lib:plus($a, $b) { $a + $b };" +
                            "declare function lib:subtract($a, $b) { $a - $b };" +
                            "declare variable $lib:five := 5;");

            XdmAtomicValue result1 = (XdmAtomicValue) comp.compile(
                    "import module namespace lib = 'http://example.com/library';\n" +
                            "lib:plus($lib:five, lib:subtract(10, 3))").load().evaluate();
            System.out.println("Result1: " + result1.getStringValue());

            XdmAtomicValue result2 = (XdmAtomicValue) comp.compile(
                    "import module namespace lib = 'http://example.com/library';\n" +
                            "lib:plus($lib:five, lib:subtract(9, 6))").load().evaluate();
            System.out.println("Result2: " + result2.getStringValue());
        }
    }


    /**
     * Compile and execute an updating query taking no input,
     * producing a document as its result and serializing this directly to System.out
     */

    private static class QueryUpdateA implements Application.Test {
        public String name() {
            return "QueryUpdateA";
        }

        public boolean needsSaxonEE() {
            return true;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(true);
            XQueryCompiler comp = proc.newXQueryCompiler();
            comp.setBaseURI(new File(System.getProperty("user.dir")).toURI());
            comp.setUpdatingEnabled(true);
            XQueryExecutable exp = comp.compile(
                    "for $p in doc('data/books.xml')//PRICE " +
                            "return replace value of node $p with $p + 0.05");

            XQueryEvaluator eval = exp.load();
            eval.run();
            for (Iterator<XdmNode> iter = eval.getUpdatedDocuments(); iter.hasNext(); ) {
                XdmNode root = iter.next();
                URI rootUri = root.getDocumentURI();
                if (rootUri != null && rootUri.getScheme().equals("file")) {

                    // Serialize the updated result to System.out
                    Serializer out = proc.newSerializer(System.out);
                    out.setOutputProperty(Serializer.Property.METHOD, "xml");
                    out.setOutputProperty(Serializer.Property.INDENT, "yes");
                    out.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
                    proc.writeXdmValue(root, out);

                    // To serialize to the original source file, replace the above code with the following
                    /*try {
                        Serializer out = proc.newSerializer(new FileOutputStream(new File(rootUri)));
                        out.setOutputProperty(Serializer.Property.METHOD, "xml");
                        out.setOutputProperty(Serializer.Property.INDENT, "yes");
                        out.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
                        System.out.println("Rewriting " + rootUri);
                        proc.writeXdmValue(root, out);
                    } catch (FileNotFoundException e) {
                        System.err.println("Could not write to file " + rootUri);
                    }*/
                } else {
                    System.err.println("Updated document not rewritten: location unknown or not updatable");
                }
            }
        }
    }


    // PART 2: XSLT tests

    /**
     * Compile and execute a simple transformation that applies a stylesheet to an input file,
     * and serializing the result to an output file
     */

    private static class TransformA implements Application.Test {
        public String name() {
            return "TransformA";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            XsltCompiler comp = proc.newXsltCompiler();
            XsltExecutable exp = comp.compile(new StreamSource(new File("styles/books.xsl")));
            XdmNode source = proc.newDocumentBuilder().build(new StreamSource(new File("data/books.xml")));
            Serializer out = proc.newSerializer(new File("books.html"));
            out.setOutputProperty(Serializer.Property.METHOD, "html");
            out.setOutputProperty(Serializer.Property.INDENT, "yes");
            XsltTransformer trans = exp.load();
            trans.setInitialContextNode(source);
            trans.setDestination(out);
            trans.transform();

            System.out.println("Output written to books.html");
        }
    }

    /**
     * Show a transformation that takes no input file, but accepts parameters
     */

    private static class TransformB implements Application.Test {
        public String name() {
            return "TransformB";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            XsltCompiler comp = proc.newXsltCompiler();
            XsltExecutable exp = comp.compile(new StreamSource(new File("styles/tour.xsl")));
            Serializer out = proc.newSerializer(new File("tour.html"));
            out.setOutputProperty(Serializer.Property.METHOD, "html");
            out.setOutputProperty(Serializer.Property.INDENT, "yes");
            XsltTransformer trans = exp.load();
            trans.setInitialTemplate(new QName("main"));
            trans.setDestination(out);
            trans.transform();

            System.out.println("Output written to tour.html");
        }
    }

    /**
     * Show a stylesheet being compiled once and then executed several times with different
     * source documents. The XsltTransformer object is serially reusable, but not thread-safe.
     * The Serializer object is also serially reusable.
     */

    private static class TransformC implements Application.Test {
        public String name() {
            return "TransformC";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            XsltCompiler comp = proc.newXsltCompiler();
            String stylesheet =
                    "<xsl:transform version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>" +
                            "  <xsl:param name='in'/>" +
                            "  <xsl:template name='main'><xsl:value-of select=\"contains($in, 'e')\"/></xsl:template>" +
                            "</xsl:transform>";
            XsltExecutable exp = comp.compile(new StreamSource(new StringReader(stylesheet)));

            Serializer out = proc.newSerializer();
            out.setOutputProperty(Serializer.Property.METHOD, "text");
            XsltTransformer t = exp.load();
            t.setInitialTemplate(new QName("main"));

            String[] fruit = {"apple", "banana", "cherry"};
            QName paramName = new QName("in");
            for (String s : fruit) {
                StringWriter sw = new StringWriter();
                out.setOutputWriter(sw);
                t.setParameter(paramName, new XdmAtomicValue(s));
                t.setDestination(out);
                t.transform();
                System.out.println(s + ": " + sw.toString());
            }

        }
    }

    /**
     * Show the output of one stylesheet being passed for processing to a second stylesheet
     */

    private static class TransformD implements Application.Test {
        public String name() {
            return "TransformD";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            XsltCompiler comp = proc.newXsltCompiler();
            XsltExecutable templates1 = comp.compile(new StreamSource(new File("styles/books.xsl")));
            XdmNode source = proc.newDocumentBuilder().build(new StreamSource(new File("data/books.xml")));

            XsltTransformer trans1 = templates1.load();
            trans1.setInitialContextNode(source);

            String stylesheet2 =
                    "<xsl:transform version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>" +
                            "  <xsl:template match='/'><xsl:value-of select=\"count(//*)\"/></xsl:template>" +
                            "</xsl:transform>";
            XsltExecutable templates2 = comp.compile(new StreamSource(new StringReader(stylesheet2)));
            XsltTransformer trans2 = templates2.load();
            XdmDestination resultTree = new XdmDestination();
            trans2.setDestination(resultTree);

            trans1.setDestination(trans2);
            trans1.transform();

            System.out.println(resultTree.getXdmNode());
        }
    }

    /**
     * Run a transformation by writing a document node to a Transformer
     */

    private static class TransformE implements Application.Test {
        public String name() {
            return "TransformE";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            XsltCompiler comp = proc.newXsltCompiler();
            XsltExecutable templates = comp.compile(new StreamSource(new File("styles/books.xsl")));
            XsltTransformer transformer = templates.load();
            XdmNode source = proc.newDocumentBuilder().build(new StreamSource(new File("data/books.xml")));
            Serializer out = proc.newSerializer(System.out);
            transformer.setDestination(out);
            proc.writeXdmValue(source, transformer);
        }
    }

    /**
     * Run a transformation that sends xsl:message output to a user-supplied MessageListener
     */

    private static class TransformF implements Application.Test {
        public String name() {
            return "TransformF";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            XsltCompiler comp = proc.newXsltCompiler();
            String stylesheet =
                    "<xsl:transform version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>\n" +
                            "  <xsl:template name='main'>\n" +
                            "    <xsl:message><msg>Reading http://www.w3.org/TR/xslt20/ ...</msg></xsl:message>/>\n" +
                            "    <exists>\n" +
                            "      <xsl:value-of select=\"doc-available('http://www.w3.org/TR/xslt20/')\"/>\n" +
                            "    </exists>\n" +
                            "    <xsl:message><msg>finishing</msg></xsl:message>\n" +
                            "  </xsl:template>\n" +
                            "</xsl:transform>";
            StringReader reader = new StringReader(stylesheet);
            StreamSource styleSource = new StreamSource(reader, "http://localhost/string");
            XsltExecutable templates = comp.compile(styleSource);
            XsltTransformer transformer = templates.load();
            transformer.setInitialTemplate(new QName("main"));
            transformer.setMessageListener(
                    new MessageListener() {
                        public void message(XdmNode content, boolean terminate, SourceLocator locator) {
                            System.err.println("MESSAGE terminate=" + (terminate ? "yes" : "no") + " at " + new Date());
                            System.err.println("From instruction at line " + locator.getLineNumber() +
                                    " of " + locator.getSystemId());
                            System.err.println(">>" + content.getStringValue());
                        }
                    }
            );
            Serializer out = proc.newSerializer(System.out);
            transformer.setDestination(out);
            transformer.transform();
            System.err.println("Finished TransformF");
        }
    }

    /**
     * Call a function in a stylesheet, getting back raw results
     */

    private static class TransformG implements Application.Test {
        public String name() {
            return "TransformG";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            XsltCompiler comp = proc.newXsltCompiler();
            XsltExecutable templates = comp.compile(new StreamSource(new File("styles/summarize.xsl")));
            Xslt30Transformer transformer = templates.load30();
            XdmNode source = proc.newDocumentBuilder().build(new StreamSource(new File("data/books.xml")));
            XdmValue books = proc.newXPathCompiler().evaluate("//ITEM[1]/*", source);
            for (XdmItem book : books) {
                XdmValue path = transformer.callFunction(
                        new QName("http://localhost/functions", "path"), new XdmValue[] {book});
                System.out.println(path);
            }
        }
    }


    /**
     * Show schema validation of an input document.
     */

    private static class SchemaA implements Application.Test {
        public String name() {
            return "SchemaA";
        }

        public boolean needsSaxonEE() {
            return true;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(true);
            SchemaManager sm = proc.getSchemaManager();
            sm.load(new StreamSource(new File("data/books.xsd")));

            try {
                SchemaValidator sv = sm.newSchemaValidator();
                sv.validate(new StreamSource(new File("data/books.xml")));
                System.out.println("First schema validation succeeded (as planned)");
            } catch (SaxonApiException err) {
                System.out.println("First schema validation failed");
            }

            // modify books.xml to make it invalid

            XsltCompiler comp = proc.newXsltCompiler();
            String stylesheet =
                    "<xsl:transform version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>" +
                            "  <xsl:import href='identity.xsl'/>" +
                            "  <xsl:template match='TITLE'><OLD-TITLE/></xsl:template>" +
                            "</xsl:transform>";
            StreamSource ss = new StreamSource(new StringReader(stylesheet));
            ss.setSystemId(new File("styles/books.xsl"));
            XsltTransformer trans = comp.compile(ss).load();
            trans.setSource(new StreamSource(new File("data/books.xml")));

            // Create a validator for the result of the transformation, and run the transformation
            // with this validator as its destination

            final List<Exception> errorList = new ArrayList<Exception>();
            try {
                SchemaValidator sv = sm.newSchemaValidator();

                // Create a custom ErrorListener for the validator, which collects validation
                // exceptions in a list for later reference

                ErrorListener listener = new ErrorListener() {
                    public void error(TransformerException exception) throws TransformerException {
                        errorList.add(exception);
                    }

                    public void fatalError(TransformerException exception) throws TransformerException {
                        errorList.add(exception);
                        throw exception;
                    }

                    public void warning(TransformerException exception) throws TransformerException {
                        // no action
                    }
                };
                sv.setErrorListener(listener);
                trans.setDestination(sv);
                trans.transform();
                System.out.println("Second schema validation succeeded");
            } catch (SaxonApiException err) {
                System.out.println("Second schema validation failed (as intended)");
                if (!errorList.isEmpty()) {
                    System.out.println(errorList.get(0).getMessage());
                }
            }

        }
    }

    /**
     * Validate a document by writing the document node to a Validator
     */

    private static class SchemaB implements Application.Test {
        public String name() {
            return "SchemaB";
        }

        public boolean needsSaxonEE() {
            return true;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(true);
            SchemaManager sm = proc.getSchemaManager();
            sm.load(new StreamSource(new File("data/books.xsd")));
            SchemaValidator validator = sm.newSchemaValidator();
            XdmNode source = proc.newDocumentBuilder().build(new StreamSource(new File("data/books.xml")));
            proc.writeXdmValue(source, validator);
            System.out.println("Validation succeeded");
        }
    }


    /**
     * Demonstrate navigation of an input document using XPath and native methods.
     */

    private static class XPathA implements Application.Test {
        public String name() {
            return "XPathA";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            XPathCompiler xpath = proc.newXPathCompiler();
            xpath.declareNamespace("saxon", "http://saxon.sf.net/"); // not actually used, just for demonstration

            DocumentBuilder builder = proc.newDocumentBuilder();
            builder.setLineNumbering(true);
            builder.setWhitespaceStrippingPolicy(WhitespaceStrippingPolicy.ALL);
            XdmNode booksDoc = builder.build(new File("data/books.xml"));

            // find all the ITEM elements, and for each one display the TITLE child

            XPathSelector selector = xpath.compile("//ITEM").load();
            selector.setContextItem(booksDoc);
            QName titleName = new QName("TITLE");
            for (XdmItem item : selector) {
                XdmNode title = getChild((XdmNode) item, titleName);
                System.out.println(title.getNodeName() +
                        "(" + title.getLineNumber() + "): " +
                        title.getStringValue());
            }
        }

        // Helper method to get the first child of an element having a given name.
        // If there is no child with the given name it returns null

        private static XdmNode getChild(XdmNode parent, QName childName) {
            XdmSequenceIterator iter = parent.axisIterator(Axis.CHILD, childName);
            if (iter.hasNext()) {
                return (XdmNode) iter.next();
            } else {
                return null;
            }
        }
    }

    /**
     * Demonstrate use of an XPath expression with variables.
     */

    private static class XPathB implements Application.Test {
        public String name() {
            return "XPathB";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            Processor proc = new Processor(false);
            ItemTypeFactory itf = new ItemTypeFactory(proc);
            ItemType decimalType = itf.getAtomicType(new QName("http://www.w3.org/2001/XMLSchema", "decimal"));
            XPathCompiler xpath = proc.newXPathCompiler();
            xpath.declareVariable(new QName("http://www.example.com/", "x"), decimalType, OccurrenceIndicator.ONE);
            xpath.declareVariable(new QName("http://www.example.com/", "y"), decimalType, OccurrenceIndicator.ONE);
            xpath.declareNamespace("e", "http://www.example.com/");
            XPathExecutable xx = xpath.compile("$e:x + $e:y");
            XPathSelector selector = xx.load();
            selector.setVariable(new QName("http://www.example.com/", "x"), new XdmAtomicValue(new BigDecimal("2.5")));
            selector.setVariable(new QName("http://www.example.com/", "y"), new XdmAtomicValue(new BigDecimal("3.61")));
            XdmAtomicValue result = (XdmAtomicValue) selector.evaluateSingle();
            System.out.println("Result: " + result.getDecimalValue());
        }

    }


    /**
     * Demonstrate use of a schema-aware XPath expression
     */

    private static class XPathC implements Application.Test {
        public String name() {
            return "XPathC";
        }

        public boolean needsSaxonEE() {
            return true;
        }

        public void run() throws SaxonApiException {

            Processor proc = new Processor(true);

            SchemaManager sm = proc.getSchemaManager();
            sm.load(new StreamSource(new File("data/books.xsd")));
            SchemaValidator sv = sm.newSchemaValidator();
            sv.setLax(false);

            XPathCompiler xpath = proc.newXPathCompiler();
            xpath.declareNamespace("saxon", "http://saxon.sf.net/"); // not actually used, just for demonstration
            xpath.importSchemaNamespace("");                         // import schema for the non-namespace

            DocumentBuilder builder = proc.newDocumentBuilder();
            builder.setLineNumbering(true);
            builder.setWhitespaceStrippingPolicy(WhitespaceStrippingPolicy.ALL);
            builder.setSchemaValidator(sv);
            XdmNode booksDoc = builder.build(new File("data/books.xml"));

            // find all the ITEM elements, and for each one display the TITLE child

            XPathSelector verify = xpath.compile(". instance of document-node(schema-element(BOOKLIST))").load();
            verify.setContextItem(booksDoc);
            if (((XdmAtomicValue) verify.evaluateSingle()).getBooleanValue()) {
                XPathSelector selector = xpath.compile("//schema-element(ITEM)").load();
                selector.setContextItem(booksDoc);
                QName titleName = new QName("TITLE");
                for (XdmItem item : selector) {
                    XdmNode title = getChild((XdmNode) item, titleName);
                    System.out.println(title.getNodeName() +
                            "(" + title.getLineNumber() + "): " +
                            title.getStringValue());
                }
            } else {
                System.out.println("Verification failed");
            }
        }

        // Helper method to get the first child of an element having a given name.
        // If there is no child with the given name it returns null

        private static XdmNode getChild(XdmNode parent, QName childName) {
            XdmSequenceIterator iter = parent.axisIterator(Axis.CHILD, childName);
            if (iter.hasNext()) {
                return (XdmNode) iter.next();
            } else {
                return null;
            }
        }

    }

    /**
     * Demonstrate use of an XPath expression against a DOM source document.
     */

    private static class XPathDOM implements Application.Test {
        public String name() {
            return "XPathDOM";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            // Build the DOM document
            File file = new File("data/books.xml");
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            dfactory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder docBuilder;
            try {
                docBuilder = dfactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new SaxonApiException(e);
            }
            Document doc;
            try {
                doc = docBuilder.parse(new InputSource(file.toURI().toString()));
            } catch (SAXException e) {
                throw new SaxonApiException(e);
            } catch (IOException e) {
                throw new SaxonApiException(e);
            }
            // Compile the XPath Expression
            Processor proc = new Processor(false);
            DocumentBuilder db = proc.newDocumentBuilder();
            XdmNode xdmDoc = db.wrap(doc);
            XPathCompiler xpath = proc.newXPathCompiler();
            XPathExecutable xx = xpath.compile("//ITEM/TITLE");
            // Run the XPath Expression
            XPathSelector selector = xx.load();
            selector.setContextItem(xdmDoc);
            for (XdmItem item : selector) {
                XdmNode node = (XdmNode) item;
                org.w3c.dom.Node element = (org.w3c.dom.Node) node.getExternalNode();
                System.out.println(element.getTextContent());
            }
        }

    }


    private static void handleException(Exception ex) {
        System.out.println("EXCEPTION: " + ex);
        ex.printStackTrace();
    }


}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

