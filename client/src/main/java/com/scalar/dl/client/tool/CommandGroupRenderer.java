package com.scalar.dl.client.tool;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IHelpSectionRenderer;

/**
 * Renderer to display a group of subcommands like Git. ref.
 * https://github.com/remkop/picocli/blob/main/picocli-examples/src/main/java/picocli/examples/customhelp/GroupingDemo.java
 *
 * <p>[Usage] When you initialize the renderer, provide section information to represent the usage
 * message. {@link #sections} is a key-value pair where a) the key is a section description, and b)
 * the value is the grouped command classes associated with {@link Command} processor annotation for
 * the section.
 *
 * <p>For example, a new section is registered as follows;
 *
 * <p>1) Set the section description as the key with the corresponding command class. [Note] The
 * description MUST start/end with "%n".
 *
 * <pre>
 * sections.put(
 *   "%nregister identity information%n",
 *   Arrays.asList(ContractsRegistration.class)
 * );
 * </pre>
 *
 * <p>2) The code above is turned into the usage message below.
 *
 * <pre>
 * register identity information
 *   register-cert [description of register-cert if defined in @description]
 * </pre>
 */
public class CommandGroupRenderer implements IHelpSectionRenderer {
  /**
   * 1) The key is the section header description, e.g., "start a working area (see also: git help
   * tutorial)" 2) The value is the list of command classes associated with {@link Command}
   * processor annotation, e.g., {@link CertificateRegistration}.
   */
  private final ImmutableMap<String, List<Class<?>>> sections;

  public CommandGroupRenderer(ImmutableMap<String, List<Class<?>>> sections) {
    this.sections = sections;
  }

  @Override
  public String render(CommandLine.Help help) {
    if (help.commandSpec().subcommands().isEmpty()) {
      return "";
    }

    StringBuilder result = new StringBuilder();
    sections.forEach((key, value) -> result.append(renderSection(key, value, help)));
    return result.toString();
  }

  private String renderSection(
      String sectionHeading, List<Class<?>> cmdClazzList, CommandLine.Help help) {
    CommandLine.Help.TextTable textTable = createTextTable(help);

    for (Class<?> cmdClazz : cmdClazzList) {
      Command cmd = cmdClazz.getAnnotation(Command.class);
      CommandLine.Model.CommandSpec sub =
          help.commandSpec().subcommands().get(cmd.name()).getCommandSpec();

      // create comma-separated list of command name and aliases
      String names = sub.names().toString();
      names = names.substring(1, names.length() - 1); // remove leading '[' and trailing ']'

      // description may contain line separators; use Text::splitLines to handle this
      String description = description(sub.usageMessage());
      CommandLine.Help.Ansi.Text[] lines =
          help.colorScheme().text(String.format(description)).splitLines();

      for (int i = 0; i < lines.length; i++) {
        CommandLine.Help.Ansi.Text cmdNamesText =
            help.colorScheme().commandText(i == 0 ? names : "");
        textTable.addRowValues(cmdNamesText, lines[i]);
      }
    }
    return help.createHeading(sectionHeading) + textTable.toString();
  }

  private CommandLine.Help.TextTable createTextTable(CommandLine.Help help) {
    CommandLine.Model.CommandSpec spec = help.commandSpec();
    // prepare layout: two columns
    // the left column overflows, the right column wraps if text is too long
    int commandLength = maxLength(spec.subcommands(), 37);
    CommandLine.Help.TextTable textTable =
        CommandLine.Help.TextTable.forColumns(
            help.colorScheme(),
            new CommandLine.Help.Column(
                commandLength + 2, 2, CommandLine.Help.Column.Overflow.SPAN),
            new CommandLine.Help.Column(
                spec.usageMessage().width() - (commandLength + 2),
                2,
                CommandLine.Help.Column.Overflow.WRAP));
    textTable.setAdjustLineBreaksForWideCJKCharacters(
        spec.usageMessage().adjustLineBreaksForWideCJKCharacters());
    return textTable;
  }

  private int maxLength(Map<String, CommandLine> subcommands, int max) {
    int result =
        subcommands.values().stream()
            .map(cmd -> cmd.getCommandSpec().names().toString().length() - 2)
            .max(Integer::compareTo)
            .get();
    return Math.min(max, result);
  }

  private String description(CommandLine.Model.UsageMessageSpec usageMessage) {
    if (usageMessage.header().length > 0) {
      return usageMessage.header()[0];
    }
    if (usageMessage.description().length > 0) {
      return usageMessage.description()[0];
    }
    return "";
  }
}
