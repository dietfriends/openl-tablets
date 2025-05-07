package org.openl.rules.cmatch;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.ColumnMatchMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.impl.OpenMethodHeader;

public class ColumnMatchNodeBinder extends AExecutableNodeBinder<ColumnMatchBoundNode> {
    private static SubTextSourceCodeModule cutNameOfAlgorithm(TableSyntaxNode tsn,
                                                              IOpenSourceCodeModule src,
                                                              int headerTokenLength) throws SyntaxNodeException {
        String s = src.getCode();

        // parse '<ALGORITHM>' if it exists
        int p2 = s.indexOf('>');
        if (p2 < 0) {
            return null;
        }

        int p1 = s.indexOf('<');
        for (int i = headerTokenLength; i < p1; i++) {
            if (s.charAt(i) != ' ') {
                // illegal character detected
                p1 = -1;
                break;
            }
        }

        if (p1 < 0 || p1 > p2) {
            throw SyntaxNodeExceptionUtils.createError("Illegal header format.", null, tsn);
        }

        return new SubTextSourceCodeModule(src, p1 + 1, p2);
    }

    @Override
    protected MetaInfoReader createMetaInfoReader(ColumnMatchBoundNode node) {
        return new ColumnMatchMetaInfoReader(node);
    }

    @Override
    public IOpenSourceCodeModule createHeaderSource(TableSyntaxNode tableSyntaxNode,
                                                    IBindingContext bindingContext) throws SyntaxNodeException {
        IGridTable table = tableSyntaxNode.getGridTable();

        IOpenSourceCodeModule src = new GridCellSourceCodeModule(table, bindingContext);

        int headerTokenLength = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier().length();

        var nameOfAlgorithm = cutNameOfAlgorithm(tableSyntaxNode, src, headerTokenLength);
        if (nameOfAlgorithm != null) {
            String name = nameOfAlgorithm.getCode();
            // TODO
            // headerTokenLength = name.getEndPosition() + 1;
            headerTokenLength = nameOfAlgorithm.getStartPosition() + name.length() + 1;
        }

        return new SubTextSourceCodeModule(src, headerTokenLength);
    }

    @Override
    protected ColumnMatchBoundNode createNode(TableSyntaxNode tsn,
                                              OpenL openl,
                                              OpenMethodHeader header,
                                              XlsModuleOpenClass module,
                                              IBindingContext context) {
        IGridTable table = tsn.getGridTable();
        int headerTokenLength = tsn.getHeader().getHeaderToken().getIdentifier().length();
        IOpenSourceCodeModule nameOfAlgorithm;
        try {
            nameOfAlgorithm = cutNameOfAlgorithm(tsn, new GridCellSourceCodeModule(table, context), headerTokenLength);
        } catch (SyntaxNodeException e) {
            throw new RuntimeException(e);
        }
        return new ColumnMatchBoundNode(tsn, openl, header, module, nameOfAlgorithm);
    }
}
