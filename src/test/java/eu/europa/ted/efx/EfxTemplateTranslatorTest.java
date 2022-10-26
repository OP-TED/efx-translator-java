package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.mock.DependencyFactoryMock;

class EfxTemplateTranslatorTest {
  final private String SDK_VERSION = "eforms-sdk-1.0";

  private String translate(final String template) {
    try {
      return EfxTranslator.translateTemplate(DependencyFactoryMock.INSTANCE,
          SDK_VERSION, template + "\n");
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  private static String lines(String... lines) {
    return String.join("\n", lines);
  }

  /*** Template line ***/

  @Test
  void testTemplateLineNoIdent() {
    assertEquals("declare block01 = { text('foo') }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text} foo"));
  }

  /**
   * All nodes that contain any children get an auto generated outline number.
   */
  @Test
  void testTemplateLineOutline_Autogenerated() {
    assertEquals(lines("declare block01 = { outline('1') text('foo')",
    "for-each(../..).call(block0101) }",
    "declare block0101 = { outline('1.1') text('bar')",
    "for-each(PathNode/NumberField).call(block010101) }",
    "declare block010101 = { text('foo') }",
    "for-each(/*/PathNode/TextField).call(block01)"), //
        translate(lines("{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The autogenerated number for a node can be overridden.
   * Leaf nodes don't get an outline number.
   */
  @Test
  void testTemplateLineOutline_Explicit() {
    assertEquals(lines("declare block01 = { outline('2') text('foo')",
    "for-each(../..).call(block0101) }",
    "declare block0101 = { outline('2.3') text('bar')",
    "for-each(PathNode/NumberField).call(block010101) }",
    "declare block010101 = { text('foo') }",
    "for-each(/*/PathNode/TextField).call(block01)"), //
        translate(lines("2{BT-00-Text} foo", "\t3{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The autogenerated number for some nodes can be overridden.
   * The other nodes get an auto generated outline number.
   * Leaf nodes don't get an outline number.
   */
  @Test
  void testTemplateLineOutline_Mixed() {
    assertEquals(lines("declare block01 = { outline('2') text('foo')",
    "for-each(../..).call(block0101) }",
    "declare block0101 = { outline('2.1') text('bar')",
    "for-each(PathNode/NumberField).call(block010101) }",
    "declare block010101 = { text('foo') }",
    "for-each(/*/PathNode/TextField).call(block01)"), //
        translate(lines("2{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The outline number can be suppressed for a line if overridden with the value zero.
   * Leaf nodes don't get an outline number.
   */
  @Test
  void testTemplateLineOutline_Suppressed() {
    assertEquals(lines("declare block01 = { outline('2') text('foo')",
    "for-each(../..).call(block0101) }",
    "declare block0101 = { text('bar')",
    "for-each(PathNode/NumberField).call(block010101) }",
    "declare block010101 = { text('foo') }",
    "for-each(/*/PathNode/TextField).call(block01)"), //
        translate(lines("2{BT-00-Text} foo", "\t0{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The outline number can be suppressed for a line if overridden with the value zero. Child nodes
   * will still get an outline number. Leaf nodes still won't get an outline number.
   */
  @Test
  void testTemplateLineOutline_SuppressedAtParent() {
    // Outline is ignored if the line has no children
    assertEquals(lines("declare block01 = { text('foo')",
    "for-each(../..).call(block0101) }",
    "declare block0101 = { outline('1') text('bar')",
    "for-each(PathNode/NumberField).call(block010101) }",
    "declare block010101 = { text('foo') }",
    "for-each(/*/PathNode/TextField).call(block01)"), //
        translate(lines("0{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  @Test
  void testTemplateLineFirstIndented() {
    assertThrows(ParseCancellationException.class, () -> translate("  {BT-00-Text} foo"));
  }

  @Test
  void testTemplateLineIdentTab() {
    assertEquals(
        lines("declare block01 = { outline('1') text('foo')", "for-each(.).call(block0101) }", //
        "declare block0101 = { text('bar') }", //
        "for-each(/*/PathNode/TextField).call(block01)"),//
        translate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentSpaces() {
    assertEquals(
        lines("declare block01 = { outline('1') text('foo')", "for-each(.).call(block0101) }", //
        "declare block0101 = { text('bar') }", //
        "for-each(/*/PathNode/TextField).call(block01)"),//
        translate(lines("{BT-00-Text} foo", "    {BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentMixed() {
    assertThrows(ParseCancellationException.class,
        () -> translate(lines("{BT-00-Text} foo", "\t  {BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentMixedSpaceThenTab() {
    assertThrows(ParseCancellationException.class,
        () -> translate(lines("{BT-00-Text} foo", "  \t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentLower() {
    assertEquals(
        lines("declare block01 = { outline('1') text('foo')", "for-each(.).call(block0101) }",
        "declare block0101 = { text('bar') }",
        "declare block02 = { text('code') }",
        "for-each(/*/PathNode/TextField).call(block01)",
        "for-each(/*/PathNode/CodeField).call(block02)"),
        translate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar", "{BT-00-Code} code")));
  }

  @Test
  void testTemplateLineIdentUnexpected() {
    assertThrows(ParseCancellationException.class,
        () -> translate(lines("{BT-00-Text} foo", "\t\t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLine_VariableScope() {
    assertEquals(
        lines("declare block01 = { outline('1') eval(for $x in . return $x)", //
        "for-each(.).call(block0101) }", //
        "declare block0101 = { eval(for $x in . return $x) }", //
        "for-each(/*/PathNode/TextField).call(block01)"),//
        translate(lines("{BT-00-Text} ${for text:$x in BT-00-Text return $x}", "    {BT-00-Text} ${for text:$x in BT-00-Text return $x}")));

  }


  /*** Labels ***/

  @Test
  void testStandardLabelReference() {
    assertEquals(
        "declare block01 = { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{field|name|BT-00-Text}"));
  }

  @Test
  void testStandardLabelReference_UsingLabelTypeAsAssetId() {
    assertEquals(
        "declare block01 = { label(concat('auxiliary', '|', 'text', '|', 'value')) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{auxiliary|text|value}"));
  }

  @Test
  void testShorthandBtLabelReference() {
    assertEquals(
        "declare block01 = { label(concat('business-term', '|', 'name', '|', 'BT-00')) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{name|BT-00}"));
  }

  @Test
  void testShorthandFieldLabelReference() {
    assertEquals(
        "declare block01 = { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{name|BT-00-Text}"));
  }

  @Test
  void testShorthandBtLabelReference_MissingLabelType() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{BT-01}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForIndicator() {
    assertEquals(
        "declare block01 = { label(for $item in ../IndicatorField return concat('indicator', '|', 'when', '-', $item, '|', 'BT-00-Indicator')) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{BT-00-Indicator}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCode() {
    assertEquals(
        "declare block01 = { label(for $item in ../CodeField return concat('code', '|', 'name', '|', 'main-activity', '.', $item)) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{BT-00-Code}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForInternalCode() {
    assertEquals(
        "declare block01 = { label(for $item in ../InternalCodeField return concat('code', '|', 'name', '|', 'main-activity', '.', $item)) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{BT-00-Internal-Code}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute() {
    assertEquals(
        "declare block01 = { label(for $item in ../CodeField/@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item)) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute_WithSameAttributeInContext() {
    assertEquals(
        "declare block01 = { label(for $item in ../@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item)) }\nfor-each(/*/PathNode/CodeField/@attribute).call(block01)",
        translate("{BT-00-CodeAttribute}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute_WithSameElementInContext() {
    assertEquals(
        "declare block01 = { label(for $item in ./@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item)) }\nfor-each(/*/PathNode/CodeField).call(block01)",
        translate("{BT-00-Code}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForText() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{BT-00-Text}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForAttribute() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{BT-00-Attribute}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndIndicatorField() {
    assertEquals(
        "declare block01 = { label(concat('field', '|', 'name', '|', 'BT-00-Indicator')) }\nfor-each(/*/PathNode/IndicatorField).call(block01)",
        translate("{BT-00-Indicator}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndCodeField() {
    assertEquals(
        "declare block01 = { label(concat('field', '|', 'name', '|', 'BT-00-Code')) }\nfor-each(/*/PathNode/CodeField).call(block01)",
        translate("{BT-00-Code}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndTextField() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{value}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithOtherLabelType() {
    assertEquals(
        "declare block01 = { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithUnknownLabelType() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{whatever}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithNodeContext() {
    assertEquals(
        "declare block01 = { label(concat('node', '|', 'name', '|', 'ND-Root')) }\nfor-each(/*).call(block01)",
        translate("{ND-Root}  #{name}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceFromContextField() {
    assertEquals(
        "declare block01 = { label(for $item in . return concat('code', '|', 'name', '|', 'main-activity', '.', $item)) }\nfor-each(/*/PathNode/CodeField).call(block01)",
        translate("{BT-00-Code} #value"));
  }

  @Test
  void testShorthandIndirectLabelReferenceFromContextField_WithNodeContext() {
    assertThrows(ParseCancellationException.class, () -> translate("{ND-Root} #value"));
  }


  /*** Expression block ***/

  @Test
  void testShorthandFieldValueReferenceFromContextField() {
    assertEquals("declare block01 = { eval(.) }\nfor-each(/*/PathNode/CodeField).call(block01)",
        translate("{BT-00-Code} $value"));
  }

  @Test
  void testShorthandFieldValueReferenceFromContextField_WithText() {
    assertEquals(
        "declare block01 = { text('blah ')label(for $item in . return concat('code', '|', 'name', '|', 'main-activity', '.', $item))text(' ')text('blah ')eval(.)text(' ')text('blah') }\nfor-each(/*/PathNode/CodeField).call(block01)",
        translate("{BT-00-Code} blah #value blah $value blah"));
  }

  @Test
  void testShorthandFieldValueReferenceFromContextField_WithNodeContext() {
    assertThrows(ParseCancellationException.class, () -> translate("{ND-Root} $value"));
  }


  /*** Other ***/

  @Test
  void testNestedExpression() {
    assertEquals(
        "declare block01 = { label(concat('field', '|', 'name', '|', ./normalize-space(text()))) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{field|name|${BT-00-Text}}"));
  }

  @Test
  void testEndOfLineComments() {
    assertEquals(
        "declare block01 = { label(concat('field', '|', 'name', '|', 'BT-00-Text'))text(' ')text('blah blah') }\nfor-each(/*).call(block01)",
        translate("{ND-Root} #{name|BT-00-Text} blah blah // comment blah blah"));
  }
}
