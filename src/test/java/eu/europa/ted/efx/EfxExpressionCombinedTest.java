package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.model.DecimalFormat;

/**
 * Test for EFX expressions that combine several aspects of the language.
 */
class EfxExpressionCombinedTest {
  final private String SDK_VERSION = "eforms-sdk-1.0";
  final private TranslatorOptions TRANSLATOR_OPTIONS = new EfxTranslatorOptions(DecimalFormat.EFX_DEFAULT);

  private String test(final String context, final String expression) {
    try {
      return EfxTranslator.translateExpression(DependencyFactoryMock.INSTANCE,
          SDK_VERSION, String.format("{%s} ${%s}", context, expression), TRANSLATOR_OPTIONS);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testNotAnd() {
    assertEquals("not(1 = 2) and (2 = 2)", test("BT-00-Text", "not(1 == 2) and (2 == 2)"));
  }

  @Test
  void testNotPresentAndNotPresent() {
    assertEquals("not(PathNode/TextField) and not(PathNode/IntegerField)",
        test("ND-Root", "BT-00-Text is not present and BT-00-Integer is not present"));
  }

  @Test
  void testCountWithNodeContextOverride() {
    assertEquals("count(../../PathNode/CodeField/normalize-space(text())) = 1",
        test("BT-00-Text", "count(ND-Root::BT-00-Code) == 1"));
  }

  @Test
  void testCountWithAbsoluteFieldReference() {
    assertEquals("count(/*/PathNode/CodeField/normalize-space(text())) = 1", test("BT-00-Text", "count(/BT-00-Code) == 1"));
  }

  @Test
  void testCountWithAbsoluteFieldReferenceAndPredicate() {
    assertEquals("count(/*/PathNode/CodeField[../IndicatorField = true()]/normalize-space(text())) = 1",
        test("BT-00-Text", "count(/BT-00-Code[BT-00-Indicator == TRUE]) == 1"));
  }
}
