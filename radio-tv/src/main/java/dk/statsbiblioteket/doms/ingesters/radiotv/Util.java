package dk.statsbiblioteket.doms.ingesters.radiotv;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;
import java.text.MessageFormat;

/**
 * Utility methods created for the Radio TV doms ingester
 */
public class Util {

    /**
     * Format a string for a more useful doms AUDIT trail comment.
     * The message includes the version of this doms ingester, taken from the pom.xml file jar plugin
     *
     * @param filename the name of the preingest file that drove this change
     * @param action   the thing done
     * @return the formatted comment
     */
    public static String domsCommenter(String filename, String action, Object... args) {
        String version = Util.class.getPackage().getImplementationVersion();
        //String escapedAction = action.replaceAll("'\\{", "''{").replaceAll("}'","}''"); //Add a level of pings due to message formatting....
        String escapedAction = action.replaceAll("'", "''");
        MessageFormat form = new MessageFormat(escapedAction);
        String formattedAction = form.format(args);
        return "RadioTV Digitv Ingester (" + version + ") " + formattedAction + " as part of ingest of " + filename;
    }

    /**
     * Diff two xml files. It allows for differences in namespace prefixes, comments and whitespaces
     * @param control the control document
     * @param test the test document
     * @return the difference between the documents
     */
    public static Diff xmlDiff(Source control, Source test) {
        return DiffBuilder
                .compare(control)
                .withTest(test)
                .checkForIdentical()
                .ignoreComments()
                .ignoreWhitespace()
                .withDifferenceEvaluator(
                        (Comparison comparison, ComparisonResult outcome) -> {
                            if (comparison.getType().equals(ComparisonType.NAMESPACE_PREFIX)) {
                                return ComparisonResult.EQUAL;
                            } else {
                                return outcome;
                            }
                        })
                .build();
    }
}
