package datadog.trace.api.iast

import datadog.trace.test.util.DDSpecification
import groovy.transform.Canonical

class InstrumentationBridgeTest extends DDSpecification {
  @Canonical
  static class BridgeMethod {
    String bridgeMethod
    List<Object> params
    String moduleMethod

    String toString() {
      "bridge method $bridgeMethod"
    }
  }

  private final static BRIDGE_METHODS = [
    new BridgeMethod('onCipherGetInstance', ['algo'], 'onCipherAlgorithm'),
    new BridgeMethod('onMessageDigestGetInstance', ['algo'], 'onHashingAlgorithm'),
    new BridgeMethod('onJdbcQuery', ['my query'], 'onJdbcQuery'),
  ]

  void '#bridgeMethod does not fail when module is not set'() {
    setup:
    InstrumentationBridge.registerIastModule null

    when:
    InstrumentationBridge."${bridgeMethod.bridgeMethod}"(*bridgeMethod.params)

    then:
    noExceptionThrown()

    where:
    bridgeMethod << BRIDGE_METHODS
  }

  void '#bridgeMethod delegates to the module'() {
    setup:
    def exception
    def module = Mock(IastModule)
    InstrumentationBridge.registerIastModule module

    when:
    InstrumentationBridge."${bridgeMethod.bridgeMethod}"(*bridgeMethod.params)

    then:
    1 * module."${bridgeMethod.moduleMethod}"(*_) >> { List args ->
      try {
        args.size().times { assert args[it].is(bridgeMethod.params[it]) }
      } catch (Throwable t) {
        exception = t
      }
    }
    0 * _
    exception == null

    where:
    bridgeMethod << BRIDGE_METHODS
  }

  void '#bridgeMethod leaks no exceptions'() {
    setup:
    def module = Mock(IastModule)
    InstrumentationBridge.registerIastModule module

    when:
    InstrumentationBridge."${bridgeMethod.bridgeMethod}"(*bridgeMethod.params)

    then:
    1 * module."${bridgeMethod.moduleMethod}"(*_) >> { throw new Throwable('should not leak') }
    0 * _
    noExceptionThrown()

    where:
    bridgeMethod << BRIDGE_METHODS
  }

  def "bridge calls module when onParameterName"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onParameterName('AES')

    then:
    1 * module.onParameterName('AES')
  }

  def "bridge calls module when onParameterValue"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onParameterValue('KEY','AES')

    then:
    1 * module.onParameterValue('KEY','AES')
  }

  def "bridge calls don't leak exceptions when onParameterName"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onParameterName("Pepito")

    then:
    1 * module.onParameterName(_) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when onParameterValue"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onParameterValue("pepito", "juanito")

    then:
    1 * module.onParameterValue(_, _) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls module when a new string concat is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringConcat('Hello ', 'World!', 'Hello World!')

    then:
    1 * module.onStringConcat('Hello ', 'World!', 'Hello World!')
  }

  def "bridge calls don't fail with null module when a string concat is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)

    when:
    InstrumentationBridge.onStringConcat('Hello ', 'World!', 'Hello World!')

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a string concat is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringConcat('Hello ', 'World!', 'Hello World!')

    then:
    1 * module.onStringConcat(_, _, _) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls module when a new string builder init is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)
    final self = new StringBuilder()

    when:
    InstrumentationBridge.onStringBuilderInit(self, 'test')

    then:
    1 * module.onStringBuilderInit(self, 'test')
  }

  def "bridge calls don't fail with null module when a string builder init is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)

    when:
    InstrumentationBridge.onStringBuilderInit(new StringBuilder(), 'test')

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a string builder init is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringBuilderInit(new StringBuilder(), 'test')

    then:
    1 * module.onStringBuilderInit(_, _) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls module when a new string builder append is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)
    final self = new StringBuilder()

    when:
    InstrumentationBridge.onStringBuilderAppend(self, 'test')

    then:
    1 * module.onStringBuilderAppend(self, 'test')
  }

  def "bridge calls don't fail with null module when a string builder append is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)

    when:
    InstrumentationBridge.onStringBuilderAppend(new StringBuilder(), 'test')

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a string builder append is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringBuilderAppend(new StringBuilder(), 'test')

    then:
    1 * module.onStringBuilderAppend(_, _) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls module when a new string builder toString() is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)
    final self = new StringBuilder()

    when:
    InstrumentationBridge.onStringBuilderToString(self, 'test')

    then:
    1 * module.onStringBuilderToString(self, 'test')
  }

  def "bridge calls don't fail with null module when a string builder toString is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)

    when:
    InstrumentationBridge.onStringBuilderToString(new StringBuilder('test'), 'test')

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a string builder toString is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringBuilderToString(new StringBuilder('test'), 'test')

    then:
    1 * module.onStringBuilderToString(_, _) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls module when a when a string concat factory call is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringConcatFactory('Hello World!', ['Hello ', 'World!'] as String[], "\u0001\u0001", ["a", "b"] as Object[], [0, 1] as int[])

    then:
    1 * module.onStringConcatFactory('Hello World!', ['Hello ', 'World!'] as String[],  "\u0001\u0001", ["a", "b"] as Object[], [0, 1] as int[])
  }

  def "bridge calls don't fail with null module when a string concat factory call is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)

    when:
    InstrumentationBridge.onStringConcatFactory('Hello World!', ['Hello ', 'World!'] as String[], "\u0001\u0001", [] as Object[], [0, 1] as int[])

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a string concat factory call is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringConcatFactory('Hello World!', ['Hello ', 'World!'] as String[], "\u0001\u0001", [] as Object[], [0, 1] as int[])

    then:
    1 * module.onStringConcatFactory(_, _, _, _, _) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }
}
