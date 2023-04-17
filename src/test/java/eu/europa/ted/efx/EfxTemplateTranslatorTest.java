package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.mock.DependencyFactoryMock;

class EfxTemplateTranslatorTest {
  final private String SDK_VERSION = "eforms-sdk-2.0";

  private String translate(final String template) {
    try {
      return EfxTranslator.translateTemplate(DependencyFactoryMock.INSTANCE,
          SDK_VERSION, template + "\n");
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  private String lines(String... lines) {
    return String.join("\n", lines);
  }

  /*** Template line ***/

  @Test
  void testTemplateLineNoIdent() {
    assertEquals(
        "let block01() -> { text('foo') }\nfor-each(/*/PathNode/TextField).call(block01())",
        translate("{BT-00-Text} foo"));
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
        translate(lines("{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
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
        translate(lines("2{BT-00-Text} foo", "\t3{ND-Root} bar", "\t\t{BT-00-Number} foo")));
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
        translate(lines("2{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
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
        translate(lines("2{BT-00-Text} foo", "\t0{ND-Root} bar", "\t\t{BT-00-Number} foo")));
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
        translate(lines("0{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  @Test
  void testTemplateLineFirstIndented() {
    assertThrows(ParseCancellationException.class, () -> translate("  {BT-00-Text} foo"));
  }

  @Test
  void testTemplateLineIdentTab() {
    assertEquals(
        lines("let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }", //
            "let block0101() -> { text('bar') }", //
            "for-each(/*/PathNode/TextField).call(block01())"), //
        translate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentSpaces() {
    assertEquals(
        lines("let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }", //
            "let block0101() -> { text('bar') }", //
            "for-each(/*/PathNode/TextField).call(block01())"), //
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
        lines("let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }",
            "let block0101() -> { text('bar') }",
            "let block02() -> { text('code') }",
            "for-each(/*/PathNode/TextField).call(block01())",
            "for-each(/*/PathNode/CodeField).call(block02())"),
        translate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar", "{BT-00-Code} code")));
  }

  @Test
  void testTemplateLineIdentUnexpected() {
    assertThrows(ParseCancellationException.class,
        () -> translate(lines("{BT-00-Text} foo", "\t\t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineJoining() {
    assertEquals(
        lines("let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }",
            "let block0101() -> { text('bar joined more') }",
            "let block02() -> { text('code') }",
            "for-each(/*/PathNode/TextField).call(block01())",
            "for-each(/*/PathNode/CodeField).call(block02())"),
        translate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar \\ \n  joined \\\n\\\nmore",
            "{BT-00-Code} code")));
  }

  @Test
  void testTemplateLine_VariableScope() {
    assertEquals(
        lines("let block01() -> { #1: eval(for $x in . return $x)", //
            "for-each(.).call(block0101()) }", //
            "let block0101() -> { eval(for $x in . return $x) }", //
            "for-each(/*/PathNode/TextField).call(block01())"), //
        translate(lines("{BT-00-Text} ${for text:$x in BT-00-Text return $x}",
            "    {BT-00-Text} ${for text:$x in BT-00-Text return $x}")));

  }

  @Test
  void testTemplateLine_ContextVariable() {
    assertEquals(
        lines("let block01(t, ctx) -> { #1: eval(for $x in . return concat($x, $t))", //
            "for-each(.).call(block0101(ctx:$ctx, t:$t, t2:'test'))", //
            "for-each(.).call(block0102(ctx:$ctx, t:$t, t2:'test3')) }", //
            "let block0101(t, ctx, t2) -> { #1.1: eval(for $y in . return concat($y, $t, $t2))", //
            "for-each(.).call(block010101(ctx:$ctx, t:$t, t2:$t2))", //
            "for-each(.).call(block010102(ctx:$ctx, t:$t, t2:$t2)) }", //
            "let block010101(t, ctx, t2) -> { eval(for $z in . return concat($z, $t, $ctx)) }", //
            "let block010102(t, ctx, t2) -> { eval(for $z in . return concat($z, $t, $ctx)) }", //
            "let block0102(t, ctx, t2) -> { eval(for $z in . return concat($z, $t2, $ctx)) }", //
            "for-each(/*/PathNode/TextField).call(block01(ctx:., t:./normalize-space(text())))"), //
        translate(lines(
            "{context:$ctx = BT-00-Text, text:$t = BT-00-Text} ${for text:$x in BT-00-Text return concat($x, $t)}",
            "    {BT-00-Text, text:$t2 = 'test'} ${for text:$y in BT-00-Text return concat($y, $t, $t2)}",
            "        {BT-00-Text} ${for text:$z in BT-00-Text return concat($z, $t, $ctx)}",
            "        {BT-00-Text} ${for text:$z in BT-00-Text return concat($z, $t, $ctx)}",
            "    {BT-00-Text, text:$t2 = 'test3'} ${for text:$z in BT-00-Text return concat($z, $t2, $ctx)}")));

  }


  /*** Labels ***/

  @Test
  void testStandardLabelReference() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translate("{BT-00-Text}  #{field|name|BT-00-Text}"));
  }

  @Test
  void testStandardLabelReference_UsingLabelTypeAsAssetId() {
    assertEquals(
        "let block01() -> { label(concat('auxiliary', '|', 'text', '|', 'value')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translate("{BT-00-Text}  #{auxiliary|text|value}"));
  }

  @Test
  void testShorthandBtLabelReference() {
    assertEquals(
        "let block01() -> { label(concat('business-term', '|', 'name', '|', 'BT-00')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translate("{BT-00-Text}  #{name|BT-00}"));
  }

  @Test
  void testShorthandFieldLabelReference() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translate("{BT-00-Text}  #{name|BT-00-Text}"));
  }

  @Test
  void testShorthandBtLabelReference_MissingLabelType() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{BT-01}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForIndicator() {
    assertEquals(
        "let block01() -> { label(for $item in ../IndicatorField return concat('indicator', '|', 'when', '-', $item, '|', 'BT-00-Indicator')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translate("{BT-00-Text}  #{BT-00-Indicator}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCode() {
    assertEquals(
        "let block01() -> { label(for $item in ../CodeField return concat('code', '|', 'name', '|', 'main-activity', '.', $item)) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translate("{BT-00-Text}  #{BT-00-Code}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForInternalCode() {
    assertEquals(
        "let block01() -> { label(for $item in ../InternalCodeField return concat('code', '|', 'name', '|', 'main-activity', '.', $item)) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translate("{BT-00-Text}  #{BT-00-Internal-Code}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute() {
    assertEquals(
        "let block01() -> { label(for $item in ../CodeField/@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item)) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translate("{BT-00-Text}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute_WithSameAttributeInContext() {
    assertEquals(
        "let block01() -> { label(for $item in ../@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item)) }\nfor-each(/*/PathNode/CodeField/@attribute).call(block01())",
        translate("{BT-00-CodeAttribute}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute_WithSameElementInContext() {
    assertEquals(
        "let block01() -> { label(for $item in ./@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item)) }\nfor-each(/*/PathNode/CodeField).call(block01())",
        translate("{BT-00-Code}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForText() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{BT-00-Text}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForAttribute() {
    assertThrows(ParseCancellationException.class,
        () -> translate("{BT-00-Text}  #{BT-00-Attribute}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndIndicatorField() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Indicator')) }\nfor-each(/*/PathNode/IndicatorField).call(block01())",
        translate("{BT-00-Indicator}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndCodeField() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Code')) }\nfor-each(/*/PathNode/CodeField).call(block01())",
        translate("{BT-00-Code}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndTextField() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{value}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithOtherLabelType() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translate("{BT-00-Text}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithUnknownLabelType() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{whatever}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithNodeContext() {
    assertEquals(
        "let block01() -> { label(concat('node', '|', 'name', '|', 'ND-Root')) }\nfor-each(/*).call(block01())",
        translate("{ND-Root}  #{name}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceFromContextField() {
    assertEquals(
        "let block01() -> { label(for $item in . return concat('code', '|', 'name', '|', 'main-activity', '.', $item)) }\nfor-each(/*/PathNode/CodeField).call(block01())",
        translate("{BT-00-Code} #value"));
  }

  @Test
  void testShorthandIndirectLabelReferenceFromContextField_WithNodeContext() {
    assertThrows(ParseCancellationException.class, () -> translate("{ND-Root} #value"));
  }


  /*** Expression block ***/

  @Test
  void testShorthandFieldValueReferenceFromContextField() {
    assertEquals("let block01() -> { eval(.) }\nfor-each(/*/PathNode/CodeField).call(block01())",
        translate("{BT-00-Code} $value"));
  }

  @Test
  void testShorthandFieldValueReferenceFromContextField_WithText() {
    assertEquals(
        "let block01() -> { text('blah ')label(for $item in . return concat('code', '|', 'name', '|', 'main-activity', '.', $item))text(' ')text('blah ')eval(.)text(' ')text('blah') }\nfor-each(/*/PathNode/CodeField).call(block01())",
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
        "let block01() -> { label(concat('field', '|', 'name', '|', ./normalize-space(text()))) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translate("{BT-00-Text}  #{field|name|${BT-00-Text}}"));
  }

  @Test
  void testEndOfLineComments() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text'))text(' ')text('blah blah') }\nfor-each(/*).call(block01())",
        translate("{ND-Root} #{name|BT-00-Text} blah blah // comment blah blah"));
  }
}
