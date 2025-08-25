package com.scalar.dl.client.tool;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.scalar.dl.client.config.ClientConfig;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import picocli.CommandLine;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;

public class CommandLineTestUtils {
  private static final String SOME_ENTITY_ID = "some_entity_id";
  private static final String SOME_CERT_PEM = "some_cert_string";
  private static final String SOME_PRIVATE_KEY_PEM = "some_private_key_string";

  /**
   * Returns the {@link Option} processor annotation associated with {@code fieldName} defined in
   * {@link Class<?>}.
   *
   * @param clazz class that defines {@code fieldName}.
   * @param fieldName field that is associated with {@link Option}.
   * @return {@link Option} instance.
   * @throws NoSuchFieldException if {@code fieldName} is not defined in {@link Class}.
   * @throws NullPointerException if {@code fieldName} is null.
   * @throws SecurityException if security violation occurs.
   */
  public static Option getOption(Class<?> clazz, String fieldName)
      throws NoSuchFieldException, NullPointerException, SecurityException {
    Field field = clazz.getDeclaredField(fieldName);
    return field.getAnnotation(Option.class);
  }

  /**
   * Returns the deserialized object {@code <T>} parsed from the {@code args} by the command {@link
   * CommandLine}.
   *
   * <p>If the required parameters or options of the command are not provided in the {@code args},
   * {@link MissingParameterException} is thrown.
   *
   * @param commandLine CommandLine instance to parse the {@code args}.
   * @param clazz Class type of the deserialized command, e.g., {@link CertificateRegistration}.
   * @param args Parameters and options of the command.
   * @return Deserialized object of the command.
   */
  public static <T> T parseArgs(CommandLine commandLine, Class<T> clazz, String[] args) {
    ParseResult parseResult = commandLine.parseArgs(args);
    List<CommandLine> parsed = parseResult.asCommandLineList();

    // Verify that the argument contains the top-level command.
    assertThat(parsed.size()).isEqualTo(1);

    return clazz.cast(parsed.get(0).getCommand());
  }

  /**
   * Returns the {@link File} of default client properties with minimum required settings.
   *
   * @param directory A {@link Path} of the target directory.
   * @param file A {@link String} of the file name.
   * @return the {@link File} of default client properties.
   */
  public static File createDefaultClientPropertiesFile(Path directory, String file)
      throws IOException {
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.DS_CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.DS_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);
    return createPropertiesFile(directory, file, props);
  }

  private static File createPropertiesFile(Path directory, String file, Properties properties)
      throws IOException {
    File propertiesFile = Files.createFile(directory.resolve(file)).toFile();
    try (OutputStream stream = Files.newOutputStream(propertiesFile.toPath())) {
      properties.store(stream, "test properties file");
    }
    return propertiesFile;
  }
}
