package com.scalar.dl.client.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class CommonOptionsTest {
  private CommonOptions commonOptions;

  @BeforeEach
  void setup() {
    commonOptions = new CommonOptions();
  }

  @Nested
  @DisplayName("#printStackTrace(...)")
  class printStackTrace {
    @Nested
    @DisplayName("when --stacktrace option is set")
    class whenStackTraceOptionIsSet {
      @Test
      @DisplayName("execute Exception#printStackTrace()")
      void executePrintStackTrace() {
        // Arrange
        commonOptions.stacktraceEnabled = true;
        Exception exception = mock(Exception.class);

        // Act
        commonOptions.printStackTrace(exception);

        // Assert
        verify(exception).printStackTrace();
      }
    }

    @Nested
    @DisplayName("when --stacktrace option is NOT set")
    class whenStackTraceOptionIsNotSet {
      @Test
      @DisplayName("does NOT execute Exception#printStackTrace()")
      void doesNotExecutePrintStackTrace() {
        // Arrange
        assertThat(commonOptions.stacktraceEnabled).isFalse();
        Exception exception = mock(Exception.class);

        // Act
        commonOptions.printStackTrace(exception);

        // Assert
        verify(exception, never()).printStackTrace();
      }
    }
  }

  @Nested
  @DisplayName("@Option annotation")
  class OptionAnnotation {
    @Nested
    @DisplayName("-h")
    class help {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("helpRequested");

        assertThat(option.required()).isFalse();
        assertThat(option.usageHelp()).isTrue();
        assertThat(option.names()).isEqualTo(new String[] {"-h", "--help"});
      }
    }

    @Nested
    @DisplayName("--properties / --config")
    class properties {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("properties");

        assertThat(option.required()).isTrue();
        assertThat(option.paramLabel()).isEqualTo("PROPERTIES_FILE");
        assertThat(option.names()).isEqualTo(new String[] {"--properties", "--config"});
      }
    }

    @Nested
    @DisplayName("--stacktrace")
    class stacktrace {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("stacktraceEnabled");

        assertThat(option.required()).isFalse();
        assertThat(option.names()).isEqualTo(new String[] {"--stacktrace"});
      }
    }

    @Nested
    @DisplayName("--use-gateway")
    class useGateway {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("useGateway");

        assertThat(option.required()).isFalse();
        assertThat(option.paramLabel()).isEqualTo("USE_GATEWAY");
        assertThat(option.names()).isEqualTo(new String[] {"-g", "--use-gateway"});
      }
    }
  }

  private CommandLine.Option getOption(String fieldName) throws Exception {
    return CommandLineTestUtils.getOption(CommonOptions.class, fieldName);
  }
}
