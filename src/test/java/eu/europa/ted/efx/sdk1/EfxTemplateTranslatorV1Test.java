package eu.europa.ted.efx.sdk1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.EfxTestsBase;

class EfxTemplateTranslatorV1Test extends EfxTestsBase {
  @Override
  protected String getSdkVersion() {
    return "eforms-sdk-1.0";
  }

  /*** Template line ***/

  @Test
  void testTemplateLineNoIdent() {
    assertEquals(
        "let block01() -> { text('foo') }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text} foo"));
  }

  /**
   * All nodes that contain any children get an auto generated outline number.
   */
  @Test
  void testTemplateLineOutline_Autogenerated() {
    assertEquals(lines("let block01() -> { #1: text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { #1.1: text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The autogenerated number for a node can be overridden. Leaf nodes don't get an outline number.
   */
  @Test
  void testTemplateLineOutline_Explicit() {
    assertEquals(lines("let block01() -> { #2: text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { #2.3: text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(
            lines("2{BT-00-Text} foo", "\t3{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The autogenerated number for some nodes can be overridden. The other nodes get an auto
   * generated outline number. Leaf nodes don't get an outline number.
   */
  @Test
  void testTemplateLineOutline_Mixed() {
    assertEquals(lines("let block01() -> { #2: text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { #2.1: text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("2{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The outline number can be suppressed for a line if overridden with the value zero. Leaf nodes
   * don't get an outline number.
   */
  @Test
  void testTemplateLineOutline_Suppressed() {
    assertEquals(lines("let block01() -> { #2: text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(
            lines("2{BT-00-Text} foo", "\t0{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The outline number can be suppressed for a line if overridden with the value zero. Child nodes
   * will still get an outline number. Leaf nodes still won't get an outline number.
   */
  @Test
  void testTemplateLineOutline_SuppressedAtParent() {
    // Outline is ignored if the line has no children
    assertEquals(lines("let block01() -> { text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { #1: text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("0{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  @Test
  void testTemplateLineFirstIndented() {
    assertThrows(ParseCancellationException.class, () -> translateTemplate("  {BT-00-Text} foo"));
  }

  @Test
  void testTemplateLineIdentTab() {
    assertEquals(
        lines("let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }", //
            "let block0101() -> { text('bar') }", //
            "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentSpaces() {
    assertEquals(
        lines("let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }", //
            "let block0101() -> { text('bar') }", //
            "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("{BT-00-Text} foo", "    {BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentMixed() {
    final String lines = lines("{BT-00-Text} foo", "\t  {BT-00-Text} bar");
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate(lines));
  }

  @Test
  void testTemplateLineIdentMixedSpaceThenTab() {
    final String lines = lines("{BT-00-Text} foo", "  \t{BT-00-Text} bar");
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate(lines));
  }

  @Test
  void testTemplateLineIdentLower() {
    assertEquals(
        lines("let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }",
            "let block0101() -> { text('bar') }",
            "let block02() -> { text('code') }",
            "for-each(/*/PathNode/TextField).call(block01())",
            "for-each(/*/PathNode/CodeField).call(block02())"),
        translateTemplate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar", "{BT-00-Code} code")));
  }

  @Test
  void testTemplateLineIdentUnexpected() {
    final String lines = lines("{BT-00-Text} foo", "\t\t{BT-00-Text} bar");
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate(lines));
  }

  @Test
  void testTemplateLine_VariableScope() {
    assertEquals(
        lines("let block01() -> { #1: eval(for $x in ./normalize-space(text()) return $x)", //
            "for-each(.).call(block0101()) }", //
            "let block0101() -> { eval(for $x in ./normalize-space(text()) return $x) }", //
            "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("{BT-00-Text} ${for text:$x in BT-00-Text return $x}",
            "    {BT-00-Text} ${for text:$x in BT-00-Text return $x}")));

  }

  /*** Labels ***/

  @Test
  void testStandardLabelReference() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{field|name|BT-00-Text}"));
  }

  @Test
  void testStandardLabelReference_UsingLabelTypeAsAssetId() {
    assertEquals(
        "let block01() -> { label(concat('auxiliary', '|', 'text', '|', 'value')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{auxiliary|text|value}"));
  }

  @Test
  void testStandardLabelReference_WithAssetIdIterator() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in for $t in ./normalize-space(text()) return $t return concat('field', '|', 'name', '|', $item))) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{field|name|${for text:$t in BT-00-Text return $t}}"));
  }

  @Test
  void testShorthandBtLabelReference() {
    assertEquals(
        "let block01() -> { label(concat('business-term', '|', 'name', '|', 'BT-00')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{name|BT-00}"));
  }

  @Test
  void testShorthandFieldLabelReference() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{name|BT-00-Text}"));
  }

  @Test
  void testShorthandBtLabelReference_MissingLabelType() {
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{BT-01}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForIndicator() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ../IndicatorField return concat('indicator', '|', 'when', '-', $item, '|', 'BT-00-Indicator'))) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{BT-00-Indicator}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCode() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ../CodeField/normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{BT-00-Code}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForInternalCode() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ../InternalCodeField/normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{BT-00-Internal-Code}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ../CodeField/@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute_WithSameAttributeInContext() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ../@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nfor-each(/*/PathNode/CodeField/@attribute).call(block01())",
        translateTemplate("{BT-00-CodeAttribute}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute_WithSameElementInContext() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ./@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForText() {
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{BT-00-Text}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForAttribute() {
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{BT-00-Attribute}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndIndicatorField() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Indicator')) }\nfor-each(/*/PathNode/IndicatorField).call(block01())",
        translateTemplate("{BT-00-Indicator}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndCodeField() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Code')) }\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndTextField() {
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{value}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithOtherLabelType() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithUnknownLabelType() {
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{whatever}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithNodeContext() {
    assertEquals(
        "let block01() -> { label(concat('node', '|', 'name', '|', 'ND-Root')) }\nfor-each(/*).call(block01())",
        translateTemplate("{ND-Root}  #{name}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceFromContextField() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ./normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code} #value"));
  }

  @Test
  void testShorthandIndirectLabelReferenceFromContextField_WithNodeContext() {
    assertThrows(ParseCancellationException.class, () -> translateTemplate("{ND-Root} #value"));
  }


  /*** Expression block ***/

  @Test
  void testShorthandFieldValueReferenceFromContextField() {
    assertEquals("let block01() -> { eval(./normalize-space(text())) }\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code} $value"));
  }

  @Test
  void testShorthandFieldValueReferenceFromContextField_WithText() {
    assertEquals(
        "let block01() -> { text('blah ')label(distinct-values(for $item in ./normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item)))text(' ')text('blah ')eval(./normalize-space(text()))text(' ')text('blah') }\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code} blah #value blah $value blah"));
  }

  @Test
  void testShorthandFieldValueReferenceFromContextField_WithNodeContext() {
    assertThrows(ParseCancellationException.class, () -> translateTemplate("{ND-Root} $value"));
  }


  /*** Other ***/

  @Test
  void testNestedExpression() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', ./normalize-space(text()))) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{field|name|${BT-00-Text}}"));
  }

  @Test
  void testEndOfLineComments() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text'))text(' ')text('blah blah') }\nfor-each(/*).call(block01())",
        translateTemplate("{ND-Root} #{name|BT-00-Text} blah blah // comment blah blah"));
  }

  @Test
  void testImplicitFormatting_Dates() {
    assertEquals("let block01() -> { eval(for $item in PathNode/StartDateField/xs:date(text()) return format-date($item, '[D01]/[M01]/[Y0001]')) }\nfor-each(/*).call(block01())", translateTemplate("{ND-Root} ${BT-00-StartDate}"));
  }

  @Test
  void testImplicitFormatting_Times() {
    assertEquals("let block01() -> { eval(for $item in PathNode/StartTimeField/xs:time(text()) return format-time($item, '[H01]:[m01] [Z]')) }\nfor-each(/*).call(block01())", translateTemplate("{ND-Root} ${BT-00-StartTime}"));
  }
}
