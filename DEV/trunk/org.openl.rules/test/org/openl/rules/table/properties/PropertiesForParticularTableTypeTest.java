package org.openl.rules.table.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

import org.openl.rules.enumeration.RegionsEnum;
import org.openl.rules.enumeration.UsregionsEnum;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.inherit.InheritanceLevel;

public class PropertiesForParticularTableTypeTest extends BaseOpenlBuilderHelper {
    
    private static String __src = "test/rules/PropertiesForParticularTableType.xls";
    
    public PropertiesForParticularTableTypeTest() {
        super(__src);        
    }
    
    @Test
    public void testErrorParsing() {
        String tableName = "Rules void hello1(int hour)";
        TableSyntaxNode[] tsns = getTableSyntaxNodes();
        TableSyntaxNode resultTsn = findTable(tableName, tsns);
        if (resultTsn != null) {
            ITableProperties tableProperties  = resultTsn.getTableProperties();
            assertNotNull(tableProperties);
            
            assertTrue("Properties failed to load due the error", tableProperties.getAllProperties().isEmpty());
            
            assertEquals("There is one binding error", 1, getJavaWrapper().getCompiledClass().getBindingErrors().length);
            
            assertEquals("Property with name [scope] can`t be defined on the " +
            		"[Table] level.", 
            		getJavaWrapper().getCompiledClass().getBindingErrors()[0].getOriginalCause().getMessage());
            
            } else {
                fail();
        }
    }
    
    @Test
    public void testNotProcessingInheritPropertiesForTableType() {
        String tableName = "Rules void hello2(int hour)";
        TableSyntaxNode[] tsns = getTableSyntaxNodes();
        TableSyntaxNode resultTsn = findTable(tableName, tsns);
        if (resultTsn != null) {
            ITableProperties tableProperties  = resultTsn.getTableProperties();
            assertNotNull(tableProperties);
                
            Map<String, Object> categoryProperties = tableProperties.getPropertiesAppliedForCategory();                
            assertTrue(categoryProperties.size() == 5);
            // check that we have all properties from category level
            assertEquals(InheritanceLevel.CATEGORY.getDisplayName(),(String) categoryProperties.get("scope"));
            assertEquals("My Category",(String) categoryProperties.get("category"));
            assertEquals("newLob",(String) categoryProperties.get("lob"));
            assertEquals(UsregionsEnum.SE.name(), ((UsregionsEnum)categoryProperties.get("usregion")).name());                
            assertEquals(RegionsEnum.NCSA.name(),((RegionsEnum) categoryProperties.get("region")).name());
                
            Map<String, Object> allProperties = tableProperties.getAllProperties();
            assertEquals("AllProperties size is 10, ignore property 'scope' and including default properties", 
                    10, allProperties.size());
            assertTrue("There is no property 'scope' applied for this table, as it can`t be defined in such table type",
                    !allProperties.containsKey("scope"));            
            
            } else {
                fail();
        }
    }
    
}
