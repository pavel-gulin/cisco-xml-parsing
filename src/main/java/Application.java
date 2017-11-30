import net.sf.saxon.s9api.*;
import java.io.*;

public class Application {
    private Application() {
    }
    public static void main(String[] argv) {
        if (argv.length !=2) {
            System.out.println("Usage: <input file name in format: file:/// with forward slashes> <output file name with backward slashes>");
            System.out.println("For example: file:///D:/2_Work/Companies/Digicel/Config/Trinidad_and_Tobago/TT-ASR920-T043-Ana-Street.txt D:\\2_Work\\Companies\\Digicel\\Config\\Trinidad_and_Tobago\\Output.html");
            return;
        }
        String inputFileName = argv[0];
        String outputFileName = argv[1];
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
                            "return <tr><td>{$prod/Param/text()}</td>" +
                            "<td>{$prod/ConfigIf-Configuration/description/UpTo200CharactersInterface/text()}</td>" +
                            "<td>{$prod/ConfigIf-Configuration/vrf/forwarding/VRFName/text()}</td>" +
                            "<td>{$prod/ConfigIf-Configuration/ip/mtu/MTUBytes/text()}</td>" +
                            "<td>{$prod/ConfigIf-Configuration/ip/address/IPAddress/text()}</td>" +
                            "<td>{$prod/ConfigIf-Configuration/ip/address/IPSubnetMask/text()}</td>" +
                            "</tr>");
            XQueryEvaluator qe = exp.load();
            qe.setExternalVariable(new QName("dc"), new XdmAtomicValue(inputFileName));
            XdmValue result = qe.evaluate();
            PrintWriter out = new PrintWriter(outputFileName);
            String stringResult=htmlStart + result + htmlEnd;
            out.write(stringResult);
            out.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
