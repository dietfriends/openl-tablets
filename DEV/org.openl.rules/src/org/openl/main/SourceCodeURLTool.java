/*
 * Created on Dec 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.main;

import java.io.PrintWriter;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.CompositeSourceCodeModule;
import org.openl.util.StringUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

/**
 * @author snshor
 */
public final class SourceCodeURLTool {
    private static final Logger LOG = LoggerFactory.getLogger(SourceCodeURLTool.class);

    private SourceCodeURLTool() {
    }

    private static final Pattern NEW_LINE = Pattern.compile("[\r\n]+");

    public static String makeSourceLocationURL(ILocation location, IOpenSourceCodeModule module) {

        if (module != null && StringUtils.isEmpty(module.getUri())) {
            return StringUtils.EMPTY;
        }

        int start = -1;
        int end = -1;

        String lineInfo = null;

        if (module == null) {
            return "NO_MODULE";
        }

        if (location != null && location.isTextLocation()) {
            String src = module.getCode();
            TextInfo info = new TextInfo(src);
            try {
                start = location.getStart().getAbsolutePosition(info) + module.getStartPosition();
                end = location.getEnd().getAbsolutePosition(info) + module.getStartPosition();
            } catch (UnsupportedOperationException e) {
                LOG.warn("Cannot make source location URL", e);
            }

            lineInfo = "start=" + start + "&end=" + end;

        }

        String moduleUri = getUri(module, location);

        String suffix = !moduleUri.contains("?") ? "?" : "&";

        String url = moduleUri;
        if (lineInfo != null) {
            url += suffix + lineInfo;
        } else if (location != null) {
            url += suffix + location;
        }

        return url;
    }

    private static String getUri(IOpenSourceCodeModule module, ILocation location) {
        String moduleUri;
        if (module instanceof CompositeSourceCodeModule && location != null && location.isTextLocation()) {
            int line = location.getStart().getLine(new TextInfo(module.getCode()));

            IOpenSourceCodeModule[] modules = ((CompositeSourceCodeModule) module).getModules();
            if (modules.length <= line || line < 0) {
                // Occurs when Method table expression has several lines but reside inside single cell.
                LOG.debug("Modules count in composite module are less than error line number. Return first found module uri.");
                moduleUri = module.getUri();
            } else {
                // Occurs when Method table expression has several lines and each line resides inside his own cell.
                IOpenSourceCodeModule actualModule = modules[line];
                moduleUri = actualModule == null ? module.getUri() : actualModule.getUri();
            }
        } else {
            moduleUri = module.getUri();
        }
        return moduleUri;
    }

    /**
     * Prints source code fragment and underlines a fragment where an error had been occurred
     *
     * <p>
     * Openl Code Fragment:<br>
     * =======================<br>
     * Double getRAWPremium(Policy p)<br>
     * ^^^^^^<br>
     * =======================<br>
     * </p>
     *
     * @param location source location
     * @param src      source code
     * @param pw       writer
     */
    public static void printCodeAndError(ILocation location, String src, PrintWriter pw) {

        if (location == null) {
            return;
        }

        if (!location.isTextLocation()) {
            return;
        }

        TextInfo info = new TextInfo(src);
        String[] lines = NEW_LINE.split(src);

        pw.print("Openl Code Fragment:\r\n");
        pw.print("=======================\r\n");

        int line1 = location.getStart().getLine(info);
        int column1 = location.getStart().getColumn(info);

        int line2 = location.getEnd().getLine(info);
        int column2 = location.getEnd().getColumn(info);

        int start = Math.max(line1 - 2, 0);

        int end = Math.min(start + 4, lines.length);

        for (int i = start; i < end; ++i) {
            String line = lines[i].replace('\t', ' ');
            pw.print(line);
            pw.print("\r\n");
            if (i == line1) {
                for (int i2 = 0; i2 < column1 - 1; i2++) {
                    pw.print(' ');
                }
                int col2 = line1 == line2 ? (column2 + 1) : line.length();

                for (int i3 = 0; i3 < col2 - column1; i3++) {
                    pw.print('^');
                }
                pw.print("\r\n");
            }
        }
        pw.print("=======================\r\n");

    }

    public static void printSourceLocation(String sourceLocation, PrintWriter pw) {
        if (!StringUtils.isEmpty(sourceLocation)) {
            pw.print("    at ");
            pw.print(sourceLocation);
            pw.print("\r\n");
        }
    }

}
